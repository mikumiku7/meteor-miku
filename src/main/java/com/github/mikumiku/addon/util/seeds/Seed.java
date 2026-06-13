package com.github.mikumiku.addon.util.seeds;

import com.seedfinding.mccore.version.MCVersion;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtLong;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.Optional;

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
        Object version1 = tag.getString("version");
        String key;
        if (version1 instanceof Optional<?> v) {
            key = (String) v.get();
        }else {
            key = (String) version1;
        }

        Object seed1 = tag.getLong("seed");
        Long key2;
         if (seed1 instanceof Optional<?> s) {
            key2 = (Long) s.get();
        }else {
            key2 = (Long) seed1;
        }

        return new Seed(
            key2,
            MCVersion.fromString(key)
        );
    }

//    public Text toText() {
//        MutableText text = Text.literal(String.format("[%s%s%s] (%s)",
//            Formatting.GREEN,
//            seed.toString(),
//            Formatting.WHITE,
//            version.toString()
//        ));
//        text.setStyle(text.getStyle()
//            .withClickEvent(new ClickEvent(
//                ClickEvent.Action.COPY_TO_CLIPBOARD,
//                seed.toString()
//            ))
//            .withHoverEvent(new HoverEvent(
//                HoverEvent.Action.SHOW_TEXT,
//                Text.literal("Copy to clipboard")
//            ))
//        );
//        return text;
//    }
}
