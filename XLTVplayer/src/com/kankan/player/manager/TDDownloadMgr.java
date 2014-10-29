package com.kankan.player.manager;

import android.content.Context;
import com.kankan.player.api.tddownload.SysInfo;
import com.kankan.player.explorer.FileItem;

import java.util.List;

/**
 * Created by wangyong on 14-7-9.
 */
public abstract class TDDownloadMgr {

    public abstract void init(Context context);

    public abstract SysInfo getSysInfo();

    public abstract boolean isSupportTD();

    public abstract List<FileItem> getTDDownloadList(int complete);

    public abstract void setDownloadedFileItems(List<FileItem> list, Context context);

    public abstract void setDownloadingFilesNum(int num);

    public abstract int getDownloadingFilesNum();

    public abstract List<FileItem> getFileItems();

    public abstract int getTDownloadNewFilesNum();

    public abstract int getTDFilesCounts();

    public abstract void getSublistFileItems(final FileItem file, final Context context);
}
