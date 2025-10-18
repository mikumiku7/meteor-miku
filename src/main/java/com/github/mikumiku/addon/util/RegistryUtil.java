package com.github.mikumiku.addon.util;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public interface RegistryUtil {

    Setting<Boolean> coal = new BoolSetting.Builder().name("煤炭").description("coal_ore").build();
    Setting<Boolean> iron = new BoolSetting.Builder().name("铁").description("iron_ore").build();
    Setting<Boolean> gold = new BoolSetting.Builder().name("金").description("gold_ore").build();
    Setting<Boolean> redstone = new BoolSetting.Builder().name("红石").description("redstone_ore").build();
    Setting<Boolean> diamond = new BoolSetting.Builder().name("钻石").description("diamond_ore").build();
    Setting<Boolean> lapis = new BoolSetting.Builder().name("青金石").description("lapis_ore").build();
    Setting<Boolean> copper = new BoolSetting.Builder().name("铜").description("copper_ore").build();
    Setting<Boolean> emerald = new BoolSetting.Builder().name("绿宝石").description("emerald_ore").build();
    Setting<Boolean> quartz = new BoolSetting.Builder().name("石英").description("nether_quartz_ore").build();
    Setting<Boolean> debris = new BoolSetting.Builder().name("远古残骸").description("ancient_debris").build();
    List<Setting<Boolean>> oreSettings = new ArrayList<>(Arrays.asList(coal, iron, gold, redstone, diamond, lapis, copper, emerald, quartz, debris));

    Map<RegistryKey<Biome>, List<Ore>> getRegistry(Dimension dimension);
}
