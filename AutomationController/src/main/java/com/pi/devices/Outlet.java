/**
 * 
 */
package com.pi.devices;

import java.io.IOException;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.pi.Application;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType;
import com.pi.model.Action;
import com.pi.model.DeviceState;


/**
 * @author Christian Everett
 *
 */
public class Outlet extends Device
{
	private int headerPin = -1;
	private final int PULSELENGTH = 187;
	private final int ON;
	private final int OFF;
	private final int IR_PIN;
	private int signalRedundancy = 3;
	private boolean state = false;
	
	public Outlet(String name, int headerPin, int onCode, int offCode) throws IOException
	{
		super(name);
		this.ON = onCode;
		this.OFF = offCode;
		this.headerPin = headerPin;
		IR_PIN = pins.get(headerPin).getWiringPI_Pin().getAddress();
	}

	@Override
	public void performAction(Action action)
	{
		int code = Boolean.parseBoolean(action.getData())? ON : OFF;
		
		try
		{
			sendIRSignal(code);
		}
		catch (IOException | InterruptedException e)
		{
			Application.LOGGER.severe(e.getMessage());
		}
	}

	@Override
	public void close()
	{
		try
		{
			sendIRSignal(OFF);
		}
		catch (IOException | InterruptedException e)
		{
			Application.LOGGER.severe(e.getMessage());
		}
	}
	
	private synchronized void sendIRSignal(int code) throws IOException, InterruptedException
	{
		state = (code == ON) ? true : false;		
		
		for(int x = 0; x < signalRedundancy; x++)
		{
			Process process = rt.exec("sudo ./codesend " + code + " -l " + PULSELENGTH + " -p " + IR_PIN);
			process.waitFor();
		}
	}

	@Override
	public DeviceState getState()
	{
		return new OutletState(name, state);
	}

	@Override
	public String getType()
	{
		return DeviceType.OUTLET;
	}
	
	public static class OutletState extends DeviceState
	{
		private boolean deviceOn;
		
		public OutletState(String deviceName, boolean deviceOn)
		{
			super(deviceName);
			this.deviceOn = deviceOn;
		}

		public boolean getDeviceOn()
		{
			return deviceOn;
		}

		@Override
		public String getType()
		{
			return DeviceType.OUTLET;
		}
	}
	
	@XmlRootElement(name = DEVICE)
	public static class OutletConfig extends DeviceConfig
	{
		int header, offCode, onCode;		
		
		@Override
		public Device buildDevice() throws IOException
		{
			return new Outlet(name, header, onCode, offCode);
		}

		@XmlElement
		public void setHeader(int header)
		{
			this.header = header;
		}

		@XmlElement
		public void setOffCode(int offCode)
		{
			this.offCode = offCode;
		}

		@XmlElement
		public void setOnCode(int onCode)
		{
			this.onCode = onCode;
		}
	}
}
