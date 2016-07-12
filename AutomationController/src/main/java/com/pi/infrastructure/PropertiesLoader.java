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


/**
 * @author Christian Everett
 *
 */
public class PropertiesLoader
{
	private Properties properties = new Properties();
	private OutputStream output = null;
	private InputStream input = null;
	
	private static final File PROPERTIES_FILE = new File("deviceConfig.properites");
	
	private static PropertiesLoader singleton = null;
	
	public static PropertiesLoader getInstance() throws IOException
	{
		if(singleton == null)
			singleton = new PropertiesLoader();
		
		return singleton;
	}
	
	private PropertiesLoader() throws IOException
	{
		//Create file if it does't exist
		PROPERTIES_FILE.createNewFile();
		
		input = new FileInputStream(PROPERTIES_FILE);
		properties.load(input);
		
		input.close();
		
		output = new FileOutputStream(PROPERTIES_FILE);
	}
	
	public String loadProperty(String key)
	{
		return properties.getProperty(key);
	}
	
	public void storeProperty(String key, String value) throws IOException
	{
		properties.setProperty(key, value);
		properties.store(output, null);
	}
}
