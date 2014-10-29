package com.plugin.common.cache.disc;

import java.io.File;
import java.io.InputStream;

public interface IDiscCache<T> {

	String put(String key, T t);
	
	String put(String key, InputStream in);
	
	String put(String key, byte[] bytes);
	
	String copy(String source, String dest);
	
	T get(String key);
	
	boolean remove(String key);
	
	void clear();
}
