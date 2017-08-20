/**
 * 
 */
package com.pi.services;

import static com.pi.infrastructure.util.PropertyManger.PropertyKeys;
import static com.pi.infrastructure.util.PropertyManger.loadProperty;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.pi.Application;
import com.pi.SystemLogger;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType;
import com.pi.infrastructure.DeviceType.DeviceTypeMap;
import com.pi.infrastructure.BaseNodeController;
import com.pi.infrastructure.RemoteDeviceProxy;
import com.pi.infrastructure.Device.DeviceConfig;
import com.pi.infrastructure.RemoteDeviceProxy.RemoteDeviceConfig;

/**
 * @author Christian Everett
 *
 */

@Service
public class DeviceLoadingService
{
	public static final String DEVICE_NAME = "device-name";
	public static final String DEVICE_TYPE = "device-type";
	public static final String DEVICE = "device";
	
	private Document xmlDocument;
	private DocumentBuilder dBuilder;
	private File xmlFile;

	private DeviceLoadingService() throws IOException, ParserConfigurationException, SAXException
	{
		xmlFile = new File(loadProperty(PropertyKeys.DEVICE_CONFIG));
		boolean create = xmlFile.createNewFile();

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dBuilder = dbFactory.newDocumentBuilder();
	}

	private void refreshXMLDocument() throws SAXException, IOException
	{
		try
		{
			xmlDocument = dBuilder.parse(xmlFile);

			xmlDocument.getDocumentElement().normalize();
		}
		catch (Exception e)
		{
			throw new RuntimeException("Could not refresh XML document: " + e.getMessage());
		}
	}

	public DeviceConfig loadDevice(String name)
	{
		List<Element> elements = getDeviceElements();
		
		try
		{
			for(Element element : elements)
			{
				String xmlName = element.getAttribute(DeviceLoadingService.DEVICE_NAME);
				
				if(name.equals(xmlName))
				{
					DeviceConfig config = load(element);
					
					if(config instanceof RemoteDeviceConfig)
						return loadRemoteDevice(element, config);
					else
						return config;
				}
			}
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe("Error loading Device: " + name + ". Exception: " + e.getMessage());
		}
		
		throw new RuntimeException("Could not load: " + name);
	}
	
	public List<DeviceConfig> loadDevices()
	{
		List<Element> elements = getDeviceElements();
		List<DeviceConfig> configs = new LinkedList<>();
		
		for(Element element : elements)
		{
			try
			{
				DeviceConfig config = load(element);
				
				if(config instanceof RemoteDeviceConfig)
					configs.add(loadRemoteDevice(element, config));
				else
					configs.add(config);
			}
			catch (Exception e)
			{
				SystemLogger.getLogger().severe("Error loading Device. Exception: " + e.getMessage());
			}
		}
		
		return configs;
	}
	
	private List<Element> getDeviceElements()
	{
		try
		{
			refreshXMLDocument();
			//<devices>
			Element root = xmlDocument.getDocumentElement();
			//<device>
			NodeList node = root.getChildNodes();
			List<Element> elements = new LinkedList<>();
			
			for(int x = 0; x < node.getLength(); x++)
			{
				if(node.item(x).getNodeType() == Node.ELEMENT_NODE)
				{
					Element element = (Element) node.item(x);
					elements.add(element);
				}
			}
			
			return elements;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private DeviceConfig load(Element element) throws Exception, JAXBException
	{
		String type = element.getAttribute(DeviceLoadingService.DEVICE_TYPE);
		
		Class<?> configType = DeviceTypeMap.getConfig(type);
		if (configType == null)
			throw new Exception("Device Config not found: " + type);
		
		JAXBContext jaxbContext = JAXBContext.newInstance(configType);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		DeviceConfig config = (DeviceConfig) jaxbUnmarshaller.unmarshal(element);
		return config;
	}
	
	private DeviceConfig loadRemoteDevice(Element element, DeviceConfig config) throws JAXBException, Exception
	{
		Element deviceElement = (Element) element.getElementsByTagName(DeviceLoadingService.DEVICE).item(0);
		String name = deviceElement.getAttribute(DeviceLoadingService.DEVICE_NAME);
		String type = deviceElement.getAttribute(DeviceLoadingService.DEVICE_TYPE);
		String nodeID = element.getElementsByTagName("nodeID").item(0).getTextContent();
		
		((RemoteDeviceConfig) config).setConfig(load(deviceElement));
		((RemoteDeviceConfig) config).setType(type);
		
		return config;
	}
	

}
