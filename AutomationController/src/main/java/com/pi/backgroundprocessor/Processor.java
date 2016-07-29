/**
 * 
 */
package com.pi.backgroundprocessor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.pi.Application;
import com.pi.devices.Led;
import com.pi.devices.Outlet;
import com.pi.devices.Switch;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceLoader;
import com.pi.infrastructure.DeviceType;
import com.pi.infrastructure.PropertiesLoader;
import com.pi.repository.Action;
import com.pi.repository.Timer;
import com.pi.repository.TimerRepository;


/**
 * @author Christian Everett
 *
 */

public class Processor extends Thread
{
	private static Processor singleton = null;
	private Queue<Action> processingQueue = new ConcurrentLinkedQueue<>();
	private ExecutorService executorService = Executors.newFixedThreadPool(10);
	private ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
	private HashMap<String, Device> deviceMap = new HashMap<>();
	private PersistenceManager persistenceManager;

	private Processor() 
	{
		try
		{
			PropertiesLoader properties = PropertiesLoader.getInstance();
			DeviceLoader deviceLoader = DeviceLoader.getInstance();
			
			Application.LOGGER.info("Loading Devices");
			deviceLoader.populateDeviceMap(deviceMap);
			
			Application.LOGGER.info("Loading Device States");
			persistenceManager = PersistenceManager.loadPersistanceManager();
			
			for(int x = 0; x < persistenceManager.getNumberOfSavedStates(); x++)
			{
				scheduleAction(persistenceManager.getSavedState(x));
			}
			
			Application.LOGGER.info("Scheduling Tasks");
			exec.scheduleAtFixedRate(() ->
			{
				try
				{
					timerEvaluator();
				}
				catch (SQLException e)
				{
					Application.LOGGER.severe(e.getMessage());
				}
			}, 1, 2, TimeUnit.SECONDS);
			
			exec.scheduleAtFixedRate(() ->
			{
				try
				{
					persistenceManager.commit();
					Application.LOGGER.info("Timer/State Commit");
				}
				catch (SQLException e)
				{
					Application.LOGGER.severe(e.getMessage());
				}
			}, 1, 2, TimeUnit.HOURS);
			
			exec.scheduleAtFixedRate(() ->
			{
				NetworkConnectionPoller.checkConnection();
			}, 1, 20, TimeUnit.MINUTES);
			
			Runtime.getRuntime().addShutdownHook(new Thread()
	        {
	            @Override
	            public void run()
	            {
	            	shutdownBackgroundProcessor();
	            }
	        });
		}
		catch(Exception e)
		{
			Application.LOGGER.severe(e.getMessage());
			shutdownBackgroundProcessor();
		}
	}
	
	@Override
	public void run()
	{
		try
		{	    		
			while(true)
			{
				Action result = processingQueue.poll();
				
				if(result != null)
					processAction(result);
							
				if(!NetworkConnectionPoller.isConnected())
					throw new IllegalStateException("Lost Network Connection");
				
				Thread.sleep(4);
			}
		} 
		catch (IllegalStateException e)
		{
			Application.LOGGER.severe(e.getMessage());
		}
		catch (UnsupportedOperationException | InterruptedException e)
		{
			Application.LOGGER.severe(e.getMessage());
			Application.LOGGER.severe("Restarting Processor");
			this.run();
		}
		
		shutdownBackgroundProcessor();
	}
	
	private void processAction(Action action) throws UnsupportedOperationException
	{
		try
		{
			if(action != null)
			{
				String command = action.getDevice();
				Device device =  deviceMap.get(command);
				
				if(device == null)
				{
					switch (command)
					{	
					case DeviceType.RUN_ECHO_SERVER:
						ProcessBuilder process = new ProcessBuilder("python", "../echo/fauxmo.py");
						//process.redirectOutput(ProcessBuilder.Redirect.INHERIT);
						process.start();
						break;
						
					case DeviceType.RELOAD_DEVICES:
						deviceMap.forEach((k, v) -> v.close());
						deviceMap.clear();
						DeviceLoader.getInstance().populateDeviceMap(deviceMap);
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
		catch(Exception e)
		{
			throw new UnsupportedOperationException("Invalid Action");
		}
	}
	
	private void timerEvaluator() throws SQLException
	{
		int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
		int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		int minute = Calendar.getInstance().get(Calendar.MINUTE);
		
		//set hour for day divide when reseting timers during 11:00pm
		int previousHour = ((hour == 0) ? 24 : hour) - 1;
		
		TimerRepository timerRepository = TimerRepository.getInstance();
		
		for(Long id : timerRepository.getAllKeys())
		{
			Timer timer = timerRepository.get(id);
			
			if(timer.getHour() == hour && timer.getMinute() == minute && !timer.getEvaluated())
			{
				scheduleAction(new Action(timer.getAction().getDevice(), timer.getAction().getData()));
				timer.setEvaluated(true);
				timerRepository.add(timer);
				Application.LOGGER.info("Timer Triggered: " + timer.getAction().getDevice() + " Time: " + timer.getTime());
			}
			else if(timer.getHour() == previousHour)
			{
				timer.setEvaluated(false);
				timerRepository.add(timer);
			}
		}
	}
	
	public synchronized boolean scheduleAction(Action action)
	{
		return processingQueue.add(action);
	}
	
	public List<Action> getStates()
	{	
		List<Action> stateList = new ArrayList<>();
		
		for(Entry<String, Device> device : deviceMap.entrySet())
		{
			stateList.add(device.getValue().getState());
		}
		
		return stateList;
	}
	
	public Action getStateByDeviceName(String name)
	{
		return deviceMap.get(name).getState();
	}
	
	public void shutdownBackgroundProcessor()
	{
		try
		{
			//rt.exec("java -jar EmailModule.jar christian.everett1@gmail.com pi 'The pi controller has shutdown'");
			
			//Shutdown all devices
			Application.LOGGER.info("Shutting down all devices");
			deviceMap.forEach((k, v) -> v.close());
			
			persistenceManager.close();
		}
		catch (Exception e)
		{
			Application.LOGGER.severe(e.getMessage());
		}
		finally 
		{
			Application.LOGGER.severe("System has been shutdown.");
			System.exit(1);
		}    
	}
	
	public static Processor getBackgroundProcessor()
	{
		if(singleton == null)
			singleton = new Processor();
		
		return singleton;
	}
}
