package gov.nist.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MultiValueMapImpl<V> implements MultiValueMap<String, V>, Cloneable {
	private static final long serialVersionUID = 4275505380960964605L;

	// lazy init of the map to reduce memory consumption
	private Map<String, List<V>> map = null;

	public List<V> put(String key, V value) {
		List<V> keyList = null;
		if(map != null) {
			keyList = map.get(key);
		}

		if(keyList == null) {
			keyList = new ArrayList<>();

			getMap().put(key, keyList);
		}

		keyList.add(value);

		return keyList;
	}

	public boolean containsValue(Object value) {
		Set pairs = null;
		if(map != null) {
			pairs = map.entrySet();
		}

		if(pairs == null) {
			return false;
		}

		Iterator pairsIterator = pairs.iterator();
		while(pairsIterator.hasNext()) {
			Map.Entry keyValuePair = (Map.Entry) (pairsIterator.next());
			ArrayList list = (ArrayList) (keyValuePair.getValue());
			if(list.contains(value)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void clear() {
		if(map != null) {
			Set pairs = map.entrySet();
			Iterator pairsIterator = pairs.iterator();

			while(pairsIterator.hasNext()) {
				Map.Entry keyValuePair = (Map.Entry) (pairsIterator.next());
				ArrayList list = (ArrayList) (keyValuePair.getValue());
				list.clear();
			}

			map.clear();
		}
	}

	public Collection values() {
		if(map == null) {
			return new ArrayList();
		}

		ArrayList returnList = new ArrayList(map.size());

		Set pairs = map.entrySet();
		Iterator pairsIterator = pairs.iterator();

		while(pairsIterator.hasNext()) {
			Map.Entry keyValuePair = (Map.Entry) (pairsIterator.next());
			ArrayList list = (ArrayList) (keyValuePair.getValue());

			Object[] values = list.toArray();
			for(int ii = 0; ii < values.length; ii++) {
				returnList.add(values[ii]);
			}
		}

		return returnList;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object clone() {
		MultiValueMapImpl<V> obj = new MultiValueMapImpl<>();
		if(map != null) {
			HashMap<String, List<V>> hashMap = (HashMap<String, List<V>>) this.map;

			obj.map = (HashMap<String, List<V>>) hashMap.clone();
		}

		return obj;
	}

	public int size() {
		if(map == null) {
			return 0;
		}

		return this.map.size();
	}

	public boolean containsKey(Object key) {
		if(map == null) {
			return false;
		}

		return map.containsKey(key);
	}

	public Set<Entry<String, List<V>>> entrySet() {
		if(map == null) {
			return new HashSet<>();
		}

		return map.entrySet();
	}

	public boolean isEmpty() {
		if(map == null) {
			return true;
		}

		return map.isEmpty();
	}

	public Set<String> keySet() {
		if(map == null) {
			return new HashSet<>();
		}

		return this.map.keySet();
	}

	public Object removeKV(String key, V item) {
		if(map == null) {
			return null;
		}

		List<V> list = this.map.get(key);

		if(list == null) {
			return null;
		}

		return list.remove(item);
	}

	public List<V> get(Object key) {
		if(null == map) {
			return null;
		}

		return map.get(key);
	}

	public List<V> put(String key, List<V> value) {
		return this.getMap().put(key,value);
	}

	public List<V> remove(Object key) {
		if(null == map) {
			return null;
		}

		return map.remove(key);
	}

	public void putAll(Map< ? extends String, ? extends List<V>> mapToPut) {
		for(String k : mapToPut.keySet()) {
			List<V> al = new ArrayList<>();

			al.addAll(mapToPut.get(k));
			getMap().put(k, al);
		}
	}

	/**
	 * @return the map
	 */
	public Map<String, List<V>> getMap() {
		if(map == null) {
			map = new HashMap<>(0);
		}

		return map;
	}
}
