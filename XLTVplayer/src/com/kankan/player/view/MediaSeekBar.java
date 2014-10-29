package com.kankan.player.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.SeekBar;

/**
 * 解决API16以后才有getThumb方法的问题
 */
public class MediaSeekBar extends SeekBar {
    Drawable mThumb;

    public MediaSeekBar(Context context) {
        super(context);
    }

    public MediaSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MediaSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setThumb(Drawable thumb) {
        super.setThumb(thumb);
        mThumb = thumb;
    }

    public Drawable getSeekBarThumb() {
        return mThumb;
    }
}
