package com.github.mikumiku.addon.modules;

import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import baritone.api.pathing.goals.GoalBlock;
import com.github.mikumiku.addon.MikuMikuAddon;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ShovelItem;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class AutoSandMiner extends Module {

    // 设置组
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgShulkerBoxes = settings.createGroup("潜影盒设置");
    private final SettingGroup sgTools = settings.createGroup("工具管理");

    // 基本设置
    private final Setting<Integer> miningRange = sgGeneral.add(new IntSetting.Builder()
        .name("挖掘范围")
        .description("搜索沙子的范围")
        .defaultValue(32)
        .min(4)
        .max(200)
        .sliderMin(8)
        .sliderMax(200)
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
    private final Setting<Integer> minDurability = sgTools.add(new IntSetting.Builder()
        .name("最低耐久度")
        .description("铲子耐久度低于此值时更换")
        .defaultValue(10)
        .min(1)
        .max(100)
        .build()
    );

    private final Setting<List<Item>> preferredShovels = sgTools.add(new ItemListSetting.Builder()
        .name("优先铲子")
        .description("优先使用的铲子类型")
        .defaultValue(Arrays.asList(
            Items.NETHERITE_SHOVEL,
            Items.DIAMOND_SHOVEL,
            Items.IRON_SHOVEL,
            Items.STONE_SHOVEL,
            Items.WOODEN_SHOVEL
        ))
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
    private BlockPos lastMiningPos = null;
    private BlockPos currentTarget = null;
    private BlockPos toolShulkerPos = null; // 用户选择的工具潜影盒位置
    private Set<BlockPos> protectedShulkerBoxes = new HashSet<>(); // 受保护的潜影盒位置
    private int shulkerInteractionTimer = 0; // 潜影盒交互计时器
    private boolean waitingForShulkerOpen = false; // 等待潜影盒打开

    public AutoSandMiner() {
        super(MikuMikuAddon.CATEGORY, "自动挖沙", "自动挖沙模块，支持背包管理和工具更换");
    }

    // 重置工具潜影盒选择的方法
    public void resetToolShulkerSelection() {
        toolShulkerPos = null;
        currentState = MinerState.WAITING_TOOL_SELECTION;
        info("工具潜影盒选择已重置，请重新右键选择工具潜影盒！");
    }

    @Override
    public void onActivate() {
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

        // 扫描并保护所有潜影盒
        scanAndProtectShulkerBoxes();

        info("自动挖沙模块已启动");
        info("请右键点击工具存储潜影盒来选择它！");
    }

    @Override
    public void onDeactivate() {
        // 停止baritone
        if (BaritoneUtils.IS_AVAILABLE) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
            // 清理保护设置
            clearProtectedBlocks();
        }

        protectedShulkerBoxes.clear();
        info("自动挖沙模块已停止");
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

            info("工具潜影盒已选择: " + pos.toShortString());
            info("已保护所有潜影盒免被挖掘");
            info("开始自动挖沙！");
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

        // 寻找沙子并开始挖掘
        BlockPos sandPos = findNearestSand();
        if (sandPos != null) {
            if (currentTarget == null || !currentTarget.equals(sandPos)) {
                currentTarget = sandPos;
                lastMiningPos = mc.player.getBlockPos();

                // 使用baritone挖掘
                BaritoneAPI.getProvider().getPrimaryBaritone().getMineProcess().mine(Blocks.SAND);
//                info("开始挖掘沙子: " + sandPos.toShortString());
            }
        } else {
            info("附近没有找到沙子");
        }
    }

    private void handleInventoryFull() {
        info("背包已满，搜索沙子存储潜影盒");

        BlockPos sandShulker = findSandStorageShulkerBox();
        if (sandShulker != null) {
            currentState = MinerState.GOING_TO_STORAGE;
//            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();

            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(sandShulker));
            info("找到沙子存储潜影盒: " + sandShulker.toShortString());
        } else {
            error("未找到沙子存储潜影盒！");
            currentState = MinerState.MINING;
        }
    }

    private void handleToolBroken() {
        info("工具耐久度过低，前往工具潜影盒");

        if (toolShulkerPos != null) {
            currentState = MinerState.GOING_TO_TOOLS;
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(toolShulkerPos));
            info("前往工具存储潜影盒: " + toolShulkerPos.toShortString());
        } else {
            error("工具潜影盒位置未设置！请重新启动模块并选择工具潜影盒。");
            currentState = MinerState.WAITING_TOOL_SELECTION;
            info("请右键点击工具存储潜影盒来选择它！");
        }
    }

    private void handleGoingToStorage() {
        // 检查是否到达任何潜影盒附近
        BlockPos nearbyShulker = findNearbyShulkerBox();
        if (nearbyShulker != null && mc.player.getBlockPos().isWithinDistance(nearbyShulker, 4)) {
            currentState = MinerState.STORING_ITEMS;
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        }
    }

    private void handleGoingToTools() {
        // 检查是否到达工具潜影盒附近
        if (toolShulkerPos != null && mc.player.getBlockPos().isWithinDistance(toolShulkerPos, 4)) {
            currentState = MinerState.GETTING_TOOLS;
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        }
    }

    private void handleStoringItems() {
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
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
            info("已返回挖掘位置");
        }
    }

    private boolean isInventoryFull() {
        int emptySlots = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                emptySlots++;
            }
        }
        return emptySlots <= 2; // 保留2个空槽位
    }

    private boolean needNewTool() {
        FindItemResult shovel = InvUtils.find(itemStack -> itemStack.getItem() instanceof ShovelItem);
        if (!shovel.found()) return true;

        // 获取物品堆栈
        var itemStack = mc.player.getInventory().getStack(shovel.slot());

        // 检查物品是否可损坏
        if (!itemStack.isDamageable()) return false;

        // 计算剩余耐久度
        int maxDamage = itemStack.getMaxDamage();
        int currentDamage = itemStack.getDamage();
        int remainingDurability = maxDamage - currentDamage;

        return remainingDurability <= minDurability.get();
    }

    private BlockPos findNearestSand() {
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos nearestSand = null;
        double nearestDistance = Double.MAX_VALUE;

        int range = miningRange.get();
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (mc.world.getBlockState(pos).getBlock() == Blocks.SAND) {
                        double distance = playerPos.getSquaredDistance(pos);
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestSand = pos;
                        }
                    }
                }
            }
        }

        return nearestSand;
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

        info("已扫描到 " + protectedShulkerBoxes.size() + " 个潜影盒，将被保护");
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

        int sandMoved = 0;
        int maxMovesPerTick = 3; // 每tick最多移动3组物品，防止操作过快

        // 遍历玩家背包，寻找沙子
        for (int i = 0; i < mc.player.getInventory().size() && sandMoved < maxMovesPerTick; i++) {
            var stack = mc.player.getInventory().getStack(i);

            if (!stack.isEmpty() && stack.getItem() == Items.SAND) {
                // 寻找潜影盒中的空槽位或相同物品槽位
                int targetSlot = findShulkerSlotForItem(stack);

                if (targetSlot != -1) {
                    // 移动物品到潜影盒
                    moveItemToShulker(i, targetSlot);
                    sandMoved++;
                    info("移动沙子到潜影盒，数量: " + stack.getCount());
                }
            }
        }

        // 如果没有更多沙子需要存储，关闭潜影盒并继续
        if (sandMoved == 0 || !hasMoreSandToStore()) {
            closeShulkerBox();

            if (autoReturn.get() && lastMiningPos != null) {
                currentState = MinerState.RETURNING;
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(lastMiningPos));
            } else {
                currentState = MinerState.MINING;
            }

            info("沙子存储完成");
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
            warning("潜影盒中没有找到可用的铲子！");
            closeShulkerBox();
            currentState = MinerState.MINING;
        }
    }

    private int findShulkerSlotForItem(net.minecraft.item.ItemStack stack) {
        if (!(mc.currentScreen instanceof ShulkerBoxScreen)) {
            return -1;
        }

        var handler = mc.player.currentScreenHandler;

        // 潜影盒槽位通常是前27个槽位（0-26）
        for (int i = 0; i < 27; i++) {
            if (i >= handler.slots.size()) break;

            var slotStack = handler.getSlot(i).getStack();

            // 寻找空槽位或相同物品的槽位
            if (slotStack.isEmpty() ||
                (slotStack.getItem() == stack.getItem() && slotStack.getCount() < slotStack.getMaxCount())) {
                return i;
            }
        }

        return -1;
    }

    private int findToolInShulker() {
        if (!(mc.currentScreen instanceof ShulkerBoxScreen)) {
            return -1;
        }

        var handler = mc.player.currentScreenHandler;

        // 按优先级顺序寻找工具
        for (Item preferredTool : preferredShovels.get()) {
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
        // 首先寻找损坏的工具槽位
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            var stack = mc.player.getInventory().getStack(i);

            if (!stack.isEmpty() && stack.getItem() instanceof ShovelItem) {
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

    private boolean hasMoreSandToStore() {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            var stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.SAND) {
                return true;
            }
        }
        return false;
    }

    private void closeShulkerBox() {
        if (mc.currentScreen instanceof ShulkerBoxScreen) {
            mc.player.closeHandledScreen();
            info("关闭潜影盒");
        }
    }

    private boolean isShulkerBoxOpen() {
        // 使用正确的方法检测潜影盒界面
        return mc.currentScreen instanceof ShulkerBoxScreen;
    }
}
