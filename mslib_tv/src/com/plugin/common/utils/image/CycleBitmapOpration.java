/**
 * CycleBitmapOpration.java
 */
package com.plugin.common.utils.image;

import android.graphics.Bitmap;

import com.plugin.common.utils.image.ImageDownloader.BitmapOperationListener;

/**
 * @author Guoqing Sun Nov 28, 201212:02:13 PM
 */
public final class CycleBitmapOpration implements BitmapOperationListener {

    /* (non-Javadoc)
     * @see com.sound.dubbler.utils.image.ImageDownloader.BitmapOperationListener#onAfterBitmapDownload(android.graphics.Bitmap)
     */
    @Override
    public Bitmap onAfterBitmapDownload(Bitmap downloadBt) {
        if (downloadBt != null && !downloadBt.isRecycled()) {
            return ImageUtils.createCircleBitmapWithScale(downloadBt, 150, 150);
        }
        
        return null;
    }

}
