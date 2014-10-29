package com.kankan.player.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class TopCropImageView extends ImageView {

    Matrix matrix = new Matrix();

    public TopCropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TopCropImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public TopCropImageView(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        Drawable drawable = getDrawable();
        if (drawable != null) {
            float scaleWidth = getWidth() / (float) getDrawable().getIntrinsicWidth();
            matrix.setScale(scaleWidth, scaleWidth);
            canvas.concat(matrix);
            drawable.draw(canvas);
            canvas.restore();
        }
    }
}
