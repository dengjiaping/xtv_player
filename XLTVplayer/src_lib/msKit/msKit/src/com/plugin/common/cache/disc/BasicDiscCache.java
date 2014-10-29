package com.plugin.common.cache.disc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import com.plugin.common.cache.disc.naming.FileNameGenerator;
import com.plugin.common.cache.disc.naming.HashCodeFileNameGenerator;
import com.plugin.common.cache.disc.utils.DisCacheUtil;

public abstract class BasicDiscCache<T> implements IDiscCache<T>{

	private static final int INVALID_SIZE = -1;
	
	private static final String ERROR_ARG_NULL = "\"%s\" argument must be not null";
	
	protected File discDir;
	
	protected FileNameGenerator fileNameGenerator;
	
	private Object obj = new Object();
	
	protected final int discCacheSizeLimit;
	
	private final AtomicInteger cacheSize;

	private final Map<File, Long> lastUsageDates = Collections.synchronizedMap(new HashMap<File, Long>());
	
//	public BasicDiscCache(File dir) {
//		this(dir, new HashCodeFileNameGenerator());
//	}
	
	public BasicDiscCache(DiscCacheOption option){
		String dir = option.getDisCachedir();
		FileNameGenerator nameGenerator = option.getNameGenerator();
		int discCacheSize = option.getDiscCacheSize();
		if(dir == null){
			throw new IllegalArgumentException(String.format(ERROR_ARG_NULL, "dir"));
		}
		
		if(nameGenerator == null){
			throw new IllegalArgumentException(String.format(ERROR_ARG_NULL, "FileNameGenerator"));
		}
		
		this.discDir = new File(dir);
		this.fileNameGenerator = nameGenerator;
		this.discCacheSizeLimit = discCacheSize;
		this.cacheSize = new AtomicInteger();
		calculateCacheSizeAndFillUsageMap();
	}
	
	private void calculateCacheSizeAndFillUsageMap() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				int size = 0;
				File[] cachedFiles = discDir.listFiles();
				if (cachedFiles != null) { // rarely but it can happen, don't know why
					for (File cachedFile : cachedFiles) {
						size += getSize(cachedFile);
						lastUsageDates.put(cachedFile, cachedFile.lastModified());
					}
					cacheSize.set(size);
				}
			}
		}).start();
	}
	
	@Override
	public String put(String key, InputStream in) {
		String fileName = fileNameGenerator.generate(key);
		File file = new File(discDir, fileName);
		String fullName = DisCacheUtil.saveFileByStream(in, file);
		updateInfoWithPut(file);
		return fullName;
	}
	
	@Override
	public String put(String key, byte[] bytes) {
		String fileName = fileNameGenerator.generate(key);
		File file = new File(discDir, fileName);
		String fullName = DisCacheUtil.saveFileByBytes(file, bytes);
		updateInfoWithPut(file);
		return fullName;
	}


	@Override
	public boolean remove(String key) {
		String fileName = fileNameGenerator.generate(key);
		synchronized (lastUsageDates) {
			File[] files = discDir.listFiles();
			if (files != null) {
				for (File f : files) {
					if(f.getName().equals(fileName)){
						lastUsageDates.remove(f);
						f.delete();
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public void clear() {
		File[] files = discDir.listFiles();
		if (files != null) {
			for (File f : files) {
				f.delete();
			}
		}
	}
	
	protected int getSize(File file){
		return (int) file.length();
	}
	
	private void updateInfoWithPut(File file){
		int valueSize = getSize(file);
		int curCacheSize = cacheSize.get();

		while (curCacheSize + valueSize > discCacheSizeLimit) {
			int freedSize = removeNext();
			if (freedSize == INVALID_SIZE) break; // cache is empty (have nothing to delete)
			curCacheSize = cacheSize.addAndGet(-freedSize);
		}
		cacheSize.addAndGet(valueSize);

		Long currentTime = System.currentTimeMillis();
		file.setLastModified(currentTime);
		lastUsageDates.put(file, currentTime);
	}
	
	protected void updateInfoWithGet(File file){
		Long currentTime = System.currentTimeMillis();
		file.setLastModified(currentTime);
		lastUsageDates.put(file, currentTime);
	}
	
	
	/** Remove next file and returns it's size */
	private int removeNext() {
		if (lastUsageDates.isEmpty()) {
			return INVALID_SIZE;
		}
		Long oldestUsage = null;
		File mostLongUsedFile = null;
		Set<Entry<File, Long>> entries = lastUsageDates.entrySet();
		synchronized (lastUsageDates) {
			for (Entry<File, Long> entry : entries) {
				if (mostLongUsedFile == null) {
					mostLongUsedFile = entry.getKey();
					oldestUsage = entry.getValue();
				} else {
					Long lastValueUsage = entry.getValue();
					if (lastValueUsage < oldestUsage) {
						oldestUsage = lastValueUsage;
						mostLongUsedFile = entry.getKey();
					}
				}
			}
		}

		int fileSize = 0;
		if (mostLongUsedFile != null) {
			if (mostLongUsedFile.exists()) {
				fileSize = getSize(mostLongUsedFile);
				if (mostLongUsedFile.delete()) {
					lastUsageDates.remove(mostLongUsedFile);
				}
			} else {
				lastUsageDates.remove(mostLongUsedFile);
			}
		}
		return fileSize;
	}
}
