package com.kankan.player.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.*;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.*;
import com.kankan.player.api.tddownload.SysInfo;
import com.kankan.player.app.AppConfig;
import com.kankan.player.app.AppRuntime;
import com.kankan.player.app.Constants;
import com.kankan.player.dao.model.VideoHistory;
import com.kankan.player.event.DeviceEvent;
import com.kankan.player.event.DeviceInfoEvent;
import com.kankan.player.event.VideoHistoryEvent;
import com.kankan.player.item.DeviceItem;
import com.kankan.player.manager.DeviceManager;
import com.kankan.player.manager.LocalTDDownloadManager;
import com.kankan.player.manager.VideoHistoryManager;
import com.kankan.player.manager.XLRouterDownloadMgr;
import com.kankan.player.service.BootReceiver;
import com.kankan.player.service.TdScanService;
import com.kankan.player.util.DeviceModelUtil;
import com.kankan.player.util.RemoteUtils;
import com.kankan.player.util.SettingManager;
import com.kankan.player.util.SmbUtil;
import com.kankan.player.view.AnimateLinearLayout;
import com.kankan.player.view.MarqueenTextView;
import com.plugin.common.utils.CustomThreadPool;
import com.plugin.common.utils.UtilsConfig;
import com.umeng.analytics.MobclickAgent;
import com.umeng.update.UmengUpdateAgent;
import com.xunlei.tv.player.R;
import de.greenrobot.event.EventBus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends BaseActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

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

    private ScannerReceiver mScannerReceiver;
    private VideoHistoryManager mVideoHistoryManager;

    private boolean mHasScanned = false;

    // 这两个变量用来控制焦点，因为在后台的时候改变焦点会出bug(据说在前台有时候也会有，但目前暂时没观察到，观察到再说吧)
    private boolean mNeedRefreshFocus = false;
    private boolean mIsBackground = false;

    //用于判断是否收到远程服务安装成功的广播
    private boolean mIsGetInstallSucess = false;

    Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UmengUpdateAgent.update(getApplicationContext());

        initUI();

        checkUpdateRemote();//检查是否是新的远程下载版本，如果是则释放文件并启动远程服务

        mVideoHistoryManager = new VideoHistoryManager(getApplicationContext());
        EventBus.getDefault().register(this);
        refreshDevice();//刷新主页上的我的下载，路由器，历史记录和本地文件，外接设备等item
        registerScannerReceiver();//监听外接设备的插拔变化并刷新UI

        registerTdReceiver();//外接设备中文件发生变化
        
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 统计--激活
                if (!SettingManager.getInstance().isAppFirstLaunch()) {
                    SettingManager.getInstance().setAppLaunched();
                    AppRuntime appRuntime = AppRuntime.getInstance(getApplicationContext());
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("device", appRuntime.getModel());
                    map.put("oem_channel", appRuntime.getChannel());
                    map.put("if_rmtdwn", LocalTDDownloadManager.getInstance().isSupportTD() ? "1" : "0");
                    map.put("if_xlrouter", SettingManager.getInstance().isSmbEnable() ? "1" : "0");
                    MobclickAgent.onEvent(MainActivity.this, "Activation", map);
                }

                HashMap<String, String> map = new HashMap<String, String>();
                map.put("From", "home");
                MobclickAgent.onEvent(MainActivity.this, "Startup", map);
            }
        }, 5000L);
    }

    @Override
    protected String getUmengPageName() {
        return "MainActivity";
    }

    private void initUI() {
        if (AppConfig.DEBUG) {
            AppConfig.LOGD("[[MainActivity]]  display parameters:\n");
            AppConfig.LOGD("\t density = " + getResources().getDisplayMetrics().density);
            AppConfig.LOGD("\t densityDpi = " + getResources().getDisplayMetrics().densityDpi);
            AppConfig.LOGD("\t widthPixel = " + getResources().getDisplayMetrics().widthPixels);
            AppConfig.LOGD("\t heightPixel = " + getResources().getDisplayMetrics().heightPixels);
        }

        mLayoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View bar = findViewById(R.id.top_bar);
        ImageView logoIv = (ImageView) bar.findViewById(R.id.home_up_iv);
        logoIv.setImageResource(R.drawable.logo_entry);
        logoIv.setLayoutParams(new LinearLayout.LayoutParams((int) getResources().getDimension(R.dimen.top_bar_logo_width), (int) getResources().getDimension(R.dimen.top_bar_logo_height)));
        findViewById(R.id.home_divider).setVisibility(View.GONE);

        mDeviceNumTv = (TextView) findViewById(R.id.device_num);
        mContainer = (LinearLayout) findViewById(R.id.container_ll);
        mScrollView = (HorizontalScrollView) findViewById(R.id.hsv);
        mLoadingProgress = (ProgressBar) findViewById(R.id.loading_pb);
        mShadowView = findViewById(R.id.shadow);

        mMarginLeft = getResources().getDimensionPixelSize(R.dimen.device_margin_left);
        mMarginRight = getResources().getDimensionPixelSize(R.dimen.device_margin_right);
        mFirstMarginLeft = getResources().getDimensionPixelSize(R.dimen.device_first_margin_left);
        mLastMarginRight = getResources().getDimensionPixelSize(R.dimen.device_last_margin_right);

        mContainer.requestFocus();

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mNeedRefreshFocus) {
            mNeedRefreshFocus = false;
            if (mViewItems.size() > 0) {
                mViewItems.get(0).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mViewItems.get(0).requestFocus();
                    }
                }, 300L);
            }
        }

        mIsBackground = false;

        //暂时加到这里，保证数目刷新及时
        refreshTDownloadCount();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mIsBackground = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        unregisterReceiver(mScannerReceiver);
        unregisterReceiver(mTdReceiver);
        MobclickAgent.flush(getApplicationContext());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            int pid = android.os.Process.myPid();
            android.os.Process.killProcess(pid);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onEventMainThread(DeviceEvent event) {
        if (event != null && event.deviceItems != null) {
            final boolean isItemChanged = handlerViewItem(event.deviceItems, event.types);

            if (!mIsBackground) {
                mViewItems.get(0).post(new Runnable() {
                    @Override
                    public void run() {
                        if(isItemChanged){
                            mViewItems.get(0).requestFocus();
                        }
                    }
                });
            } else {
                mNeedRefreshFocus = true;
            }
        }
    }

    private void deliverBroadCast(String action) {
        Intent intent = new Intent();
        intent.setAction(action);
        sendBroadcast(intent);
    }

    public void onEventMainThread(VideoHistoryEvent event) {
        // 当视频历史记录发生变化的时候，刷新界面
        for (int i = 0; i < mDeviceList.size(); i++) {
            DeviceItem item = mDeviceList.get(i);
            if (item.getType() == DeviceItem.DeviceType.HISTORY) {
                View view = mContainer.getChildAt(i);
                ImageView previewIv = (ImageView) view.findViewById(R.id.preview_iv);
                ImageView thumbnailIv = (ImageView) view.findViewById(R.id.thumbnail_iv);
                ImageView previewIvIcon = (ImageView) view.findViewById(R.id.preview_iv_1);
                VideoHistory videoHistory = mVideoHistoryManager.getLatestHistoryVideo();
                if (videoHistory != null && !TextUtils.isEmpty(videoHistory.getThumbnailPath())) {
                    previewIv.setVisibility(View.VISIBLE);
                    previewIv.setImageBitmap(BitmapFactory.decodeFile(videoHistory.getThumbnailPath()));
                    thumbnailIv.setVisibility(View.VISIBLE);
                    previewIvIcon.setVisibility(View.GONE);
                } else {
                    previewIv.setVisibility(View.GONE);
                    thumbnailIv.setVisibility(View.GONE);
                    previewIvIcon.setVisibility(View.VISIBLE);
                }

                break;
            }
        }
    }

    private void refreshTDownloadCount() {
        //这里逻辑我改了，不能inflate一个view来设置，应该获取当前的view
        if (mContainer != null) {

            for (int i = 0; i < mContainer.getChildCount(); i++) {

                View view = mContainer.getChildAt(i);

                DeviceItem item = (DeviceItem) view.getTag();

                if (item.getType() == DeviceItem.DeviceType.TD_DOWNLOAD) {

                    MarqueenTextView descTv = (MarqueenTextView) view.findViewById(R.id.desc);
                    TextView countTv = (TextView) view.findViewById(R.id.count_tv);
                    ImageView previewIvIcon = (ImageView) view.findViewById(R.id.preview_iv_1);
                    ImageView downloadingIv = (ImageView) view.findViewById(R.id.downloading_iv);
                    View loadingCoverView = view.findViewById(R.id.loading_rl);


                    if (AppConfig.isRemoteRouterOpen) {
                        int num = LocalTDDownloadManager.getInstance().getTDownloadNewFilesNum();
                        int routerNum = XLRouterDownloadMgr.getInstance().getTDownloadNewFilesNum();

                        int count = LocalTDDownloadManager.getInstance().getTDFilesCounts();
                        int routerCount = XLRouterDownloadMgr.getInstance().getTDFilesCounts();

                        int downloadingCount = LocalTDDownloadManager.getInstance().getDownloadingFilesNum();
                        int downloadingRouterCount = XLRouterDownloadMgr.getInstance().getDownloadingFilesNum();

                        if(!LocalTDDownloadManager.getInstance().isSupportTD() && !XLRouterDownloadMgr.getInstance().isSupportTD()){
                            loadingCoverView.setVisibility(View.VISIBLE);
                        }else{
                            loadingCoverView.setVisibility(View.GONE);
                        }

                        AppConfig.logRemote("in mainactivity count is: " + count);
                        int totalCount = count + routerCount;
                        if (totalCount > 0) {
                            descTv.setText("共下载" + totalCount + "部视频");
                        } else {
                            descTv.setText("如何远程下载");

                        }

                        AppConfig.logRemote("in mainactivity new num is: " + num);
                        int totalNum = num + routerNum;
                        if (totalNum > 0) {
                            countTv.setText("" + totalNum);
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

                    } else {
                        int count = LocalTDDownloadManager.getInstance().getTDownloadNewFilesNum();
                        int num = LocalTDDownloadManager.getInstance().getTDFilesCounts();

                        int downloadingCount = LocalTDDownloadManager.getInstance().getDownloadingFilesNum();

                        if(LocalTDDownloadManager.getInstance().isSupportTD()){
                            loadingCoverView.setVisibility(View.GONE);
                        }else{
                            loadingCoverView.setVisibility(View.VISIBLE);
                        }


                        UtilsConfig.LOGD("in mainactivity new count is: " + count);

                        if (count > 0) {
                            countTv.setText("" + count);
                            countTv.setVisibility(View.VISIBLE);
                        } else {
                            countTv.setText("");
                            countTv.setVisibility(View.INVISIBLE);
                        }


                        UtilsConfig.LOGD("in mainactivity new num is: " + num);

                        if (num > 0) {
                            descTv.setText("共下载" + num + "部视频");
                        } else {
                            descTv.setText("如何远程下载");
                        }

//                        if(downloadingCount > 0){
//                            AnimationDrawable animationDrawable = (AnimationDrawable) downloadingIv.getDrawable();
//                            animationDrawable.start();
//                            downloadingIv.setVisibility(View.VISIBLE);
//                        }else{
//                            AnimationDrawable animationDrawable = (AnimationDrawable) downloadingIv.getDrawable();
//                            animationDrawable.stop();
//                            downloadingIv.setVisibility(View.GONE);
//                        }
                    }


                }
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

        if (item.getType() == DeviceItem.DeviceType.HISTORY) {
            VideoHistory videoHistory = mVideoHistoryManager.getLatestHistoryVideo();
            if (videoHistory != null && !TextUtils.isEmpty(videoHistory.getThumbnailPath())) {
                previewIv.setVisibility(View.VISIBLE);
                previewIv.setImageBitmap(BitmapFactory.decodeFile(videoHistory.getThumbnailPath()));
                thumbnailIv.setVisibility(View.VISIBLE);
                previewIvIcon.setVisibility(View.INVISIBLE);
            } else {
                previewIv.setVisibility(View.GONE);
                thumbnailIv.setVisibility(View.GONE);
                previewIvIcon.setVisibility(View.VISIBLE);
            }
        } else if (item.getType() == DeviceItem.DeviceType.TD_DOWNLOAD) {
            // 根据情况显示新增文件数，刚加载时得不到数组，暂不显示.
            countTv.setVisibility(View.INVISIBLE);
            previewIvIcon.setVisibility(View.VISIBLE);

            if(!LocalTDDownloadManager.getInstance().isSupportTD()||XLRouterDownloadMgr.getInstance().isSupportTD()){
                loadingCoverView.setVisibility(View.VISIBLE);
            }else{
                loadingCoverView.setVisibility(View.GONE);
            }

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

    private int getCoverResId(DeviceItem.DeviceType type) {
        if (type == DeviceItem.DeviceType.USB) {
            return R.drawable.usb_bg;
        } else if (type == DeviceItem.DeviceType.HHD) {
            return R.drawable.disk_bg;
        } else if (type == DeviceItem.DeviceType.TD_DOWNLOAD) {
            return R.drawable.td_bg;
        } else if (type == DeviceItem.DeviceType.VIDEO_LIST) {
            return R.drawable.cover_video;
        } else if (type == DeviceItem.DeviceType.HISTORY) {
            return R.drawable.history_bg;
        } else if (type == DeviceItem.DeviceType.EXTERNAL) {
            return R.drawable.local_bg;
        } else if (type == DeviceItem.DeviceType.XL_ROUTER) {
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
            return R.drawable.logo_entry;
        } else if (type == DeviceItem.DeviceType.VIDEO_LIST) {
            return R.drawable.cover_video;
        } else if (type == DeviceItem.DeviceType.HISTORY) {
            return R.drawable.cover_icon_history;
        } else if (type == DeviceItem.DeviceType.EXTERNAL) {
            return R.drawable.cover_icon_local;
        } else if (type == DeviceItem.DeviceType.XL_ROUTER) {
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
                view.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.zoom_in));
                descTx.startMarqueen();
            } else {
                view.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.zoom_out));
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

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            View view = mContainer.getFocusedChild();
            if (view != null) {
                DeviceItem deviceItem = (DeviceItem) view.getTag();
                if (deviceItem.getType() == DeviceItem.DeviceType.USB) {
                    String ret = execShell("umount " + deviceItem.getPath());
                    LOGD(ret);
                }
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void jumpPage(DeviceItem item) {
        String clickFrom = "";
        if (item.getType() == DeviceItem.DeviceType.HISTORY) {
            startActivity(new Intent(this, VideoHistoryActivity.class));
            clickFrom = "history";
        } else if (item.getType() == DeviceItem.DeviceType.VIDEO_LIST) {
            startActivity(new Intent(this, VideoListActivity.class));
        } else if (item.getType() == DeviceItem.DeviceType.TD_DOWNLOAD) {
            startThunberDownloadActivity();
            clickFrom = "remote";
        } else if (item.getType() == DeviceItem.DeviceType.XL_ROUTER) {
            SmbExplorerActivity.start(this, item, item.getPath(), 0);
            String routerName = SettingManager.getInstance().getRouterName();
            if (SmbUtil.ROUTER_XIAOMI.equals(routerName)) {
                clickFrom = "xiaomirouter";
            } else if (SmbUtil.ROUTER_XUNLEI.equals(routerName)) {
                clickFrom = "xlrouter";
            } else {
                clickFrom = "smb";
            }
        } else {
            FileExplorerActivity.startActivity(this, item, item.getPath(), false, true);
            if (item.getType() == DeviceItem.DeviceType.EXTERNAL) {
                clickFrom = "local";
            } else {
                clickFrom = "plug";
            }
        }

        HashMap<String, String> map = new HashMap<String, String>();
        map.put("From", clickFrom);
        MobclickAgent.onEvent(this, "File_enter", map);
    }

    public Bitmap createReflectedBitmap(Bitmap bitmap) {
        final int reflectionGap = 4;// 倒影图和原图之间的距离
        int index = 0;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        // 创建矩阵对象
        Matrix matrix = new Matrix();

        // 指定一个角度以0,0为坐标
        // matrix.setRotate(30);

        // 指定矩阵(x轴不变，y轴相反)屏幕中央两边图片旋转方向不同
        matrix.preScale(1, -1);

        // 将矩阵应用到该原图之中，返回一个宽度不变，高度为原图1/2的倒影位图
        Bitmap reflectionImage = Bitmap.createBitmap(bitmap, 0,
                height / 2, width, height / 2, matrix, false);
        Canvas canvas = new Canvas(reflectionImage);
        Paint deafaultPaint = new Paint();
        deafaultPaint.setAntiAlias(false);
            /*canvas.drawRect(0, height, width, height + reflectionGap,
                    deafaultPaint);*/
        canvas.drawBitmap(reflectionImage, 0, height + reflectionGap, null);
        Paint paint = new Paint();
        paint.setAntiAlias(false);

        /**
         * 参数一:为渐变起初点坐标x位置， 参数二:为y轴位置， 参数三和四:分辨对应渐变终点， 最后参数为平铺方式，
         * 这里设置为镜像Gradient是基于Shader类，所以我们通过Paint的setShader方法来设置这个渐变
         */
        LinearGradient shader = new LinearGradient(0, 0, 0,
                reflectionImage.getHeight(),
                0x70ffffff, 0x00ffffff, Shader.TileMode.MIRROR);
        // 设置阴影
        paint.setShader(shader);
        paint.setXfermode(new PorterDuffXfermode(
                android.graphics.PorterDuff.Mode.DST_IN));
        // 用已经定义好的画笔构建一个矩形阴影渐变效果
        canvas.drawRect(0, 0, width, reflectionImage.getHeight(), paint);

        return reflectionImage;
    }

    private void registerScannerReceiver() {
        mScannerReceiver = new ScannerReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        intentFilter.addDataScheme("file");
        registerReceiver(mScannerReceiver, intentFilter);
    }

    private void registerTdReceiver() {
        mTdReceiver = new TdReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TdScanService.ACTION_SEND_QUERY_LOCAL_RESULT);
        intentFilter.addAction(TdScanService.ACTION_SEND_QUERY_ROUTER_RESULT);
        intentFilter.addAction(TdScanService.ACTION_INSTALL_SUCESS);
        registerReceiver(mTdReceiver, intentFilter);

    }

    private class ScannerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // 插拔存储设备的广播系统真烂。。乱七八糟的，擦
            if (Intent.ACTION_MEDIA_EJECT.equals(action) || Intent.ACTION_MEDIA_MOUNTED.equals(action) || Intent.ACTION_MEDIA_UNMOUNTED.equals(action)) {
                if (!mHasScanned) {
                    mHasScanned = true;

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            refreshUsbDevices();
                            mHasScanned = false;
                        }
                    }, 500L);
                }
            }
        }
    }

    private TdReceiver mTdReceiver;

    private class TdReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(TdScanService.ACTION_SEND_QUERY_LOCAL_RESULT)
                    || action.equals(TdScanService.ACTION_SEND_QUERY_ROUTER_RESULT)) {

                if (AppConfig.isRemoteRouterOpen) {
                    if (LocalTDDownloadManager.getInstance().isSupportTD()
                            || XLRouterDownloadMgr.getInstance().isSupportTD()) {

                        addTdDownload();

                        refreshTDownloadCount();



                    } else {

                        deliverBroadCast(BootReceiver.ACTION_ALARM_TIME);
                    }
                } else {
                    if (LocalTDDownloadManager.getInstance().isSupportTD()) {

                        addTdDownload();

                        refreshTDownloadCount();

                    } else {

                        deliverBroadCast(BootReceiver.ACTION_ALARM_TIME);
                    }
                }

            }

        }
    }

    private void addTdDownload(){
        if (mDeviceList != null) {
            int i = 0;
            for (; i < mDeviceList.size(); i++) {
                DeviceItem item = mDeviceList.get(i);
                if (item.getType() == DeviceItem.DeviceType.TD_DOWNLOAD) {
                    break;
                }

            }

            if (i >= mDeviceList.size()) {
                List<DeviceItem> items = new ArrayList<DeviceItem>();
                List<DeviceItem.DeviceType> types = new ArrayList<DeviceItem.DeviceType>();
                items.add(new DeviceItem(getString(R.string.remote_title_mydownload), DeviceItem.DeviceType.TD_DOWNLOAD, "", 0, getString(R.string.remote_how_download)));
                types.add(DeviceItem.DeviceType.TD_DOWNLOAD);


                DeviceEvent event = new DeviceEvent();
                event.deviceItems = items;
                event.types = types;
                EventBus.getDefault().post(event);
            }

        }
    }

    private String execShell(String cmd) {
        String[] cmdStrings = new String[]{"sh", "-c", cmd};
        StringBuffer retString = new StringBuffer();

        BufferedReader outBr = null;
        BufferedReader errBr = null;
        try {
            Process process = Runtime.getRuntime().exec(cmdStrings);
            outBr = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));
            errBr = new BufferedReader(new InputStreamReader(
                    process.getErrorStream()));

            String line;
            while (!TextUtils.isEmpty(line = outBr.readLine())) {
                retString.append(line + "\n");
            }
            retString.append("\n");
            while (!TextUtils.isEmpty(line = errBr.readLine())) {
                retString.append(line + "\n");
            }
        } catch (Exception e) {
            retString.append(e.getMessage());
        } finally {
            try {
                if (outBr != null)
                    outBr.close();
                if (errBr != null)
                    errBr.close();
            } catch (IOException e) {
            }
        }
        return retString.toString();
    }

    private void startThunberDownloadActivity() {

        if (!AppConfig.isRemoteRouterOpen) {
            if(LocalTDDownloadManager.getInstance().isSupportTD()){
                Intent intent = new Intent();
                intent.setClass(this, BindTdActivity.class);
                startActivity(intent);
            }else{
                RemoteUsrHelpActivity.launchUserHelpPage(this);
            }

        } else {

            if (XLRouterDownloadMgr.getInstance().isSupportTD()) {
                Intent intent = new Intent();
                intent.setClass(this, RemoteEnterActivity.class);
                startActivity(intent);
            } else if (LocalTDDownloadManager.getInstance().isSupportTD()) {

                if (DeviceModelUtil.isSupportReleaseService()) {
                    Intent intent = new Intent();
                    intent.setClass(this, BindTdActivity.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent();
                    intent.setClass(this, RemoteBindTdActivity.class);
                    intent.putExtra(Constants.KEY_REMOTE_TYPE, Constants.KEY_REMOTE_LOCAL);
                    startActivity(intent);
                }
            }else{
                RemoteUsrHelpActivity.launchUserHelpPage(this);
            }
        }

    }

    private void checkShadow() {
        mShadowView.setVisibility(mScrollView.getWidth() > getResources().getDisplayMetrics().widthPixels ? View.VISIBLE : View.GONE);
    }

    private boolean handlerViewItem(List<DeviceItem> items, List<DeviceItem.DeviceType> types) {
        //fix 移动设备拔出，异常退出问题
        List<DeviceItem> removeItemList = new ArrayList<DeviceItem>();
        for (DeviceItem item : mDeviceList) {
            if (!items.contains(item) && types.contains(item.getType())) {
                removeItemList.add(item);
            }
        }

        boolean itemsChanged = false;
        if(removeItemList.size() > 0){
            itemsChanged = true;
        }

        for (DeviceItem item : removeItemList) {
            removeItem(item);
        }

        removeItemList.clear();

        for (DeviceItem item : items) {
            if (!mDeviceList.contains(item) && types.contains(item.getType())) {
                addItem(item);
                itemsChanged = true;
            }
        }

        return itemsChanged;
    }

    private int getInsertIndexOfItem(DeviceItem item) {
        if (item == null || mDeviceList.contains(item)) {
            return -1;
        }

        int index = 0;
        for (int i = 0; i < mDeviceList.size(); i++) {
            if (DeviceItem.DeviceType.compare(item.getType(), mDeviceList.get(i).getType())) {
                index = i;
                break;
            }
        }
        return index;
    }

    private void LOGD(String msg) {
        if (AppConfig.DEBUG && !TextUtils.isEmpty(msg)) {
            Log.d(TAG, msg);
        }
    }

    private void refreshDevice() {
        mLoadingProgress.setVisibility(View.VISIBLE);

        CustomThreadPool.asyncWork(new Runnable() {
            @Override
            public void run() {
                DeviceManager.getInstance(getApplicationContext()).refreshDevices();

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mLoadingProgress.setVisibility(View.GONE);


                    }
                });
            }
        });
    }

    private void refreshTdDownload() {

        CustomThreadPool.asyncWork(new Runnable() {
            @Override
            public void run() {
                DeviceManager.getInstance(getApplicationContext()).refreshTdDownload();

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {

                        if (AppConfig.isForXLRouter) {
                            // 有时候smb服务器会出异常，第一次连不上，第二次或过一会就能连上了，这里如果第一次没检测到的话，过一会再检测一次
                            boolean hasSmbRouter = false;
                            for (int i = 0; i < mDeviceList.size(); i++) {
                                if (mDeviceList.get(i).getType() == DeviceItem.DeviceType.XL_ROUTER) {
                                    hasSmbRouter = true;
                                    break;
                                }
                            }
                            if (!hasSmbRouter) {
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        refreshRouter();
                                    }
                                }, 5000L);
                            }
                        }
                    }
                });
            }
        });

    }

    private void refreshUsbDevices() {
        mLoadingProgress.setVisibility(View.VISIBLE);

        CustomThreadPool.asyncWork(new Runnable() {
            @Override
            public void run() {
                DeviceManager.getInstance(getApplicationContext()).refreshUsbDevices();

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mLoadingProgress.setVisibility(View.GONE);
                    }
                });
            }
        });

    }

    private void refreshRouter() {
        mLoadingProgress.setVisibility(View.VISIBLE);

        CustomThreadPool.asyncWork(new Runnable() {
            @Override
            public void run() {
                DeviceManager.getInstance(getApplicationContext()).refreshRouter();

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mLoadingProgress.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    public void onEventMainThread(DeviceInfoEvent event) {
        if (event != null) {
            if(event.device != null){
                DeviceModelUtil.setDeviceInfo(event.device);
                if (DeviceModelUtil.isSupportReleaseService()) {
                    AppConfig.logRemote("in eventmainThread to start td");
                    if(!LocalTDDownloadManager.getInstance().isSupportTD()){
                        if(!LocalTDDownloadManager.getInstance().getIsTimeOut()){
                            TdScanService.startTdScanServiceInstallFiles(getApplicationContext());
                        }
                    }
                } else {
                    if(AppConfig.DEBUG){
                        AppConfig.logRemote("not support device prepare to kill");
                    }
                    TdScanService.stopTdServer(getApplicationContext());
                    deliverBroadCast(BootReceiver.ACTION_ALARM_TIME);
                }
            }else{
                AppConfig.logRemote("not support device null");

                if(DeviceModelUtil.isSupportBox()||TextUtils.isEmpty(SettingManager.getInstance().getPartnerId())){
                    deliverBroadCast(BootReceiver.ACTION_ALARM_TIME);
                    return;
                }

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshTdDownload();
                    }
                }, 1000);
            }

        }
    }

    private void checkUpdateRemote(){
        int currVerionCode = RemoteUtils.getCurrentVersionCode(getApplicationContext());
        int savedVersionCode = SettingManager.getInstance().getVersionCode();

        if (currVerionCode > savedVersionCode) {
            SettingManager.getInstance().setVersionUpdate(true);
        } else {
            SettingManager.getInstance().setVersionUpdate(false);
        }

        SettingManager.getInstance().setVersionCode(currVerionCode);


        //当应用版本更新去释放远程服务
        if (SettingManager.getInstance().getVersionUpdate()) {
            SettingManager.getInstance().setVersionUpdate(false);
            AppConfig.logRemote("in mainactivity to start td");
            TdScanService.startTdScanServiceInstallFiles(getApplicationContext());
        }
    }


}
