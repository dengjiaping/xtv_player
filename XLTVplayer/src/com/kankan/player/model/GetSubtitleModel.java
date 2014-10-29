package com.kankan.player.model;

import android.content.Context;
import android.text.TextUtils;
import com.kankan.player.api.rest.subtitle.GetSubTitleRequest;
import com.kankan.player.api.rest.subtitle.GetSubTitleResponse;
import com.kankan.player.app.AppConfig;
import com.kankan.player.dao.model.Subtitle;
import com.kankan.player.event.LocalSubttileEvent;
import com.kankan.player.event.OnlineSubtitleEvent;
import com.kankan.player.subtitle.*;
import com.plugin.common.utils.CustomThreadPool;
import com.plugin.common.utils.SingleInstanceBase;
import com.plugin.common.utils.files.FileDownloader;
import com.plugin.common.utils.files.FileUtil;
import com.plugin.internet.InternetUtils;
import com.plugin.internet.core.NetWorkException;
import de.greenrobot.event.EventBus;
import info.monitorenter.cpdetector.io.*;

import java.io.*;
import java.util.*;

/**
 * Created by wangyong on 14-3-31.
 */
public class GetSubtitleModel extends SingleInstanceBase {
    private static final String KEY_LANGUAGE_1 = "简体";
    private static final String KEY_LANGUAGE_2 = "英语";
    private static final String KEY_LANGUAGE_3 = "简体&英语";
    private static final String KEY_LANGUAGE_4 = "繁体";
    private static final String KEY_LANGUAGE_5 = "繁体&英语";
    private static final String KEY_LANGUAGE_6 = "繁体&eng";
    private static final String KEY_LANGUAGE_7 = "chs&eng";
    private static final String KEY_LANGUAGE_8 = "cht&eng";
    private static final String KEY_LANGUAGE_9 = "cht";
    private static final String KEY_LANGUAGE_10 = "eng";
    private static final String KEY_LANGUAGE_11 = "简体&韩语";
    private static final String KEY_LANGUAGE_12 = "简体&日语";

    public static final int MAX_ONLINE_SUBTITLE_COUNT = 5;

    private static final Map<String, String> SUBTITLE_LANGUAGE_DESCRIPTION = new HashMap<String, String>();
    // 支持的字幕文件类型
    private static final Set<String> SUBTITLE_TYPES_SUPPORTED = new HashSet<String>();

    private Context mContext;

    private FileDownloader mFileDownloader;

    static {
        // 字幕语言对应的名称
        SUBTITLE_LANGUAGE_DESCRIPTION.put(KEY_LANGUAGE_1, "简体中字");
        SUBTITLE_LANGUAGE_DESCRIPTION.put(KEY_LANGUAGE_2, "英文字幕");
        SUBTITLE_LANGUAGE_DESCRIPTION.put(KEY_LANGUAGE_3, "中英双字");
        SUBTITLE_LANGUAGE_DESCRIPTION.put(KEY_LANGUAGE_4, "繁体中字");
        SUBTITLE_LANGUAGE_DESCRIPTION.put(KEY_LANGUAGE_5, "繁英双字");
        SUBTITLE_LANGUAGE_DESCRIPTION.put(KEY_LANGUAGE_6, "繁英双字");
        SUBTITLE_LANGUAGE_DESCRIPTION.put(KEY_LANGUAGE_7, "中英双字");
        SUBTITLE_LANGUAGE_DESCRIPTION.put(KEY_LANGUAGE_8, "繁英双字");
        SUBTITLE_LANGUAGE_DESCRIPTION.put(KEY_LANGUAGE_9, "繁体中字");
        SUBTITLE_LANGUAGE_DESCRIPTION.put(KEY_LANGUAGE_10, "简体中字");
        SUBTITLE_LANGUAGE_DESCRIPTION.put(KEY_LANGUAGE_11, "中韩双字");
        SUBTITLE_LANGUAGE_DESCRIPTION.put(KEY_LANGUAGE_12, "中日双字");

        SUBTITLE_TYPES_SUPPORTED.add("srt");
        SUBTITLE_TYPES_SUPPORTED.add("stl");
        SUBTITLE_TYPES_SUPPORTED.add("scc");
        SUBTITLE_TYPES_SUPPORTED.add("xml");
        SUBTITLE_TYPES_SUPPORTED.add("ass");
    }

    public class VideoSubtitle {
        // 第一次加载的时候是否自动加载中英双字（如果中英双字权重排前5那么加载，如果不排前5则不加载）
        public boolean needLoadZhEn = false;
        // 是否需要自动加载字幕
        public boolean needAutoload = true;
        public Subtitle innerSubtitle;
        public List<Subtitle> localSubtitles = new ArrayList<Subtitle>();
        public List<Subtitle> onlineSubtitles = new ArrayList<Subtitle>();
    }

    private VideoSubtitle mSubtitle = new VideoSubtitle();

    public boolean needLoadZhEn() {
        if (mSubtitle != null) {
            return mSubtitle.needLoadZhEn;
        }
        return false;
    }

    public boolean needAutoloadSubtitle() {
        if (mSubtitle != null) {
            return mSubtitle.needAutoload;
        }
        return true;
    }

    public List<Subtitle> getDisplaySubtitleList() {
        List<Subtitle> sbs = new ArrayList<Subtitle>();
        sbs.add(makeCancelAllSubtitleItem());
        if (mSubtitle != null) {
            if (mSubtitle.innerSubtitle != null) {
                sbs.add(mSubtitle.innerSubtitle);
            }
            if (mSubtitle.localSubtitles.size() > 0) {
                sbs.addAll(mSubtitle.localSubtitles);
            }
            if (mSubtitle.onlineSubtitles.size() > 0) {
                sbs.addAll(mSubtitle.onlineSubtitles);
            }
        }

        AppConfig.LOGD("[[GetSubtitleModel]] getDisplaySubtitleList size = " + sbs.size());
        for (int i = 0; i < sbs.size(); i++) {
            AppConfig.LOGD("\tsubtitle: " + sbs.get(i).getName());
        }

        return sbs;
    }

    public void clearSubtitles() {
        if (mSubtitle != null) {
            mSubtitle.needLoadZhEn = false;
            mSubtitle.needAutoload = true;
            mSubtitle.innerSubtitle = null;
            mSubtitle.localSubtitles.clear();
            mSubtitle.onlineSubtitles.clear();
        }
    }

    private Subtitle makeCancelAllSubtitleItem() {
        Subtitle subtitle = new Subtitle();
        subtitle.setType(SubtitleType.NONE.ordinal());
        subtitle.setName("取消其他字幕");
        return subtitle;
    }

    private GetSubtitleModel() {

    }

    @Override
    protected void init(Context context) {
        this.mContext = context.getApplicationContext();
        this.mFileDownloader = FileDownloader.getInstance(mContext);
    }

    public void getSubTitleList(final String cid, final String moviePath, final int duration) {
        AppConfig.LOGD("[[GetSubtitleModel]] getSubTitleList cid=" + cid + ", duration=" + duration + ", moviePath=" + moviePath);
        CustomThreadPool.getInstance().excute(new CustomThreadPool.TaskWrapper(new Runnable() {
            @Override
            public void run() {
                String movieFileName = FileUtil.getNameFromFilepath(moviePath);
                GetSubTitleRequest request = new GetSubTitleRequest(cid, duration, movieFileName);

                ArrayList<Subtitle> onlineList = null;
                try {
                    GetSubTitleResponse response = InternetUtils.request(mContext, request);
                    if (response != null) {
                        AppConfig.LOGD("[[GetSubtitleModel]] getSubTitleList has network subtitles, autoload = " + response.autoload);
                        onlineList = convertOnlineSubtitle2Subtitle(response.sublist);
                        mSubtitle.needAutoload = response.autoload == 0 ? false : true;
                    }
                } catch (NetWorkException e) {
                    AppConfig.LOGD("[[GetSubtitleModel]] getSubTitleList NetWorkException:" + e.getMessage());
                }

                setOnlineSubtitles(onlineList);

                OnlineSubtitleEvent onlineEvent = new OnlineSubtitleEvent();
                onlineEvent.onlineSubtitles.addAll(mSubtitle.onlineSubtitles);

                List<Subtitle> localList = getLocalSubtitles(cid, moviePath);
                if (localList != null && localList.size() > 0) {
                    onlineEvent.localSubtitles.addAll(localList);
                    mSubtitle.localSubtitles.addAll(localList);
                }

                EventBus.getDefault().post(onlineEvent);
            }
        }));
    }

    //目前的策略是显示5条网络字幕，按svote值从高到低选择，尽量保证有一个中英双字字幕
    private void setOnlineSubtitles(List<Subtitle> onlineSubtitles) {
        if (onlineSubtitles == null || onlineSubtitles.size() == 0) {
            return;
        }

        int size = onlineSubtitles.size();
        for (int i = 0; i < size && i < MAX_ONLINE_SUBTITLE_COUNT; i++) {
            mSubtitle.onlineSubtitles.add(onlineSubtitles.get(i));
        }

        if (size > MAX_ONLINE_SUBTITLE_COUNT) {
            //如果前MAX_ONLINE_SUBTITLE_COUNT个都没有中英双字，那么搜索字幕中的中英双字替换成第MAX_ONLINE_SUBTITLE_COUNT个
            boolean hasZhEnLanguage = false;
            for (int i = 0; i < MAX_ONLINE_SUBTITLE_COUNT; i++) {
                if (isSubtitleZhEn(onlineSubtitles.get(i))) {
                    hasZhEnLanguage = true;
                    mSubtitle.needLoadZhEn = true;
                    break;
                }
            }

            if (!hasZhEnLanguage) {
                for (int i = MAX_ONLINE_SUBTITLE_COUNT; i < size; i++) {
                    if (isSubtitleZhEn(onlineSubtitles.get(i))) {
                        mSubtitle.onlineSubtitles.set(MAX_ONLINE_SUBTITLE_COUNT - 1, onlineSubtitles.get(i));
                        break;
                    }
                }
            }
        }

        if (AppConfig.DEBUG) {
            for (int i = 0; i < size && i < MAX_ONLINE_SUBTITLE_COUNT; i++) {
                AppConfig.LOGD("[[GetSubtitleModel]] setOnlineSubtitles title" + i + ": " + mSubtitle.onlineSubtitles.get(i).getLanguage() + ", " + mSubtitle.onlineSubtitles.get(i).getDownloadurl());
            }
        }
    }

    public void downloadSubTitle(String url, String videoPath) {
        AppConfig.LOGD("[[GetSubtitleModel]] downloadSubTitle url=" + url + ", videoPath=" + videoPath);
        mFileDownloader.postRequest(new FileDownloader.DownloadRequest(url, url.substring(url.length() - 3)), new SubtitleDownloadListener(mContext));
    }

    private class SubtitleDownloadListener implements FileDownloader.DownloadListener {
        private Context mContext;

        public SubtitleDownloadListener(Context context) {
            this.mContext = context;
        }

        @Override
        public void onDownloadProcess(int fileSize, int downloadSize) {

        }

        @Override
        public void onDownloadFinished(int status, final Object response) {
            if (status == FileDownloader.DOWNLOAD_SUCCESS) {
                log("download finished");
                FileDownloader.DownloadResponse downloadResponse = (FileDownloader.DownloadResponse) response;
                final String path = downloadResponse.getRawLocalPath();
                CustomThreadPool.getInstance().excute(new CustomThreadPool.TaskWrapper(new Runnable() {
                    @Override
                    public void run() {
                        TimedTextObject subtitleObj = loadSubtitleFile(path);
                        log("load subtitle result is: " + subtitleObj);
                        LocalSubttileEvent event = new LocalSubttileEvent();
                        event.obj = subtitleObj;
                        event.downloadUrl = ((FileDownloader.DownloadResponse) response).getDownloadUrl();
                        EventBus.getDefault().post(event);
                    }
                }));
            } else {
                log("download error: status=" + status);
                LocalSubttileEvent event = new LocalSubttileEvent();
                event.obj = null;
                EventBus.getDefault().post(event);
            }
        }
    }

    public void loadSubtitle(final String filePath) {
        CustomThreadPool.getInstance().excute(new CustomThreadPool.TaskWrapper(new Runnable() {
            @Override
            public void run() {
                TimedTextObject subtitleObj = loadSubtitleFile(filePath);
                LocalSubttileEvent event = new LocalSubttileEvent();
                event.obj = subtitleObj;
                EventBus.getDefault().post(event);
            }
        }));
    }

    private List<String> getLocalSubtitleList(final String moviePath) {
        if (!TextUtils.isEmpty(moviePath)) {
            File movieFile = new File(moviePath);
            File parentFile = movieFile.getParentFile();
            String movieName = movieFile.getName();
            File[] childFiles = parentFile.listFiles();
            List<String> subtitles = new ArrayList<String>();
            if (childFiles != null) {
                for (File childFile : childFiles) {
                    AppConfig.LOGD("[[GetSubtitleModel]] getLocalSubtitleList childFile=" + childFile.getAbsolutePath());
                    if (isSubtitleSupported(childFile.getName()) && childFile.getName().toLowerCase().startsWith(FileUtil.getNameFromFilename(movieName).toLowerCase())) {
                        subtitles.add(childFile.getAbsolutePath());
                    }
                }
            }

            return subtitles;

        }

        return null;
    }

    private TimedTextObject loadSubtitleFile(String path) {
        TimedTextObject subtitleobj = null;
        File subtitlefile = new File(path);
        if (subtitlefile.exists()) {
            InputStream stream = null;
            String fileEncode = getFileEncoding(path);
            try {
                stream = new FileInputStream(new File(path));

                String inputformat = path.substring(path.length() - 3);
                TimedTextFileFormat formatobj = createFormatObj(inputformat);

                if (formatobj != null) {
                    subtitleobj = formatobj.parseFile(subtitlefile.getName(), stream, fileEncode);
                }
            } catch (FileNotFoundException e) {
                AppConfig.LOGD("[[GetSubtitleModel]] loadSubtitleFile FileNotFoundException: " + e.getMessage());
            } catch (IOException e) {
                AppConfig.LOGD("[[GetSubtitleModel]] loadSubtitleFile IOException: " + e.getMessage());
            } catch (FatalParsingException e) {
                AppConfig.LOGD("[[GetSubtitleModel]] loadSubtitleFile FatalParsingException: " + e.getMessage());
            }
        }

        return subtitleobj;
    }

    public void setInnerSubtitle(String cid, boolean hasInnerSubtitle) {
        if (hasInnerSubtitle) {
            Subtitle subtitle = new Subtitle();
            subtitle.setCid(cid);
            subtitle.setName("内置字幕");
            subtitle.setLanguage("");
            subtitle.setRate(0);
            subtitle.setDisplay("");
            subtitle.setDownloadurl("");
            subtitle.setLocalpath("");
            subtitle.setSvote(0);
            subtitle.setOffset(0l);
            subtitle.setType(SubtitleType.INNER.ordinal());
            subtitle.setSelected(false);

            mSubtitle.innerSubtitle = subtitle;
        }

    }


    private List<Subtitle> getLocalSubtitles(String cid, String videoPath) {
        ArrayList<Subtitle> list = new ArrayList<Subtitle>();

        ArrayList<String> localSubtitles = (ArrayList<String>) getLocalSubtitleList(videoPath);
        for (String str : localSubtitles) {
            File file = new File(str);
            if (file.exists() && file.canRead()) {
                Subtitle subtitle = new Subtitle();
                subtitle.setCid(cid);
                subtitle.setName(file.getName());
                subtitle.setLanguage("");
                subtitle.setRate(0);
                subtitle.setDisplay("");
                subtitle.setDownloadurl("");
                subtitle.setLocalpath(file.getAbsolutePath());
                subtitle.setSvote(0);
                subtitle.setOffset(0l);
                subtitle.setType(SubtitleType.LOCAL.ordinal());
                subtitle.setSelected(false);

                list.add(subtitle);
            }
        }

        // 命名
        if (list.size() == 1) {
            list.get(0).setName("本地字幕");
        } else {
            for (int i = 0; i < list.size(); i++) {
                list.get(i).setName("本地字幕" + (i + 1));
            }
        }

        return list;
    }

    private ArrayList<Subtitle> convertOnlineSubtitle2Subtitle(List<GetSubTitleResponse.OnlineSubtitle> sublist) {
        ArrayList<Subtitle> subtitles = new ArrayList<Subtitle>();
        if (sublist != null) {
            // 排序
            Collections.sort(sublist);

            for (GetSubTitleResponse.OnlineSubtitle onlineSubtitle : sublist) {
                AppConfig.LOGD("[[GetSubtitleModel]] onlineSubtitle language = " + (onlineSubtitle.language == null ? "null" : onlineSubtitle.language));
                // 过滤掉其他的未知语言
                if (onlineSubtitle.language == null || !SUBTITLE_LANGUAGE_DESCRIPTION.containsKey(onlineSubtitle.language)) {
                    continue;
                }
                // 过滤不支持的字幕文件
                if (!isSubtitleSupported(onlineSubtitle.surl)) {
                    continue;
                }

                Subtitle subtitle = new Subtitle();
                subtitle.setCid(onlineSubtitle.scid);
                subtitle.setName(onlineSubtitle.sname);
                subtitle.setLanguage(onlineSubtitle.language);
                subtitle.setRate(onlineSubtitle.rate);
                subtitle.setDisplay(onlineSubtitle.display);
                subtitle.setDownloadurl(onlineSubtitle.surl);
                subtitle.setLocalpath("");
                subtitle.setSvote(onlineSubtitle.svote);
                subtitle.setOffset(onlineSubtitle.roffset);
                subtitle.setType(SubtitleType.ONLINE.ordinal());
                subtitle.setSelected(false);

                subtitles.add(subtitle);
            }
        }
        AppConfig.LOGD("[[GetSubtitleModel]] convertOnlineSubtitle2Subtitle size=" + subtitles.size() + ", original size=" + sublist.size());
        return subtitles;
    }

    // 由于CPDetector检测的时间太长了，这里先进行简单的判断，如果判断不出来那么用cpdetector来检测
    public String getFileEncoding(String path) {
        String code = null;
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(path);
            byte[] head = new byte[3];
            inputStream.read(head);
            if (head[0] == -1 && head[1] == -2) {
                code = "UTF-16";
            } else if (head[0] == -2 && head[1] == -1) {
                code = "Unicode";
            } else if (head[0] == -17 && head[1] == -69 && head[2] == -65) {
                code = "UTF-8";
            }

            inputStream.close();
        } catch (FileNotFoundException e) {
            AppConfig.LOGD("[[GetSubtitleModel]] getFileEncoding error: " + e.getMessage());
        } catch (IOException e) {
            AppConfig.LOGD("[[GetSubtitleModel]] getFileEncoding error: " + e.getMessage());
        }

        if (code == null) {
            CodepageDetectorProxy detector = CodepageDetectorProxy.getInstance();
            detector.add(new ParsingDetector(false));
            detector.add(JChardetFacade.getInstance());// 用到antlr.jar、chardet.jar
            // ASCIIDetector用于ASCII编码测定
            detector.add(ASCIIDetector.getInstance());
            // UnicodeDetector用于Unicode家族编码的测定
            detector.add(UnicodeDetector.getInstance());
            java.nio.charset.Charset charset = null;
            File f = new File(path);
            try {
                charset = detector.detectCodepage(f.toURI().toURL());
            } catch (Exception e) {
                AppConfig.LOGD("[[GetSubtitleModel]] getFileEncoding cpdetector error: " + e.getMessage());
            }
            if (charset != null) {
                code = charset.name();
                AppConfig.LOGD("[[GetSubtitleModel]] getFileEncoding use cpdetector and encoding is " + code);
            } else {
                code = "Unicode";
                AppConfig.LOGD("[[GetSubtitleModel]] getFileEncoding use cpdetector and encoding is default Unicode");
            }
        }

        return code;
    }

    public TimedTextFileFormat createFormatObj(String inputFormat) {
        TimedTextFileFormat ttff = null;
        try {
            if ("SRT".equalsIgnoreCase(inputFormat)) {
                ttff = new FormatSRT();
            } else if ("STL".equalsIgnoreCase(inputFormat)) {
                ttff = new FormatSTL();
            } else if ("SCC".equalsIgnoreCase(inputFormat)) {
                ttff = new FormatSCC();
            } else if ("XML".equalsIgnoreCase(inputFormat)) {
                ttff = new FormatTTML();
            } else if ("ASS".equalsIgnoreCase(inputFormat)) {
                ttff = new FormatASS();
            } else {
                throw new Exception("Unrecognized input format: " + inputFormat + " only [SRT,STL,SCC,XML,ASS] are possible");
            }
        } catch (Exception e) {
            AppConfig.LOGD("[[GetSubtitleModel]] createFomatObj " + e.getMessage());
        }

        return ttff;
    }

    private void log(String msg) {
        AppConfig.LOGD("[[GetSubtitleModel]] " + msg);
    }

    public String getLanguageDescription(String key) {
        return SUBTITLE_LANGUAGE_DESCRIPTION.get(key);
    }

    // 是否为中英双字
    public boolean isSubtitleZhEn(Subtitle subtitle) {
        String language = subtitle.getLanguage();
        if (TextUtils.isEmpty(language)) {
            return false;
        }

        return KEY_LANGUAGE_3.equals(language) || KEY_LANGUAGE_7.equals(language);
    }

    private boolean isSubtitleSupported(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        int index = url.lastIndexOf(".");
        String ext = url.toLowerCase();
        if (index != -1) {
            ext = url.substring(index + 1).toLowerCase();
        }

        if (!SUBTITLE_TYPES_SUPPORTED.contains(ext)) {
            return false;
        }

        return true;
    }
}
