/**
 * AbsBitmapCacheManager.java
 */
package com.plugin.common.cache.image;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedList;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.text.TextUtils;

import com.plugin.common.cache.ICacheManager;
import com.plugin.common.cache.ICacheStrategy;
import com.plugin.common.utils.DebugLog;
import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.files.FileOperatorHelper;
import com.plugin.common.utils.files.FileUtil;
import com.plugin.common.utils.image.ImageUtils;

/**
 * @author Guoqing Sun Jan 22, 20132:31:51 PM
 */
abstract class AbsBitmapCacheManager implements ICacheManager<Bitmap> {
	
	private static final String TAG = "AbsBitmapCacheManager";

    protected static final boolean DEBUG = true && UtilsConfig.UTILS_DEBUG;

    protected final boolean ENABLE_BITMAP_REUSE;
    
    protected BitmapCacheOption mOption;
    
    protected static final class BitmapObject {
        int btSize;
        int btWidth;
        int btHeight;
        Bitmap bt;
        String category;

        static int ObjdefaultSize() {
            return 4 * 4;
        }

        int caculateSize() {
            if (bt != null) {
                btSize = 4 * 3 + bt.getRowBytes() * bt.getHeight();
            }

            return btSize;
        }

        @Override
        public String toString() {
            return "BitmapObject [btSize=" + btSize + ", btWidth=" + btWidth + ", btHeight=" + btHeight + ", bt=" + bt
                    + ", category=" + category + "]";
        }

    }

    protected final class BitmapReusedObject {
        LinkedList<BitmapObject> reusedList;
        int count;

        BitmapReusedObject() {
            reusedList = new LinkedList<BitmapObject>();
            count = 0;
        }
    }

    protected LruCache<String, BitmapObject> mLruCache;

    protected BitmapReusedObject mBitmapReusedObjectObj;

    protected Object mIOLockObject = new Object();

    protected AbsBitmapCacheManager() {
//        if (Runtime.DEVICE_INFO.sdkTarget < 11) {
            ENABLE_BITMAP_REUSE = false;
//        } else {
//            ENABLE_BITMAP_REUSE = true;
//            mBitmapReusedObjectObj = new BitmapReusedObject();
//        }

        mLruCache = makeLruCacheObj();
        if (mLruCache == null) {
            throw new IllegalArgumentException("LruCache can't be null");
        }
    }

    abstract LruCache<String, BitmapObject> makeLruCacheObj();

    abstract BitmapObject searchReusedBitmapObj(String category);
    
    public void setBitmapCacheOption(BitmapCacheOption option) {
        mOption = option;
    }

    @Override
    public String getResourcePath(String category, String key) {
        if (TextUtils.isEmpty(category) || TextUtils.isEmpty(key)) {
            return null;
        }
        return BitmapDiskTools.getBitmapSavePath(makeFileKeyName(category, key));
    }

    @Override
    public Bitmap getResource(String category, String key) {
        if (TextUtils.isEmpty(category) || TextUtils.isEmpty(key)) {
            if (DEBUG) {
                LOGD("[[getResource::" + makeFileKeyName(category, key) + "]] faild for category or key is empty");
            }
            return null;
        }

        Bitmap ret = getBitmapByCategoryAndKey(category, key, mLruCache);

        if (ret == null || ret.isRecycled()) {
            // 使用category和key一起定位一张图片
            synchronized (mIOLockObject) {
                if (ENABLE_BITMAP_REUSE) {
                    if (mBitmapReusedObjectObj != null) {
                        BitmapObject btObj = searchReusedBitmapObj(category);
                        if (btObj != null) {
                            ret = BitmapDiskTools.getBitmapFromDiskWithReuseBitmap(makeFileKeyName(category, key),
                                    btObj.bt);
                            if ((ret == null)
                                    || (ret != null && ret != btObj.bt && btObj.bt != null && !btObj.bt.isRecycled())) {
                                mBitmapReusedObjectObj.reusedList.add(btObj);
                                mBitmapReusedObjectObj.count++;
                            }
                        } else {
                            ret = BitmapDiskTools
                                    .getBitmapFromDiskWithReuseBitmap(makeFileKeyName(category, key), null);
                        }
                    } else {
                        ret = BitmapDiskTools.getBitmapFromDiskWithReuseBitmap(makeFileKeyName(category, key), null);
                    }
                } else {
                    ret = BitmapDiskTools.getBitmapFromDiskWithReuseBitmap(makeFileKeyName(category, key), null);
                }
                if (ret != null) {
                    cacheBitmapByCategoryAndKey(category, key, ret, false, mLruCache);
                }
            }
        }
        
        if (DEBUG) {
            if (ret != null) {
                LOGD("[[getResource::" + makeFileKeyName(category, key) + "]] << surccess >>");
            } else {
                LOGD("[[getResource::" + makeFileKeyName(category, key) + "]] << failed >>");
            }
        }

        return ret;
    }

    @Override
    public Bitmap getResourceFromMem(String category, String key) {
        if (TextUtils.isEmpty(category) || TextUtils.isEmpty(key)) {
            return null;
        }

        Bitmap ret = getBitmapByCategoryAndKey(category, key, mLruCache);

        if (DEBUG) {
            if (ret != null) {
                LOGD("get bitmap resource [[" + makeFileKeyName(category, key) + "]] from cache (only memory) success");
            } else {
                LOGD("get bitmap resource [[" + makeFileKeyName(category, key)
                        + "]] from cache (only memory) <<failed>>");
            }
        }

        return ret;
    }

    @Override
    public String putResource(String category, String key, Bitmap bt) {
        return cacheBitmapByCategoryAndKey(category, key, bt, true, mLruCache);
    }

    @Override
    public String putResource(String category, String key, CharSequence sourceFullFile) {
        if (TextUtils.isEmpty(category) || TextUtils.isEmpty(key) || TextUtils.isEmpty(sourceFullFile)) {
            return null;
        }

        try {
            Bitmap bm = getResource(category, key);
            if (bm != null) {
                return this.getResourcePath(category, key);
            }
            synchronized (mIOLockObject) {
            	File localFileCheck = new File(sourceFullFile.toString());
            	if (localFileCheck.exists()) {
            		String cachePath = getResourcePath(category, key);
            		if (!TextUtils.isEmpty(cachePath)) {
            			if (FileOperatorHelper.copyFile(sourceFullFile.toString(), cachePath) != null) {
            				return cachePath;
            			}
            			
            		}
            	}
            	
                bm = ImageUtils.loadBitmapWithSizeOrientation(sourceFullFile.toString());
            }

            if (bm != null) {
                putResource(category, key, bm);
                return this.getResourcePath(category, key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String putResource(String category, String key, InputStream is) {
        if (TextUtils.isEmpty(key) || TextUtils.isEmpty(category) || is == null) {
            return null;
        }

        // save the file to temp
        String saveKey = makeFileKeyName(category, key) + "_tmp";
        String tempSavePath = BitmapDiskTools.saveRawBitmap(saveKey, is);

        if (!TextUtils.isEmpty(tempSavePath)) {
            synchronized (mIOLockObject) {
                String targetPath = BitmapDiskTools.getBitmapSavePath(makeFileKeyName(category, key));
                if (FileOperatorHelper.moveFile(tempSavePath, targetPath)) {
                    return targetPath;
                }
            }
        }

        return null;
    }

    @Override
    public void releaseResource(String category) {
        releaseByCategory(category, true);
    }

    @Override
    public void releaseResource(String category, String key) {
        releaseByCategoryAndKey(category, key, true);
    }

    @Override
    public void releaseAllResource() {
        mLruCache.evictAll();
        if (mBitmapReusedObjectObj != null) {
            mBitmapReusedObjectObj.reusedList.clear();
            mBitmapReusedObjectObj.count = 0;
        }
    }

    @Override
    public void clearResource() {
        synchronized (mIOLockObject) {
            mLruCache.evictAll();
            //TODO: 删除cache的磁盘文件
        }
    }

    @Override
    public final ICacheStrategy setCacheStrategy(ICacheStrategy strategy) {
        ICacheStrategy def = BitmapDiskTools.sDefaultCacheStrategy;
    	BitmapDiskTools.sDefaultCacheStrategy = strategy;
    	
    	return def;
    }
    
    private void releaseByCategory(String category, boolean ifRecyle) {
        releaseAllResource();
    }

    private void releaseByCategoryAndKey(String category, String key, boolean ifRecyle) {
        if (TextUtils.isEmpty(category) || TextUtils.isEmpty(key)) {
            return;
        }

        mLruCache.remove(makeFileKeyName(category, key));
    }

    protected BitmapObject loopupOneReusedBitmap(BitmapReusedObject reuseObj, int width, int height) {
        if (reuseObj == null || reuseObj.reusedList == null) {
            return null;
        }
        BitmapObject ret = null;
        for (BitmapObject btObj : reuseObj.reusedList) {
            if (btObj.btWidth == width && btObj.btHeight == height) {
                ret = btObj;
            }
        }

        if (ret != null) {
            reuseObj.reusedList.remove(ret);
            reuseObj.count--;
            if (DEBUG) {
                LOGD("++++++ loopup reused btimap success for Width : " + width + " Height : " + height
                        + " BitmapObject Info : " + ret);
            }
        }

        return ret;
    }

    private Bitmap getBitmapByCategoryAndKey(String category, String key, LruCache<String, BitmapObject> cache) {
        Bitmap ret = null;

        if (cache.get(makeFileKeyName(category, key)) != null) {
            ret = cache.get(makeFileKeyName(category, key)).bt;
        }

        return ret;
    }

    private String cacheBitmapByCategoryAndKey(String category, String key, Bitmap bt, boolean forceSaveToDisk,
            LruCache<String, BitmapObject> cache) {
        if (DEBUG) {
            LOGD("[[cacheBitmapByCategoryAndKey]] category = " + category + " key = " + key + " for bitmap   >>>>>>>>");
        }

        if (TextUtils.isEmpty(key) || TextUtils.isEmpty(category) || bt == null || bt.isRecycled()) {
            return null;
        }

        synchronized (mIOLockObject) {
            Bitmap cacheBt = getBitmapByCategoryAndKey(category, key, cache);
            boolean shouldSaveToDisk = true;
            if (cacheBt == bt) {
                shouldSaveToDisk = false;
            }

            String saveKey = makeFileKeyName(category, key);

            if (DEBUG) {
                LOGD("[[cacheBitmapByCategoryAndKey]] before put, the object count = " + curCacheSize(cache) + "M");
            }
            BitmapObject btObject = new BitmapObject();
            btObject.btSize = bt.getRowBytes() * bt.getHeight() + 4 * 3;
            btObject.btHeight = bt.getHeight();
            btObject.btWidth = bt.getWidth();
            btObject.bt = bt;
            btObject.category = category;
            cache.put(saveKey, btObject);
            if (DEBUG) {
                LOGD("[[cacheBitmapByCategoryAndKey]] end put, the object count = " + curCacheSize(cache) + "M"
                        + "  current add bitmap size = "
                        + FileUtil.convertStorage(btObject.btSize)
                        + " ------------ bt size = (width : " + bt.getWidth() + ", height : " + bt.getHeight() + ")"
                        + " ************ bt byte = (width : " + bt.getRowBytes() + ", height : " + bt.getHeight() + ")");
            }

            if (shouldSaveToDisk && forceSaveToDisk) {
                try {
                    // 使用category和key一起定位一张图片
                    // String file = DiskTools.saveRawBitmapByByte(saveKey,
                    // ImageUtils.getBitmapBytes(bt));
                    String file = BitmapDiskTools.saveRawBitmap(saveKey, bt);
                    if (TextUtils.isEmpty(file)) {
                        BitmapDiskTools.removeBitmap(saveKey);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    BitmapDiskTools.removeBitmap(saveKey);
                }
            }
        }

        return this.getResourcePath(category, key);
    }

    protected float curCacheSize(LruCache<String, BitmapObject> cache) {
        return (float) ((cache.size() * 1.0) / 1024 / 1024);
    }

    protected String makeFileKeyName(String category, String key) {
        StringBuilder sb = new StringBuilder(256);
        sb.append(category).append("/").append(key);
        return sb.toString();
    }

    protected static final void LOGD(String msg) {
        if (DEBUG) {
        	DebugLog.d(TAG, msg);
        }
    }
}
