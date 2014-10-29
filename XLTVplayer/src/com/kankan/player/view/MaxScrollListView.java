package com.kankan.player.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

public class MaxScrollListView extends ListView {
    public MaxScrollListView(Context context) {
        super(context);
    }

    public MaxScrollListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MaxScrollListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * 默认的maxScrollAmount可能太小，导致需要按两下一个item才能完全被显示
     *
     * @return
     */
    @Override
    public int getMaxScrollAmount() {
        return (int) (0.5 * (getBottom() - getTop()));
    }
}