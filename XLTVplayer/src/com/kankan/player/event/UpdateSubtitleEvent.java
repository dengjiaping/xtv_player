package com.kankan.player.event;

public class UpdateSubtitleEvent extends AbsEventBase {

    public int what;
    public int type;

    public UpdateSubtitleEvent() {

    }

    public UpdateSubtitleEvent(int type) {
        this.type = type;
    }

    public UpdateSubtitleEvent(int what, int type) {
        this.what = what;
        this.type = type;
    }
}
