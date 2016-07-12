/**
 * 
 */
package com.pi.backgroundprocessor;

import java.net.InetSocketAddress;
import java.net.Socket;

import com.pi.Application;

/**
 * @author Christian Everett
 *
 */
class NetworkConnectionPoller
{
	private static boolean lock = false;
	private static boolean connected = true;
	
	public static boolean isLocked() {return lock;}
	public static boolean isConnected() {return connected;}
	
	public static synchronized void checkConnection()
	{
		lock = true;

		int timeout = 20000;
		
		try(Socket socket = new Socket())
		{
			socket.connect(new InetSocketAddress("www.google.com", 80), timeout);
			socket.close();
			connected = true;
		} 
		catch (Exception e)
		{
			try(Socket socket = new Socket())
			{
				Thread.sleep(2000);
				socket.connect(new InetSocketAddress("www.google.com", 80), timeout);
				socket.close();
				connected = true;
			}
			catch (Exception e2) 
			{
				connected = false;
				Application.LOGGER.severe("Lost Network Connection");
			}

		}

		lock = false;
	}
}
