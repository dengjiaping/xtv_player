/**

 * TipsDialog.java
 */
package com.kankan.player.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import com.xunlei.tv.player.R;

/**
 * @author Guoqing Sun Sep 10, 20124:28:18 PM
 */
public class TipsDialog {
    
    private static final int DISMISS_TIME = 2000;

    class CustomDialog extends Dialog implements OnClickListener {
        
        ImageView progress;
        
        Animation rotate;

        CustomDialog(Context context) {
            super(context, R.style.tips_dialog);
            // super(context);

            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            setContentView(R.layout.tips_layout);
            progress = (ImageView) findViewById(R.id.progress_icon);
            rotate = AnimationUtils.loadAnimation(context, R.anim.rotate_center);
            progress.startAnimation(rotate);
            // progress.setImageResource(R.drawable.big_loading_anim_icon);
        }

        /*
         * (non-Javadoc)
         * 
         * @see android.view.View.OnClickListener#onClick(android.view.View)
         */
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
        }
        
        void setAnimation(boolean start) {
            if (start) {
                progress.startAnimation(rotate);
            } else {
                progress.clearAnimation();
            }
        }

        void setImageResource(int id) {
            progress.clearAnimation();
            progress.setImageResource(id);
        }

        void setTips(String tips) {
            TextView tv = (TextView) findViewById(R.id.tips);
            if (!TextUtils.isEmpty(tips)) {
                tv.setText(tips);
            } else {
                tv.setVisibility(View.GONE);
            }
        }
    }

    private static final String TAG = "TipsDialog";
    private static TipsDialog gTipsDialog = new TipsDialog();
    
    private static Handler handler = new Handler();

    public static TipsDialog getInstance() {
        if (gTipsDialog == null) {
            gTipsDialog = new TipsDialog();
        }

        return gTipsDialog;
    }

    private CustomDialog mDialog;
    
    private TipsDialog() {
    }

    public void dismiss() {
        if ((mDialog != null) && mDialog.isShowing()) {
            try {
                mDialog.dismiss();
            } catch (Exception e) {
            }
            mDialog = null;
        }
    }

    @Deprecated
    public void showProcess(Activity a, int iconResId, String tips, boolean rotateAnim) {
        dismiss();

        mDialog = new CustomDialog(a);
        mDialog.setImageResource(iconResId);
        mDialog.setTips(tips);
        mDialog.setAnimation(rotateAnim);
        mDialog.setCanceledOnTouchOutside(false);
        try {
            mDialog.show();
        } catch (Exception e) {
        }
    }
    
    public void show(Activity a, int iconResId, int tipsRes, boolean rotateAnim, boolean autoDismiss) {
        dismiss();
        if (a != null) {
            show(a, iconResId, a.getString(tipsRes), rotateAnim, autoDismiss, true);
        }
    }
    
    public void show(Activity a, int iconResId, int tipsRes, boolean rotateAnim, boolean autoDismiss, boolean cancel) {
        dismiss();
        if (a != null) {
            show(a, iconResId, a.getString(tipsRes), rotateAnim, autoDismiss, cancel);
        }
    }
    
    public void show(Activity a, int iconResId, String msg, boolean rotateAnim, boolean autoDismiss) {
        dismiss();
        
        if (a != null) {
            show(a, iconResId, msg, rotateAnim, autoDismiss, true);
        }
    }
    
    public void show(Activity a, int iconResId, String msg, boolean rotateAnim, boolean autoDismiss, boolean cancel) {
        dismiss();
        
        if (a != null) {
            mDialog = new CustomDialog(a);
            mDialog.setImageResource(iconResId);
            mDialog.setTips(msg);
            mDialog.setAnimation(rotateAnim);
            mDialog.setCancelable(cancel);
            mDialog.setCanceledOnTouchOutside(false);
            try {
                mDialog.show();
            } catch (Exception e) {
            }
            
            if (autoDismiss) {
                final Dialog showDialog = mDialog;
                handler.postDelayed(new Runnable() {
                    
                    @Override
                    public void run() {
                        if (showDialog != null && showDialog.isShowing()) {
                            try {
                                showDialog.dismiss();
                            } catch (Exception e) {
                            }
                        }
                    }
                }, DISMISS_TIME);
            }
        }
    }

    public void show(Activity a, int iconResId, String tipsStr, boolean autoDismiss) {
        dismiss();

        if (a != null) {
            mDialog = new CustomDialog(a);
            mDialog.setImageResource(iconResId);
            mDialog.setTips(tipsStr);
            mDialog.setAnimation(false);
            mDialog.setCanceledOnTouchOutside(false);
            try {
                mDialog.show();
            } catch (Exception e) {
            }

            if (autoDismiss) {
                final Dialog showDialog = mDialog;
                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        if (showDialog != null && showDialog.isShowing()) {
                            try {
                                showDialog.dismiss();
                            } catch (Exception e) {
                            }
                        }
                    }
                }, DISMISS_TIME);
            }
        }
    }

    public void show(Activity a, int iconResId, int tipsRes, boolean autoDismiss) {
        dismiss();
        
        if (a != null) {
            mDialog = new CustomDialog(a);
            mDialog.setImageResource(iconResId);
            mDialog.setTips(a.getString(tipsRes));
            mDialog.setAnimation(false);
            mDialog.setCanceledOnTouchOutside(false);
            try {
                mDialog.show();
            } catch (Exception e) {
            }
            
            if (autoDismiss) {
                final Dialog showDialog = mDialog;
                handler.postDelayed(new Runnable() {
                    
                    @Override
                    public void run() {
                        if (showDialog != null && showDialog.isShowing()) {
                            try {
                                showDialog.dismiss();
                            } catch (Exception e) {
                            }
                        }
                    }
                }, DISMISS_TIME);
            }
        }
    }
    
    public void setUncanceled() {
        if (mDialog != null) {
            mDialog.setCancelable(false);
            mDialog.setCanceledOnTouchOutside(false);
        }
    }
}
