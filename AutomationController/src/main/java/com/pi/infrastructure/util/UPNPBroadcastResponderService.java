package com.pi.infrastructure.util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import com.pi.SystemLogger;
import com.pi.infrastructure.Service;

public class UPNPBroadcastResponderService extends Service
{
	private static final String IP_STRING = "239.255.255.250";
	private static final int PORT = 1900;
	private MulticastSocket serverSocket = null;
	private List<UPNPDevice> upnpDevices = new CopyOnWriteArrayList<UPNPDevice>();

	public UPNPBroadcastResponderService() throws IOException
	{
		serverSocket = new MulticastSocket(PORT);
		InetAddress address = InetAddress.getByName(IP_STRING);
		serverSocket.joinGroup(address);
	}

	@Override
	public void executeService() throws Exception
	{
		listenForSearch();
	}
	
	public void listenForSearch() throws Exception
	{
		try
		{
			UPNPPacket result = recvFrom();

			for (UPNPDevice upnpdevice : upnpDevices)
			{
				if (upnpdevice.isValidSearch(result))
				{
					respond(upnpdevice, result);
				}
			}
		}
		catch (IOException e)
		{
			if(!serverSocket.isClosed())
				throw e;
		}
	}

	private void respond(UPNPDevice upnpDevice, UPNPPacket request) throws Exception
	{
		String responseMessage = upnpDevice.getSearchResponse(request);
		
		try (DatagramSocket tempSocket = new DatagramSocket())
		{
			DatagramPacket response = new DatagramPacket(responseMessage.toString().getBytes(), responseMessage.toString().getBytes().length, 
					request.getAddress(), request.getPort());
			tempSocket.send(response);
		}
		catch (Exception e)
		{
			throw e;
		}
	}
	
	private UPNPPacket recvFrom() throws IOException
	{
		byte[] buf = new byte[1024];

		DatagramPacket msgPacket = new DatagramPacket(buf, buf.length);
		serverSocket.receive(msgPacket);

		return new UPNPPacket(msgPacket.getAddress(), msgPacket.getPort(), new String(buf, 0, buf.length));
	}

	public static InetAddress getLocalIpAddress() throws IOException
	{	
		for (Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements();)
		{
			NetworkInterface networkInterface = interfaces.nextElement();
			
			if(networkInterface.getDisplayName().equals("eth0"))
			{
				for (Enumeration<InetAddress> addresses = networkInterface.getInetAddresses(); addresses.hasMoreElements();)
				{
					InetAddress address = addresses.nextElement();
					
					if(address instanceof Inet4Address)
						return address;
				}
			}
		}
		
		throw new IOException("Could not find local IP address");
	}
	
	public void addDevice(UPNPDevice device)
	{
		upnpDevices.add(device);
	}
	
	public void removeDevice(UPNPDevice device)
	{
		upnpDevices.remove(device);
	}
	
	public void close()
	{
		serverSocket.close();
	}
}
