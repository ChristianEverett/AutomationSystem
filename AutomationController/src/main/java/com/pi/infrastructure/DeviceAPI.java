package com.pi.infrastructure;

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.pi.model.DeviceState;

public interface DeviceAPI extends RepositoryObserver, Remote
{
	public void execute(DeviceState deviceState) throws RemoteException;
	public DeviceState getCurrentDeviceState() throws RemoteException;
	public void close() throws RemoteException;
	public boolean isAsynchronousDevice() throws RemoteException;
}
