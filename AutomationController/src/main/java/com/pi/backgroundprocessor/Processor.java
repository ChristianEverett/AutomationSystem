/**
 * 
 */
package com.pi.backgroundprocessor;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.pi.Application;
import com.pi.backgroundprocessor.TaskExecutorService.Task;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceLoader;
import com.pi.infrastructure.util.PropertyManger;
import com.pi.infrastructure.util.PropertyManger.PropertyKeys;
import com.pi.model.DeviceState;

/**
 * @author Christian Everett
 *
 */

public class Processor extends Thread
{
	private static Processor singleton = null;
	private AtomicBoolean processorRunning = new AtomicBoolean(false);

	// Background processor data structures
	private LinkedBlockingQueue<DeviceState> processingQueue = new LinkedBlockingQueue<>(50_000);
	private HashMap<String, InetAddress> nodeMap = new HashMap<>();

	// Background processor services
	private TaskExecutorService taskService = new TaskExecutorService(2);
	private PersistenceManager persistenceManager = null;
	private TimeActionProcessor timeActionProcessor = null;
	private DeviceLoader deviceLoader = null;

	// Background processor tasks
	private Task databaseTask;

	public static void createBackgroundProcessor() throws Exception
	{
		if (singleton != null)
			throw new Exception("Background Processor already created");
		singleton = new Processor();

		singleton.scheduleTasksAndTimers();
	}

	public static synchronized Processor getBackgroundProcessor()
	{
		if (singleton == null)
			throw new NullPointerException("Background Processor has not been created");
		return singleton;
	}

	private Processor() throws Exception
	{
		persistenceManager = PersistenceManager.loadPersistanceManager();
		TimeActionProcessor.createTimeActionProcessor(this);
		timeActionProcessor = TimeActionProcessor.getTimeActionProcessor();
		deviceLoader = DeviceLoader.createNewDeviceLoader();
	}

	@Override
	public void run()
	{
		this.setPriority(Thread.MAX_PRIORITY);

		try
		{
			processorRunning.set(true);

			while (true)
			{
				DeviceState result = processingQueue.take();

				if (result != null)
					processAction(result);
			}
		}
		catch (Exception e)
		{
			processorRunning.set(false);
			Application.LOGGER.info(e.getMessage());
		}
	}

	private synchronized void processAction(DeviceState state) throws Exception
	{
		try
		{
			if (state != null)
			{
				String deviceToApplyState = state.getName();
				Device device = Device.lookupDevice(deviceToApplyState);
				String deviceToConfig = (String)state.getParam(PROCESSOR_ACTIONS.DEVICE);

				switch (deviceToApplyState)
				{
				case PROCESSOR_ACTIONS.RUN_ECHO_SERVER:
					ProcessBuilder process = new ProcessBuilder("python", "../echo/fauxmo.py");
					// process.redirectOutput(ProcessBuilder.Redirect.INHERIT);
					process.start();
					break;

				case PROCESSOR_ACTIONS.RELOAD_DEVICE:
					
					device = Device.lookupDevice(deviceToConfig);

					if (device != null)
					{
						DeviceState savedState = device.getState();
						Device.close(deviceToConfig);
						deviceLoader.loadDevice(deviceToConfig);
						device = Device.lookupDevice(deviceToConfig);
						
						if(device != null)
						{
							device.performAction(savedState);
							Application.LOGGER.info("Reloaded " + deviceToConfig);
						}
						else 
							Application.LOGGER.info("Could not load: " + deviceToConfig);
					}
	
					break;
				case PROCESSOR_ACTIONS.RELOAD_DEVICE_ALL:
					saveAndCloseAllDevices();
					loadDevicesAndDeviceStates();
					Application.LOGGER.info("Reloaded Devices");
					break;
				case PROCESSOR_ACTIONS.LOAD_DEVICE:
					deviceLoader.loadDevice(deviceToConfig);
					break;

				case PROCESSOR_ACTIONS.CLOSE_DEVICE:
					Device.close(deviceToConfig);
					break;

				case PROCESSOR_ACTIONS.SAVE_DATA:
					save();
					break;

				case PROCESSOR_ACTIONS.SHUTDOWN:
					shutdownBackgroundProcessor();
					break;

				default:
					if (device != null)
					{
						device.performAction(state);
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
			if(e instanceof InterruptedException)
				throw e;
			Application.LOGGER.severe(e.getMessage());
		}
	}

	public synchronized void registerNode(String node, InetAddress address)
	{
		nodeMap.put(node, address);
	}
	
	public synchronized boolean scheduleAction(DeviceState state)
	{
		return processingQueue.add(state);
	}

	public TaskExecutorService getTaskExecutorService()
	{
		return taskService;
	}

	public TimeActionProcessor getTimeActionProcessor()
	{
		return timeActionProcessor;
	}

	public void loadDevicesAndDeviceStates() throws ParserConfigurationException, SAXException, IOException, SQLException
	{
		Application.LOGGER.info("Loading Devices");
		deviceLoader.loadDevices();

		Application.LOGGER.info("Loading Device States");
		List<DeviceState> savedStates = persistenceManager.loadDeviceStates();
		savedStates.forEach((action) ->
		{
			Device device = Device.lookupDevice(action.getName());
			if (device != null)
				device.performAction(action);
		});
	}

	private void scheduleTasksAndTimers()
	{
		try
		{
			Application.LOGGER.info("Scheduling Timers");
			timeActionProcessor.load(persistenceManager.loadTimedActions());

			Application.LOGGER.info("Scheduling Database Task");
			Long interval = Long.parseLong(PropertyManger.loadProperty(PropertyKeys.DATABASE_POLL_FREQUENCY, "2"));

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
			}, 1L, interval, TimeUnit.HOURS);
			
			Application.LOGGER.info("Starting Node Discovery Service");
			//NodeDiscovererService.startDiscovering();
		}
		catch (Exception e)
		{
			Application.LOGGER.severe(e.getMessage());
		}
	}

	/**
	 * @throws SQLException
	 */
	private void save() throws SQLException
	{
		persistenceManager.commitStates(Device.getStates());
		persistenceManager.commitTimers(timeActionProcessor.retrieveAllTimedActions());
	}

	/**
	 * @throws SQLException
	 */
	private void saveAndCloseAllDevices() throws SQLException
	{
		save();
		Device.closeAll();
	}

	public void shutdownBackgroundProcessor()
	{
		try
		{
			if (processorRunning.get())
				this.interrupt();
			Application.LOGGER.info("Stopping all background tasks");
			taskService.cancelAllTasks();
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

	private interface PROCESSOR_ACTIONS
	{
		public static final String RELOAD_DEVICE_ALL = "reload_device_all";
		public static final String RELOAD_DEVICE = "reload_device";
		public static final String LOAD_DEVICE = "load_device";
		public static final String CLOSE_DEVICE = "close_device";
		public static final String SAVE_DATA = "save_data";
		public static final String RUN_ECHO_SERVER = "run_echo_server";
		public static final String SHUTDOWN = "shutdown";
		
		public static final String DEVICE = "device";
	}
}
