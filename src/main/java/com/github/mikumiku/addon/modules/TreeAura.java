package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.BagUtil;
import com.github.mikumiku.addon.util.BaritoneUtil;
import com.github.mikumiku.addon.util.WorldUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BoneMealItem;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

public class TreeAura extends BaseModule {

    // 设置组，用于组织模块的设置选项
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // 种植树木的延迟设置（tick），控制种植操作之间的间隔时间
    private final Setting<Integer> plantDelay = sgGeneral.add(new IntSetting.Builder()
            .name("种植延迟").description("种植树木之间的延迟").defaultValue(6).min(0).sliderMax(25).build());
    // 施加骨粉的延迟设置（tick），控制对树苗使用骨粉的间隔时间
    private final Setting<Integer> bonemealDelay = sgGeneral.add(new IntSetting.Builder()
            .name("骨粉延迟").description("在树木上放置骨粉之间的延迟").defaultValue(1).min(0).sliderMax(25).build());
    // 最大骨粉使用次数设置，控制对单个树苗最多使用多少次骨粉
    private final Setting<Integer> maxBonemealAttempts = sgGeneral.add(new IntSetting.Builder()
            .name("最大骨粉次数").description("对单个树苗最多使用骨粉的次数").defaultValue(20).min(1).sliderMax(200).build());
    // 水平种植半径设置，控制水平方向上的种植范围
    private final Setting<Integer> rRange = sgGeneral.add(new IntSetting.Builder()
            .name("半径").description("您可以水平放置多远").defaultValue(4).min(1).sliderMax(5).build());
    // 垂直种植范围设置，控制垂直方向上的种植范围
    private final Setting<Integer> yRange = sgGeneral.add(new IntSetting.Builder()
            .name("Y轴范围").description("您可以垂直放置多远").defaultValue(3).min(1).sliderMax(5).build());
    // 排序模式设置（最近或最远），控制选择种植/施肥位置的优先级
    private final Setting<SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<SortMode>()
            .name("排序模式").description("如何排序附近的树木/放置位置").defaultValue(SortMode.Farthest).build());
    // 树苗间隔设置，控制树苗之间的最小距离
    private final Setting<Integer> saplingSpacing = sgGeneral.add(new IntSetting.Builder()
            .name("树苗间隔").description("树苗之间的最小间隔距离（格数）").defaultValue(1).min(0).sliderMax(10).build());
    // 防止堵路设置，开启后只在玩家移动的身后种树
    private final Setting<Boolean> preventBlocking = sgGeneral.add(new BoolSetting.Builder()
            .name("防止堵路").description("开启后只在玩家移动的身后种树，避免堵住玩家的路").defaultValue(false).build());

    // 骨粉使用计时器和种植计时器，用于控制操作频率
    private int bonemealTimer, plantTimer;
    // 跟踪每个树苗位置的骨粉使用次数
    private Map<BlockPos, Integer> saplingBonemealCount;
    // 用于跟踪玩家位置变化，检测移动方向
    private BlockPos lastPlayerPos;

    // 错误消息时间控制（避免频繁打印）
    private long lastBonemealErrorTime = 0;
    private long lastSaplingErrorTime = 0;
    private static final long ERROR_COOLDOWN = 5000; //  冷却时间


    public TreeAura() { // CopeTypes
        super("自动种树", "在你周围种树！催熟");
    }

    @Override
    public void onActivate() {
        // 激活模块时重置计时器和状态
        bonemealTimer = 0;
        plantTimer = 0;
        saplingBonemealCount = new HashMap<>();
        lastPlayerPos = MinecraftClient.getInstance().player.getBlockPos();
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        // 每个游戏刻减少计时器
        plantTimer--;
        bonemealTimer--;

        // 更新玩家位置用于移动检测
        BlockPos currentPos = MinecraftClient.getInstance().player.getBlockPos();
        if (lastPlayerPos == null) {
            lastPlayerPos = currentPos;
        }

        // 当骨粉计时器归零时处理骨粉催熟逻辑
        if (bonemealTimer <= 0) {
            processBonemealForAllSaplings();
            bonemealTimer = bonemealDelay.get();
        }

        // 当种植计时器归零时寻找种植位置并种植树苗
        if (plantTimer <= 0) {
            BlockPos plantPos = findPlantLocation();
            if (plantPos != null) {
                doPlant(plantPos);
            }
            plantTimer = plantDelay.get();
        }

        // 更新上一个位置
        lastPlayerPos = currentPos;
    }


    // 查找背包中的骨粉
    private int findBonemeal() {
        return BagUtil.findClassInventorySlotGrim(BoneMealItem.class);
    }

    // 查找背包中的树苗
    private int findSapling() {
        FindItemResult result = InvUtils.find(itemStack -> Block.getBlockFromItem(itemStack.getItem()) instanceof SaplingBlock);
        return BagUtil.findClassInventorySlotGrim(SaplingBlock.class);
    }

    // 判断指定位置是否为树苗
    private boolean isSapling(BlockPos pos) {
        return MinecraftClient.getInstance().world.getBlockState(pos).getBlock() instanceof SaplingBlock;
    }

    // 判断指定位置是否适合生长
    // 判断指定位置是否适合树木生长（树叶、原木、空气都不会阻止生长）
    private boolean isGrowthFriendlyBlock(BlockPos pos) {
        Block block = MinecraftClient.getInstance().world.getBlockState(pos).getBlock();


        // 空气、树叶、原木、藤蔓等不会阻止树木生长
        return block instanceof AirBlock ||
                block instanceof LeavesBlock ||
                block instanceof PillarBlock ||  // 原木类方块
                block.getDefaultState().isIn(BlockTags.LOGS) ||
                block.getDefaultState().isIn(BlockTags.LEAVES) ||
                block instanceof VineBlock ||    // 藤蔓
                block instanceof PlantBlock ||   // 植物类方块（草、花等）
                block.equals(Blocks.SNOW) ||     // 雪
                block.equals(Blocks.TALL_GRASS) ||
                block.equals(Blocks.SHORT_GRASS) ||
                block.equals(Blocks.FERN) ||
                block.equals(Blocks.LARGE_FERN);
    }

    // 在指定位置种植树苗
    private void doPlant(BlockPos plantPos) {
        int sapling = findSapling();
        if (sapling <= -1) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastSaplingErrorTime >= ERROR_COOLDOWN) {
                error("快捷栏中没有树苗");
                lastSaplingErrorTime = currentTime;
            }
            return;
        }
        BagUtil.doSwap(sapling);
        // 使用 BaritoneUtil.clickBlock 方法，会自动处理角度
        BaritoneUtil.clickBlock(plantPos, Direction.UP, true, Hand.MAIN_HAND, BaritoneUtil.SwingSide.All);
        BagUtil.doSwap(sapling);
    }

    // 处理范围内所有树苗的骨粉催熟逻辑
    private void processBonemealForAllSaplings() {
        // 清理已经不是树苗的位置记录
        cleanupSaplingRecords();

        // 获取范围内所有树苗
        List<BlockPos> saplings = findAllSaplingsInRange();
        if (saplings.isEmpty()) return;

        // 找到第一个还没达到最大骨粉次数的树苗
        for (BlockPos sapling : saplings) {
            int currentCount = saplingBonemealCount.getOrDefault(sapling, 0);
            if (currentCount < maxBonemealAttempts.get()) {
                // 对这个树苗使用骨粉
                doBonemeal(sapling);
                // 更新使用次数
                saplingBonemealCount.put(sapling, currentCount + 1);
                // 每次只处理一个树苗，然后等待下一个tick
                return;
            } else {
                info("树苗 " + sapling.toShortString() + " 已达到最大使用次数，跳过");
            }
        }
    }

    // 清理已经不是树苗的位置记录
    private void cleanupSaplingRecords() {
        saplingBonemealCount.entrySet().removeIf(entry -> !isSapling(entry.getKey()));
    }

    // 获取范围内所有树苗
    private List<BlockPos> findAllSaplingsInRange() {
        return findSaplings(MinecraftClient.getInstance().player.getBlockPos(), rRange.get(), yRange.get());
    }

    // 对指定位置的树苗施加骨粉
    private void doBonemeal(BlockPos sapling) {
        int bonemeal = findBonemeal();
        if (bonemeal <= -1) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastBonemealErrorTime >= ERROR_COOLDOWN) {
                error("快捷栏中没有骨粉");
                lastBonemealErrorTime = currentTime;
            }
            return;
        }

        BagUtil.doSwap(bonemeal);
        // 使用 BaritoneUtil.clickBlock 方法，会自动处理角度
        BaritoneUtil.clickBlock(sapling, Direction.UP, true, Hand.MAIN_HAND, BaritoneUtil.SwingSide.All);
        BagUtil.doSwap(bonemeal);
    }

    // 判断指定位置是否可以种植树苗
    private boolean canPlant(BlockPos pos) {
        Block b = MinecraftClient.getInstance().world.getBlockState(pos).getBlock();
        // 只能在草方块、泥土等允许种植的地方种植
        if (b.equals(Blocks.GRASS_BLOCK)
                || b.equals(Blocks.MOSS_BLOCK)
                || b.equals(Blocks.PODZOL)
                || b.equals(Blocks.ROOTED_DIRT)
                || b.equals(Blocks.FARMLAND)
                || b.equals(Blocks.DIRT_PATH)
                || b.equals(Blocks.MYCELIUM)
                || b.equals(Blocks.DIRT)
                || b.equals(Blocks.COARSE_DIRT)) {

            // 检查设定间隔范围内是否有树苗或树干，确保间隔符合设置
            // 检查位置是在方块上面一格（树苗种植位置）及其周围
            int spacing = saplingSpacing.get();
            if (spacing > 0) {
                for (int x = -spacing; x <= spacing; x++) {
                    for (int z = -spacing; z <= spacing; z++) {
                        // 跳过中心位置（当前要种植的位置）
                        if (x == 0 && z == 0) continue;

                        // 检查地面上一格的位置（树苗实际种植的高度）
                        BlockPos checkPos = pos.add(x, 1, z);
                        Block checkBlock = MinecraftClient.getInstance().world.getBlockState(checkPos).getBlock();

                        // 如果检查位置是树苗或树干，则不能种植
                        if (checkBlock instanceof SaplingBlock || checkBlock instanceof PillarBlock) {
                            return false;
                        }
                    }
                }
            }

            final AtomicBoolean plant = new AtomicBoolean(true);
            // 检查上方5格内是否有障碍物，确保树木有足够的生长空间
            IntStream.rangeClosed(1, 5).forEach(i -> {
                // 检查正上方 - 必须是空气（树干生长位置）
                BlockPos check = pos.up(i);
                if (!MinecraftClient.getInstance().world.getBlockState(check).getBlock().equals(Blocks.AIR)) {
                    plant.set(false);
                    return;
                }
                // 检查四周 - 使用更宽松的生长友好检查（树叶、原木等可以存在）
                for (CardinalDirection dir : CardinalDirection.values()) {
                    BlockPos sidePos = check.offset(dir.toDirection(), 1);
                    if (!isGrowthFriendlyBlock(sidePos)) {
                        plant.set(false);
                        return;
                    }
                }
            });
            return plant.get();
        }
        return false;
    }

    // 查找指定范围内的所有树苗
    private List<BlockPos> findSaplings(BlockPos centerPos, int radius, int height) {
        ArrayList<BlockPos> blocc = new ArrayList<>();
        List<BlockPos> blocks = WorldUtils.getSphere(centerPos, radius, height);
        for (BlockPos b : blocks) if (isSapling(b)) blocc.add(b);
        return blocc;
    }


    // 获取可以种植树苗的位置列表
    private List<BlockPos> getPlantLocations(BlockPos centerPos, int radius, int height) {
        ArrayList<BlockPos> blocc = new ArrayList<>();
        List<BlockPos> blocks = WorldUtils.getSphere(centerPos, radius, height);
        for (BlockPos b : blocks) if (canPlant(b)) blocc.add(b);
        return blocc;
    }

    // 查找合适的种植位置
    private BlockPos findPlantLocation() {
        List<BlockPos> nearby = getPlantLocations(MinecraftClient.getInstance().player.getBlockPos(), rRange.get(), yRange.get());
        if (nearby.isEmpty()) return null;

        // 如果开启了防止堵路功能，过滤掉不在玩家身后的位置
        if (preventBlocking.get()) {
            nearby = nearby.stream()
                    .filter(this::isPositionBehindPlayer)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

            if (nearby.isEmpty()) return null;
        }

        // 根据距离排序
        nearby.sort(Comparator.comparingDouble(PlayerUtils::distanceTo));
        // 根据排序模式决定使用最近还是最远的位置
        if (sortMode.get().equals(SortMode.Farthest)) Collections.reverse(nearby);
        return nearby.get(0);
    }

    // 计算两个位置之间的距离
    private double distanceBetween(BlockPos pos1, BlockPos pos2) {
        double d = pos1.getX() - pos2.getX();
        double e = pos1.getY() - pos2.getY();
        double f = pos1.getZ() - pos2.getZ();
        return MathHelper.sqrt((float) (d * d + e * e + f * f));
    }

    // 检测玩家是否在移动
    private boolean isPlayerMoving() {
        // 检查按键输入
        boolean keyPressed = Input.isPressed(MinecraftClient.getInstance().options.forwardKey) ||
                Input.isPressed(MinecraftClient.getInstance().options.backKey) ||
                Input.isPressed(MinecraftClient.getInstance().options.leftKey) ||
                Input.isPressed(MinecraftClient.getInstance().options.rightKey);

        // 检查位置变化
        BlockPos currentPos = MinecraftClient.getInstance().player.getBlockPos();
        boolean positionChanged = lastPlayerPos != null && !currentPos.equals(lastPlayerPos);

        return keyPressed || positionChanged;
    }

    // 获取玩家的移动方向
    private Direction getPlayerMovementDirection() {
        if (!isPlayerMoving()) {
            return null;
        }

        BlockPos currentPos = MinecraftClient.getInstance().player.getBlockPos();

        // 如果位置发生了变化，根据位置变化计算方向
        if (lastPlayerPos != null && !currentPos.equals(lastPlayerPos)) {
            int deltaX = currentPos.getX() - lastPlayerPos.getX();
            int deltaZ = currentPos.getZ() - lastPlayerPos.getZ();

            // 根据位置变化确定主要移动方向
            if (Math.abs(deltaX) > Math.abs(deltaZ)) {
                return deltaX > 0 ? Direction.EAST : Direction.WEST;
            } else if (Math.abs(deltaZ) > 0) {
                return deltaZ > 0 ? Direction.SOUTH : Direction.NORTH;
            }
        }

        // 如果位置没有变化但有按键输入，根据玩家朝向和按键判断方向
        float yaw = MinecraftClient.getInstance().player.getYaw();
        Direction facingDirection = getDirectionFromYaw(yaw);

        if (Input.isPressed(MinecraftClient.getInstance().options.forwardKey)) {
            return facingDirection;
        } else if (Input.isPressed(MinecraftClient.getInstance().options.backKey)) {
            return facingDirection.getOpposite();
        } else if (Input.isPressed(MinecraftClient.getInstance().options.leftKey)) {
            return getLeftDirection(facingDirection);
        } else if (Input.isPressed(MinecraftClient.getInstance().options.rightKey)) {
            return getRightDirection(facingDirection);
        }

        return null;
    }

    // 根据yaw角度获取Direction
    private Direction getDirectionFromYaw(float yaw) {
        // 标准化yaw角度到0-360范围
        yaw = ((yaw % 360) + 360) % 360;

        if (yaw >= 315 || yaw < 45) {
            return Direction.SOUTH;  // 0度朝南
        } else if (yaw >= 45 && yaw < 135) {
            return Direction.WEST;   // 90度朝西
        } else if (yaw >= 135 && yaw < 225) {
            return Direction.NORTH;  // 180度朝北
        } else {
            return Direction.EAST;   // 270度朝东
        }
    }

    // 获取左侧方向
    private Direction getLeftDirection(Direction direction) {
        switch (direction) {
            case NORTH:
                return Direction.WEST;
            case WEST:
                return Direction.SOUTH;
            case SOUTH:
                return Direction.EAST;
            case EAST:
                return Direction.NORTH;
            default:
                return direction;
        }
    }

    // 获取右侧方向
    private Direction getRightDirection(Direction direction) {
        switch (direction) {
            case NORTH:
                return Direction.EAST;
            case EAST:
                return Direction.SOUTH;
            case SOUTH:
                return Direction.WEST;
            case WEST:
                return Direction.NORTH;
            default:
                return direction;
        }
    }

    // 检查位置是否在玩家身后
    private boolean isPositionBehindPlayer(BlockPos pos) {
        if (!preventBlocking.get()) {
            return true; // 如果没有开启防止堵路，所有位置都可以
        }

        Direction movementDirection = getPlayerMovementDirection();
        if (movementDirection == null) {
            return true; // 如果玩家没有移动，所有位置都可以
        }

        BlockPos playerPos = MinecraftClient.getInstance().player.getBlockPos();
        Direction oppositeDirection = movementDirection.getOpposite();

        // 计算位置相对于玩家的方向
        int deltaX = pos.getX() - playerPos.getX();
        int deltaZ = pos.getZ() - playerPos.getZ();

        // 检查位置是否在移动方向的相反方向（身后）
        switch (oppositeDirection) {
            case NORTH:
                return deltaZ < 0; // 北方（Z负方向）
            case SOUTH:
                return deltaZ > 0; // 南方（Z正方向）
            case WEST:
                return deltaX < 0; // 西方（X负方向）
            case EAST:
                return deltaX > 0; // 东方（X正方向）
            default:
                return true;
        }
    }

    // 排序模式枚举：最近或最远
    public enum SortMode {
        // 最近优先模式
        Closest("最近优先"),
        // 最远优先模式
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
