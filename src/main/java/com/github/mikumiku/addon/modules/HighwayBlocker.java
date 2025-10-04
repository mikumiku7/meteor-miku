package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.BagUtil;
import com.github.mikumiku.addon.util.BaritoneUtil;
import com.github.mikumiku.addon.util.WorldUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 高速路堵塞模块 - 自动在下界放置方块堵塞高速路
 *
 * <p>自动检测玩家所在的方向，并在身后放置方块来堵塞下界高速路。
 * 根据玩家坐标绝对值最大的方向来确定堵塞方向。</p>
 *
 * <p>特色功能：</p>
 * <ul>
 *   <li>智能方向检测：根据坐标自动确定堵塞方向</li>
 *   <li>安全放置检查：只在不影响玩家的位置放置</li>
 *   <li>范围限制：可配置的放置范围</li>
 *   <li>数量控制：可限制单次放置的方块数量</li>
 *   <li>自动切换：自动切换到选定的方块</li>
 *   <li>方块选择：用户可以选择任意方块类型</li>
 * </ul>
 *
 * @author MikuMiku
 * @since 1.0.0
 */
public class HighwayBlocker extends BaseModule {

    // 设置组
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("渲染");

    // 方块类型设置
    private final Setting<Block> blockType = sgGeneral.add(new BlockSetting.Builder()
        .name("方块类型")
        .description("选择用于堵塞的方块类型")
        .defaultValue(Blocks.OBSIDIAN)
        .build());

    // 放置范围设置
    private final Setting<Integer> placeRange = sgGeneral.add(new IntSetting.Builder()
        .name("放置范围")
        .description("在身后多远的位置放置方块")
        .defaultValue(4)
        .min(1)
        .sliderMax(10)
        .build());

    // 最大放置数量设置
    private final Setting<Integer> maxPlaceCount = sgGeneral.add(new IntSetting.Builder()
        .name("最大放置数量")
        .description("每次最多放置的黑曜石数量")
        .defaultValue(2)
        .min(1)
        .sliderMax(5)
        .build());

    // 延迟设置
    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("放置延迟")
        .description("放置黑曜石之间的延迟（tick）")
        .defaultValue(0)
        .min(0)
        .sliderMax(20)
        .build());

    // 排序模式设置
    private final Setting<SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<SortMode>()
        .name("排序模式")
        .description("如何排序要放置的位置")
        .defaultValue(SortMode.Farthest)
        .build());

    // 仅下界设置
    private final Setting<Boolean> onlyNether = sgGeneral.add(new BoolSetting.Builder()
        .name("仅下界")
        .description("只在下界生效")
        .defaultValue(true)
        .build());

    // 渲染设置
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("显示渲染")
        .description("是否显示待放置黑曜石的渲染预览")
        .defaultValue(true)
        .build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("渲染模式")
        .description("选择渲染的显示模式")
        .defaultValue(ShapeMode.Both)
        .build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("侧面颜色")
        .description("待放置黑曜石的侧面填充颜色")
        .defaultValue(new SettingColor(128, 0, 128, 30)) // 紫色
        .build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("线条颜色")
        .description("待放置黑曜石的线条颜色")
        .defaultValue(new SettingColor(128, 0, 128, 255)) // 紫色
        .build());

    // 内部状态变量
    private int currentDirection = 0; // 0=X+, 1=Z+, 2=X-, 3=Z-
    private int preDirection = -1; // 0=X+, 1=Z+, 2=X-, 3=Z-
    private int totalPlaced = 0;
    private List<BlockPos> placeList = new ArrayList<>();
    private int tickCounter = 0;
    private int tickBlockCount = 0;

    public HighwayBlocker() {
        super("高速堵路者", "自动在下界放置方块堵塞高速路, 建议配合[一键补给]、[自动走路] 一起使用。");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        totalPlaced = 0;
        tickCounter = 0;
        tickBlockCount = 0;
        preDirection = -1;
        placeList.clear();
        info("高速路堵塞模块已启用！");
    }

    @Override
    public void onDeactivate() {
        info("高速路堵塞模块已停用！总共放置了 " + totalPlaced + " 个 " + blockType.get().getName().getString());
        preDirection = -1;
        resetStats();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {

        placeList.clear();
        tickBlockCount = 0;

        // 使用 tick 计时
        if (tickCounter < placeDelay.get()) {
            tickCounter++;
            return;
        }
        tickCounter = 0;

        // 检查是否在下界
        if (onlyNether.get() && !isInNether()) {
            return;
        }

        // 检查是否有选定的方块
        if (!BagUtil.find(blockType.get().asItem()).found()) {
            info("没有找到 " + blockType.get().getName().getString() + "，停止模块");
            toggle();
            return;
        }

        // 更新当前方向
        updateCurrentDirection();
        if (currentDirection != preDirection) {
            preDirection = currentDirection;
            info(getDirectionInfo());
        }

        // 计算放置位置
        BlockPos playerPos = mc.player.getBlockPos();

        // 根据方向寻找合适的放置位置
        List<BlockPos> spherePositions = WorldUtils.getSphere(placeRange.get());
        List<BlockPos> validPositions = new ArrayList<>();

        for (BlockPos pos : spherePositions) {
            if (isPlacePositionValid(pos, playerPos)) {
                validPositions.add(pos);
            }
        }

        // 根据排序模式排序
        sortPositions(validPositions, playerPos);

        // 按顺序放置
        for (BlockPos pos : validPositions) {
            tryPlace(pos);
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get() && placeList.size() > 0 && BagUtil.find(blockType.get().asItem()).found()) {
            for (int i = 0; i < placeList.size(); i++) {
                double x1 = placeList.get(i).getX();
                double y1 = placeList.get(i).getY();
                double z1 = placeList.get(i).getZ();
                double x2 = placeList.get(i).getX() + 1;
                double y2 = placeList.get(i).getY() + 1;
                double z2 = placeList.get(i).getZ() + 1;
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
     * 查找黑曜石槽位
     */
    private int findObsidianSlot() {
        // 搜索快捷栏
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.OBSIDIAN) {
                return i;
            }
        }

        // 搜索主物品栏
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.OBSIDIAN) {
                return i;
            }
        }

        return -1;
    }

    /**
     * 更新当前方向
     */
    private void updateCurrentDirection() {
        if (mc.player == null) return;

        double x = Math.abs(mc.player.getX());
        double z = Math.abs(mc.player.getZ());

        if (x > z) {
            // X方向
            currentDirection = mc.player.getX() > 0 ? 0 : 2; // X+ 或 X-
        } else {
            // Z方向
            currentDirection = mc.player.getZ() > 0 ? 1 : 3; // Z+ 或 Z-
        }
    }

    /**
     * 检查放置位置是否有效
     */
    private boolean isPlacePositionValid(BlockPos pos, BlockPos playerPos) {
        // 计算到玩家的距离
        double distance = Math.sqrt(
            Math.pow(pos.getX() - playerPos.getX(), 2) +
                Math.pow(pos.getY() - playerPos.getY(), 2) +
                Math.pow(pos.getZ() - playerPos.getZ(), 2)
        );

        // 太近的位置不放置
        if (distance < 0.2) {
            return false;
        }

        // 检查是否在正确的方向
        if (!isInCorrectDirection(pos, playerPos)) {
            return false;
        }

        // 检查是否可以放置
        return canPlaceAt(pos);
    }

    /**
     * 检查是否在正确的方向
     */
    private boolean isInCorrectDirection(BlockPos pos, BlockPos playerPos) {
        switch (currentDirection) {
            case 0: // X+方向，在X-方向放置
                return pos.getX() < playerPos.getX();
            case 1: // Z+方向，在Z-方向放置
                return pos.getZ() < playerPos.getZ();
            case 2: // X-方向，在X+方向放置
                return pos.getX() > playerPos.getX();
            case 3: // Z-方向，在Z+方向放置
                return pos.getZ() > playerPos.getZ();
            default:
                return false;
        }
    }

    /**
     * 尝试放置方块
     */
    private void tryPlace(BlockPos pos) {
        if (pos == null) {
            return;
        }
        if (placeList.contains(pos)) {
            return;
        }
        if (tickBlockCount >= maxPlaceCount.get()) {
            return;
        }
        double distance = BaritoneUtil.getEyesPos().squaredDistanceTo(pos.toCenterPos());

        if (MathHelper.sqrt((float) distance) > placeRange.get()) {
            return;
        }

        int slot = BagUtil.findItemInventorySlotGrim(blockType.get().asItem());

        if (slot == -1) {
            return;
        }

        placeList.add(pos);
        BagUtil.doSwap(slot);

        // 放置方块
        BaritoneUtil.placeBlock(pos);

        BagUtil.doSwap(slot);
        BagUtil.sync();
        tickBlockCount++;
        totalPlaced++;
    }

    /**
     * 检查是否可以在指定位置放置
     */
    private boolean canPlaceAt(BlockPos pos) {
        // 检查位置是否为空气或可替换的方块
        if (!mc.world.getBlockState(pos).isAir() &&
            !mc.world.getBlockState(pos).getBlock().equals(Blocks.WATER) &&
            !mc.world.getBlockState(pos).getBlock().equals(Blocks.LAVA)) {
            return false;
        }

        // 检查是否可以看到放置位置
        return BaritoneUtil.canPlace(pos, true);
    }

    /**
     * 获取当前方向信息
     */
    public String getDirectionInfo() {
        if (!isActive()) {
            return "未启用";
        }

        String direction;
        switch (currentDirection) {
            case 0:
                direction = "X+轴 (向东堵塞)";
                break;
            case 1:
                direction = "Z+轴 (向南堵塞)";
                break;
            case 2:
                direction = "X-轴 (向西堵塞)";
                break;
            case 3:
                direction = "Z-轴 (向北堵塞)";
                break;
            default:
                direction = "未知";
        }

        return String.format("%s | 已放置: %d", direction, totalPlaced);
    }

    /**
     * 根据排序模式排序位置
     */
    private void sortPositions(List<BlockPos> positions, BlockPos playerPos) {
        if (positions.isEmpty()) return;

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

        // 根据排序模式决定是否反转
        if (sortMode.get() == SortMode.Farthest) {
            java.util.Collections.reverse(positions);
        }
    }

    /**
     * 重置统计
     */
    public void resetStats() {
        totalPlaced = 0;
        info("统计已重置");
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
