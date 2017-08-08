package com.pi.infrastructure.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN, reason="Device is locked")
public class DeviceLockedException extends RuntimeException
{
	public DeviceLockedException(String deviceName)
	{
		super("Device is lcoked - " + deviceName);
	}
}
