package com.github.mikumiku.addon.impl.v1211;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;

public class EventUtil implements com.github.mikumiku.addon.util.EventUtil {
    @Override
    public ClickEvent of(ClickEvent.Action action, String value) {
        return new ClickEvent(
            action,
            value
        );
    }

    @Override
    public HoverEvent of(HoverEvent.Action action, Object value) {
        return new HoverEvent(
            action,
            value
        );
    }
}
