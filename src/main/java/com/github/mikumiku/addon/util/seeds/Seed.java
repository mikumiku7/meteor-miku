package com.github.mikumiku.addon.util.seeds;

import com.github.mikumiku.addon.dynamic.DV;
import com.github.mikumiku.addon.util.EventUtil;
import com.github.mikumiku.addon.util.NbtUtil;
import com.seedfinding.mccore.version.MCVersion;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtLong;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

public class Seed {
    public final Long seed;
    public final MCVersion version;

    public Seed(Long seed, MCVersion version) {
        this.seed = seed;
        if (version == null)
            version = MCVersion.latest();
        this.version = version;
    }

    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.put("seed", NbtLong.of(seed));
        tag.put("version", NbtString.of(version.name));
        return tag;
    }

    public static Seed fromTag(NbtCompound tag) {
        return new Seed(
            DV.of(NbtUtil.class).getLong(tag, "seed"),
            MCVersion.fromString(DV.of(NbtUtil.class).getString(tag, "version"))
        );
    }

    public Text toText() {
        MutableText text = Text.literal(String.format("[%s%s%s] (%s)",
            Formatting.GREEN,
            seed.toString(),
            Formatting.WHITE,
            version.toString()
        ));
        text.setStyle(text.getStyle()
            .withClickEvent(DV.of(EventUtil.class).of(
                ClickEvent.Action.COPY_TO_CLIPBOARD,
                seed.toString()
            ))
            .withHoverEvent(DV.of(EventUtil.class).of(
                HoverEvent.Action.SHOW_TEXT,
                Text.literal("Copy to clipboard")
            ))
        );
        return text;
    }
}
