package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.BaritoneUtil;
import com.github.mikumiku.addon.util.WorldUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * 高速清路者模块 - 自动清理道路上的方块
 *
 * <p>自动检测玩家所在的方向，并清理道路上的方块。
 * 根据玩家坐标绝对值最大的方向来确定清理方向，只挖掘指定路面宽度范围内的方块。</p>
 *
 * <p>特色功能：</p>
 * <ul>
 *   <li>智能方向检测：根据坐标自动确定清理方向</li>
 *   <li>路面保护：只挖掘路面范围内的方块，保护路面完整性</li>
 *   <li>宽度控制：可配置的路面宽度</li>
 *   <li>范围控制：可配置的清理范围</li>
 *   <li>数量控制：可限制单次挖掘的方块数量</li>
 *   <li>自动切换：自动切换到合适的工具</li>
 *   <li>方块过滤：可选择要清理的方块类型</li>
 * </ul>
 *
 * @author MikuMiku
 * @since 1.0.0
 */
public class HighwayClearer extends BaseModule {

    // 设置组
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgFilter = settings.createGroup("过滤");
    private final SettingGroup sgRender = settings.createGroup("渲染");

    // 清理范围设置
    private final Setting<Integer> clearRange = sgGeneral.add(new IntSetting.Builder()
        .name("清理范围")
        .description("在多远的位置清理方块")
        .defaultValue(5)
        .min(1)
        .sliderMax(7)
        .build());

    // 路宽度设置
    private final Setting<Integer> roadWidth = sgGeneral.add(new IntSetting.Builder()
        .name("路宽度")
        .description("路的宽度，只挖掘这个范围内的方块")
        .defaultValue(5)
        .min(1)
        .sliderMax(9)
        .build());

    // 最大挖掘数量设置
    private final Setting<Integer> maxMineCount = sgGeneral.add(new IntSetting.Builder()
        .name("最大挖掘数量")
        .description("每次最多挖掘的方块数量")
        .defaultValue(2)
        .min(1)
        .sliderMax(5)
        .build());

    // 挖掘延迟设置
    private final Setting<Integer> mineDelay = sgGeneral.add(new IntSetting.Builder()
        .name("挖掘延迟")
        .description("挖掘方块之间的延迟（tick）")
        .defaultValue(0)
        .min(0)
        .sliderMax(20)
        .build());

    // 自动切换设置
    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("自动切换")
        .description("自动切换到合适的工具")
        .defaultValue(true)
        .build());

    // 仅下界设置
    private final Setting<Boolean> onlyNether = sgGeneral.add(new BoolSetting.Builder()
        .name("仅下界")
        .description("只在下界生效")
        .defaultValue(true)
        .build());

    // 方向设置
    private final Setting<Direction> direction = sgGeneral.add(new EnumSetting.Builder<Direction>()
        .name("方向")
        .description("选择清理方向")
        .defaultValue(Direction.All)
        .build());

    // 排序模式设置
    private final Setting<SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<SortMode>()
        .name("排序模式")
        .description("如何排序要清理的位置")
        .defaultValue(SortMode.Farthest)
        .build());

    // 方块过滤模式设置
    private final Setting<ListMode> listMode = sgFilter.add(new EnumSetting.Builder<ListMode>()
        .name("过滤模式")
        .description("选择方块过滤模式")
        .defaultValue(ListMode.Whitelist)
        .build());

    // 白名单设置
    private final Setting<List<Block>> whitelist = sgFilter.add(new BlockListSetting.Builder()
        .name("白名单")
        .description("选择要清理的方块类型（仅在白名单模式下生效）")
        .defaultValue(Blocks.NETHERRACK, Blocks.OBSIDIAN, Blocks.GRAVEL, Blocks.SOUL_SAND)
        .visible(() -> listMode.get() == ListMode.Whitelist)
        .build());

    // 黑名单设置
    private final Setting<List<Block>> blacklist = sgFilter.add(new BlockListSetting.Builder()
        .name("黑名单")
        .description("选择不清理的方块类型（仅在黑名单模式下生效）")
        .defaultValue(Blocks.BEDROCK)
        .visible(() -> listMode.get() == ListMode.Blacklist)
        .build());


    // 渲染设置
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("显示渲染")
        .description("是否显示待清理方块的渲染预览")
        .defaultValue(true)
        .build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("渲染模式")
        .description("选择渲染的显示模式")
        .defaultValue(ShapeMode.Both)
        .build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("侧面颜色")
        .description("待清理方块的侧面填充颜色")
        .defaultValue(new SettingColor(255, 165, 0, 30)) // 橙色
        .build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("线条颜色")
        .description("待清理方块的线条颜色")
        .defaultValue(new SettingColor(255, 165, 0, 255)) // 橙色
        .build());

    // 内部状态变量
    private int currentDirection = 0; // 0=X+, 1=Z+, 2=X-, 3=Z-
    private int preDirection = -1; // 0=X+, 1=Z+, 2=X-, 3=Z-
    private int totalMined = 0;
    private List<BlockPos> mineList = new ArrayList<>();
    private int tickCounter = 0;
    private int tickBlockCount = 0;

    public HighwayClearer() {
        super(CATEGORY_MIKU_BUILD, "高速清路者", "自动清理道路前方高处的方块, 建议配合[自动走路]一起使用。也可以作为隧道挖掘器、三体平整器等使用。");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        totalMined = 0;
        tickCounter = 0;
        tickBlockCount = 0;
        preDirection = -1;
        mineList.clear();
        info("高速清路者模块已启用！");
    }

    @Override
    public void onDeactivate() {
        info("高速清路者模块已停用！总共清理了 " + totalMined + " 个方块");
        preDirection = -1;
        resetStats();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {

        mineList.clear();
        tickBlockCount = 0;

        // 使用 tick 计时
        if (tickCounter < mineDelay.get()) {
            tickCounter++;
            return;
        }
        tickCounter = 0;

        // 检查是否在下界
        if (onlyNether.get() && !isInNether()) {
            return;
        }

        // 更新当前方向
        updateCurrentDirection();
        if (currentDirection != preDirection) {
            preDirection = currentDirection;
            info(getDirectionInfo());
        }

        // 计算清理位置
        BlockPos playerPos = mc.player.getBlockPos();

        // 根据方向寻找合适的清理位置
        List<BlockPos> spherePositions = WorldUtils.getSphere(playerPos, clearRange.get(), clearRange.get());
        List<BlockPos> validPositions = new ArrayList<>();

        for (BlockPos pos : spherePositions) {
            if (isMinePositionValid(pos, playerPos)) {
                validPositions.add(pos);
            }
        }

        // 根据排序模式排序
        sortPositions(validPositions, playerPos);

        // 按顺序挖掘
        for (BlockPos pos : validPositions) {
            tryMine(pos);
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get() && mineList.size() > 0) {
            for (int i = 0; i < mineList.size(); i++) {
                double x1 = mineList.get(i).getX();
                double y1 = mineList.get(i).getY();
                double z1 = mineList.get(i).getZ();
                double x2 = mineList.get(i).getX() + 1;
                double y2 = mineList.get(i).getY() + 1;
                double z2 = mineList.get(i).getZ() + 1;
                event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }
    }

    /**
     * 检查是否在下界
     */
    private boolean isInNether() {
        if (mc.world == null) return false;
        return PlayerUtils.getDimension().equals(Dimension.Nether);
    }

    /**
     * 更新当前方向
     */
    private void updateCurrentDirection() {
        if (mc.player == null) return;

        Direction selectedDirection = direction.get();

        switch (selectedDirection) {
            case Smart:
                // 智能模式：根据坐标绝对值最大的方向来确定清理方向（和当前逻辑一样）
                double x = Math.abs(mc.player.getX());
                double z = Math.abs(mc.player.getZ());

                if (x > z) {
                    // X方向
                    currentDirection = mc.player.getX() > 0 ? 0 : 2; // X+ 或 X-
                } else {
                    // Z方向
                    currentDirection = mc.player.getZ() > 0 ? 1 : 3; // Z+ 或 Z-
                }
                break;

            case East:
                currentDirection = 0; // X+方向
                break;

            case South:
                currentDirection = 1; // Z+方向
                break;

            case West:
                currentDirection = 2; // X-方向
                break;

            case North:
                currentDirection = 3; // Z-方向
                break;

            case All:
                // 全方向模式：使用一个特殊值表示全方向
                currentDirection = -1;
                break;
        }
    }

    /**
     * 检查挖掘位置是否有效
     */
    private boolean isMinePositionValid(BlockPos pos, BlockPos playerPos) {
        // 只挖掘路面范围内的方块
        if (!isInRoadArea(pos, playerPos)) {
            return false;
        }

        // 计算到玩家的距离
        double distance = Math.sqrt(
            Math.pow(pos.getX() - playerPos.getX(), 2) +
                Math.pow(pos.getY() - playerPos.getY(), 2) +
                Math.pow(pos.getZ() - playerPos.getZ(), 2)
        );

        // 太近的位置不挖掘
        if (distance < 1.0) {
            return false;
        }

        // 检查是否在正确的方向
        if (!isInCorrectDirection(pos, playerPos)) {
            return false;
        }

        // 检查是否是要清理的方块
        return isBlockToMine(pos) && !isBedrock(pos);
    }

    /**
     * 检查位置是否在清理范围内
     */
    private boolean isInRoadArea(BlockPos pos, BlockPos playerPos) {
        int width = roadWidth.get();
        int halfWidth = width / 2;
        double playerY = mc.player.getY() + 0.3;
        // 只挖掘高于玩家的方块
        if (pos.getY() < (int) Math.floor(playerY)) {
            return false;
        }

        // 全方向模式：检查所有方向
        if (currentDirection == -1) {
            int xDiff = Math.abs(pos.getX() - playerPos.getX());
            int zDiff = Math.abs(pos.getZ() - playerPos.getZ());
            return xDiff <= halfWidth && zDiff <= halfWidth;
        }

        // 根据当前方向判断是否在清理范围内
        switch (currentDirection) {
            case 0: // X+方向，检查Z轴方向
            case 2: // X-方向，检查Z轴方向
                int zDiff = Math.abs(pos.getZ() - playerPos.getZ());
                return zDiff <= halfWidth;

            case 1: // Z+方向，检查X轴方向
            case 3: // Z-方向，检查X轴方向
                int xDiff = Math.abs(pos.getX() - playerPos.getX());
                return xDiff <= halfWidth;

            default:
                return false;
        }
    }

    /**
     * 检查是否是基岩
     */
    private boolean isBedrock(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK;
    }

    /**
     * 检查是否在正确的方向
     */
    private boolean isInCorrectDirection(BlockPos pos, BlockPos playerPos) {
        // 全方向模式：不限制方向
        if (currentDirection == -1) {
            return true;
        }

        switch (currentDirection) {
            case 0: // X+方向，在X+方向清理（前方）
                return pos.getX() > playerPos.getX();
            case 1: // Z+方向，在Z+方向清理（前方）
                return pos.getZ() > playerPos.getZ();
            case 2: // X-方向，在X-方向清理（前方）
                return pos.getX() < playerPos.getX();
            case 3: // Z-方向，在Z-方向清理（前方）
                return pos.getZ() < playerPos.getZ();
            default:
                return false;
        }
    }

    /**
     * 检查是否是要清理的方块
     */
    private boolean isBlockToMine(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        if (block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.WATER || block == Blocks.LAVA
        ) {
            return false;
        }
        // 根据过滤模式检查方块
        if (listMode.get() == ListMode.Whitelist) {
            return whitelist.get().contains(block);
        } else {
            return !blacklist.get().contains(block);
        }
    }

    /**
     * 尝试挖掘方块
     */
    private void tryMine(BlockPos pos) {
        if (pos == null) {
            return;
        }
        if (mineList.contains(pos)) {
            return;
        }
        if (tickBlockCount >= maxMineCount.get()) {
            return;
        }
        if (GhostMine.getMiningBlockCount() >= 2) {
            return;
        }

        double distance = BaritoneUtil.getEyesPos().distanceTo(pos.toCenterPos());

        if (distance > clearRange.get()) {
            return;
        }

        mineList.add(pos);

        // 挖掘方块
        BlockUtils.breakBlock(pos, true);
        tickBlockCount++;
        totalMined++;
    }

    /**
     * 根据排序模式排序位置
     */
    private void sortPositions(List<BlockPos> positions, BlockPos playerPos) {
        if (positions.isEmpty()) return;

        // 全方向模式：按距离排序
        if (currentDirection == -1) {
            positions.sort((a, b) -> {
                double aDist = Math.sqrt(
                    Math.pow(a.getX() - playerPos.getX(), 2) +
                        Math.pow(a.getY() - playerPos.getY(), 2) +
                        Math.pow(a.getZ() - playerPos.getZ(), 2)
                );
                double bDist = Math.sqrt(
                    Math.pow(b.getX() - playerPos.getX(), 2) +
                        Math.pow(b.getY() - playerPos.getY(), 2) +
                        Math.pow(b.getZ() - playerPos.getZ(), 2)
                );
                return Double.compare(aDist, bDist);
            });
        } else {
            // 根据当前方向选择排序依据
            switch (currentDirection) {
                case 0: // X+方向，按X坐标排序（从小到大，即远离玩家）
                case 2: // X-方向，按X坐标排序（从大到小，即远离玩家）
                    positions.sort((a, b) -> {
                        double aDist = Math.abs(a.getX() - playerPos.getX());
                        double bDist = Math.abs(b.getX() - playerPos.getX());
                        return Double.compare(aDist, bDist);
                    });
                    break;
                case 1: // Z+方向，按Z坐标排序（从小到大，即远离玩家）
                case 3: // Z-方向，按Z坐标排序（从大到小，即远离玩家）
                    positions.sort((a, b) -> {
                        double aDist = Math.abs(a.getZ() - playerPos.getZ());
                        double bDist = Math.abs(b.getZ() - playerPos.getZ());
                        return Double.compare(aDist, bDist);
                    });
                    break;
            }
        }

        // 根据排序模式决定是否反转
        if (sortMode.get() == SortMode.Farthest) {
            java.util.Collections.reverse(positions);
        }
    }

    /**
     * 获取当前方向信息
     */
    public String getDirectionInfo() {
        if (!isActive()) {
            return "未启用";
        }

        Direction selectedDirection = direction.get();
        String directionInfo;

        if (selectedDirection == Direction.Smart) {
            // 智能模式显示实际方向
            switch (currentDirection) {
                case 0:
                    directionInfo = "智能 - X+轴 (向东清理)";
                    break;
                case 1:
                    directionInfo = "智能 - Z+轴 (向南清理)";
                    break;
                case 2:
                    directionInfo = "智能 - X-轴 (向西清理)";
                    break;
                case 3:
                    directionInfo = "智能 - Z-轴 (向北清理)";
                    break;
                default:
                    directionInfo = "智能 - 未知";
                    break;
            }
        } else {
            // 其他模式直接显示选择的模式
            switch (selectedDirection) {
                case East:
                    directionInfo = "东向 (X+轴)";
                    break;
                case South:
                    directionInfo = "南向 (Z+轴)";
                    break;
                case West:
                    directionInfo = "西向 (X-轴)";
                    break;
                case North:
                    directionInfo = "北向 (Z-轴)";
                    break;
                case All:
                    directionInfo = "全方向";
                    break;
                default:
                    directionInfo = "未知";
                    break;
            }
        }

        return String.format("%s | 已清理: %d", directionInfo, totalMined);
    }

    /**
     * 重置统计
     */
    public void resetStats() {
        totalMined = 0;
        info("统计已重置");
    }

    /**
     * 方向模式枚举
     */
    public enum Direction {
        Smart("智能"),
        East("东"),
        South("南"),
        West("西"),
        North("北"),
        All("全方向");

        private final String displayName;

        Direction(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }


    /**
     * 排序模式枚举
     */
    public enum SortMode {
        Closest("最近优先"),
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
