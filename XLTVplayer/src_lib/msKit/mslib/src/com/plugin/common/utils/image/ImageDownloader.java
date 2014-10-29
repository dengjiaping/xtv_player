package com.plugin.common.utils.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;

import com.plugin.common.cache.CacheFactory;
import com.plugin.common.cache.ICacheManager;
import com.plugin.common.utils.Destroyable;
import com.plugin.common.utils.SingleInstanceBase;
import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.files.FileDownloader;
import com.plugin.internet.core.HttpRequestHookListener;

/**
 * 用于后台获取图片，默认使用RRThreadPool作为服务线程，当队列中的服务已经完成以后，等待
 * keepalive设置的时间，如果超出这个时间没有响应，就会释放RRThreadPool中的线程给其他的 模块服务。
 * 
 * 为了提高下载显示效率，默认将新加入的下载任务添加到当前下载队列的最前面。
 * 
 * @author Guoqing Sun Sep 11, 20125:04:07 PM
 */
public class ImageDownloader extends FileDownloader implements Runnable, Destroyable, HttpRequestHookListener {

    private static final String TAG = ImageDownloader.class.getSimpleName();

    private static final String DEFAULT_RAW_IMAGE_CATEGORY = UtilsConfig.IMAGE_CACHE_CATEGORY_RAW;

    public static interface BitmapOperationListener {
        Bitmap onAfterBitmapDownload(Bitmap downloadBt);
    }

    public static final class ImageFetchRequest extends DownloadRequest {

        private String mCategory;
        private BitmapOperationListener mBitmapOperationListener;

        /*
         * 通过URL下载，默认的type是 MEDIUM，默认的category是 DEFAULT_RAW_IMAGE_CATEGORY
         */
        public ImageFetchRequest(String downloadUrl) {
            this(DOWNLOAD_TYPE.IMAGE, downloadUrl);
        }

        public ImageFetchRequest(String downloadUrl, BitmapOperationListener l) {
            this(DOWNLOAD_TYPE.IMAGE, downloadUrl, DEFAULT_RAW_IMAGE_CATEGORY, l);
        }

        /*
         * 默认的category是 DEFAULT_RAW_IMAGE_CATEGORY
         */
        public ImageFetchRequest(DOWNLOAD_TYPE type, String downloadUrl) {
            this(type, downloadUrl, DEFAULT_RAW_IMAGE_CATEGORY);
        }

        public ImageFetchRequest(DOWNLOAD_TYPE type, String downloadUrl, String category) {
            this(type, downloadUrl, category, null);
        }

        public ImageFetchRequest(DOWNLOAD_TYPE type, String downloadUrl, String category, BitmapOperationListener l) {
            super(type, downloadUrl);

            if (TextUtils.isEmpty(downloadUrl) || TextUtils.isEmpty(category)) {
                throw new IllegalArgumentException("download Image url can't be empty");
            }

            mCategory = category;
            mBitmapOperationListener = l;
        }

        @Override
        public String toString() {
            return "ImageFetchRequest [mCategory=" + mCategory + ", mBitmapOperationListener=" + mBitmapOperationListener + ", mDownloadUrl=" + mDownloadUrl
                    + ", mUrlHashCode=" + mUrlHashCode + ", mType=" + mType + ", mStatus=" + mStatus + "]";
        }

    }

    public static final class ImageFetchResponse extends DownloadResponse {
        /**
         * 下载的图片的Bitmap对象，可以直接用于显示.
         */
        private Bitmap mBt;
        /**
         * 下载的图片的cache的路径，此路径中指向的图片是缓存库的一份磁盘镜像，不要使用此路径对文件进行操作。
         */
        private String mLocalCachePath;

        private ImageFetchResponse() {
            super();
        }

        private ImageFetchResponse(Bitmap bt, String cachePath, String downloadUrl, String rawPath, DownloadRequest request) {
            super(downloadUrl, rawPath, request);
            mBt = bt;
            mLocalCachePath = cachePath;
        }

        public Bitmap getmBt() {
            return mBt;
        }

        public String getmLocalCachePath() {
            return mLocalCachePath;
        }

        @Override
        public String toString() {
            return "ImageFetchResponse [mBt=" + mBt + ", mLocalCachePath=" + mLocalCachePath + ", mDownloadUrl=" + mDownloadUrl + ", mRawLocalPath="
                    + mLocalRawPath + ", mRequest=" + mRequest + "]";
        }

    }

    private ICacheManager<Bitmap> mCacheManager;

    public static ImageDownloader getInstance(Context context) {
        return SingleInstanceBase.getInstance(ImageDownloader.class);
    }

    protected ImageDownloader() {
        super();
    }

    @Override
    protected void init(Context context) {
        super.init(context);
        mCacheManager = CacheFactory.getCacheManager(CacheFactory.TYPE_CACHE.TYPE_IMAGE);
    }

    @Override
    protected DownloadResponse tryToHandleDownloadFile(String downloadLocalPath, DownloadRequest requestOrg) {
        Bitmap bt = null;
        String localPath = null;
        ImageFetchRequest request = (ImageFetchRequest) requestOrg;
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            try {
                if (DEBUG) {
                    UtilsConfig.LOGD("return is cache file path : " + downloadLocalPath);
                }

                if (request.mBitmapOperationListener != null) {
                    mCacheManager.putResource(DEFAULT_RAW_IMAGE_CATEGORY, request.getmDownloadUrl(), downloadLocalPath);
                    Bitmap downloadBt = mCacheManager.getResource(DEFAULT_RAW_IMAGE_CATEGORY, request.getmDownloadUrl());
                    if (downloadBt != null) {
                        bt = request.mBitmapOperationListener.onAfterBitmapDownload(downloadBt);
                        if (bt != null) {
                            mCacheManager.putResource(request.mCategory, request.getmDownloadUrl(), bt);
                        }

                        mCacheManager.releaseResource(DEFAULT_RAW_IMAGE_CATEGORY, request.getmDownloadUrl());
                        if (downloadBt != null && !downloadBt.isRecycled()) {
                            downloadBt.recycle();
                            downloadBt = null;
                        }
                    }
                } else {
                    localPath = mCacheManager.putResource(request.mCategory, request.getmDownloadUrl(), downloadLocalPath);
                    bt = mCacheManager.getResource(request.mCategory, request.getmDownloadUrl());
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (DEBUG) {
                    UtilsConfig.LOGD("Excption : ", e);
                }
            }
        } else {
            try {
                bt = ImageUtils.loadBitmapWithSizeOrientation(downloadLocalPath);
            } catch (Exception e) {
                e.printStackTrace();
                if (DEBUG) {
                    UtilsConfig.LOGD("Excption : ", e);
                }
            }
        }

        if (bt != null) {
            ImageFetchResponse response = new ImageFetchResponse(bt, localPath, request.getmDownloadUrl(), downloadLocalPath, request);

            return response;
        }

        return null;
    }

    @Override
    protected boolean checkInputStreamDownloadFile(String filePath) {
        return ImageUtils.isBitmapData(filePath);
    }

}
