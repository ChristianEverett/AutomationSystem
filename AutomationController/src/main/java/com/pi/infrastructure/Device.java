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
import com.pi.devices.Switch;
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
	public static final int GET_EXPECTED_PARAMS = 3;
	
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
	protected abstract void performAction(DeviceState state);

	/**
	 * @param forDatabase
	 * @return action representing the current state of the device. If device is
	 *         closed returns null
	 * @throws IOException 
	 */
	public abstract DeviceState getState(Boolean forDatabase) throws IOException;

	/**
	 * Shutdown device and release resources. All future calls to performAction
	 * will do nothing
	 * @throws IOException 
	 */
	public abstract void close() throws IOException;
	
	/**
	 * @return list of params this device expects
	 */
	public abstract List<String> getExpectedParams();

	public DeviceState getState() throws IOException{return getState(false);}
	
	public final void execute(DeviceState state)
	{
		if(validate(state))
			performAction(state);
		
		try
		{
			if(!(this instanceof RemoteDevice) && !(this instanceof AsynchronousDevice))
				update(getState(false));
		}
		catch (IOException e)
		{
			Application.LOGGER.severe(e.getMessage());
		}
	}
	
	protected void update(DeviceState state)
	{
		if(node != null)
		{
			node.notifyAutomationControllerOfStateUpdate(state);
		}
		else
		{
			Processor.getBackgroundProcessor().geEventProcessingService().update(state);
		}
	}
	
	/**
	 * @param state
	 * @param expectedParams
	 * @return true if the state contains all the expected param's of the correct type, otherwise return false
	 */
	public boolean validate(DeviceState state)
	{
		if(state == null)
			return false;
		
		List<String> expectedParams = getExpectedParams();
		
		for(String expectedParam : expectedParams)
		{
			Object param = state.getParam(expectedParam);
			
			if(param == null)
				return false;
			Class<?> type = DeviceType.paramTypes.get(expectedParam);
			
			if(type == null || !type.isInstance(param))
				return false;
		}
		
		return true;
	}
	
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
		return getDeviceState(name, false);
	}
	
	public static final DeviceState getDeviceState(String name, Boolean forDatabase)
	{
		Device device = lookupDevice(name);
		
		if(device != null)
		{
			try
			{
				return device.getState(forDatabase);
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
				//Don't pass forDatabase param, automation node should never need device config
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
	 * @param forDatabase
	 * @return all device states
	 */
	public static List<DeviceState> getStates(Boolean forDatabase)
	{	
		List<DeviceState> stateList = new ArrayList<>();
		
		for(Entry<String, Device> device : deviceMap.entrySet())
		{
			DeviceState state = getDeviceState(device.getKey(), forDatabase);
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
	
	public static boolean closeDevice(String name)
	{
		Device device = deviceMap.remove(name);
		
		try
		{
			if (device != null)
			{
				Application.LOGGER.info("Closing: " + name);
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
			closeDevice(entry.getKey());
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

	public static DeviceState createNewDeviceState(String name)
	{
		Device device = lookupDevice(name);
		
		return DeviceState.create(name, device != null ? device.getType() : DeviceType.UNKNOWN);
	}
	
	protected static abstract class DeviceConfig
	{
		protected String name;
		
		@XmlAttribute(name = "device-name")
		public void setName(String name)
		{
			this.name = name;
		}
		
		public abstract Device buildDevice() throws Exception;
	}
}
