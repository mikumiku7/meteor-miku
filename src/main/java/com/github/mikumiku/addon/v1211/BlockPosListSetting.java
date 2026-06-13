package com.github.mikumiku.addon.v1211;

import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class BlockPosListSetting extends Setting<List<BlockPos>> {

    public BlockPosListSetting(String name, String description, List<BlockPos> defaultValue,
                               Consumer<List<BlockPos>> onChanged,
                               Consumer<Setting<List<BlockPos>>> onModuleActivated,
                               IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    protected List<BlockPos> parseImpl(String str) {
        // 格式: x1,y1,z1;x2,y2,z2;x3,y3,z3
        String[] entries = str.split(";");
        List<BlockPos> positions = new ArrayList<>(entries.length);

        try {
            for (String entry : entries) {
                String[] coords = entry.trim().split(",");
                if (coords.length != 3) continue;

                int x = Integer.parseInt(coords[0].trim());
                int y = Integer.parseInt(coords[1].trim());
                int z = Integer.parseInt(coords[2].trim());
                positions.add(new BlockPos(x, y, z));
            }
        } catch (NumberFormatException ignored) {
        }

        return positions;
    }

    @Override
    protected void resetImpl() {
        value = new ArrayList<>(defaultValue);
    }

    @Override
    protected boolean isValueValid(List<BlockPos> value) {
        return true;
    }

    @Override
    protected NbtCompound save(NbtCompound tag) {
        NbtList valueTag = new NbtList();

        for (BlockPos pos : get()) {
            valueTag.add(new NbtIntArray(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
        }

        tag.put("value", valueTag);
        return tag;
    }

    @Override
    protected List<BlockPos> load(NbtCompound tag) {
        get().clear();

        NbtList valueTag = tag.getList("value", NbtElement.INT_ARRAY_TYPE);
        for (NbtElement element : valueTag) {
            int[] coords = ((NbtIntArray) element).getIntArray();
            if (coords.length == 3) {
                get().add(new BlockPos(coords[0], coords[1], coords[2]));
            }
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, List<BlockPos>, BlockPosListSetting> {

        public Builder() {
            super(new ArrayList<>(0));
        }

        public Builder defaultValue(BlockPos... defaults) {
            return defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
        }

        public Builder defaultValue(List<BlockPos> defaults) {
            this.defaultValue = new ArrayList<>(defaults);
            return this;
        }

        @Override
        public BlockPosListSetting build() {
            return new BlockPosListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
