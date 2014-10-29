package com.kankan.player.event;

import com.kankan.player.dao.model.Subtitle;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangyong on 14-3-31.
 */
public class OnlineSubtitleEvent extends AbsEventBase {

    public List<Subtitle> list;

    public List<Subtitle> localSubtitles = new ArrayList<Subtitle>();
    public List<Subtitle> onlineSubtitles = new ArrayList<Subtitle>();

}
