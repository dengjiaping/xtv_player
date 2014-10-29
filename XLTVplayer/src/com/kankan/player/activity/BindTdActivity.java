package com.kankan.player.activity;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.kankan.player.api.tddownload.SysInfo;
import com.kankan.player.app.AppConfig;
import com.kankan.player.app.Constants;
import com.kankan.player.explorer.FileItem;
import com.kankan.player.fragment.BindEntryFragment;
import com.kankan.player.fragment.DownlistFragment;
import com.kankan.player.fragment.DownloadEntryFragment;
import com.kankan.player.item.DeviceItem;
import com.kankan.player.item.TdGuideItem;
import com.kankan.player.manager.LocalTDDownloadManager;
import com.kankan.player.manager.UsbManager;
import com.kankan.player.service.TdScanService;
import com.kankan.player.util.TipsDialog;
import com.kankan.player.util.UIHelper;
import com.umeng.analytics.MobclickAgent;
import com.xunlei.tv.player.R;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 * Created by wangyong on 14-5-6.
 */
public class BindTdActivity extends BaseActivity {

    public static final int EXTRA_REQUST_CODE = 0x123;

    public static final int EXTRA_GO_TO_BIND = 0x1001;
    public static final int EXTRA_UNBIND_SUCESS = 0x1002;
    public static final int EXTRA_GO_TO_GUIDE_DOWNLOAD = 0x1003;

    private FragmentManager mFragmentMgr;

    private BindEntryFragment mBindEntryFragment;
    private DownloadEntryFragment mDownloadEntryFragment;
    private DownlistFragment mDownloadListFragment;

    private static int mCurrentStatus = -1;

    private List<FileItem> mTdDownloadList;

    private TDReceiver mTdReceiver;

    private LocalTDDownloadManager mTdDownloadMgr;

    private Fragment mCurrentFragment;

    private TextView mPageTitleTv;
    private ImageView mThunderIconIv;
    private TextView mUsrNameTv;
    private ImageView mMenuIconIv;
    private TextView mMenuTv;

    private String mUserName;
    private String mTitle;

    public class TDReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)) {

                if (mCurrentFragment == mDownloadEntryFragment) {

                    if (mDownloadEntryFragment.getDiskMountType() == Constants.KEY_FRAGMENT_ENTRY_DISK) {

                        mDownloadEntryFragment.switchDownloadEntryView();
                    }

                }

            }

            if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)
                    || intent.getAction().equals(Intent.ACTION_MEDIA_BAD_REMOVAL)) {

                if (mCurrentFragment == mDownloadEntryFragment) {

                    if (mFragmentMgr != null) {
                        mFragmentMgr.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    }
                    turn2DownloadEntry(false);
                }

            }


            if (intent.getAction().equals(TdScanService.ACTION_SEND_QUERY_LOCAL_RESULT)) {

                if(mTdDownloadMgr.getSysInfo() == null){
                    return;
                }

                final int bind = mTdDownloadMgr.getSysInfo().isBindOk;
                final int userId = mTdDownloadMgr.getSysInfo().userId;

                mTdDownloadList = mTdDownloadMgr.getFileItems();
                if(mTdDownloadList != null){
                    AppConfig.LOGD("tddownload get size is:"+ mTdDownloadList.size());
                }

                if (!TextUtils.isEmpty(mTdDownloadMgr.getSysInfo().bindAcktiveKey)) {
                    TipsDialog.getInstance().dismiss();
                }

                if (bind == mCurrentStatus) {
                    //绑定状态没有改变
                    if ((mTdDownloadList != null && mTdDownloadList.size() > 0) || mTdDownloadMgr.getDownloadingFilesNum() > 0) {

                        if (mCurrentFragment != null) {

                            if (mCurrentFragment == mDownloadListFragment) {

                                mDownloadListFragment.refreshTDDownloadlist(mTdDownloadList);
                                mDownloadListFragment.changeBindstatusView();


                            }

                            if (mCurrentFragment == mDownloadEntryFragment) {

                                if (mFragmentMgr != null) {
                                    mFragmentMgr.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                                }
                                turn2Downlist();
                            }

                        }
                    } else {


                        if (mCurrentStatus == Constants.KEY_REMOTE_BIND_SUCESS) {
                            if (mCurrentFragment != mDownloadEntryFragment) {
                                if (mFragmentMgr != null) {
                                    mFragmentMgr.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                                }
                                turn2DownloadEntry(false);
                            }

                        }

                        if (mCurrentStatus == Constants.KEY_REMOTE_BIND_FAILED) {

                            if (mCurrentFragment != mBindEntryFragment) {
                                if (mFragmentMgr != null) {
                                    mFragmentMgr.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                                }
                                AppConfig.LOGD("bindstatus in receiver call turn2bindentry");

                                turn2BindEntry(false, false);
                            }else{
                                if(mListener != null){
                                    mListener.refreshStatus();
                                }
                            }

                        }
                    }

                    SysInfo infos = mTdDownloadMgr.getSysInfo();
                    if (infos != null) {

                        if (mUserName != null) {
                            if (!mUserName.equals(infos.userName)) {
                                changeBarView();
                            }
                        }

                    }


                } else {


                    if (bind == Constants.KEY_REMOTE_BIND_SUCESS) {
                        //绑定成功

                        AppConfig.LOGD("bindstatus changed to binded");

                        SysInfo infos = mTdDownloadMgr.getSysInfo();
                        if (infos != null) {
                            mUserName = infos.userName;
                            if (mUserName != null) {

                                if(mCurrentStatus == Constants.KEY_REMOTE_BIND_ERROR){

                                    dealWithturn2BindSucess(bind,userId);
                                }else {
                                    UIHelper.showRemoteAlertTips(BindTdActivity.this, mUserName, new UIHelper.RemoteOnclickListener() {
                                        @Override
                                        public void onclick() {
                                            dealWithturn2BindSucess(bind,userId);
                                        }
                                    });
                                }

                            }

                        }
                    }else if (bind == Constants.KEY_REMOTE_BIND_FAILED) {
                        //未绑定
                        AppConfig.LOGD("bindstatus changed to unbind");
                        //绑定状态发生改变
                        mCurrentStatus = bind;
                        HashMap<String, String> map = new HashMap<String, String>();
                        map.put("UsrId",userId+"");
                        MobclickAgent.onEvent(BindTdActivity.this, "Remote_unbind",map);

                        if (mTdDownloadList != null && mTdDownloadList.size() > 0) {

                            if (mCurrentFragment != null) {

                                if (mCurrentFragment instanceof DownlistFragment) {

                                    ((DownlistFragment)mCurrentFragment).changeBindstatusView();

                                } else {

                                    turn2Downlist();
                                }

                            }
                        } else {

                            turn2BindEntry(false, false);
                        }
                    }

                }

                //改变actionbar状态
                changeBarView();

            }
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_bind_td);


        initUI();

        initData();
    }

    @Override
    protected String getUmengPageName() {
        return "BindTdActivity";
    }

    private void initUI() {

        View topbar = findViewById(R.id.top_bar);
        this.mPageTitleTv = (TextView) topbar.findViewById(R.id.title);
        this.mThunderIconIv = (ImageView) topbar.findViewById(R.id.xunlei_iv);
        this.mUsrNameTv = (TextView) topbar.findViewById(R.id.status_tv);
        this.mMenuIconIv = (ImageView) topbar.findViewById(R.id.menu_iv);
        this.mMenuTv = (TextView) topbar.findViewById(R.id.menu_tv);
    }

    private void initData() {

        mFragmentMgr = getFragmentManager();

        mTdReceiver = new TDReceiver();

        this.mTdDownloadMgr = LocalTDDownloadManager.getInstance();

        SysInfo infos = LocalTDDownloadManager.getInstance().getSysInfo();
        if (infos != null) {
            mCurrentStatus = infos.isBindOk;
        }

        mTdDownloadList = LocalTDDownloadManager.getInstance().getFileItems();

        if (mCurrentStatus == Constants.KEY_REMOTE_BIND_SUCESS) {

            if (mTdDownloadList != null && mTdDownloadList.size() > 0) {
                turn2Downlist();
            } else {
                turn2DownloadEntry(false);
            }
        }

        if (mCurrentStatus == Constants.KEY_REMOTE_BIND_FAILED) {

            if (mTdDownloadList != null && mTdDownloadList.size() > 0) {
                turn2Downlist();
            } else {
                AppConfig.LOGD("bindstatus in initData call turn2bindentry");
                turn2BindEntry(false, false);
            }
        }

        mTitle = getString(R.string.remote_title_mydownload);
        Intent intent = getIntent();
        if(intent != null){
            if(!TextUtils.isEmpty(intent.getStringExtra(Constants.KEY_PAGE_TTILE))){
                mTitle = intent.getStringExtra(Constants.KEY_PAGE_TTILE);
            }
        }

    }


    public void turn2BindEntry(boolean popAlert, boolean saveStatus) {
        mBindEntryFragment = new BindEntryFragment();
        Bundle bundle = new Bundle();
        if (popAlert) {
            bundle.putBoolean(Constants.KEY_NEED_POP_DIALOG, true);
        } else {
            bundle.putBoolean(Constants.KEY_NEED_POP_DIALOG, false);
        }
        bundle.putInt(Constants.KEY_REMOTE_STATUS,mCurrentStatus);
        mBindEntryFragment.setArguments(bundle);
        setRefreshListener(mBindEntryFragment);
        FragmentTransaction trans = mFragmentMgr.beginTransaction();
        trans.replace(R.id.container_fl, mBindEntryFragment);

        if (saveStatus) {
            trans.addToBackStack(Constants.KEY_FRAGMENT_BIND_ENTRY);
        }

        trans.commit();
        mCurrentFragment = mBindEntryFragment;
        changeBarView();
    }

    public void turn2DownloadEntry(boolean popAlert) {
        mDownloadEntryFragment = new DownloadEntryFragment();
        Bundle bundle = new Bundle();
        List<DeviceItem> list = UsbManager.getUsbDeviceList();
        if (list != null && list.size() > 0) {
            bundle.putInt(Constants.KEY_FRAGMENT_ENTRY_GUIDE, Constants.KEY_FRAGMENT_ENTRY_DOWNLOAD);
        } else {
            bundle.putInt(Constants.KEY_FRAGMENT_ENTRY_GUIDE, Constants.KEY_FRAGMENT_ENTRY_DISK);
        }
        if (popAlert) {
            bundle.putBoolean(Constants.KEY_NEED_POP_DIALOG, true);
        } else {
            bundle.putBoolean(Constants.KEY_NEED_POP_DIALOG, false);
        }
        bundle.putInt(Constants.KEY_REMOTE_TYPE, Constants.KEY_REMOTE_LOCAL);
        mDownloadEntryFragment.setArguments(bundle);
        FragmentTransaction trans = mFragmentMgr.beginTransaction();
        trans.replace(R.id.container_fl, mDownloadEntryFragment);
        trans.commit();
        mCurrentFragment = mDownloadEntryFragment;
        changeBarView();

    }

    public void turn2Downlist() {
        mDownloadListFragment = new DownlistFragment();
        Bundle bundle = new Bundle();
        if (mTdDownloadList != null) {
            bundle.putSerializable(Constants.KEY_TDDOWNLOAD_LIST, (Serializable) mTdDownloadList);
        }
        mDownloadListFragment.setArguments(bundle);
        FragmentTransaction trans = mFragmentMgr.beginTransaction();
        trans.replace(R.id.container_fl, mDownloadListFragment);
        trans.commit();
        mCurrentFragment = mDownloadListFragment;
        changeBarView();

    }

    public void changeBarView() {

        SysInfo infos = mTdDownloadMgr.getSysInfo();
        if (infos != null) {
            mUserName = infos.userName;
        }

        if (mCurrentStatus == Constants.KEY_REMOTE_BIND_SUCESS) {

            if (mCurrentFragment == mDownloadEntryFragment) {
                TdGuideItem.GuideItemType type = mDownloadEntryFragment.getCurrentType();

                if (type == TdGuideItem.GuideItemType.WEB_DOWN) {
                    this.mPageTitleTv.setText(getString(R.string.remote_title_guidedownload));
                    this.mThunderIconIv.setImageResource(R.drawable.bar_thunder_bind);
                    this.mThunderIconIv.setVisibility(View.VISIBLE);
                    this.mUsrNameTv.setTextColor(getResources().getColor(R.color.bar_bind_color));
                    this.mUsrNameTv.setVisibility(View.VISIBLE);
                    if (TextUtils.isEmpty(mUserName)) {
                        this.mUsrNameTv.setText(getString(R.string.remote_usrname_null));
                    } else {
                        this.mUsrNameTv.setText(mUserName);
                    }
                    this.mMenuIconIv.setImageResource(R.drawable.bar_left_right);
                    this.mMenuIconIv.setVisibility(View.VISIBLE);
                    this.mMenuTv.setText(getString(R.string.remote_bar_left_right_tv));
                    this.mMenuTv.setVisibility(View.VISIBLE);
                }

                if (type == TdGuideItem.GuideItemType.MOBILE_DOWN) {
                    this.mPageTitleTv.setText(getString(R.string.remote_title_guidedownload));
                    this.mThunderIconIv.setImageResource(R.drawable.bar_thunder_bind);
                    this.mThunderIconIv.setVisibility(View.VISIBLE);
                    this.mUsrNameTv.setVisibility(View.VISIBLE);
                    if (TextUtils.isEmpty(mUserName)) {
                        this.mUsrNameTv.setText(getString(R.string.remote_usrname_null));
                    } else {
                        this.mUsrNameTv.setText(mUserName);
                    }
                    this.mUsrNameTv.setTextColor(getResources().getColor(R.color.bar_bind_color));
                    this.mMenuIconIv.setImageResource(R.drawable.bar_left_right);
                    this.mMenuIconIv.setVisibility(View.VISIBLE);
                    this.mMenuTv.setText(getString(R.string.remote_bar_left_right_tv));
                    this.mMenuTv.setVisibility(View.VISIBLE);
                }

                if (type == TdGuideItem.GuideItemType.OTHER || type == null) {
                    this.mPageTitleTv.setText(mTitle);
                    this.mThunderIconIv.setImageResource(R.drawable.bar_thunder_bind);
                    this.mThunderIconIv.setVisibility(View.VISIBLE);
                    this.mUsrNameTv.setVisibility(View.VISIBLE);
                    if (TextUtils.isEmpty(mUserName)) {
                        this.mUsrNameTv.setText(getString(R.string.remote_usrname_null));
                    } else {
                        this.mUsrNameTv.setText(mUserName);
                    }
                    this.mUsrNameTv.setTextColor(getResources().getColor(R.color.bar_bind_color));
                    this.mMenuIconIv.setImageResource(R.drawable.actionbar_menu_icon);
                    this.mMenuIconIv.setVisibility(View.VISIBLE);
                    this.mMenuTv.setText(getString(R.string.remote_bar_menu_tv));
                    this.mMenuTv.setVisibility(View.VISIBLE);
                }
            }

            if (mCurrentFragment instanceof DownlistFragment) {
                if (((DownlistFragment)mCurrentFragment).isSubList()) {
                    if (!TextUtils.isEmpty(mDownloadListFragment.getmFileName())) {
                        this.mPageTitleTv.setText(mDownloadListFragment.getmFileName());
                    }
                } else {
                    this.mPageTitleTv.setText(mTitle);
                }
                this.mThunderIconIv.setImageResource(R.drawable.bar_thunder_bind);
                this.mThunderIconIv.setVisibility(View.VISIBLE);
                if(TextUtils.isEmpty(mUserName)){
                    this.mUsrNameTv.setText(getString(R.string.remote_usrname_null));
                }else{
                    this.mUsrNameTv.setText(mUserName);
                }
                this.mUsrNameTv.setTextColor(getResources().getColor(R.color.bar_bind_color));
                this.mUsrNameTv.setVisibility(View.VISIBLE);
                this.mMenuIconIv.setImageResource(R.drawable.actionbar_menu_icon);
                this.mMenuIconIv.setVisibility(View.VISIBLE);
                this.mMenuTv.setText(getString(R.string.remote_bar_menu_tv));
                this.mMenuTv.setVisibility(View.VISIBLE);
            }

        }

        if (mCurrentStatus == Constants.KEY_REMOTE_BIND_FAILED) {

            if (mCurrentFragment == mBindEntryFragment) {
                TdGuideItem.GuideItemType type = mBindEntryFragment.getCurrentType();

                if (type == TdGuideItem.GuideItemType.WEB_BIND) {
                    this.mPageTitleTv.setText(getString(R.string.remote_title_webbind));
                    this.mThunderIconIv.setImageResource(R.drawable.actionbar_thunder_unbind);
                    this.mThunderIconIv.setVisibility(View.VISIBLE);
                    this.mUsrNameTv.setText(getString(R.string.remote_bar_unbind_tv));
                    this.mUsrNameTv.setTextColor(getResources().getColor(R.color.bar_unbind_color));
                    this.mUsrNameTv.setVisibility(View.VISIBLE);
                    this.mMenuIconIv.setImageResource(R.drawable.bar_left_right);
                    this.mMenuIconIv.setVisibility(View.VISIBLE);
                    this.mMenuTv.setText(getString(R.string.remote_bar_left_right_tv));
                    this.mMenuTv.setVisibility(View.VISIBLE);
                }

                if (type == TdGuideItem.GuideItemType.MOBILE_BIND) {
                    this.mPageTitleTv.setText(getString(R.string.remote_title_mobilebind));
                    this.mThunderIconIv.setImageResource(R.drawable.actionbar_thunder_unbind);
                    this.mThunderIconIv.setVisibility(View.VISIBLE);
                    this.mUsrNameTv.setText(getString(R.string.remote_bar_unbind_tv));
                    this.mUsrNameTv.setTextColor(getResources().getColor(R.color.bar_unbind_color));
                    this.mUsrNameTv.setVisibility(View.VISIBLE);
                    this.mMenuIconIv.setImageResource(R.drawable.bar_left_right);
                    this.mMenuIconIv.setVisibility(View.VISIBLE);
                    this.mMenuTv.setText(getString(R.string.remote_bar_left_right_tv));
                    this.mMenuTv.setVisibility(View.VISIBLE);
                }

                if (type == TdGuideItem.GuideItemType.OTHER || type == null) {
                    this.mPageTitleTv.setText(mTitle);
                    this.mThunderIconIv.setImageResource(R.drawable.actionbar_thunder_unbind);
                    this.mThunderIconIv.setVisibility(View.VISIBLE);
                    this.mUsrNameTv.setText(getString(R.string.remote_bar_unbind_tv));
                    this.mUsrNameTv.setTextColor(getResources().getColor(R.color.bar_unbind_color));
                    this.mMenuIconIv.setVisibility(View.GONE);
                    this.mMenuTv.setVisibility(View.GONE);
                }

            }

            if(mCurrentFragment instanceof DownlistFragment){
                if (((DownlistFragment)mCurrentFragment).isSubList()) {
                    if (!TextUtils.isEmpty(mDownloadListFragment.getmFileName())) {
                        this.mPageTitleTv.setText(mDownloadListFragment.getmFileName());
                    }
                } else {
                    this.mPageTitleTv.setText(mTitle);
                }
                this.mThunderIconIv.setImageResource(R.drawable.actionbar_thunder_unbind);
                this.mThunderIconIv.setVisibility(View.VISIBLE);
                this.mUsrNameTv.setText(getString(R.string.remote_bar_unbind_tv));
                this.mUsrNameTv.setVisibility(View.VISIBLE);
                this.mUsrNameTv.setTextColor(getResources().getColor(R.color.bar_unbind_color));
                this.mMenuIconIv.setImageResource(R.drawable.actionbar_menu_icon);
                this.mMenuIconIv.setVisibility(View.VISIBLE);
                this.mMenuTv.setText(getString(R.string.remote_bar_menu_tv));
                this.mMenuTv.setVisibility(View.VISIBLE);
            }

        }

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        filter.addAction(TdScanService.ACTION_SEND_QUERY_LOCAL_RESULT);
        registerReceiver(mTdReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mTdReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TipsDialog.getInstance().dismiss();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {

            //绑定界面不弹出绑定menu
            if (mCurrentFragment != null) {
                if (mCurrentFragment == mBindEntryFragment) {
                    return super.onKeyDown(keyCode, event);
                }

                if (mCurrentFragment instanceof DownlistFragment) {
                    Intent intent = new Intent(this, TDRemoteMenuActivity.class);
                    intent.putExtra(Constants.KEY_SHOW_GUIDE_DOWNLOAD, true);
                    intent.putExtra(Constants.KEY_SHOW_BIND, true);
                    intent.putExtra(Constants.KEY_REMOTE_TYPE, Constants.KEY_REMOTE_LOCAL);
                    startActivityForResult(intent, EXTRA_REQUST_CODE);
                    return true;
                }
            }

            Intent intent = new Intent(this, TDRemoteMenuActivity.class);
            intent.putExtra(Constants.KEY_SHOW_BIND, true);
            startActivityForResult(intent, EXTRA_REQUST_CODE);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EXTRA_REQUST_CODE && resultCode == RESULT_OK) {

            if (data != null) {

                int what = data.getIntExtra(Constants.KEY_WHAT, -1);

                if (what == EXTRA_GO_TO_BIND) {
                    AppConfig.LOGD("bindstatus in onactivityresult call turn2bindentry");
                    turn2BindEntry(true, true);
                }

                if (what == EXTRA_UNBIND_SUCESS) {

                    TipsDialog.getInstance().showProcess(this, R.drawable.tip_loading, "正在解除绑定", true);
                }

                if (what == EXTRA_GO_TO_GUIDE_DOWNLOAD) {
                    turn2DownloadEntry(false);
                }
            }
        }

    }

    public void setBarTitle(String name) {
        if (!TextUtils.isEmpty(name)) {
            this.mPageTitleTv.setText(name);
        }
    }

    public void setCurrentFragment(Fragment fragment) {
        mCurrentFragment = fragment;
    }



    private void dealWithturn2BindSucess(int bind, int usrId){
        if (mTdDownloadList != null && mTdDownloadList.size() > 0) {

            if (mCurrentFragment != null) {

                if (mCurrentFragment instanceof DownlistFragment) {

                    ((DownlistFragment)mCurrentFragment).changeBindstatusView();

                } else {
                    if (mCurrentFragment == mBindEntryFragment) {

                        if (mBindEntryFragment.getCurrentType() == TdGuideItem.GuideItemType.WEB_BIND) {
                            HashMap<String, String> map = new HashMap<String, String>();
                            map.put("From", "webguide");
                            MobclickAgent.onEvent(BindTdActivity.this, "Remote_guidebind", map);
                        }

                        if (mBindEntryFragment.getCurrentType() == TdGuideItem.GuideItemType.MOBILE_BIND) {
                            HashMap<String, String> map = new HashMap<String, String>();
                            map.put("From", "tvAssistant");
                            MobclickAgent.onEvent(BindTdActivity.this, "Remote_guidebind", map);
                        }
                    }

                    if (mFragmentMgr != null) {
                        mFragmentMgr.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    }
                    turn2Downlist();
                }


            }

        } else {

            if (mCurrentFragment == mBindEntryFragment) {

                if (mBindEntryFragment.getCurrentType() == TdGuideItem.GuideItemType.WEB_BIND) {
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("From", "webguide");
                    map.put("UsrId",usrId+"");
                    MobclickAgent.onEvent(BindTdActivity.this, "Remote_guidebind", map);
                }

                if (mBindEntryFragment.getCurrentType() == TdGuideItem.GuideItemType.MOBILE_BIND) {
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("From", "tvAssistant");
                    map.put("UsrId",usrId+"");
                    MobclickAgent.onEvent(BindTdActivity.this, "Remote_guidebind", map);
                }
            }
            if (mFragmentMgr != null) {
                mFragmentMgr.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
            turn2DownloadEntry(false);
        }

        //绑定状态发生改变
        mCurrentStatus = bind;

    }

    public interface RefreshStatusListener {

        void refreshStatus(List<FileItem> list);

        void refreshStatus();
    }

    private RefreshStatusListener mListener;

    private void setRefreshListener(RefreshStatusListener listener) {
        this.mListener = listener;
    }


}