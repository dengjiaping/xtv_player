package com.kankan.player.fragment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import com.kankan.player.activity.BindTdActivity;
import com.kankan.player.activity.RemoteBindTdActivity;
import com.kankan.player.app.AppConfig;
import com.kankan.player.app.Constants;
import com.kankan.player.explorer.FileItem;
import com.kankan.player.item.TdGuideItem;
import com.kankan.player.manager.LocalTDDownloadManager;
import com.kankan.player.util.UIHelper;
import com.umeng.analytics.MobclickAgent;
import com.xunlei.tv.player.R;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 * Created by wangyong on 14-5-6.
 */
public class DownloadEntryFragment extends Fragment {


    private View mGuideDownloadView;
    private View mGuidePluginDiskView;

    private ImageView mDownloadShadowView;
    private ImageView mDiskShadowView;

    private Button mBtndownload;
    private Button mBtndownFail;

    private TdGuideFragment mTdGuideFragment;

    private FragmentManager mFragmentMgr;

    private TdGuideItem.GuideItemType mCurrentType;

    private int mDiskMountType;

    private int mRemoteDeviceType;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_down_entry, null);

        mGuideDownloadView = view.findViewById(R.id.nodownload_rl);
        mGuidePluginDiskView = view.findViewById(R.id.nodisk_rl);

        mDownloadShadowView = (ImageView) view.findViewById(R.id.nodwon_shadow_iv);
        mDiskShadowView = (ImageView) view.findViewById(R.id.nodisc_shadow_iv);

        this.mBtndownload = (Button) view.findViewById(R.id.btn_down);

        this.mBtndownFail = (Button) view.findViewById(R.id.btn_failed);

        this.mBtndownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAlertDialog();
            }
        });

        this.mBtndownFail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

        mFragmentMgr = getFragmentManager();

        Bundle bundle = getArguments();
        if(bundle != null){
            mRemoteDeviceType = bundle.getInt(Constants.KEY_REMOTE_TYPE, -1);
        }

        switchDownloadEntryView();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mCurrentType = null;
        if(getActivity() instanceof  BindTdActivity){
            ((BindTdActivity) getActivity()).changeBarView();
        }else if(getActivity() instanceof RemoteBindTdActivity){
            RemoteBindTdActivity activity = (RemoteBindTdActivity) getActivity();
            activity.changeBarView();
        }
        mBtndownload.requestFocus();
    }



    private void go2TdWebDownFragment() {
        mTdGuideFragment = new TdGuideFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.KEY_GUIDE_TYPE, TdGuideItem.GuideItemType.WEB_DOWN.ordinal());
        bundle.putInt(Constants.KEY_REMOTE_TYPE,mRemoteDeviceType);
        mTdGuideFragment.setArguments(bundle);
        FragmentTransaction trans = mFragmentMgr.beginTransaction();
        trans.replace(R.id.container_fl, mTdGuideFragment);
        trans.addToBackStack(Constants.KEY_FRAGMENT_WEB_DOWNLOAD);
        trans.commit();
        mCurrentType = TdGuideItem.GuideItemType.WEB_DOWN;
        changeBarView();

        HashMap<String, String> map = new HashMap<String, String>();
        map.put("From", "browser");
        MobclickAgent.onEvent(getActivity(), "download_guide", map);
        AppConfig.LOGD("[[Send download_guide from browser]]");
    }

    private void go2TdMobileDownFragment() {
        mTdGuideFragment = new TdGuideFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.KEY_GUIDE_TYPE, TdGuideItem.GuideItemType.MOBILE_DOWN.ordinal());
        bundle.putInt(Constants.KEY_REMOTE_TYPE,mRemoteDeviceType);
        mTdGuideFragment.setArguments(bundle);
        FragmentTransaction trans = mFragmentMgr.beginTransaction();
        trans.replace(R.id.container_fl, mTdGuideFragment);
        trans.addToBackStack(Constants.KEY_FRAGMENT_MOBILE_DOWNLOAD);
        trans.commit();
        mCurrentType = TdGuideItem.GuideItemType.MOBILE_DOWN;
        changeBarView();

        HashMap<String, String> map = new HashMap<String, String>();
        map.put("From", "mobile");
        MobclickAgent.onEvent(getActivity(), "download_guide", map);
        AppConfig.LOGD("[[Send download_guide from mobile]]");
    }

    public TdGuideItem.GuideItemType getCurrentType() {
        return mCurrentType;
    }

    private void changeBarView() {
        if (getActivity() != null) {
            if (getActivity() instanceof BindTdActivity) {
                BindTdActivity activity = (BindTdActivity) getActivity();
                activity.changeBarView();
            }else if(getActivity() instanceof RemoteBindTdActivity){
                RemoteBindTdActivity activity = (RemoteBindTdActivity) getActivity();
                activity.changeBarView();
            }
        }
    }

    public void switchDownloadEntryView(){
        Bundle bundle = getArguments();
        if(bundle != null){
            mDiskMountType = bundle.getInt(Constants.KEY_FRAGMENT_ENTRY_GUIDE, -1);
            if(mDiskMountType != -1){

                if(mDiskMountType == Constants.KEY_FRAGMENT_ENTRY_DISK){
                    mGuidePluginDiskView.setVisibility(View.VISIBLE);
                    mGuideDownloadView.setVisibility(View.GONE);
                    mDiskShadowView.setVisibility(View.VISIBLE);
                    mDownloadShadowView.setVisibility(View.GONE);
                }

                if(mDiskMountType == Constants.KEY_FRAGMENT_ENTRY_DOWNLOAD){
                    mGuidePluginDiskView.setVisibility(View.GONE);
                    mGuideDownloadView.setVisibility(View.VISIBLE);
                    mDiskShadowView.setVisibility(View.GONE);
                    mDownloadShadowView.setVisibility(View.VISIBLE);

                  boolean popDialog = bundle.getBoolean(Constants.KEY_NEED_POP_DIALOG, false);
                    if (popDialog) {
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                showAlertDialog();
                            }
                        }, 300);
                    }
                }

            }


        }
    }

    public int getDiskMountType() {
        return mDiskMountType;
    }

    private void showAlertDialog(){
        UIHelper.showRemoteAlertDialog(getActivity(), R.drawable.remote_dialog_webbind, R.drawable.remote_dialog_mobilebind,
                getString(R.string.remote_dialog_title_webbind), getString(R.string.remote_dialog_title_mobilebind),
                new UIHelper.RemoteOnclickListener() {
                    @Override
                    public void onclick() {
                        go2TdWebDownFragment();
                    }
                }, new UIHelper.RemoteOnclickListener() {
                    @Override
                    public void onclick() {
                        go2TdMobileDownFragment();
                    }
                }
        );
    }
}