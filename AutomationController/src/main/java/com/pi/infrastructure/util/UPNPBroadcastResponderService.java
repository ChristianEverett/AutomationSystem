package com.pi.infrastructure.util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.pi.SystemLogger;
import com.pi.infrastructure.BaseService;

public class UPNPBroadcastResponderService extends BaseService
{
	private static final String IP_STRING = "239.255.255.250";
	private static final int PORT = 1900;
	private MulticastSocket multiCastServer = null;
	private Map<Integer, UPNPDevice> upnpDevices = new ConcurrentHashMap<Integer, UPNPDevice>();
	private Selector selector = Selector.open();
	private AtomicBoolean selectorLock = new AtomicBoolean(false);
	
	public UPNPBroadcastResponderService() throws IOException
	{
		multiCastServer = new MulticastSocket(PORT);
		InetAddress address = InetAddress.getByName(IP_STRING);
		multiCastServer.joinGroup(address);
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

			for (UPNPDevice upnpdevice : upnpDevices.values())
			{
				if (upnpdevice.isValidSearch(result))
				{
					SystemLogger.getLogger().info("Responding to discovery for : " + upnpdevice.getName());
					respond(upnpdevice, result);
				}
			}
		}
		catch (IOException e)
		{
			if (!multiCastServer.isClosed())
				throw e;
		}
	}

	public void listenForRequests() throws IOException
	{
		if(selectorLock.get()) return;
		
		int readyChannels = selector.select();
		if(readyChannels == 0) return;
		
		Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
		
		while(selectedKeys.hasNext())
		{
			SelectionKey key = selectedKeys.next();
			
			if (key.isAcceptable())
			{
				ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
				UPNPDevice upnpDevice = upnpDevices.get(serverSocketChannel.socket().getLocalPort());
				
				if(upnpDevice != null)
					upnpDevice.acceptRequest(serverSocketChannel);
			}
			
			selectedKeys.remove();
		}
	}
	
	private void respond(UPNPDevice upnpDevice, UPNPPacket request) throws Exception
	{
		String responseMessage = upnpDevice.getSearchResponse(request);

		try (DatagramSocket tempSocket = new DatagramSocket())
		{
			DatagramPacket response = new DatagramPacket(responseMessage.toString().getBytes(), responseMessage.toString().getBytes().length, request.getAddress(), request.getPort());
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
		multiCastServer.receive(msgPacket);
		
		return new UPNPPacket(msgPacket.getAddress(), msgPacket.getPort(), new String(buf, 0, buf.length));
	}

	public static InetAddress getLocalIpAddress() throws IOException
	{
		for (Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements();)
		{
			NetworkInterface networkInterface = interfaces.nextElement();

			if (networkInterface.getDisplayName().equals("eth0"))
			{
				for (Enumeration<InetAddress> addresses = networkInterface.getInetAddresses(); addresses.hasMoreElements();)
				{
					InetAddress address = addresses.nextElement();

					if (address instanceof Inet4Address)
						return address;
				}
			}
		}

		throw new IOException("Could not find local IP address");
	}

	public void addDevice(UPNPDevice device) throws ClosedChannelException
	{
		selectorLock.set(true);
		selector.wakeup();
		int port = device.register(selector);
		selectorLock.set(false);
		upnpDevices.put(port, device);
	}

	public void removeDevice(UPNPDevice device) throws IOException 
	{
		try
		{
			device.close();
		}
		catch (IOException e)
		{
			throw e;
		}
		finally 
		{
			upnpDevices.remove(device);
		}
	}

	public void removeAll()
	{
		for(UPNPDevice device : upnpDevices.values())
			try
			{
				device.close();
			}
			catch (IOException e)
			{
				SystemLogger.getLogger().severe(e.getMessage());
			}
	}
	
	public void close()
	{
		multiCastServer.close();
		removeAll();
	}
}
