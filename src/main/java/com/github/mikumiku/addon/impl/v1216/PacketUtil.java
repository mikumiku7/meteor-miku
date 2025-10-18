package com.github.mikumiku.addon.impl.v1216;

import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.util.PlayerInput;

public class PacketUtil implements com.github.mikumiku.addon.util.PacketUtil {
    @Override
    public void sendPressShift() {
        PlayerInput playerInput = new PlayerInput(false, false, false, false, false, true, false);
        MeteorClient.mc.player.networkHandler.sendPacket(new PlayerInputC2SPacket(playerInput));
    }

    @Override
    public void sendReleaseShift() {
        PlayerInput playerInput = new PlayerInput(false, false, false, false, false, false, false);
        MeteorClient.mc.player.networkHandler.sendPacket(new PlayerInputC2SPacket(playerInput));
    }
}
