package com.pi.infrastructure;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
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
public abstract class Device implements RepositoryObserver, DeviceAPI
{
	private static TaskExecutorService taskService = new TaskExecutorService(3);

	protected static Runtime rt = Runtime.getRuntime();
	protected static GpioController gpioController = GpioFactory.getInstance();

	// Used to find Remote Devices that exist on another node
	private static BaseNodeController node = null;

	// Device Name
	protected final String name;
	private boolean deviceClosed = false;
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
	
	public boolean isAsynchronousDevice() 
	{
		return false;
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
	public abstract DeviceState getState(DeviceState state);

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
	
	public synchronized final void loadSavedData(DeviceState state) throws Exception
	{
		if (state != null && !isAsynchronousDevice())
		{
			performAction(state);
		}
	}
	
	public synchronized final void execute(DeviceState state)
	{
		try
		{
			if(deviceClosed)
				throw new RuntimeException("Can't execute closed device");
			
			if (state != null)
			{
				performAction(state);
				if (!(this instanceof RemoteDeviceProxy) && !isAsynchronousDevice())
					update(getCurrentDeviceState());
			} 
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public synchronized final DeviceState getCurrentDeviceState()
	{
		try
		{
			DeviceState state = Device.createNewDeviceState(name);
			
			return getState(state);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public synchronized void close() throws Exception
	{
		deviceClosed = true;
		tearDown();
	}

	protected void update(DeviceState state)
	{
		if (state != null)
		{
			try
			{
				node.update(state);
			}
			catch (RemoteException e)
			{
				throw new RuntimeException(e);
			}
		}
	}

	protected void trigger(String profileName)
	{
		if (profileName != null)
		{
			try
			{
				node.trigger(profileName);
			}
			catch (RemoteException e)
			{
				throw new RuntimeException(e);
			}
		}
	}
	
	protected void unTrigger(String profileName)
	{
		if (profileName != null)
		{
			try
			{
				node.unTrigger(profileName);
			}
			catch (RemoteException e)
			{
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * @param name of device
	 * @return device state or null if device not found
	 */
	public static final DeviceState getDeviceState(String name)
	{
		return node.getDeviceState(name);
	}

	public static void queueAction(DeviceState state) throws IOException
	{
		node.scheduleAction(state);
	}

	public static void queueActions(List<DeviceState> states) throws IOException
	{
		for (DeviceState state : states)
		{
			node.scheduleAction(state);
		}
	}
	
	protected <T extends Serializable> Collection<T> getRepositoryValues(String type)
	{
		try
		{
			return node.getRepositoryValues(type);
		}
		catch (RemoteException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	protected <T extends Serializable> T getRepositoryValue(String type, String key)
	{
		try
		{
			return node.getRepositoryValue(type, key);
		}
		catch (RemoteException e)
		{
			throw new RuntimeException(e);
		}	
	}
	
	protected <T extends Serializable> void setRepositoryValue(String type, T value)
	{
		try
		{
			node.setRepositoryValue(type, value);
		}
		catch (RemoteException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	protected Task createTask(Runnable task, Long delay, TimeUnit unit)
	{
		return taskService.scheduleTask(task, delay, unit);
	}

	protected Task createTask(Runnable task, Long delay, Long interval, TimeUnit unit)
	{
		return taskService.scheduleTask(task, delay, interval, unit);
	}

	protected Task createFixedRateTask(Runnable task, Long delay, Long interval, TimeUnit unit)
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
