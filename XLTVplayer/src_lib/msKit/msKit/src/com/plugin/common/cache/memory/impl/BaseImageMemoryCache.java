package com.plugin.common.cache.memory.impl;

import java.io.BufferedInputStream;
import java.io.InputStream;

import com.plugin.common.cache.disc.DiscCacheFactory;
import com.plugin.common.cache.disc.impl.ImageDiscCache;
import com.plugin.common.cache.memory.IMemoryCache;
import com.plugin.common.cache.memory.MemoryCacheOption;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;
import android.text.TextUtils;

public class BaseImageMemoryCache implements IMemoryCache<String, Bitmap> {


	protected LruCache<String, Bitmap> lruCache;
	
	private boolean autoSave2Disk;
	
	private boolean autoFetchFromDisk;


	protected BaseImageMemoryCache(MemoryCacheOption option) {
		this.lruCache = new LruCache<String, Bitmap>(
				option.getMaxMemoryCacheSize()) {
			@Override
			protected int sizeOf(String key, Bitmap bmp) {
				return bmp.getRowBytes() * bmp.getHeight();
			}
		};

        //初始化四个imageDiscCache
        DiscCacheFactory.getInstance().createImageDefaultDisc(option.getImageDefaultCategories());
		
		autoSave2Disk = option.isAutoSave2Disk();
		autoFetchFromDisk = option.isAutoFetchFromDisk();
	}

	@Override
	public boolean put(String category, String key, Bitmap value) {
		if(TextUtils.isEmpty(category)|| TextUtils.isEmpty(key) || value == null){
			return false;
		}
        String formatKey = makeFileKeyName(category, key);
		this.lruCache.put(formatKey, value);
		save2Disc(autoSave2Disk,category,key,value);
		return true;
	}

	@Override
	public boolean put(String category, String key, InputStream in) {
		if(TextUtils.isEmpty(category)|| TextUtils.isEmpty(key) || in == null){
			return false;
		}
        String formatKey = makeFileKeyName(category, key);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(in);
		Bitmap bmp = BitmapFactory.decodeStream(bufferedInputStream);
		if(bmp != null){
			this.lruCache.put(formatKey, bmp);

            save2Disc(autoSave2Disk,category,key,in);

            return true;
		}
		return false;
	}

	@Override
	public boolean put(String category, String key, byte[] bytes) {
		if(TextUtils.isEmpty(category)|| TextUtils.isEmpty(key) || bytes == null){
			return false;
		}
        String formatKey = makeFileKeyName(category, key);
        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
		if(bmp != null){
			this.lruCache.put(formatKey, bmp);

            save2Disc(autoSave2Disk,category,key,bytes);
            return true;
		}
		
		return false;
	}
	
	@Override
	public boolean put(String category, String key, String sourceFilePath) {
	    if(TextUtils.isEmpty(category)|| TextUtils.isEmpty(key)|| TextUtils.isEmpty(sourceFilePath)){
            return false;
        }
        Bitmap bmp = get(category,key);
        if(bmp != null){
        	return true;
        }
        String formatKey = makeFileKeyName(category, key);
        ImageDiscCache imageCache = (ImageDiscCache) DiscCacheFactory.getInstance().getImageDiscCache(category);
        String path = imageCache.copy(sourceFilePath, formatKey);
		if(path != null){
			return true;
		}
        return false;
	}

	@Override
	public Bitmap get(String category, String key) {
		if (!TextUtils.isEmpty(category) && !TextUtils.isEmpty(key)) {
			Bitmap bmp = lruCache.get(key);
			
			if(bmp == null && autoFetchFromDisk){
				bmp = fetchFromDisc(category,key);
				if(bmp != null){
					put(category, key, bmp);
				}
			}
			return bmp;
		}
		return null;
	}

	@Override
	public void remove(String category, String key) {
		if(!TextUtils.isEmpty(category) && !TextUtils.isEmpty(key)){
			lruCache.remove(key);
		}
	}


	@Override
	public void clear() {
		lruCache.evictAll();
        DiscCacheFactory.getInstance().clear();
	}

    protected String makeFileKeyName(String category, String key) {
        StringBuilder sb = new StringBuilder(256);
        sb.append(category).append("/").append(key);
        return sb.toString();
    }

    private void save2Disc(boolean autoSave2Disk, String category, String key, Bitmap value){
        if(autoSave2Disk){
            ImageDiscCache imageCache = (ImageDiscCache) DiscCacheFactory.getInstance().getImageDiscCache(category);

            if(imageCache != null){
                imageCache.put(key,value);
                return;
            }
        }
    }

    private void save2Disc(boolean autoSave2Disk, String category, String key, byte[] bytes){
        if(autoSave2Disk){
            ImageDiscCache imageCache = (ImageDiscCache) DiscCacheFactory.getInstance().getImageDiscCache(category);
            if(imageCache != null){
                imageCache.put(key,bytes);
                return;
            }

        }
    }

    private void save2Disc(boolean autoSave2Disk, String category, String key, InputStream in){
        if(autoSave2Disk){
            ImageDiscCache imageCache = (ImageDiscCache) DiscCacheFactory.getInstance().getImageDiscCache(category);
            if(imageCache != null){
                imageCache.put(key,in);
                return;
            }

        }
    }

    private Bitmap fetchFromDisc(String category, String key){
        ImageDiscCache imageCache = (ImageDiscCache) DiscCacheFactory.getInstance().getImageDiscCache(category);
            if(imageCache != null){
                return imageCache.get(key);
            }
            return null;
    }


}
