/**
 * 
 */
package com.pi.infrastructure;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.pi.Application;
import static com.pi.infrastructure.PropertyManger.loadProperty;
import static com.pi.infrastructure.PropertyManger.PropertyKeys;

/**
 * @author Christian Everett
 *
 */
public class DeviceLoader
{
	public static final String DEVICE_NAME = "device-name";
	public static final String DEVICE_TYPE = "device-type";
	public static final String DEVICE = "device";
	
	private static DeviceLoader singleton = null;
	private Document xmlDocument = null;
	
	private DeviceLoader() throws IOException, ParserConfigurationException, SAXException
	{
		File xmlFile = new File(loadProperty(PropertyKeys.DEVICE_CONFIG));
		boolean create = xmlFile.createNewFile();
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		xmlDocument = dBuilder.parse(xmlFile);
		
		xmlDocument.getDocumentElement().normalize();
	}
	
	public void loadDevices()
	{
		NodeList deviceList = xmlDocument.getElementsByTagName(DEVICE);
		
		for (int x = 0; x < deviceList.getLength(); x++)
		{
			Node node = deviceList.item(x);
			
			if(node.getNodeType() == Node.ELEMENT_NODE)
			{
				Element element = (Element) node;
							
				Device.createNewDevice(element);
			}
		}
	}
	
	public static DeviceLoader createNewDeviceLoader() throws IOException, ParserConfigurationException, SAXException
	{
		if(singleton == null)
			singleton = new DeviceLoader();
		
		return singleton;
	}
}
