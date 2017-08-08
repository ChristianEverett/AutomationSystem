package com.pi.infrastructure.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason="Repository not found")
public class RepositoryDoesNotExistException extends RuntimeException
{
	public RepositoryDoesNotExistException(String repository)
	{
		super("Repository not found - " + repository);
	}
}
