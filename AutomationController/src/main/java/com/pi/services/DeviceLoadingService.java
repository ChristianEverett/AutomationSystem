/**
 * 
 */
package com.pi.services;

import static com.pi.infrastructure.util.PropertyManger.PropertyKeys;
import static com.pi.infrastructure.util.PropertyManger.loadProperty;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
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
{//TODO rework
	public static final String DEVICE_NAME = "device-name";
	public static final String DEVICE_TYPE = "device-type";
	public static final String DEVICE = "device";
	private Document xmlDocument = null;

	private static HashMap<String, Class<?>> registeredDeviceConfigs = new HashMap<>();

	static
	{
		registeredDeviceConfigs.putAll(DeviceType.registerAllDeviceConfigs());
	}

	private DeviceLoadingService() throws IOException, ParserConfigurationException, SAXException
	{
		File xmlFile = new File(loadProperty(PropertyKeys.DEVICE_CONFIG));
		boolean create = xmlFile.createNewFile();

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		xmlDocument = dBuilder.parse(xmlFile);

		xmlDocument.getDocumentElement().normalize();
	}

	public DeviceConfig loadDevice()
	{
		return null;
		
	}
	
	public List<DeviceConfig> loadDevices()
	{
		return null;
		
	}
	
	private Map<String, NodeList> getDeviceTags()
	{
		Map<String, NodeList> deviceTags = new HashMap<>();
		Set<String> tags = registeredDeviceConfigs.keySet();
		
		for(String tag : tags)
		{
			NodeList deviceList = xmlDocument.getElementsByTagName(tag);
			deviceTags.put(tag, deviceList);
		}
		
		return deviceTags;
	}
	
	public void loadDevices(BaseNodeController deviceFactory)
	{
		loadDevice(deviceFactory, null);
	}

	public void loadDevice(BaseNodeController deviceFactory, String name)
	{
		NodeList deviceList = xmlDocument.getElementsByTagName(DEVICE);

		for (int x = 0; x < deviceList.getLength(); x++)
		{
			Node node = deviceList.item(x);

			if (node.getNodeType() == Node.ELEMENT_NODE)
			{
				Element element = (Element) node;
				createDevice(deviceFactory, element, name);
			}
		}
	}
	
	private void createDevice(BaseNodeController deviceFactory, Element element, String name)
	{
		String type = element.getAttribute(DeviceLoadingService.DEVICE_TYPE);
		String xmlName = element.getAttribute(DeviceLoadingService.DEVICE_NAME);
		
		if(name != null && !xmlName.equals(name))
			return;
			
		try
		{
			DeviceConfig config = load(element, type);
			
			if(config instanceof RemoteDeviceConfig)
				deviceFactory.createNewDevice(loadRemoteDevice(element, config), true);
			else
				deviceFactory.createNewDevice(config, false);
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe("Error creating Device: " + xmlName + ". Exception: " + e.getMessage());
		}

	}
	
	private DeviceConfig loadRemoteDevice(Element element, DeviceConfig config) throws JAXBException, Exception
	{
		Element deviceElement = (Element) element.getElementsByTagName(DeviceLoadingService.DEVICE).item(0);
		String name = deviceElement.getAttribute(DeviceLoadingService.DEVICE_NAME);
		String type = deviceElement.getAttribute(DeviceLoadingService.DEVICE_TYPE);
		String nodeID = element.getElementsByTagName("nodeID").item(0).getTextContent();
		
		((RemoteDeviceConfig) config).setElement(load(deviceElement, type));
		((RemoteDeviceConfig) config).setType(type);
		((RemoteDeviceConfig) config).setNodeID(nodeID);
		
		return config;
	}
	
	private DeviceConfig load(Element element, String type) throws Exception, JAXBException
	{
		Class<?> configType = registeredDeviceConfigs.get(type);
		if (configType == null)
			throw new Exception("Device Config not found: " + type);
		
		JAXBContext jaxbContext = JAXBContext.newInstance(configType);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		DeviceConfig config = (DeviceConfig) jaxbUnmarshaller.unmarshal(element);
		return config;
	}
}