package com.github.mikumiku.addon.mixinface;

import com.github.mikumiku.addon.modules.ElytraFlyPlusPlus;
import com.github.mikumiku.addon.modules.FakeCoordinates;
import com.github.mikumiku.addon.modules.SeedMine;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class MagicMix {

    // 静态变量用于存储偏移后的坐标
    public static double x = 0;
    public static double z = 0;

    public static List<BlockPos> oreGoals = new ArrayList<>();

    public static boolean eflyenabled() {
        ElytraFlyPlusPlus efly = Modules.get().get(ElytraFlyPlusPlus.class);
        return efly != null && efly.enabled();
    }

    public static boolean coordinatesisActive() {
        FakeCoordinates coordinates = Modules.get().get(FakeCoordinates.class);

        return coordinates != null && coordinates.isActive();
    }

    public static boolean oreSimBaritone() {

        SeedMine oreSim = Modules.get().get(SeedMine.class);
        if (oreSim == null || !oreSim.baritone())
            return false;

        return true;
    }
}
