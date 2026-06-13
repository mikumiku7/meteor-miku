package com.github.mikumiku.addon.modules;


import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalXZ;
import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.Via;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class NetherSearchArea extends BaseModule {
    public enum SearchMode {
        Spiral("螺旋模式"),      // 螺旋模式
        Sequential("逐行模式")  // 逐行模式
        ;
        private final String displayName;

        SearchMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum SearchShape {
        Rectangle("矩形"),   // 矩形形状
        Spiral("螺旋")      // 螺旋形状
        ;
        private final String displayName;

        SearchShape(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SearchMode> searchMode = sgGeneral.add(new EnumSetting.Builder<SearchMode>()
        .name("搜索模式")
        .description("螺旋：从中心螺旋向外；逐行：按行扫描。")
        .defaultValue(SearchMode.Spiral)
        .build()
    );

    private final Setting<SearchShape> searchShape = sgGeneral.add(new EnumSetting.Builder<SearchShape>()
        .name("搜索形状")
        .description("矩形：方形区域；螺旋：圆形螺旋。")
        .defaultValue(SearchShape.Rectangle)
        .build()
    );

    private final Setting<Integer> searchRadius = sgGeneral.add(new IntSetting.Builder()
        .name("搜索半径")
        .description("搜索区域的半径，以区块为单位。")
        .defaultValue(80)
        .min(1)
        .max(10000)
        .sliderRange(10, 10000)
        .build()
    );

    private final Setting<Integer> searchSpeed = sgGeneral.add(new IntSetting.Builder()
        .name("搜索速度")
        .description("每次移动之间的刻数间隔（数值越小速度越快）。")
        .defaultValue(20)
        .min(1)
        .max(100)
        .build()
    );

    private final Setting<Boolean> autoStart = sgGeneral.add(new BoolSetting.Builder()
        .name("自动开始")
        .description("进入地狱时自动开始搜索。")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> chunkDistance = sgGeneral.add(new IntSetting.Builder()
        .name("点间距离")
        .description("搜索点之间的距离，以区块为单位。如果加载距离为8, 建议设置为16。")
        .defaultValue(16)
        .min(2)
        .max(128)
        .build()
    );

    private final Setting<Integer> targetProximity = sgGeneral.add(new IntSetting.Builder()
        .name("目标接近距离")
        .description("认为到达目标点的距离阈值（方块）。")
        .defaultValue(48)
        .min(2)
        .max(500)
        .sliderRange(2, 128)
        .build()
    );

    private List<BlockPos> searchPoints;
    private int currentPointIndex;
    private BlockPos startPos;
    private BlockPos currentTarget;
    private int tickCounter;
    private boolean isSearching;
    private boolean wasInNether;

    public NetherSearchArea() {
        super("地狱扫图", "在地狱中系统性地搜索结构或物品的区域。");
        reset();
    }

    @Override
    public void onActivate() {
        if (mc.player == null || mc.world == null) {
            ChatUtils.error("Player or world is null!");
            toggle();
            return;
        }

        if (!isInNether()) {
            ChatUtils.warning("你必须在地狱中才能使用此模块！");
            if (!autoStart.get()) {
                toggle();
                return;
            }
        }

        startSearch();
    }

    @Override
    public void onDeactivate() {
        stopSearch();
        reset();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // Auto-start when entering nether
        if (autoStart.get() && !wasInNether && isInNether() && !isActive()) {
            toggle();
            return;
        }
        wasInNether = isInNether();

        if (!isInNether() && isActive()) {
            ChatUtils.warning("离开了地狱，停止搜索。");
            toggle();
            return;
        }

        if (!isSearching) return;

        // 检查是否还在寻路中，如果是则等待
//        if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
//            return;
//        }

        tickCounter++;
        if (tickCounter < searchSpeed.get()) return;
        tickCounter = 0;

        if (searchPoints == null || searchPoints.isEmpty()) {
            ChatUtils.error("未生成搜索点！");
            toggle();
            return;
        }

        // 检查搜索是否完成
        if (currentPointIndex >= searchPoints.size()) {
            ChatUtils.info("地狱搜索完成！访问了 " + searchPoints.size() + " 个点。");
            toggle();
            return;
        }

        // 移动到下一个搜索点
        BlockPos targetPos = searchPoints.get(currentPointIndex);

        // 检查是否足够接近当前目标
        if (isCloseEnoughToTarget(targetPos)) {
            currentPointIndex++;
            ChatUtils.info("到达点 " + currentPointIndex + "/" + searchPoints.size());

            if (currentPointIndex < searchPoints.size()) {
                BlockPos nextTarget = searchPoints.get(currentPointIndex);
                goToPosition(nextTarget);
            }
        } else {
            // 继续移动到当前目标
            goToPosition(targetPos);
        }
    }

    private void startSearch() {
        if (mc.player == null) return;

        startPos = mc.player.getBlockPos();
        generateSearchPoints();
        currentPointIndex = 0;
        tickCounter = 0;
        isSearching = true;

        ChatUtils.info("开始地狱搜索，共 " + searchPoints.size() + " 个点，使用 " +
            searchMode.get().name() + " 模式 + " + searchShape.get().name() + " 形状。");

        if (!searchPoints.isEmpty()) {
            goToPosition(searchPoints.get(0));
        }
    }

    private void stopSearch() {
        isSearching = false;
        // Cancel any active baritone pathfinding
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        ChatUtils.info("地狱搜索已停止。");
    }

    private void generateSearchPoints() {
        searchPoints = new ArrayList<>();

        if (startPos == null) return;

        int radius = searchRadius.get();
        int distance = chunkDistance.get() * 16;

        SearchMode mode = searchMode.get();
        SearchShape shape = searchShape.get();

        if (shape == SearchShape.Spiral) {
            generateSpiralShapePoints(startPos, radius, distance);
        } else {
            generateRectangleShapePoints(startPos, radius, distance, mode);
        }

        ChatUtils.info("生成了 " + searchPoints.size() + " 个搜索点。");
    }

    /**
     * 生成螺旋形状的点（阿基米德螺旋）
     * 这种形状不区分模式，因为螺旋本身就是从中心向外的
     */
    private void generateSpiralShapePoints(BlockPos center, int radius, int distance) {
        double angle = 0;
        double r = 0;
        double angleStep = 0.5;
        double radiusStep = distance / (2 * Math.PI);

        int maxRadius = radius * 16;

        while (r <= maxRadius) {
            int x = (int) Math.round(r * Math.cos(angle));
            int z = (int) Math.round(r * Math.sin(angle));

            BlockPos point = new BlockPos(center.getX() + x, center.getY(), center.getZ() + z);
            searchPoints.add(point);

            angle += angleStep;
            r += radiusStep * angleStep;
        }
    }

    /**
     * 生成矩形形状的点
     * 支持两种模式：螺旋模式和逐行模式
     */
    private void generateRectangleShapePoints(BlockPos center, int radius, int distance, SearchMode mode) {
        if (mode == SearchMode.Sequential) {
            // 逐行模式：从上到下，从左到右扫描
            for (int z = -radius; z <= radius; z++) {
                for (int x = -radius; x <= radius; x++) {
                    BlockPos point = new BlockPos(
                        center.getX() + x * distance,
                        center.getY(),
                        center.getZ() + z * distance
                    );
                    searchPoints.add(point);
                }
            }
        } else {
            // 螺旋模式：从中心开始顺时针螺旋向外
            // 方向：右、下、左、上（顺时针）
            int[][] directions = {
                {1, 0},   // 右
                {0, 1},   // 下
                {-1, 0},  // 左
                {0, -1}   // 上
            };

            int x = 0, z = 0;
            int dirIdx = 0;
            int steps = 1;
            int turnsAtCurrentStep = 0;

            // 添加起始点（中心）
            BlockPos point = new BlockPos(center.getX(), center.getY(), center.getZ());
            searchPoints.add(point);

            // 螺旋向外扩展
            int maxSteps = radius * 4;
            while (steps <= maxSteps) {
                int[] dir = directions[dirIdx];

                for (int i = 0; i < steps; i++) {
                    x += dir[0];
                    z += dir[1];

                    // 检查是否在半径范围内
                    if (Math.abs(x) <= radius && Math.abs(z) <= radius) {
                        BlockPos p = new BlockPos(
                            center.getX() + x * distance,
                            center.getY(),
                            center.getZ() + z * distance
                        );
                        searchPoints.add(p);
                    }
                }

                dirIdx = (dirIdx + 1) % 4;
                turnsAtCurrentStep++;

                // 每转两次方向，步数增加1
                if (turnsAtCurrentStep == 2) {
                    steps++;
                    turnsAtCurrentStep = 0;
                }
            }
        }
    }

    private void goToPosition(BlockPos pos) {
        if (currentTarget != null && currentTarget.getX() == pos.getX() && currentTarget.getZ() == pos.getZ()) {
            return; // Same target, no need to path again
        }

        currentTarget = pos;
        try {
            BaritoneAPI.getProvider().getPrimaryBaritone().getElytraProcess().pathTo(new GoalXZ(pos.getX(), pos.getZ()));
        } catch (Exception e) {
            ChatUtils.error("寻路失败: " + e.getMessage());
        }
    }

    private boolean isCloseEnoughToTarget(BlockPos target) {
        if (mc.player == null) return false;

        Vec3d playerPos = Via.getEntityPos(mc.player);
        double distance = Math.sqrt(
            Math.pow(playerPos.x - target.getX(), 2) +
                Math.pow(playerPos.z - target.getZ(), 2)
        );

        return distance < targetProximity.get();
    }

    private boolean isInNether() {
        if (mc.world == null) return false;

        return PlayerUtils.getDimension().equals(Dimension.Nether);
    }

    private void reset() {
        searchPoints = null;
        currentPointIndex = 0;
        startPos = null;
        currentTarget = null;
        tickCounter = 0;
        isSearching = false;
        wasInNether = false;
    }

    public int getCurrentPointIndex() {
        return currentPointIndex;
    }

    public int getTotalPoints() {
        return searchPoints != null ? searchPoints.size() : 0;
    }

    public boolean isSearching() {
        return isSearching;
    }
}
