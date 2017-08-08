package com.pi.model.repository;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public abstract class BaseRepository<K, V> extends ConcurrentHashMap<K, V>
{
	public <T> void put(String key, T value)
	{
		put(key, value);	
	}
}
