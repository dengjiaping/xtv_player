package com.kankan.player.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.xunlei.tv.player.R;

public class HistoryMenuActivity extends BaseActivity implements View.OnClickListener {
    public static final int EXTRA_CLEAR_ALL = 1;
    public static final int EXTRA_REMOVE_ITEM = 2;

    public static final String EXTRA_EDIT_MODE = "extra_edit_mode";

    LinearLayout mRemoveLl;
    LinearLayout mClearLl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_history_menu);

        getWindow().getAttributes().gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        mRemoveLl = (LinearLayout) findViewById(R.id.remove_ll);
        mClearLl = (LinearLayout) findViewById(R.id.clear_ll);

        mRemoveLl.setOnClickListener(this);
        mClearLl.setOnClickListener(this);
    }

    @Override
    protected String getUmengPageName() {
        return "HistoryMenuActivity";
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            finish();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.remove_ll:
                sendResult(EXTRA_REMOVE_ITEM);
                break;
            case R.id.clear_ll:
                sendResult(EXTRA_CLEAR_ALL);
                break;
            default:
                break;
        }
    }

    private void sendResult(int mode) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_EDIT_MODE, mode);
        setResult(RESULT_OK, intent);

        finish();
    }
}
