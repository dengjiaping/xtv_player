/**
 * ICacheStrategy.java
 */
package com.plugin.common.cache;

/**
 * @author Guoqing Sun Mar 11, 20136:09:08 PM
 */
public interface ICacheStrategy {

    String onMakeFileKeyName(String category, String key);
    
	String onMakeImageCacheFullPath(String rootPath, String key, String ext);
	
}
