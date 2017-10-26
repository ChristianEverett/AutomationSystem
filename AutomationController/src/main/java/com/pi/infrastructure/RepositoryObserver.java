package com.pi.infrastructure;

import java.rmi.RemoteException;
import java.util.Collection;

public interface RepositoryObserver
{
	public default void newActionProfile(Collection<String> actionProfileNames) throws RemoteException
	{
		
	}
}
