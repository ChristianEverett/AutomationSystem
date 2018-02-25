package com.pi.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.function.BiConsumer;

import org.springframework.stereotype.Service;

import com.pi.SystemLogger;
import com.pi.infrastructure.BaseService;
import com.pi.infrastructure.util.PropertyManger;
import com.pi.infrastructure.util.PropertyManger.PropertyKeys;

@Service
public class NodeDiscovererService extends BaseService
{
	public static final String AUTOMATION_CONTROLLER = "automation_controller";
	private static final String INET_ADDRESS = "224.0.0.3";
	private static final int DISCOVERY_PORT = 9876;

	private MulticastSocket serverSocket;
	
	private BiConsumer<String, InetAddress> register;

	private NodeDiscovererService()
	{
		try
		{
			InetAddress address = InetAddress.getByName(INET_ADDRESS);
			serverSocket = new MulticastSocket(DISCOVERY_PORT);
			serverSocket.joinGroup(address);
		}
		catch (IOException e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
		}
	}
	
	public synchronized void start(int seconds, BiConsumer<String, InetAddress> register)
	{
		this.register = register;
		super.start(seconds);
	}

	@Override
	public void executeService() throws Exception
	{
		try
		{
			DatagramPacket receivePacket = listenForProbe();
			Probe probe = extractProbeFromDatagram(receivePacket);
					
			register.accept(probe.getNodeName(), receivePacket.getAddress());
			replyToNode(receivePacket.getAddress(), receivePacket.getPort());
		}
		catch (Exception e)
		{
			if (e instanceof InterruptedException)
				SystemLogger.getLogger().info("Stopping Node Discovery");
			else
				throw e;
		}
	}

	private DatagramPacket listenForProbe() throws ClassNotFoundException, IOException
	{
		byte[] packet = new byte[1024];
		DatagramPacket receivePacket = new DatagramPacket(packet, packet.length);
		serverSocket.receive(receivePacket);
		
		return receivePacket;
	}

	public static Probe extractProbeFromDatagram(DatagramPacket receivePacket) throws IOException, ClassNotFoundException
	{
		InetAddress IPAddress = receivePacket.getAddress();
		byte[] data = receivePacket.getData();
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		ObjectInputStream objectInput = new ObjectInputStream(in);
		return (Probe) objectInput.readObject();
	}
	
	private void replyToNode(InetAddress address, int port) throws IOException
	{
		Probe probe = new Probe(AUTOMATION_CONTROLLER, Probe.BROAD_CAST_ACK);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutput = new ObjectOutputStream(outputStream);
		objectOutput.writeObject(probe);
		
		DatagramPacket sendPacket = new DatagramPacket(outputStream.toByteArray(), outputStream.toByteArray().length, address, port);
		serverSocket.send(sendPacket);
	}
	
	@Override
	protected void close()
	{
		serverSocket.close();	
	}
	
	public static InetAddress broadCastFromNode(String nodeID) throws Exception
	{
		Probe probe = new Probe(nodeID, Probe.BROAD_CAST);
		
		try(DatagramSocket clientSocket = new DatagramSocket())
		{	
			InetAddress IPAddress = InetAddress.getByName(INET_ADDRESS);
			clientSocket.setSoTimeout(5000);
			
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ObjectOutputStream objectOutput = new ObjectOutputStream(outputStream);
			objectOutput.writeObject(probe);

			DatagramPacket sendPacket = new DatagramPacket(outputStream.toByteArray(), outputStream.toByteArray().length, 
					IPAddress, NodeDiscovererService.DISCOVERY_PORT);
			
			byte[] packet = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(packet, packet.length);
			
			while (receivePacket.getAddress() == null)
			{
				try
				{
					clientSocket.send(sendPacket);

					while (probe == null || probe.getType() != Probe.BROAD_CAST_ACK)
					{
						clientSocket.receive(receivePacket);
						probe = NodeDiscovererService.extractProbeFromDatagram(receivePacket);
					}
				}
				catch (SocketTimeoutException e)
				{
					SystemLogger.getLogger().info("Sending Another Packet");
				} 
			}
			
			return receivePacket.getAddress();
		}
		catch (Exception e)
		{
			throw e;
		}	
	}

	public static String getLocalIpAddressAsString() 
	{	
		return NodeDiscovererService.getLocalIpAddress().getHostAddress();
	}
	
	public static InetAddress getLocalIpAddress()
	{
		try
		{
			for (Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements();)
			{
				NetworkInterface networkInterface = interfaces.nextElement();

				if (networkInterface.getDisplayName().equals(PropertyManger.loadProperty(PropertyKeys.NETWORK_INTERFACE, "wlan0")))
				{
					for (Enumeration<InetAddress> addresses = networkInterface.getInetAddresses(); addresses.hasMoreElements();)
					{
						InetAddress address = addresses.nextElement();

						if (address instanceof Inet4Address && !address.isLoopbackAddress())
							return address;
					}
				}
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		
		throw new RuntimeException("Could not find local IP address");
	}
	
	private static class Probe implements Serializable
	{
		public static final int BROAD_CAST = 1;
		public static final int BROAD_CAST_ACK = 2;
		
		private int type;
		private String nodeName;

		public Probe(String name, int type)
		{
			nodeName = name;
			this.type = type;
		}

		public String getNodeName()
		{
			return nodeName;
		}

		public int getType()
		{
			return type;
		}
	}
}
