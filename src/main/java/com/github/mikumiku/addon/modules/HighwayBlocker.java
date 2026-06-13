package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.*;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Modules;
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
import java.util.Collections;
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

    // 自动停止设置
    private final Setting<Boolean> autoStopOnNoBlock = sgGeneral.add(new BoolSetting.Builder()
        .name("无方块停止")
        .description("当背包中没有选定方块时自动停止模块")
        .defaultValue(true)
        .build());

    // 自动补给设置
    private final Setting<Boolean> autoRefill = sgGeneral.add(new BoolSetting.Builder()
        .name("自动补给")
        .description("当方块消耗完毕时，自动使用一键补给模块进行补给")
        .defaultValue(false)
        .build());

    // 路宽度设置
    private final Setting<Boolean> accurate = sgGeneral.add(new BoolSetting.Builder()
        .name("精准堵路")
        .description("精准堵地狱高速路")
        .defaultValue(false)
        .build());

    // 路宽度设置
    private final Setting<Integer> roadWidth = sgGeneral.add(new IntSetting.Builder()
        .name("路宽度")
        .description("路的宽度，只处理这个范围内的方块")
        .defaultValue(4)
        .min(1)
        .sliderMax(9)
        .visible(accurate::get)
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

    // 自动补给相关状态
    private boolean isRefilling = false;
    private boolean wasAutoWalkActive = false;

    /**
     * 位置缓存管理器，防止短时间重复放置
     */
    private final PositionCache positionCache = new PositionCache(1000L); // 1秒过期

    public HighwayBlocker() {
        super(CATEGORY_MIKU_BUILD, "高速堵路者", "自动在下界放置方块堵塞高速路, 建议配合[一键补给]、[自动走路] 一起使用。");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        totalPlaced = 0;
        tickCounter = 0;
        tickBlockCount = 0;
        preDirection = -1;
        placeList.clear();
        positionCache.startCleanupThread();
        ChatUtils.sendMsg("高速堵塞模块已启用！");
    }

    @Override
    public void onDeactivate() {
        ChatUtils.sendMsg("高速堵塞模块已停用！总共放置了 " + totalPlaced + " 个 " + blockType.get().getName().getString());
        preDirection = -1;
        positionCache.shutdown(); // 关闭缓存管理器
        resetStats();

        // 重置补给状态
        isRefilling = false;
        wasAutoWalkActive = false;
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
            // 如果开启了自动补给，尝试补给
            if (autoRefill.get() && !isRefilling) {
                startAutoRefill();
                return;
            }

            // 如果正在补给中，等待补给完成
            if (isRefilling) {
                checkRefillComplete();
                return;
            }

            // 没有开启自动补给或补给失败，根据设置决定是否停止
            if (autoStopOnNoBlock.get()) {
                ChatUtils.sendMsg("没有找到 " + blockType.get().getName().getString() + "，停止模块");
                toggle();
            }
            return;
        }

        // 更新当前方向
        updateCurrentDirection();
        if (currentDirection != preDirection) {
            preDirection = currentDirection;
            ChatUtils.sendMsg(getDirectionInfo());
        }

        // 计算放置位置
        BlockPos playerPos = mc.player.getBlockPos();
        List<BlockPos> validPositions = new ArrayList<>();

        // 根据是否开启精准堵路来选择不同的位置计算方式
        if (accurate.get()) {
            // 精准堵路模式：只在 Y=121,122,123 且符合路宽度的区域放置
            validPositions = getAccuratePlacePositions(playerPos);
        } else {
            // 普通模式：使用球形范围
            List<BlockPos> spherePositions = WorldUtils.getSphere(placeRange.get());
            for (BlockPos pos : spherePositions) {
                if (isPlacePositionValid(pos, playerPos)) {
                    validPositions.add(pos);
                }
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
     * 获取精准堵路的放置位置
     * 只在 Y=121,122,123 且符合路宽度的区域放置
     */
    private List<BlockPos> getAccuratePlacePositions(BlockPos playerPos) {
        List<BlockPos> positions = new ArrayList<>();
        int width = roadWidth.get();

        // 计算路的中心坐标（根据玩家坐标对齐到路的中心）
        int centerX, centerZ;

        switch (currentDirection) {
            case 0: // X+方向（向东），在X-方向放置，路沿Z轴延伸
            case 2: // X-方向（向西），在X+方向放置，路沿Z轴延伸
                // Z轴为中心，根据玩家Z坐标对齐
                centerZ = alignToRoadCenter(playerPos.getZ(), width);
                // X轴根据方向确定放置位置
                int placeX = (currentDirection == 0) ? playerPos.getX() - 1 : playerPos.getX() + 1;

                // 在Y=120,121,122三层，Z轴宽度范围内放置
                for (int y = 120; y <= 122; y++) {
                    // 按照指定的顺序生成偏移量：0, -1, 1, -2, 2, -3, 3, ...
                    for (int i = 0; i < width; i++) {
                        int zOffset;
                        if (i == 0) {
                            zOffset = 0;
                        } else if (i % 2 == 1) {
                            zOffset = -(i + 1) / 2;  // 奇数：-1, -2, -3, ...
                        } else {
                            zOffset = i / 2;  // 偶数：1, 2, 3, ...
                        }
                        BlockPos pos = new BlockPos(placeX, y, centerZ + zOffset);
                        if (canPlaceAt(pos) && isPlacePositionValid(pos, playerPos)) {
                            positions.add(pos);
                        }
                    }
                }
                break;

            case 1: // Z+方向（向南），在Z-方向放置，路沿X轴延伸
            case 3: // Z-方向（向北），在Z+方向放置，路沿X轴延伸
                // X轴为中心，根据玩家X坐标对齐
                centerX = alignToRoadCenter(playerPos.getX(), width);
                // Z轴根据方向确定放置位置
                int placeZ = (currentDirection == 1) ? playerPos.getZ() - 1 : playerPos.getZ() + 1;

                // 在Y=120,121,122三层，X轴宽度范围内放置
                for (int y = 120; y <= 122; y++) {
                    // 按照指定的顺序生成偏移量：0, -1, 1, -2, 2, -3, 3, ...
                    for (int i = 0; i < width; i++) {
                        int xOffset;
                        if (i == 0) {
                            xOffset = 0;
                        } else if (i % 2 == 1) {
                            xOffset = -(i + 1) / 2;  // 奇数：-1, -2, -3, ...
                        } else {
                            xOffset = i / 2;  // 偶数：1, 2, 3, ...
                        }
                        BlockPos pos = new BlockPos(centerX + xOffset, y, placeZ);
                        if (canPlaceAt(pos) && isPlacePositionValid(pos, playerPos)) {
                            positions.add(pos);
                        }
                    }
                }
                break;
        }

        return positions;
    }

    /**
     * 将坐标对齐到路的中心
     * 确保路的中心是对称的
     */
    private int alignToRoadCenter(int coord, int width) {
        // 找到最近的中心点，使得路的范围对称
        // 例如：width=4 时，中心点为 ..., -6, -2, 2, 6, 10, ...
        // 路的范围为 [center-width/2, center+width/2]
        int halfWidth = width / 2;
        return Math.round((float) coord / width) * width + halfWidth;
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

        // 检查位置是否在缓存中（防止短时间重复放置）
        if (positionCache.isInCache(pos)) {
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

        // 将位置添加到缓存，记录放置尝试
        positionCache.addToCache(pos);
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
            Collections.reverse(positions);
        }
    }

    /**
     * 重置统计
     */
    public void resetStats() {
        totalPlaced = 0;
        ChatUtils.sendMsg("统计已重置");
    }

    /**
     * 开始自动补给
     */
    private void startAutoRefill() {
        ShulkerBoxItemFetcher fetcher = Modules.get().get(ShulkerBoxItemFetcher.class);
        AutoWalk autoWalk = Modules.get().get(AutoWalk.class);

        if (fetcher == null) {
            ChatUtils.sendMsg("错误：找不到一键补给模块");
            return;
        }

        // 检查 AutoWalk 是否开启，如果开启则暂停
        if (autoWalk != null && autoWalk.isActive()) {
            wasAutoWalkActive = true;
            autoWalk.toggle();
            ChatUtils.sendMsg("暂停自动走路以进行补给");
        } else {
            wasAutoWalkActive = false;
        }

        // 配置补给模块
        fetcher.targetItem.set(blockType.get().asItem());
        fetcher.autoClose.set(true);

        // 启动补给模块
        isRefilling = true;
        if (!fetcher.isActive()) {
            fetcher.toggle();
            ChatUtils.sendMsg("正在使用一键补给获取 " + blockType.get().getName().getString());
        }
    }

    /**
     * 检查补给是否完成
     */
    private void checkRefillComplete() {
        ShulkerBoxItemFetcher fetcher = Modules.get().get(ShulkerBoxItemFetcher.class);
        AutoWalk autoWalk = Modules.get().get(AutoWalk.class);

        if (fetcher == null) {
            isRefilling = false;
            return;
        }

        // 检查补给模块是否已经关闭（autoClose=true 时会自动关闭）
        if (!fetcher.isActive()) {
            isRefilling = false;
            ChatUtils.sendMsg("补给完成，继续堵路");

            // 如果之前 AutoWalk 是开启的，现在重新开启
            if (wasAutoWalkActive && autoWalk != null && !autoWalk.isActive()) {
                autoWalk.toggle();
                ChatUtils.sendMsg("恢复自动走路");
            }

            // 检查是否有方块了
            if (!BagUtil.find(blockType.get().asItem()).found()) {
                ChatUtils.sendMsg("补给后仍未找到 " + blockType.get().getName().getString());
                if (autoStopOnNoBlock.get()) {
                    toggle();
                }
            }
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
