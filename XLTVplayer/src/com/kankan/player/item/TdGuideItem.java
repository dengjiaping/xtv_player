package com.kankan.player.item;

import java.io.Serializable;

/**
 * Created by wangyong on 14-5-6.
 */
public class TdGuideItem implements Serializable {

    public static enum GuideItemType{
        OTHER,WEB_BIND,MOBILE_BIND,WEB_DOWN,MOBILE_DOWN
    }

    public TdGuideItem(int id, GuideItemType type){
          this.resourceId = id;
          this.type = type;
    }

    private GuideItemType type;

    private int resourceId;

    private String firstLine;
    private String secondLine;

    private boolean needRewrite;
    private boolean hasNext;

    private String prefix;
    private String suffix;


    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public String getFirstLine() {
        return firstLine;
    }

    public void setFirstLine(String firstLine) {
        this.firstLine = firstLine;
    }

    public String getSecondLine() {
        return secondLine;
    }

    public void setSecondLine(String secondLine) {
        this.secondLine = secondLine;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public void setNeedRewrite(boolean needRewrite) {
        this.needRewrite = needRewrite;
    }

    public boolean isNeedRewrite() {
        return needRewrite;
    }

    public GuideItemType getType() {
        return type;
    }

    public void setType(GuideItemType type) {
        this.type = type;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public boolean isHasNext() {
        return hasNext;
    }
}
