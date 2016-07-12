/**
 * 
 */
package com.pi.infrastructure;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Christian Everett
 *
 */
public class DeviceLoader
{
	private static DeviceLoader singleton = null;
	private Document xmlDocument = null;
	
	private DeviceLoader() throws ParserConfigurationException, SAXException, IOException
	{
		File xmlFile = new File("./devices.xml");
		xmlFile.createNewFile();
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		xmlDocument = dBuilder.parse(xmlFile);
		
		xmlDocument.getDocumentElement().normalize();
	}
	
	public void populateDeviceMap(HashMap<String, Device> deviceMap)
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
				Device device = Device.CreateNewDevice(name, type, element);
				
				if(device != null)
					deviceMap.put(name, device);
			}
		}
	}
	
	public static DeviceLoader getInstance() throws ParserConfigurationException, SAXException, IOException
	{
		if(singleton == null)
			singleton = new DeviceLoader();
		
		return singleton;
	}
}
