package com.pi.infrastructure.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.UUID;

import com.pi.SystemLogger;
import com.pi.services.NodeDiscovererService;

public abstract class UPNPDevice
{
	protected String persistent_uuid;
	protected String version = "Unspecified, UPnP/1.0, Unspecified";
	protected String headers;
	private String uuid;
	private ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
	
	public UPNPDevice(String persistent_uuid, String headers) throws IOException
	{
		this.persistent_uuid = persistent_uuid;
		this.headers = headers;
		this.uuid = UUID.randomUUID().toString();
		serverSocketChannel.socket().bind(new InetSocketAddress(0));
		serverSocketChannel.configureBlocking(false);
	}

	abstract protected String handleRequest(InetAddress address, String request);
	abstract protected boolean isValidSearch(UPNPPacket result);
	abstract public String getName();

	protected void acceptRequest(ServerSocketChannel serverSocketChannel) throws IOException
	{
		SocketChannel channel;
		
		try
		{
			channel = serverSocketChannel.accept();
		}
		catch (Exception e)
		{
			return;
		}
		
		try(InputStreamReader input = new InputStreamReader(channel.socket().getInputStream());
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(channel.socket().getOutputStream())))
		{
			char[] buf = new char[4096];
			int length = input.read(buf);
			String message = new String(buf, 0, length);
			String response = handleRequest(channel.socket().getInetAddress(), message);
			writer.write(response);
		}
		catch (Exception e)
		{
			throw e;
		}
		finally 
		{
			channel.close();
		}
	}
	
	protected int register(Selector selector) throws ClosedChannelException
	{
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		return serverSocketChannel.socket().getLocalPort();
	}
	
	public String getSearchResponse(UPNPPacket request) throws IOException
	{
		String target = "urn:Belkin:device:**";
		
		StringBuilder message = new StringBuilder();
		message.append("HTTP/1.1 200 OK\r\n");
		message.append("CACHE-CONTROL: max-age=86400\r\n");
		message.append("DATE: " + new Date().toString() + "\r\n");
		message.append("EXT:\r\n");
		message.append("LOCATION: http://" + NodeDiscovererService.getLocalIpAddressAsString() + ":" + serverSocketChannel.socket().getLocalPort() + "/setup.xml\r\n");
		message.append("OPT: \"http://schemas.upnp.org/upnp/1/0/\"; ns=01\r\n");
		message.append("01-NLS:" + uuid + "\r\n");
		message.append("SERVER: " + version + "\r\n");
		message.append("ST: " + target + "\r\n");
		message.append("USN: uuid:" + persistent_uuid + "::" + target + "\r\n");
		message.append(headers + "\r\n");
		message.append("\r\n");

		return message.toString();
	}
	
	protected void createOkResponse(StringBuilder response, int length)
	{
		response.append("HTTP/1.1 200 OK\r\n");
		response.append("CONTENT-LENGTH: " + length + "\r\n");
		response.append("CONTENT-TYPE: text/xml\r\n");
		response.append("DATE: " + new Date() +"\r\n");
		response.append("LAST-MODIFIED: Sat, 01 Jan 2000 00:01:15 GMT\r\n");
		response.append("SERVER: " + version + "\r\n");
		response.append(headers + "\r\n");
		response.append("CONNECTION: close\r\n");
		response.append("\r\n");
	}
	
	public final void close() throws IOException
	{
		serverSocketChannel.close();
	}
}
