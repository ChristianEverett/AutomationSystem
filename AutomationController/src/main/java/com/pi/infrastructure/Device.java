/**
 * 
 */
package com.pi.infrastructure;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;

import org.w3c.dom.Element;
import com.pi.Application;
import com.pi.backgroundprocessor.Processor;
import com.pi.backgroundprocessor.TaskExecutorService;
import com.pi.backgroundprocessor.TaskExecutorService.Task;
import com.pi.infrastructure.RemoteDevice.Node;
import com.pi.infrastructure.RemoteDevice.RemoteDeviceConfig;
import com.pi.model.DeviceState;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;

/**
 * @author Christian Everett
 *
 */
public abstract class Device
{
	private static HashMap<String, Device> deviceMap = new HashMap<>();
	private static HashMap<String, Class<?>> registeredDeviceConfigs = new HashMap<>();
	private static TaskExecutorService taskService = new TaskExecutorService(2);

	protected static Runtime rt = Runtime.getRuntime();
	protected static GpioController gpioController = GpioFactory.getInstance();

	//Used to find Remote Devices that exist on another node
	private static Node node = null;
	
	//Device Name
	protected final String name;

	//Constants to bind method calls on remote devices
	public static final int PERFORM_ACTION = 0;
	public static final int GET_STATE = 1;
	public static final int CLOSE = 2;
	
	protected static final String DEVICE = "device";
	
	static
	{
		DeviceType.registerAllDeviceConfigs();
	}
	
	public Device(String name) throws IOException
	{
		this.name = name;
	}

	/**
	 * @return the name of this device
	 */
	public String getName()
	{
		return name;
	}
	
	protected static void registerDevice(String name, Class<?> type)
	{
		registeredDeviceConfigs.put(name, type);
	}
	
	/**
	 * @return the type of device
	 */
	public abstract String getType();
	
	/**
	 * @param state
	 * @throws Exception 
	 * @throws IOException
	 */
	public abstract void performAction(DeviceState state);

	/**
	 * @return action representing the current state of the device. If device is
	 *         closed returns null
	 * @throws IOException 
	 */
	public abstract DeviceState getState() throws IOException;

	/**
	 * Shutdown device and release resources. All future calls to performAction
	 * will do nothing
	 * @throws IOException 
	 */
	public abstract void close() throws IOException;

	public static final void registerRemoteDeviceLookup(Node node)
	{
		Device.node = node;
	}
	
	/**
	 * @param name of device
	 * @return Device
	 */
	public static final Device lookupDevice(String name)
	{
		return deviceMap.get(name);
	}
	
	/**
	 * @return all devices
	 */
	public static final Set<Entry<String, Device>> getDeviceSet()
	{
		return deviceMap.entrySet();
	}
	
	/**
	 * @param name of device
	 * @return device state or null if device not found
	 */
	public static final DeviceState getDeviceState(String name)
	{
		Device device = lookupDevice(name);
		
		if(device != null)
		{
			try
			{
				return device.getState();
			}
			catch (Exception e)
			{
				Application.LOGGER.severe("Could not get state: " + name + " " + e.getMessage());
			}
		}
		else if(node != null)
		{
			try
			{
				return node.getDeviceState(name);
			}
			catch (Exception e)
			{
				Application.LOGGER.severe(e.getMessage());
			}
		}
		
		return null;
	}
	
	/**
	 * @return all device states
	 */
	public static List<DeviceState> getStates()
	{	
		List<DeviceState> stateList = new ArrayList<>();
		
		for(Entry<String, Device> device : deviceMap.entrySet())
		{
			DeviceState state = getDeviceState(device.getKey());
			if(state != null)
				stateList.add(state);
		}
		
		return stateList;
	}
	
	public static boolean queueAction(DeviceState state)
	{
		if(node == null)
		{
			return Processor.getBackgroundProcessor().scheduleAction(state);
		}
		else 
		{
			Device device = lookupDevice(state.getName());
			
			if(device != null)
			{
				try
				{
					device.performAction(state);
					return true;
				}
				catch(Exception e)
				{
					Application.LOGGER.severe(e.getMessage());
				}
				
				return false;
			}
			else
			{
				return node.requestAction(state);
			}
		}
	}
	
	public static Task createTask(Runnable task, Long delay, TimeUnit unit)
	{
		return taskService.scheduleTask(task, delay, unit);
	}
	
	public static Task createTask(Runnable task, Long delay, Long interval, TimeUnit unit)
	{
		return taskService.scheduleTask(task, delay, interval, unit);
	}
	
	public static boolean close(String name)
	{
		Device device = deviceMap.remove(name);
		
		try
		{
			if (device != null)
			{
				device.close();
				return true;
			} 
		}
		catch (Exception e)
		{
			Application.LOGGER.severe("Failed to close " + name + " Got:" + e.getMessage());
		}
		
		return false;
	}
	
	public static void closeAll()
	{
		deviceMap.entrySet().forEach((Entry<String, Device> entry) -> 
		{
			Application.LOGGER.info("closing: " + entry.getKey());
			close(entry.getKey());
		});
		deviceMap.clear();
	}
	
	public static void createNewDevice(Element element)//TODO smart device create
	{
		String type = element.getAttribute(DeviceLoader.DEVICE_TYPE);
		String name = element.getAttribute(DeviceLoader.DEVICE_NAME);
		
		try
		{
			if(deviceMap.get(name) != null)
				return;
			
			Class<?> configType = registeredDeviceConfigs.get(type);
			if(configType == null)
				throw new Exception("Device Config not found: " + type);
			
			JAXBContext jaxbContext = JAXBContext.newInstance(configType);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			DeviceConfig config = (DeviceConfig) jaxbUnmarshaller.unmarshal(element);
			
			//Extract inner device for RemoteDevice type
			if (DeviceType.REMOTE_DEVICE.equals(type))
			{
				Element deviceElement = (Element) element.getElementsByTagName(DeviceLoader.DEVICE).item(0);
				name = deviceElement.getAttribute(DeviceLoader.DEVICE_NAME);
				type = deviceElement.getAttribute(DeviceLoader.DEVICE_TYPE);
				String nodeID = element.getElementsByTagName("nodeID").item(0).getTextContent();

				InetAddress address = Processor.getBackgroundProcessor().lookupNodeAddress(nodeID);
				if (address == null)
					return;
						
				((RemoteDeviceConfig) config).setElement(deviceElement);
				((RemoteDeviceConfig) config).setType(type);
				((RemoteDeviceConfig) config).setUrl("http://" + address.getHostAddress() + ":8080");
			}
			
			Device device = config.buildDevice();
			deviceMap.put(name, device);
			Application.LOGGER.info("Loaded: " + name);
		}
		catch (Exception e)
		{
			Application.LOGGER.severe("Error Loading Device: " + name + ". Exception: " + e.getMessage());
		}
	}

	protected static abstract class DeviceConfig
	{
		protected String name;
		
		@XmlAttribute(name = "device-name")
		public void setName(String name)
		{
			this.name = name;
		}
		
		public abstract Device buildDevice() throws IOException;
	}
}
