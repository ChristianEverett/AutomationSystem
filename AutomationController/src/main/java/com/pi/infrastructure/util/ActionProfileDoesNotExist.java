package com.pi.infrastructure.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value=HttpStatus.BAD_REQUEST)
public class ActionProfileDoesNotExist extends RuntimeException
{
	public ActionProfileDoesNotExist(String profileName)
	{
		super("No Profile by name of: " + profileName);
	}
}
