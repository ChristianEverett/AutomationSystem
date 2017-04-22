/**
 * 
 */
package com.pi.devices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.pi.Application;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType;
import com.pi.infrastructure.DeviceType.Params;
import com.pi.infrastructure.util.GPIO_PIN;
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
	private AtomicBoolean isOn = new AtomicBoolean(false);
	
	public Outlet(String name, int headerPin, int onCode, int offCode) throws IOException
	{
		super(name);
		this.ON = onCode;
		this.OFF = offCode;
		this.headerPin = headerPin;
		IR_PIN = GPIO_PIN.getWiringPI_Pin(headerPin).getAddress();
	}

	@Override
	protected void performAction(DeviceState state)
	{
		int code = (boolean) state.getParam(Params.IS_ON) ? ON : OFF;
		
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
	protected void tearDown()
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
		isOn.set((code == ON) ? true : false);		
		
		for(int x = 0; x < signalRedundancy; x++)
		{
			Process process = rt.exec("sudo ./codesend " + code + " -l " + PULSELENGTH + " -p " + IR_PIN);
			process.waitFor();
		}
	}

	@Override
	public DeviceState getState(Boolean forDatabase)
	{
		DeviceState state = Device.createNewDeviceState(name);
		state.setParam(Params.IS_ON, isOn.get());
		
		return state;
	}
	
	@Override
	public List<String> getExpectedParams()
	{
		List<String> list = new ArrayList<>();
		list.add(Params.IS_ON);
		return list;
	}

	@Override
	public String getType()
	{
		return DeviceType.OUTLET;
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
