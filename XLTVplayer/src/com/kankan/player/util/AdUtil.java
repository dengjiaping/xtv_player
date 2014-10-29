package com.kankan.player.util;

import android.content.Context;
import android.os.AsyncTask;
import com.kankan.player.api.rest.ads.GetAdvImagesRequest;
import com.kankan.player.api.rest.ads.GetAdvImagesResponse;
import com.kankan.player.app.AppConfig;
import com.plugin.internet.InternetUtils;
import com.plugin.internet.core.NetWorkException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 广告工具，获取广告图片，策略是如果图片张数发生变化了之后，打开应用会立即重新更新所有图片，如果张数没发生变化，每天第一次打开应用的时候会更新这些图片
 */
public class AdUtil {
    public static final String ADS_IMAGE_DIR = "adv_images";
    private static final String PREFIX_DONE = ".xl";
    private static final String PREFIX_DOWNLOADING = ".tmp";

    private static AdUtil instance;

    private final Context mContext;
    private File mImagesDir;
    private List<GetAdvImagesResponse.AdvImage> mImages = new ArrayList<GetAdvImagesResponse.AdvImage>();

    private AdUtil(Context context) {
        mContext = context;
        mImagesDir = new File(context.getExternalCacheDir(), ADS_IMAGE_DIR);
        if (!mImagesDir.exists()) {
            mImagesDir.mkdirs();
        }
    }

    public static AdUtil getInstance(Context context) {
        if (instance == null) {
            synchronized (AdUtil.class) {
                if (instance == null) {
                    instance = new AdUtil(context);
                }
            }
        }
        return instance;
    }

    public boolean canDownload() {
        if (!mImagesDir.exists() || !mImagesDir.canRead() || !mImagesDir.canWrite()) {
            return false;
        }
        return true;
    }

    public boolean hasAdImage() {
        return mImages != null && mImages.size() > 0;
    }

    /**
     * 随机取一张广告图片，按权重来算
     *
     * @return
     */
    public GetAdvImagesResponse.AdvImage getRandomImage() {
        int total = 0;
        int current = 0;
        for (GetAdvImagesResponse.AdvImage image : mImages) {
            total += image.orderVal;
        }

        int seed = (int) (Math.floor(Math.random() * total)) + 1;
        AppConfig.LOGD("[[AdUtil]] getRandomImage seed = " + seed + ", total = " + total);
        for (GetAdvImagesResponse.AdvImage image : mImages) {
            current += image.orderVal;
            if (current >= seed) {
                AppConfig.LOGD("[[AdUtil]] getRandomImage index=" + image.id + ", path=" + image.images + ", order=" + image.orderVal);
                return image;
            }
        }

        return null;
    }

    /**
     * 默认将图片下载到external cache dir
     *
     * @return
     */
    public File[] getLocalImages() {
        if (!canDownload()) {
            return null;
        }

        mImages.clear();
        return mImagesDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                // 下载过程中会命名为.tmp文件，下载完成后改成.xl文件
                if (filename.startsWith(".") || !filename.endsWith(PREFIX_DONE)) {
                    return false;
                }

                GetAdvImagesResponse.AdvImage image = new GetAdvImagesResponse.AdvImage();
                image.images = new File(dir, filename).getAbsolutePath();
                int indexBegin = filename.indexOf('-');
                int indexEnd = filename.indexOf('.');
                if (indexBegin != -1 && indexEnd != -1) {
                    try {
                        image.orderVal = Integer.parseInt(filename.substring(indexBegin + 1, indexEnd));
                        image.id = Integer.parseInt(filename.substring(0, indexBegin));
                    } catch (NumberFormatException e) {
                    }
                }
                mImages.add(image);
                return true;
            }
        });
    }

    public void downloadImages() {
        if (!canDownload()) {
            AppConfig.LOGD("[[AdUtil]] downloadImages can not download image.");
            return;
        }

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                GetAdvImagesRequest request = new GetAdvImagesRequest();
                int widthPixels = mContext.getResources().getDisplayMetrics().widthPixels;
                if (widthPixels > 1280) {
                    request.resolution = 960;
                } else {
                    request.resolution = 720;
                }
                try {
                    AppConfig.LOGD("[[AdUtil]] downloadImages resolution: " + request.resolution);
                    GetAdvImagesResponse response = InternetUtils.request(mContext, request);
                    if (response != null && response.rtnCode == 0 && response.data != null) {
                        int size = response.data.length;
                        if (size > 0) {
                            File[] images = getLocalImages();
                            if (images == null || (images != null && (images.length != size || images.length == 0)) || refreshAfterOneDay()) {
                                clearAllAdImages();
                                mImages.clear();

                                // try download one by one
                                for (int i = 0; i < size; i++) {
                                    downloadImageByUrl(response.data[i], i + 1);
                                }
                            }
                        }
                    }
                } catch (NetWorkException e) {
                    AppConfig.LOGD("[[AdUtil]] downloadImages request network error.");
                }
            }
        });
    }

    private void downloadImageByUrl(GetAdvImagesResponse.AdvImage image, int index) {
        HttpURLConnection connection = null;
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        boolean isSuccess = false;
        File saveFile = new File(mImagesDir, index + PREFIX_DOWNLOADING);
        try {
            URL u = new URL(image.images);
            connection = (HttpURLConnection) u.openConnection();
            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                bis = new BufferedInputStream(connection.getInputStream());
                bos = new BufferedOutputStream(new FileOutputStream(saveFile));

                byte[] buf = new byte[8 * 1024];
                int len;
                while ((len = bis.read(buf)) > 0) {
                    bos.write(buf, 0, len);
                }
                bos.flush();
                bis.close();
                bos.close();
                connection.disconnect();
                isSuccess = true;
            }
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
                if (bos != null) {
                    bos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (connection != null) {
                connection.disconnect();
            }
        }

        // 改名字
        if (isSuccess) {
            File destFile = new File(mImagesDir, index + "-" + image.orderVal + PREFIX_DONE);
            if (destFile.exists()) {
                destFile.delete();
            }
            saveFile.renameTo(destFile);

            image.images = destFile.getAbsolutePath();
            image.id = index;
            mImages.add(image);

            AppConfig.LOGD("[[AdUtil]] success download " + image.images + " to " + destFile.getAbsolutePath());
        }
    }

    /**
     * 每天刷新
     *
     * @return
     */
    private boolean refreshAfterOneDay() {
        long lastFetchTime = SettingManager.getInstance().getAdvLastFetchTime();
        int d = DateTimeFormatter.daysBetween(lastFetchTime, System.currentTimeMillis());
        if (d > 0) {
            SettingManager.getInstance().setAdvLastFetchTime(System.currentTimeMillis());
        }
        return d > 0;
    }

    private void clearAllAdImages() {
        if (mImagesDir.exists() && mImagesDir.canRead()) {
            File[] files = mImagesDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
        }
    }
}
