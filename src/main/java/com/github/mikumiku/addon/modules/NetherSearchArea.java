package com.github.mikumiku.addon.modules;


import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalXZ;
import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.dynamic.DV;
import com.github.mikumiku.addon.util.PlayerUtil;
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
    public enum SearchPattern {
        Rectangle,
        Spiral
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SearchPattern> searchPattern = sgGeneral.add(new EnumSetting.Builder<SearchPattern>()
        .name("搜索模式")
        .description("在地狱中搜索时使用的模式。")
        .defaultValue(SearchPattern.Spiral)
        .build()
    );

    private final Setting<Integer> searchRadius = sgGeneral.add(new IntSetting.Builder()
        .name("搜索半径")
        .description("搜索区域的半径，以区块为单位。")
        .defaultValue(10)
        .min(1)
        .max(2000)
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
        .name("区块距离")
        .description("搜索点之间的距离，以区块为单位。")
        .defaultValue(8)
        .min(2)
        .max(128)
        .build()
    );

    private final Setting<Integer> targetProximity = sgGeneral.add(new IntSetting.Builder()
        .name("目标接近距离")
        .description("认为到达目标点的距离阈值（方块）。")
        .defaultValue(64)
        .min(2)
        .max(500)
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

        ChatUtils.info("开始地狱搜索，共 " + searchPoints.size() + " 个点，使用 " + searchPattern.get().name() + " 模式。");

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

        int radius = searchRadius.get() * 16;
        int distance = chunkDistance.get() * 16;

        switch (searchPattern.get()) {
            case Spiral:
                generateSpiralPoints(startPos, radius, distance);
                break;
            case Rectangle:
                generateRectanglePoints(startPos, radius, distance);
                break;
        }

        ChatUtils.info("生成了 " + searchPoints.size() + " 个搜索点。");
    }

    private void generateSpiralPoints(BlockPos center, int radius, int distance) {
        int x = 0, z = 0;
        int dx = 0, dz = -1;
        int maxSteps = (2 * radius + 1) * (2 * radius + 1);

        for (int i = 0; i < maxSteps; i++) {
            if (Math.abs(x) <= radius && Math.abs(z) <= radius) {
                BlockPos point = new BlockPos(center.getX() + x * distance, center.getY(), center.getZ() + z * distance);
                searchPoints.add(point);
            }

            if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
                int temp = dx;
                dx = -dz;
                dz = temp;
            }
            x += dx;
            z += dz;
        }
    }

    private void generateRectanglePoints(BlockPos center, int radius, int distance) {
        // 中心点
        BlockPos point = new BlockPos(center.getX(), center.getY(), center.getZ());
        searchPoints.add(point);

        // 螺旋向外搜索矩形边界
        for (int layer = 1; layer <= radius; layer++) {
            // 上边：从左到右
            for (int x = -layer; x <= layer; x++) {
                BlockPos p = new BlockPos(center.getX() + x * distance, center.getY(), center.getZ() + (-layer) * distance);
                searchPoints.add(p);
            }

            // 右边：从上到下（跳过右上角，因为已经添加过）
            for (int z = -layer + 1; z <= layer; z++) {
                BlockPos p = new BlockPos(center.getX() + layer * distance, center.getY(), center.getZ() + z * distance);
                searchPoints.add(p);
            }

            // 下边：从右到左（跳过右下角，因为已经添加过）
            for (int x = layer - 1; x >= -layer; x--) {
                BlockPos p = new BlockPos(center.getX() + x * distance, center.getY(), center.getZ() + layer * distance);
                searchPoints.add(p);
            }

            // 左边：从下到上（跳过左下角和左上角，因为已经添加过）
            for (int z = layer - 1; z >= -layer + 1; z--) {
                BlockPos p = new BlockPos(center.getX() + (-layer) * distance, center.getY(), center.getZ() + z * distance);
                searchPoints.add(p);
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

        Vec3d playerPos = DV.of(PlayerUtil.class).getEntityPos(mc.player);
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
