package com.plugin.common.cache.memory.impl;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;

import com.plugin.common.cache.disc.DiscCacheFactory;
import com.plugin.common.cache.disc.DiscCacheOption;
import com.plugin.common.cache.disc.impl.FileDiscCache;
import com.plugin.common.cache.memory.IMemoryCache;
import com.plugin.common.cache.memory.MemoryCacheOption;

public class FileMemoryCache implements IMemoryCache<String, File>{
	
	private static FileMemoryCache gFileMemoryCache;
	
	private FileDiscCache fileDiscCache;
	
	private MemoryCacheOption mOption;
	

	public FileMemoryCache(MemoryCacheOption option) {
        if(option != null){
            String category = option.getFileDefaultCategory();
            fileDiscCache = (FileDiscCache) DiscCacheFactory.getInstance().getFileDiscCache(category);
            this.mOption = option;
        }
	}
	
	@Override
	public boolean put(String category, String key, File value) {
		return false;
	}

	@Override
	public boolean put(String category, String key, InputStream in) {
		fileDiscCache.put(key, in);
		return true;
	}

	@Override
	public boolean put(String category, String key, byte[] bytes) {
		fileDiscCache.put(key, bytes);
		return true;
	}

	@Override
	public File get(String category, String key) {
		return fileDiscCache.get(key);
	}

	@Override
	public void remove(String category, String key) {
		fileDiscCache.remove(key);
	}


	@Override
	public void clear() {
		fileDiscCache.clear();
	}

	@Override
	public boolean put(String category, String key, String sourceFilePath) {
        fileDiscCache.copy(sourceFilePath,key);
		return true;
	}


}
