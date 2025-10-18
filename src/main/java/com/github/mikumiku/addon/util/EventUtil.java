package com.github.mikumiku.addon.util;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;

public interface EventUtil {

    ClickEvent of(ClickEvent.Action action, String value);

    HoverEvent of(HoverEvent.Action action, Object value);

}
