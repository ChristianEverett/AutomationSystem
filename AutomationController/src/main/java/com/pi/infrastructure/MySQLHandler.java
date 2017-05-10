package com.pi.infrastructure;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;

public class MySQLHandler
{
	private Connection connection = null;
	private Statement statement = null;
	private Properties properties = new Properties();
	private String DATABASE;
	private File sqlQueryFile;

	public MySQLHandler(String username, String password, String queryFile) throws Exception
	{
		sqlQueryFile = new File(queryFile);

		if (!sqlQueryFile.exists())
			throw new IOException(queryFile + " does not exist");

		FileInputStream input = new FileInputStream(sqlQueryFile);
		properties.load(input);
		input.close();

		// This will load the MySQL driver, each DB has its own driver
		Class.forName("com.mysql.jdbc.Driver");
		// Setup the connection with the DB
		connection = DriverManager.getConnection("jdbc:mysql://localhost/?user=" + username + "&password=" + password);
		connection.setAutoCommit(false);
	}

	public void loadDatabase(final String DATABASE) throws Exception
	{
		if (connection == null)
			throw new Exception("Null Connection");

		ResultSet resultSet = connection.getMetaData().getCatalogs();
		statement = connection.createStatement();

		boolean dbExists = false;

		while (resultSet.next())
		{
			if (resultSet.getString(1).equalsIgnoreCase(DATABASE))
				dbExists = true;
		}

		if (dbExists == false)
		{
			statement.executeUpdate("CREATE DATABASE " + DATABASE);
		}

		connection.setCatalog(DATABASE);
		this.DATABASE = DATABASE;
		statement = connection.createStatement();
	}

	public void createTable(String table, String... columns) throws SQLException
	{
		String query = "CREATE TABLE " + table + "(";

		for (int i = 0; i < columns.length; i += 2)
		{
			query += columns[i] + " " + columns[i + 1] + ", ";
		}

		query = query.substring(0, query.length() - 2);
		query += ")";

		statement.executeUpdate(query);
	}

	public boolean tableExists(String table) throws Exception
	{
		ResultSet result = connection.getMetaData().getTables(null, null, table.toLowerCase(), null);

		if (result.next() != false)
		{
			result.close();
			return true;
		}

		result.close();
		return false;
	}

	public void clearTable(String table) throws SQLException
	{
		statement.execute("TRUNCATE TABLE " + table);
	}

	public Statement createStatement() throws SQLException
	{
		return connection.createStatement();
	}

	public void commit() throws SQLException
	{
		connection.commit();
	}

	public void rollback() throws SQLException
	{
		connection.rollback();
	}

	public void close() throws SQLException
	{
		connection.close();
	}

	public int getKey(PreparedStatement pstmt) throws SQLException
	{
		ResultSet result = pstmt.getGeneratedKeys();

		int key = 0;

		if (result.next())
			key = result.getInt(1);
		result.close();

		return key;
	}

	public PreparedStatement createSQLStatment(String key) throws IOException, SQLException
	{
		String statement = properties.getProperty(key);

		if (statement.isEmpty())
			throw new IOException("No statement for: " + key);

		return connection.prepareStatement(statement);
	}

	public void closeStatment(PreparedStatement statement)
	{
		try
		{
			if (statement != null)
				statement.close();
		}
		catch (Exception e)
		{
		}
	}
	
	@Override
	protected void finalize() throws Throwable
	{
		this.close();
		super.finalize();
	}
}
