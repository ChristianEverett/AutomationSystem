package com.pi.infrastructure;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;

import com.pi.model.DeviceState;

public interface NodeControllerAPI extends Remote
{
	abstract public void scheduleAction(DeviceState state) throws RemoteException;
	abstract public DeviceState getDeviceState(String name) throws RemoteException;	
	abstract public void update(DeviceState state) throws RemoteException;	
	abstract public void trigger(String actionProfileName) throws RemoteException;	
	abstract public void unTrigger(String profileName) throws RemoteException;
	abstract public <T extends Serializable> Collection<T> getRepositoryValues(String type) throws RemoteException;
	abstract public <T extends Serializable, K extends Serializable> T getRepositoryValue(String type, K key) throws RemoteException;
	abstract public <T extends Serializable, K extends Serializable> void setRepositoryValue(String type, K key, T value) throws RemoteException;
}
