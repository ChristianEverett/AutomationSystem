package com.pi.infrastructure;

import java.sql.*;


public class MySQLHandler
{
	private Connection connection = null;
	private Statement statement = null;
	private String DATABASE;
	
	public MySQLHandler(String username, String password) throws Exception
	{
		// This will load the MySQL driver, each DB has its own driver
		Class.forName("com.mysql.jdbc.Driver");
		// Setup the connection with the DB
		connection = DriverManager.getConnection("jdbc:mysql://localhost/?user=" + username + "&password=" + password);
	}
	
	public void loadDatabase(final String DATABASE) throws Exception
	{
		if(connection == null)
			throw new Exception("Null Connection");
		
		ResultSet resultSet = connection.getMetaData().getCatalogs();
		statement = connection.createStatement();
		
		boolean dbExists = false;
		
		while (resultSet.next()) 
		{
		    if(resultSet.getString(1).equalsIgnoreCase(DATABASE))
		    	dbExists = true;
		}
		
		if(dbExists == false)
		{
			statement.executeUpdate("CREATE DATABASE " + DATABASE);
		}
		
		connection.setCatalog(DATABASE);
		this.DATABASE = DATABASE;
		statement = connection.createStatement();
	}
	
	public void createTable(String table, String ... columns) throws SQLException
	{
		String query = "CREATE TABLE " + table + "(";
		
		for(int i = 0; i < columns.length; i+=2)
		{
			query += columns[i] + " " + columns[i+1] + ", ";
		}
		
		query = query.substring(0, query.length() - 2);
		query += ")";
		
		statement.executeUpdate(query);
	}
	
	public boolean tableExists(String table) throws Exception
	{
		ResultSet result = connection.getMetaData().getTables(null, null, table.toLowerCase(), null);
		
		if(result.next() != false)
		{
			result.close();
			return true;
		}
		
		result.close();
		return false;
	}
	/**
	 * obj.SELECT("*", [table name], "thing = otherThing")
	 * @param statement
	 * @param columns
	 * @param table
	 * @param where
	 * @return
	 * @throws SQLException
	 */
	public ResultSet SELECT(Statement statement, String columns, String table, String where) throws SQLException
	{	
		String line = "SELECT " + columns + " FROM " + table;
		
		if(where != null)
			line += " WHERE " + where;
		
		return statement.executeQuery(line);
	}
	/**
	 * obj.INSERT([table name], thing, thing, thing)
	 * @param table
	 * @param values
	 * @throws SQLException
	 */
	public void INSERT(String table, String... values) throws SQLException
	{
		String line = "";
		
		for (int i = 0; i < values.length; i++)
		{
			line += (values[i] + ", ");
		}
		
		line = "INSERT INTO " + table + " VALUES (" + line.substring(0, line.length() - 2) + ")";
		
		statement.executeUpdate(line);
	}
	
	public int INSERT_OBJECT(String table, String objectColunmName, Object object) throws SQLException
	{
		String line = "INSERT INTO " + table + "(name, " + objectColunmName + ") VALUES (?, ?)";
		PreparedStatement pstmt = connection.prepareStatement(line);
		
	    pstmt.setString(1, object.getClass().getName());
	    pstmt.setObject(2, object);
	    pstmt.executeUpdate();
		ResultSet result = pstmt.getGeneratedKeys();
		
		int key = 0;
		
		if(result.next())
			key = result.getInt(1);
		result.close();
		pstmt.close();
		
		return key;
	}
	
	public void UPDATE_OBJECT(String table, String objectColunmName, Object object, String where) throws SQLException
	{
		String line = "UPDATE " + table + " SET " + objectColunmName + " = ?";
		
		if(where != null)
			line += " WHERE " + where;
		
		PreparedStatement pstmt = connection.prepareStatement(line);
	    pstmt.setObject(1, object);
	    pstmt.executeUpdate();
	    pstmt.close();
	}
	
	/**
	 * obj.UPDATE([table name], [column name], [new value], "thing = otherThing")
	 * @param table
	 * @param column
	 * @param value
	 * @param where
	 * @throws SQLException
	 */
	public void UPDATE(String table, String column, String value, String where) throws SQLException
	{
		String line = "UPDATE " + table + " SET " + column + " = " + value;
		
		if(where != null)
			line += " WHERE " + where;

		statement.executeUpdate(line);
	}
	
	public void DELETE(String table, String where) throws SQLException
	{
		String line = "DELETE FROM " + table;
		
		if(where != null)
			line += (" WHERE " + where);
		
		statement.executeUpdate(line);
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
	
	public void close() throws SQLException
	{
		connection.close();
	}

	@Override
	protected void finalize() throws Throwable
	{
		this.close();
		super.finalize();
	}
}
