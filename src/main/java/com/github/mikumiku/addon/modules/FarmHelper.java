package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.BagUtil;
import com.github.mikumiku.addon.util.BaritoneUtil;
import com.github.mikumiku.addon.util.WorldUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;

import static net.minecraft.block.AbstractPlantStemBlock.AGE;
import static net.minecraft.state.property.Properties.WATERLOGGED;

public class FarmHelper extends BaseModule {

    // 设置组，用于组织模块的设置选项
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // 工作模式设置
    private final Setting<WorkMode> workMode = sgGeneral.add(new EnumSetting.Builder<WorkMode>()
        .name("工作模式").description("选择农场助手的工作模式").defaultValue(WorkMode.All).build());

    // 锄地设置
    private final Setting<Integer> tillDelay = sgGeneral.add(new IntSetting.Builder()
        .name("锄地延迟").description("锄地操作之间的延迟").defaultValue(1).min(0).sliderMax(20).build());
    private final Setting<Integer> tillRange = sgGeneral.add(new IntSetting.Builder()
        .name("锄地范围").description("锄地的水平范围").defaultValue(4).min(1).sliderMax(6).build());
    private final Setting<Boolean> autoSwitchHoe = sgGeneral.add(new BoolSetting.Builder()
        .name("自动切换锄头").description("没有锄头时自动切换到可用的锄头").defaultValue(true).build());

    // 种植设置
    private final Setting<Integer> plantDelay = sgGeneral.add(new IntSetting.Builder()
        .name("种植延迟").description("种植操作之间的延迟").defaultValue(1).min(0).sliderMax(20).build());
    private final Setting<Integer> plantRange = sgGeneral.add(new IntSetting.Builder()
        .name("种植范围").description("种植的水平范围").defaultValue(4).min(1).sliderMax(6).build());
    private final Setting<CropType> cropType = sgGeneral.add(new EnumSetting.Builder<CropType>()
        .name("作物类型").description("选择要种植的作物类型").defaultValue(CropType.Wheat).build());
    private final Setting<Boolean> autoSwitchSeeds = sgGeneral.add(new BoolSetting.Builder()
        .name("自动切换种子").description("种植时自动切换到选定的种子").defaultValue(true).build());

    // 收割设置
    private final Setting<Integer> harvestDelay = sgGeneral.add(new IntSetting.Builder()
        .name("收割延迟").description("收割操作之间的延迟").defaultValue(1).min(0).sliderMax(20).build());
    private final Setting<Integer> harvestRange = sgGeneral.add(new IntSetting.Builder()
        .name("收割范围").description("收割的水平范围").defaultValue(4).min(1).sliderMax(6).build());
    private final Setting<Boolean> harvestOnlyMature = sgGeneral.add(new BoolSetting.Builder()
        .name("仅收割成熟").description("只收割成熟的作物").defaultValue(true).build());

    // 排序模式
    private final Setting<SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<SortMode>()
        .name("排序模式").description("如何排序要处理的方块").defaultValue(SortMode.Farthest).build());

    // 计时器
    private int tillTimer, plantTimer, harvestTimer;

    // 背包扫描
    private Map<Item, Integer> availableTools;
    private int toolSlotCurrent = -1;
    private Map<Item, Integer> availableSeeds;
    private long lastInventoryScanTime = 0;
    private static final long INVENTORY_SCAN_INTERVAL = 1000; // 1秒扫描一次背包

    // 错误消息控制
    private long lastHoeErrorTime = 0;
    private long lastSeedErrorTime = 0;
    private static final long ERROR_COOLDOWN = 5000; // 5秒冷却时间

    public FarmHelper() {
        super("农场助手", "自动锄地、种植和收割作物");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        // 激活模块时重置计时器和状态
        tillTimer = 0;
        plantTimer = 0;
        harvestTimer = 0;
        availableTools = new HashMap<>();
        availableSeeds = new HashMap<>();
        lastInventoryScanTime = 0;
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        // 每个游戏刻减少计时器
        tillTimer--;
        plantTimer--;
        harvestTimer--;

        // 扫描背包中的工具和种子
        scanInventory();

        // 根据工作模式执行相应操作
        WorkMode mode = workMode.get();

        // 锄地
        if ((mode == WorkMode.All || mode == WorkMode.Till) && tillTimer <= 0) {
            BlockPos tillPos = findTillPosition();
            if (tillPos != null) {
                doTill(tillPos);
            }
            tillTimer = tillDelay.get();
        }

        // 种植
        if ((mode == WorkMode.All || mode == WorkMode.Plant) && plantTimer <= 0) {
            BlockPos plantPos = findPlantPosition();
            if (plantPos != null) {
                doPlant(plantPos);
            }
            plantTimer = plantDelay.get();
        }

        // 收割
        if ((mode == WorkMode.All || mode == WorkMode.Harvest) && harvestTimer <= 0) {
            BlockPos harvestPos = findHarvestPosition();
            if (harvestPos != null) {
                doHarvest(harvestPos);
            }
            harvestTimer = harvestDelay.get();
        }
    }

    // 扫描背包中的工具和种子
    private void scanInventory() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInventoryScanTime < INVENTORY_SCAN_INTERVAL) {
            return;
        }

        availableTools.clear();
        availableSeeds.clear();
        toolSlotCurrent = -1;
        // 扫描锄头
        List<Item> tools = Arrays.asList(Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, Items.GOLDEN_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE);
        for (Item tool : tools) {
            int toolSlot = BagUtil.findItemInventorySlot(tool);
            if (toolSlot != -1) {
                availableTools.put(tool, toolSlot);
                toolSlotCurrent = toolSlot;
            }
        }

        // 扫描种子
        CropType type = cropType.get();
        switch (type) {
            case Wheat:
                int wheatSlot = BagUtil.findItemInventorySlot(Items.WHEAT_SEEDS);
                if (wheatSlot != -1) {
                    availableSeeds.put(Items.WHEAT_SEEDS, wheatSlot);
                }
                break;
            case Carrot:
                int carrotSlot = BagUtil.findItemInventorySlot(Items.CARROT);
                if (carrotSlot != -1) {
                    availableSeeds.put(Items.CARROT, carrotSlot);
                }
                break;
            case Potato:
                int potatoSlot = BagUtil.findItemInventorySlot(Items.POTATO);
                if (potatoSlot != -1) {
                    availableSeeds.put(Items.POTATO, potatoSlot);
                }
                break;
            case Beetroot:
                int beetrootSlot = BagUtil.findItemInventorySlot(Items.BEETROOT_SEEDS);
                if (beetrootSlot != -1) {
                    availableSeeds.put(Items.BEETROOT_SEEDS, beetrootSlot);
                }
                break;
            case Pumpkin:
                int pumpkinSlot = BagUtil.findItemInventorySlot(Items.PUMPKIN_SEEDS);
                if (pumpkinSlot != -1) {
                    availableSeeds.put(Items.PUMPKIN_SEEDS, pumpkinSlot);
                }
                break;
            case Melon:
                int melonSlot = BagUtil.findItemInventorySlot(Items.MELON_SEEDS);
                if (melonSlot != -1) {
                    availableSeeds.put(Items.MELON_SEEDS, melonSlot);
                }
                break;
        }

        lastInventoryScanTime = currentTime;
    }

    // 检查位置附近是否有水
    private boolean hasWaterNearby(BlockPos pos) {
        int radius = 4; // 9x9范围的半径是4

        for (int x = -radius; x <= radius; x++) {
            for (int y = 0; y <= 1; y++) { // 检查自身高度和上一格
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = pos.add(x, y, z);
                    BlockState state = mc.world.getBlockState(checkPos);

                    // 检查是否是水方块、水源、水流或含水方块
                    if (isWaterBlock(state)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean isWaterBlock(BlockState state) {
        Block block = state.getBlock();
        // 检查是否是水方块、水源、水流或含水方块
        if (block == Blocks.WATER ||
            block == Blocks.SEAGRASS ||
            block == Blocks.TALL_SEAGRASS ||
            block == Blocks.KELP ||
            block == Blocks.KELP_PLANT ||
            block == Blocks.BUBBLE_COLUMN ||
            state.getFluidState().isOf(Fluids.WATER)) {
            return true;
        }

        // 可 waterlogged 且已含水
        if (state.contains(WATERLOGGED) && state.get(WATERLOGGED)) {
            return true;
        }
        return false;
    }

    // 检查方块是否可以锄地
    private boolean canTillBlock(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        Block block = state.getBlock();

        // 检查是否是泥土或草方块
        if (block == Blocks.DIRT ||
            block == Blocks.GRASS_BLOCK ||
            block == Blocks.COARSE_DIRT ||
            block == Blocks.PODZOL ||
            block == Blocks.MYCELIUM ||
            block == Blocks.ROOTED_DIRT) {

            // 检查上方是否是空气
            BlockPos abovePos = pos.up();
            BlockState aboveState = mc.world.getBlockState(abovePos);

            if (aboveState.getBlock() == Blocks.AIR) {
                // 检查附近是否有水源
                return hasWaterNearby(pos);
            }
        }

        return false;
    }

    // 检查方块是否是耕地
    private boolean isFarmland(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock() == Blocks.FARMLAND;
    }

    // 检查耕地是否可以种植
    private boolean canPlantOnFarmland(BlockPos pos) {
        // 检查下方是否是耕地
        if (!isFarmland(pos)) {
            return false;
        }

        // 检查上方是否是空气
        BlockPos abovePos = pos.up();
        BlockState aboveState = mc.world.getBlockState(abovePos);

        if (aboveState.getBlock() != Blocks.AIR) {
            return false;
        }
        if (!BaritoneUtil.canSeeBlockFace(pos, Direction.UP)) {
            return false;
        }
        // 检查是否有对应的种子
        CropType type = cropType.get();
        switch (type) {
            case Wheat:
                return availableSeeds.containsKey(Items.WHEAT_SEEDS);
            case Carrot:
                return availableSeeds.containsKey(Items.CARROT);
            case Potato:
                return availableSeeds.containsKey(Items.POTATO);
            case Beetroot:
                return availableSeeds.containsKey(Items.BEETROOT_SEEDS);
            case Pumpkin:
                return availableSeeds.containsKey(Items.PUMPKIN_SEEDS);
            case Melon:
                return availableSeeds.containsKey(Items.MELON_SEEDS);
            default:
                return false;
        }
    }

    // 检查作物是否成熟
    private boolean isCropMature(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof CropBlock) {
            CropBlock crop = (CropBlock) block;
            return crop.isMature(state);
        } else if (block == Blocks.MELON || block == Blocks.PUMPKIN) {
            // 南瓜和西瓜没有成熟度，直接可以收割
            return true;
        } else if (block == Blocks.CARROTS || block == Blocks.POTATOES) {
            // 胡萝卜和土豆
            return state.get(AGE) >= 7;
        } else if (block == Blocks.BEETROOTS) {
            // 甜菜根
            return state.get(BeetrootsBlock.AGE) >= 3;
        }

        return false;
    }

    // 检查方块是否可以收割
    private boolean canHarvestBlock(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        Block block = state.getBlock();

        // 检查是否是作物
        if (block instanceof CropBlock ||
            block == Blocks.MELON ||
            block == Blocks.PUMPKIN ||
            block == Blocks.CARROTS ||
            block == Blocks.POTATOES ||
            block == Blocks.BEETROOTS) {

            // 如果设置了仅收割成熟，则检查是否成熟
            if (harvestOnlyMature.get()) {
                return isCropMature(pos);
            }

            return true;
        }

        return false;
    }

    // 查找要锄地的位置
    private BlockPos findTillPosition() {
        if (availableTools.isEmpty()) {
            return null;
        }

        List<BlockPos> validPositions = new ArrayList<>();
        BlockPos playerPos = mc.player.getBlockPos();
        List<BlockPos> nearbyPositions = WorldUtils.getSphere(playerPos, tillRange.get(), 1);

        // 遍历附近的位置，找到可以锄地的位置
        for (BlockPos pos : nearbyPositions) {
            if (canTillBlock(pos)) {
                validPositions.add(pos);
            }
        }

        if (validPositions.isEmpty()) {
            return null;
        }

        // 根据距离排序
        validPositions.sort(Comparator.comparingDouble(PlayerUtils::distanceTo));

        // 根据排序模式决定使用最近还是最远的位置
        if (sortMode.get() == SortMode.Farthest) {
            Collections.reverse(validPositions);
        }

        return validPositions.get(0);
    }

    // 查找要种植的位置
    private BlockPos findPlantPosition() {
        if (availableSeeds.isEmpty()) {
            return null;
        }

        List<BlockPos> validPositions = new ArrayList<>();
        BlockPos playerPos = mc.player.getBlockPos();
        List<BlockPos> nearbyPositions = WorldUtils.getSphere(playerPos, plantRange.get(), 2);

        // 遍历附近的位置，找到可以种植的位置
        for (BlockPos pos : nearbyPositions) {
            if (canPlantOnFarmland(pos)) {
                validPositions.add(pos);
            }
        }

        if (validPositions.isEmpty()) {
            return null;
        }

        // 根据距离排序
        validPositions.sort(Comparator.comparingDouble(PlayerUtils::distanceTo));

        // 根据排序模式决定使用最近还是最远的位置
        if (sortMode.get() == SortMode.Farthest) {
            Collections.reverse(validPositions);
        }

        return validPositions.get(0);
    }

    // 查找要收割的位置
    private BlockPos findHarvestPosition() {
        List<BlockPos> validPositions = new ArrayList<>();
        BlockPos playerPos = mc.player.getBlockPos();
        List<BlockPos> nearbyPositions = WorldUtils.getSphere(playerPos, harvestRange.get(), 2);

        // 遍历附近的位置，找到可以收割的位置
        for (BlockPos pos : nearbyPositions) {
            if (canHarvestBlock(pos)) {
                validPositions.add(pos);
            }
        }

        if (validPositions.isEmpty()) {
            return null;
        }

        // 根据距离排序
        validPositions.sort(Comparator.comparingDouble(PlayerUtils::distanceTo));

        // 根据排序模式决定使用最近还是最远的位置
        if (sortMode.get() == SortMode.Farthest) {
            Collections.reverse(validPositions);
        }

        return validPositions.get(0);
    }

    // 执行锄地操作
    private void doTill(BlockPos pos) {
        // 获取锄头

        if (toolSlotCurrent == -1) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastHoeErrorTime >= ERROR_COOLDOWN) {
                error("背包中没有找到钻石锄或下界合金锄");
                lastHoeErrorTime = currentTime;
            }
            return;
        }

        // 切换到锄头
        if (autoSwitchHoe.get()) {
            BagUtil.doSwap(toolSlotCurrent);
        }

        // 使用锄头
        BaritoneUtil.clickBlock(pos, Direction.UP, true, Hand.MAIN_HAND, BaritoneUtil.SwingSide.All);

        // 切换回原来的物品
        if (autoSwitchHoe.get()) {
            BagUtil.doSwap(toolSlotCurrent);
        }
    }

    // 执行种植操作
    private void doPlant(BlockPos pos) {
        // 获取种子
        Item seed = null;
        Integer seedSlot = null;

        CropType type = cropType.get();
        switch (type) {
            case Wheat:
                seed = Items.WHEAT_SEEDS;
                break;
            case Carrot:
                seed = Items.CARROT;
                break;
            case Potato:
                seed = Items.POTATO;
                break;
            case Beetroot:
                seed = Items.BEETROOT_SEEDS;
                break;
            case Pumpkin:
                seed = Items.PUMPKIN_SEEDS;
                break;
            case Melon:
                seed = Items.MELON_SEEDS;
                break;
        }

        if (seed != null) {
            seedSlot = availableSeeds.get(seed);
        }

        if (seedSlot == null) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastSeedErrorTime >= ERROR_COOLDOWN) {
                error("背包中没有找到选定的种子");
                lastSeedErrorTime = currentTime;
            }
            return;
        }

        // 切换到种子
        if (autoSwitchSeeds.get()) {
            BagUtil.doSwap(seedSlot);
        }

        // 种植种子（在耕地上方）
        BlockPos plantPos = pos.up();
        BaritoneUtil.clickBlock(plantPos, Direction.UP, true, Hand.MAIN_HAND, BaritoneUtil.SwingSide.All);

        // 切换回原来的物品
        if (autoSwitchSeeds.get()) {
            BagUtil.doSwap(seedSlot);
        }
    }

    // 执行收割操作
    private void doHarvest(BlockPos pos) {
        // 如果有锄头，切换到锄头再收割
        boolean shouldSwitchToHoe = autoSwitchHoe.get() && toolSlotCurrent != -1;

        if (shouldSwitchToHoe) {
            BagUtil.doSwap(toolSlotCurrent);
        }

        // 使用左键破坏作物方块
        BlockUtils.breakBlock(pos, true);

        // 切换回原来的物品
        if (shouldSwitchToHoe) {
            BagUtil.doSwap(toolSlotCurrent);
        }
    }

    // 工作模式枚举
    public enum WorkMode {
        All("全部功能"),
        Till("仅锄地"),
        Plant("仅种植"),
        Harvest("仅收割");

        private final String displayName;

        WorkMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    // 作物类型枚举
    public enum CropType {
        Wheat("小麦"),
        Carrot("胡萝卜"),
        Potato("土豆"),
        Beetroot("甜菜根"),
        Pumpkin("南瓜"),
        Melon("西瓜");

        private final String displayName;

        CropType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    // 排序模式枚举
    public enum SortMode {
        Closest("最近优先"),
        Farthest("最远优先");

        private final String displayName;

        SortMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
