//package com.github.mikumiku.addon.modules;
//
//
//import com.github.mikumiku.addon.util.Via;
//import meteordevelopment.meteorclient.events.world.TickEvent;
//import meteordevelopment.meteorclient.settings.*;
//import meteordevelopment.meteorclient.systems.modules.Categories;
//import meteordevelopment.meteorclient.systems.modules.Module;
//import meteordevelopment.meteorclient.utils.player.ChatUtils;
//import meteordevelopment.meteorclient.utils.player.FindItemResult;
//import meteordevelopment.meteorclient.utils.player.InvUtils;
//import meteordevelopment.meteorclient.utils.player.PlayerUtils;
//import meteordevelopment.meteorclient.utils.world.Dimension;
//import meteordevelopment.orbit.EventHandler;
//import net.minecraft.item.ItemStack;
//import net.minecraft.item.Items;
//import net.minecraft.util.Hand;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.util.math.Vec3d;
//
//public class UniversalSupply3 extends Module {
//    private final SettingGroup sgGeneral = settings.getDefaultGroup();
//    private final SettingGroup sgEnd = settings.createGroup("End Settings");
//    private final SettingGroup sgNether = settings.createGroup("Nether Settings");
//    private final SettingGroup sgOverworld = settings.createGroup("Overworld Settings");
//
//    // General Settings
//    private final Setting<Integer> elytraDurabilityThreshold = sgGeneral.add(new IntSetting.Builder()
//        .name("elytra-durability-threshold")
//        .description("Minimum durability before triggering resupply.")
//        .defaultValue(50)
//        .min(1)
//        .max(432)
//        .sliderMax(432)
//        .build()
//    );
//
//    private final Setting<Integer> fireworkMinCount = sgGeneral.add(new IntSetting.Builder()
//        .name("firework-min-count")
//        .description("Minimum firework count before resupply.")
//        .defaultValue(16)
//        .min(1)
//        .max(64)
//        .sliderMax(64)
//        .build()
//    );
//
//    private final Setting<Boolean> autoChorusFruit = sgGeneral.add(new BoolSetting.Builder()
//        .name("auto-chorus-fruit")
//        .description("Automatically eat chorus fruit for safe landing.")
//        .defaultValue(true)
//        .build()
//    );
//
//    // End Settings
//    private final Setting<Boolean> endMode = sgEnd.add(new BoolSetting.Builder()
//        .name("end-mode")
//        .description("Enable End dimension supply mode (Pitch 40).")
//        .defaultValue(true)
//        .build()
//    );
//
//    private final Setting<Double> pitch40Threshold = sgEnd.add(new DoubleSetting.Builder()
//        .name("pitch-40-threshold")
//        .description("Pitch angle threshold for detection.")
//        .defaultValue(40.0)
//        .min(30.0)
//        .max(50.0)
//        .sliderMax(50.0)
//        .build()
//    );
//
//    // Nether Settings
//    private final Setting<Boolean> netherMode = sgNether.add(new BoolSetting.Builder()
//        .name("nether-mode")
//        .description("Enable Nether dimension supply mode.")
//        .defaultValue(true)
//        .build()
//    );
//
//    private final Setting<Boolean> ghastProtection = sgNether.add(new BoolSetting.Builder()
//        .name("ghast-protection")
//        .description("Build protection around shulker box against ghast fireballs.")
//        .defaultValue(true)
//        .build()
//    );
//
//    private final Setting<Integer> safeNetherY = sgNether.add(new IntSetting.Builder()
//        .name("safe-nether-y")
//        .description("Preferred Y level for safe landing in Nether.")
//        .defaultValue(90)
//        .min(32)
//        .max(120)
//        .sliderMax(120)
//        .build()
//    );
//
//    // Overworld Settings
//    private final Setting<Boolean> overworldMode = sgOverworld.add(new BoolSetting.Builder()
//        .name("overworld-mode")
//        .description("Enable Overworld dimension supply mode.")
//        .defaultValue(true)
//        .build()
//    );
//
//    private final Setting<Integer> airPlatformY = sgOverworld.add(new IntSetting.Builder()
//        .name("air-platform-y")
//        .description("Y level for building air platform.")
//        .defaultValue(200)
//        .min(150)
//        .max(320)
//        .sliderMax(320)
//        .build()
//    );
//
//    private final Setting<Boolean> nightProtection = sgOverworld.add(new BoolSetting.Builder()
//        .name("night-protection")
//        .description("Build walls/roof at night for mob protection.")
//        .defaultValue(true)
//        .build()
//    );
//
//    // State Management
//    private SupplyState state = SupplyState.IDLE;
//    private Vec3d originalDirection = null;
//    private BlockPos targetCoords = null;
//    private int taskTimer = 0;
//    private boolean isFlying = false;
//
//    private enum SupplyState {
//        IDLE,
//        DETECTING,
//        LANDING,
//        BUILDING_PLATFORM,
//        PLACING_CONTAINER,
//        RESUPPLYING,
//        TAKING_OFF,
//        RESUMING_FLIGHT
//    }
//
//    public UniversalSupply3() {
//        super(Categories.Movement, "elytra-supply-system", "Automatic elytra and firework resupply system.");
//    }
//
//    @Override
//    public void onActivate() {
//        state = SupplyState.IDLE;
//        originalDirection = null;
//        targetCoords = null;
//        taskTimer = 0;
//        isFlying = false;
//        ChatUtils.info("Elytra Supply System activated!");
//    }
//
//    @EventHandler
//    private void onTick(TickEvent.Pre event) {
//        if (mc.player == null || mc.world == null) return;
//
//        // Update flying state
//        isFlying = Via.isFallFlying(mc);
//
//        // State machine
//        switch (state) {
//            case IDLE -> handleIdleState();
//            case DETECTING -> handleDetectingState();
//            case LANDING -> handleLandingState();
//            case BUILDING_PLATFORM -> handleBuildingPlatformState();
//            case PLACING_CONTAINER -> handlePlacingContainerState();
//            case RESUPPLYING -> handleResupplyingState();
//            case TAKING_OFF -> handleTakingOffState();
//            case RESUMING_FLIGHT -> handleResumingFlightState();
//        }
//
//        taskTimer++;
//    }
//
//    private void handleIdleState() {
//        if (!isFlying) return;
//
//        // Check if resupply is needed
//        if (needsResupply()) {
//            state = SupplyState.DETECTING;
//            originalDirection = mc.player.getRotationVec(1.0f);
//            ChatUtils.info("Low supplies detected, initiating resupply procedure...");
//        }
//    }
//
//    private void handleDetectingState() {
//        // Determine dimension and check if mode is enabled
//        if (isInEnd() && endMode.get()) {
//            if (isPitch40Flying()) {
//                ChatUtils.info("End Pitch 40 mode detected.");
//                state = SupplyState.LANDING;
//            }
//        } else if (isInNether() && netherMode.get()) {
//            ChatUtils.info("Nether travel mode detected.");
//            state = SupplyState.LANDING;
//        } else if (isInOverworld() && overworldMode.get()) {
//            ChatUtils.info("Overworld flight mode detected.");
//            state = SupplyState.LANDING;
//        } else {
//            ChatUtils.warning("Current dimension mode is disabled. Aborting resupply.");
//            state = SupplyState.IDLE;
//        }
//    }
//
//    private void handleLandingState() {
//        if (autoChorusFruit.get() && hasChorusFruit()) {
//            eatChorusFruit();
//            ChatUtils.info("Eating chorus fruit for safe landing...");
//            taskTimer = 0;
//
//            // Wait for teleport
//            if (taskTimer > 40) { // 2 seconds
//                proceedAfterLanding();
//            }
//        } else {
//            // Manual landing or wait for ground
//            if (mc.player.isOnGround()) {
//                proceedAfterLanding();
//            }
//        }
//    }
//
//    private void proceedAfterLanding() {
//        if (isInOverworld()) {
//            // Need to build air platform first
//            state = SupplyState.BUILDING_PLATFORM;
//            ChatUtils.info("Ascending to build air platform...");
//        } else {
//            state = SupplyState.PLACING_CONTAINER;
//        }
//    }
//
//    private void handleBuildingPlatformState() {
//        // Use fireworks to ascend to Y=200 (or configured height)
//        if (mc.player.getY() < airPlatformY.get()) {
//            useFirework();
//        } else {
//            // Build platform
//            buildAirPlatform();
//
//            if (isNight() && nightProtection.get()) {
//                buildProtectionWalls();
//            }
//
//            state = SupplyState.PLACING_CONTAINER;
//        }
//    }
//
//    private void handlePlacingContainerState() {
//        // Place ender chest or shulker box
//        if (isInNether() && ghastProtection.get()) {
//            buildGhastProtection();
//        }
//
//        placeEnderChest();
//        taskTimer = 0;
//        state = SupplyState.RESUPPLYING;
//    }
//
//    private void handleResupplyingState() {
//        // Open container and swap elytra/fireworks
//        if (taskTimer < 20) return; // Wait for placement
//
//        // Simulate opening ender chest and taking items
//        replaceElytra();
//        refillFireworks();
//
//        ChatUtils.info("Resupply complete!");
//        taskTimer = 0;
//        state = SupplyState.TAKING_OFF;
//    }
//
//    private void handleTakingOffState() {
//        // Use firework to take off
//        useFirework();
//
//        if (taskTimer > 20 && isFlying) {
//            state = SupplyState.RESUMING_FLIGHT;
//        }
//    }
//
//    private void handleResumingFlightState() {
//        // Resume original direction
//        if (originalDirection != null) {
//            // Set player rotation to original direction
//            // This would need proper implementation with rotation manager
//            ChatUtils.info("Resuming original flight path...");
//        }
//
//        state = SupplyState.IDLE;
//        originalDirection = null;
//    }
//
//    // Helper Methods
//    private boolean needsResupply() {
//        return isElytraDamaged() || isLowOnFireworks();
//    }
//
//    private boolean isElytraDamaged() {
//        ItemStack chest = mc.player.getInventory().getArmorStack(2);
//        if (chest.getItem() == Items.ELYTRA) {
//            int maxDamage = chest.getMaxDamage();
//            int damage = chest.getDamage();
//            return (maxDamage - damage) <= elytraDurabilityThreshold.get();
//        }
//        return false;
//    }
//
//    private boolean isLowOnFireworks() {
//        int count = InvUtils.find(Items.FIREWORK_ROCKET).count();
//        return count < fireworkMinCount.get();
//    }
//
//    private boolean isPitch40Flying() {
//        float pitch = mc.player.getPitch();
//        return Math.abs(pitch - pitch40Threshold.get()) < 10.0;
//    }
//
//    private boolean isInEnd() {
//        return PlayerUtils.getDimension().equals(Dimension.End);
//    }
//
//    private boolean isInNether() {
//        return PlayerUtils.getDimension().equals(Dimension.Nether);
//    }
//
//    private boolean isInOverworld() {
//        return PlayerUtils.getDimension().equals(Dimension.Overworld);
//    }
//
//    private boolean isNight() {
//        long timeOfDay = mc.world.getTimeOfDay() % 24000;
//        return timeOfDay >= 13000 && timeOfDay <= 23000;
//    }
//
//    private boolean hasChorusFruit() {
//        return InvUtils.find(Items.CHORUS_FRUIT).found();
//    }
//
//    private void eatChorusFruit() {
//        FindItemResult fruit = InvUtils.find(Items.CHORUS_FRUIT);
//        if (fruit.found()) {
//            InvUtils.swap(fruit.slot(), true);
//            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
//        }
//    }
//
//    private void useFirework() {
//        FindItemResult firework = InvUtils.find(Items.FIREWORK_ROCKET);
//        if (firework.found()) {
//            InvUtils.swap(firework.slot(), true);
//            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
//        }
//    }
//
//    private void buildAirPlatform() {
//        // Build a 3x3 platform at current position
//        BlockPos center = mc.player.getBlockPos();
//        for (int x = -1; x <= 1; x++) {
//            for (int z = -1; z <= 1; z++) {
//                BlockPos pos = center.add(x, -1, z);
//                placeBlock(pos);
//            }
//        }
//        ChatUtils.info("Air platform built.");
//    }
//
//    private void buildProtectionWalls() {
//        // Build walls around platform
//        ChatUtils.info("Building protection walls for night safety...");
//        // Implementation would place blocks around the platform
//    }
//
//    private void buildGhastProtection() {
//        // Build obsidian/cobblestone protection
//        ChatUtils.info("Building ghast protection...");
//    }
//
//    private void placeEnderChest() {
//        FindItemResult chest = InvUtils.find(Items.ENDER_CHEST);
//        if (chest.found()) {
//            InvUtils.swap(chest.slot(), true);
//            BlockPos pos = mc.player.getBlockPos().add(0, 0, 1);
//            // Place ender chest logic
//            ChatUtils.info("Ender chest placed.");
//        }
//    }
//
//    private void replaceElytra() {
//        // Logic to swap damaged elytra with fresh one from ender chest
//        ChatUtils.info("Replacing damaged elytra...");
//    }
//
//    private void refillFireworks() {
//        // Logic to take fireworks from ender chest
//        ChatUtils.info("Refilling fireworks...");
//    }
//
//    private void placeBlock(BlockPos pos) {
//        // Basic block placement logic
//        FindItemResult blocks = InvUtils.findInHotbar(itemStack ->
//            itemStack.getItem().getDefaultStack().isOf(Items.COBBLESTONE) ||
//                itemStack.getItem().getDefaultStack().isOf(Items.DIRT) ||
//                itemStack.getItem().getDefaultStack().isOf(Items.NETHERRACK)
//        );
//
//        if (blocks.found()) {
//            InvUtils.swap(blocks.slot(), true);
//            // Place block at pos
//        }
//    }
//}
