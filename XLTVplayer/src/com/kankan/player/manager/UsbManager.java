package com.kankan.player.manager;

import android.text.TextUtils;
import android.util.Log;
import com.kankan.player.app.AppConfig;
import com.kankan.player.item.DeviceItem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangdi on 14-3-27.
 * 读取/proc/mounts和/proc/partitions文件，获得挂载的Usb设备
 */
public class UsbManager {

    private static class Mount {
        public String device;

        public String path;

        @Override
        public String toString() {
            return "Mount{" +
                    "device='" + device + '\'' +
                    ", path='" + path + '\'' +
                    '}';
        }
    }

    private static class Partition {
        public int major;

        public int minor;

        public long blocks;

        public String name;

        @Override
        public String toString() {
            return "Partition{" +
                    "major=" + major +
                    ", minor=" + minor +
                    ", blocks=" + blocks +
                    ", name='" + name + '\'' +
                    '}';
        }
    }

    public static List<DeviceItem> getUsbDeviceList() {
        List<DeviceItem> deviceList = new ArrayList<DeviceItem>();

        List<Partition> partitionList = getPartitions();
        List<Mount> mountList = getMounts();

        if (partitionList != null && mountList != null) {
            for (Partition partition : partitionList) {
                for (Mount mount : mountList) {
                    if (isMatch(partition, mount)) {
                        DeviceItem.DeviceType type;
                        if (partition.blocks > 60 * 1000 * 1000) {
                            type = DeviceItem.DeviceType.HHD;
                        } else {
                            type = DeviceItem.DeviceType.USB;
                        }
                        DeviceItem device = new DeviceItem(partition.name, type, mount.path, partition.blocks * 1000,
                                String.format("%.1fG\b|\bUSB", partition.blocks / 1000000f)); 
                        LOGD(device.toString());
                        deviceList.add(device);
                        break;
                    }
                }
            }
        }

        return deviceList;
    }

    private static boolean isMatch(Partition partition, Mount mount) {
        if (partition != null && mount != null) {
            if (!TextUtils.isEmpty(mount.path) && mount.device != null && mount.device.endsWith(partition.major + ":" + partition.minor)) {
                return true;
            }
        }
        return false;
    }

    private static List<Mount> getMounts() {
        List<Mount> mountList = new ArrayList<Mount>();

        File mountsFile = new File("/proc/mounts");
        if (mountsFile.exists() && mountsFile.isFile() && mountsFile.canRead()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(mountsFile));
                String line;
                String[] array = new String[2];
                int i;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("/dev/block/vold/")) {
                        String[] ss = line.split(" ");
                        if (ss != null) {
                            i = 0;
                            for (String s : ss) {
                                if (!TextUtils.isEmpty(s)) {
                                    array[i++] = s;
                                }
                                if (i == 2) {
                                    break;
                                }
                            }
                            if (i < 2) {
                                continue;
                            }

                            Mount mount = new Mount();
                            mount.device = array[0];
                            mount.path = array[1];
                            LOGD(mount.toString());
                            mountList.add(mount);
                        }
                    }
                }
            } catch (IOException e) {
                AppConfig.LOGD("[[UsbManager]] getMounts IOException:" + e.getMessage());
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
        return mountList;
    }

    public static List<Partition> getPartitions() {
        List<Partition> partitionList = new ArrayList<Partition>();

        File file = new File("/proc/partitions");
        if (file.exists() && file.isFile() && file.canRead()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                String line;
                String[] array = new String[4];
                int i;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("sd")) {
                        String[] ss = line.split(" ");
                        if (ss != null) {
                            i = 0;
                            for (String s : ss) {
                                if (!TextUtils.isEmpty(s)) {
                                    array[i++] = s;
                                }
                                if (i == 4) {
                                    break;
                                }
                            }
                            if (i < 4) {
                                continue;
                            }

                            try {
                                Partition partition = new Partition();
                                partition.major = Integer.parseInt(array[0]);
                                partition.minor = Integer.parseInt(array[1]);
                                partition.blocks = Long.parseLong(array[2]);
                                partition.name = array[3];
                                LOGD(partition.toString());
                                partitionList.add(partition);
                            } catch (NumberFormatException e) {
                                AppConfig.LOGD("[[UsgManager]] getPartions NumberFormatException:" + e.getMessage());
                            }
                        }
                    }
                }
            } catch (IOException e) {
                AppConfig.LOGD("[[UsgManager]] getPartions IOException:" + e.getMessage());
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
        return partitionList;
    }

    private static void LOGD(String msg) {
        if (AppConfig.DEBUG && !TextUtils.isEmpty(msg)) {
            Log.d(UsbManager.class.getSimpleName(), msg);
        }
    }

}
