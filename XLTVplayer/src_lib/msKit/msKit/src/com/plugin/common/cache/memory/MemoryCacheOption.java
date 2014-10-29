package com.plugin.common.cache.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.plugin.common.cache.disc.DiscCacheOption;
import com.plugin.common.cache.disc.naming.HashCodeFileNameGenerator;

public class MemoryCacheOption {

	/**
	 * memory cache size, united by MB
	 */
	private int maxMemoryCacheSize;

	private static final int MB = 1024 * 1024;

	public int getMaxMemoryCacheSize() {
		return maxMemoryCacheSize;
	}

	/**
	 * 设置memoryCache 大小
	 * 
	 * @param maxMemoryCacheSize
	 *            unite by MB
	 */
	public void setMaxMemoryCacheSize(int maxMemoryCacheSize) {
		this.maxMemoryCacheSize = MB * maxMemoryCacheSize;
	}

	/**
	 * memory Cache category
	 */
    public static final String CACHE_CATEGORY_FILE = "cache_category_file";
	public static final String IMAGE_CACHE_CATEGORY_USER_HEAD_ROUNDED = "user_head_rounded";
	public static final String IMAGE_CACHE_CATEGORY_RAW = "image_cache_category_source";
	public static final String IMAGE_CACHE_CATEGORY_THUMB = "image_cache_category_thumb";
	public static final String IMAGE_CACHE_CATEGORY_SMALL = "image_cache_category_small";

    private List<String> defaultCategories = new ArrayList<String>();

	public MemoryCacheOption() {
        createImageDefaultCategory();
	}


    private void createImageDefaultCategory(){
        this.defaultCategories.add(IMAGE_CACHE_CATEGORY_SMALL);
        this.defaultCategories.add(IMAGE_CACHE_CATEGORY_RAW);
        this.defaultCategories.add(IMAGE_CACHE_CATEGORY_THUMB);
        this.defaultCategories.add(IMAGE_CACHE_CATEGORY_USER_HEAD_ROUNDED);
    }

    public List<String> getImageDefaultCategories(){
        return defaultCategories;
    }

    public String getFileDefaultCategory(){
        return CACHE_CATEGORY_FILE;
    }

	/**
	 * 是否自动写disc
	 */
	private boolean autoSave2Disk;

	public boolean isAutoSave2Disk() {
		return autoSave2Disk;
	}

	public void setAutoSave2Disk(boolean autoSave2Disk) {
		this.autoSave2Disk = autoSave2Disk;
	}

	/**
	 * 是否自动从disc读取
	 */
	private boolean autoFetchFromDisk;

	public boolean isAutoFetchFromDisk() {
		return autoFetchFromDisk;
	}

	public void setAutoFetchFromDisk(boolean autoFetchFromDisk) {
		this.autoFetchFromDisk = autoFetchFromDisk;
	}

    public static MemoryCacheOption getDefaultMemoryCacheOpt(){
        MemoryCacheOption opt = new MemoryCacheOption();
        opt.setMaxMemoryCacheSize(20);
        opt.setAutoSave2Disk(true);
        opt.setAutoFetchFromDisk(true);
        return opt;
    }

}
