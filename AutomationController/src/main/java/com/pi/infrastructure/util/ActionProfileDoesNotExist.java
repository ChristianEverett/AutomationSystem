package com.pi.infrastructure.util;

public class ActionProfileDoesNotExist extends RuntimeException
{
	public ActionProfileDoesNotExist(String message)
	{
		super(message);
	}
}
