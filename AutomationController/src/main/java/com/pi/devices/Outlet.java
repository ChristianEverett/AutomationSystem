/**
 * 
 */
package com.pi.devices;

import java.io.IOException;

import com.pi.Application;
import com.pi.infrastructure.Device;
import com.pi.model.Action;


/**
 * @author Christian Everett
 *
 */
public class Outlet extends Device
{
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
		if(isClosed)
			return;
		
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
			isClosed = true;
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
	public Action getState()
	{
		if(isClosed)
			return null;

		return new Action(name, String.valueOf(state));
	}
}
