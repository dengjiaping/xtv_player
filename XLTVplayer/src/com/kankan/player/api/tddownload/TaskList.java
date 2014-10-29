package com.kankan.player.api.tddownload;

import java.util.List;

/**
 * Created by zhangdi on 14-4-1.
 */
public class TaskList {

    public int result;

    public int pageCount;

    public List<Task> tasks;

    public static class Task {

        public int taskId;

        public int stat;

        public int type;

        public String fileName;

        public String filePath;

        public String fileSize;

        public String downloadDataSize;

        public int startTime;

        public int finishedTime;

        public int failCode;

        public int dlSpeed;

        public int ulSpeed;

        public String url;

    }
}
