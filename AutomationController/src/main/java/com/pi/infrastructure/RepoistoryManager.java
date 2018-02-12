package com.pi.infrastructure;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.pi.infrastructure.util.RepositoryDoesNotExistException;

@Service
public class RepoistoryManager
{
	@Autowired
	private Map<String, JpaRepository<?, ?>> repositorys;
	
	public Collection<String> getAllRepositorys()
	{
		return repositorys.keySet();
	}
	
	public <T extends Serializable> Collection<T> getRepositoryValues(String type)
	{
		JpaRepository<?, ?> repository = repositorys.get(type);
		
		if(repository == null)
			throw new RepositoryDoesNotExistException(type);
		
		return (Collection<T>) repository.findAll();
	}
	
	public <T extends Serializable, K extends Serializable> T getRepositoryValue(String type, K key)
	{
		JpaRepository<?, K> repository = (JpaRepository<?, K>) repositorys.get(type);
		
		if(repository == null)
			throw new RepositoryDoesNotExistException(type);
		
		return (T) repository.findOne(key);
	}

	public <T extends Serializable, K extends Serializable> void setRepositoryValues(String type, Collection<T> values)
	{
		JpaRepository<T, K> repository = (JpaRepository<T, K>) repositorys.get(type);
		
		if(repository == null)
			throw new RuntimeException("Repository does not exist");
		
		repository.save(values);
	}
	
	public <T extends Serializable, K extends Serializable> void setRepositoryValue(String type, T value)
	{
		JpaRepository<T, K> repository = (JpaRepository<T, K>) repositorys.get(type);
		
		if(repository == null)
			throw new RuntimeException("Repository does not exist");
		
		repository.save(value);
	}
	
	public <T extends Serializable, K extends Serializable> void clearRepositoryValues(String type)
	{
		JpaRepository<T, K> repository = (JpaRepository<T, K>) repositorys.get(type);
		repository.deleteAll();
	}
	
	public <T extends Serializable, K extends Serializable> void clearRepositoryValue(String type, K key)
	{
		JpaRepository<T, K> repository = (JpaRepository<T, K>) repositorys.get(type);
		repository.delete(key);
	}
	
	public void flushAll()
	{
		repositorys.forEach((name, repository) -> 
		{
			repository.flush();
		});
	}
}
