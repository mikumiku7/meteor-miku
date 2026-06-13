package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.BagUtil;
import com.github.mikumiku.addon.util.BaritoneUtil;
import com.github.mikumiku.addon.util.PositionCache;
import com.github.mikumiku.addon.util.WorldUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction.Axis;

import java.util.ArrayList;
import java.util.List;

public class AutoSlab extends BaseModule {
    // ========== 设置组 ==========
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("渲染");

    // ========== 基础设置 ==========
    private final Setting<BlockType> blockType = sgGeneral.add(
        new EnumSetting.Builder<BlockType>()
            .name("方块类型")
            .description("要放置的方块类型. 都是不刷怪的方块")
            .defaultValue(BlockType.SLAB)
            .build()
    );

    private final Setting<Integer> placementDelay = sgGeneral.add(
        new IntSetting.Builder()
            .name("延迟")
            .description("放置之间的延迟(刻)")
            .defaultValue(1)
            .build()
    );

    private final Setting<Integer> blockPerTick = sgGeneral.add(
        new IntSetting.Builder()
            .name("每刻放置数量")
            .description("每刻放置多少方块")
            .defaultValue(2)
            .sliderRange(1, 10)
            .build()
    );

    private final Setting<Double> detectionRange = sgGeneral.add(
        new DoubleSetting.Builder()
            .name("范围")
            .description("方块放置范围")
            .defaultValue(4.5)
            .range(1.0, 6.0)
            .build()
    );

    // ========== 渲染设置 ==========
    private final Setting<Boolean> renderEnabled = sgRender.add(
        new BoolSetting.Builder()
            .name("渲染")
            .description("渲染方块位置")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> renderMode = sgRender.add(
        new EnumSetting.Builder<ShapeMode>()
            .name("形状模式")
            .description("如何渲染方块")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(
        new ColorSetting.Builder()
            .name("侧面颜色")
            .description("渲染方块侧面的颜色")
            .defaultValue(new SettingColor(100, 200, 255, 45))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(
        new ColorSetting.Builder()
            .name("线条颜色")
            .description("渲染方块线条的颜色")
            .defaultValue(new SettingColor(120, 220, 255, 180))
            .build()
    );

    // ========== 运行时状态 ==========
    private final List<BlockPos> placePositions = new ArrayList<>();
    private int currentDelay;

    /**
     * 位置缓存管理器，防止短时间重复放置
     */
    private final PositionCache positionCache = new PositionCache(1000L);

    public AutoSlab() {
        super(CATEGORY_MIKU_BUILD, "铺半砖+", "自动放置半砖/活板门/铁轨/按钮/地毯, 只会铺一层， 用来防刷怪");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        placePositions.clear();
        currentDelay = 0;
        positionCache.startCleanupThread();
    }

    @Override
    public void onDeactivate() {
        placePositions.clear();
        currentDelay = 0;
        positionCache.shutdown(); // 关闭缓存管理器
    }

    /**
     * 主刻事件：扫描可放置的方块位置并执行放置
     */
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // 检查延迟计时器
        if (currentDelay < placementDelay.get()) {
            currentDelay++;
            return;
        }

        currentDelay = 0;
        scanPlacementPositions();

        if (placePositions.isEmpty()) {
            return;
        }

        // 获取物品栏中的方块
        int slot = getBlockSlot();
        if (slot == -1) {
            return;
        }

        // 计算本刻应放置的方块数量
        int placementCount = Math.min(blockPerTick.get(), placePositions.size());

        // 循环放置方块
        for (int i = 0; i < placementCount; i++) {
            BlockPos pos = placePositions.get(i);

            // 检查位置是否在缓存中（防止短时间重复放置）
            if (positionCache.isInCache(pos)) {
                continue;
            }

            // 交换到目标物品并尝试放置
            BagUtil.doSwap(slot);

            if (!isExistingPlacementBlock(mc.world.getBlockState(pos).getBlock())) {
                BaritoneUtil.placeBlock(pos, true, true, true);
                // 将位置添加到缓存，记录放置尝试
                positionCache.addToCache(pos);
            }

            BagUtil.doSwap(slot);
            BagUtil.sync();
        }
    }

    /**
     * 扫描周围可以放置方块的位置
     */
    private void scanPlacementPositions() {
        placePositions.clear();
        float range = detectionRange.get().floatValue();

        for (BlockPos pos : WorldUtils.getSphere(range)) {
            if (canPlaceAt(pos) && !positionCache.isInCache(pos)) {
                placePositions.add(pos);
            }
        }
    }

    /**
     * 判断指定位置是否可以放置方块
     */
    private boolean canPlaceAt(BlockPos pos) {
        BlockState downState = mc.world.getBlockState(pos.down());
        BlockState currentState = mc.world.getBlockState(pos);
        BlockState upState = mc.world.getBlockState(pos.up());

        Block downBlock = downState.getBlock();
        Block currentBlock = currentState.getBlock();

        // 条件1：在完整方块上放置（且不是潜行方块）
        boolean isOnFullCube = downState.isFullCube(mc.world, pos.down())
            && !BaritoneUtil.SNEAK_BLOCKS.contains(downBlock)
            && upState.isAir()
            && currentBlock != Blocks.WATER
            && currentBlock != Blocks.LAVA
            && !isExistingPlacementBlock(currentBlock)
            && BaritoneUtil.canPlace(pos, true)
            && BlockUtils.canPlace(pos);

        // 条件2：在0.8-1.0高度的半砖上放置
        double maxHeight = downState.getCollisionShape(mc.world, pos.down()).getMax(Axis.Y);
        boolean isOnHalfBlock = maxHeight > 0.8
            && maxHeight < 1.0
            && BlockUtils.canPlace(pos)
            && !BaritoneUtil.SNEAK_BLOCKS.contains(downBlock);

        return isOnFullCube || isOnHalfBlock;
    }

    /**
     * 获取物品栏中对应方块的槽位
     */
    private int getBlockSlot() {
        return BagUtil.findClassInventorySlotGrim(blockType.get().getBlockClass());
    }

    /**
     * 检查方块是否已是放置目标类型（过滤器）
     */
    private boolean isExistingPlacementBlock(Block block) {
        return block instanceof SlabBlock
            || block instanceof TrapdoorBlock
            || block instanceof AbstractRailBlock
            || block instanceof ButtonBlock;
    }

    /**
     * 渲染可放置的方块位置
     */
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!renderEnabled.get() || placePositions.isEmpty() || getBlockSlot() == -1) {
            return;
        }

        for (BlockPos pos : placePositions) {
            double x1 = pos.getX();
            double y1 = pos.getY();
            double z1 = pos.getZ();
            double x2 = x1 + 1;
            double y2 = y1 + 1;
            double z2 = z1 + 1;

            event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor.get(), lineColor.get(), renderMode.get(), 0);
        }
    }

    /**
     * 放置方块的类型枚举
     */
    public enum BlockType {
        SLAB(SlabBlock.class, "半砖"),
        TRAPDOOR(TrapdoorBlock.class, "活板门"),
        BUTTON(ButtonBlock.class, "按钮"),
        RAIL(AbstractRailBlock.class, "铁轨"),
        CARPET(CarpetBlock.class, "地毯"),
        Leave(LeavesBlock.class, "树叶"),
        FENCE(FenceBlock.class, "栅栏"),
        FENCE_GATE(FenceGateBlock.class, "栅栏门"),
        DOOR(DoorBlock.class, "门"),
        PRESSURE_PLATE(PressurePlateBlock.class, "压力板"),
        TORCH(TorchBlock.class, "火把"),
        LANTERN(LanternBlock.class, "灯笼"),
        GLASS(StainedGlassBlock.class, "染色玻璃"),
        REDSTONE_WIRE(RedstoneWireBlock.class, "红石线"),
        REDSTONE_TORCH(RedstoneTorchBlock.class, "红石火把"),
        REPEATER(RepeaterBlock.class, "红石中继器"),
        COMPARATOR(ComparatorBlock.class, "红石比较器"),
        SNOW_LAYER(SnowBlock.class, "雪层"),
        FLOWER(FlowerBlock.class, "花"),
        SAPLING(SaplingBlock.class, "树苗"),
        COBWEB(CobwebBlock.class, "蜘蛛网"),
        NOTE_BLOCK(NoteBlock.class, "音符盒"),
        DAYLIGHT_SENSOR(DaylightDetectorBlock.class, "光感器"),
        SOUL_SAND(SoulSandBlock.class, "灵魂沙"),
        CAMPFIRE(CampfireBlock.class, "营火"),
        FLOWER_POT(FlowerPotBlock.class, "花盆"),
        CHAIN(ChainBlock.class, "锁链"),
        VINE(VineBlock.class, "藤蔓"),
        BAMBOO(BambooBlock.class, "竹子"),
        ICE(IceBlock.class, "冰"),
        STAIRS(StairsBlock.class, "楼梯"),
        ;

        private final Class<?> blockClass;
        private final String displayName;

        BlockType(Class<?> blockClass, String displayName) {
            this.blockClass = blockClass;
            this.displayName = displayName;
        }

        public Class<?> getBlockClass() {
            return blockClass;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
