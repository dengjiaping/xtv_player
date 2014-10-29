package com.kankan.player.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import com.kankan.player.app.AppConfig;
import com.kankan.player.app.Constants;
import com.xunlei.tv.player.R;

/**
 * Created by wangyong on 14-8-5.
 */
public class RemoteUsrHelpActivity extends BaseActivity {

    private WebView mWebView;

    private BaseWebViewClient mBaseWebClient;
    private BaseChromClient mBaseChromClient;

    private TextView mTitleTv;
    private ImageView mLogoIv;
    private TextView mStatusTv;
    private View mMenuView;

    public static void launchUserHelpPage(Activity activity){
        Intent intent = new Intent(activity,RemoteUsrHelpActivity.class);
        activity.startActivity(intent);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usrhelp);

        init();
    }

    @Override
    protected String getUmengPageName() {
        return "remoteusrhelpactivity";
    }

    private void init(){

        mTitleTv = (TextView) findViewById(R.id.title);
        mLogoIv = (ImageView) findViewById(R.id.xunlei_iv);
        mStatusTv = (TextView) findViewById(R.id.status_tv);
        mMenuView = findViewById(R.id.rl);

        mTitleTv.setText(getString(R.string.remote_usrhelp_title));
        mLogoIv.setVisibility(View.GONE);
        mStatusTv.setVisibility(View.GONE);
        mMenuView.setVisibility(View.GONE);


        mWebView = (WebView) findViewById(R.id.wv);

        //支持js
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);

        this.mBaseWebClient = new BaseWebViewClient();
        this.mBaseChromClient = new BaseChromClient();
        mWebView.setWebViewClient(this.mBaseWebClient);
        mWebView.setWebChromeClient(this.mBaseChromClient);

        //设置滚动条样式
        mWebView.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);


//        Intent intent = getIntent();
//        if(intent != null){
//            String url = intent.getStringExtra(Constants.KEY_REMOTE_HELP_URL);
//            if(!TextUtils.isEmpty(url)){
//                mWebView.loadUrl(url);
//            }
//        }

        mWebView.loadUrl(AppConfig.TD_USR_HELP_URL);


    }

    class BaseWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
        }
    }

    class BaseChromClient extends WebChromeClient {


    }
}