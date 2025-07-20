package com.github.mikumiku.addon;

import com.github.mikumiku.addon.commands.CommandMiku;
import com.github.mikumiku.addon.hud.HudMiku;
import com.github.mikumiku.addon.modules.*;
import lombok.extern.slf4j.Slf4j;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;

@Slf4j
public class MikuMikuAddon extends MeteorAddon {
    public static final Category CATEGORY = new Category("Miku");
    public static final HudGroup HUD_GROUP = new HudGroup("Miku");

    @Override
    public void onInitialize() {
        log.info("Initializing Meteor Addon Miku");

        // Modules
        Modules modules = Modules.get();
//        modules.add(new ModuleExample());
        modules.add(new PlayerAlert());
        modules.add(new AutoTrashModule());
        modules.add(new LiquidFiller());
        modules.add(new AutoSandMiner());
        modules.add(new AutoUseItems());
        modules.add(new SeedMine());
        modules.add(new ElytraFinder());
//        modules.add(new MikuModule(CATEGORY, "miku", "miku"));


        // Commands
        Commands.add(new CommandMiku());

        // HUD
        Hud.get().register(HudMiku.INFO);

    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
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
