package com.kankan.player.fragment;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.kankan.media.Media;
import com.kankan.player.activity.BindTdActivity;
import com.kankan.player.activity.PlayVideoActivity;
import com.kankan.player.activity.RemoteBindTdActivity;
import com.kankan.player.adapter.FileExplorerAdapter;
import com.kankan.player.api.tddownload.SysInfo;
import com.kankan.player.app.AppConfig;
import com.kankan.player.app.Constants;
import com.kankan.player.event.SublistEvent;
import com.kankan.player.explorer.FileCategory;
import com.kankan.player.explorer.FileIconHelper;
import com.kankan.player.explorer.FileIconLoader;
import com.kankan.player.explorer.FileItem;
import com.kankan.player.item.DeviceItem;
import com.kankan.player.manager.FileExploreHistoryManager;
import com.kankan.player.manager.LocalTDDownloadManager;
import com.kankan.player.manager.XLRouterDownloadMgr;
import com.kankan.player.util.DeviceModelUtil;
import com.kankan.player.util.SettingManager;
import com.kankan.player.util.SmbUtil;
import com.umeng.analytics.MobclickAgent;
import com.xunlei.tv.player.R;
import de.greenrobot.event.EventBus;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Created by wangyong on 14-5-6.
 */
public class DownlistFragment extends Fragment implements AdapterView.OnItemClickListener {

    private ListView mListView;

    private FileExplorerAdapter mFileExplorerAdapter;

    private FileIconHelper mFileIconHelper;

    private LocalTDDownloadManager mTDDownloadMgr;
    private XLRouterDownloadMgr mRouterDownloadMgr;

    private LinearLayout mTopView;

    private Button mBtnBind;
    private View mDownloadlingView;
    private AnimationDrawable mAnimationDrawble;
    private View mEmptyView;
    private View mCoverView;

    private TextView mDownloadingTv;

    private List<FileItem> mFileItems = new ArrayList<FileItem>();

    private boolean isSubList = false;

    private String mFileName;

    private int mRemoteType;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = View.inflate(getActivity(), R.layout.fragment_downloadlist, null);

        mListView = (ListView) view.findViewById(R.id.lv);
        mListView.requestFocus();

        this.mTopView = (LinearLayout) view.findViewById(R.id.bind_ll);
        this.mBtnBind = (Button) view.findViewById(R.id.btn_bind);
        this.mCoverView = view.findViewById(R.id.cover);

        this.mDownloadlingView = view.findViewById(R.id.downloading_ll);
        this.mDownloadingTv = (TextView) view.findViewById(R.id.downloading_tv);
        ImageView mDownloadingIv = (ImageView) view.findViewById(R.id.downloading_iv);
        mAnimationDrawble = (AnimationDrawable) mDownloadingIv.getDrawable();
        this.mEmptyView = view.findViewById(R.id.empty_rl);

        mFileIconHelper = new FileIconHelper(getActivity().getApplicationContext());
        mFileIconHelper.setIconProcessFilter(null);

        mListView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, final View view, int position, long id) {
                mFileExplorerAdapter.setSelectedId(position);

                mCoverView.post(new Runnable() {
                    @Override
                    public void run() {
                        mCoverView.setY(view.getY() - getResources().getDimension(R.dimen.file_list_item_cover_top_margin));
                        if (mCoverView.getVisibility() != View.VISIBLE) {
                            mCoverView.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mListView.setOnItemClickListener(this);

        mListView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                int pos = mFileExplorerAdapter.getSelectedId();
                FileItem fileItem = (FileItem) mFileExplorerAdapter.getItem(pos);
                if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {

                        if (mRemoteType == Constants.KEY_REMOTE_ROUTER) {
                            return performOnclickRouterSmb(fileItem);
                        } else {
                            return performOnClickLocal(fileItem);
                        }
                    }

                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        if (isSubList) {
                            getFragmentManager().popBackStack();
                            return true;
                        }

                    }
                }


                return false;
            }
        });

        this.mBtnBind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getActivity() != null) {
                    if (getActivity() instanceof BindTdActivity) {
                        BindTdActivity activity = (BindTdActivity) getActivity();
                        AppConfig.LOGD("bindstatus in onclick call turn2bindentry");
                        activity.turn2BindEntry(true, true);
                    }
                }
            }
        });

        this.mBtnBind.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    mListView.setSelection(-1);
                    mFileExplorerAdapter.notifyDataSetChanged();
                    mCoverView.post(new Runnable() {

                        @Override
                        public void run() {
                            mCoverView.setVisibility(View.INVISIBLE);
                        }
                    });
                } else {
                    mListView.setSelection(0);
                    mFileExplorerAdapter.notifyDataSetChanged();
                    mCoverView.post(new Runnable() {

                        @Override
                        public void run() {
                            if (mCoverView.getVisibility() != View.VISIBLE) {
                                mCoverView.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                }
            }
        });

        initData();

        return view;
    }

    private void initData() {
        EventBus.getDefault().register(this);

        this.mTDDownloadMgr = LocalTDDownloadManager.getInstance();
        this.mRouterDownloadMgr = XLRouterDownloadMgr.getInstance();

        Bundle bundle = getArguments();
        if (bundle != null) {
            List<FileItem> items = (List<FileItem>) bundle.getSerializable(Constants.KEY_TDDOWNLOAD_LIST);

            if (items != null) {
                mFileItems.clear();
                mFileItems.addAll(items);

            }
            isSubList = bundle.getBoolean(Constants.KEY_SUBLIST_FRAGMENT);

            mFileName = bundle.getString(Constants.KEY_SUBLIST_TITLE, "");

            mRemoteType = bundle.getInt(Constants.KEY_REMOTE_TYPE, -1);


            if (isSubList) {

                FileItem item = (FileItem) bundle.getSerializable(Constants.KEY_TDDOWNLOAD_SUBITEM);
                if (item != null) {
                    if (mRemoteType == Constants.KEY_REMOTE_ROUTER) {
                        this.mRouterDownloadMgr.getSublistFileItems(item, getActivity().getApplicationContext());
                    } else {
                        this.mTDDownloadMgr.getSublistFileItems(item, getActivity().getApplicationContext());
                    }

                }

            }


            mFileExplorerAdapter = new FileExplorerAdapter(getActivity().getApplicationContext(), mFileItems, mFileIconHelper);
            mListView.setAdapter(mFileExplorerAdapter);
            if (mFileItems != null && mFileItems.size() > 0) {
                mListView.setSelection(0);
            }
        }

        changeBindstatusView();

    }

    @Override
    public void onResume() {
        super.onResume();
        if (mFileExplorerAdapter != null) {
            mFileExplorerAdapter.notifyDataSetChanged();
        }

        if (getActivity() instanceof BindTdActivity) {
            ((BindTdActivity) getActivity()).setCurrentFragment(this);

            ((BindTdActivity) getActivity()).changeBarView();
            if (isSubList) {
                ((BindTdActivity) getActivity()).setBarTitle(mFileName);
            }

        }

        if (getActivity() instanceof RemoteBindTdActivity) {
            ((RemoteBindTdActivity) getActivity()).setCurrentFragment(this);

            ((RemoteBindTdActivity) getActivity()).changeBarView();
            if (isSubList) {
                ((RemoteBindTdActivity) getActivity()).setBarTitle(mFileName);
            }

        }

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        FileItem fileItem = (FileItem) mFileExplorerAdapter.getItem(position);
        if (mRemoteType == Constants.KEY_REMOTE_ROUTER) {
            performOnclickRouterSmb(fileItem);
        } else {
            performOnClickLocal(fileItem);
        }
    }

    private boolean performOnClickLocal(FileItem fileItem) {
        if (fileItem != null) {

            File file = new File(fileItem.filePath);
            if (file != null && file.exists() && file.canRead()) {
                addFile2History(fileItem, getActivity().getApplicationContext(),DeviceItem.DeviceType.TD_DOWNLOAD);

                if (fileItem.category == FileCategory.DIR) {
                    turn2SubDownloadlist(fileItem, Constants.KEY_REMOTE_LOCAL);
                    return true;

                }

                if (fileItem.category == FileCategory.VIDEO) {
                    playVideo(fileItem);
                    return true;
                }

            }

            return false;

        }

        return false;
    }

    private boolean performOnclickRouterSmb(FileItem fileItem) {

        addFile2History(fileItem, getActivity().getApplicationContext(), DeviceItem.DeviceType.XL_ROUTER_TDDOWNLOAD);

        if (fileItem.category == FileCategory.DIR) {
            //todo
            turn2SubDownloadlist(fileItem, Constants.KEY_REMOTE_ROUTER);
            return true;
        }

        if (fileItem.category == FileCategory.VIDEO) {
            playVideo(fileItem.filePath);
            return true;
        }

        return false;
    }

    private void playVideo(FileItem item) {
        if (item != null) {

            PlayVideoActivity.start(this.getActivity(), item.filePath, null);

            // 统计--播放摘要
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("From", "remote");
            map.put("if_continue", item.filePath != null && Media.getDuration(Uri.parse(item.filePath)) == 0 ? "1" : "0");
            MobclickAgent.onEvent(getActivity(), "Play", map);
        }
    }

    private void playVideo(String path) {
        if (SmbUtil.isSmbPath(path)) {
            path = SmbUtil.generateSmbPlayPath(path);
        }

        DeviceItem deviceItem = new DeviceItem();
        deviceItem.setName("路由器远程下载");
        deviceItem.setType(DeviceItem.DeviceType.XL_ROUTER_TDDOWNLOAD);

        PlayVideoActivity.start(this.getActivity(), path, deviceItem);

    }

    public void changeBindstatusView() {
        SysInfo infos = null;
        int downloadingNum = 0;
        if(mRemoteType == Constants.KEY_REMOTE_ROUTER){
            infos = this.mRouterDownloadMgr.getSysInfo();
            downloadingNum = this.mRouterDownloadMgr.getDownloadingFilesNum();
        }else{
            infos = this.mTDDownloadMgr.getSysInfo();
            downloadingNum = this.mTDDownloadMgr.getDownloadingFilesNum();
        }
        if (infos != null) {
            int status = infos.isBindOk;
            if (status == Constants.KEY_REMOTE_BIND_SUCESS) {

                if (downloadingNum <= 0 || isSubList) {
                    this.mTopView.setVisibility(View.GONE);
                } else {
                    this.mDownloadingTv.setText(String.format(getString(R.string.remote_downloading_tv, downloadingNum)));

                    this.mBtnBind.setVisibility(View.GONE);
                    this.mDownloadlingView.setVisibility(View.VISIBLE);
                    this.mAnimationDrawble.start();
                    this.mTopView.setVisibility(View.VISIBLE);
                }

            } else {

                if(mRemoteType == Constants.KEY_REMOTE_ROUTER){
                    this.mBtnBind.setVisibility(View.GONE);
                    this.mAnimationDrawble.stop();
                    this.mDownloadlingView.setVisibility(View.GONE);
                    this.mTopView.setVisibility(View.GONE);
                }else{

                    if(DeviceModelUtil.isSupportReleaseService()){
                        this.mBtnBind.setVisibility(View.VISIBLE);
                        this.mAnimationDrawble.stop();
                        this.mDownloadlingView.setVisibility(View.GONE);
                        this.mTopView.setVisibility(View.VISIBLE);
                    }else{
                        this.mBtnBind.setVisibility(View.GONE);
                        this.mAnimationDrawble.stop();
                        this.mDownloadlingView.setVisibility(View.GONE);
                        this.mTopView.setVisibility(View.GONE);
                    }

                }
            }
        } else {
            //infos 为空的情况，远程接口访问失败
            this.mBtnBind.setVisibility(View.VISIBLE);
            this.mAnimationDrawble.stop();
            this.mDownloadlingView.setVisibility(View.GONE);
            this.mTopView.setVisibility(View.VISIBLE);
        }

    }

    public void refreshTDDownloadlist(List<FileItem> list) {
        if (list != null) {
            mFileExplorerAdapter.setData(list);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void turn2SubDownloadlist(FileItem videofile, int remoteType) {

        if (videofile != null) {

            DownlistFragment mInnerSubDowloadFragment = new DownlistFragment();
            FragmentTransaction trans = getFragmentManager().beginTransaction();
            Bundle bundle = new Bundle();
            bundle.putSerializable(Constants.KEY_TDDOWNLOAD_SUBITEM, (Serializable) videofile);
            bundle.putBoolean(Constants.KEY_SUBLIST_FRAGMENT, true);
            bundle.putString(Constants.KEY_SUBLIST_TITLE, videofile.fileName);
            bundle.putInt(Constants.KEY_REMOTE_TYPE, remoteType);
            mInnerSubDowloadFragment.setArguments(bundle);
            trans.replace(R.id.container_fl, mInnerSubDowloadFragment);
            trans.addToBackStack(null);
            trans.commit();

        }

    }

    public boolean isSubList() {
        return isSubList;
    }

    public String getmFileName() {
        return mFileName;
    }

    public void onEventMainThread(SublistEvent event) {
        if (event.list != null && event.list.size()>0) {
            mFileItems.clear();
            mFileItems.addAll(event.list);
            mFileExplorerAdapter.notifyDataSetChanged();

            mListView.setSelection(0);
        }else{
            mListView.setEmptyView(mEmptyView);
        }
    }

    private void addFile2History(FileItem fileItem, Context context,DeviceItem.DeviceType type) {
        FileExploreHistoryManager fileExploreHistoryManager = new FileExploreHistoryManager(getActivity().getApplicationContext());
        DeviceItem deviceItem = new DeviceItem();
        deviceItem.setType(type);
        deviceItem.setPath(fileItem.filePath);
        fileExploreHistoryManager.addFileToExploreHistory(fileItem, deviceItem);
        fileItem.isNew = false;

        mFileExplorerAdapter.notifyDataSetChanged();
    }

}