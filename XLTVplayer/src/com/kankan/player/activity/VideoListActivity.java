package com.kankan.player.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.xunlei.tv.player.R;
import com.kankan.player.adapter.VideoAdapter;
import com.kankan.player.event.VideoItemEvent;
import com.kankan.player.event.VideoListEvent;
import com.kankan.player.explorer.FileIconLoader;
import com.kankan.player.manager.VideoScanner;
import de.greenrobot.event.EventBus;

/**
 * Created by zhangdi on 14-4-8.
 */
public class VideoListActivity extends BaseActivity {

    private GridView mGridView;

    private ProgressBar mProgressBar;

    private VideoAdapter mVideoAdapter;

    private FileIconLoader mFileIconLoader;

    private VideoScanner mVideoScanner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_list);

        ((TextView) findViewById(R.id.title)).setText("影视库");

        mFileIconLoader = new FileIconLoader(this, null);

        mGridView = (GridView) findViewById(R.id.grid_view);
        mVideoAdapter = new VideoAdapter(this, mFileIconLoader);
        mGridView.setAdapter(mVideoAdapter);

        EventBus.getDefault().register(this);

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);


        mVideoScanner = VideoScanner.getInstance(getApplicationContext());
//        mVideoAdapter.setData(mVideoScanner.getVideoList());
        mVideoScanner.scanVideo();
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFileIconLoader.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFileIconLoader.pause();
    }

    @Override
    protected String getUmengPageName() {
        return "VideoListActivity";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFileIconLoader.stop();
        mVideoScanner.stop();
        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(VideoItemEvent event) {
        mVideoAdapter.addItem(event.videoItem);
    }

    public void onEventMainThread(VideoListEvent event) {
        mProgressBar.setVisibility(View.GONE);
        mVideoAdapter.setData(event.videoItemList);
    }

}
