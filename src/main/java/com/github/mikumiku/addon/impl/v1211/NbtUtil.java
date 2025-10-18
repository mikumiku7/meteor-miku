package com.github.mikumiku.addon.impl.v1211;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

public class NbtUtil implements com.github.mikumiku.addon.util.NbtUtil {
    @Override
    public NbtList getRollingList(NbtCompound tag, byte element) {
        return tag.getList("rolling", element);
    }

    @Override
    public String getString(NbtCompound tag, String key) {
        return tag.getString(key);
    }

    @Override
    public int getInt(NbtCompound tag, String key) {
        return tag.getInt(key);
    }

    @Override
    public boolean getBoolean(NbtCompound tag, String key) {
        return tag.getBoolean(key);
    }

    @Override
    public long getLong(NbtCompound tag, String key) {
        return tag.getLong(key);
    }

    @Override
    public NbtCompound getCompound(NbtCompound tag, String key) {
        return tag.getCompound(key);
    }
}
