/**
 * 
 */
package com.pi.backgroundprocessor;

import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.pi.Application;
import com.pi.infrastructure.UniqueMultiMap;
import com.pi.model.TimedAction;

/**
 * @author Christian Everett
 *
 */
public class TimeActionProcessor
{
	private static TimeActionProcessor singlton = null;
	private int timerEvaluatorTask = -1;
	private Processor bgp = Processor.getBackgroundProcessor();
	//Map hours to groups of TimedActions
	private UniqueMultiMap<Integer, TimedAction> hourToTimersMap = new UniqueMultiMap<>();

	public static TimeActionProcessor getTimeActionProcessor()
	{
		if(singlton == null)
			singlton = new TimeActionProcessor();
		return singlton;
	}
	
	private TimeActionProcessor()
	{
		timerEvaluatorTask = bgp.getThreadExecutorService().scheduleTask(() ->
		{
			try
			{
				timerEvaluator();
			}
			catch (Throwable e)
			{
				Application.LOGGER.severe(e.getMessage());
			}
		}, 1, 2, TimeUnit.SECONDS);
	}
	
	private void timerEvaluator()
	{//TODO improve
		int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
		int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		int minute = Calendar.getInstance().get(Calendar.MINUTE);
		
		//set hour for day divide when reseting timers during 11:00pm
		int previousHour = ((hour == 0) ? 24 : hour) - 1;
		
		Collection<TimedAction> timers = hourToTimersMap.get(hour);

		for(TimedAction timedAction : timers)
		{
			if(timedAction.getMinute() == minute && !timedAction.getEvaluated())
			{
				bgp.scheduleAction(timedAction.getAction());
				timedAction.setEvaluated(true);
				Application.LOGGER.info("Timer Triggered: " + timedAction.getAction().getDevice() + " Time: " + timedAction.getTime());
			}
			else if(timedAction.getHour() == previousHour)
			{
				timedAction.setEvaluated(false);
			}
		}
	}
	
	public synchronized void load(TimedAction timedAction)
	{
		hourToTimersMap.put(timedAction.getHour(), timedAction);
	}
	
	public synchronized void load(Collection<TimedAction> timedActions)
	{
		for(TimedAction timedAction : timedActions)
		{
			hourToTimersMap.put(timedAction.getHour(), timedAction);
		}
	}
	
	public TimedAction getTimedActionByID(Integer id)
	{
		return hourToTimersMap.getByHash(id);	
	}
	
	public Collection<Entry<Integer, TimedAction>> retrieveAllTimedActions()
	{
		return hourToTimersMap.getAllValues();
	}
	
	public boolean updateTimedActionByID(Integer id, TimedAction timedAction)
	{
		return hourToTimersMap.updateByHash(id, timedAction);
	}
	
	public TimedAction delete(Integer id)
	{
		return hourToTimersMap.delete(id);
	}
	
	public void deleteAll()
	{
		hourToTimersMap.clear();
	}
	
	public void shutdown()
	{
		bgp.getThreadExecutorService().cancelTask(timerEvaluatorTask);
		hourToTimersMap.clear();
		singlton = null;
	}
}
