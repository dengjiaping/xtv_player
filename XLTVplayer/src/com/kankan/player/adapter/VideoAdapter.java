package com.kankan.player.adapter;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.xunlei.tv.player.R;
import com.kankan.player.activity.PlayVideoActivity;
import com.kankan.player.activity.VideoHistoryActivity;
import com.kankan.player.explorer.FileCategory;
import com.kankan.player.explorer.FileIconLoader;
import com.kankan.player.explorer.FileUtils;
import com.kankan.player.item.DeviceItem;
import com.kankan.player.item.VideoItem;
import com.kankan.player.util.DateTimeFormatter;
import com.kankan.player.util.UIHelper;
import com.kankan.player.view.VideoView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangdi on 14-4-1.
 */
public class VideoAdapter extends BaseAdapter {

    private Context mContext;

    private LayoutInflater mLayoutInflater;

    private FileIconLoader mFileIconLoader;

    private List<VideoItem> mVideoList;
    private Handler mHandler;

    public VideoAdapter(Context context, FileIconLoader fileIconLoader) {
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mFileIconLoader = fileIconLoader;
        mVideoList = new ArrayList<VideoItem>();
    }

    public void setData(List<VideoItem> videoItems) {
        mVideoList.clear();
        if (videoItems != null) {
            mVideoList.addAll(videoItems);
        }
        notifyDataSetChanged();
    }

    public void addItem(VideoItem videoItem) {
        if (videoItem != null) {
            mVideoList.add(videoItem);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mVideoList != null ? mVideoList.size() : 0;
    }

    @Override
    public Object getItem(int i) {
        return mVideoList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.item_video, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final VideoItem videoItem = (VideoItem) getItem(position);

        String fileName = videoItem.getFileName();
        if (TextUtils.isEmpty(fileName)) {
            fileName = FileUtils.getNameFromFilepath(videoItem.getFilePath());
        }
        holder.nameTv.setText(fileName);
        holder.densityIv.setImageResource(getResourceByDensity(videoItem));
        holder.durationTv.setText(DateTimeFormatter.formatDuration(videoItem.getDuration()));
        holder.progressBar.setMax(videoItem.getDuration());
        holder.progressBar.setProgress(videoItem.getProgress());

        if (!new File(videoItem.getFilePath()).exists()) {
            //convertView.setEnabled(false);
            convertView.setPressed(false);
        }

        if (!TextUtils.isEmpty(videoItem.getFilePath())) {
            mFileIconLoader.loadIcon(holder.thumbnailIv, videoItem.getFilePath(), FileCategory.VIDEO);
        } else {
            holder.thumbnailIv.setImageDrawable(null);
        }

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mContext instanceof Activity) {
                    Activity activity = (Activity) mContext;
                    if (!TextUtils.isEmpty(videoItem.getFilePath()) && new File(videoItem.getFilePath()).exists()) {
                        PlayVideoActivity.startForResult(activity, videoItem, true, VideoHistoryActivity.REQUEST_CODE_PLAY_VDIEO);
                    } else {
                        UIHelper.showAlertTips(activity, R.drawable.icon_warn, R.string.tips_not_found_title, getVideoNotFoundSubtitleByDeviceType(videoItem.getDeviceType()), false);
                    }
                }
            }
        });

        convertView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(final View view, boolean focused) {
//                if (focused) {
//                    //holder.nameTv.setEllipsize(TextUtils.TruncateAt.MARQUEE);
//                    //holder.nameTv.setSelected(true);
//
//                    // 当焦点停留3秒则开始预播
//                    mHandler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            if (view.isFocused()) {
//                                holder.previewVideoView.setVisibility(View.VISIBLE);
//                                holder.previewVideoView.setPreviewMode(true);
//                                holder.previewVideoView.setVideoPath(videoItem.getFilePath());
//                                holder.previewVideoView.seekTo(videoItem.getProgress());
//                                holder.previewVideoView.setAutoAjustSize(false);
//                                holder.previewVideoView.start();
//                            }
//                        }
//                    }, 3000L);
//                } else {
//                    //holder.nameTv.setEllipsize(TextUtils.TruncateAt.END);
//
//                    holder.previewVideoView.pause();
//                    holder.previewVideoView.setVisibility(View.GONE);
//                }
            }
        });

        return convertView;
    }

    private static class ViewHolder {
        ImageView densityIv;
        ImageView thumbnailIv;
        VideoView previewVideoView;
        TextView nameTv;
        ProgressBar progressBar;
        TextView durationTv;

        public ViewHolder(View convertView) {
            densityIv = (ImageView) convertView.findViewById(R.id.density_indicator_iv);
            thumbnailIv = (ImageView) convertView.findViewById(R.id.thumbnail_iv);
            nameTv = (TextView) convertView.findViewById(R.id.name_tv);
            progressBar = (ProgressBar) convertView.findViewById(R.id.progress);
            durationTv = (TextView) convertView.findViewById(R.id.duration_tv);
        }
    }

    private int getResourceByDensity(VideoItem item) {
        if (item == null) {
            return -1;
        }

        int width = item.getWidth();
        int height = item.getHeight();

        VideoItem.Density density = VideoItem.Density.checkDensity(width, height);
        if (density == VideoItem.Density.UD) {
            return R.drawable.density_xxdpi;
        } else if (density == VideoItem.Density.HD) {
            return R.drawable.density_xdpi;
        } else if (density == VideoItem.Density.ND) {
            return R.drawable.density_hdpi;
        } else {
            return -1;
        }
    }

    private int getVideoNotFoundSubtitleByDeviceType(int deviceType) {
        if (deviceType == DeviceItem.DeviceType.HHD.ordinal()) {
            return R.string.tips_not_found_hdd_subtitle;
        } else if (deviceType == DeviceItem.DeviceType.USB.ordinal()) {
            return R.string.tips_not_found_usb_subtitle;
        } else {
            return R.string.tips_not_found_error_subtitle;
        }
    }
}
