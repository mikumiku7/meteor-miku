package com.github.mikumiku.addon.modules;

import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import baritone.api.pathing.goals.GoalBlock;
import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.ChatUtils;
import com.github.mikumiku.addon.util.PositionCache;
import com.github.mikumiku.addon.util.timer.SyncedTickTimer;
import com.github.mikumiku.addon.util.timer.Timers;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class AutoMiner extends BaseModule {

    // 工具类型枚举
    public enum ToolType {
        SHOVEL("铲子"),
        PICKAXE("镐子"),
        AXE("斧子"),
        HOE("锄头"),
        ;

        private final String displayName;

        ToolType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }


        @Override
        public String toString() {
            return displayName;
        }
    }

    // 设置组
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgShulkerBoxes = settings.createGroup("潜影盒设置");
    private final SettingGroup sgTools = settings.createGroup("工具管理");

    // 基本设置
    private final Setting<List<Block>> targetBlocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("挖掘目标")
        .description("要挖掘的方块类型 多选")
        .defaultValue(Arrays.asList(
            Blocks.SAND
        ))
        .build()
    );

    // 基本设置
    private final Setting<Integer> miningRange = sgGeneral.add(new IntSetting.Builder()
        .name("挖掘范围")
        .description("搜索目标方块的范围")
        .defaultValue(32)
        .min(4)
        .max(200)
        .sliderMin(8)
        .sliderMax(200)
        .build()
    );


    private final Setting<Boolean> packetMine = sgGeneral.add(new BoolSetting.Builder()
        .name("使用极速包挖")
        .description("使用包挖乱挖周围目标方块，可能捡不到")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("延迟")
        .description("操作之间的延迟（tick）")
        .defaultValue(5)
        .min(0)
        .max(20)
        .sliderMin(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> autoReturn = sgGeneral.add(new BoolSetting.Builder()
        .name("自动返回")
        .description("完成存储或取工具后自动返回挖掘")
        .defaultValue(true)
        .build()
    );

    // 潜影盒设置
    private final Setting<Integer> shulkerSearchRadius = sgShulkerBoxes.add(new IntSetting.Builder()
        .name("潜影盒搜索半径")
        .description("搜索潜影盒的半径范围")
        .defaultValue(32)
        .min(4)
        .max(200)
        .sliderMin(8)
        .sliderMax(200)
        .build()
    );

    // 工具管理设置
    private final Setting<ToolType> toolType = sgTools.add(new EnumSetting.Builder<ToolType>()
        .name("工具类型")
        .description("选择使用的工具类型")
        .defaultValue(ToolType.SHOVEL)
        .build()
    );

    private final Setting<Integer> minDurability = sgTools.add(new IntSetting.Builder()
        .name("最低耐久度")
        .description("工具耐久度低于此值时更换")
        .defaultValue(10)
        .min(1)
        .max(100)
        .build()
    );

    // 状态变量
    private enum MinerState {
        WAITING_TOOL_SELECTION, // 等待用户选择工具潜影盒
        MINING,                 // 正在挖掘
        WAITING_FOR_TARGET_BOX,   // 等待目标潜影盒
        INVENTORY_FULL,         // 背包满了，需要存储
        TOOL_BROKEN,            // 工具坏了，需要更换
        GOING_TO_STORAGE,       // 前往存储潜影盒
        GOING_TO_TOOLS,         // 前往工具潜影盒
        STORING_ITEMS,          // 正在存储物品
        GETTING_TOOLS,          // 正在获取工具
        RETURNING               // 返回挖掘位置
    }

    private MinerState currentState = MinerState.WAITING_TOOL_SELECTION;
    private int tickTimer = 0;
    private SyncedTickTimer cacheClearTimer = Timers.tickTimer();
    private BlockPos lastMiningPos = null;
    private BlockPos currentTarget = null;
    private BlockPos toolShulkerPos = null; // 用户选择的工具潜影盒位置
    private Set<BlockPos> protectedShulkerBoxes = new HashSet<>(); // 受保护的潜影盒位置
    private int shulkerInteractionTimer = 0; // 潜影盒交互计时器
    private boolean waitingForShulkerOpen = false; // 等待潜影盒打开


    /**
     * 位置缓存管理器，支持自动过期清理
     */
    private final PositionCache positionCache = new PositionCache(5000L);

    public AutoMiner() {
        super(CATEGORY_MIKU_BUILD,"挖沙挖一切", "自动挖掘指定方块，支持背包管理和工具更换");
    }

    // 重置工具潜影盒选择的方法
    public void resetToolShulkerSelection() {
        toolShulkerPos = null;
        currentState = MinerState.WAITING_TOOL_SELECTION;
        ChatUtils.sendMsg("工具潜影盒选择已重置，请重新右键选择工具潜影盒！");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        if (!BaritoneUtils.IS_AVAILABLE) {
            error("Baritone 不可用！");
            toggle();
            return;
        }

        currentState = MinerState.WAITING_TOOL_SELECTION;
        tickTimer = 0;
        lastMiningPos = null;
        currentTarget = null;
        toolShulkerPos = null;
        protectedShulkerBoxes.clear();
        shulkerInteractionTimer = 0;
        waitingForShulkerOpen = false;
        positionCache.startCleanupThread();

        // 扫描并保护所有潜影盒
        scanAndProtectShulkerBoxes();

        ChatUtils.sendMsg("自动挖掘模块已启动");
        ChatUtils.sendMsg("请右键点击工具存储潜影盒来选择它！");
    }

    @Override
    public void onDeactivate() {
        // 停止baritone
        if (BaritoneUtils.IS_AVAILABLE) {
            cancelBaritone();
            // 清理保护设置
            clearProtectedBlocks();
        }
        positionCache.shutdown(); // 关闭缓存管理器

        protectedShulkerBoxes.clear();
        ChatUtils.sendMsg("自动挖掘模块已停止");
    }

    @EventHandler
    private void onInteractBlock(InteractBlockEvent event) {
        if (currentState != MinerState.WAITING_TOOL_SELECTION) return;

        BlockPos pos = event.result.getBlockPos();
        Block block = mc.world.getBlockState(pos).getBlock();

        if (block instanceof ShulkerBoxBlock) {
            toolShulkerPos = pos;
            currentState = MinerState.MINING;

            // 更新保护设置
            updateProtectedBlocks();

            // 取消交互事件，防止打开潜影盒
            event.cancel();

            ChatUtils.sendMsg("工具潜影盒已选择: " + pos.toShortString());
            ChatUtils.sendMsg("已保护所有潜影盒免被挖掘");
            ChatUtils.sendMsg("开始自动挖掘！");
        } else {
            warning("请右键点击潜影盒来选择工具存储位置！");
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // 如果还在等待选择工具潜影盒，不执行其他逻辑
        if (currentState == MinerState.WAITING_TOOL_SELECTION) {
            return;
        }

        // 缓存清理现在由后台线程自动处理
        // if (cacheClearTimer.tick(500, true)) {
        //     cleanExpiredCache();
        // }
        // 延迟控制
        if (tickTimer > 0) {
            tickTimer--;
            return;
        }

        switch (currentState) {
            case MINING -> handleMining();
            case INVENTORY_FULL -> handleInventoryFull();
            case TOOL_BROKEN -> handleToolBroken();
            case GOING_TO_STORAGE -> handleGoingToStorage();
            case GOING_TO_TOOLS -> handleGoingToTools();
            case STORING_ITEMS -> handleStoringItems();
            case GETTING_TOOLS -> handleGettingTools();
            case RETURNING -> handleReturning();
        }

        tickTimer = delay.get();
    }


    private void handleMining() {
        // 检查背包是否满了
        if (isInventoryFull()) {
            currentState = MinerState.INVENTORY_FULL;
            return;
        }

        // 检查工具是否需要更换
        if (needNewTool()) {
            currentState = MinerState.TOOL_BROKEN;
            return;
        }

        // 寻找目标方块并开始挖掘
        BlockPos targetPos = findNearestTargetBlock();
        if (targetPos != null) {
            if (currentTarget == null || !currentTarget.equals(targetPos)) {

                if (packetMine.get() && !positionCache.isInCache(targetPos)) {
                    // 只挖掘高于玩家的方块
                    BlockPos playerPos = mc.player.getBlockPos();

                    // 计算到玩家的距离
                    double distance = Math.sqrt(
                        Math.pow(targetPos.getX() - playerPos.getX(), 2) +
                            Math.pow(targetPos.getY() - playerPos.getY(), 2) +
                            Math.pow(targetPos.getZ() - playerPos.getZ(), 2)
                    );

                    if (targetPos.getY() >= playerPos.getY() && distance < 4.2) {

                        // 将该位置加入缓存
                        positionCache.addToCache(targetPos);
                        BlockUtils.breakBlock(targetPos, true);
                    }

                }

                currentTarget = targetPos;
                lastMiningPos = mc.player.getBlockPos();

                // 使用baritone挖掘
                Block targetBlock = mc.world.getBlockState(targetPos).getBlock();

                BaritoneAPI.getProvider()
                    .getPrimaryBaritone()
                    .getMineProcess()
                    .mine(targetBlocks.get().toArray(new Block[0]));

            }
        } else {
            ChatUtils.sendMsg("附近没有找到目标方块");
        }
    }

    private void handleInventoryFull() {
        ChatUtils.sendMsg("背包已满，搜索存储潜影盒");

        BlockPos storageShulker = findSandStorageShulkerBox();
        if (storageShulker != null) {
            currentState = MinerState.GOING_TO_STORAGE;
            cancelBaritone();

            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(storageShulker));
            ChatUtils.sendMsg("找到存储潜影盒: " + storageShulker.toShortString());
        } else {
            error("未找到存储潜影盒！");
            currentState = MinerState.MINING;
        }
    }

    private void handleToolBroken() {
        ToolType selectedToolType = toolType.get();
        ChatUtils.sendMsg("工具耐久度过低，前往工具潜影盒获取" + selectedToolType.getDisplayName());

        if (toolShulkerPos != null) {
            currentState = MinerState.GOING_TO_TOOLS;
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(toolShulkerPos));
            ChatUtils.sendMsg("前往工具存储潜影盒: " + toolShulkerPos.toShortString());
        } else {
            error("工具潜影盒位置未设置！请重新启动模块并选择工具潜影盒。");
            currentState = MinerState.WAITING_TOOL_SELECTION;
            ChatUtils.sendMsg("请右键点击工具存储潜影盒来选择它！");
        }
    }

    private void handleGoingToStorage() {
        // 检查是否到达任何潜影盒附近
        BlockPos nearbyShulker = findNearbyShulkerBox();
        if (nearbyShulker != null && mc.player.getBlockPos().isWithinDistance(nearbyShulker, 4)) {
            currentState = MinerState.STORING_ITEMS;
            cancelBaritone();
        }
    }

    private void handleGoingToTools() {
        // 检查是否到达工具潜影盒附近
        if (toolShulkerPos != null && mc.player.getBlockPos().isWithinDistance(toolShulkerPos, 4)) {
            currentState = MinerState.GETTING_TOOLS;
            cancelBaritone();
        }
    }

    private void handleStoringItems() {
        // 检查当前屏幕是否是潜影盒界面
        if (isShulkerBoxOpen()) {
            // 存储沙子到潜影盒
            storeSandToShulkerBox();
            return;
        }
        // 检查是否需要等待潜影盒打开
        if (waitingForShulkerOpen) {
            shulkerInteractionTimer++;
            if (shulkerInteractionTimer > 40) { // 等待2秒
                waitingForShulkerOpen = false;
                shulkerInteractionTimer = 0;
                warning("潜影盒打开超时，重试中...");
            } else {
                return;
            }
        }

        // 检查当前屏幕是否是潜影盒界面
        if (isShulkerBoxOpen()) {
            // 存储沙子到潜影盒
            storeSandToShulkerBox();
        } else {
            // 尝试打开附近的沙子存储潜影盒
            BlockPos nearbyShulker = findNearbyShulkerBox();
            if (nearbyShulker != null) {
                openShulkerBox(nearbyShulker);
                waitingForShulkerOpen = true;
                shulkerInteractionTimer = 0;
            } else {
                error("附近没有找到潜影盒！");
                currentState = MinerState.MINING;
            }
        }
    }

    private void handleGettingTools() {
        // 检查当前屏幕是否是潜影盒界面
        if (isShulkerBoxOpen()) {
            // 从潜影盒获取工具
            getToolFromShulkerBox();
            return;
        }

        // 检查是否需要等待潜影盒打开
        if (waitingForShulkerOpen) {
            shulkerInteractionTimer++;
            if (shulkerInteractionTimer > 40) { // 等待2秒
                waitingForShulkerOpen = false;
                shulkerInteractionTimer = 0;
                warning("工具潜影盒打开超时，重试中...");
            } else {
                return;
            }
        }

        // 检查当前屏幕是否是潜影盒界面
        if (isShulkerBoxOpen()) {
            // 从潜影盒获取工具
            getToolFromShulkerBox();
        } else {
            // 尝试打开工具潜影盒
            if (toolShulkerPos != null) {
                openShulkerBox(toolShulkerPos);
                waitingForShulkerOpen = true;
                shulkerInteractionTimer = 0;
            } else {
                error("工具潜影盒位置未设置！");
                currentState = MinerState.WAITING_TOOL_SELECTION;
            }
        }
    }

    private void handleReturning() {
        if (lastMiningPos != null && mc.player.getBlockPos().isWithinDistance(lastMiningPos, 3)) {
            currentState = MinerState.MINING;
            cancelBaritone();
            info("已返回挖掘位置");
        }
    }

    private boolean isInventoryFull() {

        int emptySlots = 0;
        for (int i = 9; i < 36; i++) { // Main inventory slots only
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                emptySlots++;
            }
        }
        return emptySlots <= 2; // 保留2个空槽位
    }

    private boolean needNewTool() {
        ToolType selectedToolType = toolType.get();
        List<Item> toolPriority = getToolPriorityList(selectedToolType);

        FindItemResult tool = InvUtils.find(itemStack -> toolPriority.contains(itemStack.getItem()));
        if (!tool.found()) return true;

        // 获取物品堆栈
        var itemStack = mc.player.getInventory().getStack(tool.slot());

        // 检查物品是否可损坏
        if (!itemStack.isDamageable()) return false;

        // 计算剩余耐久度
        int maxDamage = itemStack.getMaxDamage();
        int currentDamage = itemStack.getDamage();
        int remainingDurability = maxDamage - currentDamage;

        return remainingDurability <= minDurability.get();
    }

    private BlockPos findNearestTargetBlock() {
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos nearestBlock = null;
        double nearestDistance = Double.MAX_VALUE;

        List<Block> targets = targetBlocks.get();
        int range = miningRange.get();

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();

                    if (targets.contains(block)) {
                        double distance = playerPos.getSquaredDistance(pos);
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestBlock = pos;
                        }
                    }
                }
            }
        }

        return nearestBlock;
    }

    private BlockPos findSandStorageShulkerBox() {
        BlockPos playerPos = mc.player.getBlockPos();
        int radius = shulkerSearchRadius.get();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();

                    // 找到潜影盒，但不是工具潜影盒
                    if (block instanceof ShulkerBoxBlock && !pos.equals(toolShulkerPos)) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }


    private BlockPos findNearbyShulkerBox() {
        BlockPos playerPos = mc.player.getBlockPos();

        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();

                    if (block instanceof ShulkerBoxBlock) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    private void scanAndProtectShulkerBoxes() {
        protectedShulkerBoxes.clear();
        BlockPos playerPos = mc.player.getBlockPos();
        int radius = shulkerSearchRadius.get();

        // 扫描范围内的所有潜影盒
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();

                    if (block instanceof ShulkerBoxBlock) {
                        protectedShulkerBoxes.add(pos);
                    }
                }
            }
        }

        ChatUtils.sendMsg("已扫描到 " + protectedShulkerBoxes.size() + " 个潜影盒，将被保护");
    }

    private void updateProtectedBlocks() {
        if (!BaritoneUtils.IS_AVAILABLE) return;

        try {
            Settings settings = BaritoneAPI.getSettings();


            // 将所有潜影盒添加到不可破坏的方块列表
            List<Block> blocksToAvoid = new ArrayList<>();

            // 添加所有潜影盒类型
            blocksToAvoid.add(Blocks.SHULKER_BOX);
            blocksToAvoid.add(Blocks.WHITE_SHULKER_BOX);
            blocksToAvoid.add(Blocks.ORANGE_SHULKER_BOX);
            blocksToAvoid.add(Blocks.MAGENTA_SHULKER_BOX);
            blocksToAvoid.add(Blocks.LIGHT_BLUE_SHULKER_BOX);
            blocksToAvoid.add(Blocks.YELLOW_SHULKER_BOX);
            blocksToAvoid.add(Blocks.LIME_SHULKER_BOX);
            blocksToAvoid.add(Blocks.PINK_SHULKER_BOX);
            blocksToAvoid.add(Blocks.GRAY_SHULKER_BOX);
            blocksToAvoid.add(Blocks.LIGHT_GRAY_SHULKER_BOX);
            blocksToAvoid.add(Blocks.CYAN_SHULKER_BOX);
            blocksToAvoid.add(Blocks.PURPLE_SHULKER_BOX);
            blocksToAvoid.add(Blocks.BLUE_SHULKER_BOX);
            blocksToAvoid.add(Blocks.BROWN_SHULKER_BOX);
            blocksToAvoid.add(Blocks.GREEN_SHULKER_BOX);
            blocksToAvoid.add(Blocks.RED_SHULKER_BOX);
            blocksToAvoid.add(Blocks.BLACK_SHULKER_BOX);


            // 设置不可破坏的方块
            settings.blocksToDisallowBreaking.value = blocksToAvoid;

            info("已设置 Baritone 保护所有潜影盒");

        } catch (Exception e) {
            warning("设置 Baritone 保护失败: " + e.getMessage());
        }
    }

    private void clearProtectedBlocks() {
        if (!BaritoneUtils.IS_AVAILABLE) return;

        try {
            var settings = BaritoneAPI.getSettings();

            // 清空不可破坏的方块列表
            settings.blocksToDisallowBreaking.value = new java.util.ArrayList<>();

        } catch (Exception e) {
            warning("清理 Baritone 保护设置失败: " + e.getMessage());
        }
    }

    private void openShulkerBox(BlockPos pos) {
        if (mc.interactionManager == null || mc.player == null) return;

        try {
            // 确保玩家在潜影盒附近
            if (!mc.player.getBlockPos().isWithinDistance(pos, 5)) {
                warning("距离潜影盒太远，无法打开");
                return;
            }

            // 确保目标位置确实是潜影盒
            Block block = mc.world.getBlockState(pos).getBlock();
            if (!(block instanceof ShulkerBoxBlock)) {
                warning("目标位置不是潜影盒: " + pos.toShortString());
                return;
            }

            // 计算点击位置
            Vec3d hitVec = Vec3d.ofCenter(pos);
            Direction side = Direction.UP; // 默认从上方点击

            // 创建方块命中结果
            BlockHitResult hitResult = new BlockHitResult(hitVec, side, pos, false);

            // 右键点击潜影盒
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);

            info("正在打开潜影盒: " + pos.toShortString());

        } catch (Exception e) {
            error("打开潜影盒时发生错误: " + e.getMessage());
        }
    }

    private void storeSandToShulkerBox() {
        if (mc.interactionManager == null) return;

        int itemsMoved = 0;
        int maxMovesPerTick = 27; // 每tick最多移动x组物品，防止操作过快
        List<Block> targets = targetBlocks.get();
        boolean hasMovedItems = false;

        // 遍历玩家背包，寻找目标方块
        for (int i = 9; i < 36; i++) {
            var stack = mc.player.getInventory().getStack(i);

            if (!stack.isEmpty()) {
                boolean isTargetBlock = false;
                for (Block targetBlock : targets) {
                    if (stack.getItem() == targetBlock.asItem()) {
                        isTargetBlock = true;
                        break;
                    }
                }

                if (isTargetBlock) {
                    // 寻找潜影盒中的空槽位或相同物品槽位
                    int targetSlot = findShulkerSlotForItem(stack);

                    if (targetSlot != -1) {
                        // 移动物品到潜影盒
                        moveItemToShulker(i, targetSlot);
                        itemsMoved++;
                        hasMovedItems = true;
                        info("移动" + stack.getItem().getName().getString() + "到潜影盒，数量: " + stack.getCount());

                        if (itemsMoved >= maxMovesPerTick) {
                            break; // 本tick达到最大操作数，下个tick继续
                        }
                    } else {
                        // 当前潜影盒满了，寻找下一个潜影盒
                        BlockPos nextShulker = findNextEmptyShulkerBox();
                        if (nextShulker != null) {
                            closeShulkerBox();
                            openShulkerBox(nextShulker);
                            waitingForShulkerOpen = true;
                            shulkerInteractionTimer = 0;
                            return; // 等待潜影盒打开
                        } else {
                            // 没有找到更多潜影盒，存储完成
                            closeShulkerBox();

                            if (autoReturn.get() && lastMiningPos != null) {
                                currentState = MinerState.RETURNING;
                                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(lastMiningPos));
                            } else {
                                currentState = MinerState.MINING;
                            }

                            info("物品存储完成");
                            return;
                        }
                    }
                }
            }
        }

        // 如果没有更多物品需要存储，关闭潜影盒并继续
        if (!hasMoreTargetBlocksToStore()) {
            closeShulkerBox();

            if (autoReturn.get() && lastMiningPos != null) {
                currentState = MinerState.RETURNING;
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(lastMiningPos));
            } else {
                currentState = MinerState.MINING;
            }

            info("物品存储完成");
        }
    }

    private void getToolFromShulkerBox() {
        if (mc.interactionManager == null) return;

        // 寻找潜影盒中的铲子
        int toolSlot = findToolInShulker();

        if (toolSlot != -1) {
            // 寻找玩家背包中的空槽位或损坏的工具槽位
            int targetSlot = findPlayerSlotForTool();

            if (targetSlot != -1) {
                // 移动工具到玩家背包
                moveItemFromShulker(toolSlot, targetSlot);
                info("获取新工具成功");

                // 关闭潜影盒并继续
                closeShulkerBox();

                if (autoReturn.get() && lastMiningPos != null) {
                    currentState = MinerState.RETURNING;
                    BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(lastMiningPos));
                } else {
                    currentState = MinerState.MINING;
                }
            } else {
                warning("背包已满，无法获取新工具！");
                closeShulkerBox();
                currentState = MinerState.MINING;
            }
        } else {
            ToolType selectedToolType = toolType.get();
            warning("潜影盒中没有找到可用的" + selectedToolType.getDisplayName() + "！");
            closeShulkerBox();
            currentState = MinerState.MINING;
        }
    }

    /**
     * 寻找下一个未满的潜影盒用于存储物品
     *
     * @return 找到的潜影盒位置，如果没有找到返回null
     */
    private BlockPos findNextEmptyShulkerBox() {
        BlockPos playerPos = mc.player.getBlockPos();
        int radius = shulkerSearchRadius.get();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();

                    // 找到潜影盒，但不是工具潜影盒
                    if (block instanceof ShulkerBoxBlock && !pos.equals(toolShulkerPos)) {
                        // 可以添加额外逻辑检查潜影盒是否有空间
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    private int findShulkerSlotForItem(ItemStack stack) {
        if (!(mc.currentScreen instanceof ShulkerBoxScreen)) {
            return -1;
        }


        var handler = mc.player.currentScreenHandler;

        // 潜影盒槽位通常是前27个槽位（0-26）
        for (int i = 0; i < 27; i++) {
            if (i >= handler.slots.size()) break;

            var slotStack = handler.getSlot(i).getStack();

            // 寻找空槽位或相同物品的槽位
            if (slotStack.isEmpty()) {
                return i; // 优先返回完全空的槽位
            } else if (slotStack.getItem() == stack.getItem() && slotStack.getCount() < slotStack.getMaxCount()) {
                return i;
            }
        }

        return -1; // 潜影盒已满
    }

    private int findToolInShulker() {
        if (!(mc.currentScreen instanceof ShulkerBoxScreen)) {
            return -1;
        }

        var handler = mc.player.currentScreenHandler;
        ToolType selectedToolType = toolType.get();

        // 按材质优先级顺序寻找工具（下界合金 > 钻石 > 铁 > 石头 > 木头）
        List<Item> toolPriority = getToolPriorityList(selectedToolType);

        for (Item preferredTool : toolPriority) {
            for (int i = 0; i < 27; i++) {
                if (i >= handler.slots.size()) break;

                var slotStack = handler.getSlot(i).getStack();

                if (!slotStack.isEmpty() && slotStack.getItem() == preferredTool) {
                    // 检查工具耐久度
                    if (!slotStack.isDamageable() ||
                        (slotStack.getMaxDamage() - slotStack.getDamage()) > minDurability.get()) {
                        return i;
                    }
                }
            }
        }

        return -1;
    }

    private int findPlayerSlotForTool() {
        ToolType selectedToolType = toolType.get();

        // 首先寻找损坏的工具槽位
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            var stack = mc.player.getInventory().getStack(i);


            List<Item> toolPriority = getToolPriorityList(selectedToolType);

            if (!stack.isEmpty() && toolPriority.contains(stack.getItem())) {
                if (stack.isDamageable() &&
                    (stack.getMaxDamage() - stack.getDamage()) <= minDurability.get()) {
                    return i + 27; // 转换为屏幕槽位索引
                }
            }
        }

        // 然后寻找空槽位
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                return i + 27; // 转换为屏幕槽位索引
            }
        }

        return -1;
    }

    private void moveItemToShulker(int playerSlot, int shulkerSlot) {
        if (mc.interactionManager == null) return;

        // 转换玩家槽位索引
        int screenSlot = playerSlot + 27;

        // Shift+左键快速移动
        mc.interactionManager.clickSlot(
            mc.player.currentScreenHandler.syncId,
            screenSlot,
            0,
            SlotActionType.QUICK_MOVE,
            mc.player
        );
    }

    private void moveItemFromShulker(int shulkerSlot, int playerSlot) {
        if (mc.interactionManager == null) return;

        // Shift+左键快速移动
        mc.interactionManager.clickSlot(
            mc.player.currentScreenHandler.syncId,
            shulkerSlot,
            0,
            SlotActionType.QUICK_MOVE,
            mc.player
        );
    }

    private boolean hasMoreTargetBlocksToStore() {
        List<Block> targets = targetBlocks.get();

        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            var stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                for (Block targetBlock : targets) {
                    if (stack.getItem() == targetBlock.asItem()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void cancelBaritone() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();

    }

    private void closeShulkerBox() {
        if (mc.currentScreen instanceof ShulkerBoxScreen) {
            mc.player.closeHandledScreen();
            ChatUtils.sendMsg("关闭潜影盒");
        }
    }

    private boolean isShulkerBoxOpen() {

        // 使用正确的方法检测潜影盒界面
        return mc.currentScreen instanceof ShulkerBoxScreen || mc.currentScreen instanceof HandledScreen;
    }

    private List<Item> getToolPriorityList(ToolType toolType) {
        return switch (toolType) {
            case SHOVEL -> Arrays.asList(
                Items.NETHERITE_SHOVEL,
                Items.DIAMOND_SHOVEL,
                Items.IRON_SHOVEL,
                Items.STONE_SHOVEL,
                Items.WOODEN_SHOVEL
            );
            case PICKAXE -> Arrays.asList(
                Items.NETHERITE_PICKAXE,
                Items.DIAMOND_PICKAXE,
                Items.IRON_PICKAXE,
                Items.STONE_PICKAXE,
                Items.WOODEN_PICKAXE
            );
            case AXE -> Arrays.asList(
                Items.NETHERITE_AXE,
                Items.DIAMOND_AXE,
                Items.IRON_AXE,
                Items.STONE_AXE,
                Items.WOODEN_AXE
            );
            case HOE -> Arrays.asList(
                Items.NETHERITE_HOE,
                Items.DIAMOND_HOE,
                Items.IRON_HOE,
                Items.STONE_HOE,
                Items.WOODEN_HOE
            );
        };
    }
}
