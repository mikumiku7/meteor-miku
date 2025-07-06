package com.github.mikumiku.addon.util;

import com.mojang.brigadier.StringReader;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.systems.config.Config;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class ChatUtils {
    public static Text getPrefix() {
        String prefix = "MikuMiku";
        Color startColor = new Color(0, 255, 247);
        Color endColor = new Color(48, 155, 186);
        char[] chars = prefix.toCharArray();
        MutableText result = Text.empty();
        int count = chars.length;

        for (int index = 0; index < count; index++) {
            char c = chars[index];
            double ratio = (double) index / (count - 1);
            Color color = ColorUtil.fadeColor(startColor, endColor, ratio);
            result.append(Text.literal(String.valueOf(c)).setStyle(Style.EMPTY.withColor(color.getRGB())));
        }
        result = Text.literal("[").append(result).append("] ");

        return result;
    }

    public static void sendMsg(Text msg) {
        if (MeteorClient.mc.world == null) return;

        MutableText message = Text.empty();
        message.setStyle(Style.EMPTY.withFormatting(Formatting.GRAY));
        message.append(getPrefix());
        message.append(msg);

        ((IChatHud) MeteorClient.mc.inGameHud.getChatHud()).meteor$add(message, 0);
    }

    public static void sendMsg(Text msg, int id) {
        if (MeteorClient.mc.world == null) return;

        MutableText message = Text.empty();
        message.setStyle(Style.EMPTY.withFormatting(Formatting.GRAY));
        message.append(getPrefix());
        message.append(msg);

        ((IChatHud) MeteorClient.mc.inGameHud.getChatHud()).meteor$add(message, id);
    }

    public static void sendMsg(String msg, int id) {
        sendMsg(Text.of(msg), id);
    }

    public static void sendMsg(String message) {
        sendMsg(Text.of(message));
    }

    public static void sendMsg(String prefix, Text message) {
        sendMsg(0, prefix, Formatting.LIGHT_PURPLE, message);
    }

    public static void sendMsg(Formatting color, String message, Object... args) {
        sendMsg(0, null, null, color, message, args);
    }

    public static void sendMsg(int id, Formatting color, String message, Object... args) {
        sendMsg(id, null, null, color, message, args);
    }

    public static void sendMsg(int id, @Nullable String prefixTitle, @Nullable Formatting prefixColor, Formatting messageColor, String messageContent, Object... args) {
        MutableText message = formatMsg(String.format(messageContent, args), messageColor);
        sendMsg(id, prefixTitle, prefixColor, message);
    }

    public static void sendMsg(int id, @Nullable String prefixTitle, @Nullable Formatting prefixColor, String messageContent, Formatting messageColor) {
        MutableText message = formatMsg(messageContent, messageColor);
        sendMsg(id, prefixTitle, prefixColor, message);
    }

    public static void sendMsg(int id, @Nullable String prefixTitle, @Nullable Formatting prefixColor, Text msg) {
        if (MeteorClient.mc.world == null) return;

        MutableText message = Text.empty();
        message.append(getPrefix());
        if (prefixTitle != null) message.append(getCustomPrefix(prefixTitle, prefixColor));
        message.append(msg);

        if (!Config.get().deleteChatFeedback.get()) id = 0;

        ((IChatHud) MeteorClient.mc.inGameHud.getChatHud()).meteor$add(message, id);
    }

    private static MutableText getCustomPrefix(String prefixTitle, Formatting prefixColor) {
        MutableText prefix = Text.empty();
        prefix.setStyle(prefix.getStyle().withFormatting(Formatting.GRAY));

        prefix.append("[");

        MutableText moduleTitle = Text.literal(prefixTitle);
        moduleTitle.setStyle(moduleTitle.getStyle().withFormatting(prefixColor));
        prefix.append(moduleTitle);

        prefix.append("] ");

        return prefix;
    }

    private static MutableText formatMsg(String message, Formatting defaultColor) {
        StringReader reader = new StringReader(message);
        MutableText text = Text.empty();
        Style style = Style.EMPTY.withFormatting(defaultColor);
        StringBuilder result = new StringBuilder();
        boolean formatting = false;
        while (reader.canRead()) {
            char c = reader.read();
            if (c == '(') {
                text.append(Text.literal(result.toString()).setStyle(style));
                result.setLength(0);
                result.append(c);
                formatting = true;
            } else {
                result.append(c);

                if (formatting && c == ')') {
                    switch (result.toString()) {
                        case "(default)" -> {
                            style = style.withFormatting(defaultColor);
                            result.setLength(0);
                        }
                        case "(highlight)" -> {
                            style = style.withFormatting(Formatting.WHITE);
                            result.setLength(0);
                        }
                        case "(underline)" -> {
                            style = style.withFormatting(Formatting.UNDERLINE);
                            result.setLength(0);
                        }
                        case "(bold)" -> {
                            style = style.withFormatting(Formatting.BOLD);
                            result.setLength(0);
                        }
                    }
                    formatting = false;
                }
            }
        }

        if (!result.isEmpty()) text.append(Text.literal(result.toString()).setStyle(style));

        return text;
    }
}
