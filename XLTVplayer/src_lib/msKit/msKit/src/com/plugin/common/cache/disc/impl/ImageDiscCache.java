package com.plugin.common.cache.disc.impl;

import java.io.File;

import android.graphics.Bitmap;

import com.plugin.common.cache.disc.BasicDiscCache;
import com.plugin.common.cache.disc.DiscCacheOption;
import com.plugin.common.cache.disc.IDiscCache;
import com.plugin.common.cache.disc.naming.FileNameGenerator;
import com.plugin.common.cache.disc.naming.HashCodeFileNameGenerator;
import com.plugin.common.cache.disc.utils.DisCacheUtil;
import com.plugin.common.utils.LogUtil;

public class ImageDiscCache extends BasicDiscCache<Bitmap> {


	public ImageDiscCache(DiscCacheOption option) {
		super(option);
	}

	@Override
	public String put(String key, Bitmap src) {
		String fileName = fileNameGenerator.generate(key);
		File file = new File(discDir, fileName);
		if(DisCacheUtil.compressBitmapToFile(src, file)){
			return file.getAbsolutePath();
		}
		return null;
	}

	@Override
	public Bitmap get(String key) {
		String fileName = fileNameGenerator.generate(key);
		File bmpFile =  new File(discDir, fileName);
		if (!bmpFile.exists()) {
            if (LogUtil.UTILS_DEBUG) {
                LogUtil.LOGD("[[getBitmapFromDiskWithReuseBitmap]] file name = " + bmpFile.getName() + " <<false>>");
            }
            return null;
        }
		
		updateInfoWithGet(bmpFile);
		
		Bitmap bmp = DisCacheUtil.loadBitmapWithSizeOrientation(bmpFile);
		
		 if (LogUtil.UTILS_DEBUG) {
			 LogUtil.LOGD("[[getBitmapFromDiskWithReuseBitmap]] file name = " + bmpFile.getName() + " <<true>>");
		 }
	    return bmp;
	}

	@Override
	public String copy(String source, String dest) {
		String hashKey = fileNameGenerator.generate(dest);
        String fullFileName = this.discDir.getAbsolutePath() + hashKey;
        File file = new File(source);
        if(file.exists()){
        	return DisCacheUtil.copyFile(source, fullFileName);
        }
        return null;
	}


}
