package com.github.mikumiku.addon.util;

import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;

public interface InventoryUtil {
    int getSyncId(InventoryS2CPacket inventoryS2CPacket);
}
