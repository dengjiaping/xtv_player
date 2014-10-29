package com.kankan.player.explorer;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Handler.Callback;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Files.FileColumns;
import android.text.TextUtils;
import android.widget.ImageView;
import com.kankan.media.Media;
import com.kankan.player.app.AppConfig;
import com.kankan.player.util.PlayerUtil;
import com.kankan.player.util.SmbUtil;
import com.plugin.common.cache.CacheFactory;
import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.files.FileUtil;
import com.xunlei.tv.player.R;

/**
 * Asynchronously loads file icons and thumbnail, mostly single-threaded.
 */
public class FileIconLoader implements Callback {

    private static final String LOADER_THREAD_NAME = "FileIconLoader";

    /**
     * Type of message sent by the UI thread to itself to indicate that some
     * images need to be loaded.
     */
    private static final int MESSAGE_REQUEST_LOADING = 1;

    /**
     * Type of message sent by the loader thread to indicate that some images
     * have been loaded.
     */
    private static final int MESSAGE_ICON_LOADED = 2;

    private static abstract class ImageHolder {
        public static final int NEEDED = 0;
        public static final int LOADING = 1;
        public static final int LOADED = 2;

        int state;

        public static ImageHolder create(FileCategory cate) {
            switch (cate) {
                case APK:
                    return new DrawableHolder();
                case VIDEO:
                    //return new BitmapHolder();
                    return new DrawableHolder();
            }
            return null;
        }

        public abstract boolean setImageView(ImageView v);

        public abstract boolean isNull();

        public abstract void setImage(Object image);
    }

    private static class BitmapHolder extends ImageHolder {
        SoftReference<Bitmap> bitmapRef;

        @Override
        public boolean setImageView(ImageView v) {
            if (bitmapRef.get() == null)
                return false;
            v.setImageBitmap(bitmapRef.get());
            return true;
        }

        @Override
        public boolean isNull() {
            return bitmapRef == null;
        }

        @Override
        public void setImage(Object image) {
            bitmapRef = image == null ? null : new SoftReference<Bitmap>((Bitmap) image);
        }
    }

    private static class DrawableHolder extends ImageHolder {
        SoftReference<Drawable> drawableRef;

        @Override
        public boolean setImageView(ImageView v) {
            if (drawableRef.get() == null)
                return false;

            v.setImageDrawable(drawableRef.get());
            return true;
        }

        @Override
        public boolean isNull() {
            return drawableRef == null;
        }

        @Override
        public void setImage(Object image) {
            drawableRef = image == null ? null : new SoftReference<Drawable>((Drawable) image);
        }
    }

    /**
     * A soft cache for image thumbnails. the key is file path
     */
    private final static ConcurrentHashMap<String, ImageHolder> mImageCache = new ConcurrentHashMap<String, ImageHolder>();
    /**
     * 加边框的图片和不加边框的图片使用不同的缓存，避免同一份缓存引起bug
     */
    private final static ConcurrentHashMap<String, ImageHolder> mThumnailImageCache = new ConcurrentHashMap<String, ImageHolder>();

    /**
     * A map from ImageView to the corresponding photo ID. Please note that this
     * photo ID may change before the photo loading request is started.
     */
    private final ConcurrentHashMap<ImageView, FileId> mPendingRequests = new ConcurrentHashMap<ImageView, FileId>();

    /**
     * Handler for messages sent to the UI thread.
     */
    private final Handler mMainThreadHandler = new Handler(this);

    /**
     * Thread responsible for loading photos from the database. Created upon the
     * first request.
     */
    private LoaderThread mLoaderThread;

    /**
     * A gate to make sure we only send one instance of MESSAGE_PHOTOS_NEEDED at
     * a time.
     */
    private boolean mLoadingRequested;

    /**
     * Flag indicating if the image loading is paused.
     */
    private boolean mPaused;

    private final Context mContext;

    /**
     * 缩略图模式和普通模式的默认图片不一样
     */
    private boolean mIsThumnbailMode = false;

    private IconLoadFinishListener mIconLoadListener;

    /**
     * 在设置之前对icon进行处理
     */
    private OnIconProcessFilter mIconProcessFilter;

    /**
     * Constructor.
     *
     * @param context content context
     */
    public FileIconLoader(Context context, IconLoadFinishListener l) {
        mContext = context;
        mIconLoadListener = l;
    }

    public static class FileId {
        public String mPath;

        public FileCategory mCategory;

        public FileId(String path, FileCategory cate) {
            mPath = path;
            mCategory = cate;
        }
    }

    public abstract static interface IconLoadFinishListener {
        void onIconLoadFinished(ImageView view);
    }

    public void setIconProcessFilter (OnIconProcessFilter filter) {
        mIconProcessFilter = filter;
    }

    public void setThumbnailMode() {
        mIsThumnbailMode = true;
    }

    public boolean isThumbnailMode() {
        return mIsThumnbailMode;
    }

    /**
     * Load photo into the supplied image view. If the photo is already cached,
     * it is displayed immediately. Otherwise a request is sent to load the
     * photo from the database.
     */
    public boolean loadIcon(ImageView view, String path, FileCategory cate) {
        boolean loaded = loadCachedIcon(view, path, cate);
        AppConfig.LOGD("[[FileIconLoader]] loadIcon loaded=" + loaded + ", path=" + path);
        if (loaded) {
            mPendingRequests.remove(view);
        } else {
            FileId p = new FileId(path, cate);
            mPendingRequests.put(view, p);
            if (!mPaused) {
                // Send a request to start loading photos
                requestLoading();
            }
        }
        return loaded;
    }

    public void cancelRequest(ImageView view) {
        mPendingRequests.remove(view);
    }

    /**
     * Checks if the photo is present in cache. If so, sets the photo on the
     * view, otherwise sets the state of the photo to
     * {@link BitmapHolder#NEEDED}
     */
    private boolean loadCachedIcon(ImageView view, String path, FileCategory cate) {
        ImageHolder holder = getImageCache().get(path);

        if (holder == null) {
            holder = ImageHolder.create(cate);
            if (holder == null)
                return false;

            getImageCache().put(path, holder);
        } else if (holder.state == ImageHolder.LOADED) {
            if (holder.isNull()) {
                return true;
            }

            // failing to set imageview means that the soft reference was
            // released by the GC, we need to reload the photo.
            if (holder.setImageView(view)) {
                return true;
            }
        }

        holder.state = ImageHolder.NEEDED;
        return false;
    }

    /**
     * Stops loading images, kills the image loader thread and clears all
     * caches.
     */
    public void stop() {
        pause();

        if (mLoaderThread != null) {
            mLoaderThread.quit();
            mLoaderThread = null;
        }

        clear();
    }

    public ConcurrentHashMap<String, ImageHolder> getImageCache() {
        if (mIsThumnbailMode) {
            return mThumnailImageCache;
        } else {
            return mImageCache;
        }
    }

    public void clear() {
        mPendingRequests.clear();
        mImageCache.clear();
        mThumnailImageCache.clear();
    }

    /**
     * Temporarily stops loading
     */
    public void pause() {
        mPaused = true;
    }

    /**
     * Resumes loading
     */
    public void resume() {
        mPaused = false;
        if (!mPendingRequests.isEmpty()) {
            requestLoading();
        }
    }

    /**
     * Sends a message to this thread itself to start loading images. If the
     * current view contains multiple image views, all of those image views will
     * get a chance to request their respective photos before any of those
     * requests are executed. This allows us to load images in bulk.
     */
    private void requestLoading() {
        if (!mLoadingRequested) {
            mLoadingRequested = true;
            mMainThreadHandler.sendEmptyMessage(MESSAGE_REQUEST_LOADING);
        }
    }

    /**
     * Processes requests on the main thread.
     */
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_REQUEST_LOADING: {
                mLoadingRequested = false;
                if (!mPaused) {
                    if (mLoaderThread == null) {
                        mLoaderThread = new LoaderThread();
                        mLoaderThread.start();
                    }

                    mLoaderThread.requestLoading();
                }
                return true;
            }

            case MESSAGE_ICON_LOADED: {
                if (!mPaused) {
                    processLoadedIcons();
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Goes over pending loading requests and displays loaded photos. If some of
     * the photos still haven't been loaded, sends another request for image
     * loading.
     */
    private void processLoadedIcons() {
        Iterator<ImageView> iterator = mPendingRequests.keySet().iterator();
        while (iterator.hasNext()) {
            ImageView view = iterator.next();
            FileId fileId = mPendingRequests.get(view);
            boolean loaded = loadCachedIcon(view, fileId.mPath, fileId.mCategory);
            if (loaded) {
                iterator.remove();
                if (mIconLoadListener != null) {
                    mIconLoadListener.onIconLoadFinished(view);
                }
            }
        }

        if (!mPendingRequests.isEmpty()) {
            requestLoading();
        }
    }

    /**
     * The thread that performs loading of photos from the database.
     */
    private class LoaderThread extends HandlerThread implements Callback {
        private Handler mLoaderThreadHandler;

        public LoaderThread() {
            super(LOADER_THREAD_NAME);
        }

        /**
         * Sends a message to this thread to load requested photos.
         */
        public void requestLoading() {
            if (mLoaderThreadHandler == null) {
                mLoaderThreadHandler = new Handler(getLooper(), this);
            }
            mLoaderThreadHandler.sendEmptyMessage(0);
        }

        /**
         * Receives the above message, loads photos and then sends a message to
         * the main thread to process them.
         */
        public boolean handleMessage(Message msg) {
            Iterator<FileId> iterator = mPendingRequests.values().iterator();
            while (iterator.hasNext()) {
                FileId id = iterator.next();
                ImageHolder holder = getImageCache().get(id.mPath);
                if (holder != null && holder.state == ImageHolder.NEEDED) {
                    // Assuming atomic behavior
                    holder.state = ImageHolder.LOADING;
                    switch (id.mCategory) {
                        case APK:
                            Drawable icon = getApkIcon(id.mPath);
                            holder.setImage(icon);
                            break;
                        case VIDEO:
                            holder.setImage(getVideoThumbnail(id.mPath));

                            break;
                    }

                    holder.state = BitmapHolder.LOADED;
                    getImageCache().put(id.mPath, holder);
                }
            }

            mMainThreadHandler.sendEmptyMessage(MESSAGE_ICON_LOADED);
            return true;
        }

        private Drawable getVideoThumbnail(String path) {
            Bitmap bitmap = null;
            bitmap = (Bitmap) CacheFactory.getCacheManager(CacheFactory.TYPE_CACHE.TYPE_IMAGE).getResourceFromMem(UtilsConfig.IMAGE_CACHE_CATEGORY_RAW,
                    path);
            if (bitmap != null) {
                if (mIconProcessFilter != null) {
                    return mIconProcessFilter.onIconPress(bitmap);
                } else {
                    return new BitmapDrawable(mContext.getResources(), bitmap);
                }
            }

            if (path.startsWith(SmbUtil.SCHEMA_SMB)) {
                path = SmbUtil.generateSmbPlayPath(path);
            }

            File thumbnailFile = PlayerUtil.makeVideoThumbnail(mContext, path, 6000);
            if (thumbnailFile != null) {
                bitmap = BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath());
                CacheFactory.getCacheManager(CacheFactory.TYPE_CACHE.TYPE_IMAGE).putResource(UtilsConfig.IMAGE_CACHE_CATEGORY_RAW, path, bitmap);
            }

            if (bitmap != null) {
                if (mIconProcessFilter != null) {
                    return mIconProcessFilter.onIconPress(bitmap);
                } else {
                    return new BitmapDrawable(mContext.getResources(), bitmap);
                }
            }

            if (mIsThumnbailMode) {
                return getDefaultVideoDrawable();
            } else {
                return mContext.getResources().getDrawable(R.drawable.icon_video_bg);
            }
        }

        public Drawable getApkIcon(String apkPath) {
            if (TextUtils.isEmpty(apkPath)) {
                return null;
            }

            if (apkPath.startsWith(SmbUtil.SCHEMA_SMB)) {
                String localApkPath = SmbUtil.getSmbApkFullPath(mContext, apkPath);
                AppConfig.LOGD("[[FileIconLoader]] getApkIcon localApkPath=" + localApkPath + ", apkPath=" + apkPath);
                SmbUtil.downloadSmbFile(localApkPath, apkPath);
                apkPath = localApkPath;
            }

            PackageManager pm = mContext.getPackageManager();
            PackageInfo info = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
            if (info != null) {
                ApplicationInfo appInfo = info.applicationInfo;
                appInfo.sourceDir = apkPath;
                appInfo.publicSourceDir = apkPath;
                try {
                    return appInfo.loadIcon(pm);
                } catch (OutOfMemoryError e) {
                    AppConfig.LOGD("[[FileIconLoader]] getApkIcon " + e.getMessage());
                }
            }
            return null;
        }
    }

    public static interface OnIconProcessFilter {
        public Drawable onIconPress(Bitmap bitmap);
    }

    /**
     * 在历史记录页面，如果Drawable改变了，并且大小不一样，那么会导致焦点出现问题，这里将默认的Drawable改变成与视频截图一样的大小
     *
     * @return
     */
    private Drawable getDefaultVideoDrawable() {
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.video_thumnail_default);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, PlayerUtil.THUMBNAIL_WIDTH, PlayerUtil.THUMBNAIL_HEIGHT, false);
        bitmap.recycle();
        return new BitmapDrawable(mContext.getResources(), resizedBitmap);
    }
}
