package com.plugin.common.cache.disc;

import java.util.*;

import com.plugin.common.cache.disc.impl.FileDiscCache;
import com.plugin.common.cache.disc.impl.ImageDiscCache;
import com.plugin.common.cache.disc.naming.HashCodeFileNameGenerator;

public class DiscCacheFactory {

	private static DiscCacheFactory gDiscCacheMgr = null;
	
	private Map<String,IDiscCache> discCaches = Collections.synchronizedMap(new HashMap<String, IDiscCache>());
	
	public static DiscCacheFactory getInstance(){
		if(gDiscCacheMgr == null){
			synchronized (DiscCacheFactory.class) {
				if(gDiscCacheMgr == null){
					gDiscCacheMgr = new DiscCacheFactory();
				}
			}
		}
		
		return gDiscCacheMgr;
	}
	
	private DiscCacheFactory() {
		
	}

    public void createImageDefaultDisc(List<String> categories){
        if(categories != null){
            for(String category: categories){
                getImageDiscCache(category);
            }
        }
    }

    public void createFileDefaultDisc(String category){
        if(category != null){
            getFileDiscCache(category);
        }
    }


	
	
    public IDiscCache getFileDiscCache(String category){
        IDiscCache discCache = discCaches.get(category);
        if(discCache == null){
            DiscCacheOption option = new DiscCacheOption(category,new HashCodeFileNameGenerator());
            discCache = new FileDiscCache(option);
            this.discCaches.put(category, discCache);

        }

        return discCache;
    }

    public IDiscCache getImageDiscCache(String category){
        IDiscCache discCache = discCaches.get(category);
        if(discCache == null){
            DiscCacheOption option = new DiscCacheOption(category,new HashCodeFileNameGenerator());
            discCache = new ImageDiscCache(option);
            this.discCaches.put(category, discCache);

        }

        return discCache;
    }
	
	public void clear(){
        Iterator<Map.Entry<String, IDiscCache>> iterator= this.discCaches.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<String, IDiscCache> entry = iterator.next();
            ImageDiscCache discCache = (ImageDiscCache) entry.getValue();
            discCache.clear();
        }

        this.discCaches.clear();
    }
	
	
}
