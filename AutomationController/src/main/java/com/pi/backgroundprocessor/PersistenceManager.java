/**
 * 
 */
package com.pi.backgroundprocessor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.pi.Application;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.MySQLHandler;
import com.pi.model.Action;
import com.pi.model.TimedAction;
import static com.pi.infrastructure.PropertyManger.loadProperty;
import static com.pi.infrastructure.PropertyManger.PropertyKeys;

/**
 * @author Christian Everett
 *
 */
class PersistenceManager
{	
	private MySQLHandler dbHandler = null;
	
	private Statement timerStatement;
	private Statement stateStatement;
	
	private static PersistenceManager singleton = null;
	
	public static PersistenceManager loadPersistanceManager() throws Exception
	{
		if(singleton == null)
		{
			String dbUser = loadProperty(PropertyKeys.DBUSER);
			String dbPass = loadProperty(PropertyKeys.DBPASS);
			singleton = new PersistenceManager(dbUser, dbPass);
		}
		
		return singleton;
	}
	
	private PersistenceManager(String username, String password) throws Exception
	{
		dbHandler = new MySQLHandler("root", "root");
		dbHandler.loadDatabase(TABLES.DATABASE);
		
		if(!dbHandler.tableExists(TABLES.USERS_TABLE))
		{
			Application.LOGGER.info("Users Table not found");
			dbHandler.createTable(TABLES.USERS_TABLE, "username", "char(25)", "password", "char(25)");
			//dbHandler.INSERT(TABLES.USERS_TABLE, "admin", "01433128");
		}
		if(!dbHandler.tableExists(TABLES.STATES_TABLE))
		{
			Application.LOGGER.info("State Table not found");
			dbHandler.createTable(TABLES.STATES_TABLE, STATE_TABLE_COLUMNS.DEVICE, "char(25)", STATE_TABLE_COLUMNS.DATA, "char(100)");
		}
		if(!dbHandler.tableExists(TABLES.TIMER_TABLE))
		{
			Application.LOGGER.info("Timers Table not found");
			dbHandler.createTable(TABLES.TIMER_TABLE, TIMER_TABLE_COLUMNS.ID, "BIGINT", TIMER_TABLE_COLUMNS.TIME, "char(7)", 
					TIMER_TABLE_COLUMNS.EVALUATED, "boolean", TIMER_TABLE_COLUMNS.COMMAND, "char(25)", TIMER_TABLE_COLUMNS.DATA, "char(100)");
		}
		
		timerStatement = dbHandler.createStatement();
		stateStatement = dbHandler.createStatement();
	}
	
	/**
	 * Populate CrudRepository from States table
	 */
//	public Map<String, Device> loadSavedStates()
//	{
//		Map<String, Device> deviceMap = new HashMap<>();
//		
//		try
//		{
//			ResultSet result = dbHandler.SELECT(stateStatement, "*", TABLES.STATES_TABLE, null);
//			
//			while(result.next())
//			{
//				Action action = new Action(result.getString(STATE_TABLE_COLUMNS.DEVICE), result.getString(STATE_TABLE_COLUMNS.DATA));
//			
//				Device device = deviceMap.get(action.getDevice());
//				
//				if(device != null)
//					device.performAction(action);
//			}
//			
//			result.close();
//		}
//		catch (SQLException e)
//		{
//			Application.LOGGER.severe(e.getMessage());
//		}
//		
//		return deviceMap;
//	}
	
	public void loadSavedStates(HashMap<String, Device> deviceMap) throws SQLException
	{
		ResultSet result = dbHandler.SELECT(stateStatement, "*", TABLES.STATES_TABLE, null);
		
		while(result.next())
		{
			Action action = new Action(result.getString(STATE_TABLE_COLUMNS.DEVICE), result.getString(STATE_TABLE_COLUMNS.DATA));
		
			Device device = deviceMap.get(action.getDevice());
			
			if(device != null)
				device.performAction(action);
		}
		
		result.close();
	}
	
	/**
	 * Populate CrudRepository from Timers table
	 */
	public Collection<TimedAction> loadTimers()
	{
		Collection<TimedAction> timerList = new ArrayList<>();
		
		try
		{
			ResultSet result = dbHandler.SELECT(timerStatement, TIMER_TABLE_COLUMNS.COMMAND + ", "
					+ TIMER_TABLE_COLUMNS.DATA + ", " + TIMER_TABLE_COLUMNS.TIME, TABLES.TIMER_TABLE, null);
			
			while(result.next())
			{
				String time = result.getString(TIMER_TABLE_COLUMNS.TIME);
				boolean evaluated = false;
				String command = result.getString(TIMER_TABLE_COLUMNS.COMMAND);
				String data = result.getString(TIMER_TABLE_COLUMNS.DATA);
				
				TimedAction timer = new TimedAction(time, evaluated, new Action(command, data));
				
				timerList.add(timer);
			}
			
			result.close();
		}
		catch (SQLException e)
		{
			Application.LOGGER.severe(e.getMessage());
		}
		
		return timerList;
	}
	
	public void commit(Map<String, Device> deviceMap) throws SQLException
	{//TODO
//		Set<Long> timersIDs = timerRepository.getAllKeys();
//		Set<String> devices = deviceMap.keySet();
//
//		dbHandler.clearTable(TABLES.TIMER_TABLE);
//		dbHandler.clearTable(TABLES.STATES_TABLE);
//
//		for(Long id : timersIDs)
//		{	
//			TimedAction timer = timerRepository.get(id);
//			
//			dbHandler.INSERT(TABLES.TIMER_TABLE, Long.toString(id), toMySqlString(timer.getTime()), 
//					"false", toMySqlString(timer.getAction().getDevice()), toMySqlString(timer.getAction().getData()));
//		}
//		
//		for(String deviceName : devices)
//		{
//			Device device = deviceMap.get(deviceName);
//
//			if(device != null)
//				dbHandler.INSERT(TABLES.STATES_TABLE, toMySqlString(deviceName), toMySqlString(device.getState().getData()));
//		}
	}
	
	private String toMySqlString(String string)
	{
		return ("'" + string + "'");
	}
	
	public void close() throws SQLException
	{
		dbHandler.close();
	}
	
	private interface TABLES//TODO
	{
		public static final String DATABASE = "pidb";
		public static final String USERS_TABLE = "users";
		public static final String STATES_TABLE = "states";
		public static final String TIMER_TABLE = "timers";
	}
	
	private interface STATE_TABLE_COLUMNS
	{
		public static final String DEVICE = "device";
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
