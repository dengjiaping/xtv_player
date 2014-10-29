package com.kankan.player.event;

import com.kankan.player.item.DeviceItem;

import java.util.List;

/**
 * Created by zhangdi on 14-4-2.
 */
public class DeviceEvent extends AbsEventBase {
    public List<DeviceItem.DeviceType> types;

    public List<DeviceItem> deviceItems;

}
