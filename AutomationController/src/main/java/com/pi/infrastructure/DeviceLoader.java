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
	
	public void populateDeviceMap(Map<String, Device> deviceMap)
	{
		NodeList nodeList = xmlDocument.getElementsByTagName("device");
		
		for (int x = 0; x < nodeList.getLength(); x++)
		{
			Node node = nodeList.item(x);
			
			if(node.getNodeType() == Node.ELEMENT_NODE)
			{
				Element element = (Element) node;
				
				String type = element.getAttribute("device-type");
				String name = element.getAttribute("device-name");
				
				try
				{
					Device device = Device.createNewDevice(name, type, element);
					
					if(device != null)
						deviceMap.put(name, device);
				}
				catch (Exception e)
				{
					Application.LOGGER.severe("Error Loading Device: " + name + ". Exception: " + e.getMessage());
				}
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
