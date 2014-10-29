package com.kankan.player.item;

import java.io.Serializable;

public class VideoItem implements Serializable {
    public static enum Density {
        NONE("none"), ND("Normal Density"), HD("High Density"), UD("Ultimate Density");

        private String description;

        Density(String description) {
            this.description = description;
        }

        /**
         * 根据视频的尺寸判断清晰度
         *
         * @param width
         * @param height
         * @return
         */
        public static Density checkDensity(int width, int height) {
            if (width >= 1920 || height >= 1080) {
                return UD;
            } else if (width >= 1280 || height >= 720) {
                return HD;
            } else if (width >= 1024 || height >= 500) {
                return ND;
            } else {
                return NONE;
            }
        }
    }

    private String filePath;

    private String fileName;

    private int progress;

    private int duration;

    private int width;

    private int height;

    private String thumbnailPath;

    private String deviceName;

    private int deviceType = -1;

    private int displayMode;
    private int audioMode;

    private boolean exists = true;

    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public VideoItem() {

    }

    public VideoItem(String filePath, String fileName, int progress, int duration, int width,
                     int height, String thumbnailPath, String deviceName, int deviceType) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.progress = progress;
        this.duration = duration;
        this.width = width;
        this.height = height;
        this.thumbnailPath = thumbnailPath;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
    }


    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public int getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    public int getDisplayMode() {
        return displayMode;
    }

    public void setDisplayMode(int displayMode) {
        this.displayMode = displayMode;
    }

    public int getAudioMode() {
        return audioMode;
    }

    public void setAudioMode(int audioMode) {
        this.audioMode = audioMode;
    }
}
