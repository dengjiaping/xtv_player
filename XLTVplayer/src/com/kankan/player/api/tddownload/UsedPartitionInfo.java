package com.kankan.player.api.tddownload;

import java.util.List;

/**
 * Created by zhangdi on 14-4-3.
 */
public class UsedPartitionInfo {

    public int result;

    public List<Partition> partitions;

    public static class Partition {
        public String drive;

        public String mountPath;

        @Override
        public String toString() {
            return "Partition{" +
                    "drive='" + drive + '\'' +
                    ", mountPath='" + mountPath + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "UsedPartitionInfo{" +
                "result=" + result +
                ", partitions=" + partitions +
                '}';
    }
}
