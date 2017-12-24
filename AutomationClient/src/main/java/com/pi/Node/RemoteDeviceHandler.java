package com.pi.node;

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

public class RemoteDeviceHandler implements InvocationHandler
{
	private NodeControllerImpl node;
	private Device device;
	
	public RemoteDeviceHandler(NodeControllerImpl node, Device device) throws RemoteException
	{
		this.node = node;
		this.device = device;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
	{
		return method.invoke(device, args);
	}
}
