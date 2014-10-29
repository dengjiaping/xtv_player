package com.kankan.player.manager;

import android.content.Context;
import com.kankan.player.app.Constants;
import com.kankan.player.dao.model.DaoSession;
import com.kankan.player.dao.model.VideoHistory;
import com.kankan.player.dao.model.VideoHistoryDao;
import com.kankan.player.event.VideoHistoryEvent;
import com.kankan.player.item.VideoItem;
import com.kankan.player.util.DaoUtils;
import com.kankan.player.util.DateTimeFormatter;
import de.greenrobot.event.EventBus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VideoHistoryManager {

    private final Context mContext;
    private VideoHistoryDao mDao;

    public VideoHistoryManager(Context context) {
        mContext = context;
        DaoSession session = DaoUtils.getDaoSession(context);
        mDao = session.getVideoHistoryDao();
    }

    public void saveHistoryList(List<VideoHistory> videoHistories) {
        mDao.insertOrReplaceInTx(videoHistories);
    }

    public void saveHistory(VideoHistory videoHistory) {
        mDao.insertOrReplace(videoHistory);

        EventBus.getDefault().post(new VideoHistoryEvent());
    }

    public VideoHistory getLatestHistoryVideo() {
        List<VideoHistory> videoHistories = mDao.queryBuilder().orderDesc(VideoHistoryDao.Properties.Timestamp).list();
        if (videoHistories != null && videoHistories.size() != 0) {
            return videoHistories.get(0);
        }

        return null;
    }

    public boolean clearHistory() {
        mDao.deleteAll();
        return true;
    }

    public boolean removeHistory(VideoHistory history) {
        mDao.delete(history);
        return true;
    }

    public boolean removeHistoryInTx(VideoHistory... histories) {
        mDao.deleteInTx(histories);
        return true;
    }

    /**
     * 如果cid有效，则使用cid来查找，如果无效，则cid为路径，使用路径来查找
     *
     * @param cid
     * @return
     */
    public VideoHistory getHistoryByCid(String cid) {
        if (cid == null) {
            return null;
        }

        if (cid.startsWith(Constants.CID_PREFIX)) {
            return mDao.queryBuilder().where(VideoHistoryDao.Properties.Cid.eq(cid)).unique();
        } else {
            return mDao.queryBuilder().where(VideoHistoryDao.Properties.FilePath.eq(cid)).unique();
        }
    }

    public int getHistoryVideoDeviceType(String videoFilePath) {
        VideoHistory history = mDao.queryBuilder().where(VideoHistoryDao.Properties.FilePath.eq(videoFilePath)).unique();
        if (history == null) {
            return -1;
        }

        return history.getDeviceType();
    }

    public int getHistoryProgress(String videoFilePath) {
        VideoHistory history = mDao.queryBuilder().where(VideoHistoryDao.Properties.FilePath.eq(videoFilePath)).unique();
        if (history != null && history.getProgress() != null) {
            return history.getProgress().intValue();
        }

        return 0;
    }

    /**
     * 时间倒叙排列
     *
     * @return
     */
    public List<VideoHistory> getHistoryList() {
        return mDao.queryBuilder().orderDesc(VideoHistoryDao.Properties.Timestamp).build().forCurrentThread().list();
    }

    public List<List<VideoHistory>> getHistoryListList() {
        List<List<VideoHistory>> historyListList = new ArrayList<List<VideoHistory>>();
        List<VideoHistory> videoHistories = getHistoryList();
        if (videoHistories != null) {
            long timestamp = 0;
            List<VideoHistory> videoItems = null;
            for (VideoHistory videoHistory : videoHistories) {
                int days = DateTimeFormatter.daysBetween(timestamp, videoHistory.getTimestamp());
                if (days == 0) {
                    if (videoItems == null) {
                        videoItems = new ArrayList<VideoHistory>();
                    }
                    videoItems.add(videoHistory);
                } else {
                    if (videoItems != null && videoItems.size() > 0) {
                        historyListList.add(videoItems);
                    }
                    timestamp = videoHistory.getTimestamp();
                    videoItems = new ArrayList<VideoHistory>();
                    videoItems.add(videoHistory);
                }
            }
            if (videoItems != null && videoItems.size() > 0) {
                historyListList.add(videoItems);
            }
        }

        return historyListList;
    }

}
