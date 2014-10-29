package com.plugin.common.view;

import java.io.File;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.plugin.common.cache.CacheFactory;
import com.plugin.common.cache.ICacheManager;
import com.plugin.common.mucslib.R;
import com.plugin.common.utils.CustomThreadPool;
import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.files.FileDownloader;
import com.plugin.common.utils.files.FileDownloader.DownloadRequest;
import com.plugin.common.utils.image.ImageDownloader;
import com.plugin.common.utils.image.ImageDownloader.ImageFetchRequest;
import com.plugin.common.utils.image.ImageDownloader.ImageFetchResponse;
import com.polites.android.GestureImageView;

@Deprecated
public class WebGestureImageView extends GestureImageView {
    
    private static final String FILE_CONTENT = "file://";

    protected ICacheManager<Bitmap> mImageCache;

    private ImageDownloader mImageDownloaer;

    protected String mCategory;

    private Animation mAnimation;

    private boolean mHasAnimation;

    protected String mUrl;

    private Drawable mDefaultSrc;

    private DownloadRequest mCurrentDownloadRequest;
    
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
                if (response.getDownloadUrl().equals(mUrl) && response.getmBt() != null) {
                    setImageBitmap(response.getmBt(), true);
                    // after set the image view from web, just unRegiste the
                    // handler
                    unRegistehandler();
                }
                break;
            case FileDownloader.NOTIFY_DOWNLOAD_FAILED:
                break;
            case LOCAL_LOAD_IMAGE_SUCCESS:
                setImageBitmap((Bitmap) msg.obj, true);
                break;
            case LOCAL_FILE_IMAGE_LOAD:
                setImageBitmap((Bitmap) msg.obj, true);
                break;
            }
        }
    };

    public WebGestureImageView(Context context) {
        super(context);
        init(context, null, 0);
    }
    
    public WebGestureImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    public WebGestureImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }
 
    private void init(Context context, AttributeSet attrs, int defStyle) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WebImageView, defStyle, 0);
        int res = a.getResourceId(R.styleable.WebImageView_network_animation, -1);

        if (res > 0) {
            mAnimation = AnimationUtils.loadAnimation(context.getApplicationContext(), res);
            if (mAnimation != null) {
                mHasAnimation = true;
            }
        }
        a.recycle();

        mImageCache = (ICacheManager<Bitmap>) CacheFactory.getCacheManager(CacheFactory.TYPE_CACHE.TYPE_IMAGE);
        mImageDownloaer = ImageDownloader.getInstance(context.getApplicationContext());

        mCategory = UtilsConfig.IMAGE_CACHE_CATEGORY_RAW;

        mDefaultSrc = this.getDrawable();
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

        if (uri != null) {
            Log.d("setImageURI", "[[setImageURI]] uri : " + uri.getScheme() + " uri path : " + uri.getPath()
                    + " uri title : " + uri.getHost() + " encode path = " + uri.getEncodedPath()
                    + " toString : " + uri.toString());
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

    protected void setImageBitmap(Bitmap bt, boolean withAnim) {
        if (bt == null) {
            this.setImageDrawable(mDefaultSrc);
            return;
        }
        
        BitmapDrawable btDraw = (BitmapDrawable) this.getDrawable();
        setIsCrop(isCrop(bt));
        setStartBeginTop(startTop(bt));
        super.setImageBitmap(bt);
        if (mHasAnimation) {
            this.clearAnimation();
            if ((Build.VERSION.SDK_INT >= 14) && withAnim && ((bt != null && btDraw == null) || (bt != null && btDraw != null && btDraw.getBitmap() != bt))) {
                this.startAnimation(mAnimation);
            }
        }
    }
    
    private boolean startTop(Bitmap bt) {
        if (bt != null) {
            if (bt.getHeight() > (bt.getWidth() * 2)) {
                return true;
            }
        }

        return false;
    }

    private boolean isCrop(Bitmap bt) {
        if (bt != null) {
            if (bt.getHeight() > (bt.getWidth() * 3)) {
                return true;
            }
        }

        return false;
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
                                bt = mImageCache.getResource(UtilsConfig.IMAGE_CACHE_CATEGORY_THUMB, localUrl);
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
                setImageBitmap(bt, false);
            }
        }
    }

    /**
     * 
     * @param url
     * @param syncLoad
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
                            bt = mImageCache.getResource(UtilsConfig.IMAGE_CACHE_CATEGORY_THUMB, url);
                        }
                        if (bt == null) {
                            bt = mImageCache.getResource(mCategory, url);
                        }
                        if (bt == null) {
                            registeHandler();
                            if (mCurrentDownloadRequest != null) {
                                mCurrentDownloadRequest.cancelDownload();
                            }
                            mCurrentDownloadRequest = new ImageFetchRequest(DownloadRequest.DOWNLOAD_TYPE.IMAGE, url, UtilsConfig.IMAGE_CACHE_CATEGORY_RAW);
                            mImageDownloaer.postRequest(mCurrentDownloadRequest);
                        } else {
                            Message msg = Message.obtain();
                            msg.what = LOCAL_LOAD_IMAGE_SUCCESS;
                            msg.obj = bt;
                            mHandler.sendMessageDelayed(msg, 30);
                        }
                    }
                });

                if (mHasAnimation) {
                    this.clearAnimation();
                }
                this.setImageDrawable(mDefaultSrc);
            } else {
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
}
