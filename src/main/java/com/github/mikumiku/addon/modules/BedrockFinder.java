package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.HashSet;
import java.util.Set;

/**
 * 基岩寻找器模块
 * 在下界顶层寻找3x3的基岩区域
 *
 * @author MikuMiku
 */
public class BedrockFinder extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("渲染");

    // 搜索设置
    private final Setting<Integer> searchRange = sgGeneral.add(new IntSetting.Builder()
        .name("搜索范围")
        .description("搜索基岩的范围（方块）")
        .defaultValue(128)
        .min(16)
        .max(256)
        .sliderRange(16, 128)
        .build()
    );

    private final Setting<Integer> searchSpeed = sgGeneral.add(new IntSetting.Builder()
        .name("搜索速度")
        .description("每个tick检查的方块数量")
        .defaultValue(3600)
        .min(10)
        .max(20000)
        .sliderRange(10, 10000)
        .build()
    );

    private final Setting<Boolean> notifyOnFind = sgGeneral.add(new BoolSetting.Builder()
        .name("找到时通知")
        .description("找到3x3基岩时发送聊天消息")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoPause = sgGeneral.add(new BoolSetting.Builder()
        .name("找到后暂停")
        .description("找到3x3基岩后自动暂停搜索")
        .defaultValue(false)
        .build()
    );

    // 渲染设置
    private final Setting<Boolean> renderFound = sgRender.add(new BoolSetting.Builder()
        .name("渲染找到的区域")
        .description("渲染找到的3x3基岩区域")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> xRayRender = sgRender.add(new BoolSetting.Builder()
        .name("透视渲染")
        .description("是否透视渲染（可以隔墙看到）")
        .defaultValue(true)
        .visible(renderFound::get)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("形状模式")
        .description("渲染形状模式")
        .defaultValue(ShapeMode.Both)
        .visible(renderFound::get)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("侧面颜色")
        .description("3x3基岩区域的侧面颜色")
        .defaultValue(new SettingColor(0, 120, 255, 50))
        .visible(renderFound::get)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("线条颜色")
        .description("3x3基岩区域的线条颜色")
        .defaultValue(new SettingColor(20, 146, 230, 255))
        .visible(renderFound::get)
        .build()
    );

    // 内部状态
    private BlockPos centerPos;
    private int searchRadius = 0;
    private int currentAngle = 0;
    private int blocksChecked = 0;
    private int totalBlocksChecked = 0;
    private boolean searching = true;

    // 缓存已找到的3x3基岩区域
    private final Set<BlockPos> foundBedrockAreas = new HashSet<>();

    // 缓存已检查过的位置（避免重复检查）
    private final Set<BlockPos> checkedPositions = new HashSet<>();

    public BedrockFinder() {
        super("杀雕机基岩寻找", "寻找适用于杀雕机的3x3基岩区域：Y=126-123之间3x3基岩+中心点上方基岩+中心点下方2格非基岩");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        resetSearch();
        info("开始搜索3x3基岩区域...");
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        info("基岩搜索已停止。总共检查了 " + totalBlocksChecked + " 个方块，找到了 " + foundBedrockAreas.size() + " 个3x3基岩区域。");
    }

    private void resetSearch() {
        if (mc.player == null || mc.world == null) return;

        centerPos = mc.player.getBlockPos();
        searchRadius = 0;
        currentAngle = 0;
        blocksChecked = 0;
        totalBlocksChecked = 0;
        searching = true;
        foundBedrockAreas.clear();
        checkedPositions.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || !isInNether()) {
            error("必须在下界才能使用基岩寻找器！");
            toggle();
            return;
        }

        if (!searching) return;

        // 每个tick检查指定数量的方块
        int blocksToCheck = searchSpeed.get();
        int checkedThisTick = 0;

        while (checkedThisTick < blocksToCheck && searching) {
            if (!checkNextPosition()) {
                searching = false;
                break;
            }
            checkedThisTick++;
            blocksChecked++;
            totalBlocksChecked++;
        }

        // 每检查完一个完整圈后增加搜索半径
        if (blocksChecked >= searchRadius * searchRadius * 4) {
            searchRadius++;
            blocksChecked = 0;

            // 检查是否超出搜索范围
            if (searchRadius > searchRange.get()) {
                searching = false;
                info("搜索完成！搜索范围已达到最大值 " + searchRange.get() + " 格。");
                info("总共检查了 " + totalBlocksChecked + " 个方块，找到了 " + foundBedrockAreas.size() + " 个3x3基岩区域。");

                if (autoPause.get() && !foundBedrockAreas.isEmpty()) {
                    searching = false;
                }
            }
        }
    }

    private boolean checkNextPosition() {
        // 螺旋搜索算法
        int x = centerPos.getX() + (int) (searchRadius * Math.cos(currentAngle * Math.PI / 180));
        int z = centerPos.getZ() + (int) (searchRadius * Math.sin(currentAngle * Math.PI / 180));

        // 检查Y=126到Y=123之间的基岩
        for (int y = 126; y >= 123; y--) {
            BlockPos checkPos = new BlockPos(x, y, z);

            // 避免重复检查
            if (checkedPositions.contains(checkPos)) continue;
            checkedPositions.add(checkPos);

            // 检查是否为基岩
            if (mc.world.getBlockState(checkPos).getBlock() == Blocks.BEDROCK) {
                // 检查是否为3x3基岩区域的中心
                if (checkFor3x3Bedrock(checkPos)) {
                    return true; // 找到了，继续下一个位置
                }
            }
        }

        // 更新角度
        currentAngle += 360 / Math.max(1, searchRadius * 8); // 根据半径调整角度步长
        if (currentAngle >= 360) {
            currentAngle = 0;
        }

        return true;
    }

    private boolean checkFor3x3Bedrock(BlockPos center) {
        // 检查是否为3x3基岩区域
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = center.add(x, 0, z);
                if (mc.world.getBlockState(checkPos).getBlock() != Blocks.BEDROCK) {
                    return false;
                }
            }
        }

        // 检查中心点上方一个是否为基岩
        BlockPos aboveCenter = center.up();
        if (mc.world.getBlockState(aboveCenter).getBlock() != Blocks.BEDROCK) {
            return false;
        }

        // 检查中心点下方2格是否为基岩（这个位置不能是基岩）
        BlockPos belowTwoCenter = center.down(2);
        if (mc.world.getBlockState(belowTwoCenter).getBlock() == Blocks.BEDROCK) {
            return false;
        }
        // 检查 belowTwoCenter 平面的4个方向至少有一个方向有完整的3x3非基岩区
        if (!checkForNonBedrockArea(belowTwoCenter)) {
            return false;
        }

        // 检查是否已经找到过这个区域
        if (foundBedrockAreas.contains(center)) {
            return false;
        }

        // 找到了3x3基岩区域！
        foundBedrockAreas.add(center);

        if (notifyOnFind.get()) {
            info("🎉 找到3x3基岩区域！位置: " + center.getX() + ", " + center.getY() + ", " + center.getZ());
            info("距离: " + (int) mc.player.getBlockPos().getManhattanDistance(center) + " 格");
        }

        if (autoPause.get()) {
            searching = false;
            info("搜索已暂停，找到了目标位置。");
        }

        return true;
    }


    /**
     * 该方法检查 belowTwoCenter 平面的4个方向（北、南、东、西）
     * 对每个方向，检查平移一格后以该点为中心的3x3区域是否全部为非基岩方块
     * 只要有一个方向满足条件（有完整的3x3非基岩区），就返回 true
     * 如果4个方向都不满足条件，返回 false，整个检查失败
     *
     * @param belowTwoCenter
     * @return
     */
    private boolean checkForNonBedrockArea(BlockPos belowTwoCenter) {
        // 定义4个方向的偏移量 (北、南、东、西)
        int[][] directions = {
            {0, 1},   // 北 (+Z)
            {0, -1},  // 南 (-Z)
            {1, 0},   // 东 (+X)
            {-1, 0}   // 西 (-X)
        };

        // 检查每个方向
        for (int[] dir : directions) {
            BlockPos directionCenter = belowTwoCenter.add(dir[0], 0, dir[1]);

            // 检查以 directionCenter 为中心的3x3区域是否全部不是基岩
            boolean hasComplete3x3NonBedrock = true;
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos checkPos = directionCenter.add(x, 0, z);
                    if (mc.world.getBlockState(checkPos).getBlock() == Blocks.BEDROCK) {
                        hasComplete3x3NonBedrock = false;
                        break;
                    }
                }
                if (!hasComplete3x3NonBedrock) {
                    break;
                }
            }

            // 如果找到至少一个方向有完整的3x3非基岩区，返回true
            if (hasComplete3x3NonBedrock) {
                return true;
            }
        }

        // 4个方向都没有完整的3x3非基岩区
        return false;
    }

    /**
     * 检查是否在下界
     */
    private boolean isInNether() {
        if (mc.world == null) return false;

        return PlayerUtils.getDimension().equals(Dimension.Nether);
    }

    /**
     * 检查玩家是否能直接看到指定位置（不透视模式）
     */
    private boolean canSeePosition(BlockPos pos) {
        if (mc.player == null || mc.world == null) return false;

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d targetPos = Vec3d.ofCenter(pos);

        // 进行射线检测，检查是否有方块遮挡
        RaycastContext context = new RaycastContext(
            eyePos,
            targetPos,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            mc.player
        );

        return mc.world.raycast(context).getType() == net.minecraft.util.hit.HitResult.Type.MISS;
    }


    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!renderFound.get() || foundBedrockAreas.isEmpty()) return;

        // 渲染所有找到的3x3基岩区域
        for (BlockPos center : foundBedrockAreas) {
            // 如果是不透视模式，检查玩家是否能看到这个位置
            if (!xRayRender.get() && !canSeePosition(center)) {
                continue;
            }
            // 渲染3x3基岩区域
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = center.add(x, 0, z);

                    // 如果是不透视模式，检查玩家是否能看到这个具体方块
                    if (!xRayRender.get() && !canSeePosition(pos)) {
                        continue;
                    }

                    double x1 = pos.getX();
                    double y1 = pos.getY();
                    double z1 = pos.getZ();
                    double x2 = pos.getX() + 1;
                    double y2 = pos.getY() + 1;
                    double z2 = pos.getZ() + 1;

                    event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                }
            }

            // 高亮中心点上方的基岩
            BlockPos aboveCenter = center.up();

            // 如果是不透视模式，检查玩家是否能看到这个位置
            boolean canSeeAbove = xRayRender.get() || canSeePosition(aboveCenter);
            if (canSeeAbove) {
                double x1 = aboveCenter.getX();
                double y1 = aboveCenter.getY();
                double z1 = aboveCenter.getZ();
                double x2 = aboveCenter.getX() + 1;
                double y2 = aboveCenter.getY() + 1;
                double z2 = aboveCenter.getZ() + 1;

                event.renderer.box(x1, y1, z1, x2, y2, z2,
                    new SettingColor(255, 255, 0, 120), // 黄色
                    new SettingColor(255, 255, 0, 255), // 亮黄色
                    shapeMode.get(), 0);
            }

            // 高亮中心点下方2格的位置（关键位置，不能是基岩）
            BlockPos belowTwoCenter = center.down(2);

            // 如果是不透视模式，检查玩家是否能看到这个位置
            boolean canSeeBelow = xRayRender.get() || canSeePosition(belowTwoCenter);
            if (canSeeBelow) {
                double x1 = belowTwoCenter.getX();
                double y1 = belowTwoCenter.getY();
                double z1 = belowTwoCenter.getZ();
                double x2 = belowTwoCenter.getX() + 1;
                double y2 = belowTwoCenter.getY() + 1;
                double z2 = belowTwoCenter.getZ() + 1;

                // 用绿色渲染这个关键位置，表示这里需要是空的
                event.renderer.box(x1, y1, z1, x2, y2, z2,
                    new SettingColor(0, 255, 0, 100), // 绿色
                    new SettingColor(0, 255, 0, 200), // 亮绿色
                    shapeMode.get(), 0);
            }
        }
    }

    /**
     * 获取搜索状态信息
     */
    public String getSearchStatus() {
        if (!isActive()) return "基岩寻找器: 未启用";

        String status = searching ? "搜索中" : "已暂停";
        return String.format("基岩寻找器: %s | 半径: %d/%d | 已检查: %d | 找到: %d",
            status, searchRadius, searchRange.get(), totalBlocksChecked, foundBedrockAreas.size());
    }

    /**
     * 清除已找到的基岩区域缓存
     */
    public void clearFoundAreas() {
        foundBedrockAreas.clear();
        info("已清除所有找到的基岩区域缓存。");
    }

    /**
     * 重新开始搜索
     */
    public void restartSearch() {
        resetSearch();
        info("重新开始搜索3x3基岩区域...");
    }
}
