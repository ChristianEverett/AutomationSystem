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

/**
 * @author Christian Everett
 * @param <K>
 * @param <V>
 *
 */
public class UniqueMultiMap<K, V>
{
	private static final long serialVersionUID = 1L;
	private HashMap<K, List<V>> map = new HashMap<>();
	private HashMap<Integer, MapKey> hashToIndexMap = new HashMap<>();

	public UniqueMultiMap()
	{
		super();
	}

	public boolean put(K key, V value)
	{
		List<V> colleciton = map.get(key);

		if (colleciton == null)
		{
			colleciton = new ArrayList<>();
			map.put(key, colleciton);
		}

		// Contains uses equals for comparison
		if (colleciton.contains(value))
		{
			return false;
		}
		else
		{
			boolean changed = colleciton.add(value);
			if(changed)
				hashToIndexMap.put(value.hashCode(), new MapKey(key, colleciton.size() - 1));
			return changed;
		}
	}

	public Collection<V> get(K key)
	{
		return map.get(key);
	}

	public V delete(Integer hash)
	{
		MapKey mapKey = hashToIndexMap.remove(hash);

		if(mapKey != null)
		{
			return map.get(mapKey.getMutliMapKey()).remove(mapKey.getCollectionOffset());
		}
		
		return null;
	}

	public V getByHash(Integer hash)
	{
		MapKey mapKey = hashToIndexMap.get(hash);

		if(mapKey != null)
		{
			return map.get(mapKey.getMutliMapKey()).get(mapKey.getCollectionOffset());
		}
		
		return null;
	}

	public Collection<Entry<Integer, V>> getAllValues()
	{
		Collection<Entry<Integer, V>> valueCollection = new ArrayList<>();
		
		for(Iterator<Entry<K, List<V>>> iter = map.entrySet().iterator(); iter.hasNext(); )
		{
			Entry<K, List<V>> element = iter.next();
			
			for (V value : element.getValue())
			{
				valueCollection.add(new AbstractMap.SimpleEntry<Integer, V>(value.hashCode(), value));
			}
		}
		
		return valueCollection;
	}
	
	public boolean updateByHash(Integer hash, V value)
	{
		MapKey mapKey = hashToIndexMap.get(hash);
		
		if(mapKey == null)
			return false;
		
		map.get(mapKey.getMutliMapKey()).set(mapKey.collectionOffset, value);
		return true;
	}
	
	public void clear()
	{
		map.clear();
		hashToIndexMap.clear();
	}
	
	private class MapKey
	{
		private K mutliMapKey;
		private int collectionOffset;
		
		public MapKey(K mutliMapKey, int collectionOffset)
		{
			this.mutliMapKey = mutliMapKey;
			this.collectionOffset = collectionOffset;
		}

		/**
		 * @return the mutliMapKey
		 */
		public K getMutliMapKey()
		{
			return mutliMapKey;
		}

		/**
		 * @return the collectionOffset
		 */
		public int getCollectionOffset()
		{
			return collectionOffset;
		}
	}
}
