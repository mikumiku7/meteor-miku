package com.github.mikumiku.addon.modules;

import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static com.github.mikumiku.addon.util.ChatUtils.sendMsg;

@SuppressWarnings("unused")
public class MikuModule extends Module {
    private boolean active;
    private final int priority;
    protected MinecraftClient mc = MinecraftClient.getInstance();


    public MikuModule(Category Category, String name, String desc) {
        super(Category, name, desc);
        this.priority = 100;
        mc = MinecraftClient.getInstance();

    }

    public void sendToggledMsg() {
        Text onMsg = Text.empty().setStyle(Style.EMPTY.withFormatting(Formatting.GREEN)).append("ON");
        Text offMsg = Text.empty().setStyle(Style.EMPTY.withFormatting(Formatting.RED)).append("OFF");
        ChatUtils.forceNextPrefixClass(getClass());
        MutableText toggledMsg = Text.empty();
        toggledMsg.append(Text.empty().setStyle(Style.EMPTY.withFormatting(Formatting.WHITE)).append(title));
        toggledMsg.append(" ");
        toggledMsg.append(Text.empty().setStyle(Style.EMPTY.withFormatting(Formatting.GRAY)).append("toggled "));
        toggledMsg.append((isActive() ? onMsg : offMsg));

        sendMsg(toggledMsg, hashCode());
    }

}
