package com.plugin.common.cache;

import java.io.InputStream;
import java.util.HashMap;

import android.text.TextUtils;

class StringCacheManager implements ICacheManager<String> {

    private static final boolean SUPPORT_LOCAL_SAVE = true;

    private HashMap<String, HashMap<String, String>> mDataCache;
    private CacheLocalSave mCacheLocalSave;

    private static Object mObj = new Object();

    private static StringCacheManager gCacheManager;

    public static StringCacheManager getInstance() {
        if (gCacheManager == null) {
            synchronized (mObj) {
                if (gCacheManager == null) {
                    gCacheManager = new StringCacheManager();
                }
            }
        }

        return gCacheManager;
    }

    private StringCacheManager() {
        mDataCache = new HashMap<String, HashMap<String, String>>();
        mCacheLocalSave = new CacheLocalSave(String.class.getName());
    }

    @Override
    public String getResource(String category, String key) {
        if (TextUtils.isEmpty(category) || TextUtils.isEmpty(key)) {
            return null;
        }

        String ret = getResourceInternal(category, key);
        if (SUPPORT_LOCAL_SAVE && ret == null) {
            ret = mCacheLocalSave.loadFromLocal(category, key);
            if (ret != null) {
                this.putResource(category, key, ret);
            }
        }

        return ret;
    }
    
    @Override
    public String getResourceFromMem(String category, String key) {
        if (TextUtils.isEmpty(category) || TextUtils.isEmpty(key)) {
            return null;
        }

        String ret = getResourceInternal(category, key);
        
        return ret;
    }
    

    private String getResourceInternal(String category, String key) {
        synchronized (mObj) {
            if (mDataCache.containsKey(category)) {
                if (mDataCache.get(category).containsKey(key)) {
                    return mDataCache.get(category).get(key);
                }
            }
        }
        return null;
    }

    @Override
    public String putResource(String category, String key, CharSequence res) {
        if (TextUtils.isEmpty(category) || TextUtils.isEmpty(key) || TextUtils.isEmpty(res)) {
            return null;
        }

        synchronized (mObj) {
            HashMap<String, String> map = mDataCache.get(category);
            if (map == null) {
                map = new HashMap<String, String>();
                mDataCache.put(category, map);
            }
            map.put(key, res.toString());
        }

        if (SUPPORT_LOCAL_SAVE) {
            mCacheLocalSave.saveToLocal(category, key, res.toString());
        }

        return null;
    }
    
    @Override
    public String putResource(String category, String key, InputStream is) {
        return null;
    }
    
    @Override
    public void releaseResource(String category) {
        if (!TextUtils.isEmpty(category))
            mDataCache.remove(category);
    }
    
    @Override
    public void releaseAllResource() {
    }

    @Override
    public void releaseResource(String category, String key) {

    }

    @Override
    public void clearResource() {
        mDataCache.clear();

        if (SUPPORT_LOCAL_SAVE) {
            mCacheLocalSave.clearLocal();
        }
    }

	@Override
	public String getResourcePath(String category, String key) {
		// TODO
		return null;
	}

    /* (non-Javadoc)
     * @see com.sound.dubbler.cache.ICacheManager#putResource(java.lang.String, java.lang.String, java.lang.Object)
     */
    @Override
    public String putResource(String category, String key, String res) {
        // TODO Auto-generated method stub
        return null;
    }

	/* (non-Javadoc)
	 * @see com.plugin.cache.ICacheManager#setCacheStrategy(com.plugin.cache.ICacheStrategy)
	 */
	@Override
	public ICacheStrategy setCacheStrategy(ICacheStrategy strategy) {
	    return null;
	}

}
