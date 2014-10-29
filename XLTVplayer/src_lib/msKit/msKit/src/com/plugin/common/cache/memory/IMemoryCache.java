package com.plugin.common.cache.memory;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

public interface IMemoryCache<K,V> {

	boolean put(K category, K key,V value);
	
	boolean put(K category, K key, InputStream in);
	
	boolean put(K category, K key, byte[] bytes);
	
	boolean put(K category, K key, String sourceFilePath);
	
	V get(K category, K key);
	
	void remove(K category, K key);
	
	void clear();
}
