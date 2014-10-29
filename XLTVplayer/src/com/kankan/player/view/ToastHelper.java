package com.kankan.player.view;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

/**
 * Created by wangyong on 14-4-16.
 */
public class ToastHelper {

    private static Toast toast;

    public static void showMessage(Context context, int msgId){
        showMessage(context, context.getString(msgId));
    }

    public static void showMessage(Context context, String msg){
        synchronized (ToastHelper.class){
            if(toast == null){
                toast = Toast.makeText(context, "",Toast.LENGTH_SHORT);
            }
        }

        toast.setText(msg);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM,0,0);
        toast.show();

    }


}
