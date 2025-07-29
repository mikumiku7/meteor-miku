package com.github.mikumiku.addon;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import meteordevelopment.meteorclient.pathing.NopPathManager;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.MinecraftClient;

public abstract class BaseModule extends Module {

    protected MinecraftClient mc;

    IBaritone baritone;

    public BaseModule(Category category, String name, String description, String... aliases) {
        super(category, name, description, aliases);
        mc = MinecraftClient.getInstance();
    }

    public BaseModule(Category category, String name, String desc) {
        super(category, name, desc);
        mc = MinecraftClient.getInstance();


        try {
            if (PathManagers.get() instanceof NopPathManager) {
                //noop
            } else {
                baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            }
        } catch (Exception e) {
            error("请安装Baritone!");
        }
    }

    public BaseModule(String name, String desc) {
        super(MikuMikuAddon.CATEGORY, name, desc);
    }

}
