/**
 * 
 */
package com.pi.infrastructure;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import com.pi.Application;


/**
 * @author Christian Everett
 *
 */
public class PropertyManger
{
	private static Properties properties = new Properties();
	private static OutputStream output = null;
	private static InputStream input = null;
	
	private static final File PROPERTIES_FILE = new File("config.properties");
	
	static
	{
		try
		{
			//Create file if it does't exist
			boolean created = PROPERTIES_FILE.createNewFile();
			
			input = new FileInputStream(PROPERTIES_FILE);
			properties.load(input);
			
			input.close();
			
			output = new FileOutputStream(PROPERTIES_FILE, true);
		}
		catch (Exception e)
		{
			Application.LOGGER.severe(e.getMessage());
		}
	}
	
	public static String loadProperty(String key)
	{
		return properties.getProperty(key);
	}
	
	public static void storeProperty(String key, String value) throws IOException
	{
		properties.setProperty(key, value);
		properties.store(output, null);
	}
	
	public static interface PropertyKeys
	{
		public static final String LOGFILE = "log.file";
		public static final String DBUSER = "database.username";
		public static final String DBPASS = "database.password";
		public static final String ADMIN_EMAIL = "admin.email";
		public static final String DEVICE_CONFIG = "device.config";
	}
}