package com.pi.backgroundprocessor;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import com.pi.Application;

public class NodeDiscovererService extends Thread
{
	public static final int DISCOVERY_PORT = 9876;
	
	private static NodeDiscovererService singlton = null;
	
	public static void startDiscovering()
	{
		if(singlton == null)
		{
			singlton = new NodeDiscovererService();
			singlton.start();
		}
	}
	
	public static void stopDiscovering()
	{
		if(singlton != null)
		{
			singlton.interrupt();
			singlton = null;
		}
	}
	
	private NodeDiscovererService()
	{
	}
	
	@Override
	public void run()
	{
		try(DatagramSocket serverSocket = new DatagramSocket(DISCOVERY_PORT))
		{
			byte[] packet = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(packet, packet.length);
			serverSocket.receive(receivePacket);
			InetAddress IPAddress = receivePacket.getAddress();
			byte[] data = receivePacket.getData();
			ByteArrayInputStream in = new ByteArrayInputStream(data);
			ObjectInputStream objectInput = new ObjectInputStream(in);
			Probe probe = (Probe) objectInput.readObject();
			Processor.getBackgroundProcessor().registerNode(probe.getNodeName(), IPAddress);
		}
		catch (Exception e)
		{
			if(e instanceof InterruptedException)
				Application.LOGGER.info("Stopping Node Discovery");
			else
				Application.LOGGER.severe(e.getMessage());	
		}
	}
	
	public static class Probe implements Serializable
	{
		private String nodeName;
		
		public Probe(String name)
		{
			nodeName = name;
		}

		public String getNodeName()
		{
			return nodeName;
		}
	}
}
