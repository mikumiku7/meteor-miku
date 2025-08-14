package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.BagUtil;
import com.github.mikumiku.addon.util.BaritoneUtil;
import com.github.mikumiku.addon.util.WorldUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.input.Input;
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
    // 只使用骨粉设置，开启后只对现有树苗使用骨粉，不种植新的树苗
    private final Setting<Boolean> onlyBonemeal = sgGeneral.add(new BoolSetting.Builder()
        .name("只使用骨粉").description("开启后只对现有树苗使用骨粉催熟，不种植新的树苗").defaultValue(false).build());

    // 骨粉使用计时器和种植计时器，用于控制操作频率
    private int bonemealTimer, plantTimer;
    // 跟踪每个树苗位置的骨粉使用次数
    private Map<BlockPos, Integer> saplingBonemealCount;
    // 用于跟踪玩家位置变化，检测移动方向
    private BlockPos lastPlayerPos;
    // 记住最后的移动方向，即使停止移动也保持
    private Direction lastMovementDirection;

    // 缓存背包中可用的种植物品
    private Map<Block, Integer> availablePlantItems; // Block -> 槽位
    private long lastInventoryScanTime = 0;
    private static final long INVENTORY_SCAN_INTERVAL = 1000; // 1秒扫描一次背包

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
        lastMovementDirection = null;
        availablePlantItems = new HashMap<>();
        lastInventoryScanTime = 0;
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

        // 只有在非"只使用骨粉"模式下才扫描种植物品
        if (!onlyBonemeal.get()) {
            scanInventoryForPlantItems();
        }

        // 当骨粉计时器归零时处理骨粉催熟逻辑
        if (bonemealTimer <= 0) {
            processBonemealForAllSaplings();
            bonemealTimer = bonemealDelay.get();
        }

        // 当种植计时器归零时寻找种植位置并种植树苗（仅在未开启"只使用骨粉"模式时执行）
        if (plantTimer <= 0 && !onlyBonemeal.get()) {
            BlockPos plantPos = findPlantLocation();
            if (plantPos != null) {
                doPlant(plantPos);
            }
            plantTimer = plantDelay.get();
        }

        // 更新上一个位置
        lastPlayerPos = currentPos;
    }


    // 扫描背包中的种植物品
    private void scanInventoryForPlantItems() {
        availablePlantItems.clear();

        int slot = BagUtil.findClassInventorySlotGrim(SaplingBlock.class);
        if (slot != -1) {
            availablePlantItems.put(Blocks.OAK_SAPLING, slot);
        }
        int slot2 = BagUtil.findBlockInventorySlotGrim(Blocks.CRIMSON_FUNGUS);
        if (slot2 != -1) {
            availablePlantItems.put(Blocks.CRIMSON_FUNGUS, slot2);
        }
        int slot3 = BagUtil.findBlockInventorySlotGrim(Blocks.WARPED_FUNGUS);
        if (slot3 != -1) {
            availablePlantItems.put(Blocks.WARPED_FUNGUS, slot3);
        }

        lastInventoryScanTime = System.currentTimeMillis();

    }

    // 查找背包中的骨粉
    private int findBonemeal() {
        return BagUtil.findClassInventorySlotGrim(BoneMealItem.class);
    }


    // 判断指定位置是否为树苗
    private boolean isSapling(BlockPos pos) {
        Block block = MinecraftClient.getInstance().world.getBlockState(pos).getBlock();
        return block instanceof SaplingBlock
            || block instanceof FungusBlock
            || block.equals(Blocks.CRIMSON_FUNGUS)
            || block.equals(Blocks.WARPED_FUNGUS);
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

    // 检查菌类和菌岩是否匹配
    private boolean isFungusNyliumMatch(Block fungus, Block nylium) {
        if (fungus.equals(Blocks.CRIMSON_FUNGUS) && nylium.equals(Blocks.CRIMSON_NYLIUM)) {
            return true;
        }
        if (fungus.equals(Blocks.WARPED_FUNGUS) && nylium.equals(Blocks.WARPED_NYLIUM)) {
            return true;
        }
        return false;
    }

    // 检查是否可以在指定位置种植（基于可用物品）
    private boolean canPlantAtPosition(BlockPos pos, Block groundBlock) {
        // 检查菌岩匹配
        if (groundBlock.equals(Blocks.CRIMSON_NYLIUM)) {
            return availablePlantItems.containsKey(Blocks.CRIMSON_FUNGUS) && canPlantBasicCheck(pos, true);
        } else if (groundBlock.equals(Blocks.WARPED_NYLIUM)) {
            return availablePlantItems.containsKey(Blocks.WARPED_FUNGUS) && canPlantBasicCheck(pos, true);
        }

        // 检查普通土壤
        boolean isSuitableSoil = groundBlock.equals(Blocks.GRASS_BLOCK)
            || groundBlock.equals(Blocks.MOSS_BLOCK)
            || groundBlock.equals(Blocks.PODZOL)
            || groundBlock.equals(Blocks.ROOTED_DIRT)
            || groundBlock.equals(Blocks.FARMLAND)
            || groundBlock.equals(Blocks.DIRT_PATH)
            || groundBlock.equals(Blocks.MYCELIUM)
            || groundBlock.equals(Blocks.DIRT)
            || groundBlock.equals(Blocks.COARSE_DIRT);

        if (isSuitableSoil) {
            // 检查是否有任何树苗
            for (Block block : availablePlantItems.keySet()) {
                if (block instanceof SaplingBlock) {
                    return canPlantBasicCheck(pos, false);
                }
            }
        }

        return false;
    }


    // 基础种植检查（间隔、空间等）
    private boolean canPlantBasicCheck(BlockPos pos, boolean isFungus) {
        // 检查设定间隔范围内是否有树苗或树干，确保间隔符合设置
        int spacing = saplingSpacing.get();
        if (spacing > 0) {
            for (int x = -spacing; x <= spacing; x++) {
                for (int z = -spacing; z <= spacing; z++) {
                    // 跳过中心位置（当前要种植的位置）
                    if (x == 0 && z == 0) continue;

                    // 检查地面上一格的位置（树苗实际种植的高度）
                    BlockPos checkPos = pos.add(x, 1, z);
                    Block checkBlock = MinecraftClient.getInstance().world.getBlockState(checkPos).getBlock();

                    // 如果检查位置是树苗、菌类或树干，则不能种植
                    if (checkBlock instanceof SaplingBlock ||
                        checkBlock instanceof FungusBlock ||
                        checkBlock.equals(Blocks.CRIMSON_FUNGUS) ||
                        checkBlock.equals(Blocks.WARPED_FUNGUS) ||
                        checkBlock instanceof PillarBlock) {
                        return false;
                    }
                }
            }
        }

        // 如果可以种植菌类，则只需要检查上方位置是否为空气 菌类 随便生长
        if (isFungus) {
            BlockPos check = pos.up(1);
            Block block = MinecraftClient.getInstance().world.getBlockState(check).getBlock();

            if (block.equals(Blocks.AIR)
                || block.equals(Blocks.WATER)
            ) {
                return true;
            }
        }


        // 检查上方5格内是否有障碍物，确保树木有足够的生长空间
        final AtomicBoolean canPlant = new AtomicBoolean(true);
        IntStream.rangeClosed(1, 5).forEach(i -> {
            // 检查正上方 - 必须是空气（树干生长位置）
            BlockPos check = pos.up(i);
            Block block = MinecraftClient.getInstance().world.getBlockState(check).getBlock();
            if (!block.equals(Blocks.AIR) && !block.equals(Blocks.WATER)) {
                canPlant.set(false);
                return;
            }
            // 检查四周 - 使用更宽松的生长友好检查（树叶、原木等可以存在）
            for (CardinalDirection dir : CardinalDirection.values()) {
                BlockPos sidePos = check.offset(dir.toDirection(), 1);
                if (!isGrowthFriendlyBlock(sidePos)) {
                    canPlant.set(false);
                    return;
                }
            }
        });
        return canPlant.get();
    }

    // 在指定位置种植树苗或菌类（使用缓存的物品信息）
    private void doPlant(BlockPos plantPos) {
        Block groundBlock = MinecraftClient.getInstance().world.getBlockState(plantPos).getBlock();
        Integer itemSlot = null;

        // 根据地面类型选择合适的种植物品
        if (groundBlock.equals(Blocks.CRIMSON_NYLIUM)) {
            itemSlot = availablePlantItems.get(Blocks.CRIMSON_FUNGUS);
            if (itemSlot == null) {
                error("绯红菌岩需要绯红菌，但背包中没有找到");
                return;
            }
        } else if (groundBlock.equals(Blocks.WARPED_NYLIUM)) {
            itemSlot = availablePlantItems.get(Blocks.WARPED_FUNGUS);
            if (itemSlot == null) {
                error("诡异菌岩需要诡异菌，但背包中没有找到");
                return;
            }
        } else {
            // 普通土壤，选择任意可用的树苗
            for (Map.Entry<Block, Integer> entry : availablePlantItems.entrySet()) {
                if (entry.getKey() instanceof SaplingBlock) {
                    itemSlot = entry.getValue();
                    break;
                }
            }
            if (itemSlot == null) {
                error("没有找到可用的树苗");
                return;
            }
        }

        BagUtil.doSwap(itemSlot);
        // 使用 BaritoneUtil.clickBlock 方法，会自动处理角度
        BaritoneUtil.clickBlock(plantPos, Direction.UP, true, Hand.MAIN_HAND, BaritoneUtil.SwingSide.All);
        BagUtil.doSwap(itemSlot);
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
//                info("树苗 " + sapling + " 已达到最大使用次数，跳过");
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


    // 查找指定范围内的所有树苗
    private List<BlockPos> findSaplings(BlockPos centerPos, int radius, int height) {
        ArrayList<BlockPos> blocc = new ArrayList<>();
        List<BlockPos> blocks = WorldUtils.getSphere(centerPos, radius, height);
        for (BlockPos b : blocks) {
            if (isSapling(b)) {
                blocc.add(b);
            }
        }
        return blocc;
    }


    // 查找合适的种植位置（基于可用的种植物品）
    private BlockPos findPlantLocation() {
        if (availablePlantItems.isEmpty()) {
            return null;
        }

        List<BlockPos> validPositions = new ArrayList<>();
        BlockPos playerPos = MinecraftClient.getInstance().player.getBlockPos();
        List<BlockPos> nearbyPositions = WorldUtils.getSphere(playerPos, rRange.get(), yRange.get());

        // 遍历附近的位置，找到可以种植的位置
        for (BlockPos pos : nearbyPositions) {
            Block groundBlock = MinecraftClient.getInstance().world.getBlockState(pos).getBlock();

            // 检查是否有对应的种植物品
            if (canPlantAtPosition(pos, groundBlock)) {
                // 检查距离玩家的距离，防止在玩家脚下种植（避免卡进方块）
                BlockPos check = pos.up(1);
                double distanceToPlayer = distanceBetween(playerPos, check);
                if (distanceToPlayer >= 1.0) {
                    validPositions.add(pos);
                }
            }
        }

        if (validPositions.isEmpty()) return null;

        // 如果开启了防止堵路功能，过滤掉不在玩家身后的位置
        if (preventBlocking.get()) {
            validPositions = validPositions.stream()
                .filter(this::isPositionBehindPlayer)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

            if (validPositions.isEmpty()) return null;
        }

        // 根据距离排序
        validPositions.sort(Comparator.comparingDouble(PlayerUtils::distanceTo));
        // 根据排序模式决定使用最近还是最远的位置
        if (sortMode.get().equals(SortMode.Farthest)) Collections.reverse(validPositions);
        return validPositions.get(0);
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

    // 获取玩家的移动方向（包括记住的方向）
    private Direction getPlayerMovementDirection() {
        Direction currentDirection = getCurrentMovementDirection();

        // 如果当前有移动方向，更新记住的方向
        if (currentDirection != null) {
            lastMovementDirection = currentDirection;
            return currentDirection;
        }

        // 如果当前没有移动但之前有记住的方向，返回记住的方向
        return lastMovementDirection;
    }

    // 获取当前实时的移动方向
    private Direction getCurrentMovementDirection() {
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

    // 检查位置是否在玩家身后或侧面（不在前方）
    private boolean isPositionBehindPlayer(BlockPos pos) {
        if (!preventBlocking.get()) {
            return true; // 如果没有开启防止堵路，所有位置都可以
        }

        Direction movementDirection = getPlayerMovementDirection();
        if (movementDirection == null) {
            return true; // 如果玩家从未移动，所有位置都可以
        }

        BlockPos playerPos = MinecraftClient.getInstance().player.getBlockPos();

        // 计算位置相对于玩家的方向
        int deltaX = pos.getX() - playerPos.getX();
        int deltaZ = pos.getZ() - playerPos.getZ();

        // 检查位置是否不在移动方向的前方（即在身后或侧面）
        switch (movementDirection) {
            case NORTH:
                return deltaZ >= 0; // 不在北方前面（即在南方或同一行）
            case SOUTH:
                return deltaZ <= 0; // 不在南方前面（即在北方或同一行）
            case WEST:
                return deltaX >= 0; // 不在西方前面（即在东方或同一列）
            case EAST:
                return deltaX <= 0; // 不在东方前面（即在西方或同一列）
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
