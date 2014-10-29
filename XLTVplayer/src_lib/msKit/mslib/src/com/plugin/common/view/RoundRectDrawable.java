package com.plugin.common.view;

import java.io.InputStream;

import com.plugin.common.utils.UtilsConfig;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ComposeShader;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;

class RoundRectDrawable extends BitmapDrawable {
    private static final boolean USE_VIGNETTE = false;

    private float mCornerRadius;

    private RectF mRect = new RectF();

    private BitmapShader mBitmapShader;

    private Paint mPaint;
    
    private boolean mUseCanvasClip;
    
    private int mBitmapWidth;
    
    private int mBitmapHeight;

    public RoundRectDrawable(Resources res) {
        super(res);
        init();
    }

    public RoundRectDrawable(Resources res, Bitmap bitmap) {
        super(res, bitmap);
        init();
    }

    public RoundRectDrawable(Resources res, String filepath) {
        super(res, filepath);
        init();
    }

    public RoundRectDrawable(Resources res, InputStream is) {
        super(res, is);
        init();
    }

    public RoundRectDrawable(Bitmap bt) {
        super(bt);
        init();
    }

    private void init() {
        if (getBitmap() != null) {
            mBitmapShader = new BitmapShader(getBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setShader(mBitmapShader);
            
            mBitmapWidth = getBitmap().getWidth();
            mBitmapHeight = getBitmap().getHeight();
        }
    }

    public void setConerRadius(float conerRadius) {
        mCornerRadius = conerRadius;
    }

    public void setUseCanvasClip(boolean canvasClip) {
        mUseCanvasClip = canvasClip;
    }
    
    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mRect.set(bounds);

        if (USE_VIGNETTE) {
            RadialGradient vignette = new RadialGradient(mRect.centerX(), mRect.centerY() * 1.0f / 0.7f, mRect.centerX() * 1.3f,
                    new int[] { 0, 0, 0x7f000000 }, new float[] { 0.0f, 0.7f, 1.0f }, Shader.TileMode.CLAMP);

            Matrix oval = new Matrix();
            oval.setScale(1.0f, 0.7f);
            vignette.setLocalMatrix(oval);

            mPaint.setShader(new ComposeShader(mBitmapShader, vignette, PorterDuff.Mode.SRC_OVER));
        }
    }

    @Override
    public void draw(Canvas canvas) {
//        if (UtilsConfig.UTILS_DEBUG) {
//            System.out.println("[[RoundRectDrawable::draw]] mRect = " + mRect.toString() + " mCorner = " + mCornerRadius + " ::::::::::" + " drawable = "
//                    + this);
//        }
        if (mUseCanvasClip) {
            mRect.set(canvas.getClipBounds());
        }
        
        float drawCorner = mCornerRadius;
        // corner radius should base on View size
        // now just support centerCrop
        if (mUseCanvasClip && mBitmapHeight > 0 && mBitmapWidth > 0) {
            int canvasWidth = canvas.getWidth();
            int canvasHeight = canvas.getHeight();
            
            if (canvasHeight > mBitmapHeight || canvasWidth > mBitmapWidth) {
                // scale image to large 
                float widthScale = (canvasWidth * ((float) 1.0)) / mBitmapWidth;
                float heightScale = (canvasHeight * ((float) 1.0)) / mBitmapHeight;
                float scale = (widthScale >= heightScale) ? widthScale : heightScale;
                drawCorner = mCornerRadius / scale;
            } else if (canvasWidth < mBitmapWidth || canvasHeight < mBitmapHeight) {
                // scale image to small
                float widthScale = (mBitmapWidth * ((float) 1.0)) / canvasWidth;
                float heightScale = (mBitmapHeight * ((float) 1.0)) / canvasHeight;
                float scale = (widthScale > heightScale) ? heightScale : widthScale;
                drawCorner = mCornerRadius * scale;
            }
        }
        
        canvas.drawRoundRect(mRect, drawCorner, drawCorner, mPaint);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }

}
