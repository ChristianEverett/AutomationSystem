/**
 * 
 */
package com.pi.services;

import static com.pi.infrastructure.util.PropertyManger.PropertyKeys;
import static com.pi.infrastructure.util.PropertyManger.loadProperty;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import com.pi.Application;
import com.pi.SystemLogger;
import com.pi.infrastructure.MySQLHandler;
import com.pi.infrastructure.util.PropertyManger;
import com.pi.model.DatabaseElement;
import com.pi.model.DeviceState;
import com.pi.model.DeviceStateRecord;
import com.pi.model.EventHandler;

/**
 * @author Christian Everett
 *
 */
public class PersistenceManager
{
	public static final String CREATE_STATE_TABLE = "createStateTable";
	public static final String CREATE_EVENT_TABLE = "createEventTable";
	public static final String CREATE_STATE_LOG_TABLE = "createStateLog";
	
	public static final String READ_STATE_TABLE = "readStateTable";
	public static final String INSERT_STATE_TABLE = "insertSaveState";
	public static final String UPDATE_STATE_TABLE = "updateSaveState";

	public static final String READ_EVENT_TABLE = "readEventTable";
	public static final String INSERT_EVENT = "writeEvent";
	public static final String UPDATE_EVENT = "updateEvent";
	public static final String DELETE_EVENT = "deleteEvent";

	public static final String READ_FROM_DEVICE_LOG = "readDeviceLog";
	public static final String WRITE_TO_DEVICE_LOG = "writeDeviceLog";

	private MySQLHandler dbHandler = null;
	private static PersistenceManager singleton = null;

	public static PersistenceManager loadPersistanceManager() throws Exception
	{
		if (singleton == null)
		{
			String dbUser = loadProperty(PropertyKeys.DBUSER);
			String dbPass = loadProperty(PropertyKeys.DBPASS);
			singleton = new PersistenceManager(dbUser, dbPass);
		}

		return singleton;
	}

	private PersistenceManager(String username, String password) throws Exception
	{
		String dbuser = PropertyManger.loadProperty(PropertyKeys.DBUSER);
		String dbpass = PropertyManger.loadProperty(PropertyKeys.DBPASS);
		String propertiesFile = PropertyManger.loadProperty(PropertyKeys.SQL_PROPERTIES);
		dbHandler = new MySQLHandler(dbuser, dbpass, propertiesFile);
		dbHandler.loadDatabase(TABLES.DATABASE);

		if (!dbHandler.tableExists(TABLES.USERS_TABLE))
		{
			SystemLogger.getLogger().info("Creating: " + TABLES.USERS_TABLE);
			dbHandler.createTable(TABLES.USERS_TABLE, "username", "char(25)", "password", "char(25)");
			// dbHandler.INSERT(TABLES.USERS_TABLE, "admin", "01433128");
		}
		if (!dbHandler.tableExists(TABLES.DEVICE_STATE_TABLE))
		{
			SystemLogger.getLogger().info("Creating: " + TABLES.DEVICE_STATE_TABLE);
			dbHandler.createTable(CREATE_STATE_TABLE);
		}
		if (!dbHandler.tableExists(TABLES.EVENT_TABLE))
		{
			SystemLogger.getLogger().info("Creating: " + TABLES.EVENT_TABLE);
			dbHandler.createTable(CREATE_EVENT_TABLE);
		}
		if (!dbHandler.tableExists(TABLES.STATE_LOG_TABLE))
		{
			SystemLogger.getLogger().info("Creating: " + TABLES.STATE_LOG_TABLE);
			dbHandler.createTable(CREATE_STATE_LOG_TABLE);
		}
	}

	public void commitToDeviceLog(List<DeviceStateRecord> states) throws SQLException
	{
		try
		{
			PreparedStatement statement = dbHandler.createSQLStatment(WRITE_TO_DEVICE_LOG);

			for (DeviceStateRecord record : states)
			{
				statement.setInt(1, record.getDatabaseIdentification());
				statement.setObject(2, record);
				statement.setTimestamp(3, new java.sql.Timestamp(record.getDate().getTime()));
				statement.addBatch();
			}

			statement.executeBatch();
			statement.close();
			dbHandler.commit();
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
			dbHandler.rollback();
		}
	}

	public List<DeviceStateRecord> getRecords(String start, String end) throws Exception
	{
		List<DeviceStateRecord> list = new LinkedList<>();
		PreparedStatement statement = dbHandler.createSQLStatment(READ_FROM_DEVICE_LOG);
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy a hh:mm:ss");
		statement.setTimestamp(1, new java.sql.Timestamp(sdf.parse(start).getTime()));
		statement.setTimestamp(2, new java.sql.Timestamp(sdf.parse(end).getTime()));
		ResultSet result = statement.executeQuery();

		while (result.next())
		{
			byte[] bytes = result.getBytes(3);

			try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes)))
			{
				DeviceStateRecord element = (DeviceStateRecord) input.readObject();
				list.add(element);
			}
			catch (Exception e)
			{
				result.close();
				statement.close();
				throw e;
			}
		}

		result.close();
		statement.close();
		return list;
	}

	private HashSet<Integer> getDatabaseMap(String key)
	{
		List<? extends DatabaseElement> list = read(key);
		HashSet<Integer> set = new HashSet<>(list.size());

		for (DatabaseElement element : list)
		{
			set.add(element.getDatabaseIdentification());
		}

		return set;
	}
	
	@SuppressWarnings("unchecked")
	public List<DeviceState> readAllDeviceStates()
	{
		return (List<DeviceState>)read(READ_STATE_TABLE);
	}
	
	public synchronized void saveStates(List<DeviceState> states)
	{	
		for(DeviceState state : states)
		{
			create(INSERT_STATE_TABLE, state, false);
		}
		
		try
		{
			dbHandler.commit();
		}
		catch (SQLException e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
			try
			{
				dbHandler.rollback();
			}
			catch (SQLException e1)
			{
			}
		}
	}
	
	public void createEvent(EventHandler element) 
	{
		create(INSERT_EVENT, element, true);
	}
	
	@SuppressWarnings("unchecked")
	public List<EventHandler> readAllEvent()
	{
		return (List<EventHandler>)read(READ_EVENT_TABLE);
	}

	public void updateEvent(EventHandler element, int oldElementId) 
	{
		update(UPDATE_EVENT, element, oldElementId, true);
	}
	
	public void deleteEvent(EventHandler event) 
	{
		delete(DELETE_EVENT, event, true);
	}

	private synchronized void create(String createQuery, DatabaseElement element, boolean autoCommit)
	{
		PreparedStatement statement = null;
		
		try
		{
			statement = dbHandler.createSQLStatment(createQuery);
			statement.setInt(1, element.getDatabaseIdentification());
			statement.setString(2, element.getName());
			statement.setObject(3, element);
			statement.setObject(4, element);
		}
		catch (IOException | SQLException e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
		}
		finally
		{
			try
			{
				dbHandler.applyAndCloseStatement(statement, autoCommit);
			}
			catch (SQLException e)
			{
				SystemLogger.getLogger().severe(e.getMessage());
			}
		}
	}
	
	private List<? extends DatabaseElement> read(String readQuery)
	{
		List<DatabaseElement> elements = new LinkedList<>();
		PreparedStatement statement = null;
		
		try
		{
			statement = dbHandler.createSQLStatment(readQuery);

			ResultSet result = statement.executeQuery();
			
			while (result.next())
			{
				byte[] bytes = result.getBytes(3);
				
				try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes)))
				{
					DatabaseElement element = (DatabaseElement) input.readObject();
					elements.add(element);
				}
				catch (Exception e)
				{
					throw e;
				}
			}
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
		}
		finally
		{
			dbHandler.closeStatement(statement);
		}
		
		return elements;
	}
	
	private synchronized void update(String updateQuery, DatabaseElement element, int oldElementId, boolean autoCommit)
	{
		PreparedStatement statement = null;
		
		try
		{
			statement = dbHandler.createSQLStatment(updateQuery);
			statement.setObject(1, element);
			statement.setInt(3, oldElementId);
		}
		catch (IOException | SQLException e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
		}
		finally
		{
			try
			{
				dbHandler.applyAndCloseStatement(statement, autoCommit);
			}
			catch (SQLException e)
			{
				SystemLogger.getLogger().severe(e.getMessage());
			}
		}
	}
	
	private synchronized void delete(String deleteQuery, DatabaseElement element, boolean autoCommit)
	{
		PreparedStatement statement = null;
		
		try
		{
			statement = dbHandler.createSQLStatment(deleteQuery);
			statement.setInt(1, element.getDatabaseIdentification());
		}
		catch (IOException | SQLException e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
		}
		finally
		{
			try
			{
				dbHandler.applyAndCloseStatement(statement, autoCommit);
			}
			catch (SQLException e)
			{
				SystemLogger.getLogger().severe(e.getMessage());
			}
		}
	}

	public synchronized void close() throws SQLException
	{
		dbHandler.close();
	}

	private interface TABLES
	{
		public static final String DATABASE = "pidb";
		public static final String USERS_TABLE = "users";
		public static final String DEVICE_STATE_TABLE = "device_state";
		public static final String EVENT_TABLE = "event";
		public static final String STATE_LOG_TABLE = "state_log";
	}
}
