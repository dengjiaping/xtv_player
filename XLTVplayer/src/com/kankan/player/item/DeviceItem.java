package com.kankan.player.item;

import java.io.Serializable;

public class DeviceItem implements Serializable {

    public static enum DeviceType {
        // 注意这个排序反应了首页的排序情况，相同的type排序随缘（例外的是usb和hhd不按次顺序，这个已经反应在了compare方法中）
        TD_DOWNLOAD, XL_ROUTER_TDDOWNLOAD, XL_ROUTER, USB, HHD, VIDEO_LIST, HISTORY, EXTERNAL;

        public static boolean compare(DeviceType m, DeviceType n) {
            if ((m == USB && n == HHD) || (m == HHD && n == USB)) {
                return false;
            }

            return m.ordinal() < n.ordinal();
        }
    }

    private DeviceType type;

    private String name;

    private String path;

    // 设备的存储空间大小
    private long size;

    private String description;

    public DeviceItem() {

    }

    public DeviceItem(String name, DeviceType type, String path, long size, String description) {
        this.name = name;
        this.type = type;
        this.path = path;
        this.size = size;
        this.description = description;
    }

    public DeviceType getType() {
        return type;
    }

    public void setType(DeviceType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        DeviceItem item = (DeviceItem) o;

        if (size != item.size) return false;
        if (!description.equals(item.description)) return false;
        if (!name.equals(item.name)) return false;
        if (!path.equals(item.path)) return false;
        if (type != item.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + name.hashCode();
        result = 31 * result + path.hashCode();
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + description.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DeviceItem{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", size=" + size +
                ", description='" + description + '\'' +
                '}';
    }
}
