package com.github.mikumiku.addon.impl.v1216;

import net.minecraft.item.ItemStack;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;

import java.net.URI;

public class EventUtil implements com.github.mikumiku.addon.util.EventUtil {
    @Override
    public ClickEvent of(ClickEvent.Action action, String value) {
        return switch (action) {
            case OPEN_URL -> new ClickEvent.OpenUrl(URI.create(value));
            case OPEN_FILE -> new ClickEvent.OpenFile(value);
            case RUN_COMMAND -> new ClickEvent.RunCommand(value);
            case SUGGEST_COMMAND -> new ClickEvent.SuggestCommand(value);
            case SHOW_DIALOG, CUSTOM -> throw new RuntimeException("该事件未实现！");
            case CHANGE_PAGE -> new ClickEvent.ChangePage(Integer.parseInt(value));
            case COPY_TO_CLIPBOARD -> new ClickEvent.CopyToClipboard(value);
        };
    }

    @Override
    public HoverEvent of(HoverEvent.Action action, Object value) {
        return switch (action) {
            case SHOW_TEXT -> new HoverEvent.ShowText((Text) value);
            case SHOW_ITEM -> new HoverEvent.ShowItem((ItemStack) value);
            case SHOW_ENTITY -> new HoverEvent.ShowEntity((HoverEvent.EntityContent) value);
        };
    }
}
