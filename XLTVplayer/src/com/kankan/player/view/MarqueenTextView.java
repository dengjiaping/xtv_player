package com.kankan.player.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TextView;
import com.kankan.player.app.AppConfig;
import com.xunlei.tv.player.R;

public class MarqueenTextView extends TextView implements Runnable {
    private int mCurrentScrollX;
    private boolean mIsStop = true;
    private int mTextWidth;
    private boolean mIsMeasure = false;
    //一般而言初始scrollX为0，但是在某些嵌套的环境下，可能不为0，所以要获取一个初始值作为基准
    private int mInitialScrollX;
    private boolean mInitialized = false;
    private int mBaseScrollX;

    public MarqueenTextView(Context context) {
        super(context);
    }

    public MarqueenTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MarqueenTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!mIsMeasure) {
            getTextWidth();
        }

        if (!mInitialized) {
            mInitialized = true;
            mInitialScrollX = getScrollX() + (mTextWidth > getWidth() ? (mTextWidth - getWidth()) / 2 : 0);
            mBaseScrollX = mInitialScrollX;
        }

        if (!mIsMeasure) {
            mIsMeasure = true;
            mBaseScrollX = mInitialScrollX - (mTextWidth > getWidth() ? (mTextWidth - getWidth()) / 2 : 0);
            setScrollX(mBaseScrollX);
        }
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        mIsMeasure = false;
        super.setText(text, type);
    }

    private void getTextWidth() {
        Paint paint = this.getPaint();
        String str = this.getText().toString();
        mTextWidth = (int) paint.measureText(str);
    }

    @Override
    public void run() {
        mCurrentScrollX += 2;
        scrollTo(mBaseScrollX + mCurrentScrollX, 0);
        if (mIsStop) {
            return;
        }

        if (mCurrentScrollX >= (mTextWidth)) {
            mCurrentScrollX = -getWidth();
            scrollTo(mBaseScrollX + mCurrentScrollX, 0);
        }
        postDelayed(this, 60);
    }

    public void stopMarqueen() {
        mIsStop = true;
        mCurrentScrollX = 0;
        removeCallbacks(this);
    }

    public void startMarqueen() {
        if (mTextWidth > getWidth()) {
            mIsStop = false;
            this.removeCallbacks(this);
            post(this);
        } else {
            mIsStop = true;
        }
    }

    public void restartMarqueen(String text) {
        stopMarqueen();
        setText(text);

        postDelayed(new Runnable() {
            @Override
            public void run() {
                startMarqueen();
            }
        }, 300L);
    }
}