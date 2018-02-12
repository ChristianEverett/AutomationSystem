/**
 * 
 */
package com.pi.services;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
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
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.pi.SystemLogger;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.Device.DeviceConfig;
import com.pi.infrastructure.DeviceType.Params;
import com.pi.infrastructure.BaseNodeController;
import com.pi.infrastructure.RemoteDeviceProxy.RemoteDeviceConfig;
import com.pi.infrastructure.RepoistoryManager;
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
	public static final long UP_TIME = System.currentTimeMillis();

	// NodeController data structures
	private ConcurrentHashMap<String, InetAddress> nodeMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, DeviceState> cachedStates = new ConcurrentHashMap<>();
	private Map<String, DeviceState> databaseSavedStates = new HashMap<>();
	private Set<String> lockedDevices = new HashSet<>();
	protected Multimap<String, RemoteDeviceConfig> uninitializedRemoteDevices = ArrayListMultimap.create();	
	
	@Autowired
	private RepoistoryManager repositoryManager;
	
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
	@Autowired
	private SimpMessageSendingOperations messagingTemplate;

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
		databaseTask = taskService.scheduleSafeTask(() ->
		{
			save();
		}, 1L, interval, TimeUnit.MINUTES);
		
		try
		{
			SystemLogger.getLogger().info("Starting Node Discovery Service");
			nodeDiscovererService.start(5, (node, address) -> registerNode(node, address));

			SystemLogger.getLogger().info("Starting Device Logging Service");
			deviceLoggingService.start();
			
			loadDeviceStatesIntoDatabaseCache();	
			
			SystemLogger.getLogger().info("Starting rmi process");
			System.setProperty("java.rmi.server.hostname", NodeDiscovererService.getLocalIPv4Address()); 
			LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
			Naming.rebind(RMI_NAME, UnicastRemoteObject.exportObject(this, 0));
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
		}
	}
	
	@Override
	public <T extends Serializable> Collection<T> getRepositoryValues(String type)
	{
		return repositoryManager.getRepositoryValues(type);
	}
	
	@Override
	public <T extends Serializable, K extends Serializable> T getRepositoryValue(String type, K key)
	{
		return repositoryManager.getRepositoryValue(type, key);
	}

	@Override
	public <T extends Serializable, K extends Serializable> void setRepositoryValue(String type, T value)
	{
		repositoryManager.setRepositoryValue(type, value);
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
	
	public boolean hasStateChanged(DeviceState deviceState)
	{
		DeviceState cacheState = cachedStates.get(deviceState.getName());
		
		return cacheState == null || !cacheState.contains(deviceState);
	}

	@Override
	public void update(DeviceState state)
	{
		if(hasStateChanged(state))
		{
			messagingTemplate.convertAndSend("/topic/" + state.getName(), state);	
			cachedStates.put(state.getName(), state);		
			eventProcessingService.update(state);
			deviceLoggingService.log(state);
		}
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
	public synchronized Device createNewDevice(DeviceConfig config) throws Exception
	{
		Device device = super.createNewDevice(config);
		loadSavedState(device);
		
		return device;
	}
	
	public synchronized void createNewRemoteDevice(DeviceConfig config)
	{
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
				
				addDevice(device);
			}
			catch (Exception e)
			{
				SystemLogger.getLogger().severe("Error Loading Device: " + config.getName() + ". Exception: " + e.getMessage());
			}
		}
	}

	private void loadSavedState(Device device) throws Exception
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

	private void loadDeviceStatesIntoDatabaseCache() throws SQLException, IOException
	{
		SystemLogger.getLogger().info("Loading Device States");
		Collection<DeviceStateDAO> savedStates = repositoryManager.getRepositoryValues(RepositoryType.DeviceState);
		
		savedStates.forEach((state) ->
		{
			databaseSavedStates.put(state.getDeviceName(), state.getDeviceState());
		});
	}
	
	private void save()
	{	
		List<DeviceState> states = getStates();
		List<DeviceStateDAO> savedStates = new ArrayList<>(states.size());
		
		states.forEach(state ->
		{
			savedStates.add(new DeviceStateDAO(state));
		});
		
		repositoryManager.setRepositoryValues(RepositoryType.DeviceState,  savedStates);
	}

	private void saveAndCloseAllDevices()
	{
		// Shutdown all devices and save their state
		SystemLogger.getLogger().info("Saving Device");
		save();
		closeAllDevices();
	}

	public boolean deviceNeedsUpdate(Device device, DeviceState state)
	{
		if(device == null)
			throw new DeviceDoesNotExist(state.getName());
		
		return (device.isAsynchronousDevice() || hasStateChanged(state));
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
		scheduleAll(profileName, false);
	}
	
	@Override
	public void unTrigger(String profileName)
	{
		scheduleAll(profileName, true);
	}
	
	private void scheduleAll(String profileName, boolean inverted)
	{
		ActionProfile profile = repositoryManager.getRepositoryValue(RepositoryType.ActionProfile, profileName);
		
		if (profile != null)
		{
			for (DeviceState element : inverted ? profile.getInvertedDeviceStates() : profile.getDeviceStates())
			{
				if (hasStateChanged(element))
				{
					scheduleAction(element);
				}
			} 
		}
	}
	
	private void processAction(DeviceState state)
	{
		try
		{
			if (state != null)
			{
				String deviceToApplyState = state.getName();
				Device device = lookupDevice(deviceToApplyState);
				String deviceToConfig = (String) state.getParam(PROCESSOR_ACTIONS.DEVICE);

				switch (deviceToApplyState)
				{
				case PROCESSOR_ACTIONS.RELOAD_DEVICE:
					reloadDevice(deviceToConfig);
					break;
				case PROCESSOR_ACTIONS.RELOAD_DEVICE_ALL:
				{
					saveAndCloseAllDevices();
					loadDeviceStatesIntoDatabaseCache();
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
			SystemLogger.getLogger().severe(e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private void reloadDevice(String deviceToConfig) throws Exception
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
								
				saveAndCloseAllDevices();
				repositoryManager.flushAll();
				
				synchronized (this)
				{
					notify();
				}
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
