package com.kankan.player.dao.model;

// THIS CODE IS GENERATED BY greenDAO, EDIT ONLY INSIDE THE "KEEP"-SECTIONS

// KEEP INCLUDES - put your custom includes here
// KEEP INCLUDES END
/**
 * Entity mapped to table FILE_EXPLORE_HISTORY.
 */
public class FileExploreHistory {

    private Long id;
    /** Not-null value. */
    private String filePath;
    private Integer fileCategory;
    private String deviceName;
    private Integer deviceType;
    private Long fileSize;
    private Long lastModifyTime;
    private String devicePath;
    private Long deviceSize;
    private String cid;

    // KEEP FIELDS - put your custom fields here
    // KEEP FIELDS END

    public FileExploreHistory() {
    }

    public FileExploreHistory(Long id) {
        this.id = id;
    }

    public FileExploreHistory(Long id, String filePath, Integer fileCategory, String deviceName, Integer deviceType, Long fileSize, Long lastModifyTime, String devicePath, Long deviceSize, String cid) {
        this.id = id;
        this.filePath = filePath;
        this.fileCategory = fileCategory;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.fileSize = fileSize;
        this.lastModifyTime = lastModifyTime;
        this.devicePath = devicePath;
        this.deviceSize = deviceSize;
        this.cid = cid;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /** Not-null value. */
    public String getFilePath() {
        return filePath;
    }

    /** Not-null value; ensure this value is available before it is saved to the database. */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Integer getFileCategory() {
        return fileCategory;
    }

    public void setFileCategory(Integer fileCategory) {
        this.fileCategory = fileCategory;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public Integer getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(Integer deviceType) {
        this.deviceType = deviceType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Long getLastModifyTime() {
        return lastModifyTime;
    }

    public void setLastModifyTime(Long lastModifyTime) {
        this.lastModifyTime = lastModifyTime;
    }

    public String getDevicePath() {
        return devicePath;
    }

    public void setDevicePath(String devicePath) {
        this.devicePath = devicePath;
    }

    public Long getDeviceSize() {
        return deviceSize;
    }

    public void setDeviceSize(Long deviceSize) {
        this.deviceSize = deviceSize;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    // KEEP METHODS - put your custom methods here
    // KEEP METHODS END

}