package com.github.mikumiku.addon.impl.v1211;

import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;

public class PacketUtil implements com.github.mikumiku.addon.util.PacketUtil {
    @Override
    public void sendPressShift() {
        MeteorClient.mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(MeteorClient.mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
    }

    @Override
    public void sendReleaseShift() {
        MeteorClient.mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(MeteorClient.mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
    }
}
