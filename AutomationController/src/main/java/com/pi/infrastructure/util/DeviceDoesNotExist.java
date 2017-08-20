package com.pi.infrastructure.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value=HttpStatus.NOT_FOUND, reason="Device does not exist")
public class DeviceDoesNotExist extends RuntimeException
{
	public DeviceDoesNotExist(String deviceName)
	{
		super("Device does not exist: " + deviceName);
	}
}
