package com.github.mikumiku.addon.impl.v1215;

import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;

public class VillagerUtil implements com.github.mikumiku.addon.util.VillagerUtil {
    @Override
    public boolean isNoneProfession(VillagerData data) {
        return data.profession() == VillagerProfession.NONE;
    }
}
