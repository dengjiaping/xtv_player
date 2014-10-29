/**
 * ThumbnailBitmapCacheManager.java
 */
package com.plugin.common.cache.image;

import android.support.v4.util.LruCache;

import com.plugin.common.utils.files.FileUtil;

/**
 * @author Guoqing Sun Jan 22, 20133:51:39 PM
 */
final class ThumbnailBitmapCacheManager extends AbsBitmapCacheManager {

    /*
     * (non-Javadoc)
     * 
     * @see com.sound.dubbler.cache.AbsBitmapCacheManager#makeLruCacheObj()
     */
    @Override
    LruCache<String, BitmapObject> makeLruCacheObj() {
        return new LruCache<String, BitmapObject>(1024 * 1024 * 2) {
            @Override
            protected int sizeOf(String key, BitmapObject value) {
                if (value != null) {
                    if (DEBUG) {
                        System.out.println(" ");
                        System.out.println("//########## [[ThumbnailBitmapCacheManager::SizeOf]] ##############");
                        System.out.println("| Current Bitmap info :");
                        System.out.println("| size = " + FileUtil.convertStorage(value.btSize));
                        System.out.println("| width = " + value.btWidth);
                        System.out.println("| height = " + value.btHeight);
                        System.out.println("| category = " + value.category);
                        System.out.println("\\########## [[ThumbnailBitmapCacheManager::SizeOf]] ##############");
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
                    System.out.println("//########## [[ThumbnailBitmapCacheManager::entryRemoved]] ##############");
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
                    } else {
                        System.out.println("| Current new Bitmap info : null");
                    }
                    System.out.println("| ");
                    System.out.println("| Current LRU Size = " + curCacheSize(mLruCache));
                    System.out.println("\\########## [[ThumbnailBitmapCacheManager::entryRemoved]] ##############");
                    System.out.println(" ");
                }

                if (oldValue != null && oldValue.bt != null) {
                    if (ENABLE_BITMAP_REUSE && mBitmapReusedObjectObj != null && (mBitmapReusedObjectObj.count < 11)
                            && oldValue.bt.getWidth() == mOption.thumbnailSize
                            && oldValue.bt.getHeight() == mOption.thumbnailSize) {
                        mBitmapReusedObjectObj.reusedList.add(oldValue);
                        mBitmapReusedObjectObj.count++;
                    }
                }
                oldValue = null;
            }
        };
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sound.dubbler.cache.AbsBitmapCacheManager#searchReusedBitmapObj(java
     * .lang.String)
     */
    @Override
    BitmapObject searchReusedBitmapObj(String category) {
        return loopupOneReusedBitmap(mBitmapReusedObjectObj, mOption.thumbnailSize, mOption.thumbnailSize);
    }

}
