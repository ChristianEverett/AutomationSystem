package com.pi.infrastructure.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.UUID;

public abstract class UPNPDevice
{
	protected String persistent_uuid;
	protected String version = "Unspecified, UPnP/1.0, Unspecified";
	protected String headers;
	private String uuid;
	private ServerSocket serverSocket;
	protected UPNPBroadcastResponderService responder;

	public UPNPDevice(UPNPBroadcastResponderService responder, String persistent_uuid, String headers) throws IOException
	{
		this.persistent_uuid = persistent_uuid;
		this.headers = headers;
		this.uuid = UUID.randomUUID().toString();
		this.responder = responder;
		this.serverSocket = new ServerSocket(0);
		responder.addDevice(this);
	}

	abstract protected String handleRequest(InetAddress address, String request) throws IOException;
	abstract protected boolean isValidSearch(UPNPPacket result);

	public void listen() throws IOException
	{
		Socket connection;
		
		try
		{
			connection = serverSocket.accept();
		}
		catch (Exception e)
		{
			return;
		}

		try(InputStreamReader input = new InputStreamReader(connection.getInputStream());
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream())))
		{
			char[] buf = new char[4096];
			int length = input.read(buf);
			String message = new String(buf, 0, length);
			
			String response = handleRequest(connection.getInetAddress(), message);
			
			writer.write(response);
		}
		catch (Exception e) 
		{
			throw e;
		}
		finally 
		{
			connection.close();
		}	
	}

	public String getSearchResponse(UPNPPacket request) throws IOException
	{
		String target = "urn:Belkin:device:**";
		
		StringBuilder message = new StringBuilder();
		message.append("HTTP/1.1 200 OK\r\n");
		message.append("CACHE-CONTROL: max-age=86400\r\n");
		message.append("DATE: " + new Date().toString() + "\r\n");
		message.append("EXT:\r\n");
		message.append("LOCATION: http://" + UPNPBroadcastResponderService.getLocalIpAddress().getHostAddress() + ":" + serverSocket.getLocalPort() + "/setup.xml\r\n");
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
		responder.removeDevice(this);
		serverSocket.close();
	}
}
