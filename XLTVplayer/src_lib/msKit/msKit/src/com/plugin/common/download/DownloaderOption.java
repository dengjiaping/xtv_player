package com.plugin.common.download;

import android.os.Environment;
import com.plugin.common.cache.memory.MemoryCacheOption;

public class DownloaderOption {

    public static  String DEFAULT_DOWNLOAD_TEMP_PATH = Environment.getExternalStorageDirectory()+"/";

	private  String downloadTmpPath;
	
	public String getDownloadTmpPath() {
		return downloadTmpPath;
	}
	
	public void setDownloadTmpPath(String downloadTmpPath) {
		this.downloadTmpPath = downloadTmpPath;
	}
	
	private boolean isStop;
	
	public boolean isStop() {
		return isStop;
	}
	
	public void setStop(boolean isStop) {
		this.isStop = isStop;
	}
	
	public static final int DEFAULT_KEEPALIVE = 5 * 1000;
	
	private int keepAlive = DEFAULT_KEEPALIVE;
	
	public int getKeepAlive() {
		return keepAlive;
	}
	
	public void setKeepAlive(int keepAlive) {
		this.keepAlive = keepAlive;
	}
	
	private MemoryCacheOption memoryOption;
	
	public MemoryCacheOption getMemoryOption() {
		return memoryOption;
	}
	
	public void setMemoryOption(MemoryCacheOption memoryOption) {
		this.memoryOption = memoryOption;
	}

    public static DownloaderOption getDownloaderDefaultOpt(){
        DownloaderOption opt = new DownloaderOption();
        opt.setDownloadTmpPath(DEFAULT_DOWNLOAD_TEMP_PATH);
        opt.setStop(false);
        opt.setKeepAlive(DEFAULT_KEEPALIVE);
        opt.setMemoryOption(MemoryCacheOption.getDefaultMemoryCacheOpt());
        return opt;
    }
}
