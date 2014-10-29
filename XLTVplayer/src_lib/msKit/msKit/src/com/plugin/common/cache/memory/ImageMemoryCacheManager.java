package com.plugin.common.cache.memory;

import java.io.File;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.text.TextUtils;

import com.plugin.common.cache.disc.naming.HashCodeFileNameGenerator;
import com.plugin.common.cache.memory.impl.ImageMemoryCache;

public class ImageMemoryCacheManager {

	private static ImageMemoryCacheManager gMemoryCacheMgr = null;

	private ImageMemoryCache imageMemoryCache;
	

	private ImageMemoryCacheManager(MemoryCacheOption option) {
		imageMemoryCache = ImageMemoryCache.getInstance(option);
	}
	

	public static ImageMemoryCacheManager getIntance(MemoryCacheOption option) {
		if (gMemoryCacheMgr == null) {
			synchronized (ImageMemoryCacheManager.class) {
				if (gMemoryCacheMgr == null) {
					gMemoryCacheMgr = new ImageMemoryCacheManager(option);
				}
			}
		}

		return gMemoryCacheMgr;
	}



    public boolean put(String category, String key, String bmp) {
        if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(key) && bmp != null) {
            imageMemoryCache.put(category, key, bmp);
            return true;
        }

        return false;

    }

    public boolean put(String category, String key, Bitmap in) {
        if (!TextUtils.isEmpty(category) && !TextUtils.isEmpty(key) && in != null) {
            imageMemoryCache.put(category, key, in);
            return false;
        }
        return false;
    }

    public boolean put(String category, String key, byte[] bytes) {
        if (!TextUtils.isEmpty(category) && !TextUtils.isEmpty(key) && bytes != null) {
            imageMemoryCache.put(category, key, bytes);
            return false;
        }

        return false;
    }

    public Bitmap get(String category, String key) {
        if (!TextUtils.isEmpty(category) && !TextUtils.isEmpty(key)) {
            Bitmap bmp = imageMemoryCache.get(category, key);
            return bmp;
        }
        return null;
    }

    public void remove(String category, String key) {
        if (!TextUtils.isEmpty(category) && !TextUtils.isEmpty(key)) {
            imageMemoryCache.remove(category ,key);
        }
    }

    public void clear() {
        this.imageMemoryCache.clear();
    }

}
