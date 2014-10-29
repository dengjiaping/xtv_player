package com.plugin.common.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.plugin.common.utils.image.ImageUtils;
import com.polites.android.GestureImageViewListener;
import com.polites.android.RectF;

@Deprecated
public class RegionWebGestureImageView extends WebGestureImageView implements GestureImageViewListener {

    private static final int MOVE_OVER_CONFIRM_DELAY = 100;

    private RegionBitmap mRegionBitmap;

    private Rect mRegionDrawRect;

    private String mFullPath;

    private RectF mDestRectF = new RectF();

    private RectF mWindowRectF;

    private boolean mOnMove = false;

    private boolean mCanDrawRegion = true;

    private Handler mDrawHandler = new Handler(Looper.getMainLooper());
    
    public RegionWebGestureImageView(Context context) {
        super(context);
    }
    
    public RegionWebGestureImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void init() {
        setGestureImageViewListener(this);
    }

    public void setImageFullPath(String path) {
        mFullPath = path;
        
        if (TextUtils.isEmpty(path)) {
            if (mRegionBitmap != null && mRegionBitmap.btDraw != null && !mRegionBitmap.btDraw.isRecycled()) {
                mRegionBitmap.btDraw.recycle();
            }
            mRegionBitmap = null;
        }
    }
    
    @Override
    protected void setImageBitmap(Bitmap bt, boolean withAnim) {
        super.setImageBitmap(bt, withAnim);
        if (!TextUtils.isEmpty(mUrl)) {
            this.setImageFullPath(mImageCache.getResourcePath(mCategory, mUrl));
        }
    }
    
    public void setStartDrawRegion(boolean drawRegion) {
        mCanDrawRegion = drawRegion;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mCanDrawRegion && !TextUtils.isEmpty(mFullPath)) {
            if (mRegionBitmap != null && mRegionBitmap.btDraw != null && !mRegionBitmap.btDraw.isRecycled()) {
                mRegionBitmap.btDraw.recycle();
            }
            mRegionBitmap = null;

            RectF destRectF = caculateDestRectF();
            RectF windowRectF = getWindowRectF();

            if (RectF.intersects(destRectF, windowRectF)) {
                mRegionBitmap = ImageRegionDecodeUtils.getRegionBitmap(this.mFullPath, destRectF,
                        intersect(destRectF, windowRectF));
                if (mRegionBitmap != null && mRegionBitmap.bitmapDrawRect != null && mRegionBitmap.btDraw != null
                        && !mRegionBitmap.btDraw.isRecycled()) {
                    canvas.save();

                    Drawable regionDrawable = new BitmapDrawable(getResources(), mRegionBitmap.btDraw);

                    if (mRegionDrawRect == null) {
                        mRegionDrawRect = new Rect();
                    }
                    mRegionDrawRect.set(Math.round(mRegionBitmap.bitmapDrawRect.left),
                            Math.round(mRegionBitmap.bitmapDrawRect.top),
                            Math.round(mRegionBitmap.bitmapDrawRect.right),
                            Math.round(mRegionBitmap.bitmapDrawRect.bottom));
                    regionDrawable.setBounds(mRegionDrawRect);

                    regionDrawable.draw(canvas);

                    canvas.restore();
                }
            }
        }
    }

    private RectF caculateDestRectF() {
        if (mDestRectF == null) {
            mDestRectF = new RectF();
        }
        mDestRectF.left = getImageX() - (getImageWidth() * getScale() / 2.0f);
        mDestRectF.top = getImageY() - (getImageHeight() * getScale() / 2.0f);
        mDestRectF.right = mDestRectF.left + getImageWidth() * getScale();
        mDestRectF.bottom = mDestRectF.top + getImageHeight() * getScale();

        return mDestRectF;
    }

    private RectF getWindowRectF() {
        if (mWindowRectF == null) {
            mWindowRectF = new RectF();
        }
        if (mWindowRectF.width() == 0 || mWindowRectF.height() == 0) {
            mWindowRectF.left = 0;
            mWindowRectF.top = 0;
            mWindowRectF.right = mWindowRectF.left + getWidth();
            mWindowRectF.bottom = mWindowRectF.top + getHeight();
        }

        return mWindowRectF;
    }

    private RectF intersect(RectF one, RectF two) {
        RectF ret = new RectF();
        if (RectF.intersects(one, two)) {
            ret.left = Math.max(one.left, two.left);
            ret.top = Math.max(one.top, two.top);
            ret.right = Math.min(one.right, two.right);
            ret.bottom = Math.min(one.bottom, two.bottom);

            return ret;
        }

        return ret;
    }

    private static final class RegionBitmap {
        RectF bitmapDrawRect;
        Bitmap btDraw;

        @Override
        public String toString() {
            return "RegionBitmap [bitmapDrawRect=" + bitmapDrawRect + ", btDraw=" + btDraw + "]";
        }
    }

    private static final class ImageRegionDecodeUtils {
        static RegionBitmap getRegionBitmap(String btFullPath, RectF btCurRect, RectF drawRect) {
            if (!TextUtils.isEmpty(btFullPath) && btCurRect != null && drawRect != null && btCurRect.width() > 0
                    && btCurRect.height() > 0 && drawRect.width() > 0 && drawRect.height() > 0) {
                if (btCurRect.width() > drawRect.width() || btCurRect.height() > drawRect.height()
                        || btCurRect.contains(drawRect)) {
                    try {
                        BitmapFactory.Options opts = ImageUtils.getBitmapHeaderInfo(btFullPath);
                        if (opts != null) {
                            int btWidth = opts.outWidth;
                            int btHeight = opts.outHeight;

                            float scaleX = (float) ((btWidth * 1.0) / btCurRect.width());
                            float scaleY = (float) ((btHeight * 1.0) / btCurRect.height());

                            Rect decodeRect = new Rect();
                            int decodeWidth = (int) (drawRect.width() * scaleX);
                            int decodeHeight = (int) (drawRect.height() * scaleY);
                            decodeRect.left = (int) (Math.abs((btCurRect.left - drawRect.left)) * scaleX);
                            decodeRect.top = (int) (Math.abs(btCurRect.top - drawRect.top) * scaleY);
                            decodeRect.right = decodeRect.left + decodeWidth;
                            decodeRect.bottom = decodeRect.top + decodeHeight;

                            RegionBitmap ret = new RegionBitmap();
                            ret.bitmapDrawRect = drawRect;
                            ret.btDraw = ImageUtils.getBitmapFromRegionDecoder(decodeRect, btFullPath);

//                          Config.LOGD("decodeRect = " + decodeRect.toString() + " btCurRect = " + btCurRect
//                                  + " drawRect = " + drawRect + " bt size = (" + btWidth + ", " + btHeight + ")"
//                                  + " scaleX = " + scaleX + " scaleY = " + scaleY + " ret.btDraw size = ("
//                                  + (ret.btDraw != null ? ret.btDraw.getWidth() : 0) + ", "
//                                  + (ret.btDraw != null ? ret.btDraw.getHeight() : 0) + ")");

                            return ret;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            return null;
        }
    }

    @Override
    public void onTouch(float x, float y) {
        mOnMove = true;
        mCanDrawRegion = false;
    }

    @Override
    public void onScale(float scale) {
        mOnMove = true;
        mCanDrawRegion = false;
    }

    @Override
    public void onPosition(float x, float y) {
        mOnMove = true;
        mCanDrawRegion = false;
    }

    @Override
    public void onMoveOver() {
        mOnMove = false;
        mDrawHandler.removeCallbacksAndMessages(null);
        mDrawHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mOnMove) {
                    mCanDrawRegion = true;
                    RegionWebGestureImageView.this.redraw();
                }
            }
        }, MOVE_OVER_CONFIRM_DELAY);
    }

}
