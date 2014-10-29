package com.kankan.player.activity;

import android.app.Activity;
import android.os.Bundle;
import com.umeng.analytics.MobclickAgent;

public abstract class BaseActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        MobclickAgent.onPageStart(getUmengPageName());
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        MobclickAgent.onPageEnd(getUmengPageName());
        MobclickAgent.onPause(this);
    }

    protected abstract String getUmengPageName();
}
