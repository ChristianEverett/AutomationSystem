/**
 * 
 */
package com.pi.infrastructure.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import com.pi.services.TaskExecutorService.Task;

/**
 * @author Christian Everett
 * @param <K>
 * @param <V>
 *
 */
public class TaskMap<V>
{
	private HashMap<Integer, V> hashToValueMap = new HashMap<>();
	private HashMap<Integer, Task> hashToTaskMap = new HashMap<>();

	public TaskMap()
	{
		super();
	}

	public boolean put(V value, Task task)
	{
		int hash = value.hashCode();
		if(hashToValueMap.get(hash) == null)
		{
			hashToValueMap.put(hash, value);
			hashToTaskMap.put(hash, task);
			return true;
		}

		return false;
	}

	public V get(Integer key)
	{
		return hashToValueMap.get(key);
	}

	public Task getTaskID(Integer hash)
	{
		return hashToTaskMap.get(hash);
	}
	
	public V delete(Integer hash)
	{
		V value = hashToValueMap.remove(hash);

		if(value != null)
		{
			hashToTaskMap.remove(hash);
		}
		
		return value;
	}

	public boolean update(Integer hash, V item, Task task)
	{
		if(delete(hash) == null)
			return false;
			
		return put(item, task);
	}
	
	public List<V> getAllValues()
	{
		List<V> valueCollection = new ArrayList<>();
		
		for(Iterator<Entry<Integer, V>> iter = hashToValueMap.entrySet().iterator(); iter.hasNext(); )
		{
			Entry<Integer, V> element = iter.next();
			
			valueCollection.add(element.getValue());
		}
		
		return valueCollection;
	}
	
	public Collection<Task> getAllTaskIDs()
	{
		Collection<Task> taskCollection = new ArrayList<>();
		
		hashToTaskMap.entrySet().forEach((item) -> 
		{
			taskCollection.add(item.getValue());
		});
		
		return taskCollection;
	}
	
	public void clear()
	{
		hashToValueMap.clear();
		hashToTaskMap.clear();
	}
}
