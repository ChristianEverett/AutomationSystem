/**
 * 
 */
package com.pi.services;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.pi.SystemLogger;
import com.pi.infrastructure.AsynchronousDevice;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.Device.DeviceConfig;
import com.pi.infrastructure.DeviceType.Params;
import com.pi.infrastructure.DeviceType;
import com.pi.infrastructure.BaseNodeController;
import com.pi.infrastructure.RemoteDeviceProxy.RemoteDeviceConfig;
import com.pi.infrastructure.util.DeviceDoesNotExist;
import com.pi.infrastructure.util.DeviceLockedException;
import com.pi.infrastructure.util.PropertyManger;
import com.pi.infrastructure.util.PropertyManger.PropertyKeys;
import com.pi.infrastructure.util.RepositoryDoesNotExistException;
import com.pi.model.DeviceState;
import com.pi.model.repository.BaseRepository;
import com.pi.services.TaskExecutorService.Task;

/**
 * @author Christian Everett
 *
 */

@Service
public class Processor extends BaseNodeController implements Runnable
{
	private AtomicBoolean shutdownProcessor = new AtomicBoolean(false);
	private static Thread processingThread = null;

	// Background processor data structures
	private LinkedBlockingQueue<DeviceState> processingQueue = new LinkedBlockingQueue<>(10_000);
	private ConcurrentHashMap<String, InetAddress> nodeMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, DeviceState> cachedStates = new ConcurrentHashMap<>();
	private Map<String, DeviceState> databaseSavedStates = new HashMap<>();
	private Set<String> lockedDevices = new HashSet<>();
	protected Multimap<String, RemoteDeviceConfig> uninitializedRemoteDevices = ArrayListMultimap.create();	
	@Autowired
	private Map<String, BaseRepository<?, ?>> repositorys;
	
	// Background processor services
	private TaskExecutorService taskService = new TaskExecutorService(2);
	@Autowired
	private DatabaseService persistenceManager = null;
	@Autowired
	private EventProcessingService eventProcessingService;
	@Autowired
	private DeviceLoggingService deviceLoggingService;
	@Autowired
	private DeviceLoadingService deviceLoader;
	@Autowired
	private NodeDiscovererService nodeDiscovererService;

	// Background processor tasks
	private Task databaseTask;

	private Processor() throws Exception
	{
		Device.registerNodeManger(this);

		SystemLogger.getLogger().info("Scheduling Database Task");
		Long interval = Long.parseLong(PropertyManger.loadProperty(PropertyKeys.DATABASE_POLL_FREQUENCY, "30"));
		databaseTask = taskService.scheduleTask(() ->
		{
			try
			{
				save();
			}
			catch (Throwable e)
			{
				SystemLogger.getLogger().severe(e.getMessage());
			}
		}, 1L, interval, TimeUnit.MINUTES);
		
		SystemLogger.getLogger().info("Starting Node Discovery Service");
		nodeDiscovererService.start(5, (node, address) -> registerNode(node, address));
		singleton = this;
	}

	@Override
	public void run()
	{	
		try
		{
			while (!shutdownProcessor.get())
			{
				DeviceState result = processingQueue.take();

				if (!shutdownProcessor.get() && result != null)
					processAction(result);
			}
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().info(e.getMessage());
		}
	}

	public TaskExecutorService getTaskExecutorService()
	{
		return taskService;
	}

	public DatabaseService getPersistenceManger()
	{
		return persistenceManager;
	}

	public EventProcessingService getEventProcessingService()
	{
		return eventProcessingService;
	}

	@Override
	public <T extends Serializable> T getRepositoryValue(String type, String key)
	{
		BaseRepository<?, ?> repository = repositorys.get(type);
		
		if(repository == null)
			throw new RepositoryDoesNotExistException(type);
		
		return (T) repository.get(key);
	}

	@Override
	public <T extends Serializable> void setRepositoryValue(String type, String key, T value)
	{
		BaseRepository<?, ?> repository = repositorys.get(type);
		
		if(repository == null)
			throw new RuntimeException("Repository does not exist");
		
		repository.put(key, value);
	}
	
	public synchronized void registerNode(String node, InetAddress address)
	{
		if (nodeMap.get(node) == null)
		{
			nodeMap.put(node, address);
			createRemoteDevicesForNode(node);
			SystemLogger.getLogger().info("Registered Node: " + node);
		}
	}

	public InetAddress lookupNodeAddress(String node)
	{
		return nodeMap.get(node);
	}

	@Override
	public void update(DeviceState state)
	{
		cachedStates.put(state.getName(), state);
		
		if (eventProcessingService != null)
			eventProcessingService.update(state);
		
		deviceLoggingService.log(state);
	}

	@Override
	public void scheduleAction(DeviceState state)
	{
		checkIfLockShouldBeSet(state);
		
		if (state.hasData())
		{
			if (!isLocked(state.getName()))
				processingQueue.add(state);
			else
				throw new DeviceLockedException(state.getName());
		}
	}

	@Override
	public DeviceState getDeviceState(String name, boolean isForDatabase)
	{
		if (!isForDatabase)
		{
			DeviceState state = cachedStates.get(name);
			if (state != null)
				return state;
		}
		
		return super.getDeviceState(name, isForDatabase);
	}

	public void loadDevices()
	{
		List<DeviceConfig> configs = deviceLoader.loadDevices();
		
		for(DeviceConfig config : configs)
		{
			try
			{
				if (config instanceof RemoteDeviceConfig)
					createNewRemoteDevice(config);
				else
					createNewDevice(config);
			}
			catch (Exception e)
			{
				SystemLogger.getLogger().severe("Error creating Device: " + config.getName() + ". Exception: " + e.getMessage());
			}
		}
	}
	
	@Override
	public synchronized String createNewDevice(DeviceConfig config) throws IOException
	{
		String name = super.createNewDevice(config);
		Device device = lookupDevice(name);
		loadSavedState(device);
		
		return name;
	}
	
	public synchronized void createNewRemoteDevice(DeviceConfig config)
	{
		deviceMap.put(config.getName(), null);
		RemoteDeviceConfig remoteDeviceConfig = (RemoteDeviceConfig) config;
		uninitializedRemoteDevices.put(remoteDeviceConfig.getNodeID(), remoteDeviceConfig);
		createRemoteDevicesForNode(remoteDeviceConfig.getNodeID());
	}

	public synchronized void createRemoteDevicesForNode(String nodeID)
	{
		InetAddress address = lookupNodeAddress(nodeID);

		if (address == null)
			return;

		Collection<RemoteDeviceConfig> configs = uninitializedRemoteDevices.removeAll(nodeID);

		for (RemoteDeviceConfig config : configs)
		{
			try
			{
				config.setUrl("http://" + address.getHostAddress() + ":8080");
				Device device = config.buildDevice();

				loadSavedState(device);
				
				deviceMap.put(device.getName(), device);
			}
			catch (Exception e)
			{
				SystemLogger.getLogger().severe("Error Loading Device: " + config.getName() + ". Exception: " + e.getMessage());
			}
		}
	}

	private void loadSavedState(Device device) throws IOException
	{
		if(device != null)
			device.loadSavedData(databaseSavedStates.remove(device.getName()));
	}

	public void load()
	{
		try
		{
			SystemLogger.getLogger().info("Starting Device Logging Service");
			deviceLoggingService.start();
			
			loadDeviceStatesIntoCache();

			SystemLogger.getLogger().info("Loading Devices");
			loadDevices();
			
			SystemLogger.getLogger().info("Loading Events");
			eventProcessingService.createEvents(persistenceManager.readAllEvent());
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
		}
	}

	private void loadDeviceStatesIntoCache() throws SQLException, IOException
	{
		SystemLogger.getLogger().info("Loading Device States");
		persistenceManager.readAllDeviceStates().forEach((state) ->
		{
			databaseSavedStates.put(state.getName(), state);
		});
	}
	
	private void save() throws SQLException, IOException
	{
		persistenceManager.saveStates(getStates(true));
	}

	private void saveAndCloseAllDevices() throws SQLException, IOException
	{
		save();
		closeAllDevices();
	}

	public boolean deviceNeedsUpdate(Device device, DeviceState state)
	{
		if(device == null)
			throw new RuntimeException("device not found for: " + state.getName());
		
		return (device instanceof AsynchronousDevice || !state.contains(cachedStates.get(state.getName())));
	}
	
	public synchronized void shutdown()
	{
		if (!shutdownProcessor.get())
		{
			try
			{
				shutdownProcessor.set(true);
				
				if (processingThread.isAlive())
				{
					// Queue a No-Op to wakeup background processor
					processingQueue.add(Device.createNewDeviceState(DeviceType.UNKNOWN));
				}
				SystemLogger.getLogger().info("Stopping all background tasks");
				taskService.cancelAllTasks();
				nodeDiscovererService.stop();
				deviceLoggingService.stop();
				// Shutdown all devices and save their state
				SystemLogger.getLogger().info("Saving Device States and shutting down");
				saveAndCloseAllDevices();
				persistenceManager.close();
			}
			catch (Throwable e)
			{
				SystemLogger.getLogger().severe(e.getMessage());
			}
			finally
			{
				SystemLogger.getLogger().info("System has been shutdown.");
			} 
		}
	}

	private synchronized void processAction(DeviceState state) throws Exception
	{
		try
		{
			if (state != null)
			{
				String deviceToApplyState = state.getName();
				Device device = lookupDevice(deviceToApplyState);
				String deviceToConfig = (String) state.getParam(PROCESSOR_ACTIONS.DEVICE, false);

				// ProcessBuilder process = new ProcessBuilder("python", "../echo/fauxmo.py");
				// process.redirectOutput(ProcessBuilder.Redirect.INHERIT);
				// process.start();
				// Process process = rt.exec("sudo ./codesend " + code + " -l " + PULSELENGTH + " -p " + IR_PIN);
				// process.waitFor();

				switch (deviceToApplyState)
				{
				case PROCESSOR_ACTIONS.RELOAD_DEVICE:

					reloadDevice(deviceToConfig);

					break;
				case PROCESSOR_ACTIONS.RELOAD_DEVICE_ALL:
				{
					saveAndCloseAllDevices();
					loadDeviceStatesIntoCache();
					DeviceConfig config = deviceLoader.loadDevice(deviceToConfig);
					createNewDevice(config);
					SystemLogger.getLogger().info("Reloaded Devices");
					break;
				}
				case PROCESSOR_ACTIONS.LOAD_DEVICE:
				{
					DeviceConfig config = deviceLoader.loadDevice(deviceToConfig);
					createNewDevice(config);
					SystemLogger.getLogger().info("Reloaded Device: " + deviceToConfig);
					break;
				}
				case PROCESSOR_ACTIONS.CLOSE_DEVICE:
					closeDevice(deviceToConfig);
					break;

				case PROCESSOR_ACTIONS.SAVE_DATA:
					save();
					break;

				case PROCESSOR_ACTIONS.SHUTDOWN:
					shutdown();
					break;
					
				default:					
					if (deviceNeedsUpdate(device, state))
					{
						device.execute(state);
					}
					break;
				}
			}
		}
		catch (Exception e)
		{
			if (e instanceof InterruptedException)
				throw e;
			SystemLogger.getLogger().severe(e.getMessage());
		}
	}

	private void reloadDevice(String deviceToConfig) throws IOException
	{
		Device device = lookupDevice(deviceToConfig);

		if(device == null)
			throw new DeviceDoesNotExist(deviceToConfig);

		DeviceState savedState = device.getState(true);
		closeDevice(deviceToConfig);
		DeviceConfig config = deviceLoader.loadDevice(deviceToConfig);
		createNewDevice(config);
		device = lookupDevice(deviceToConfig);

		if (device != null)
		{
			device.loadSavedData(savedState);
			SystemLogger.getLogger().info("Reloaded " + deviceToConfig);
		}
		else
			SystemLogger.getLogger().info("Could not load: " + deviceToConfig);
	}

	public void checkIfLockShouldBeSet(DeviceState state)
	{
		Boolean shouldLock = state.getParamTyped(Params.LOCK, Boolean.class, null);
		
		if(shouldLock != null)
		{
			if (shouldLock)
				lockedDevices.add(state.getName());
			else 									
				lockedDevices.remove(state.getName());
		}
	}

	public boolean isLocked(String deviceName)
	{
		return lockedDevices.contains(deviceName);
	}
	
	private interface PROCESSOR_ACTIONS
	{
		public static final String RELOAD_DEVICE_ALL = "reload_device_all";
		public static final String RELOAD_DEVICE = "reload_device";
		public static final String LOAD_DEVICE = "load_device";
		public static final String CLOSE_DEVICE = "close_device";
		public static final String SAVE_DATA = "save_data";
		public static final String SHUTDOWN = "shutdown";

		public static final String DEVICE = "device";
	}
}
