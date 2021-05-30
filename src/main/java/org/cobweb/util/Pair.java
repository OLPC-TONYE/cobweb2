package org.cobweb.util;

import java.util.Map.Entry;

public class Pair<K, V> implements Entry<K, V> {

	K key;
	V val;

	public Pair(K key, V val) {
		super();
		this.key = key;
		this.val = val;
	}

	@Override
	public K getKey() {
		return key;
	}

	@Override
	public V getValue() {
		return val;
	}

	@Override
	public V setValue(V value) {
		return val;
	}

	@Override
	public int hashCode() {

		return (key == null ? 0 : key.hashCode()) ^
				(val == null ? 0 : val.hashCode());
	}

	@Override
	public boolean equals(Object obj) {

		if(obj == null || obj.getClass() != getClass())
			return false;

		Pair<K, V> e2 = (Pair<K, V>) obj;

		return (key==null ? e2.getKey()==null : key.equals(e2.getKey())) &&
				(val==null ? e2.getValue()==null : val.equals(e2.getValue()));
	}

}
