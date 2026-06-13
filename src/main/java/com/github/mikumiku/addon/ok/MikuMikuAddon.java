package com.github.mikumiku.addon.ok;

import com.github.mikumiku.addon.MikuApp;
import lombok.extern.slf4j.Slf4j;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.HudGroup;

@Slf4j
public class MikuMikuAddon extends MeteorAddon {
    public static final HudGroup HUD_GROUP = new HudGroup("Miku");

    @Override
    public void onInitialize() {

        MikuApp.init();
    }

    @Override
    public void onRegisterCategories() {
        MikuApp.onRegisterCategories();
    }

    @Override
    public String getPackage() {
        return "com.github.mikumiku.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("mikumiku7", "meteor-miku");
    }
}
