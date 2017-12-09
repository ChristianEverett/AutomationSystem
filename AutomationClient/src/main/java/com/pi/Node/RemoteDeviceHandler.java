package com.pi.Node;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;

import com.pi.SystemLogger;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceAPI;
import com.pi.model.DeviceState;
import com.pi.model.MacAddress;

public class RemoteDeviceHandler extends UnicastRemoteObject implements /*InvocationHandler,*/ DeviceAPI
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
		device.execute(deviceState);
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

	@Override
	public void newMacAddress(Collection<MacAddress> addresses) throws RemoteException
	{
		device.newMacAddress(addresses);
	}

//	@Override
//	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
//	{
//		return method.invoke(device, args); TODO
//	}
}
