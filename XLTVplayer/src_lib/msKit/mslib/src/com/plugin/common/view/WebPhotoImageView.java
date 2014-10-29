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
import com.plugin.common.utils.UtilsRuntime;
import com.plugin.common.utils.files.FileDownloader;
import com.plugin.common.utils.image.ImageDownloader;
import uk.co.senab.photoview.PhotoView;

import java.io.File;

/**
 * 支持图片缩放的WebImageView
 *
 * Created with IntelliJ IDEA.
 * User: michael
 * Date: 13-8-6
 * Time: PM2:14
 * To change this template use File | Settings | File Templates.
 */
public class WebPhotoImageView extends PhotoView implements WebImageViewStatusInterface {

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

    public static class ThumnbailBitmapOperation implements ImageDownloader.BitmapOperationListener {

        @Override
        public Bitmap onAfterBitmapDownload(Bitmap downloadBt) {
            if (downloadBt != null && !downloadBt.isRecycled()) {
                return ThumbnailUtils.extractThumbnail(downloadBt, 100, 100);
            }

            return null;
        }

    }

    public static class SmallBitmapOperation implements ImageDownloader.BitmapOperationListener {

        @Override
        public Bitmap onAfterBitmapDownload(Bitmap downloadBt) {
            if (downloadBt != null && !downloadBt.isRecycled()) {
                return ThumbnailUtils.extractThumbnail(downloadBt, 200, 200);
            }

            return null;
        }

    }

    private CustomImageCategory mImageCategory = CustomImageCategory.DEFAULT_CATEGORY;

    private ImageDownloader.BitmapOperationListener mBitmapOperationListener;

    private static final int MARGIN = 0;

    private static final String FILE_CONTENT = "file://";

    private static final String DATA_KEY_URL = "url";

    private ICacheManager<Bitmap> mImageCache;

    private ImageDownloader mImageDownloaer;

    private boolean mCurrentBtLoadSuccess;

    protected String mCategory;

    private Animation mAnimation;

    private boolean mHasAnimation;

    private String mUrl;

    private Drawable mDefaultSrc;

    private float mCornerRadius;

    private boolean mHasAdjust;

    private WebImageViewStatusListener mWebImageViewStatusListener;

    private FileDownloader.DownloadRequest mCurrentDownloadRequest;

    private static final int LOCAL_LOAD_IMAGE_SUCCESS = 10000;
    private static final int LOCAL_FILE_IMAGE_LOAD = 10001;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.obj == null) {
                return;
            }

            switch (msg.what) {
                case FileDownloader.NOTIFY_DOWNLOAD_SUCCESS:
                    ImageDownloader.ImageFetchResponse response = (ImageDownloader.ImageFetchResponse) msg.obj;
                    if (response.getDownloadUrl().equals(mUrl)) {
                        if (response.getmBt() != null) {
                            mCurrentBtLoadSuccess = true;
                            setImageBitmap(response.getmBt(), true);
                        } else {
                        }
                        // after set the image view from web, just unRegiste the
                        // handler
                        unRegistehandler();
                        notifyImageLoadStatus(response.getmBt(), mUrl, WebPhotoImageView.this);
                    }
                    break;
                case FileDownloader.NOTIFY_DOWNLOAD_FAILED:
                    unRegistehandler();
                    notifyImageLoadStatus(null, mUrl, WebPhotoImageView.this);
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

    public WebPhotoImageView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public WebPhotoImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WebPhotoImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    public void setWebImageViewStatusListener(WebImageViewStatusListener l) {
        mWebImageViewStatusListener = l;
    }

    //just used for fit center
    private void adjustScaleFactor(Bitmap bt) {
        if (bt == null) {
            return;
        }

        int width = getWidth();
        int height = getHeight();
//        if (width == 0 || height == 0) {
//            int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
//            int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
//            measure(w, h);
//            width = getMeasuredWidth();
//            height = getMeasuredHeight();
//        }

        if (width != 0 && height != 0 && bt.getWidth() != 0 && bt.getHeight() != 0) {
            int btWidth = bt.getWidth();
            int btHeight = bt.getHeight();
            float wPreScale = ((float) (btWidth * 1.0)) / width;
            float hPreScale = ((float) (btHeight * 1.0)) / height;
            float preScale = wPreScale > hPreScale ? wPreScale : hPreScale;

            float wScale = ((float) (width * 1.0)) / (btWidth / preScale);
            float hScale = ((float) (height * 1.0)) / (btHeight / preScale);
            float scale = wScale > hScale ? wScale : hScale;
            if (scale > getMaxScale()) {
                setMaxScale(scale);
            }
        }
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
            rDrawable.setUseCanvasClip(this.getScaleType() == ImageView.ScaleType.CENTER_CROP);
            mDefaultSrc = rDrawable;
            this.setImageDrawable(mDefaultSrc);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable preDrawable = getDrawable();
        if (preDrawable instanceof BitmapDrawable && preDrawable != null) {
            Bitmap bt = ((BitmapDrawable) preDrawable).getBitmap();
            adjustScaleFactor(bt);
            mHasAdjust = true;
        }

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
        mHasAdjust = false;

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
        mHasAdjust = false;
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
        rDrawable.setUseCanvasClip(this.getScaleType() == ImageView.ScaleType.CENTER_CROP);
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
     *
     * @param url
     * @param forceOriginLoad
     *            标示支持同步加载，如果不支持同步加载的，就是fastload模式
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
                        if (bt == null) {
                            registeHandler();
                            if (mCurrentDownloadRequest != null && !url.equals(mCurrentDownloadRequest.getmDownloadUrl())) {
                                mCurrentDownloadRequest.cancelDownload();
                            }
                            if (mWebImageViewStatusListener != null) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mWebImageViewStatusListener.onPreLoadImage(WebPhotoImageView.this, mUrl);
                                    }
                                });
                            }

                            mCurrentDownloadRequest = new ImageDownloader.ImageFetchRequest(FileDownloader.DownloadRequest.DOWNLOAD_TYPE.IMAGE, url, mCategory, mBitmapOperationListener);
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
