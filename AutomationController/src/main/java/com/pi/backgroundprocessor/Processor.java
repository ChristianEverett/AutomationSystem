/**
 * 
 */
package com.pi.backgroundprocessor;

import java.io.IOException;
import java.sql.SQLException;
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
import com.pi.infrastructure.DeviceType;
import com.pi.model.Action;


/**
 * @author Christian Everett
 *
 */

public class Processor extends Thread
{
	private static Processor singleton = null;
	private AtomicBoolean processorRunning = new AtomicBoolean(false);
	
	//Background processor data structures
	private LinkedBlockingQueue<Action> processingQueue = new LinkedBlockingQueue<>(100_000);
	
	//Background processor services
	private TaskExecutorService taskService = new TaskExecutorService(2);
	private PersistenceManager persistenceManager = null;
	private TimeActionProcessor timeActionProcessor = null;
	
	//Background processor tasks
	private Task databaseTask;

	public static void createBackgroundProcessor() throws Exception
	{
		if(singleton != null)
			throw new Exception("Background Processor already created");
		singleton = new Processor();
		
		singleton.loadDevices();
		singleton.scheduleTasksAndTimers();
	}
	
	public static synchronized Processor getBackgroundProcessor()
	{
		if(singleton == null)
			throw new NullPointerException("Background Processor null");
		return singleton;
	}
	
	private Processor() throws Exception 
	{
		persistenceManager = PersistenceManager.loadPersistanceManager();
		TimeActionProcessor.createTimeActionProcessor(this);
		timeActionProcessor = TimeActionProcessor.getTimeActionProcessor();
	}	

	@Override
	public void run()
	{
		try
		{
			processorRunning.set(true);
			
			while(true)
			{
				Action result = processingQueue.take();
				
				if(result != null)
					processAction(result);
			}
		} 
		catch (InterruptedException e)
		{
			processorRunning.set(false);
			Application.LOGGER.info(e.getMessage());
		}
	}
	
	private void processAction(Action action) throws InterruptedException
	{
		try
		{
			if(action != null)
			{
				String name = action.getDevice();
				Device device =  Device.lookupDevice(name);
				
				if(device == null)
				{
					switch (name)
					{	
					case DeviceType.RUN_ECHO_SERVER:
						ProcessBuilder process = new ProcessBuilder("python", "../echo/fauxmo.py");
						//process.redirectOutput(ProcessBuilder.Redirect.INHERIT);
						process.start();
						break;
						
					case DeviceType.RELOAD_DEVICES:
						saveAndCloseAllDevices();
						loadDevices();
						Application.LOGGER.info("Reloaded Devices");
						break;
						
					case DeviceType.SHUTDOWN:
						shutdownBackgroundProcessor();
						break;

					default:
						Application.LOGGER.severe("Action not Supported");
						break;
					}
				}
				else 
				{
					device.performAction(action);
				}
			}	
		}
		catch(IOException | SQLException | ParserConfigurationException | SAXException e)
		{
			Application.LOGGER.severe(e.getMessage());
		}
	}

	public synchronized boolean scheduleAction(Action action)
	{
		return processingQueue.add(action);
	}
	
	public TaskExecutorService getThreadExecutorService()
	{
		return taskService;
	}
	
	public TimeActionProcessor getTimeActionProcessor()
	{
		return timeActionProcessor;
	}
	
	private void loadDevices() throws ParserConfigurationException, SAXException, IOException, SQLException
	{
		Application.LOGGER.info("Loading Devices");
		DeviceLoader deviceLoader = DeviceLoader.createNewDeviceLoader();
		deviceLoader.loadDevices();
		
		Application.LOGGER.info("Loading Device States");
		List<Action> savedStates = persistenceManager.loadSavedStates();
		savedStates.forEach((action) -> 
		{
			Device device = Device.lookupDevice(action.getDevice());
			if(device != null)
				device.performAction(action);
		});		
	}
	
	private void scheduleTasksAndTimers()
	{
		Application.LOGGER.info("Scheduling Timers");		
		timeActionProcessor.load(persistenceManager.loadTimers());
		
		Application.LOGGER.info("Scheduling Tasks");		
		databaseTask = taskService.scheduleTask(() ->
		{
			try
			{
				persistenceManager.commit(Device.getStates());
				persistenceManager.commit(timeActionProcessor.retrieveAllTimedActions());
			}
			catch (Throwable e)
			{
				Application.LOGGER.severe(e.getMessage());
			}
		}, 1L, 2L, TimeUnit.HOURS);	
	}
	
	/**
	 * @throws SQLException
	 */
	private void saveAndCloseAllDevices() throws SQLException
	{
		persistenceManager.commit(Device.getStates());
		Device.closeAll();
	}
	
	public void shutdownBackgroundProcessor()
	{	
		try
		{
			if(processorRunning.get())
				this.interrupt();
			Application.LOGGER.info("Stopping all background tasks");
			taskService.cancelAllTasks();
			//Shutdown all devices and save their state
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
