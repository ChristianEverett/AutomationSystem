/**
 * 
 */
package com.pi.backgroundprocessor;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.pi.Application;
import com.pi.backgroundprocessor.TaskExecutorService.Task;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.Device.DeviceConfig;
import com.pi.infrastructure.DeviceLoader;
import com.pi.infrastructure.DeviceType;
import com.pi.infrastructure.NodeController;
import com.pi.infrastructure.RemoteDevice.RemoteDeviceConfig;
import com.pi.infrastructure.util.PropertyManger;
import com.pi.infrastructure.util.PropertyManger.PropertyKeys;
import com.pi.model.DeviceState;

/**
 * @author Christian Everett
 *
 */

public class Processor extends NodeController implements Runnable
{
	private AtomicBoolean processorRunning = new AtomicBoolean(false);
	private AtomicBoolean shutdownProcessor = new AtomicBoolean(false);

	// Background processor data structures
	private LinkedBlockingQueue<DeviceState> processingQueue = new LinkedBlockingQueue<>(50_000);
	private HashMap<String, InetAddress> nodeMap = new HashMap<>();
	private ConcurrentHashMap<String, DeviceState> cachedStates = new ConcurrentHashMap<>();
	private Map<String, DeviceState> savedStates = new HashMap<>();

	// Background processor services
	private TaskExecutorService taskService = new TaskExecutorService(2);
	private PersistenceManager persistenceManager = null;
	private TimeActionProcessor timeActionProcessor = null;
	private EventProcessingService eventProcessingService = null;
	private DeviceLoggingService deviceLoggingService = null;
	private DeviceLoader deviceLoader = null;

	// Background processor tasks
	private Task databaseTask;

	public static synchronized void createBackgroundProcessor() throws Exception
	{
		if (singleton != null)
			throw new Exception("Background Processor already created");
		singleton = new Processor();
	}

	public static Processor getInstance()
	{
		return (Processor) NodeController.getInstance();
	}

	private Processor() throws Exception
	{
		Application.LOGGER.info("Starting Node Discovery Service");
		NodeDiscovererService.startDiscovering(this);
		Device.registerNodeManger(this);

		persistenceManager = PersistenceManager.loadPersistanceManager();
		TimeActionProcessor.createTimeActionProcessor(this);
		timeActionProcessor = TimeActionProcessor.getTimeActionProcessor();
		deviceLoader = DeviceLoader.createNewDeviceLoader();
		eventProcessingService = EventProcessingService.startEventProcessingService(this);

		Application.LOGGER.info("Starting Device Logging Service");
		deviceLoggingService = DeviceLoggingService.start(this);
		
		Application.LOGGER.info("Scheduling Database Task");
		Long interval = Long.parseLong(PropertyManger.loadProperty(PropertyKeys.DATABASE_POLL_FREQUENCY, "30"));
		databaseTask = taskService.scheduleTask(() ->
		{
			try
			{
				save();
			}
			catch (Throwable e)
			{
				Application.LOGGER.severe(e.getMessage());
			}
		}, 1L, interval, TimeUnit.MINUTES);
	}

	@Override
	public void run()
	{
		try
		{
			processorRunning.set(true);

			while (!shutdownProcessor.get())
			{
				DeviceState result = processingQueue.take();

				if (!shutdownProcessor.get() && result != null)
					processAction(result);
			}
		}
		catch (Exception e)
		{
			Application.LOGGER.info(e.getMessage());
		}
		
		processorRunning.set(false);
	}

	public TaskExecutorService getTaskExecutorService()
	{
		return taskService;
	}

	public TimeActionProcessor getTimeActionProcessor()
	{
		return timeActionProcessor;
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
		Application.LOGGER.info("Registered Node: " + node);
	}

	public InetAddress lookupNodeAddress(String node)
	{
		return nodeMap.get(node);
	}

	@Override
	public void update(DeviceState state)
	{
		cachedStates.put(state.getName(), state);
		eventProcessingService.update(state);
		deviceLoggingService.log(state);
	}

	@Override
	public boolean scheduleAction(DeviceState state)
	{
		return processingQueue.add(state);
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
	public synchronized String createNewDevice(DeviceConfig config, boolean isRemoteDevice)
	{
		// TODO enable logging
		if(!isRemoteDevice)
		{
			String name = super.createNewDevice(config, false);
			Device device = lookupDevice(name);
			if(device != null)
				device.execute(savedStates.remove(name));
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
					device.execute(savedStates.remove(device.getName()));
				
				deviceMap.put(device.getName(), device);
			}
			catch (Exception e)
			{
				Application.LOGGER.severe("Error Loading Device: " + config.getName() + ". Exception: " + e.getMessage());
			}
		}
	}

	public void load()
	{
		try
		{
			loadDeviceStatesIntoCache();

			Application.LOGGER.info("Loading Devices");
			deviceLoader.loadDevices(this);
			
			Application.LOGGER.info("Loading Events");
			eventProcessingService.createEvents(persistenceManager.loadEvents());

			Application.LOGGER.info("Scheduling Timers");
			timeActionProcessor.load(persistenceManager.loadTimedActions());
		}
		catch (Exception e)
		{
			Application.LOGGER.severe(e.getMessage());
		}
	}

	private void loadDeviceStatesIntoCache() throws SQLException, IOException
	{
		Application.LOGGER.info("Loading Device States");
		persistenceManager.loadDeviceStates().forEach((state) ->
		{
			savedStates.put(state.getName(), state);
		});
	}
	
	private void save() throws SQLException, IOException
	{
		persistenceManager.commitStates(getStates(true));
		persistenceManager.commitTimers(timeActionProcessor.retrieveAllTimedActions());
		persistenceManager.commitEvents(eventProcessingService.getAllEvents());
	}

	private void saveAndCloseAllDevices() throws SQLException, IOException
	{
		save();
		closeAllDevices();
	}

	public synchronized void shutdown()
	{
		if (!shutdownProcessor.get())
		{
			try
			{
				shutdownProcessor.set(true);
				
				if (processorRunning.get())
				{
					// Queue a No-Op to wakeup background processor
					processingQueue.add(Device.createNewDeviceState(DeviceType.UNKNOWN));
				}
				Application.LOGGER.info("Stopping all background tasks");
				taskService.cancelAllTasks();
				NodeDiscovererService.stopDiscovering();
				deviceLoggingService.stop();
				// Shutdown all devices and save their state
				Application.LOGGER.info("Saving Device States and shutting down");
				saveAndCloseAllDevices();
				persistenceManager.close();
			}
			catch (Throwable e)
			{
				Application.LOGGER.severe(e.getMessage());
			}
			finally
			{
				Application.LOGGER.info("System has been shutdown.");
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
				String deviceToConfig = (String) state.getParam(PROCESSOR_ACTIONS.DEVICE);

				// ProcessBuilder process = new ProcessBuilder("python", "../echo/fauxmo.py");
				// process.redirectOutput(ProcessBuilder.Redirect.INHERIT);
				// process.start();

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
							Application.LOGGER.info("Reloaded " + deviceToConfig);
						}
						else
							Application.LOGGER.info("Could not load: " + deviceToConfig);
					}

					break;
				case PROCESSOR_ACTIONS.RELOAD_DEVICE_ALL:
					saveAndCloseAllDevices();
					loadDeviceStatesIntoCache();
					deviceLoader.loadDevices(this);
					Application.LOGGER.info("Reloaded Devices");
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
					if (device != null)
					{
						device.execute(state);
					}
					else
					{
						Application.LOGGER.severe("Can't find device for : " + deviceToApplyState);
					}
					break;
				}
			}
		}
		catch (Exception e)
		{
			if (e instanceof InterruptedException)
				throw e;
			Application.LOGGER.severe(e.getMessage());
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
