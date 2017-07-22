package com.pi.infrastructure.util;

import java.net.InetAddress;

public class UPNPPacket
{
	private InetAddress address;
	private int port;
	private String data;

	public UPNPPacket(InetAddress address, int port, String data)
	{
		this.address = address;
		this.data = data;
		this.port = port;
	}

	public InetAddress getAddress()
	{
		return address;
	}

	public String getData()
	{
		return data;
	}

	public int getPort()
	{
		return port;
	}
}
