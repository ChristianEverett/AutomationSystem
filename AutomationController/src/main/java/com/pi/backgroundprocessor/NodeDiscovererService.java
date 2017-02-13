package com.pi.backgroundprocessor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import com.pi.Application;
import com.pi.infrastructure.Device;

public class NodeDiscovererService extends Thread
{
	public static final String AUTOMATION_CONTROLLER = "automation_controller";
	public static final int DISCOVERY_PORT = 9876;

	private static NodeDiscovererService singlton = null;
	private DatagramSocket serverSocket = null;

	public static void startDiscovering()
	{
		if (singlton == null)
		{
			singlton = new NodeDiscovererService();
			singlton.start();
		}
	}

	public static void stopDiscovering()
	{
		if (singlton != null)
		{
			singlton.interrupt();
			singlton.serverSocket.close();
			singlton = null;
		}
	}

	private NodeDiscovererService()
	{
		try
		{
			serverSocket = new DatagramSocket(DISCOVERY_PORT);
		}
		catch (SocketException e)
		{
			Application.LOGGER.severe(e.getMessage());
		}
	}

	@Override
	public void run()
	{
		try
		{
			while (!this.isInterrupted())
			{
				DatagramPacket receivePacket = listenForProbe();
				Probe probe = extractProbeFromDatagram(receivePacket);
						
				Processor.getBackgroundProcessor().registerNode(probe.getNodeName(), receivePacket.getAddress());
				replyToNode(receivePacket.getAddress(), receivePacket.getPort());
			}
		}
		catch (Exception e)
		{
			if (e instanceof InterruptedException)
				Application.LOGGER.info("Stopping Node Discovery");
			else
				Application.LOGGER.severe(e.getMessage());
		}
	}

	private DatagramPacket listenForProbe() throws ClassNotFoundException, IOException
	{
		byte[] packet = new byte[1024];
		DatagramPacket receivePacket = new DatagramPacket(packet, packet.length);
		serverSocket.receive(receivePacket);
		
		return receivePacket;
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

	public static Probe extractProbeFromDatagram(DatagramPacket receivePacket) throws IOException, ClassNotFoundException
	{
		InetAddress IPAddress = receivePacket.getAddress();
		byte[] data = receivePacket.getData();
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		ObjectInputStream objectInput = new ObjectInputStream(in);
		return (Probe) objectInput.readObject();
	}
	
	public static class Probe implements Serializable
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
