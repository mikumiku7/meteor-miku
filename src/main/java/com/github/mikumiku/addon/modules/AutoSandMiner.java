package com.github.mikumiku.addon.modules;

import baritone.api.BaritoneAPI;
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
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.List;

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

        info("自动挖沙模块已启动");
        info("请右键点击工具存储潜影盒来选择它！");
    }

    @Override
    public void onDeactivate() {
        // 停止baritone
        if (BaritoneUtils.IS_AVAILABLE) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        }

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

            // 取消交互事件，防止打开潜影盒
            event.cancel();

            info("工具潜影盒已选择: " + pos.toShortString());
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
        if (nearbyShulker != null && mc.player.getBlockPos().isWithinDistance(nearbyShulker, 2)) {
            currentState = MinerState.STORING_ITEMS;
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        }
    }

    private void handleGoingToTools() {
        // 检查是否到达工具潜影盒附近
        if (toolShulkerPos != null && mc.player.getBlockPos().isWithinDistance(toolShulkerPos, 2)) {
            currentState = MinerState.GETTING_TOOLS;
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        }
    }

    private void handleStoringItems() {
        // 这里应该实现打开潜影盒并存储沙子的逻辑
        // 由于复杂性，这里简化处理
        info("正在存储沙子...");

        // 模拟存储完成
        if (autoReturn.get() && lastMiningPos != null) {
            currentState = MinerState.RETURNING;
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(lastMiningPos));
        } else {
            currentState = MinerState.MINING;
        }
    }

    private void handleGettingTools() {
        // 这里应该实现打开潜影盒并获取新工具的逻辑
        info("正在获取新工具...");

        // 模拟获取完成
        if (autoReturn.get() && lastMiningPos != null) {
            currentState = MinerState.RETURNING;
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(lastMiningPos));
        } else {
            currentState = MinerState.MINING;
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
}
