package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.BagUtil;
import com.github.mikumiku.addon.util.BaritoneUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class LiquidFiller extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWhitelist = settings.createGroup("白名单");

    private final HashMap<BlockPos, Long> failedCache = new HashMap<>();
    private final long cacheDuration = 2000; // 2秒（毫秒）


    private final Setting<PlaceIn> placeInLiquids = sgGeneral.add(new EnumSetting.Builder<PlaceIn>()
        .name("放置位置")
        .description("选择在哪种类型的液体中放置方块。")
        .defaultValue(PlaceIn.Both)
        .build()
    );

    private final Setting<Shape> shape = sgGeneral.add(new EnumSetting.Builder<Shape>()
        .name("形状")
        .description("放置算法的形状。")
        .defaultValue(Shape.Sphere)
        .build()
    );

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("放置范围")
        .description("可以放置方块的范围。")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Double> placeWallsRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("穿墙范围")
        .description("在方块后面放置时的范围。")
        .defaultValue(3)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("延迟")
        .description("动作之间的延迟（以游戏刻为单位）。")
        .defaultValue(0)
        .min(0)
        .build()
    );

    private final Setting<Integer> maxBlocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("每刻最大方块数")
        .description("每游戏刻尝试放置的最大方块数。")
        .defaultValue(1)
        .min(1)
        .sliderRange(1, 10)
        .build()
    );

    private final Setting<SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<SortMode>()
        .name("排序模式")
        .description("优先放置方块的顺序。")
        .defaultValue(SortMode.BottomUp)
        .build()
    );

    // 白名单和黑名单
    private final Setting<ListMode> listMode = sgWhitelist.add(new EnumSetting.Builder<ListMode>()
        .name("列表模式")
        .description("选择模式。")
        .defaultValue(ListMode.Whitelist)
        .build()
    );

    private final Setting<List<Block>> whitelist = sgWhitelist.add(new BlockListSetting.Builder()
        .name("白名单")
        .description("允许用来填充液体的方块。")
        .defaultValue(
            Blocks.DIRT,
            Blocks.COBBLESTONE,
            Blocks.STONE,
            Blocks.NETHERRACK,
            Blocks.DIORITE,
            Blocks.GRANITE,
            Blocks.SLIME_BLOCK,
            Blocks.ANDESITE
        )
        .visible(() -> listMode.get() == ListMode.Whitelist)
        .build()
    );

    private final Setting<List<Block>> blacklist = sgWhitelist.add(new BlockListSetting.Builder()
        .name("黑名单")
        .description("禁止用来填充液体的方块。")
        .visible(() -> listMode.get() == ListMode.Blacklist)
        .build()
    );

    private final List<BlockPos.Mutable> blocks = new ArrayList<>();

    private int timer;

    public LiquidFiller() {
        super("自动填水填海", "自动将方块放置在您范围内的水源方块内。");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        timer = 0;
        failedCache.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // 根据延迟更新计时器
        if (timer < delay.get()) {
            timer++;
            return;
        } else {
            timer = 0;
        }

        // 计算一些数据
        double pX = mc.player.getX();
        double pY = mc.player.getY();
        double pZ = mc.player.getZ();

        // 查找包含方块的槽位
        int itemSlot = -1;
        if (listMode.get() == ListMode.Whitelist) {
            itemSlot = BagUtil.findItemInventorySlot(itemStack -> itemStack.getItem() instanceof BlockItem && whitelist.get().contains(Block.getBlockFromItem(itemStack.getItem())));
        } else {
            itemSlot = BagUtil.findItemInventorySlot(itemStack -> itemStack.getItem() instanceof BlockItem && !blacklist.get().contains(Block.getBlockFromItem(itemStack.getItem())));
        }
        if (itemSlot == -1) return;

        long now = System.currentTimeMillis();

        // 遍历玩家周围的方块
        BlockIterator.register((int) Math.ceil(placeRange.get() + 1), (int) Math.ceil(placeRange.get()), (blockPos, blockState) -> {

            // 跳过失败缓存
            if (failedCache.containsKey(blockPos)) {
                if (now - failedCache.get(blockPos) < cacheDuration) return;
                else failedCache.remove(blockPos);
            }

            // 检查射线投射和范围
            if (isOutOfRange(blockPos)) return;

            // 检查方块是否为液体源方块并设置为待填充
            Fluid fluid = blockState.getFluidState().getFluid();
            if ((placeInLiquids.get() == PlaceIn.Both && (fluid != Fluids.WATER && fluid != Fluids.LAVA))
                || (placeInLiquids.get() == PlaceIn.Water && fluid != Fluids.WATER)
                || (placeInLiquids.get() == PlaceIn.Lava && fluid != Fluids.LAVA))
                return;

            // 检查玩家是否可以在该位置放置方块
            if (!BaritoneUtil.canPlace(blockPos)) return;

            // 添加方块
            blocks.add(blockPos.mutableCopy());
        });

        int finalItemSlot = itemSlot;
        BlockIterator.after(() -> {
            // 排序方块
            if (sortMode.get() == SortMode.TopDown || sortMode.get() == SortMode.BottomUp)
                blocks.sort(Comparator.comparingDouble(value -> value.getY() * (sortMode.get() == SortMode.BottomUp ? 1 : -1)));
            else if (sortMode.get() != SortMode.None)
                blocks.sort(Comparator.comparingDouble(value -> Utils.squaredDistance(pX, pY, pZ, value.getX() + 0.5, value.getY() + 0.5, value.getZ() + 0.5) * (sortMode.get() == SortMode.Closest ? 1 : -1)));

            // 放置方块并清除放置位置
            int count = 0;
            for (BlockPos pos : blocks) {
                if (count >= maxBlocksPerTick.get()) {
                    break;
                }

                if (!canPlaceNormally(pos)) {
                    // 记录为无法放置
                    failedCache.put(pos, System.currentTimeMillis());
                    continue; // 跳过该位置
                }


                BagUtil.doSwap(finalItemSlot);
                BaritoneUtil.placeBlock(pos);
                BagUtil.doSwap(finalItemSlot);

                failedCache.put(pos, System.currentTimeMillis());

                count++;
            }
            blocks.clear();
        });
    }

    public enum ListMode {
        Whitelist("白名单"),
        Blacklist("黑名单");

        private final String displayName;

        ListMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum PlaceIn {
        Both("水和岩浆"),
        Water("仅水"),
        Lava("仅岩浆");

        private final String displayName;

        PlaceIn(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum SortMode {
        None("无排序"),
        Closest("最近优先"),
        Furthest("最远优先"),
        TopDown("从上到下"),
        BottomUp("从下到上");

        private final String displayName;

        SortMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum Shape {
        Sphere("球形"),
        UniformCube("立方体");

        private final String displayName;

        Shape(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private boolean isOutOfRange(BlockPos blockPos) {
        if (!isWithinShape(blockPos, placeRange.get())) return true;

        RaycastContext raycastContext = new RaycastContext(mc.player.getEyePos(), blockPos.toCenterPos(), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
        BlockHitResult result = mc.world.raycast(raycastContext);
        if (result == null || !result.getBlockPos().equals(blockPos))
            return !isWithinShape(blockPos, placeWallsRange.get());

        return false;
    }

    private boolean isWithinShape(BlockPos blockPos, double range) {
        // 立方体形状
        if (shape.get() == Shape.UniformCube) {
            BlockPos playerBlockPos = mc.player.getBlockPos();
            double dX = Math.abs(blockPos.getX() - playerBlockPos.getX());
            double dY = Math.abs(blockPos.getY() - playerBlockPos.getY());
            double dZ = Math.abs(blockPos.getZ() - playerBlockPos.getZ());
            double maxDist = Math.max(Math.max(dX, dY), dZ);
            return maxDist <= Math.floor(range);
        }

        // 球形形状
        return PlayerUtils.isWithin(blockPos.toCenterPos(), range);
    }

    private boolean canPlaceNormally(BlockPos pos) {
        // 遍历六个方向
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            if (!mc.world.getBlockState(neighbor).isAir() && !mc.world.getBlockState(neighbor).isLiquid()) {
                return true; // 有可点击的支持面
            }
        }
        return false; // 全部是空气或液体，不能放置
    }


}
