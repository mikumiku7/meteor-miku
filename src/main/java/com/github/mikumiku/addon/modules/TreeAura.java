package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.MikuMikuAddon;
import com.github.mikumiku.addon.util.WorldUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SaplingBlock;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

public class TreeAura extends Module {

    // 设置组，用于组织模块的设置选项
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // 是否启用视角旋转设置，控制是否在放置方块时转向目标位置
    private final Setting<Boolean> rotation = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("rotate for block interactions").defaultValue(false).build());
    // 种植树木的延迟设置（tick），控制种植操作之间的间隔时间
    private final Setting<Integer> plantDelay = sgGeneral.add(new IntSetting.Builder().name("plant-delay").description("delay between planting trees").defaultValue(6).min(0).sliderMax(25).build());
    // 施加骨粉的延迟设置（tick），控制对树苗使用骨粉的间隔时间
    private final Setting<Integer> bonemealDelay = sgGeneral.add(new IntSetting.Builder().name("bonemeal-delay").description("delay between placing bonemeal on trees").defaultValue(3).min(0).sliderMax(25).build());
    // 水平种植半径设置，控制水平方向上的种植范围
    private final Setting<Integer> rRange = sgGeneral.add(new IntSetting.Builder().name("radius").description("how far you can place horizontally").defaultValue(4).min(1).sliderMax(5).build());
    // 垂直种植范围设置，控制垂直方向上的种植范围
    private final Setting<Integer> yRange = sgGeneral.add(new IntSetting.Builder().name("y-range").description("how far you can place vertically").defaultValue(3).min(1).sliderMax(5).build());
    // 排序模式设置（最近或最远），控制选择种植/施肥位置的优先级
    private final Setting<SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<SortMode>().name("sort-mode").description("how to sort nearby trees/placements.").defaultValue(SortMode.Farthest).build());

    // 骨粉使用计时器和种植计时器，用于控制操作频率
    private int bonemealTimer, plantTimer;


    public TreeAura() { // CopeTypes
        super(MikuMikuAddon.CATEGORY, "自动种树", "在你周围种树！");
    }

    @Override
    public void onActivate() {
        // 激活模块时重置计时器
        bonemealTimer = 0;
        plantTimer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        // 每个游戏刻减少计时器
        plantTimer--;
        bonemealTimer--;

        // 当种植计时器归零时寻找种植位置并种植树苗
        if (plantTimer <= 0) {
            BlockPos plantPos = findPlantLocation();
            if (plantPos == null) return;
            doPlant(plantPos);
            plantTimer = plantDelay.get();
        }

        // 当骨粉计时器归零时寻找已种植的树苗并施加骨粉
        if (bonemealTimer <= 0) {
            BlockPos p = findPlantedSapling();
            if (p == null) return;
            doBonemeal(p);
            bonemealTimer = bonemealDelay.get();
        }
    }


    // 查找背包中的骨粉
    private FindItemResult findBonemeal() {
        return InvUtils.findInHotbar(Items.BONE_MEAL);
    }

    // 查找背包中的树苗
    private FindItemResult findSapling() {
        return InvUtils.findInHotbar(itemStack -> Block.getBlockFromItem(itemStack.getItem()) instanceof SaplingBlock);
    }

    // 判断指定位置是否为树苗
    private boolean isSapling(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock() instanceof SaplingBlock;
    }

    // 在指定位置种植树苗
    private void doPlant(BlockPos plantPos) {
        FindItemResult sapling = findSapling();
        if (!sapling.found()) {
            error("快捷栏中没有树苗");
            toggle();
            return;
        }
        InvUtils.swap(sapling.slot(), false);
        // 根据设置决定是否旋转视角
        if (rotation.get())
            Rotations.rotate(Rotations.getYaw(plantPos), Rotations.getPitch(plantPos), () -> mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(plantPos), Direction.UP, plantPos, false), 0)));
        else
            mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(plantPos), Direction.UP, plantPos, false), 0));
    }

    // 对指定位置的树苗施加骨粉
    private void doBonemeal(BlockPos sapling) {
        FindItemResult bonemeal = findBonemeal();
        if (!bonemeal.found()) {
            error("快捷栏中没有骨粉");
            toggle();
            return;
        }
        InvUtils.swap(bonemeal.slot(), false);
        // 根据设置决定是否旋转视角
        if (rotation.get())
            Rotations.rotate(Rotations.getYaw(sapling), Rotations.getPitch(sapling), () -> mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(sapling), Direction.UP, sapling, false), 0)));
        else
            mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(sapling), Direction.UP, sapling, false), 0));
    }

    // 判断指定位置是否可以种植树苗
    private boolean canPlant(BlockPos pos) {
        Block b = mc.world.getBlockState(pos).getBlock();
        // 只能在草方块、泥土等允许种植的地方种植
        if (b.equals(Blocks.SHORT_GRASS) || b.equals(Blocks.GRASS_BLOCK) || b.equals(Blocks.DIRT) || b.equals(Blocks.COARSE_DIRT)) {
            final AtomicBoolean plant = new AtomicBoolean(true);
            // 检查上方5格内是否有障碍物，确保树木有足够的生长空间
            IntStream.rangeClosed(1, 5).forEach(i -> {
                // 检查上方
                BlockPos check = pos.up(i);
                if (!mc.world.getBlockState(check).getBlock().equals(Blocks.AIR)) {
                    plant.set(false);
                    return;
                }
                // 检查四周
                for (CardinalDirection dir : CardinalDirection.values()) {
                    if (!mc.world.getBlockState(check.offset(dir.toDirection(), i)).getBlock().equals(Blocks.AIR)) {
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

    // 查找需要施加骨粉的已种植树苗
    private BlockPos findPlantedSapling() {
        List<BlockPos> saplings = findSaplings(mc.player.getBlockPos(), rRange.get(), yRange.get());
        if (saplings.isEmpty()) return null;
        // 根据距离排序
        saplings.sort(Comparator.comparingDouble(PlayerUtils::distanceTo));
        // 根据排序模式决定使用最近还是最远的树苗
        if (sortMode.get().equals(SortMode.Farthest)) Collections.reverse(saplings);
        return saplings.get(0);
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
        List<BlockPos> nearby = getPlantLocations(mc.player.getBlockPos(), rRange.get(), yRange.get());
        if (nearby.isEmpty()) return null;
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

    // 排序模式枚举：最近或最远
    public enum SortMode {
        // 最近优先模式
        Closest,
        // 最远优先模式
        Farthest
    }

}
