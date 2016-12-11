/**
 * 
 */
package com.pi.infrastructure;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * @author Christian Everett
 * @param <K>
 * @param <V>
 *
 */
public class TaskMap<V>
{
	private HashMap<Integer, V> hashToValueMap = new HashMap<>();
	private HashMap<Integer, Integer> hashToTaskMap = new HashMap<>();

	public TaskMap()
	{
		super();
	}

	public boolean put(V value, Integer task)
	{
		int hash = value.hashCode();
		if(hashToValueMap.get(hash) != null)
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

	public Integer getTaskID(Integer hash)
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

	public boolean update(Integer hash, V item, Integer task)
	{
		if(delete(hash) == null)
			return false;
			
		return put(item, task);
	}
	
	public Collection<Entry<Integer, V>> getAllValues()
	{
		Collection<Entry<Integer, V>> valueCollection = new ArrayList<>();
		
		for(Iterator<Entry<Integer, V>> iter = hashToValueMap.entrySet().iterator(); iter.hasNext(); )
		{
			Entry<Integer, V> element = iter.next();
			
			valueCollection.add(new AbstractMap.SimpleEntry<Integer, V>(element.hashCode(), element.getValue()));
		}
		
		return valueCollection;
	}
	
	public Collection<Integer> getAllTaskIDs()
	{
		Collection<Integer> taskCollection = new ArrayList<>();
		
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
