/**
 * 
 */
package com.pi.backgroundprocessor;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.data.repository.CrudRepository;

import com.pi.Application;
import com.pi.repository.Action;
import com.pi.repository.RepositoryContainer;
import com.pi.repository.StateRepository;
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
	ExecutorService executorService = Executors.newFixedThreadPool(10);
	ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
	
	private GPIOController gpioController = null;
	private PersistenceManager persistenceManager;

	private Processor() 
	{
		try
		{
			persistenceManager = new PersistenceManager();		
			processLastKnowStates();
			
			Application.LOGGER.info("Loading GPIO Controller");
			gpioController = GPIOController.loadGPIOController();
		    
			exec.scheduleAtFixedRate(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						timerEvaluator();
					}
					catch (SQLException e)
					{
						Application.LOGGER.severe(e.getMessage());
					}
				}
			}, 1, 2, TimeUnit.SECONDS);
			
			exec.scheduleAtFixedRate(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						persistenceManager.commit();
					}
					catch (SQLException e)
					{
						Application.LOGGER.severe(e.getMessage());
					}
				}
			}, 1, 1, TimeUnit.HOURS);
			
			exec.scheduleAtFixedRate(new Runnable()
			{
				@Override
				public void run()
				{
					NetworkConnectionPoller.checkConnection();
				}
			}, 1, 10, TimeUnit.MINUTES);
			
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
				
				Thread.sleep(5);
			}
		} 
		catch (IllegalStateException e)
		{
			Application.LOGGER.severe(e.getMessage());
			shutdownBackgroundProcessor();
		}
		catch (UnsupportedOperationException | InterruptedException e)
		{
			Application.LOGGER.severe(e.getMessage());
		}
	}
	
	private void processAction(Action result) throws UnsupportedOperationException
	{
		try
		{
			if(result != null)
			{
				String command = result.getCommand();
				String state = result.getData();
				
				switch (command)
				{
				case ACTIONS.LED1:			
				case ACTIONS.LED2:
					
					int RED, GREEN, BLUE;
					
					if(state == "")
					{
						RED = GREEN = BLUE = 0;
					}
					else
					{
						String[] splited = state.split("\\s+");
						
						RED = Integer.parseInt(splited[0]);
						GREEN = Integer.parseInt(splited[1]);
						BLUE = Integer.parseInt(splited[2]);
					}
		
					gpioController.pulseWidthModulate(command, RED, GREEN, BLUE);
					persistenceManager.saveState(result);
					break;
					
				case ACTIONS.SWITCH_ONE:			
				case ACTIONS.SWITCH_TWO:				
				case ACTIONS.SWITCH_THREE:	
				case ACTIONS.SWITCH_FOUR:
					gpioController.setPin(command, !Boolean.parseBoolean(state));
					persistenceManager.saveState(result);
					break;
					
				case ACTIONS.RUN_ECHO_SERVER:
					ProcessBuilder process = new ProcessBuilder("python", "../echo/fauxmo.py");
					//process.redirectOutput(ProcessBuilder.Redirect.INHERIT);
					process.start();
					break;
					
				case ACTIONS.SHUTDOWN:
					shutdownBackgroundProcessor();
					break;

				default:
					Application.LOGGER.severe("Action not Supported");
					break;
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

		String time = hour + ":" + (minute < 10 ? "0" + minute : minute);
		
		//set hour for day divide when reseting timers during 11:00pm
		hour = (hour == 0) ? 24 : hour;
		
		Iterator<Timer> timers = persistenceManager.getAllTimers();
		
		while(timers.hasNext())
		{
			Timer timer = timers.next();
			
			if(timer.getTime().equalsIgnoreCase(time) && !timer.getEvaluated())
			{
				scheduleAction(new Action(timer.getCommand(), timer.getData()));
				timer.setEvaluated(true);
				persistenceManager.saveTimer(timer);
				Application.LOGGER.info("Timer Triggered: " + timer.getCommand() + " Time: " + timer.getTime());
			}
			else if(timer.getTime() == ((hour - 1) + ":" + (minute < 10 ? "0" + minute : minute)))
			{
				timer.setEvaluated(false);
				persistenceManager.saveTimer(timer);
			}
		}
	}
	
	private void processLastKnowStates()
	{
		Iterator<Action> states = persistenceManager.getAllStates();
		
		while(states.hasNext())
		{
			scheduleAction(states.next());
		}
	}
	
	public boolean scheduleAction(Action action)
	{
		return processingQueue.add(action);
	}
	
	public void shutdownBackgroundProcessor()
	{
		try
		{
			Application.LOGGER.severe("System Shutting down!");
			//rt.exec("java -jar EmailModule.jar christian.everett1@gmail.com pi 'The pi controller has shutdown'");
			persistenceManager.close();
		}
		catch (SQLException e)
		{
			Application.LOGGER.severe(e.getMessage());
		}
		finally 
		{
			//Shutdown all relays
			gpioController.close();
			
			System.exit(1);
		}    
	}
	
	public static Processor getBackgroundProcessor()
	{
		if(singleton == null)
			singleton = new Processor();
		
		return singleton;
	}
	
	//supported actions
	private interface ACTIONS
	{
		public static final String RUN_ECHO_SERVER = "run_echo_server";
		public static final String LED1 = "led1";
		public static final String LED2 = "led2";
		public static final String SWITCH_ONE = "switch1";
		public static final String SWITCH_TWO = "switch2";
		public static final String SWITCH_THREE = "switch3";
		public static final String SWITCH_FOUR = "switch4";
		public static final String SHUTDOWN = "shutdown";
	}
}
