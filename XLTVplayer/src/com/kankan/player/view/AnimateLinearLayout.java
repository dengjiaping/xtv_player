package com.kankan.player.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class AnimateLinearLayout extends LinearLayout {
    int mCurrentFocusedChildId;

    public AnimateLinearLayout(Context context) {
        super(context);

        setChildrenDrawingOrderEnabled(true);
    }

    public AnimateLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        setChildrenDrawingOrderEnabled(true);
    }

    public AnimateLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setChildrenDrawingOrderEnabled(true);
    }

    public void setCurrentFocusedId(int id) {
        mCurrentFocusedChildId = id;
    }

    public void setCurrentFocusedId(View childView) {
        mCurrentFocusedChildId = indexOfChild(childView);
        invalidate();
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (i == mCurrentFocusedChildId) {
            return childCount - 1;
        }

        if (i == childCount - 1) {
            return mCurrentFocusedChildId;
        }

        return i;
    }

}
