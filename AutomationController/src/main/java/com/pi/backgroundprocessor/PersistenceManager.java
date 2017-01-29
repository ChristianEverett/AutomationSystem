/**
 * 
 */
package com.pi.backgroundprocessor;

import static com.pi.infrastructure.util.PropertyManger.PropertyKeys;
import static com.pi.infrastructure.util.PropertyManger.loadProperty;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.pi.Application;
import com.pi.infrastructure.DatabaseElement;
import com.pi.infrastructure.MySQLHandler;
import com.pi.model.DeviceState;
import com.pi.model.Event;
import com.pi.model.TimedAction;

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
	
	private Lock lock = new ReentrantLock();
	
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
			Application.LOGGER.info("Creating: " + TABLES.USERS_TABLE);
			dbHandler.createTable(TABLES.USERS_TABLE, "username", "char(25)", "password", "char(25)");
			//dbHandler.INSERT(TABLES.USERS_TABLE, "admin", "01433128");
		}
		if(!dbHandler.tableExists(TABLES.TIMED_ACTION_TABLE))
		{
			Application.LOGGER.info("Creating: " + TABLES.TIMED_ACTION_TABLE);
			dbHandler.createTable(TABLES.TIMED_ACTION_TABLE, 
					TIMED_ACTION_TABLE_COLUMNS.ID, "INT AUTO_INCREMENT",
					TIMED_ACTION_TABLE_COLUMNS.NAME, "varchar(128)",
					TIMED_ACTION_TABLE_COLUMNS.DATA, "BLOB",
					"primary key", "(id)");
		}
		if(!dbHandler.tableExists(TABLES.DEVICE_STATE_TABLE))
		{
			Application.LOGGER.info("Creating: " + TABLES.DEVICE_STATE_TABLE);
			dbHandler.createTable(TABLES.DEVICE_STATE_TABLE, 
					DEVICE_STATE_TABLE_COLUMNS.ID, "INT AUTO_INCREMENT",
					DEVICE_STATE_TABLE_COLUMNS.NAME, "varchar(128)",
					DEVICE_STATE_TABLE_COLUMNS.DATA, "BLOB",
					"primary key", "(id)");
		}
		if(!dbHandler.tableExists(TABLES.EVENT_TABLE))
		{
			Application.LOGGER.info("Creating: " + TABLES.EVENT_TABLE);
			dbHandler.createTable(TABLES.EVENT_TABLE, 
					EVENT_TABLE_COLUMNS.ID, "INT AUTO_INCREMENT",
					EVENT_TABLE_COLUMNS.NAME, "varchar(128)",
					EVENT_TABLE_COLUMNS.DATA, "BLOB",
					"primary key", "(id)");
		}
		
		timerStatement = dbHandler.createStatement();
		stateStatement = dbHandler.createStatement();
	}
	
	@SuppressWarnings("unchecked")
	public List<DeviceState> loadDeviceStates() throws SQLException
	{
		return (List<DeviceState>) load(TABLES.DEVICE_STATE_TABLE);
	}
	
	@SuppressWarnings("unchecked")
	public List<TimedAction> loadTimedActions() throws SQLException
	{
		return (List<TimedAction>) load(TABLES.TIMED_ACTION_TABLE);
	}
	
	@SuppressWarnings("unchecked")
	public List<Event> loadEvents() throws SQLException
	{
		return (List<Event>) load(TABLES.EVENT_TABLE);
	}
	
	private List<? extends DatabaseElement> load(String table) throws SQLException
	{
		List<DatabaseElement> elements = new ArrayList<>();
		ResultSet result = dbHandler.SELECT(stateStatement, "*", table, null);
		
		while(result.next())
		{
			byte[] bytes = result.getBytes(3);
			try(ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes)))
			{
				DatabaseElement element = (DatabaseElement) input.readObject();
				elements.add(element);
			}
			catch(Exception e)
			{
				Application.LOGGER.severe(e.getMessage());
			}
		}
		
		result.close();
		return elements;
	}
	
	public void commitStates(List<DeviceState> states) throws SQLException
	{	
		commit(states, TABLES.DEVICE_STATE_TABLE, DEVICE_STATE_TABLE_COLUMNS.DATA);
	}
	
	public void commitTimers(List<TimedAction> timers) throws SQLException
	{		
		commit(timers, TABLES.TIMED_ACTION_TABLE, TIMED_ACTION_TABLE_COLUMNS.DATA);
	}
	
	public void commitEvents(List<Event> events) throws SQLException
	{
		commit(events, TABLES.EVENT_TABLE, EVENT_TABLE_COLUMNS.DATA);
	}
	
	private void commit(List<? extends DatabaseElement> list, String table, String dataColunm) throws SQLException
	{
		lock.lock();
		for(DatabaseElement object : list)
		{
			if(object.isInDatabase())
				dbHandler.UPDATE_OBJECT(table, dataColunm, object, object.getDatabaseID().toString());
			else
				dbHandler.INSERT_OBJECT(table, dataColunm, object);
		}
		lock.unlock();
	}
	
	private String toMySqlString(String string)
	{
		return ("'" + string + "'");
	}
	
	public void close() throws SQLException
	{
		dbHandler.close();
	}
	
	private interface TABLES
	{
		public static final String DATABASE = "pidb";
		public static final String USERS_TABLE = "users";
		public static final String TIMED_ACTION_TABLE = "timed_action";
		public static final String DEVICE_STATE_TABLE = "device_state";
		public static final String EVENT_TABLE = "event_table";
	}
	
	private interface DEVICE_STATE_TABLE_COLUMNS
	{
		public static final String ID = "id";
		public static final String NAME = "name";
		public static final String DATA = "data";
	}
	
	private interface TIMED_ACTION_TABLE_COLUMNS
	{
		public static final String ID = "id";
		public static final String NAME = "name";
		public static final String DATA = "data";
	}
	
	private interface EVENT_TABLE_COLUMNS
	{
		public static final String ID = "id";
		public static final String NAME = "name";
		public static final String DATA = "data";
	}
}
