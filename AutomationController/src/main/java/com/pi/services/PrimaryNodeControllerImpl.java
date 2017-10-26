/**
 * 
 */
package com.pi.services;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.pi.SystemLogger;
import com.pi.infrastructure.AsynchronousDevice;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.Device.DeviceConfig;
import com.pi.infrastructure.DeviceType.Params;
import com.pi.infrastructure.BaseNodeController;
import com.pi.infrastructure.RemoteDeviceProxy.RemoteDeviceConfig;
import com.pi.infrastructure.util.DeviceDoesNotExist;
import com.pi.infrastructure.util.DeviceLockedException;
import com.pi.infrastructure.util.PropertyManger;
import com.pi.infrastructure.util.PropertyManger.PropertyKeys;
import com.pi.infrastructure.util.RepositoryDoesNotExistException;
import com.pi.model.ActionProfile;
import com.pi.model.DeviceState;
import com.pi.model.DeviceStateDAO;
import com.pi.model.repository.ActionProfileJpaRepository;
import com.pi.model.repository.CurrentDeviceStateJpaRepository;
import com.pi.model.repository.RepositoryType;
import com.pi.services.TaskExecutorService.Task;

/**
 * @author Christian Everett
 *
 */

@Service
public class PrimaryNodeControllerImpl extends BaseNodeController
{
	private AtomicBoolean shutdownProcessor = new AtomicBoolean(false);

	// NodeController data structures
	private ConcurrentHashMap<String, InetAddress> nodeMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, DeviceState> cachedStates = new ConcurrentHashMap<>();
	private Map<String, DeviceState> databaseSavedStates = new HashMap<>();
	private Set<String> lockedDevices = new HashSet<>();
	protected Multimap<String, RemoteDeviceConfig> uninitializedRemoteDevices = ArrayListMultimap.create();	
	@Autowired
	private Map<String, JpaRepository<?, ?>> repositorys;
	
	// NodeController services
	private TaskExecutorService taskService = new TaskExecutorService(2);

	@Autowired
	private EventProcessingService eventProcessingService;
	@Autowired
	private DeviceLoggingService deviceLoggingService;
	@Autowired
	private DeviceLoadingService deviceLoader;
	@Autowired
	private NodeDiscovererService nodeDiscovererService;

	// NodeController tasks
	private Task databaseTask;

	private PrimaryNodeControllerImpl() throws Exception
	{
	}

	@PostConstruct
	public void load()
	{
		singleton = this;
		
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
		
		try
		{
			SystemLogger.getLogger().info("Starting Node Discovery Service");
			nodeDiscovererService.start(5, (node, address) -> registerNode(node, address));

			SystemLogger.getLogger().info("Starting Device Logging Service");
			deviceLoggingService.start();
			
			loadDeviceStatesIntoCache();			
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
		}
	}
	
	@Override
	public <T extends Serializable> Collection<T> getRepositoryValues(String type)
	{
		JpaRepository<?, ?> repository = repositorys.get(type);
		
		if(repository == null)
			throw new RepositoryDoesNotExistException(type);
		
		return (Collection<T>) repository.findAll();
	}
	
	@Override
	public <T extends Serializable, K extends Serializable> T getRepositoryValue(String type, K key)
	{
		JpaRepository<?, K> repository = (JpaRepository<?, K>) repositorys.get(type);
		
		if(repository == null)
			throw new RepositoryDoesNotExistException(type);
		
		return (T) repository.findOne(key);
	}

	@Override
	public <T extends Serializable, K extends Serializable> void setRepositoryValue(String type, K key, T value)
	{
		JpaRepository<T, K> repository = (JpaRepository<T, K>) repositorys.get(type);
		
		if(repository == null)
			throw new RuntimeException("Repository does not exist");
		
		repository.save(value);
	}
	
	public synchronized void registerNode(String node, InetAddress address)
	{
		if (!nodeMap.containsKey(node))
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
	
	private boolean isInCache(DeviceState deviceState)
	{
		DeviceState cacheState = cachedStates.get(deviceState.getName());
		
		return cacheState != null && cacheState.contains(deviceState);
	}

	@Override
	public void update(DeviceState state)
	{
		deviceLoggingService.log(state);
		
//		if(isInCache(state))
//			return;
		
		cachedStates.put(state.getName(), state);
		eventProcessingService.update(state);
	}
	
	@Override
	public DeviceState getDeviceState(String name)
	{
		DeviceState state = cachedStates.get(name);
		if (state != null)
			return state;
			
		return super.getDeviceState(name);
	}

	@Override
	public void scheduleAction(DeviceState state)
	{
		if (state.hasData())
		{
			if (!isLocked(state.getName()))
				processAction(state);
			else
				throw new DeviceLockedException(state.getName());
		}
		
		checkIfLockShouldBeSet(state);
	}

	@Override
	public void trigger(String profileName)
	{
		ActionProfileJpaRepository actionProfileRepository = (ActionProfileJpaRepository) repositorys.get(RepositoryType.ActionProfile);
		ActionProfile profile = actionProfileRepository.findOne(profileName);
		
		if (profile != null)
		{
			for (DeviceState element : profile.getDeviceStates())
			{
				if (!element.equals(getDeviceState(element.getName())))
				{
					scheduleAction(element);
				}
			} 
		}
	}
	
	@Override
	public synchronized Device createNewDevice(DeviceConfig config) throws IOException
	{
		Device device = super.createNewDevice(config);
		loadSavedState(device);
		
		return device;
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
				config.setHost(address.getHostAddress());
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

	public void loadDevices()
	{
		SystemLogger.getLogger().info("Loading Devices");
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

	private void loadDeviceStatesIntoCache() throws SQLException, IOException
	{
		SystemLogger.getLogger().info("Loading Device States");
		CurrentDeviceStateJpaRepository repository = (CurrentDeviceStateJpaRepository) repositorys.get(RepositoryType.DeviceState);
		
		repository.findAll().forEach((state) ->
		{
			databaseSavedStates.put(state.getDeviceName(), state.getDeviceState());
		});
	}
	
	private void save() throws SQLException, IOException
	{
		CurrentDeviceStateJpaRepository repository = (CurrentDeviceStateJpaRepository) repositorys.get(RepositoryType.DeviceState);
		
		List<DeviceState> states = getStates();
		List<DeviceStateDAO> savedStates = new ArrayList<>(states.size());
		
		states.forEach(state ->
		{
			savedStates.add(new DeviceStateDAO(state));
		});
		
		repository.save(savedStates);
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
		
		return (device.isAsynchronousDevice() || !isInCache(state));
	}
	
	public void shutdown()
	{
		if (!shutdownProcessor.get())
		{
			try
			{
				shutdownProcessor.set(true);
				
				SystemLogger.getLogger().info("Stopping all background tasks");
				taskService.cancelAllTasks();
				nodeDiscovererService.stop();
				deviceLoggingService.stop();
				
				// Shutdown all devices and save their state
				SystemLogger.getLogger().info("Saving Device States and shutting down");
				saveAndCloseAllDevices();
				repositorys.forEach((name, repository) -> 
				{
					repository.flush();
				});
				
				notify();
			}
			catch (Throwable e)
			{
				SystemLogger.getLogger().severe(e.getMessage());
			}
			finally
			{
				SystemLogger.getLogger().info("Primary Node has been shutdown.");
			} 
		}
	}

	private synchronized void processAction(DeviceState state)
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
						//eventProcessingService.updateEventSuppression(state); TODO
						device.execute(state);
					}
					break;
				}
			}
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
		}
	}

	private void reloadDevice(String deviceToConfig) throws IOException
	{
		Device device = lookupDevice(deviceToConfig);

		if(device == null)
			throw new DeviceDoesNotExist(deviceToConfig);

		DeviceState savedState = device.getCurrentDeviceState();
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

	public synchronized void checkIfLockShouldBeSet(DeviceState state)
	{
		Boolean shouldLock = state.getParamTyped(Params.LOCK, null);
		
		if(shouldLock != null)
		{
			if (shouldLock)
				lockedDevices.add(state.getName());
			else 									
				lockedDevices.remove(state.getName());
		}
	}

	public synchronized boolean isLocked(String deviceName)
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
