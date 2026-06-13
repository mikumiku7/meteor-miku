package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.*;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;

public class BestPrinter extends BaseModule {

    // ==================== 设置组 ====================
    private final SettingGroup generalSettings = settings.getDefaultGroup();
    private final SettingGroup slabSettings = settings.createGroup("特殊方块设置");
    private final SettingGroup substituteSettings = settings.createGroup("替补方块");
    private final SettingGroup renderSettings = settings.createGroup("渲染");

    // ==================== 放置相关设置 ====================

    /**
     * 放置模式：快速模式或合法模式
     */
    private final Setting<PlaceMode> placeMode = generalSettings.add(
        new EnumSetting.Builder<PlaceMode>()
            .name("放置模式")
            .description("决定打印方块时的放置逻辑：严格或合法模式。")
            .defaultValue(PlaceMode.快速)
            .build()
    );

    /**
     * 每tick最多放置的方块数量
     */
    private final Setting<Integer> blocksPerTick = generalSettings.add(
        new IntSetting.Builder()
            .name("每次放置数量")
            .description("每tick最多放置的方块数量。")
            .defaultValue(2)
            .sliderRange(1, 6)
            .build()
    );

    /**
     * 两次放置之间的延迟（tick）
     */
    private final Setting<Integer> placeDelayTicks = generalSettings.add(
        new IntSetting.Builder()
            .name("放置延迟")
            .description("两次方块放置之间的延迟（tick）。")
            .defaultValue(0)
            .sliderRange(0, 10)
            .build()
    );

    /**
     * 搜索可放置方块的半径范围
     */
    private final Setting<Double> searchRadius = generalSettings.add(
        new DoubleSetting.Builder()
            .name("放置范围")
            .description("搜索可放置方块的半径范围。")
            .defaultValue(4.1)
            .sliderRange(1, 7)
            .build()
    );

    /**
     * 搜索可放置方块的半径范围
     */
    private final Setting<Boolean> autoFix = generalSettings.add(
        new BoolSetting.Builder()
            .name("自动纠错")
            .description("记录打印位置，错误自动挖掉纠正。")
            .defaultValue(false)
            .build()
    );

    /**
     * 空气放置功能
     */
    private final Setting<Boolean> airPlace = generalSettings.add(
        new BoolSetting.Builder()
            .name("空气放置")
            .description("允许在空气中放置方块，无视放置限制。并且方向可能不精准。需要服务器支持, 已知支持: org, cc 不支持: 3c")
            .defaultValue(false)
            .build()
    );

    // ==================== 半砖相关设置 ====================


    private final Setting<Boolean> redHandling = slabSettings.add(
        new BoolSetting.Builder()
            .name("红石元件处理")
            .description("是否启用特殊放置逻辑。")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> stairsHandling = slabSettings.add(
        new BoolSetting.Builder()
            .name("楼梯处理")
            .description("是否启用楼梯的特殊放置逻辑。")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> slabHandling = slabSettings.add(
        new BoolSetting.Builder()
            .name("半砖处理")
            .description("是否启用半砖的特殊放置逻辑。")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> trapdoorHandling = slabSettings.add(
        new BoolSetting.Builder()
            .name("活板门处理")
            .description("是否启用活板门的特殊放置逻辑。")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> enableTorchHandling = slabSettings.add(
        new BoolSetting.Builder()
            .name("火把处理")
            .description("是否启用火把的特殊放置逻辑。")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> hopperHandling = slabSettings.add(
        new BoolSetting.Builder()
            .name("漏斗处理")
            .description("是否启用特殊放置逻辑。")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> chestHandling = slabSettings.add(
        new BoolSetting.Builder()
            .name("箱子处理")
            .description("是否启用特殊放置逻辑。")
            .defaultValue(true)
            .build()
    );


    private final Setting<Boolean> dirtHandling = slabSettings.add(
        new BoolSetting.Builder()
            .name("泥土处理")
            .description("草方块不足时，泥土也认为是草方块。")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> debug = slabSettings.add(
        new BoolSetting.Builder()
            .name("debug")
            .description("debug log")
            .defaultValue(false)
            .build()
    );


    // ==================== 替补方块设置 ====================

    /**
     * 是否启用替补方块功能
     */
    private final Setting<Boolean> enableSubstitute = substituteSettings.add(
        new BoolSetting.Builder()
            .name("启用替补")
            .description("当目标方块缺失时，自动使用替补方块。")
            .defaultValue(true)
            .build()
    );

    /**
     * 替补方块槽位1 - 原方块
     */
    private final Setting<Block> substituteOriginal1 = substituteSettings.add(
        new BlockSetting.Builder()
            .name("原方块-1")
            .description("需要被替换的方块类型。")
            .defaultValue(Blocks.GRASS_BLOCK)
            .visible(enableSubstitute::get)
            .build()
    );

    /**
     * 替补方块槽位1 - 替补方块
     */
    private final Setting<Block> substituteReplacement1 = substituteSettings.add(
        new BlockSetting.Builder()
            .name("替补方块-1")
            .description("用于替换的方块类型。")
            .defaultValue(Blocks.DIRT)
            .visible(enableSubstitute::get)
            .build()
    );

    /**
     * 替补方块槽位2 - 原方块
     */
    private final Setting<Block> substituteOriginal2 = substituteSettings.add(
        new BlockSetting.Builder()
            .name("原方块-2")
            .description("需要被替换的方块类型。")
            .defaultValue(Blocks.MYCELIUM)
            .visible(enableSubstitute::get)
            .build()
    );

    /**
     * 替补方块槽位2 - 替补方块
     */
    private final Setting<Block> substituteReplacement2 = substituteSettings.add(
        new BlockSetting.Builder()
            .name("替补方块-2")
            .description("用于替换的方块类型。")
            .defaultValue(Blocks.DIRT)
            .visible(enableSubstitute::get)
            .build()
    );

    /**
     * 替补方块槽位3 - 原方块
     */
    private final Setting<Block> substituteOriginal3 = substituteSettings.add(
        new BlockSetting.Builder()
            .name("原方块-3")
            .description("需要被替换的方块类型。")
            .defaultValue(Blocks.PODZOL)
            .visible(enableSubstitute::get)
            .build()
    );

    /**
     * 替补方块槽位3 - 替补方块
     */
    private final Setting<Block> substituteReplacement3 = substituteSettings.add(
        new BlockSetting.Builder()
            .name("替补方块-3")
            .description("用于替换的方块类型。")
            .defaultValue(Blocks.DIRT)
            .visible(enableSubstitute::get)
            .build()
    );

    /**
     * 替补方块槽位4 - 原方块
     */
    private final Setting<Block> substituteOriginal4 = substituteSettings.add(
        new BlockSetting.Builder()
            .name("原方块-4")
            .description("需要被替换的方块类型。")
            .defaultValue(Blocks.STONE)
            .visible(enableSubstitute::get)
            .build()
    );

    /**
     * 替补方块槽位4 - 替补方块
     */
    private final Setting<Block> substituteReplacement4 = substituteSettings.add(
        new BlockSetting.Builder()
            .name("替补方块-4")
            .description("用于替换的方块类型。")
            .defaultValue(Blocks.COBBLESTONE)
            .visible(enableSubstitute::get)
            .build()
    );

    /**
     * 替补方块槽位5 - 原方块
     */
    private final Setting<Block> substituteOriginal5 = substituteSettings.add(
        new BlockSetting.Builder()
            .name("原方块-5")
            .description("需要被替换的方块类型。")
            .defaultValue(Blocks.AIR)
            .visible(enableSubstitute::get)
            .build()
    );

    /**
     * 替补方块槽位5 - 替补方块
     */
    private final Setting<Block> substituteReplacement5 = substituteSettings.add(
        new BlockSetting.Builder()
            .name("替补方块-5")
            .description("用于替换的方块类型。")
            .defaultValue(Blocks.AIR)
            .visible(enableSubstitute::get)
            .build()
    );

    // ==================== 渲染相关设置 ====================

    /**
     * 是否启用渲染预览
     */
    private final Setting<Boolean> enableRender = renderSettings.add(
        new BoolSetting.Builder()
            .name("渲染预览")
            .description("是否渲染打印方块的预览框。")
            .defaultValue(true)
            .build()
    );

    /**
     * 渲染形状模式
     */
    private final Setting<ShapeMode> shapeMode = renderSettings.add(
        new EnumSetting.Builder<ShapeMode>()
            .name("渲染模式")
            .description("选择渲染方式：线框、填充或两者。")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    /**
     * 方块预览的侧面颜色
     */
    private final Setting<SettingColor> blockSideColor = renderSettings.add(
        new ColorSetting.Builder()
            .name("侧面颜色")
            .description("打印方块预览的侧面颜色。")
            .defaultValue(new SettingColor(0, 120, 255, 50))
            .build()
    );

    /**
     * 方块预览的边框颜色
     */
    private final Setting<SettingColor> blockLineColor = renderSettings.add(
        new ColorSetting.Builder()
            .name("边框色")
            .description("打印方块预览的线条颜色。")
            .defaultValue(new SettingColor(20, 146, 230, 255))
            .build()
    );

    // ==================== 内部状态 ====================

    /**
     * 待放置方块的位置列表
     */
    private final ArrayList<BlockPos> pendingBlocks = new ArrayList<>();

    /**
     * 方块位置到方块状态的映射
     */
    private final Map<BlockPos, BlockState> blockStateMap = new HashMap<>();

    /**
     * 当前延迟计数器
     */
    private int currentDelay = 0;

    /**
     * 不需要反向放置的方块列表（观察者等特殊方块）
     */
    private final List<Block> noOppositeBlocks = Arrays.asList(Blocks.OBSERVER, Blocks.HOPPER);

    /**
     * 位置缓存管理器，支持自动过期清理
     */
    private final PositionCache positionCache = new PositionCache(1000L);

    // ==================== 构造函数 ====================

    public BestPrinter() {
        super(CATEGORY_MIKU_BUILD,
            "Miku投影打印机",
            "最强打印机。根据投影蓝图自动放置方块。\n自动重建投影文件（Schematic）中的建筑。\n使用前最好把Via 调1.20.6或以下"
        );
    }

    // ==================== 生命周期方法 ====================

    @Override
    public void onActivate() {
        super.onActivate();
        currentDelay = 0;
        positionCache.startCleanupThread();

        ChatUtils.sendMsg("投影打印建议 Via 调整到1.20.6或以下版本");
    }

    @Override
    public void onDeactivate() {
        currentDelay = 0;
        pendingBlocks.clear();
        blockStateMap.clear();
        positionCache.shutdown(); // 关闭缓存管理器
    }

    // ==================== 核心逻辑 ====================

    /**
     * 每tick执行的主逻辑
     * 负责扫描投影、计算待放置方块并执行放置
     */
    @EventHandler
    public void onTick(TickEvent.Post event) {
        // 获取当前加载的投影世界
        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();

        if (mc.player == null || schematicWorld == null) {
            return;
        }

        // 清理过期的缓存条目（现在由后台线程自动处理）
        // positionCache.cleanExpiredCache(); // 可选：手动清理

        // 处理延迟逻辑
        if (currentDelay < placeDelayTicks.get()) {
            currentDelay++;
            return;
        }

        // 重置延迟并开始新一轮放置
        currentDelay = 0;
        updatePendingBlocks();

        if (pendingBlocks.isEmpty()) {
            return;
        }

        // 计算本次实际要放置的方块数量
        int blocksToPlace = Math.min(blocksPerTick.get(), pendingBlocks.size());

        for (int i = 0; i < blocksToPlace; i++) {
            placeBlockAtIndex(i);
        }
    }

    /**
     * 放置指定索引位置的方块
     *
     * @param index 待放置方块列表的索引
     */
    private void placeBlockAtIndex(int index) {
        BlockPos targetPos = pendingBlocks.get(index);
        BlockState targetState = blockStateMap.get(targetPos);

        // 查找背包中对应的方块，支持草方块替代逻辑
        int inventorySlot = findBlockInInventory(targetState);
        if (inventorySlot == -1) {
            return; // 背包中没有该方块或替代方块
        }

        BagUtil.doSwap(inventorySlot);

        // 检查是否是半砖方块，需要特殊处理
        if (slabHandling.get() && targetState.getBlock() instanceof SlabBlock) {
            placeSlabBlock(targetPos, targetState);
        }
        // 检查是否是楼梯方块，需要特殊处理
        else if (targetState.getBlock() instanceof StairsBlock) {
            placeStairsBlock(targetPos, targetState);
        } else if (isTorchBlock(targetState.getBlock())) {
            // 检查是否是火把类方块（包括红石火把），需要特殊处理
            placeTorchBlock(targetPos, targetState);
        } else if (trapdoorHandling.get() && targetState.getBlock() instanceof TrapdoorBlock) {
            // 检查是否是活板门方块
            placeTrapdoorBlock(targetPos, targetState);
        } else if (targetState.getProperties().contains(Properties.FACING)) {
            // 根据方块的朝向属性进行放置
            placeFacingBlock(targetPos, targetState, inventorySlot, Properties.FACING);
        } else if (targetState.getProperties().contains(Properties.HOPPER_FACING)) {
            placeHopperBlock(targetPos, targetState, inventorySlot);
//            placeFacingBlock(targetPos, targetState, inventorySlot, Properties.HOPPER_FACING);

        } else if (targetState.getProperties().contains(Properties.HORIZONTAL_FACING)) {
            placeFacingBlock(targetPos, targetState, inventorySlot, Properties.HORIZONTAL_FACING);
        } else {
            // 没有朝向属性的方块，直接放置
            placeSimpleBlock(targetPos, inventorySlot);
        }

        // 将该位置加入缓存，记录放置尝试时间
        positionCache.addToCache(targetPos);

        // 同步背包状态
        BagUtil.doSwap(inventorySlot);
        BagUtil.sync();
    }

    /**
     * 放置带有朝向属性的方块
     */
    private void placeFacingBlock(BlockPos pos, BlockState state, int slot, Property<Direction> property) {
        Direction facing = state.get(property);

        // 跳过上下朝向的方块


        // 判断是否需要使用反向朝向
        Direction placementDirection = shouldUseOriginalDirection(state.getBlock())
            ? facing
            : facing.getOpposite();

        // 如果启用了空气放置，使用空气放置方法
        if (airPlace.get()) {
            BaritoneUtil.airPlaceBlock(pos);
        } else {
            BaritoneUtil.placeBlockByFaceDirection(pos, true, true, true, placementDirection);
        }
    }

    /**
     * 放置漏斗方块（特殊处理）
     */
    private void placeHopperBlock(BlockPos pos, BlockState state, int slot) {
        Direction facing = state.get(Properties.HOPPER_FACING);

        // 跳过上朝向的漏斗
        if (facing == Direction.DOWN) {
            //需要检测上下方有方块才能放
            BlockPos upPos = pos.up();
            BlockPos downPos = pos.down();
            BlockState upState = mc.world.getBlockState(upPos);
            BlockState downState = mc.world.getBlockState(downPos);

            // 检查上方或下方是否有固体方块支撑
            boolean upSupports = upState.isSolid();
            boolean downSupports = downState.isSolid();

            if (!upSupports && !downSupports) {
                return; // 上下都没有固体方块支撑，无法放置朝下的漏斗
            }
        }

        BlockPos targetPos = pos.offset(facing);
        BlockState targetState = mc.world.getBlockState(targetPos);

        // 检查该方向上是否有实体方块或容器方块（例如箱子、熔炉、漏斗等）
        boolean inValidTarget = targetState.isAir() || targetState.isLiquid();

        if (inValidTarget) {
            return;
        }

        // 如果启用了空气放置，使用空气放置方法
        if (airPlace.get()) {
            BaritoneUtil.airPlaceBlock(pos);
        } else {
            BaritoneUtil.placeBlockDirectionOnly(pos, true, true, true, facing);
        }
    }

    /**
     * 放置简单方块（无朝向属性）
     */
    private void placeSimpleBlock(BlockPos pos, int slot) {
        // 如果启用了空气放置，使用空气放置方法
        if (airPlace.get()) {
            BaritoneUtil.airPlaceBlock(pos);
        } else {
            BaritoneUtil.placeBlock(pos);
        }
    }

    /**
     * 放置半砖方块（特殊处理）
     * 根据投影中目标状态的SlabType自动选择正确的放置方式
     */
    private void placeSlabBlock(BlockPos pos, BlockState targetState) {
        // 如果启用了空气放置，使用空气放置方法
        if (airPlace.get()) {
            BaritoneUtil.airPlaceBlock(pos);
            return;
        }

        // 检查目标状态是否包含SlabType属性
        if (!targetState.getProperties().contains(Properties.SLAB_TYPE)) {
            // 如果没有SlabType属性，按普通方块处理
            BaritoneUtil.placeBlock(pos, true, true, true);
            return;
        }

        SlabType targetSlabType = targetState.get(Properties.SLAB_TYPE);

        // 根据投影中的SlabType自动决定放置方式
        if (targetSlabType == SlabType.TOP) {
            // 上半砖：向上放置
            BaritoneUtil.placeUpBlock(pos, true, true, true);
        } else if (targetSlabType == SlabType.BOTTOM) {
            // 下半砖：向下放置
            BaritoneUtil.placeDownBlock(pos, true, true, true);
        } else if (targetSlabType == SlabType.DOUBLE) {
            // 双层半砖：需要放置两次，第一次先放置下半砖
            // 第二次会自动在同一位置再次放置形成双层
            BaritoneUtil.placeDownBlock(pos, true, true, true);
        } else {
            // 默认处理
            BaritoneUtil.placeBlock(pos, true, true, true);
        }
    }

    /**
     * 放置活板门方块（特殊处理）
     * 处理特征：
     * 1. 方向 (FACING) - 决定哪一面铰链
     * 2. 上下半部分 (HALF) - 决定放在方块上部还是下部
     * 3. 不处理 OPEN 属性，放置后默认为关闭状态
     */
    private void placeTrapdoorBlock(BlockPos pos, BlockState targetState) {
        // 如果启用了空气放置，使用空气放置方法
        if (airPlace.get()) {
            BaritoneUtil.airPlaceBlock(pos);
            return;
        }

        Block targetBlock = targetState.getBlock();

        Direction facing = targetState.get(TrapdoorBlock.FACING);
        BlockHalf half = targetState.get(TrapdoorBlock.HALF);

        // 检查是否具备必要的属性
        if (facing == null || half == null) {
            // 无法获取朝向或上下位置属性时，按普通放置处理
            BaritoneUtil.placeBlock(pos, true, true, true);
            return;
        }

        // 活板门依附在相邻方块的侧面，因此我们需要点击其背后方块的该面
        Direction placementDirection = facing.getOpposite();

        // 上半与下半的区别：影响点击高度（BaritoneUtil可模拟）
        if (half == BlockHalf.TOP) {
            // 上半活板门：需要从上方点击，保证铰链位于上半
            BaritoneUtil.placeUpBlockByFaceDirection(pos, true, true, true, placementDirection);
        } else {
            // 下半活板门：正常点击方块侧面放置
            BaritoneUtil.placeBlockByFaceDirection(pos, true, true, true, placementDirection);
        }
    }

    /**
     * 放置楼梯方块（特殊处理）
     * 楼梯有三个关键属性需要处理：
     * 1. FACING: 楼梯的朝向（东南西北）
     * 2. HALF: 上半部分(TOP)或下半部分(BOTTOM)
     * 3. SHAPE: 楼梯形状（直线、内角、外角等，由游戏自动处理）
     */
    private void placeStairsBlock(BlockPos pos, BlockState targetState) {
        // 如果启用了空气放置，使用空气放置方法
        if (airPlace.get()) {
            BaritoneUtil.airPlaceBlock(pos);
            return;
        }

        // 检查是否有必需的属性
        if (!targetState.getProperties().contains(Properties.HORIZONTAL_FACING)) {
            // 没有朝向属性，按普通方块处理
            BaritoneUtil.placeBlock(pos, true, true, true);
            return;
        }

        Direction facing = targetState.get(Properties.HORIZONTAL_FACING);

        // 检查是否有HALF属性（上半部分还是下半部分）
        if (!targetState.getProperties().contains(Properties.BLOCK_HALF)) {
            // 没有HALF属性，使用普通朝向放置（默认为下半部分）
            BaritoneUtil.placeBlockByFaceDirection(pos, true, true, true, facing);
            return;
        }

        BlockHalf targetHalf = targetState.get(Properties.BLOCK_HALF);

        // 根据HALF属性决定放置方式
        if (targetHalf == BlockHalf.TOP) {
            // 上半楼梯：需要点击方块的上表面
            // 使用placeUpBlock来确保楼梯放置在上半部分
            BaritoneUtil.placeUpBlock(pos, true, true, true);
        } else {
            // 下半楼梯：正常放置，朝向已经由facing决定
            // 使用原始朝向（不需要反向，楼梯会自动处理）
            BaritoneUtil.placeBlockByFaceDirection(pos, true, true, true, facing);
        }
    }

    /**
     * 判断方块是否是火把类型
     * 包括：普通火把、红石火把、灵魂火把
     */
    private boolean isTorchBlock(Block block) {
        return block == Blocks.TORCH
            || block == Blocks.WALL_TORCH
            || block == Blocks.REDSTONE_TORCH
            || block == Blocks.REDSTONE_WALL_TORCH
            || block == Blocks.SOUL_TORCH
            || block == Blocks.SOUL_WALL_TORCH;
    }

    /**
     * 放置火把方块（特殊处理）
     * 火把有两种形态：
     * 1. 立式火把：放置在方块上方，无朝向属性
     * 2. 墙上火把：放置在方块侧面，有HORIZONTAL_FACING属性
     */
    private void placeTorchBlock(BlockPos pos, BlockState targetState) {

        Block targetBlock = targetState.getBlock();

        // 判断是否是墙上火把（有HORIZONTAL_FACING属性）
        boolean isWallTorch = targetBlock == Blocks.WALL_TORCH
            || targetBlock == Blocks.REDSTONE_WALL_TORCH
            || targetBlock == Blocks.SOUL_WALL_TORCH;

        if (isWallTorch) {
            Direction facing = targetState.get(Properties.HORIZONTAL_FACING).getOpposite();

            // 墙上火把：需要点击墙面放置

            // 火把的朝向是指向外的，我们需要点击火把背后的墙面
            // 所以使用相反方向的面来放置

            BlockPos targetPos = pos.offset(facing);
            BlockState hitState = mc.world.getBlockState(targetPos);

            // 检查该方向上是否有实体方块或容器方块（例如箱子、熔炉、漏斗等）
            boolean inValidTarget = hitState.isAir() || hitState.isLiquid();

            if (inValidTarget) {
                return;
            }
            //facing = NORTH
            // 代表火把亮光朝外的方向  ，若方块状态为 facing=NORTH，说明火把"挂"在南面的墙上

            //程序会点击"南面"的墙，而不是"北面"。
            //
            //这会导致：
            //
            //有时火把贴反方向（朝向错误）。
            //
            //如果"南面"没方块可点，就会退化成"放在地上"（fall-back 到地面）
            Direction placementDirection = facing;

            // 点击墙面放置火把
            BaritoneUtil.placeBlockDirectionOnly(pos, true, true, true, placementDirection);
        } else {
            // 立式火把：直接放置在方块上方
            BlockPos below = pos.down();
            BlockState belowState = mc.world.getBlockState(below);

            // 下方不能是空气，且必须是能支撑小方块的实体方块
            boolean canSupport = !belowState.isAir() && Block.sideCoversSmallSquare(mc.world, below, Direction.UP);
            if (canSupport) {
                // 使用placeBlock会自动选择合适的面（通常是下方方块的上表面）
                BaritoneUtil.placeBlock(pos, true, true, true);
            }
            //否则不放
        }
    }


    /**
     * 判断方块是否应该使用原始朝向而非反向
     * 某些方块（如观察者）需要特殊处理，楼梯已单独处理
     */
    private boolean shouldUseOriginalDirection(Block block) {
        return noOppositeBlocks.contains(block);
    }

    /**
     * 更新待放置方块列表
     * 扫描范围内所有需要放置的方块
     */
    private void updatePendingBlocks() {
        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
        List<BlockPos> spherePositions = WorldUtils.getSphere(searchRadius.get());

        pendingBlocks.clear();
        blockStateMap.clear();

        for (BlockPos pos : spherePositions) {
            if (shouldPlaceBlockAt(pos, schematicWorld)) {
                SchematicContext context = new SchematicContext(Via.getEntityWorld(mc.player), schematicWorld, pos);
                pendingBlocks.add(pos);
                blockStateMap.put(pos, context.targetState);
            }
        }
    }

    /**
     * 判断指定位置是否应该放置方块
     * 综合考虑多个条件：投影状态、实际状态、背包物品、高度限制、缓存等
     */
    private boolean shouldPlaceBlockAt(BlockPos pos, WorldSchematic schematicWorld) {
        SchematicContext context = new SchematicContext(Via.getEntityWorld(mc.player), schematicWorld, pos);

        // 半砖特殊验证
        if (slabHandling.get() && context.targetState.getBlock() instanceof SlabBlock) {
            if (!isValidSlabPlacement(pos, context)) {
                return false;
            }
        }

        // 检查各项条件
        return canPlaceByMode(pos)
            && isValidTargetBlock(context.targetState)
            && isBlockDifferent(context)
            && isWithinLayerLimit(pos)
            && hasBlockInInventory(context.targetState)
            && !pendingBlocks.contains(pos)
            && !positionCache.isInCache(pos);  // 检查是否在缓存中
    }

    /**
     * 验证半砖放置是否有效
     * 检查相邻方块是否有合适的半砖支撑或完整方块支撑
     */
    private boolean isValidSlabPlacement(BlockPos pos, SchematicContext context) {
        BlockState targetState = context.targetState;

        if (!targetState.getProperties().contains(Properties.SLAB_TYPE)) {
            return true; // 不是标准半砖，按普通方块处理
        }

        SlabType targetSlabType = targetState.get(Properties.SLAB_TYPE);
        Block targetBlock = targetState.getBlock();

        // 根据半砖类型确定需要检查的方向
        List<Direction> directionsToCheck = new ArrayList<>();

        // 所有半砖都检查水平四个方向
        directionsToCheck.add(Direction.NORTH);
        directionsToCheck.add(Direction.SOUTH);
        directionsToCheck.add(Direction.EAST);
        directionsToCheck.add(Direction.WEST);

        // 上半砖额外检查上方
        if (targetSlabType == SlabType.TOP) {
            directionsToCheck.add(Direction.UP);
        }

        // 下半砖额外检查下方
        if (targetSlabType == SlabType.BOTTOM) {
            directionsToCheck.add(Direction.DOWN);
        }

        // 双层半砖检查上下方
        if (targetSlabType == SlabType.DOUBLE) {
            directionsToCheck.add(Direction.UP);
            directionsToCheck.add(Direction.DOWN);
        }

        // 检查所有指定方向
        for (Direction direction : directionsToCheck) {
            BlockPos neighborPos = pos.offset(direction);
            BlockState neighborState = mc.world.getBlockState(neighborPos);
            Block neighborBlock = neighborState.getBlock();

            // 情况1：相邻方块是相同类型的半砖
            if (neighborBlock instanceof SlabBlock) {
                if (neighborState.getProperties().contains(Properties.SLAB_TYPE)) {
                    SlabType neighborSlabType = neighborState.get(Properties.SLAB_TYPE);

                    // 上半砖：相邻需要是上半砖或双层
                    if (targetSlabType == SlabType.TOP &&
                        (neighborSlabType == SlabType.TOP || neighborSlabType == SlabType.DOUBLE)) {
                        return true;
                    }

                    // 下半砖：相邻需要是下半砖或双层
                    if (targetSlabType == SlabType.BOTTOM &&
                        (neighborSlabType == SlabType.BOTTOM || neighborSlabType == SlabType.DOUBLE)) {
                        return true;
                    }

                    // 双层半砖：相邻有任何半砖即可
                    if (targetSlabType == SlabType.DOUBLE) {
                        return true;
                    }
                }
            } else {

                BlockState blockToPlace = Blocks.COBBLESTONE_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP);
                blockToPlace = targetState;
                boolean canPlace = targetState.canPlaceAt(mc.world, pos);
                if (canPlace) {
                    return true;
                }
            }
        }

        // 没有找到合适的相邻方块支撑
        return false;
    }

    /**
     * 检查目标方块是否有效（不是空气）
     */
    private boolean isValidTargetBlock(BlockState targetState) {
        return targetState.getBlock() != Blocks.AIR
            && targetState.getBlock().asItem() != Items.AIR;
    }

    /**
     * 检查目标方块与当前方块是否不同
     */
    private boolean isBlockDifferent(SchematicContext context) {
        return context.targetState.getBlock() != context.currentState.getBlock();
    }

    /**
     * 检查位置是否在渲染层高度限制内
     */
    private boolean isWithinLayerLimit(BlockPos pos) {
        return pos.getY() <= DataManager.getRenderLayerRange().getLayerMax();
    }

    /**
     * 检查背包中是否有目标方块
     */
    private boolean hasBlockInInventory(BlockState targetState) {
        return findBlockInInventory(targetState) != -1;
    }

    /**
     * 在背包中查找目标方块
     * 支持草方块替代逻辑：如果是草方块且背包没有，则尝试用泥土代替
     *
     * @param targetState 目标方块状态
     * @return 物品在背包中的槽位，如果没有找到返回-1
     */
    private int findBlockInInventory(BlockState targetState) {
        Block targetBlock = targetState.getBlock();
        Item targetItem = targetBlock.asItem();

        // 先尝试查找目标方块本身
        int slot = BagUtil.findItemInventorySlotGrim(targetItem);

        // 如果找到了，直接返回
        if (slot != -1) {
            return slot;
        }

        // 特殊处理：草方块替代逻辑
        if (dirtHandling.get() && targetBlock == Blocks.GRASS_BLOCK) {
            // 背包没有草方块，尝试用泥土代替
            slot = BagUtil.findItemInventorySlotGrim(Items.DIRT);
            if (slot != -1) {
                return slot;
            }
        }

        // 如果启用了替补方块功能，尝试查找替补
        if (enableSubstitute.get()) {
            Block substitute = findSubstituteBlock(targetBlock);
            if (substitute != null && substitute != Blocks.AIR) {
                slot = BagUtil.findItemInventorySlotGrim(substitute.asItem());
                if (slot != -1) {
                    return slot;
                }
            }
        }

        // 都没有找到，返回-1
        return -1;
    }

    /**
     * 查找目标方块对应的替补方块
     * 遍历所有用户配置的替补规则
     *
     * @param targetBlock 目标方块
     * @return 替补方块，如果没有找到返回null
     */
    private Block findSubstituteBlock(Block targetBlock) {
        // 检查替补槽位1
        if (substituteOriginal1.get() == targetBlock) {
            return substituteReplacement1.get();
        }

        // 检查替补槽位2
        if (substituteOriginal2.get() == targetBlock) {
            return substituteReplacement2.get();
        }

        // 检查替补槽位3
        if (substituteOriginal3.get() == targetBlock) {
            return substituteReplacement3.get();
        }

        // 检查替补槽位4
        if (substituteOriginal4.get() == targetBlock) {
            return substituteReplacement4.get();
        }

        // 检查替补槽位5
        if (substituteOriginal5.get() == targetBlock) {
            return substituteReplacement5.get();
        }

        // 没有找到匹配的替补规则
        return null;
    }

    /**
     * 根据放置模式判断是否可以放置方块
     */
    private boolean canPlaceByMode(BlockPos pos) {
        // 如果启用了空气放置，则允许在空中放置方块
        if (airPlace.get()) {
            return true;
        }

        if (placeMode.get() == PlaceMode.合法) {
            // 合法模式：检查距离和可达性
            return BaritoneUtil.canPlaceWithDis(pos, searchRadius.get(), true);
        }
        // 快速模式：只检查是否有可交互的面
        return BaritoneUtil.getInteractDirection(pos, true) != null;
    }

    // ==================== 缓存管理 ====================
    // 缓存管理已移至 PositionCache 工具类，支持后台自动清理

    // ==================== 渲染逻辑 ====================

    /**
     * 渲染待放置方块的预览框
     */
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!enableRender.get() || pendingBlocks.isEmpty()) {
            return;
        }

        for (BlockPos pos : pendingBlocks) {
            renderBlockPreview(event, pos);
        }
    }

    /**
     * 渲染单个方块的预览框
     */
    private void renderBlockPreview(Render3DEvent event, BlockPos pos) {
        double x1 = pos.getX();
        double y1 = pos.getY();
        double z1 = pos.getZ();
        double x2 = pos.getX() + 1;
        double y2 = pos.getY() + 1;
        double z2 = pos.getZ() + 1;

        event.renderer.box(
            x1, y1, z1,
            x2, y2, z2,
            blockSideColor.get(),
            blockLineColor.get(),
            shapeMode.get(),
            0
        );
    }

    // ==================== 枚举类型 ====================

    /**
     * 放置模式枚举
     * 快速：只检查基本交互面
     * 合法：严格检查距离和可达性
     */
    public enum PlaceMode {
        快速,
        合法
    }
}
