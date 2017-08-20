package com.pi.infrastructure;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.annotation.XmlAttribute;

import com.pi.infrastructure.DeviceType.DeviceTypeMap;
import com.pi.model.DeviceState;
import com.pi.services.TaskExecutorService;
import com.pi.services.TaskExecutorService.Task;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;

/**
 * @author Christian Everett
 *
 */
public abstract class Device
{
	private static TaskExecutorService taskService = new TaskExecutorService(3);

	protected static Runtime rt = Runtime.getRuntime();
	protected static GpioController gpioController = GpioFactory.getInstance();

	// Used to find Remote Devices that exist on another node
	private static BaseNodeController node = null;

	// Device Name
	protected final String name;

	// Constants to bind method calls on remote devices
	public static final int PERFORM_ACTION = 0;
	public static final int GET_STATE = 1;
	public static final int CLOSE = 2;

	protected static final String DEVICE = "device";
	
	public static final void registerNodeManger(BaseNodeController node)
	{
		Device.node = node;
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

	/**
	 * @param state
	 * @throws Exception
	 * @throws IOException
	 */
	protected abstract void performAction(DeviceState state) throws Exception;

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
	 * 
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	protected abstract void tearDown() throws Exception;
	
	public String getType()
	{
		return DeviceTypeMap.getType(this.getClass());
	}
	
	public synchronized final void loadSavedData(DeviceState state) throws IOException
	{
		if (state != null)
		{
			load(state);
		}
	}
	
	protected synchronized void load(DeviceState state) throws IOException
	{
		execute(state);
	}
	
	public synchronized final void execute(DeviceState state) throws IOException
	{
		try
		{
			if (state != null)
			{
				performAction(state);
				if (!(this instanceof RemoteDeviceProxy) && !(this instanceof AsynchronousDevice))
					update(getState(false));
			} 
		}
		catch (Exception e)
		{
			throw new IOException("Could not performAction on " + state.getName());
		}
	}
	
	public void close() throws Exception
	{
		tearDown();
	}

	protected void update(DeviceState state)
	{
		if (state != null)
		{
			node.update(state);
		}
	}

	public synchronized DeviceState getState() throws IOException
	{
		return getState(false);
	}

	/**
	 * @param name of device
	 * @return device state or null if device not found
	 */
	public static final DeviceState getDeviceState(String name)
	{
		return node.getDeviceState(name, false);
	}

	public static void queueAction(DeviceState state) throws IOException
	{
		node.scheduleAction(state);
	}

	protected <T extends Serializable> T getRepositoryValue(String type, String key)
	{
		return node.getRepositoryValue(type, key);	
	}
	
	protected <T extends Serializable> void setRepositoryValue(String type, String key, T value)
	{
		node.setRepositoryValue(type, key, value);
	}
	
	public static Task createTask(Runnable task, Long delay, TimeUnit unit)
	{
		return taskService.scheduleTask(task, delay, unit);
	}

	public static Task createTask(Runnable task, Long delay, Long interval, TimeUnit unit)
	{
		return taskService.scheduleTask(task, delay, interval, unit);
	}

	public static Task createFixedRateTask(Runnable task, Long delay, Long interval, TimeUnit unit)
	{
		return taskService.scheduleFixedRateTask(task, delay, interval, unit);
	}

	public static DeviceState createNewDeviceState(String name)
	{
		Device device = node.lookupDevice(name);

		return new DeviceState(name, device != null ? device.getType() : DeviceType.UNKNOWN);
	}
	
	public static abstract class DeviceConfig implements Serializable
	{
		protected String name;
		
		@XmlAttribute(name = "device-name")
		public void setName(String name)
		{
			this.name = name;
		}

		public String getName()
		{
			return name;
		}

		public abstract Device buildDevice() throws Exception;
	}
}
