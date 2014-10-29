package com.plugin.common.cache;

import java.io.InputStream;


public interface ICacheManager <T> {

    /**
     * 不支持线程同步
     * 
     * @param category
     * @param key
     * @return
     */
    public T getResource(String category, String key);
    
    public T getResourceFromMem(String category, String key);
    
    public String getResourcePath(String category, String key);
    
    /**
     * 不支持线程同步
     * 
     * @param category
     * @param key
     * @param res
     * @return
     */
    public String putResource(String category, String key, T res);
    
    /**
     * 从sourceFile将资源移动到cache中，如果成功返回成功文件的全路径。
     * 注意：成功以后不会将资源放入cache的内存中。同时原来的sourceFile文件会被删除。
     * 
     * 可能存在同步问题.
     * 
     * @param category
     * @param key
     * @param sourceFile
     * @return
     */
    public String putResource(String category, String key, CharSequence sourceFullFile);
    
    /**
     * 从InputStream将资源移动到cache中，如果成功返回成功文件的全路径。
     * 注意：成功以后不会将资源放入cache的内存中。同时原来的InputStream指向的文件不会被移动。
     * 
     * @param category
     * @param key
     * @param is
     * @return
     */
    public String putResource(String category, String key, InputStream is);
    
    public void releaseResource(String category);
    
    public void releaseResource(String category, String key);
    
    public void releaseAllResource();
    
    public void clearResource();
    
    public ICacheStrategy setCacheStrategy(ICacheStrategy strategy);
    
}
