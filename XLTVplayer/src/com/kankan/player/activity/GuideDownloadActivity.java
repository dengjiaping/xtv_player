package com.kankan.player.activity;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.kankan.player.app.Constants;
import com.kankan.player.fragment.DownloadEntryFragment;
import com.kankan.player.fragment.TdGuideFragment;
import com.kankan.player.item.DeviceItem;
import com.kankan.player.item.TdGuideItem;
import com.kankan.player.manager.UsbManager;
import com.kankan.player.service.TdScanService;
import com.xunlei.tv.player.R;

import java.util.List;

/**
 * Created by wangyong on 14-5-19.
 */
public class GuideDownloadActivity extends Activity {


    private FragmentManager mFragmentMgr;

    private TdGuideFragment mTdGuideFragment;
    private TextView mPageTitleTv;
    private ImageView mThunderIconIv;
    private TextView mUsrNameTv;
    private ImageView mMenuIconIv;
    private TextView mMenuTv;

    private int mRemoteDeviceType;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivity_guide_download);

        View topbar = findViewById(R.id.top_bar);
        this.mPageTitleTv = (TextView) topbar.findViewById(R.id.title);
        this.mThunderIconIv = (ImageView) topbar.findViewById(R.id.xunlei_iv);
        this.mUsrNameTv = (TextView) topbar.findViewById(R.id.status_tv);
        this.mMenuIconIv = (ImageView) topbar.findViewById(R.id.menu_iv);
        this.mMenuTv = (TextView) topbar.findViewById(R.id.menu_tv);

        mFragmentMgr = getFragmentManager();

        this.mMenuIconIv.setVisibility(View.GONE);
        this.mUsrNameTv.setVisibility(View.GONE);
        this.mMenuIconIv.setVisibility(View.VISIBLE);
        this.mMenuTv.setVisibility(View.VISIBLE);

        this.mMenuIconIv.setImageResource(R.drawable.bar_left_right);
        this.mMenuTv.setText(R.string.remote_bar_left_right_tv);

        Intent intent  = getIntent();
        int type = intent.getIntExtra(Constants.KEY_SHOW_GUIDE, -1);
        mRemoteDeviceType = intent.getIntExtra(Constants.KEY_REMOTE_TYPE, -1);
        if(type == Constants.KEY_SHOW_MOBILE_GUIDE){
            go2TdMobileDownFragment();
            this.mPageTitleTv.setText(getString(R.string.remote_title_guidedownload));

        }

        if(type == Constants.KEY_SHOW_WEB_GUIDE){
            go2TdWebDownFragment();
            this.mPageTitleTv.setText(getString(R.string.remote_title_guidedownload));
        }


    }


    private void go2TdWebDownFragment() {
        mTdGuideFragment = new TdGuideFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.KEY_GUIDE_TYPE, TdGuideItem.GuideItemType.WEB_DOWN.ordinal());
        bundle.putInt(Constants.KEY_REMOTE_TYPE,mRemoteDeviceType);
        mTdGuideFragment.setArguments(bundle);
        FragmentTransaction trans = mFragmentMgr.beginTransaction();
        trans.replace(R.id.container_fl, mTdGuideFragment);
        trans.commit();
    }

    private void go2TdMobileDownFragment() {
        mTdGuideFragment = new TdGuideFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.KEY_GUIDE_TYPE, TdGuideItem.GuideItemType.MOBILE_DOWN.ordinal());
        bundle.putInt(Constants.KEY_REMOTE_TYPE,mRemoteDeviceType);
        mTdGuideFragment.setArguments(bundle);
        FragmentTransaction trans = mFragmentMgr.beginTransaction();
        trans.replace(R.id.container_fl, mTdGuideFragment);
        trans.commit();
    }
}