package com.kankan.player.view;

import android.app.Dialog;
import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.xunlei.tv.player.R;

public class CustomToast extends Dialog {

    public CustomToast(Context context, String message, OnDismissListener listener) {
        super(context, R.style.toast);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.view_custom_toast, null);
        TextView messageTv = (TextView) view.findViewById(R.id.message);
        messageTv.setText(message);
        setContentView(view);

        show();
        setCanceledOnTouchOutside(true);
        setCancelable(true);

        if (listener != null) {
            setOnDismissListener(listener);
        }
    }

    public CustomToast(Context context, String message) {
        this(context, message, null);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            dismiss();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
