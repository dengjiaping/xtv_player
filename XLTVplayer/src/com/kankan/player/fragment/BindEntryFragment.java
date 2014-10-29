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
import com.kankan.player.activity.BindTdActivity;
import com.kankan.player.app.Constants;
import com.kankan.player.explorer.FileItem;
import com.kankan.player.item.TdGuideItem;
import com.kankan.player.util.UIHelper;
import com.umeng.analytics.MobclickAgent;
import com.xunlei.tv.player.R;

import java.util.HashMap;
import java.util.List;

/**
 * Created by wangyong on 14-5-6.
 */
public class BindEntryFragment extends Fragment implements BindTdActivity.RefreshStatusListener{

    private Button mBtnBind;

    private View mContainerView;

    private TdGuideFragment mTdGuideFragment;

    private FragmentManager mFragmentMgr;

    private TdGuideItem.GuideItemType mCurrentType;

    private boolean hasPop = false;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_bind_entry, null);

        this.mContainerView = view.findViewById(R.id.container_fl);
        this.mBtnBind = (Button) view.findViewById(R.id.btn_bind);
        this.mBtnBind.requestFocus();

        this.mBtnBind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAlertDialog();
            }
        });

        mFragmentMgr = getFragmentManager();

        Bundle bundle = getArguments();
        boolean popDialog = bundle.getBoolean(Constants.KEY_NEED_POP_DIALOG, false);
        if (popDialog && !hasPop) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showAlertDialog();
                    hasPop = true;
                }
            }, 300);
        }

        int status = bundle.getInt(Constants.KEY_REMOTE_STATUS,Constants.KEY_REMOTE_BIND_ERROR);
        if(status == Constants.KEY_REMOTE_BIND_ERROR){
            this.mContainerView.setVisibility(View.INVISIBLE);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mCurrentType = null;
        if(getActivity() instanceof  BindTdActivity){
            ((BindTdActivity) getActivity()).changeBarView();
        }
    }

    private void go2TdWebGuideFragment() {
        mTdGuideFragment = new TdGuideFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.KEY_GUIDE_TYPE, TdGuideItem.GuideItemType.WEB_BIND.ordinal());
        mTdGuideFragment.setArguments(bundle);
        FragmentTransaction trans = mFragmentMgr.beginTransaction();
        trans.replace(R.id.container_fl, mTdGuideFragment);
        trans.addToBackStack(Constants.KEY_FRAGMENT_WEB_BIND);
        trans.commit();
        mCurrentType = TdGuideItem.GuideItemType.WEB_BIND;
        changeBarView();

        HashMap<String, String> map = new HashMap<String, String>();
        map.put("From", "browser");
        MobclickAgent.onEvent(getActivity(), "Bind_choose", map);
    }

    private void go2TdMobileGuideFragment() {
        mTdGuideFragment = new TdGuideFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.KEY_GUIDE_TYPE, TdGuideItem.GuideItemType.MOBILE_BIND.ordinal());
        mTdGuideFragment.setArguments(bundle);
        FragmentTransaction trans = mFragmentMgr.beginTransaction();
        trans.replace(R.id.container_fl, mTdGuideFragment);
        trans.addToBackStack(Constants.KEY_FRAGMENT_MOBILE_BIND);
        trans.commit();
        mCurrentType = TdGuideItem.GuideItemType.MOBILE_BIND;
        changeBarView();

        HashMap<String, String> map = new HashMap<String, String>();
        map.put("From", "mobile");
        MobclickAgent.onEvent(getActivity(), "Bind_choose", map);

    }

    public TdGuideItem.GuideItemType getCurrentType() {
        return mCurrentType;
    }

    private void changeBarView() {
        if (getActivity() != null) {
            if (getActivity() instanceof BindTdActivity) {
                BindTdActivity activity = (BindTdActivity) getActivity();
                activity.changeBarView();
            }
        }
    }

    private void showAlertDialog() {
        if(this.isAdded()){
            UIHelper.showRemoteAlertDialog(getActivity(), R.drawable.remote_dialog_webbind, R.drawable.remote_dialog_mobilebind,
                    getString(R.string.remote_title_webbind), getString(R.string.remote_title_mobilebind), new UIHelper.RemoteOnclickListener() {
                        @Override
                        public void onclick() {
                            go2TdWebGuideFragment();
                        }
                    }, new UIHelper.RemoteOnclickListener() {
                        @Override
                        public void onclick() {
                            go2TdMobileGuideFragment();
                        }
                    }
            );
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void refreshStatus(List<FileItem> list) {

    }

    @Override
    public void refreshStatus() {
        if(mTdGuideFragment != null && mTdGuideFragment.isAdded()){
            mTdGuideFragment.updateCode();
        }
    }
}