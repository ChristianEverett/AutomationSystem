/**
 * 
 */
package com.pi.backgroundprocessor;

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
import com.pi.model.Event;

/**
 * @author Christian Everett
 *
 */
public class PersistenceManager
{
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
			dbHandler.createTable(TABLES.DEVICE_STATE_TABLE, DEVICE_STATE_TABLE_COLUMNS.ID, "INT AUTO_INCREMENT", DEVICE_STATE_TABLE_COLUMNS.NAME, "varchar(128)",
					DEVICE_STATE_TABLE_COLUMNS.DATA, "BLOB", "primary key", "(id)");
		}
		if (!dbHandler.tableExists(TABLES.EVENT_TABLE))
		{
			SystemLogger.getLogger().info("Creating: " + TABLES.EVENT_TABLE);
			dbHandler.createTable(TABLES.EVENT_TABLE, EVENT_TABLE_COLUMNS.ID, "INT AUTO_INCREMENT", EVENT_TABLE_COLUMNS.NAME, "varchar(128)", EVENT_TABLE_COLUMNS.DATA, "BLOB", "primary key",
					"(id)");
		}
		if (!dbHandler.tableExists(TABLES.STATE_LOG_TABLE))
		{
			SystemLogger.getLogger().info("Creating: " + TABLES.STATE_LOG_TABLE);
			dbHandler.createTable(TABLES.STATE_LOG_TABLE, STATE_LOG_TABLE_COLUMNS.ID, "INT AUTO_INCREMENT", STATE_LOG_TABLE_COLUMNS.NAME, "varchar(128)", STATE_LOG_TABLE_COLUMNS.DATA,
					"BLOB", STATE_LOG_TABLE_COLUMNS.TIME_STAMP, "DATETIME", "primary key", "(id)");
		}
	}

	@SuppressWarnings("unchecked")
	public List<DeviceState> loadDeviceStates() throws SQLException, IOException
	{
		return (List<DeviceState>) load(READ_STATE_TABLE);
	}

	@SuppressWarnings("unchecked")
	public List<Event> loadEvents() throws SQLException, IOException
	{
		return (List<Event>) load(READ_EVENT_TABLE);
	}

	private List<? extends DatabaseElement> load(String key) throws SQLException, IOException
	{
		List<DatabaseElement> elements = new ArrayList<>();

		synchronized (this)
		{
			PreparedStatement statement = dbHandler.createSQLStatment(key);
			ResultSet result = statement.executeQuery();

			while (result.next())
			{
				int id = result.getInt(1);
				byte[] bytes = result.getBytes(3);
				try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes)))
				{
					DatabaseElement element = (DatabaseElement) input.readObject();
					element.setDatabaseID(id);
					elements.add(element);
				}
				catch (Exception e)
				{
					SystemLogger.getLogger().severe(e.getMessage());
				}
			}

			result.close();
			statement.close();
		}
		return elements;
	}

	public void commitStates(List<DeviceState> states) throws SQLException, IOException
	{
		commit(states, READ_STATE_TABLE, INSERT_STATE_TABLE, UPDATE_STATE_TABLE);
	}

	public void commitEvents(List<Event> events) throws SQLException, IOException
	{
		commit(events, READ_EVENT_TABLE, INSERT_EVENT, UPDATE_EVENT);
	}
	
	private void commit(List<? extends DatabaseElement> list, String readQuery, String insertQuery, String updateQuery) throws SQLException, IOException
	{
		synchronized (this)
		{
			HashSet<Object> set = getDatabaseMap(readQuery);
			String databaseString = "";

			try
			{
				for (DatabaseElement object : list)
				{
					databaseString = object.getDatabaseIdentificationForQuery();
					
					if (set.contains(object.getDatabaseIdentification()))
					{
						PreparedStatement statement = dbHandler.createSQLStatment(updateQuery);
						statement.setObject(1, object);
						statement.setString(2, databaseString);
						statement.executeUpdate();
						statement.close();
					}
					else
					{
						PreparedStatement statement = dbHandler.createSQLStatment(insertQuery);
						statement.setString(1, databaseString);
						statement.setObject(2, object);
						statement.executeUpdate();
						int id = dbHandler.getKey(statement);
						object.setDatabaseID(id);
						statement.close();
					}
				}
				
				dbHandler.commit();
			}
			catch (Exception e)
			{
				SystemLogger.getLogger().severe(e.getMessage() + " object: " + databaseString);
				dbHandler.rollback();
			}
		}
	}

	private HashSet<Object> getDatabaseMap(String key) throws SQLException, IOException
	{
		List<? extends DatabaseElement> list = load(key);
		HashSet<Object> set = new HashSet<>(list.size());

		for (DatabaseElement element : list)
		{
			set.add(element.getDatabaseIdentification());
		}

		return set;
	}

	public void commitToDeviceLog(List<DeviceStateRecord> states) throws SQLException
	{
		try
		{
			PreparedStatement statement = dbHandler.createSQLStatment(WRITE_TO_DEVICE_LOG);

			for (DeviceStateRecord record : states)
			{
				statement.setString(1, record.getDatabaseIdentificationForQuery());
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

	public void createEvent(Event element) 
	{
		create(INSERT_EVENT, element);
	}
	
	public void readAllEvent(String name)
	{
		read(READ_EVENT_TABLE);
	}

	public void updateEvent(Event element, Event oldElement) 
	{
		update(UPDATE_EVENT, element, oldElement);
	}
	
	public void deleteEvent(Event event) 
	{
		delete(DELETE_EVENT, event);
	}

	public void create(String createQuery, DatabaseElement element)
	{
		PreparedStatement statement = null;
		
		try
		{
			statement = dbHandler.createSQLStatment(createQuery);
			statement.setString(1, element.getDatabaseIdentificationForQuery());
			statement.setObject(2, element);
			dbHandler.commit();
		}
		catch (IOException | SQLException e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
		}
		finally
		{
			dbHandler.closeStatment(statement);
		}
	}
	
	public List<DatabaseElement> read(String readQuery)
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
			dbHandler.closeStatment(statement);
		}
		
		return elements;
	}
	
	public void update(String updateQuery, DatabaseElement element, DatabaseElement oldElement)
	{
		PreparedStatement statement = null;
		
		try
		{
			statement = dbHandler.createSQLStatment(updateQuery);
			statement.setObject(1, element);
			statement.setString(2, element.getDatabaseIdentificationForQuery());
			dbHandler.commit();
		}
		catch (IOException | SQLException e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
		}
		finally
		{
			dbHandler.closeStatment(statement);
		}
	}
	
	public void delete(String deleteQuery, DatabaseElement element)
	{
		PreparedStatement statement = null;
		
		try
		{
			statement = dbHandler.createSQLStatment(deleteQuery);
			statement.setString(1, element.getDatabaseIdentificationForQuery());
			dbHandler.commit();
		}
		catch (IOException | SQLException e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
		}
		finally
		{
			dbHandler.closeStatment(statement);
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

	private interface DEVICE_STATE_TABLE_COLUMNS
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

	private interface STATE_LOG_TABLE_COLUMNS
	{
		public static final String ID = "id";
		public static final String NAME = "name";
		public static final String DATA = "data";
		public static final String TIME_STAMP = "time_stamp";
	}
}
