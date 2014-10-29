package com.kankan.player.view;

import android.app.Dialog;
import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.xunlei.tv.player.R;

/**
 * Created by wangyong on 14-5-6.
 */
public class RemoteDialog extends Dialog {

    private AnimateLinearLayout mRoot;
    private View mleftll;
    private View mRightll;

    private ImageView mBtnLeft;
    private ImageView mBtnRight;

    public RemoteDialog(Context context) {
        super(context);
        init();
    }

    public RemoteDialog(Context context, int theme) {
        super(context, theme);
        init();
    }

    protected RemoteDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        init();
    }

    private void init(){
        setContentView(R.layout.layout_alert_remote);

        this.mRoot = (AnimateLinearLayout) findViewById(R.id.root);
        this.mleftll = findViewById(R.id.left_ll);
        this.mRightll = findViewById(R.id.right_ll);

        this.mBtnLeft = (ImageView) findViewById(R.id.iv_left);
        this.mBtnRight = (ImageView) findViewById(R.id.iv_right);

        this.mleftll.requestFocus();
        mRoot.setCurrentFocusedId(0);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        View view = getCurrentFocus();
        if(view.getId() == R.id.left_ll){

            if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT){
                this.mleftll.clearFocus();
                this.mRightll.requestFocus();
                mRoot.setCurrentFocusedId(1);
                mRoot.postInvalidate();
                return true;
            }
        }

        if(view.getId() == R.id.right_ll){
            if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT){
                this.mRightll.clearFocus();
                this.mleftll.requestFocus();
                mRoot.setCurrentFocusedId(0);
                mRoot.postInvalidate();
                return true;
            }
        }


        return super.onKeyDown(keyCode, event);
    }
}
