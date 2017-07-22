/**
 * 
 */
package com.pi.services;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pi.SystemLogger;
import com.pi.infrastructure.AsynchronousDevice;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.Device.DeviceConfig;
import com.pi.infrastructure.DeviceType.Params;
import com.pi.infrastructure.DeviceLoader;
import com.pi.infrastructure.DeviceType;
import com.pi.infrastructure.NodeController;
import com.pi.infrastructure.RemoteDevice.RemoteDeviceConfig;
import com.pi.infrastructure.util.DeviceLockedException;
import com.pi.infrastructure.util.PropertyManger;
import com.pi.infrastructure.util.PropertyManger.PropertyKeys;
import com.pi.model.DeviceState;
import com.pi.services.TaskExecutorService.Task;

/**
 * @author Christian Everett
 *
 */

@Service
public class Processor extends NodeController implements Runnable
{
	private AtomicBoolean shutdownProcessor = new AtomicBoolean(false);
	private static Thread processingThread = null;

	// Background processor data structures
	private LinkedBlockingQueue<DeviceState> processingQueue = new LinkedBlockingQueue<>(10_000);
	private ConcurrentHashMap<String, InetAddress> nodeMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, DeviceState> cachedStates = new ConcurrentHashMap<>();
	private Map<String, DeviceState> databaseSavedStates = new HashMap<>();
	private Set<String> lockedDevices = new HashSet<>();

	// Background processor services
	private TaskExecutorService taskService = new TaskExecutorService(2);
	private PersistenceManager persistenceManager = null;
	@Autowired
	private EventProcessingService eventProcessingService;
	private DeviceLoggingService deviceLoggingService = null;
	private DeviceLoader deviceLoader = null;
	private NodeDiscovererService nodeDiscovererService = null;

	// Background processor tasks
	private Task databaseTask;

//	public static synchronized Thread createBackgroundProcessor() throws Exception
//	{
//		if (singleton != null)
//			throw new Exception("Background Processor already created");
//		singleton = new Processor();
//		processingThread = new Thread((Runnable) singleton);
//		return processingThread;
//	}

//	public static Processor getInstance()
//	{
//		return (Processor) NodeController.getInstance();
//	}

	private Processor() throws Exception
	{
		SystemLogger.getLogger().info("Starting Node Discovery Service");
		nodeDiscovererService = new NodeDiscovererService(this);
		nodeDiscovererService.start(5);
		Device.registerNodeManger(this);
		
		persistenceManager = PersistenceManager.loadPersistanceManager();
		deviceLoader = DeviceLoader.createNewDeviceLoader();

		SystemLogger.getLogger().info("Starting Device Logging Service");
		deviceLoggingService = new DeviceLoggingService(this);
		deviceLoggingService.start();
		
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

	public PersistenceManager getPersistenceManger()
	{
		return persistenceManager;
	}

	public EventProcessingService getEventProcessingService()
	{
		return eventProcessingService;
	}

	public synchronized void registerNode(String node, InetAddress address)
	{
		nodeMap.put(node, address);
		createRemoteDevices(node);
		SystemLogger.getLogger().info("Registered Node: " + node);
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
				throw new DeviceLockedException("This device is locked");
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

	@Override
	public synchronized String createNewDevice(DeviceConfig config, boolean isRemoteDevice) throws IOException
	{
		// TODO enable logging
		if(!isRemoteDevice)
		{
			String name = super.createNewDevice(config, false);
			Device device = lookupDevice(name);
			if(device != null)
				device.loadSavedData(databaseSavedStates.remove(name));
			return name;
		}
		else
		{
			deviceMap.put(config.getName(), null);
			RemoteDeviceConfig remoteDeviceConfig = (RemoteDeviceConfig) config;
			uninitializedRemoteDevices.put(remoteDeviceConfig.getNodeID(), config);
			createRemoteDevices(remoteDeviceConfig.getNodeID());
			return config.getName();
		}	
	}

	public synchronized void createRemoteDevices(String nodeID)
	{
		InetAddress address = lookupNodeAddress(nodeID);

		if (address == null)
			return;


		Collection<DeviceConfig> configs = uninitializedRemoteDevices.removeAll(nodeID);

		for (DeviceConfig config : configs)
		{
			try
			{
				((RemoteDeviceConfig) config).setUrl("http://" + address.getHostAddress() + ":8080");
				Device device = config.buildDevice();

				if(device != null)
					device.loadSavedData(databaseSavedStates.remove(device.getName()));
				
				deviceMap.put(device.getName(), device);
			}
			catch (Exception e)
			{
				SystemLogger.getLogger().severe("Error Loading Device: " + config.getName() + ". Exception: " + e.getMessage());
			}
		}
	}

	public void load()
	{
		try
		{
			loadDeviceStatesIntoCache();

			SystemLogger.getLogger().info("Loading Devices");
			deviceLoader.loadDevices(this);
			
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

					device = lookupDevice(deviceToConfig);

					if (device != null)
					{
						DeviceState savedState = device.getState(false);
						closeDevice(deviceToConfig);
						deviceLoader.loadDevice(this, deviceToConfig);
						device = lookupDevice(deviceToConfig);

						if (device != null)
						{
							device.execute(savedState);
							SystemLogger.getLogger().info("Reloaded " + deviceToConfig);
						}
						else
							SystemLogger.getLogger().info("Could not load: " + deviceToConfig);
					}

					break;
				case PROCESSOR_ACTIONS.RELOAD_DEVICE_ALL:
					saveAndCloseAllDevices();
					loadDeviceStatesIntoCache();
					deviceLoader.loadDevices(this);
					SystemLogger.getLogger().info("Reloaded Devices");
					break;
				case PROCESSOR_ACTIONS.LOAD_DEVICE:
					deviceLoader.loadDevice(this, deviceToConfig);
					break;

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
