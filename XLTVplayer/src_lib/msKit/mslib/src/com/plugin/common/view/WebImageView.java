/**
 * WebImageView.java
 */
package com.plugin.common.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.plugin.common.cache.CacheFactory;
import com.plugin.common.cache.ICacheManager;
import com.plugin.common.mucslib.R;
import com.plugin.common.utils.CustomThreadPool;
import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.files.FileDownloader;
import com.plugin.common.utils.files.FileDownloader.DownloadRequest;
import com.plugin.common.utils.image.ImageDownloader;
import com.plugin.common.utils.image.ImageDownloader.BitmapOperationListener;
import com.plugin.common.utils.image.ImageDownloader.ImageFetchRequest;
import com.plugin.common.utils.image.ImageDownloader.ImageFetchResponse;

import java.io.File;

/**
 * @author Guoqing Sun Dec 27, 201211:32:15 AM
 */
public class WebImageView extends ImageView implements WebImageViewStatusInterface {

    private static final boolean DEBUG = false & UtilsConfig.UTILS_DEBUG;

    public static enum CustomImageCategory {

        DEFAULT_CATEGORY(UtilsConfig.IMAGE_CACHE_CATEGORY_RAW),

        THUMBNAIL_CATEGORY(UtilsConfig.IMAGE_CACHE_CATEGORY_THUMB),

        SMALL_CATEGORY(UtilsConfig.IMAGE_CACHE_CATEGORY_SMALL);

        private CustomImageCategory(String category) {
            this.category = category;
        }

        @Override
        public String toString() {
            return this.category;
        }

        public String category;
    }

    public static class ThumnbailBitmapOperation implements BitmapOperationListener {

        @Override
        public Bitmap onAfterBitmapDownload(Bitmap downloadBt) {
            if (downloadBt != null && !downloadBt.isRecycled()) {
                return ThumbnailUtils.extractThumbnail(downloadBt, 100, 100);
            }

            return null;
        }

    }

    public static class SmallBitmapOperation implements BitmapOperationListener {

        @Override
        public Bitmap onAfterBitmapDownload(Bitmap downloadBt) {
            if (downloadBt != null && !downloadBt.isRecycled()) {
                return ThumbnailUtils.extractThumbnail(downloadBt, 200, 200);
            }

            return null;
        }

    }

    private CustomImageCategory mImageCategory = CustomImageCategory.DEFAULT_CATEGORY;

    private BitmapOperationListener mBitmapOperationListener;

    private static final int MARGIN = 0;

    private static final String FILE_CONTENT = "file://";

    private static final String DATA_KEY_URL = "url";

    private ICacheManager<Bitmap> mImageCache;

    private ImageDownloader mImageDownloaer;

    protected String mCategory;

    private Animation mAnimation;

    private boolean mHasAnimation;

    private String mUrl;

    private Drawable mDefaultSrc;

    private float mCornerRadius;

    private boolean mCurrentBtLoadSuccess;

    private DownloadRequest mCurrentDownloadRequest;

    private WebImageViewStatusListener mWebImageViewStatusListener;

    private static final int LOCAL_LOAD_IMAGE_SUCCESS = 10000;
    private static final int LOCAL_FILE_IMAGE_LOAD = 10001;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.obj == null) {
                return;
            }

            switch (msg.what) {
                case FileDownloader.NOTIFY_DOWNLOAD_SUCCESS:
                    ImageFetchResponse response = (ImageFetchResponse) msg.obj;
                    if (response.getDownloadUrl().equals(mUrl)) {
                        if (response.getmBt() != null) {
                            mCurrentBtLoadSuccess = true;
                            setImageBitmap(response.getmBt(), true);
                        } else {
                        }
                        // after set the image view from web, just unRegiste the
                        // handler
                        unRegistehandler();
                        notifyImageLoadStatus(response.getmBt(), mUrl, WebImageView.this);
                    }
                    break;
                case FileDownloader.NOTIFY_DOWNLOAD_FAILED:
                    unRegistehandler();
                    notifyImageLoadStatus(null, mUrl, WebImageView.this);
                    break;
                case LOCAL_LOAD_IMAGE_SUCCESS:
                    Bundle data = msg.getData();
                    if (data != null) {
                        String url = data.getString(DATA_KEY_URL);
                        if (url != null && url.equals(mUrl)) {
                            if (msg.obj != null) {
                                mCurrentBtLoadSuccess = true;
                            }
                            setImageBitmap((Bitmap) msg.obj, true);
                        }
                    }
                    break;
                case LOCAL_FILE_IMAGE_LOAD: //TODO
                    if (msg.obj != null) {
                        mCurrentBtLoadSuccess = true;
                    }
                    setImageBitmap((Bitmap) msg.obj, true);
                    break;
            }
        }
    };

    public WebImageView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public WebImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WebImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    public void setWebImageViewStatusListener(WebImageViewStatusListener l) {
        mWebImageViewStatusListener = l;
    }

    private void notifyImageLoadStatus(Bitmap bt, final String url, final ImageView v) {
        if (bt != null) {
            if (mWebImageViewStatusListener != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mWebImageViewStatusListener.onLoadImageSuccess(v, url);
                    }
                });
            }
        } else {
            if (mWebImageViewStatusListener != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mWebImageViewStatusListener.onLoadImageFailed(v, url);
                    }
                });
            }
        }
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WebImageView, defStyle, 0);
        int res = a.getResourceId(R.styleable.WebImageView_network_animation, -1);
        float coner = a.getDimension(R.styleable.WebImageView_conerRadius, 0);
        int cate = a.getInt(R.styleable.WebImageView_category, 0);

        switch (cate) {
            case 1:
                mImageCategory = CustomImageCategory.THUMBNAIL_CATEGORY;
                mBitmapOperationListener = new ThumnbailBitmapOperation();
                break;
            case 2:
                mImageCategory = CustomImageCategory.SMALL_CATEGORY;
                mBitmapOperationListener = new SmallBitmapOperation();
                break;
            default:
                mImageCategory = CustomImageCategory.DEFAULT_CATEGORY;
                mBitmapOperationListener = null;
                break;
        }

        float density = context.getResources().getDisplayMetrics().density;
        mCornerRadius = coner * density;
        if (DEBUG) {
            System.out.println("[[init]] coner in XML = " + coner + " coner radius = " + mCornerRadius + " >>>::::::::::::: + obj = " + this);
        }

        if (res > 0) {
            mAnimation = AnimationUtils.loadAnimation(context.getApplicationContext(), res);
            if (mAnimation != null) {
                mHasAnimation = true;
            }
        }
        a.recycle();

        mImageCache = (ICacheManager<Bitmap>) CacheFactory.getCacheManager(CacheFactory.TYPE_CACHE.TYPE_IMAGE);
        mImageDownloaer = ImageDownloader.getInstance(context.getApplicationContext());

        mCategory = mImageCategory.toString();

        mDefaultSrc = this.getDrawable();
        if (mDefaultSrc != null && (mDefaultSrc instanceof BitmapDrawable) && ((BitmapDrawable) mDefaultSrc).getBitmap() != null) {
            RoundRectDrawable rDrawable = new RoundRectDrawable(getResources(), ((BitmapDrawable) mDefaultSrc).getBitmap());
            rDrawable.setConerRadius(mCornerRadius);
            rDrawable.setUseCanvasClip(this.getScaleType() == ScaleType.CENTER_CROP);
            mDefaultSrc = rDrawable;
            this.setImageDrawable(mDefaultSrc);
        }
    }
    
    public void setNetworkAnimation(int res) {
        if (res > 0) {
            mAnimation = AnimationUtils.loadAnimation(getContext().getApplicationContext(), res);
            if (mAnimation != null) {
                mHasAnimation = true;
            }
        }
    }
    
    public void setConerRadius(float coner) {
        if (coner != mCornerRadius) {
            float density = getContext().getResources().getDisplayMetrics().density;
            mCornerRadius = coner * density;
            if (DEBUG) {
                System.out.println("[[init]] coner in XML = " + coner + " coner radius = " + mCornerRadius + " >>>::::::::::::: + obj = " + this);
            }
            mDefaultSrc = this.getDrawable();
            if (mDefaultSrc != null && (mDefaultSrc instanceof BitmapDrawable) && ((BitmapDrawable) mDefaultSrc).getBitmap() != null) {
                RoundRectDrawable rDrawable = new RoundRectDrawable(getResources(), ((BitmapDrawable) mDefaultSrc).getBitmap());
                rDrawable.setConerRadius(mCornerRadius);
                rDrawable.setUseCanvasClip(this.getScaleType() == ScaleType.CENTER_CROP);
                mDefaultSrc = rDrawable;
                this.setImageDrawable(mDefaultSrc);
            }
        }
    }
    
    public void setDefaultSrc(int res) {
        if (res > 0) {
            this.setImageResource(res);
            mDefaultSrc = this.getDrawable();
            if (mDefaultSrc != null && (mDefaultSrc instanceof BitmapDrawable) && ((BitmapDrawable) mDefaultSrc).getBitmap() != null) {
                RoundRectDrawable rDrawable = new RoundRectDrawable(getResources(), ((BitmapDrawable) mDefaultSrc).getBitmap());
                rDrawable.setConerRadius(mCornerRadius);
                rDrawable.setUseCanvasClip(this.getScaleType() == ScaleType.CENTER_CROP);
                mDefaultSrc = rDrawable;
                this.setImageDrawable(mDefaultSrc);
            }
        }
    }
    
    public void setCategory(int category) {
        switch (category) {
        case 1:
            mImageCategory = CustomImageCategory.THUMBNAIL_CATEGORY;
            mBitmapOperationListener = new ThumnbailBitmapOperation();
            break;
        case 2:
            mImageCategory = CustomImageCategory.SMALL_CATEGORY;
            mBitmapOperationListener = new SmallBitmapOperation();
            break;
        default:
            mImageCategory = CustomImageCategory.DEFAULT_CATEGORY;
            mBitmapOperationListener = null;
            break;
        }
        mCategory = mImageCategory.toString();
    }
    
    @Override
    public void setScaleType(ScaleType scaleType) {
        if (getScaleType() != scaleType) {
            mDefaultSrc = this.getDrawable();
            if (mDefaultSrc != null && (mDefaultSrc instanceof BitmapDrawable) && ((BitmapDrawable) mDefaultSrc).getBitmap() != null) {
                RoundRectDrawable rDrawable = new RoundRectDrawable(getResources(), ((BitmapDrawable) mDefaultSrc).getBitmap());
                rDrawable.setConerRadius(mCornerRadius);
                rDrawable.setUseCanvasClip(this.getScaleType() == ScaleType.CENTER_CROP);
                mDefaultSrc = rDrawable;
                this.setImageDrawable(mDefaultSrc);
            }
        }
        super.setScaleType(scaleType);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getVisibility() == View.VISIBLE) {
            super.onDraw(canvas);
        }
    }

    @Override
    public void setImageResource(int resId) {
        unRegistehandler();
        if (mCurrentDownloadRequest != null) {
            mCurrentDownloadRequest.cancelDownload();
        }
        if (mHasAnimation) {
            this.clearAnimation();
        }
        super.setImageResource(resId);
    }

    @Override
    public void setImageURI(Uri uri) {
        setImageURI(uri, true);
    }

    public void setImageURI(Uri uri, boolean forceOriginLoad) {
        unRegistehandler();
        mCurrentBtLoadSuccess = false;

        if (uri != null) {
            if (DEBUG) {
                Log.d("setImageURI", "[[setImageURI]] uri : " + uri.getScheme() + " uri path : " + uri.getPath() + " uri title : " + uri.getHost()
                        + " encode path = " + uri.getEncodedPath() + " toString : " + uri.toString());
            }
            String path = uri.getPath();
            if (!TextUtils.isEmpty(path) && path.toLowerCase().startsWith("http")) {
                this.setImageUrl(uri.getPath(), forceOriginLoad);
                return;
            } else if (!TextUtils.isEmpty(path) && path.toLowerCase().startsWith(FILE_CONTENT)) {
                String localUri = path.substring(FILE_CONTENT.length());
                if (!TextUtils.isEmpty(localUri)) {
                    this.setImageUrlLocal(localUri, forceOriginLoad);
                    return;
                }
            }
            mUrl = null;
            setImageDrawable(mDefaultSrc);
        } else {
            mUrl = null;
            if (mCurrentDownloadRequest != null) {
                mCurrentDownloadRequest.cancelDownload();
            }
            if (mHasAnimation) {
                this.clearAnimation();
            }
            // super.setImageURI(uri);
            if (mWebImageViewStatusListener != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mWebImageViewStatusListener.onLoadImageFailed(WebImageView.this, mUrl);
                    }
                });
            }
            setImageDrawable(mDefaultSrc);
        }
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        unRegistehandler();
        if (mCurrentDownloadRequest != null) {
            mCurrentDownloadRequest.cancelDownload();
        }
        if (mHasAnimation) {
            this.clearAnimation();
        }
        super.setImageDrawable(drawable);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        unRegistehandler();
        if (mCurrentDownloadRequest != null) {
            mCurrentDownloadRequest.cancelDownload();
        }
        setImageBitmap(bm, false);
    }

    private void setImageBitmap(Bitmap bt, boolean withAnim) {
        if (bt == null) {
            this.setImageDrawable(mDefaultSrc);
            return;
        }

        Drawable preDrawable = getDrawable();
        BitmapDrawable btDrawable = null;
        if (preDrawable instanceof BitmapDrawable) {
            btDrawable = (BitmapDrawable) preDrawable;
        }

        RoundRectDrawable rDrawable = new RoundRectDrawable(getResources(), bt);
        rDrawable.setConerRadius(mCornerRadius);
        rDrawable.setUseCanvasClip(this.getScaleType() == ScaleType.CENTER_CROP);
        if (DEBUG) {
            System.out.println("[[WebImageView::setImageBitmap]] drawable obj = " + rDrawable + " corner = " + mCornerRadius + " >>>");
        }
        super.setImageDrawable(rDrawable);
        if (mHasAnimation) {
            this.clearAnimation();
            if ((Build.VERSION.SDK_INT >= 14) && withAnim
                    && ((bt != null && btDrawable == null) || (bt != null && btDrawable != null && btDrawable.getBitmap() != bt))) {
                this.startAnimation(mAnimation);
            }
        }
    }

    private void setImageUrlLocal(final String localUrl, final boolean forceOriginLoad) {
        if (!TextUtils.isEmpty(localUrl)) {
            mUrl = localUrl;
            Bitmap bt = mImageCache.getResourceFromMem(mCategory, localUrl);
            if (bt == null) {
                CustomThreadPool.asyncWork(new Runnable() {
                    @Override
                    public void run() {
                        File file = new File(localUrl);
                        if (file.exists()) {
                            Bitmap bt = null;
                            if (!forceOriginLoad) {
                                bt = mImageCache.getResource(CustomImageCategory.THUMBNAIL_CATEGORY.toString(), localUrl);
                            }
                            if (bt == null) {
                                bt = mImageCache.getResource(mCategory, localUrl);
                            }

                            if (bt == null) {
                                mImageCache.putResource(mCategory, localUrl, localUrl);
                                bt = mImageCache.getResource(mCategory, localUrl);
                            }

                            Message msg = Message.obtain();
                            msg.what = LOCAL_FILE_IMAGE_LOAD;
                            msg.obj = bt;
                            mHandler.sendMessageDelayed(msg, 30);
                        }

                        Message msg = Message.obtain();
                        msg.what = LOCAL_FILE_IMAGE_LOAD;
                        msg.obj = null;
                        mHandler.sendMessageDelayed(msg, 30);
                    }
                });
            } else {
                mCurrentBtLoadSuccess = true;
                setImageBitmap(bt, false);
            }
        }
    }

    /**
     * @param url
     * @param forceOriginLoad 标示支持同步加载，如果不支持同步加载的，就是fastload模式
     */
    private void setImageUrl(final String url, final boolean forceOriginLoad) {
        if (!TextUtils.isEmpty(url)) {
            mUrl = url;
            Bitmap bt = mImageCache.getResourceFromMem(mCategory, url);
            if (bt == null) {
                CustomThreadPool.asyncWork(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap bt = null;
                        if (!forceOriginLoad) {
                            bt = mImageCache.getResource(CustomImageCategory.THUMBNAIL_CATEGORY.toString(), url);
                        }
                        if (bt == null) {
                            bt = mImageCache.getResource(mCategory, url);
                        }

                        //the fuck logic for category != UtilsConfig.IMAGE_CACHE_CATEGORY_RAW
                        if (bt == null && !TextUtils.isEmpty(mCategory) && !mCategory.equals(UtilsConfig.IMAGE_CACHE_CATEGORY_RAW)) {
                            Bitmap rawBt = mImageCache.getResource(UtilsConfig.IMAGE_CACHE_CATEGORY_RAW, url);
                            if (rawBt != null && mBitmapOperationListener != null) {
                                bt = mBitmapOperationListener.onAfterBitmapDownload(rawBt);
                                if (bt != null) {
                                    mImageCache.putResource(mCategory, url, bt);
                                }
                            }
                            mImageCache.releaseResource(UtilsConfig.IMAGE_CACHE_CATEGORY_RAW, url);
                        }

                        if (bt == null) {
                            registeHandler();
                            if (mCurrentDownloadRequest != null
                                    && !url.equals(mCurrentDownloadRequest.getmDownloadUrl())) {
                                mCurrentDownloadRequest.cancelDownload();
                            }

                            if (mWebImageViewStatusListener != null) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mWebImageViewStatusListener.onPreLoadImage(WebImageView.this, mUrl);
                                    }
                                });
                            }

                            mCurrentDownloadRequest = new ImageFetchRequest(DownloadRequest.DOWNLOAD_TYPE.IMAGE, url, mCategory, mBitmapOperationListener);
                            mImageDownloaer.postRequest(mCurrentDownloadRequest);
                        } else {
                            Message msg = Message.obtain();
                            msg.what = LOCAL_LOAD_IMAGE_SUCCESS;
                            msg.obj = bt;
                            Bundle data = new Bundle();
                            data.putString(DATA_KEY_URL, url);
                            msg.setData(data);
                            mHandler.sendMessageDelayed(msg, 30);
                        }
                    }
                });

                if (mHasAnimation) {
                    this.clearAnimation();
                }
                this.setImageDrawable(mDefaultSrc);
            } else {
                mCurrentBtLoadSuccess = true;
                setImageBitmap(bt, false);
            }
        }
    }

    private void registeHandler() {
        if (mImageDownloaer != null) {
            mImageDownloaer.registeSuccessHandler(mHandler);
            mImageDownloaer.registeFailedHandler(mHandler);
        }
    }

    private void unRegistehandler() {
        if (mImageDownloaer != null) {
            mImageDownloaer.unRegisteSuccessHandler(mHandler);
            mImageDownloaer.unRegisteFailedHandler(mHandler);
        }
    }

    @Override
    public boolean imageShowSuccess(Uri uri) {
        if (uri != null) {
            if (DEBUG) {
                Log.d("imageShowSuccess", "[[imageShowSuccess]] uri : " + uri.getScheme() + " uri path : " + uri.getPath() + " uri title : " + uri.getHost()
                                         + " encode path = " + uri.getEncodedPath() + " toString : " + uri.toString());
            }
            String checkUrl = uri.getPath();
            if (!TextUtils.isEmpty(checkUrl) && checkUrl.toLowerCase().startsWith("http")) {
                // do nothing
            } else if (!TextUtils.isEmpty(checkUrl) && checkUrl.toLowerCase().startsWith(FILE_CONTENT)) {
                checkUrl = checkUrl.substring(FILE_CONTENT.length());
            }
            if (!TextUtils.isEmpty(checkUrl) && checkUrl.equals(mUrl)) {
                return mCurrentBtLoadSuccess;
            }
        }

        return false;
    }

    @Override
    public Uri getCurrentShouldShowImageUri() {
        if (!TextUtils.isEmpty(mUrl)) {
            if (mUrl.toLowerCase().startsWith("http")) {
                return new Uri.Builder().path(mUrl).build();
            } else {
                return new Uri.Builder().path("file://" + mUrl).build();
            }
        }
        return null;
    }
}