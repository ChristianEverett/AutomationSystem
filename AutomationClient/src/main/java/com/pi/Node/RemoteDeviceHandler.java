package com.pi.Node;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;

import com.pi.SystemLogger;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceAPI;
import com.pi.model.DeviceState;

public class RemoteDeviceHandler extends UnicastRemoteObject implements DeviceAPI
{
	private NodeControllerImpl node;
	private Device device;
	
	public RemoteDeviceHandler(NodeControllerImpl node, Device device) throws RemoteException
	{
		this.node = node;
		this.device = device;
	}

	@Override
	public void execute(DeviceState deviceState) throws RemoteException
	{
		try
		{
			device.execute(deviceState);
		}
		catch (IOException e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
		}		
	}

	@Override
	public DeviceState getCurrentDeviceState() throws RemoteException
	{
		return device.getCurrentDeviceState();
	}

	@Override
	public void close() throws RemoteException
	{
		node.closeDevice(device.getName());
	}

	@Override
	public void newActionProfile(Collection<String> actionProfileNames) throws RemoteException
	{
		device.newActionProfile(actionProfileNames);
	}

	@Override
	public boolean isAsynchronousDevice() throws RemoteException
	{
		return device.isAsynchronousDevice();
	}
}
