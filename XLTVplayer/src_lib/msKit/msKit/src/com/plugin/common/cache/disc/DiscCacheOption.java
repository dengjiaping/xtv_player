package com.plugin.common.cache.disc;

import java.io.File;

import android.os.Environment;

import com.plugin.common.cache.disc.naming.FileNameGenerator;
import com.plugin.common.cache.disc.naming.HashCodeFileNameGenerator;

public class DiscCacheOption {

    //TODO 路径还要修改
    public DiscCacheOption(String category, FileNameGenerator nameGenerator) {
        this.category = category;
        this.disCachedir = discRootPath + category + "/";
        this.nameGenerator = nameGenerator;
    }


//	public DiscCacheOption(String category, String discDir, FileNameGenerator nameGenerator) {
//		this.category = category;
//		this.disCachedir = discDir;
//		this.nameGenerator = nameGenerator;
//	}
	
    private String discRootPath = Environment.getExternalStorageState()+ "/";
    
    public String getDiscRootPath() {
		return discRootPath;
	}
    
    public void setDiscRootPath(String rootPath) {
		this.discRootPath = rootPath;
	}
	
	
	public static String CATEGORY_FILE = "disc_cache_category_file";
	
	public static String CATEGORY_IMAGE = "disc_cache_category_image";
	
	private String category;
	
	public String getCategory() {
		return category;
	}
	
	public void setCategory(String category) {
		this.category = category;
	}
	
	/**
	 * disc cache 路径
	 */
	private String disCachedir;
	
	public String getDisCachedir() {
		return this.disCachedir;
	}
	
	public void setDisCachedir(String disCachedir) {
		this.disCachedir = disCachedir;
	}
	
	/**
	 * disc cache 文件名生成规则,默认为hashcode
	 */
	private FileNameGenerator nameGenerator = new HashCodeFileNameGenerator();
	
	public void setNameGenerator(FileNameGenerator nameGenerator) {
		this.nameGenerator = nameGenerator;
	}
	
	public FileNameGenerator getNameGenerator() {
		return nameGenerator;
	}
	
	/**
	 * Disc Cache IO stream default buffer size
	 */
	public static final int BUFFER_SIZE = 32 * 1024; // 32 KB
	
	
	/**
	 * disc Cache Size （unit by MB）,default is 10MB
	 */
	public int discCacheSize = 10;
	
	public static final int cacheSizeMBUnit = 1024 * 1024;
	
	public void setDiscCacheSize(int discCacheSizeInMB) {
		this.discCacheSize = discCacheSizeInMB * cacheSizeMBUnit;
	}
	
	public int getDiscCacheSize() {
		return discCacheSize;
	}

	/**
	 * 保存bitmap压缩率
	 */
	public static final int BITMAP_COMPRESS_LOW = 80;
    public static final int BITMAP_COMPRESS_MEDIUM = 90;
    public static final int BITMAP_COMPRESS_HIGH = 100;
    
    public final static int MAX_BITMAP_WIDTH = 2048;

    public final static int MAX_BITMAP_HEIGHT = 2048;
    
    /**
     * 最大为720的bitmap的大小
     */
    public static final long MAX_MEMORY_SIZE = 720 * 1028 * 4;

    
    
	
}
