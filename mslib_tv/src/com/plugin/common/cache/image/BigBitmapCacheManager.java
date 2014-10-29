/**
 * BigBitmapCacheManager.java
 */
package com.plugin.common.cache.image;

import android.support.v4.util.LruCache;
import android.text.TextUtils;

import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.files.FileOperatorHelper;
import com.plugin.common.utils.files.FileUtil;

/**
 * @author Guoqing Sun Jan 22, 20133:30:27 PM
 */
final class BigBitmapCacheManager extends AbsBitmapCacheManager {

	@Override
	LruCache<String, BitmapObject> makeLruCacheObj() {
		return new LruCache<String, BitmapObject>(UtilsConfig.DEVICE_INFO.image_cache_size) {
			@Override
			protected int sizeOf(String key, BitmapObject value) {
				if (value != null) {
					if (DEBUG) {
					    System.out.println(" ");
						System.out.println("//########## [[BigBitmapCacheManager::SizeOf]] ##############");
						System.out.println("| Current Bitmap info :");
						System.out.println("| size = " + FileUtil.convertStorage(value.btSize));
						System.out.println("| width = " + value.btWidth);
						System.out.println("| height = " + value.btHeight);
						System.out.println("| category = " + value.category);
						System.out.println("\\########## [[BigBitmapCacheManager::SizeOf]] ##############");
						System.out.println(" ");
					}
					return value.btSize;
				}

				return BitmapObject.ObjdefaultSize();
			}

			@Override
			protected void entryRemoved(boolean evicted, String key, BitmapObject oldValue, BitmapObject newValue) {
				if (DEBUG && oldValue != null) {
				    System.out.println(" ");
                    System.out.println("//########## [[BigBitmapCacheManager::entryRemoved]] ##############");
                    System.out.println("| Key = " + key);
                    System.out.println("| Evicted Bitmap info :");
                    System.out.println("| size = " + FileUtil.convertStorage(oldValue.btSize));
                    System.out.println("| width = " + oldValue.btWidth);
                    System.out.println("| height = " + oldValue.btHeight);
                    System.out.println("| category = " + oldValue.category);
                    System.out.println("| ");
                    if (newValue != null) {
                        System.out.println("| Current new Bitmap info :");
                        System.out.println("| size = " + FileUtil.convertStorage(newValue.btSize));
                        System.out.println("| width = " + newValue.btWidth);
                        System.out.println("| height = " + newValue.btHeight);
                        System.out.println("| category = " + newValue.category);
                        System.out.println("| ");
                    } else {
                        System.out.println("| Current new Bitmap info : null");
                    }
                    System.out.println("| Current LRU Size = " + curCacheSize(mLruCache));
                    System.out.println("\\########## [[BigBitmapCacheManager::entryRemoved]] ##############");
                    System.out.println(" ");
				}
				if (oldValue != null && oldValue.bt != null) {
					if (UtilsConfig.IMAGE_CACHE_CATEGORY_USER_HEAD_ROUNDED.equals(oldValue.category)
							&& BitmapUtils.USER_HEAD_STANDARD_SIZE == oldValue.bt.getWidth()
							&& BitmapUtils.USER_HEAD_STANDARD_SIZE == oldValue.bt.getHeight()) {
						// save the head bt obj for reuse
						if (ENABLE_BITMAP_REUSE) {
							if (mBitmapReusedObjectObj != null) {
								if (mBitmapReusedObjectObj.count < 10) {
									mBitmapReusedObjectObj.reusedList.add(oldValue);
									mBitmapReusedObjectObj.count++;
								}
							}
						}
					} else {
						oldValue.bt = null;
						oldValue = null;
					}
				}
				oldValue = null;
			}
		};
	}

	@Override
	BitmapObject searchReusedBitmapObj(String category) {
		if (UtilsConfig.IMAGE_CACHE_CATEGORY_USER_HEAD_ROUNDED.equals(category)) {
			return loopupOneReusedBitmap(mBitmapReusedObjectObj, BitmapUtils.USER_HEAD_STANDARD_SIZE,
					BitmapUtils.USER_HEAD_STANDARD_SIZE);
		}
		return null;
	}

	@Override
	public String putResource(String category, String key, CharSequence sourceFullFile) {
		if (UtilsConfig.IMAGE_CACHE_CATEGORY_USER_HEAD_ROUNDED.equals(category)) {
			try {
				synchronized (mIOLockObject) {
					String path = BitmapDiskTools.getBitmapSavePath(makeFileKeyName(category, key));
					if (!TextUtils.isEmpty(path) && FileOperatorHelper.moveFile(sourceFullFile.toString(), path)) {
						if (DEBUG) {
							LOGD("[[flushResourceByFile]] success move file from : " + sourceFullFile + " ||||| to : "
									+ path);
						}
					}

					return path;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}

		return super.putResource(category, key, sourceFullFile);
	}

}
