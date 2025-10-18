package com.github.mikumiku.addon.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

public interface NbtUtil {
    NbtList getRollingList(NbtCompound tag, byte element);
    String getString(NbtCompound tag, String key);
    int getInt(NbtCompound tag, String key);
    boolean getBoolean(NbtCompound tag, String key);
    long getLong(NbtCompound tag, String key);
    NbtCompound getCompound(NbtCompound tag, String key);
}
