package com.pi.infrastructure;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.annotation.XmlAttribute;

import com.pi.backgroundprocessor.TaskExecutorService;
import com.pi.backgroundprocessor.TaskExecutorService.Task;
import com.pi.model.DeviceState;
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
	private static NodeController node = null;

	// Device Name
	protected final String name;

	// Constants to bind method calls on remote devices
	public static final int PERFORM_ACTION = 0;
	public static final int GET_STATE = 1;
	public static final int CLOSE = 2;

	protected static final String DEVICE = "device";

	public static final void registerNodeManger(NodeController node)
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
		return DeviceType.typeToId.get(this.getClass());
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
				if (!(this instanceof RemoteDevice) && !(this instanceof AsynchronousDevice))
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

	public static boolean queueAction(DeviceState state) throws IOException
	{
		return node.scheduleAction(state);
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

		return DeviceState.create(name, device != null ? device.getType() : DeviceType.UNKNOWN);
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
