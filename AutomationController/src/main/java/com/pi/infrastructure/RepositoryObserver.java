package com.pi.infrastructure;

import java.rmi.RemoteException;
import java.util.Collection;

import com.pi.model.MacAddress;

public interface RepositoryObserver
{
	default public void newActionProfile(Collection<String> actionProfileNames) throws RemoteException
	{
		
	}
	
	default public void newMacAddress(Collection<MacAddress> addresses) throws RemoteException
	{
		
	}
}
