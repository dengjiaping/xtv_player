package com.kankan.player.explorer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.widget.ImageView;
import com.xunlei.tv.player.R;

/**
 * Created by zhangdi on 14-3-30.
 */
public class FileIconHelper implements FileIconLoader.IconLoadFinishListener {

    private final Context mContext;

    private FileIconLoader mIconLoader;

    public FileIconHelper(Context context) {
        mContext = context;
        mIconLoader = new FileIconLoader(context, this);
    }

    public void setThumbnailMode() {
        mIconLoader.setThumbnailMode();
    }

    public void setIconProcessFilter(FileIconLoader.OnIconProcessFilter filter) {
        if (mIconLoader != null) {
            if (filter == null) {
                // 默认的图标处理
                filter = new FileIconLoader.OnIconProcessFilter() {
                    @Override
                    public Drawable onIconPress(Bitmap bitmap) {
                        if (bitmap == null) {
                            return null;
                        }

                        StateListDrawable drawable = new StateListDrawable();

                        float density = mContext.getResources().getDisplayMetrics().density;
                        int size = (int) (61 * density);
                        Bitmap scaledVideoThum = Bitmap.createScaledBitmap(bitmap, (int) (50 * density), (int) (37.5 * density), false);

                        Bitmap selectedBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(selectedBmp);
                        canvas.drawColor(Color.WHITE);
                        canvas.drawBitmap(scaledVideoThum, (int) (5.5 * density), (int) (12 * density), null);
                        drawable.addState(new int[]{android.R.attr.state_selected}, new BitmapDrawable(mContext.getResources(), selectedBmp));

                        Bitmap normalBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                        canvas = new Canvas(normalBmp);
                        canvas.drawColor(Color.parseColor("#90b8ee"));
                        canvas.drawBitmap(scaledVideoThum, (int) (5.5 * density), (int) (12 * density), null);
                        drawable.addState(new int[]{}, new BitmapDrawable(mContext.getResources(), normalBmp));

                        scaledVideoThum.recycle();

                        return drawable;
                    }
                };
            }

            mIconLoader.setIconProcessFilter(filter);
        }
    }

    public void setIcon(ImageView fileImage, FileItem fileItem) {
        String filePath = fileItem.filePath;
        FileCategory category = fileItem.category;

        mIconLoader.cancelRequest(fileImage);

        switch (category) {
            case APK:
                mIconLoader.loadIcon(fileImage, filePath, category);
                break;
            case VIDEO:
                fileImage.setImageResource(R.drawable.icon_video_bg);
                mIconLoader.loadIcon(fileImage, filePath, category);
                break;
            case DIR:
                fileImage.setImageResource(R.drawable.icon_folder_bg);
                break;
            default:
                break;
        }
    }

    @Override
    public void onIconLoadFinished(ImageView view) {

    }

    public void pause() {
        mIconLoader.pause();
    }

    public void resume() {
        mIconLoader.resume();
    }

    public void stop() {
        mIconLoader.stop();
    }

}
