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
		
		initilizeIR(PULSELENGTH, IR_PIN);
	}

	@Override
	protected void performAction(DeviceState state)
	{
		int code = (Boolean) state.getParamNonNull(Params.ON) ? ON : OFF;
		
		sendIRSignal(code);
	}

	@Override
	protected void tearDown() throws IOException, InterruptedException
	{
		sendIRSignal(OFF);
	}
	
	private void sendIRSignal(int code)
	{
		isOn.set((code == ON) ? true : false);		
		
		for(int x = 0; x < signalRedundancy; x++)
		{
			send(code);
		}
	}

	@Override
	public DeviceState getState(DeviceState state)
	{
		state.setParam(Params.ON, isOn.get());
		
		return state;
	}
	
	private native void initilizeIR(int pulseLength, int PIN);
	private native void send(int code);
	
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
