/**
 * 
 */
package com.pi.backgroundprocessor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

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
class PersistenceManager
{	
	private MySQLHandler dbHandler = null;
	
	private Statement timerStatement;
	private Statement stateStatement;
	
	private TimerRepository timerRepository = RepositoryContainer.getRepositorycontainer().buildTimerRepository();
	private StateRepository stateRepository = RepositoryContainer.getRepositorycontainer().buildStateRepository();
	
	public PersistenceManager() throws Exception
	{
		dbHandler = new MySQLHandler();
		dbHandler.loadDatabase(TABLES.DATABASE);
		
		if(dbHandler.tableExists(TABLES.USERS_TABLE) == false)
		{
			Application.LOGGER.info("Users Table not found");
			dbHandler.createTable(TABLES.USERS_TABLE, "username", "char(25)", "password", "char(25)");
			//dbHandler.INSERT(TABLES.USERS_TABLE, "admin", "01433128");
		}
		if(dbHandler.tableExists(TABLES.STATES_TABLE) == false)
		{
			Application.LOGGER.info("State Table not found");
			dbHandler.createTable(TABLES.STATES_TABLE, STATE_TABLE_COLUMNS.COMMAND, "char(25)", STATE_TABLE_COLUMNS.DATA, "char(25)");
		}
		if(dbHandler.tableExists(TABLES.TIMER_TABLE) == false)
		{
			Application.LOGGER.info("Timers Table not found");
			dbHandler.createTable(TABLES.TIMER_TABLE, TIMER_TABLE_COLUMNS.ID, "BIGINT", TIMER_TABLE_COLUMNS.TIME, "char(7)", 
					TIMER_TABLE_COLUMNS.EVALUATED, "boolean", TIMER_TABLE_COLUMNS.COMMAND, "char(25)", TIMER_TABLE_COLUMNS.DATA, "char(25)");
		}
		
		timerStatement = dbHandler.createStatement();
		stateStatement = dbHandler.createStatement();
		
		loadStatesTable();
		loadTimersTable();
	}
	
	/**
	 * Populate CrudRepository from States table
	 */
	private void loadStatesTable() throws SQLException
	{
		ResultSet result = dbHandler.SELECT(stateStatement, "*", TABLES.STATES_TABLE, null);
		
		while(result.next())
		{
			Action action = new Action(result.getString(STATE_TABLE_COLUMNS.COMMAND), result.getString(STATE_TABLE_COLUMNS.DATA));
			
			stateRepository.save(action);
		}
		
		result.close();
	}
	
	/**
	 * Populate CrudRepository from Timers table
	 */
	private void loadTimersTable() throws SQLException
	{
		ResultSet result = dbHandler.SELECT(timerStatement, TIMER_TABLE_COLUMNS.COMMAND + ", "
				+ TIMER_TABLE_COLUMNS.DATA + ", " + TIMER_TABLE_COLUMNS.TIME, TABLES.TIMER_TABLE, null);
		
		while(result.next())
		{
			String time = result.getString(TIMER_TABLE_COLUMNS.TIME);
			boolean evaluated = false;
			String command = result.getString(TIMER_TABLE_COLUMNS.COMMAND);
			String data = result.getString(TIMER_TABLE_COLUMNS.DATA);
			
			Timer timer = new Timer(time, evaluated, command, data);
			
			timerRepository.save(timer);
		}
		
		result.close();
	}
	
	public Iterator<Action> getAllStates()
	{
		return stateRepository.findAll().iterator();
	}
	
	public Iterator<Timer> getAllTimers()
	{
		return timerRepository.findAll().iterator();
	}
	
	public void saveState(Action action)
	{
		stateRepository.save(action);
	}
	
	public void saveTimer(Timer timer)
	{
		timerRepository.save(timer);
	}
	
	public void commit() throws SQLException
	{
		Iterator<Timer> timers = timerRepository.findAll().iterator();
		Iterator<Action> states = stateRepository.findAll().iterator();

		ResultSet timerResult = dbHandler.SELECT(timerStatement, TIMER_TABLE_COLUMNS.ID, TABLES.TIMER_TABLE, null);
		ResultSet stateResult = dbHandler.SELECT(stateStatement, STATE_TABLE_COLUMNS.COMMAND, TABLES.TIMER_TABLE, null);
		
		boolean found = false;
		
		while(timers.hasNext())
		{
			Timer timer = timers.next();
			
			while(timerResult.next())
			{
				if(timer.getId() == timerResult.getLong(TIMER_TABLE_COLUMNS.ID))
				{
					found = true;
					break;
				}
			}
			
			if(!found)
				dbHandler.INSERT(TABLES.TIMER_TABLE, Long.toString(timer.getId()), toMySqlString(timer.getTime()), 
						"false", toMySqlString(timer.getCommand()), toMySqlString(timer.getData()));
		}
		
		found = false;
		
		while(states.hasNext())
		{
			Action action = states.next();
			
			while(stateResult.next())
			{
				if(action.getCommand().equalsIgnoreCase(stateResult.getString(STATE_TABLE_COLUMNS.COMMAND)))
				{
					dbHandler.UPDATE(TABLES.STATES_TABLE, STATE_TABLE_COLUMNS.DATA, toMySqlString(action.getData()), null);
					found = true;
				}
			}
			
			if(!found)
				dbHandler.INSERT(TABLES.STATES_TABLE, toMySqlString(action.getCommand()), toMySqlString(action.getData()));
		}
		
		
		timerResult.close();
		stateResult.close();
	}
	
	private String toMySqlString(String string)
	{
		return ("'" + string + "'");
	}
	
	public void close() throws SQLException
	{
		commit();
		dbHandler.close();
	}
	
	private interface TABLES
	{
		public static final String DATABASE = "pidb";
		public static final String USERS_TABLE = "users";
		public static final String STATES_TABLE = "states";
		public static final String TIMER_TABLE = "timers";
	}
	
	private interface STATE_TABLE_COLUMNS
	{
		public static final String COMMAND = "command";
		public static final String DATA= "data";
	}
	
	private interface TIMER_TABLE_COLUMNS
	{
		public static final String ID = "id";
		public static final String TIME = "time";
		public static final String EVALUATED = "evaluated";
		public static final String COMMAND = "command";
		public static final String DATA= "data";
	}
}
