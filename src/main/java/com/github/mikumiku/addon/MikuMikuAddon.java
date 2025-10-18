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
import meteordevelopment.meteorclient.utils.player.ChatUtils;

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
        modules.add(new TreeAura());
        modules.add(new SeedMine());
        modules.add(new OnekeyFireWork());
        modules.add(new StructureFinder());
        modules.add(new ElytraFinder());
        modules.add(new ShulkerBoxItemFetcher());
        modules.add(new VillagerRoller());
        modules.add(new LitematicaMover());
        modules.add(new AutoWalk());
        modules.add(new HandsomeSpin());
        modules.add(new AutoCrystalBlock());
        modules.add(new AutoLog());
        modules.add(new InfiniteElytra());
        modules.add(new GhostMine());
        modules.add(new AutoWither());
        modules.add(new AnchorAuraPlus());
        modules.add(new AutoEz());
        modules.add(new RoadBuilder());
//        modules.add(new KillAuraPlus());
        modules.add(new SelfTrapPlusPlus());
        modules.add(new AutoSM());
        modules.add(new AutoLoginPlus());
        modules.add(new NetherSearchArea());
        modules.add(new NoFall());
        modules.add(new EntityList());
        modules.add(new AutoTouchFire());
        modules.add(new FarmHelper());
        modules.add(new AutoXP());
        modules.add(new ChestAura());
        modules.add(new FastFall());
        modules.add(new TridentFly());
        modules.add(new ElytraFlyPlus());
        modules.add(new HighwayBlocker());
        modules.add(new HighwayClearer());
//        modules.add(new MikuModule(CATEGORY, "miku", "miku"));
//        MikuModule mikuModule = new MikuModule(CATEGORY, "miku插件", "miku");
        ChatUtils.warning("Miku插件完全开源免费。更新免费获取地址: https://github.com/mikumiku7/meteor-miku/releases");
        ChatUtils.warning("Miku插件群：1013297171");
        // Commands
        Commands.add(new CommandMiku());

        // HUD
        Hud.get().register(HudMiku.INFO);
        AutoSM autoSM = modules.get(AutoSM.class);
        //todo kill aura
        //todo fly

//        if (!autoSM.isActive()) {
//            autoSM.toggle();
//        }
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
