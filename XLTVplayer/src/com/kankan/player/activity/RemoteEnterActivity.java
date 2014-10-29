package com.kankan.player.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.*;
import android.view.animation.AnimationUtils;
import android.widget.*;
import com.kankan.player.app.AppConfig;
import com.kankan.player.app.Constants;
import com.kankan.player.event.DeviceEvent;
import com.kankan.player.explorer.FileItem;
import com.kankan.player.item.DeviceItem;
import com.kankan.player.manager.LocalTDDownloadManager;
import com.kankan.player.manager.XLRouterDownloadMgr;
import com.kankan.player.service.TdScanService;
import com.kankan.player.util.DeviceModelUtil;
import com.kankan.player.util.SettingManager;
import com.kankan.player.util.SmbUtil;
import com.kankan.player.view.AnimateLinearLayout;
import com.kankan.player.view.MarqueenTextView;
import com.xunlei.tv.player.R;
import de.greenrobot.event.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangyong on 14-5-13.
 */
public class RemoteEnterActivity extends Activity {

    private static final byte[] mLockObject = new byte[]{};


    private LayoutInflater mLayoutInflater;
    private TextView mDeviceNumTv;
    private LinearLayout mContainer;
    private HorizontalScrollView mScrollView;
    private ProgressBar mLoadingProgress;
    private View mShadowView;

    private List<DeviceItem> mDeviceList = new ArrayList<DeviceItem>();
    private List<View> mViewItems = new ArrayList<View>();


    private int mMarginLeft;
    private int mFirstMarginLeft;
    private int mMarginRight;
    private int mLastMarginRight;

    private LocalTDDownloadManager mLocalTdDownloadMgr;
    private XLRouterDownloadMgr mRouterDownloadMgr;

    private boolean mCurrentSupportLocalTd;
    private boolean mCurrentSupportRouterTd;

    private TDReceiver mTdReceiver;

    class TDReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(TdScanService.ACTION_SEND_QUERY_LOCAL_RESULT)
                    || intent.getAction().equals(TdScanService.ACTION_SEND_QUERY_ROUTER_RESULT)) {

                boolean supportLocalTd = DeviceModelUtil.isSupportReleaseService() || DeviceModelUtil.isSupportBox();
                boolean supportRouterTd = XLRouterDownloadMgr.getInstance().isSupportTD();

                List<DeviceItem> list = new ArrayList<DeviceItem>();
                List<DeviceItem.DeviceType> types = new ArrayList<DeviceItem.DeviceType>();

                if (supportLocalTd != mCurrentSupportLocalTd || supportRouterTd != mCurrentSupportRouterTd) {
                    if (supportLocalTd && supportRouterTd) {
                        list.add(getRouterDeviceItem());
                        types.add(DeviceItem.DeviceType.XL_ROUTER_TDDOWNLOAD);
                        list.add(getLocalRemoteDeviceItem());
                        types.add(DeviceItem.DeviceType.TD_DOWNLOAD);

                        handlerViewItem(list, types);
                    }

                    if (supportLocalTd && !supportRouterTd) {
                        list.add(getLocalRemoteDeviceItem());
                        types.add(DeviceItem.DeviceType.TD_DOWNLOAD);

                        handlerViewItem(list, types);
                    }

                    if (!supportLocalTd && supportRouterTd) {
                        list.add(getRouterDeviceItem());
                        types.add(DeviceItem.DeviceType.XL_ROUTER_TDDOWNLOAD);
                        handlerViewItem(list, types);

                    }

                    if (!supportLocalTd && !supportRouterTd) {
                        handlerViewItem(list, types);
                    }

                    mCurrentSupportLocalTd = supportLocalTd;
                    mCurrentSupportRouterTd = supportRouterTd;

                    AppConfig.LOGD("changed  refresh UI---------------------");

                }

                refreshTDownloadCount();

            }
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_entry);

        EventBus.getDefault().register(this);

        initUI();

        initData();

    }

    private void initUI() {
        mLayoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //findViewById(R.id.home_up_iv).setVisibility(View.GONE);
        View bar = findViewById(R.id.top_bar);
        TextView titleTv = (TextView) bar.findViewById(R.id.title);
        titleTv.setText(getString(R.string.remote_title_mydownload));

        mDeviceNumTv = (TextView) findViewById(R.id.device_num);
        mContainer = (LinearLayout) findViewById(R.id.container_ll);
        mScrollView = (HorizontalScrollView) findViewById(R.id.hsv);
        mLoadingProgress = (ProgressBar) findViewById(R.id.loading_pb);
        mLoadingProgress.setVisibility(View.INVISIBLE);
        mShadowView = findViewById(R.id.shadow);

        mMarginLeft = getResources().getDimensionPixelSize(R.dimen.device_margin_left);
        mMarginRight = getResources().getDimensionPixelSize(R.dimen.device_margin_right);
        mFirstMarginLeft = getResources().getDimensionPixelSize(R.dimen.device_first_margin_left);
        mLastMarginRight = getResources().getDimensionPixelSize(R.dimen.device_last_margin_right);

    }

    private void initData() {

        mLocalTdDownloadMgr = LocalTDDownloadManager.getInstance();
        mRouterDownloadMgr = XLRouterDownloadMgr.getInstance();

        List<DeviceItem> list = new ArrayList<DeviceItem>();
        List<DeviceItem.DeviceType> types = new ArrayList<DeviceItem.DeviceType>();

        if (DeviceModelUtil.isSupportReleaseService() || DeviceModelUtil.isSupportBox()) {
            mCurrentSupportLocalTd = true;
            list.add(getLocalRemoteDeviceItem());
            types.add(DeviceItem.DeviceType.TD_DOWNLOAD);
        }

        if (mRouterDownloadMgr.isSupportTD()) {
            mCurrentSupportRouterTd = true;
            list.add(getRouterDeviceItem());
            types.add(DeviceItem.DeviceType.XL_ROUTER_TDDOWNLOAD);
        }

        mTdReceiver = new TDReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TdScanService.ACTION_SEND_QUERY_LOCAL_RESULT);
        intentFilter.addAction(TdScanService.ACTION_SEND_QUERY_ROUTER_RESULT);
        intentFilter.addAction(TdScanService.ACTION_INSTALL_SUCESS);
        registerReceiver(mTdReceiver, intentFilter);

        handlerViewItem(list, types);

        refreshTDownloadCount();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //暂时加到这里，保证数目刷新及时
        refreshTDownloadCount();
    }

    private void handlerViewItem(List<DeviceItem> items, List<DeviceItem.DeviceType> types) {
        //fix 移动设备拔出，异常退出问题
        List<DeviceItem> removeItemList = new ArrayList<DeviceItem>();
        for (DeviceItem item : mDeviceList) {
            if (!items.contains(item) && types.contains(item.getType())) {
                removeItemList.add(item);
            }
        }

        for (DeviceItem item : removeItemList) {
            removeItem(item);
        }

        removeItemList.clear();

        for (DeviceItem item : items) {
            if (!mDeviceList.contains(item) && types.contains(item.getType())) {
                addItem(item);
            }
        }
    }

    private void removeItem(DeviceItem item) {
        if (item == null || !mDeviceList.contains(item)) {
            return;
        }

        int index = mDeviceList.indexOf(item);
        synchronized (mLockObject) {
            mDeviceList.remove(item);
            mViewItems.remove(index);
        }

        //不要删除这一行，不然会报很奇怪的nullpointer
        mContainer.requestFocus();
        mContainer.removeViewAt(index);
        if (index == 0) {
            ((LinearLayout.LayoutParams) mViewItems.get(0).getLayoutParams()).leftMargin = mFirstMarginLeft;
        }
        if (index == mDeviceList.size()) {
            ((LinearLayout.LayoutParams) mViewItems.get(0).getLayoutParams()).rightMargin = mLastMarginRight;
        }

        mDeviceNumTv.setText(String.valueOf(mDeviceList.size()));
    }

    private void addItem(DeviceItem item) {
        int index = getInsertIndexOfItem(item);
        if (index == -1) {
            return;
        }

        if (mDeviceList.size() > 0) {
            if (index == 0) {
                ((LinearLayout.LayoutParams) mViewItems.get(0).getLayoutParams()).leftMargin = mMarginLeft;
            } else if (index == mDeviceList.size()) {
                ((LinearLayout.LayoutParams) mViewItems.get(index - 1).getLayoutParams()).rightMargin = mMarginRight;
            }
        }

        View view = mLayoutInflater.inflate(R.layout.item_device_list, null);
        view.setFocusable(true);
        final View v = view.findViewById(R.id.device_rl);
        ImageView coverIv = (ImageView) view.findViewById(R.id.cover);
        TextView nameTv = (TextView) view.findViewById(R.id.name);
        MarqueenTextView descTv = (MarqueenTextView) view.findViewById(R.id.desc);
        TextView countTv = (TextView) view.findViewById(R.id.count_tv);
        ImageView previewIv = (ImageView) view.findViewById(R.id.preview_iv);
        ImageView previewIvIcon = (ImageView) view.findViewById(R.id.preview_iv_1);
        ImageView thumbnailIv = (ImageView) view.findViewById(R.id.thumbnail_iv);
        //final ImageView reflectIv = (ImageView) view.findViewById(R.id.reflect_iv);
        View loadingCoverView = view.findViewById(R.id.loading_rl);


        coverIv.setImageResource(getCoverResId(item.getType()));
        previewIvIcon.setImageResource(getIconResId(item.getType()));
        nameTv.setText(item.getName());
        descTv.setText(item.getDescription());

        if (item.getType() == DeviceItem.DeviceType.TD_DOWNLOAD) {
            // 根据情况显示新增文件数，刚加载时得不到数组，暂不显示.
            countTv.setVisibility(View.INVISIBLE);
            previewIvIcon.setVisibility(View.VISIBLE);

            if(!LocalTDDownloadManager.getInstance().isSupportTD()){
                loadingCoverView.setVisibility(View.VISIBLE);
            }else{
                loadingCoverView.setVisibility(View.GONE);
            }
        } else {
            previewIvIcon.setVisibility(View.VISIBLE);
        }

        if (item.getType() == DeviceItem.DeviceType.XL_ROUTER_TDDOWNLOAD) {
            // 根据情况显示新增文件数，刚加载时得不到数组，暂不显示.
            countTv.setVisibility(View.INVISIBLE);
            previewIvIcon.setVisibility(View.VISIBLE);
        } else {
            previewIvIcon.setVisibility(View.VISIBLE);
        }

        view.setTag(item);
        view.setOnFocusChangeListener(mOnFocusChangeListener);
        view.setOnClickListener(mOnClickListener);

        int marginLeft = mMarginLeft;
        int marginRight = mMarginRight;
        if (index == 0) {
            marginLeft = mFirstMarginLeft;
        }
        if (index == mDeviceList.size()) {
            marginRight = mLastMarginRight;
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = marginLeft;
        lp.rightMargin = marginRight;
        lp.topMargin = (int) getResources().getDimension(R.dimen.device_item_inner_top_margin);
        mContainer.addView(view, index, lp);

        synchronized (mLockObject) {
            mDeviceList.add(index, item);
            mViewItems.add(index, view);
        }

        checkShadow();

        mDeviceNumTv.setText(String.valueOf(mDeviceList.size()));
    }

    private void checkShadow() {
        mShadowView.setVisibility(mScrollView.getWidth() > getResources().getDisplayMetrics().widthPixels ? View.VISIBLE : View.GONE);
    }

    private int getInsertIndexOfItem(DeviceItem item) {
        if (item == null || mDeviceList.contains(item)) {
            return -1;
        }

        int index = 0;
        if(mDeviceList.size()>0){
            if(item.getType() == DeviceItem.DeviceType.XL_ROUTER_TDDOWNLOAD){
                index = 0;
            }
            if(item.getType() == DeviceItem.DeviceType.TD_DOWNLOAD){
                return mDeviceList.size();
            }
        }

        return index;
    }

    private int getCoverResId(DeviceItem.DeviceType type) {
        if (type == DeviceItem.DeviceType.USB) {
            return R.drawable.usb_bg;
        } else if (type == DeviceItem.DeviceType.HHD) {
            return R.drawable.disk_bg;
        } else if (type == DeviceItem.DeviceType.TD_DOWNLOAD) {
            return R.drawable.box_bg;
        } else if (type == DeviceItem.DeviceType.VIDEO_LIST) {
            return R.drawable.cover_video;
        } else if (type == DeviceItem.DeviceType.HISTORY) {
            return R.drawable.local_bg;
        } else if (type == DeviceItem.DeviceType.EXTERNAL) {
            return R.drawable.history_bg;
        } else if (type == DeviceItem.DeviceType.XL_ROUTER) {
            return R.drawable.history_bg;
        } else if (type == DeviceItem.DeviceType.XL_ROUTER_TDDOWNLOAD) {
            return R.drawable.router_bg;
        }
        return -1;
    }

    private int getIconResId(DeviceItem.DeviceType type) {
        if (type == DeviceItem.DeviceType.USB) {
            return R.drawable.cover_icon_usb;
        } else if (type == DeviceItem.DeviceType.HHD) {
            return R.drawable.cover_icon_disk;
        } else if (type == DeviceItem.DeviceType.TD_DOWNLOAD) {
            return getBoxIcon();
        } else if (type == DeviceItem.DeviceType.VIDEO_LIST) {
            return R.drawable.cover_video;
        } else if (type == DeviceItem.DeviceType.HISTORY) {
            return R.drawable.cover_icon_history;
        } else if (type == DeviceItem.DeviceType.EXTERNAL) {
            return R.drawable.cover_icon_local;
        } else if (type == DeviceItem.DeviceType.XL_ROUTER) {
            return R.drawable.cover_icon_local;
        } else if (type == DeviceItem.DeviceType.XL_ROUTER_TDDOWNLOAD) {
            String routerName = SettingManager.getInstance().getRouterName();
            if (SmbUtil.ROUTER_XIAOMI.equals(routerName)) {
                return R.drawable.cover_icon_xiaomi;
            } else if (SmbUtil.ROUTER_XUNLEI.equals(routerName)) {
                return R.drawable.cover_icon_xlrouter;
            } else {
                return R.drawable.cover_icon_router;
            }
        }
        return -1;
    }

    private int getBoxIcon() {
        String deviceName = DeviceModelUtil.getSupportBoxName();
        if (deviceName.equals(DeviceModelUtil.BOX_I71S)) {
            return R.drawable.cover_iconbox_skyworth;
        } else if (deviceName.equals(DeviceModelUtil.BOX_TCLV7)) {
            return R.drawable.cover_iconbox_tcl7v;
        } else if (deviceName.equals(DeviceModelUtil.BOX_XIAOMI)) {
            return R.drawable.cover_iconbox_mibox;
        } else {
            return R.drawable.cover_icon_commonbox;
        }
    }

    private View.OnFocusChangeListener mOnFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View view, boolean hasFocus) {
            MarqueenTextView descTx = (MarqueenTextView) view.findViewById(R.id.desc);
            if (view == mContainer.getChildAt(mContainer.getChildCount() - 1) && hasFocus) {
                mShadowView.setVisibility(View.GONE);
            } else if (mScrollView.getWidth() > getResources().getDisplayMetrics().widthPixels) {
                mShadowView.setVisibility(View.VISIBLE);
            }

            if (hasFocus && view == mContainer.getChildAt(mContainer.getChildCount() - 1)) {
                mScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
            }

            if (hasFocus && view == mContainer.getChildAt(0)) {
                mScrollView.fullScroll(HorizontalScrollView.FOCUS_LEFT);
            }

            if (hasFocus) {
                ((AnimateLinearLayout) mContainer).setCurrentFocusedId(view);
                view.startAnimation(AnimationUtils.loadAnimation(RemoteEnterActivity.this, R.anim.zoom_in));
                descTx.startMarqueen();
            } else {
                view.startAnimation(AnimationUtils.loadAnimation(RemoteEnterActivity.this, R.anim.zoom_out));
                descTx.stopMarqueen();
            }
        }
    };

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            DeviceItem item = (DeviceItem) view.getTag();
            jumpPage(item);
        }
    };


    private void jumpPage(DeviceItem item) {
        if (item.getType() == DeviceItem.DeviceType.TD_DOWNLOAD) {

            if (!DeviceModelUtil.isSupportBox()) {
                if (SettingManager.getInstance().isShowNotify()) {
                    showNotifyDialog();
                    return;
                }
            }

            if (DeviceModelUtil.isSupportReleaseService()) {
                if(mLocalTdDownloadMgr.isSupportTD()){
                    Intent intent = new Intent();
                    intent.putExtra(Constants.KEY_PAGE_TTILE,item.getName());
                    intent.setClass(this, BindTdActivity.class);
                    startActivity(intent);
                }else{
                    RemoteUsrHelpActivity.launchUserHelpPage(this);
                }

            } else {
                Intent intent = new Intent();
                intent.putExtra(Constants.KEY_REMOTE_TYPE, Constants.KEY_REMOTE_LOCAL);
                intent.putExtra(Constants.KEY_PAGE_TTILE,item.getName());
                intent.setClass(this, RemoteBindTdActivity.class);
                startActivity(intent);
            }
        }

        if (item.getType() == DeviceItem.DeviceType.XL_ROUTER_TDDOWNLOAD) {
            Intent intent = new Intent();
            intent.putExtra(Constants.KEY_PAGE_TTILE,item.getName());
            intent.setClass(this, RemoteBindTdActivity.class);
            intent.putExtra(Constants.KEY_REMOTE_TYPE, Constants.KEY_REMOTE_ROUTER);
            startActivity(intent);
        }
    }

    public void onEventMainThread(DeviceEvent event) {

    }

    private DeviceItem getLocalRemoteDeviceItem() {
        String title = null;
        String subtitle = null;
        if (DeviceModelUtil.isSupportBox()) {
            title = DeviceModelUtil.getSupportBoxName();

            List<FileItem> list = LocalTDDownloadManager.getInstance().getFileItems();
            if (list != null && list.size() > 0) {
                subtitle = "共下载" + list.size() + "部视频";
            } else {
                subtitle = "如何远程下载";
            }
        } else {
            title = getString(R.string.remote_box);
            subtitle = getString(R.string.remote_not_perfect_support);
        }
        return new DeviceItem(title, DeviceItem.DeviceType.TD_DOWNLOAD, "", 0, subtitle);
    }

    private DeviceItem getRouterDeviceItem() {
        if (XLRouterDownloadMgr.getInstance().isSupportTD()) {
            String subtitle = null;

            List<FileItem> list = XLRouterDownloadMgr.getInstance().getFileItems();
            if (list != null && list.size() > 0) {
                subtitle = "共下载" + list.size() + "部视频";
            } else {
                subtitle = "如何远程下载";
            }

            String routerName = SmbUtil.ROUTER_NAMES.get(SettingManager.getInstance().getRouterName());
            return new DeviceItem(routerName, DeviceItem.DeviceType.XL_ROUTER_TDDOWNLOAD, "", 0, subtitle);
        }

        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        unregisterReceiver(mTdReceiver);
    }

    private void showNotifyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final AlertDialog dialog = builder.create();
        View view = getLayoutInflater().inflate(R.layout.alert_supportdevice, null);
        view.findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingManager.getInstance().setShowNotify(false);
                dialog.dismiss();
            }
        });

        view.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        view.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setView(view);

        WindowManager.LayoutParams p = dialog.getWindow().getAttributes();
        p.width = (int) getResources().getDimension(R.dimen.remote_unbind_alert_width);
        p.height = (int) getResources().getDimension(R.dimen.remote_unbind_alert_height);
        p.gravity = Gravity.CENTER_VERTICAL;
        dialog.getWindow().setAttributes(p);

        dialog.show();


    }


    private void refreshTDownloadCount() {
        //这里逻辑我改了，不能inflate一个view来设置，应该获取当前的view
        if (mContainer != null) {

            for (int i = 0; i < mContainer.getChildCount(); i++) {

                View view = mContainer.getChildAt(i);

                DeviceItem item = (DeviceItem) view.getTag();

                int num = LocalTDDownloadManager.getInstance().getTDownloadNewFilesNum();
                int routerNum = XLRouterDownloadMgr.getInstance().getTDownloadNewFilesNum();

                int count = LocalTDDownloadManager.getInstance().getTDFilesCounts();
                int routerCount = XLRouterDownloadMgr.getInstance().getTDFilesCounts();

                int downloadingCount = LocalTDDownloadManager.getInstance().getDownloadingFilesNum();
                int downloadingRouterCount = XLRouterDownloadMgr.getInstance().getDownloadingFilesNum();

                MarqueenTextView descTv = (MarqueenTextView) view.findViewById(R.id.desc);
                TextView countTv = (TextView) view.findViewById(R.id.count_tv);
                ImageView previewIvIcon = (ImageView) view.findViewById(R.id.preview_iv_1);
                ImageView downloadingIv = (ImageView) view.findViewById(R.id.downloading_iv);
                View loadingCoverView = view.findViewById(R.id.loading_rl);


                if (item.getType() == DeviceItem.DeviceType.TD_DOWNLOAD) {

                    if(!LocalTDDownloadManager.getInstance().isSupportTD()){
                        loadingCoverView.setVisibility(View.VISIBLE);
                    }else{
                        loadingCoverView.setVisibility(View.GONE);
                    }

                    AppConfig.LOGD("in mainactivity count is: " + count);
                    if (count > 0) {
                        descTv.setText("共下载" + count + "部视频");
                    } else {
                        descTv.setText("如何远程下载");

                    }

                    AppConfig.LOGD("in mainactivity new num is: " + num);
                    if (num > 0) {
                        countTv.setText("" + num);
                        countTv.setVisibility(View.VISIBLE);
                    } else {
                        countTv.setText("");
                        countTv.setVisibility(View.INVISIBLE);
                    }

//                        if(downloadingCount + downloadingRouterCount > 0){
//                            AnimationDrawable animationDrawable = (AnimationDrawable) downloadingIv.getDrawable();
//                            animationDrawable.start();
//                            downloadingIv.setVisibility(View.VISIBLE);
//                        }else{
//                            AnimationDrawable animationDrawable = (AnimationDrawable) downloadingIv.getDrawable();
//                            animationDrawable.stop();
//                            downloadingIv.setVisibility(View.INVISIBLE);
//                        }
                } else if (item.getType() == DeviceItem.DeviceType.XL_ROUTER_TDDOWNLOAD) {
                    AppConfig.LOGD("in mainactivity count is: " + count);
                    if (routerCount > 0) {
                        descTv.setText("共下载" + routerCount + "部视频");
                    } else {
                        descTv.setText("如何远程下载");

                    }

                    AppConfig.LOGD("in mainactivity new num is: " + num);
                    if (routerNum > 0) {
                        countTv.setText("" + routerNum);
                        countTv.setVisibility(View.VISIBLE);
                    } else {
                        countTv.setText("");
                        countTv.setVisibility(View.INVISIBLE);
                    }

//                        if(downloadingCount + downloadingRouterCount > 0){
//                            AnimationDrawable animationDrawable = (AnimationDrawable) downloadingIv.getDrawable();
//                            animationDrawable.start();
//                            downloadingIv.setVisibility(View.VISIBLE);
//                        }else{
//                            AnimationDrawable animationDrawable = (AnimationDrawable) downloadingIv.getDrawable();
//                            animationDrawable.stop();
//                            downloadingIv.setVisibility(View.INVISIBLE);
//                        }
                }


            }
        }
    }

}