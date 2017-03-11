package com.pi.infrastructure;

import java.io.IOException;
import java.util.List;

import com.pi.backgroundprocessor.Processor;
import com.pi.model.DeviceState;

public abstract class AsynchronousDevice extends Device
{
	public AsynchronousDevice(String name) throws IOException
	{
		super(name);
		// TODO Auto-generated constructor stub
	}
}
