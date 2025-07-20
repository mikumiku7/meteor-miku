package com.github.mikumiku.addon.modules;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import com.github.mikumiku.addon.MikuMikuAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
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
        .min(1)
        .max(128)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("延迟")
        .description("操作之间的延迟（tick）")
        .defaultValue(5)
        .min(0)
        .max(20)
        .build()
    );

    private final Setting<Boolean> autoReturn = sgGeneral.add(new BoolSetting.Builder()
        .name("自动返回")
        .description("完成存储或取工具后自动返回挖掘")
        .defaultValue(true)
        .build()
    );

    // 潜影盒设置
    private final Setting<BlockPos> sandShulkerPos = sgShulkerBoxes.add(new BlockPosSetting.Builder()
        .name("沙子存储潜影盒位置")
        .description("存放沙子的潜影盒坐标")
        .defaultValue(new BlockPos(0, 64, 0))
        .build()
    );

    private final Setting<BlockPos> toolShulkerPos = sgShulkerBoxes.add(new BlockPosSetting.Builder()
        .name("工具潜影盒位置")
        .description("存放铲子的潜影盒坐标")
        .defaultValue(new BlockPos(0, 64, 1))
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
        MINING,           // 正在挖掘
        INVENTORY_FULL,   // 背包满了，需要存储
        TOOL_BROKEN,      // 工具坏了，需要更换
        GOING_TO_STORAGE, // 前往存储潜影盒
        GOING_TO_TOOLS,   // 前往工具潜影盒
        STORING_ITEMS,    // 正在存储物品
        GETTING_TOOLS,    // 正在获取工具
        RETURNING         // 返回挖掘位置
    }

    private MinerState currentState = MinerState.MINING;
    private int tickTimer = 0;
    private BlockPos lastMiningPos = null;
    private BlockPos currentTarget = null;

    public AutoSandMiner() {
        super(MikuMikuAddon.CATEGORY, "auto-sand-miner", "自动挖沙模块，支持背包管理和工具更换");
    }

    @Override
    public void onActivate() {
        if (!BaritoneUtils.IS_AVAILABLE) {
            error("Baritone 不可用！");
            toggle();
            return;
        }

        currentState = MinerState.MINING;
        tickTimer = 0;
        lastMiningPos = null;
        currentTarget = null;

        info("自动挖沙模块已启动");
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
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

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
                info("开始挖掘沙子: " + sandPos.toShortString());
            }
        } else {
            info("附近没有找到沙子");
        }
    }

    private void handleInventoryFull() {
        info("背包已满，前往存储潜影盒");
        currentState = MinerState.GOING_TO_STORAGE;

        BlockPos shulkerPos = new BlockPos(sandShulkerPos.get());
        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(shulkerPos));
    }

    private void handleToolBroken() {
        info("工具耐久度过低，前往工具潜影盒");
        currentState = MinerState.GOING_TO_TOOLS;

        BlockPos toolPos = new BlockPos(toolShulkerPos.get());
        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(toolPos));
    }

    private void handleGoingToStorage() {
        BlockPos shulkerPos = new BlockPos(sandShulkerPos.get());
        if (mc.player.getBlockPos().isWithinDistance(shulkerPos, 2)) {
            currentState = MinerState.STORING_ITEMS;
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        }
    }

    private void handleGoingToTools() {
        BlockPos toolPos = new BlockPos(toolShulkerPos.get());
        if (mc.player.getBlockPos().isWithinDistance(toolPos, 2)) {
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

//        int durability = shovel.().getMaxDamage() - shovel.stack().getDamage();

        return durability <= minDurability.get();
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
}
