package com.github.mikumiku.addon.impl.v1211;

import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;

public class InventoryUtil implements com.github.mikumiku.addon.util.InventoryUtil {
    @Override
    public int getSyncId(InventoryS2CPacket inventoryS2CPacket) {
        return inventoryS2CPacket.getSyncId();
    }
}
