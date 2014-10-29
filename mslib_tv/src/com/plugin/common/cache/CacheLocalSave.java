package com.plugin.common.cache;

import android.text.TextUtils;

class CacheLocalSave {
    
    private String mType;
    
    CacheLocalSave(String type) {
        if (TextUtils.isEmpty(type)) {
            throw new IllegalArgumentException("argument can't be empty");
        }
        
        mType = type;
    }
    
    boolean saveToLocal(String category, String key, String value) {
    	return false;
    }
    
    String loadFromLocal(String category, String key) {
    	return null;
    }
    
    void clearLocal() {
    }
}
