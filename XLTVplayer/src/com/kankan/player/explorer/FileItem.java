package com.kankan.player.explorer;

import java.io.Serializable;

/**
 * Created by zhangdi on 14-3-27.
 */
public class FileItem implements Serializable {

    public FileCategory category;

    public String fileName;

    public String filePath;

    public long fileSize;

    public boolean canRead;

    public boolean canWrite;

    public boolean isHidden;

    public boolean isNew;

    public long lastModifyTime;

    //用于标记远程任务下载任务状态
    public int fileStatus;

    public String cid;

    @Override
    public String toString() {
        return "FileItem{" +
                "category=" + category +
                ", fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileSize=" + fileSize +
                ", canRead=" + canRead +
                ", canWrite=" + canWrite +
                ", isHidden=" + isHidden +
                ", isNew=" + isNew +
                ", lastModifyTime=" + lastModifyTime +
                ", cid=" + cid +
                '}';
    }
}
