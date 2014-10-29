package com.xunlei.tv.player.android.dao.generator;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Schema;

public class PlayerDaoGenerator {

    private static final String PACKAGE_NAME = "com.kankan.player.dao.model";

    private static final int VERSION = 12;

    public static void main(String[] args) throws Exception {
        Schema schema = new Schema(VERSION, PACKAGE_NAME);

        generateVideoHistory(schema);
        generateSubtitleTable(schema);
        generateVideoTable(schema);
        generateTDVideo(schema);
        generateFileExploreHistory(schema);

        new DaoGenerator().generateAll(schema, "../../src");
    }

    private static void generateVideoHistory(Schema schema) {
        Entity videoHistory = schema.addEntity("VideoHistory");

        videoHistory.addIdProperty();
        videoHistory.addStringProperty("cid").unique().notNull();
        videoHistory.addStringProperty("filePath").unique().notNull();
        videoHistory.addStringProperty("fileName");
        videoHistory.addStringProperty("subtitlePath");
        videoHistory.addIntProperty("width");
        videoHistory.addIntProperty("height");
        videoHistory.addIntProperty("duration");
        videoHistory.addIntProperty("progress");
        videoHistory.addStringProperty("thumbnailPath");
        videoHistory.addLongProperty("timestamp").notNull();
        videoHistory.addStringProperty("deviceName");
        videoHistory.addIntProperty("deviceType");
        videoHistory.addIntProperty("displayMode");
        videoHistory.addIntProperty("audioMode");
        videoHistory.addIntProperty("subtitleType");
        videoHistory.setHasKeepSections(true);
    }

    private static void generateSubtitleTable(Schema schema){
        Entity subtitle = schema.addEntity("Subtitle");
        subtitle.implementsSerializable();
        subtitle.addIdProperty().primaryKey();
        subtitle.addStringProperty("cid").notNull();
        subtitle.addStringProperty("name");
        subtitle.addStringProperty("language");
        subtitle.addIntProperty("rate");
        subtitle.addStringProperty("display");
        subtitle.addStringProperty("downloadurl");
        subtitle.addStringProperty("localpath");
        subtitle.addIntProperty("svote");
        subtitle.addLongProperty("offset");
        subtitle.addIntProperty("type");
        subtitle.addBooleanProperty("selected");
        subtitle.setHasKeepSections(true);
    }

    private static void generateVideoTable(Schema schema) {
        Entity video = schema.addEntity("video");
        video.addStringProperty("filePath").unique().notNull();
        video.addStringProperty("fileName");
        video.addIntProperty("width");
        video.addIntProperty("height");
        video.addIntProperty("duration");
        video.addIntProperty("progress");
        video.addStringProperty("thumbnailPath");
        video.addLongProperty("scanTime").notNull();
        video.addStringProperty("deviceName");
        video.addIntProperty("deviceType");
        video.setHasKeepSections(true);
    }

    private static void generateTDVideo(Schema schema){
        Entity video = schema.addEntity("TDVideo");
        video.addIdProperty();
        video.addStringProperty("cid");
        video.addStringProperty("filePath").unique().notNull();
        video.addStringProperty("fileName");
        video.addLongProperty("fileSize");
        video.addLongProperty("createTime");
        video.addIntProperty("fileType");
        video.addBooleanProperty("islooked");
        video.setHasKeepSections(true);
    }

    private static void generateFileExploreHistory(Schema schema) {
        Entity fileExploreHistory = schema.addEntity("FileExploreHistory");
        fileExploreHistory.addIdProperty();
        fileExploreHistory.addStringProperty("filePath").notNull();
        fileExploreHistory.addIntProperty("fileCategory");
        fileExploreHistory.addStringProperty("deviceName");
        fileExploreHistory.addIntProperty("deviceType");
        fileExploreHistory.addLongProperty("fileSize");
        fileExploreHistory.addLongProperty("lastModifyTime");
        fileExploreHistory.addStringProperty("devicePath");
        fileExploreHistory.addLongProperty("deviceSize");
        fileExploreHistory.addStringProperty("cid");
        fileExploreHistory.setHasKeepSections(true);
    }

}
