package com.plugin.common.cache.memory.impl;

import com.plugin.common.cache.disc.DiscCacheFactory;
import com.plugin.common.cache.disc.DiscCacheOption;
import com.plugin.common.cache.disc.impl.FileDiscCache;
import com.plugin.common.cache.memory.MemoryCacheOption;

public class ImageMemoryCache extends BaseImageMemoryCache{

	private static ImageMemoryCache gImageMemoryCache;
	
	private FileDiscCache fileDiscCache;
	
	private MemoryCacheOption mOption;
	
	public static ImageMemoryCache getInstance(MemoryCacheOption option){
		if(gImageMemoryCache == null){
			synchronized (ImageMemoryCache.class) {
				if(gImageMemoryCache == null){
					return new ImageMemoryCache(option);
				}
			}
		}
		
		return gImageMemoryCache;
	}
	
	
	private ImageMemoryCache(MemoryCacheOption option) {
		super(option);
		this.mOption = option;
	}
	

}
