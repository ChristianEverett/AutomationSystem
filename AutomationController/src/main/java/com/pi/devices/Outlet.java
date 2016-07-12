/**
 * 
 */
package com.pi.devices;

import java.io.IOException;

import com.pi.Application;
import com.pi.infrastructure.Device;
import com.pi.repository.Action;


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
	private int signalRedundancy = 2;
	
	public Outlet(int headerPin, int onCode, int offCode) throws IOException
	{
		this.ON = onCode;
		this.OFF = offCode;
		this.headerPin = headerPin;
		IR_PIN = pins.get(headerPin).getWiringPI_Pin().getAddress();
	}

	@Override
	public boolean performAction(Action action) throws IOException, InterruptedException
	{
		if(isClosed)
			return false;
		
		int code = Boolean.parseBoolean(action.getData())? ON : OFF;
		
		sendIRSignal(code);
		
		return true;
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
	
	private void sendIRSignal(int code) throws IOException, InterruptedException
	{
		for(int x = 0; x < signalRedundancy; x++)
		{
			Process process = rt.exec("sudo ./codesend " + code + " -l " + PULSELENGTH + " -p " + IR_PIN);
			process.waitFor();
		}
	}
}
