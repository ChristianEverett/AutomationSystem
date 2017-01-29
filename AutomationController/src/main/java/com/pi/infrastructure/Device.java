/**
 * 
 */
package com.pi.infrastructure;

import java.io.IOException;
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
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;

/**
 * @author Christian Everett
 *
 */
public abstract class Device
{
	private static HashMap<String, Device> deviceMap = new HashMap<>();
	private static HashMap<String, Class<?>> registeredDeviceConfigs = new HashMap<>();
	private static TaskExecutorService taskService = new TaskExecutorService(2);

	// Map GPIO Headers to BCM/WiringPI GPIO pins
	protected static final HashMap<Integer, GPIO_PIN> pins = new HashMap<>();
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
	 * @throws IOException
	 */
	public abstract void performAction(DeviceState state);

	/**
	 * @return action representing the current state of the device. If device is
	 *         closed returns null
	 */
	public abstract DeviceState getState();

	/**
	 * Shutdown device and release resources. All future calls to performAction
	 * will do nothing
	 */
	public abstract void close();

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
			return device.getState();
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
			stateList.add(device.getValue().getState());
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
			return node.requestAction(state);
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
		
		if(device != null)
		{
			device.close();	
			return true;
		}
		
		return false;
	}
	
	public static void closeAll()
	{
		deviceMap.entrySet().forEach((Entry<String, Device> entry) -> 
		{
			Application.LOGGER.info("closing: " + entry.getKey());
			entry.getValue().close();
		});
		deviceMap.clear();
	}
	
	public static void createNewDevice(Element element)
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
				config.setName(name);
				((RemoteDeviceConfig)config).setElement(deviceElement);
				((RemoteDeviceConfig)config).setType(type);
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
	
	protected static class GPIO_PIN
	{
		private int BCM_Pin;
		private Pin WiringPI_Pin;

		public GPIO_PIN(int BCM, Pin WiringPI)
		{
			this.BCM_Pin = BCM;
			this.WiringPI_Pin = WiringPI;
		}

		/**
		 * @return the bCM_Pin
		 */
		public int getBCM_Pin()
		{
			return BCM_Pin;
		}

		/**
		 * @return the wiringPI_Pin
		 */
		public Pin getWiringPI_Pin()
		{
			return WiringPI_Pin;
		}
	}

	static
	{
		DeviceType.registerAllDeviceConfigs();
		
		pins.put(1, null);
		pins.put(2, null);
		pins.put(3, new GPIO_PIN(2, RaspiPin.GPIO_08));
		pins.put(4, null);
		pins.put(5, new GPIO_PIN(3, RaspiPin.GPIO_09));
		pins.put(6, null);
		pins.put(7, new GPIO_PIN(4, RaspiPin.GPIO_07));
		pins.put(8, new GPIO_PIN(14, RaspiPin.GPIO_15));
		pins.put(9, null);
		pins.put(10, new GPIO_PIN(15, RaspiPin.GPIO_16));
		pins.put(11, new GPIO_PIN(17, RaspiPin.GPIO_00));
		pins.put(12, new GPIO_PIN(18, RaspiPin.GPIO_01));
		pins.put(13, new GPIO_PIN(27, RaspiPin.GPIO_02));
		pins.put(14, null);
		pins.put(15, new GPIO_PIN(22, RaspiPin.GPIO_03));
		pins.put(16, new GPIO_PIN(23, RaspiPin.GPIO_04));
		pins.put(17, null);
		pins.put(18, new GPIO_PIN(24, RaspiPin.GPIO_05));
		pins.put(19, new GPIO_PIN(10, RaspiPin.GPIO_12));
		pins.put(20, null);
		pins.put(21, new GPIO_PIN(9, RaspiPin.GPIO_13));
		pins.put(22, new GPIO_PIN(25, RaspiPin.GPIO_06));
		pins.put(23, new GPIO_PIN(11, RaspiPin.GPIO_14));
		pins.put(24, new GPIO_PIN(8, RaspiPin.GPIO_10));
		pins.put(25, null);
		pins.put(26, new GPIO_PIN(7, RaspiPin.GPIO_11));
		pins.put(27, null);
		pins.put(28, null);
		pins.put(29, new GPIO_PIN(5, RaspiPin.GPIO_21));
		pins.put(30, null);
		pins.put(31, new GPIO_PIN(6, RaspiPin.GPIO_22));
		pins.put(32, new GPIO_PIN(12, RaspiPin.GPIO_26));
		pins.put(33, new GPIO_PIN(13, RaspiPin.GPIO_23));
		pins.put(34, null);
		pins.put(35, new GPIO_PIN(19, RaspiPin.GPIO_24));
		pins.put(36, new GPIO_PIN(16, RaspiPin.GPIO_27));
		pins.put(37, new GPIO_PIN(26, RaspiPin.GPIO_25));
		pins.put(38, new GPIO_PIN(20, RaspiPin.GPIO_28));
		pins.put(39, null);
		pins.put(40, new GPIO_PIN(21, RaspiPin.GPIO_29));
	}
}
