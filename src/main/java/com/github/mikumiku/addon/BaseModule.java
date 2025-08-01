package com.github.mikumiku.addon;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import com.github.mikumiku.addon.mixin.IClientWorld;
import meteordevelopment.meteorclient.pathing.NopPathManager;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static com.github.mikumiku.addon.util.ChatUtils.sendMsg;

public abstract class BaseModule extends Module {

    protected MinecraftClient mc = MinecraftClient.getInstance();

    IBaritone baritone;

    public BaseModule(Category category, String name, String description, String... aliases) {
        super(category, name, description, aliases);
        mc = MinecraftClient.getInstance();
    }

    public BaseModule(Category category, String name, String desc) {
        super(category, name, desc);
        mc = MinecraftClient.getInstance();


        try {
            if (PathManagers.get() instanceof NopPathManager) {
                //noop
            } else {
                baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            }
        } catch (Exception e) {
            error("请安装Baritone!");
        }
    }

    public BaseModule(String name, String desc) {
        super(MikuMikuAddon.CATEGORY, name, desc);
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


    public static void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null || mc.world == null) return;
        try (PendingUpdateManager pendingUpdateManager = ((IClientWorld) mc.world).getPendingManager().incrementSequence()){
            int i = pendingUpdateManager.getSequence();
            mc.getNetworkHandler().sendPacket(packetCreator.predict(i));
        }
    }
}
