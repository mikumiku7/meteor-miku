package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.dynamic.DV;
import com.github.mikumiku.addon.util.ItemUtil;
import com.github.mikumiku.addon.util.PlayerUtil;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GhostMine extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public Setting<Integer> range = sgGeneral
        .add(
            new IntSetting.Builder()
                .name("挖掘范围")
                .description("设置挖掘方块的最大距离")
                .sliderRange(1, 6)
                .defaultValue(6)
                .build()
        );
    public Setting<Double> speed = sgGeneral
        .add(
            new DoubleSetting.Builder()
                .name("挖掘速度")
                .description("控制挖掘方块的速度倍率")
                .sliderRange(0.65, 1.0)
                .defaultValue(0.85)
                .build()
        );
    public Setting<Boolean> doubleBreak = sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("双挖")
                .description("同时挖掘两个方块以提高效率")
                .defaultValue(true)
                .build()
        );
    public Setting<Boolean> rebreak = sgGeneral
        .add(new BoolSetting.Builder().name("自动重挖").description("自动重新挖掘已破坏的方块").defaultValue(true).build());
    public Setting<Integer> rebreakDelay = sgGeneral
        .add(
            new IntSetting.Builder()
                .name("重挖延迟")
                .description("重新挖掘方块前的延迟时间(tick)")
                .sliderRange(0, 10)
                .defaultValue(0)
                .visible(rebreak::get)
                .build()
        );
    public Setting<SwapMode> swapModeSetting = sgGeneral
        .add(new EnumSetting.Builder<SwapMode>()
            .name("切换模式")
            .description("选择工具切换的方式")
            .defaultValue(SwapMode.NORMAL)
            .visible(() -> false)
            .build()
        );
    private final SettingGroup sgRender = settings.createGroup("渲染设置");
    private final Setting<Boolean> render = sgRender
        .add(
            new BoolSetting.Builder()
                .name("显示渲染")
                .description("是否显示正在挖掘方块的可视化效果")
                .defaultValue(true)
                .build()
        );

    private final Setting<ShapeMode> shapeMode = sgRender
        .add(new EnumSetting.Builder<ShapeMode>()
            .name("形状")
            .description("选择渲染形状的显示方式")
            .defaultValue(ShapeMode.Both)
            .build()
        );
    private final Setting<SettingColor> readySideColor = sgRender
        .add(
            new ColorSetting.Builder()
                .name("完成侧面颜色")
                .description("方块挖掘完成时侧面的颜色")
                .defaultValue(new SettingColor(255, 192, 203, 80))
                .build()
        );
    private final Setting<SettingColor> readyLineColor = sgRender
        .add(
            new ColorSetting.Builder()
                .name("完成边框颜色")
                .description("方块挖掘完成时边框的颜色")
                .defaultValue(new SettingColor(255, 192, 203, 255))
                .build()
        );
    private final Setting<SettingColor> sideColor = sgRender
        .add(
            new ColorSetting.Builder()
                .name("侧面颜色")
                .description("正在挖掘方块侧面的颜色")
                .defaultValue(new SettingColor(255, 192, 203, 80))
                .build()
        );
    private final Setting<SettingColor> lineColor = sgRender
        .add(
            new ColorSetting.Builder()
                .name("边框颜色")
                .description("正在挖掘方块边框的颜色")
                .defaultValue(new SettingColor(255, 192, 203, 255))
                .build()
        );

    private final List<BlockDate> breakBlocks = new ArrayList<>();
    public static BlockDate firstBlockDate = null;
    public static BlockDate secondBlockDate = null;
    private BlockDate rebreakBlockDate = null;
    public static BlockDate tempBlockDate = null;
    private int rebreakTicks = 0;
    public static final List<Block> unbreakableBlocks = Arrays.asList(
        Blocks.COMMAND_BLOCK,
        Blocks.LAVA_CAULDRON,
        Blocks.LAVA,
        Blocks.WATER_CAULDRON,
        Blocks.WATER,
        Blocks.BEDROCK,
        Blocks.BARRIER,
        Blocks.END_PORTAL,
        Blocks.NETHER_PORTAL,
        Blocks.END_PORTAL_FRAME
    );

    public GhostMine() {
        super("发包挖掘", "使用发包快速挖掘方块");
    }

    @Override
    public void onActivate() {
        super.onActivate();

        firstBlockDate = null;
        secondBlockDate = null;
        rebreakBlockDate = null;
        tempBlockDate = null;
        rebreakTicks = 0;
    }

    @Override
    public void onDeactivate() {
        firstBlockDate = null;
        secondBlockDate = null;
        rebreakBlockDate = null;
        tempBlockDate = null;
        rebreakTicks = 0;
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc == null) {
            mc = MinecraftClient.getInstance();
        }

        rangeCheck();
        rebreakTicks++;
        if (!doubleBreak.get()) {
            if (firstBlockDate != null && mc.world.getBlockState(firstBlockDate.pos).getBlock() == Blocks.AIR) {
                firstBlockDate = null;
            }

            if (firstBlockDate != null && !firstBlockDate.isMining) {
                mineBlock(firstBlockDate.pos, firstBlockDate.direction);
                firstBlockDate.isMining = true;
            }

            if (firstBlockDate != null
                && firstBlockDate.isMining
                && mc.world.getBlockState(firstBlockDate.pos).getBlock() == Blocks.AIR
                && rebreak.get()) {
                firstBlockDate.isBreaked = true;
            }

            if (firstBlockDate != null && firstBlockDate.isMining && !firstBlockDate.done) {
                firstBlockDate.freshProgress();
            }

            if (firstBlockDate != null && !firstBlockDate.isBreaked && firstBlockDate.isMining && firstBlockDate.done) {
                if (swapModeSetting.get() == SwapMode.SILENT) {
                    int slot = getBestTool(mc.world.getBlockState(firstBlockDate.pos));
                    if (slot != -1) {
                        InvUtils.swap(slot, true);
                    }
                }

                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, firstBlockDate.pos, firstBlockDate.direction));
                if (swapModeSetting.get() == SwapMode.SILENT) {
                    InvUtils.swapBack();
                }

                if (firstBlockDate.rebreak) {
                    rebreakBlockDate = firstBlockDate;
                } else {
                    rebreakBlockDate = null;
                }

                firstBlockDate = null;
            }

            if (rebreakBlockDate != null
                && firstBlockDate == null
                && mc.world.getBlockState(rebreakBlockDate.pos).getBlock() != Blocks.AIR
                && mc.world.getBlockState(rebreakBlockDate.pos).getBlock() != Blocks.WATER
                && mc.world.getBlockState(rebreakBlockDate.pos).getBlock() != Blocks.LAVA
                && rebreak.get()
                && rebreakTicks >= (rebreakDelay.get() * 4)) {
                int slot = getBestTool(mc.world.getBlockState(rebreakBlockDate.pos));
                if (slot != -1 && slot != DV.of(PlayerUtil.class).getSelectedSlot(mc.player.getInventory()) && swapModeSetting.get() == SwapMode.SILENT) {
                    InvUtils.swap(slot, true);
                }

                if (slot != -1 && slot != DV.of(PlayerUtil.class).getSelectedSlot(mc.player.getInventory()) && swapModeSetting.get() == SwapMode.NORMAL) {
                    InvUtils.swap(slot, false);
                }

                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, rebreakBlockDate.pos, rebreakBlockDate.direction));
                rebreakTicks = 0;
                if (slot != -1 && swapModeSetting.get() == SwapMode.SILENT) {
                    InvUtils.swapBack();
                }
            }
        } else {
            if (firstBlockDate != null && mc.world.getBlockState(firstBlockDate.pos).getBlock() == Blocks.AIR) {
                firstBlockDate = null;
            }

            if (secondBlockDate != null && mc.world.getBlockState(secondBlockDate.pos).getBlock() == Blocks.AIR) {
                secondBlockDate = null;
            }

            if (firstBlockDate != null && !firstBlockDate.isMining && secondBlockDate == null) {
                mineBlock(firstBlockDate.pos, firstBlockDate.direction);
                firstBlockDate.isMining = true;
            }

            if (secondBlockDate != null && !secondBlockDate.isMining) {
                if (firstBlockDate != null) {
                    mineBlock(firstBlockDate.pos, firstBlockDate.direction);
                }

                mineBlock(secondBlockDate.pos, secondBlockDate.direction);
                secondBlockDate.isMining = true;
            }

            if (firstBlockDate != null && firstBlockDate.isMining && !firstBlockDate.done) {
                firstBlockDate.freshProgress();
            }

            if (secondBlockDate != null && secondBlockDate.isMining && !secondBlockDate.done) {
                secondBlockDate.freshProgress();
            }

            if (tempBlockDate != null && tempBlockDate.isMining && !tempBlockDate.done) {
                tempBlockDate.freshProgress();
            }

            if (tempBlockDate != null && tempBlockDate.done) {
                tempBlockDate = null;
            }

            if (firstBlockDate != null && !firstBlockDate.isBreaked && firstBlockDate.isMining && firstBlockDate.done && secondBlockDate == null) {
                if (swapModeSetting.get() == SwapMode.SILENT) {
                    int slotx = getBestTool(mc.world.getBlockState(firstBlockDate.pos));
                    if (slotx != -1) {
                        InvUtils.swap(slotx, true);
                    }
                }

                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, firstBlockDate.pos, firstBlockDate.direction));
                if (firstBlockDate.rebreak) {
                    rebreakBlockDate = new BlockDate(firstBlockDate.pos, firstBlockDate.direction);
                } else {
                    rebreakBlockDate = null;
                }

                firstBlockDate = null;
                if (swapModeSetting.get() == SwapMode.SILENT) {
                    InvUtils.swapBack();
                }
            }

            if (firstBlockDate != null && !firstBlockDate.isBreaked && firstBlockDate.isMining && firstBlockDate.done && secondBlockDate != null) {
                if (swapModeSetting.get() == SwapMode.SILENT) {
                    int slotx = getBestTool(mc.world.getBlockState(secondBlockDate.pos));
                    if (slotx != -1) {
                        InvUtils.swap(slotx, true);
                    }
                }

                if (swapModeSetting.get() == SwapMode.NORMAL) {
                    int firstSlot = getBestTool(mc.world.getBlockState(firstBlockDate.pos));
                    int secondSlot = getBestTool(mc.world.getBlockState(secondBlockDate.pos));
                    if (firstSlot == -1 && secondSlot != -1) {
                        InvUtils.swap(secondSlot, false);
                    }

                    if (firstSlot != -1 && secondSlot == -1) {
                        InvUtils.swap(firstSlot, false);
                    }

                    if (firstSlot != -1 && secondSlot != -1 && firstSlot != secondSlot) {
                        if (DV.of(ItemUtil.class).isPickaxeItem(mc.player.getInventory().getStack(firstSlot).getItem())) {
                            InvUtils.swap(firstSlot, false);
                        } else if (DV.of(ItemUtil.class).isPickaxeItem(mc.player.getInventory().getStack(secondSlot).getItem())) {
                            InvUtils.swap(secondSlot, false);
                        }
                    }
                }

                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, firstBlockDate.pos, firstBlockDate.direction));
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, secondBlockDate.pos, secondBlockDate.direction));
                firstBlockDate = null;
                if (swapModeSetting.get() == SwapMode.SILENT) {
                    InvUtils.swapBack();
                }
            }

            if (secondBlockDate != null && secondBlockDate.done) {
                if (secondBlockDate.rebreak) {
                    rebreakBlockDate = new BlockDate(secondBlockDate.pos, secondBlockDate.direction);
                } else {
                    rebreakBlockDate = null;
                }

                if (firstBlockDate != null && firstBlockDate.done) {
                    mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, firstBlockDate.pos, firstBlockDate.direction));
                }

                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, secondBlockDate.pos, secondBlockDate.direction));
                secondBlockDate = null;
                if (firstBlockDate != null && !firstBlockDate.done) {
                    tempBlockDate = firstBlockDate;
                }

                firstBlockDate = null;
            }

            if (rebreakBlockDate != null
                && firstBlockDate == null
                && secondBlockDate == null
                && mc.world.getBlockState(rebreakBlockDate.pos).getBlock() != Blocks.AIR
                && mc.world.getBlockState(rebreakBlockDate.pos).getBlock() != Blocks.WATER
                && mc.world.getBlockState(rebreakBlockDate.pos).getBlock() != Blocks.LAVA
                && rebreak.get()
                && rebreakTicks >= (rebreakDelay.get() * 4)) {
                int slotx = getBestTool(mc.world.getBlockState(rebreakBlockDate.pos));
                if (slotx != -1 && slotx != DV.of(PlayerUtil.class).getSelectedSlot(mc.player.getInventory()) && swapModeSetting.get() == SwapMode.SILENT) {
                    InvUtils.swap(slotx, true);
                }

                if (slotx != -1 && slotx != DV.of(PlayerUtil.class).getSelectedSlot(mc.player.getInventory()) && swapModeSetting.get() == SwapMode.NORMAL) {
                    InvUtils.swap(slotx, false);
                }

                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, rebreakBlockDate.pos, rebreakBlockDate.direction));
                rebreakTicks = 0;
                if (slotx != -1) {
                    InvUtils.swapBack();
                }
            }
        }
    }

    public static void stopMine(BlockPos pos) {
        MinecraftClient.getInstance().player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, pos.add(0, 300, 0), Direction.UP));
        MinecraftClient.getInstance().player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos.add(0, 300, 0), Direction.UP));
    }

    public void rangeCheck() {
        if (firstBlockDate != null && PlayerUtils.distanceTo(firstBlockDate.pos) > range.get().intValue()) {
            firstBlockDate = null;
        }

        if (secondBlockDate != null && PlayerUtils.distanceTo(secondBlockDate.pos) > range.get().intValue()) {
            firstBlockDate = null;
        }

        if (rebreakBlockDate != null && PlayerUtils.distanceTo(rebreakBlockDate.pos) > range.get().intValue()) {
            rebreakBlockDate = null;
        }
    }

    public void mineBlock(BlockPos pos, Direction direction) {
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, direction));
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, pos, direction));
        mc.player.swingHand(Hand.MAIN_HAND);
        if (!doubleBreak.get()) {
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, pos, direction));
        } else {
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, direction));
        }
    }

    @EventHandler
    public void onPacket(PacketEvent.Send event) {
        if (event.packet instanceof PlayerActionC2SPacket playerActionC2SPacket) {
            if (playerActionC2SPacket.getAction() == Action.START_DESTROY_BLOCK
                && unbreakableBlocks.contains(mc.world.getBlockState(playerActionC2SPacket.getPos()).getBlock())) {
                event.cancel();
                stopMine(playerActionC2SPacket.getPos());
            }

            if (playerActionC2SPacket.getAction() == Action.ABORT_DESTROY_BLOCK
                && unbreakableBlocks.contains(mc.world.getBlockState(playerActionC2SPacket.getPos()).getBlock())) {
                event.cancel();
                stopMine(playerActionC2SPacket.getPos());
            }

            if (playerActionC2SPacket.getAction() == Action.STOP_DESTROY_BLOCK
                && unbreakableBlocks.contains(mc.world.getBlockState(playerActionC2SPacket.getPos()).getBlock())) {
                event.cancel();
                stopMine(playerActionC2SPacket.getPos());
            }
        }
    }

    @EventHandler
    public void onClickBlock(StartBreakingBlockEvent event) {
        BlockPos pos = event.blockPos;
        Direction direction = event.direction;
        if (!unbreakableBlocks.contains(mc.world.getBlockState(pos).getBlock())
            && !breakBlocks.contains(pos)
            && !(pos.toCenterPos().distanceTo(DV.of(PlayerUtil.class).getEntityPos(mc.player)) > range.get().intValue())) {
            if ((firstBlockDate == null || !pos.equals(firstBlockDate.pos)) && (secondBlockDate == null || !pos.equals(secondBlockDate.pos))) {
                if (firstBlockDate != null && !pos.equals(firstBlockDate.pos) && doubleBreak.get()) {
                    if (secondBlockDate == null || !pos.equals(secondBlockDate.pos)) {
                        secondBlockDate = new BlockDate(pos, direction);
                        firstBlockDate.progress = firstBlockDate.progress - (1.0 - speed.get()) * 0.7;
                    }
                } else if (firstBlockDate == null || !pos.equals(firstBlockDate.pos)) {
                    firstBlockDate = new BlockDate(pos, direction);
                }

                if (doubleBreak.get()) {
                    mineBlock(pos, direction);
                }

                if (!doubleBreak.get()) {
                    event.cancel();
                }
            } else {
                event.cancel();
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {

        if (render.get()) {
            if (firstBlockDate != null && mc.world.getBlockState(firstBlockDate.pos).getBlock() != Blocks.AIR) {
                BlockPos blockPos = firstBlockDate.pos;
                double progress = firstBlockDate.progress * (1.0 / speed.get());
                if (progress > 1.0) {
                    progress = 1.0;
                }

                if (progress < 0.0) {
                    progress = 0.0;
                }

                double x1 = blockPos.getX() + (0.5 - 0.5 * progress);
                double y1 = blockPos.getY()
                    + (0.5 - 0.5 * progress);
                double z1 = blockPos.getZ() + (0.5 - 0.5 * progress);
                double x2 = blockPos.getX()
                    + 0.5
                    + 0.5 * progress;
                double y2 = blockPos.getY()
                    + 0.5
                    + 0.5 * progress;
                double z2 = blockPos.getZ()
                    + 0.5
                    + 0.5 * progress;
                int side_r = sideColor.get().r + (int) ((readySideColor.get().r - sideColor.get().r) * progress);
                int side_g = sideColor.get().g + (int) ((readySideColor.get().g - sideColor.get().g) * progress);
                int side_b = sideColor.get().b + (int) ((readySideColor.get().b - sideColor.get().b) * progress);
                int side_a = sideColor.get().a + (int) ((readySideColor.get().a - sideColor.get().a) * progress);
                int line_r = lineColor.get().r + (int) ((readyLineColor.get().r - lineColor.get().r) * progress);
                int line_g = lineColor.get().g + (int) ((readyLineColor.get().g - lineColor.get().g) * progress);
                int line_b = lineColor.get().b + (int) ((readyLineColor.get().b - lineColor.get().b) * progress);
                int linr_a = lineColor.get().a + (int) ((readyLineColor.get().a - lineColor.get().a) * progress);
                SettingColor _sideColor = new SettingColor(side_r, side_g, side_b, side_a);
                SettingColor _lineColor = new SettingColor(line_r, line_g, line_b, linr_a);
                event.renderer.box(x1, y1, z1, x2, y2, z2, _sideColor, _lineColor, shapeMode.get(), 0);
            }

            if (secondBlockDate != null && mc.world.getBlockState(secondBlockDate.pos).getBlock() != Blocks.AIR) {
                BlockPos blockPosx = secondBlockDate.pos;
                double progressx = secondBlockDate.progress * (1.0 / speed.get());
                if (progressx > 1.0) {
                    progressx = 1.0;
                }

                if (progressx < 0.0) {
                    progressx = 0.0;
                }

                double x1 = blockPosx.getX()
                    + (0.5 - 0.5 * progressx);
                double y1 = blockPosx.getY()
                    + (0.5 - 0.5 * progressx);
                double z1 = blockPosx.getZ()
                    + (0.5 - 0.5 * progressx);
                double x2 = blockPosx.getX()
                    + 0.5
                    + 0.5 * progressx;
                double y2 = blockPosx.getY()
                    + 0.5
                    + 0.5 * progressx;
                double z2 = blockPosx.getZ() + 0.5 + 0.5 * progressx;
                int side_r = sideColor.get().r + (int) ((readySideColor.get().r - sideColor.get().r) * progressx);
                int side_g = sideColor.get().g + (int) ((readySideColor.get().g - sideColor.get().g) * progressx);
                int side_b = sideColor.get().b + (int) ((readySideColor.get().b - sideColor.get().b) * progressx);
                int side_a = sideColor.get().a + (int) ((readySideColor.get().a - sideColor.get().a) * progressx);
                int line_r = lineColor.get().r + (int) ((readyLineColor.get().r - lineColor.get().r) * progressx);
                int line_g = lineColor.get().g + (int) ((readyLineColor.get().g - lineColor.get().g) * progressx);
                int line_b = lineColor.get().b + (int) ((readyLineColor.get().b - lineColor.get().b) * progressx);
                int linr_a = lineColor.get().a + (int) ((readyLineColor.get().a - lineColor.get().a) * progressx);
                SettingColor _sideColor = new SettingColor(side_r, side_g, side_b, side_a);
                SettingColor _lineColor = new SettingColor(line_r, line_g, line_b, linr_a);
                event.renderer.box(x1, y1, z1, x2, y2, z2, _sideColor, _lineColor, shapeMode.get(), 0);
            }

            if (tempBlockDate != null && mc.world.getBlockState(tempBlockDate.pos).getBlock() != Blocks.AIR) {
                BlockPos blockPosxx = tempBlockDate.pos;
                double progressxx = tempBlockDate.progress * (1.0 / speed.get());
                if (progressxx > 1.0) {
                    progressxx = 1.0;
                }

                if (progressxx < 0.0) {
                    progressxx = 0.0;
                }

                double x1 = blockPosxx.getX()
                    + (0.5 - 0.5 * progressxx);
                double y1 = blockPosxx.getY()
                    + (0.5 - 0.5 * progressxx);
                double z1 = blockPosxx.getZ()
                    + (0.5 - 0.5 * progressxx);
                double x2 = blockPosxx.getX()
                    + 0.5
                    + 0.5 * progressxx;
                double y2 = blockPosxx.getY()
                    + 0.5
                    + 0.5 * progressxx;
                double z2 = blockPosxx.getZ()
                    + 0.5
                    + 0.5 * progressxx;
                int side_r = sideColor.get().r + (int) ((readySideColor.get().r - sideColor.get().r) * progressxx);
                int side_g = sideColor.get().g + (int) ((readySideColor.get().g - sideColor.get().g) * progressxx);
                int side_b = sideColor.get().b + (int) ((readySideColor.get().b - sideColor.get().b) * progressxx);
                int side_a = sideColor.get().a + (int) ((readySideColor.get().a - sideColor.get().a) * progressxx);
                int line_r = lineColor.get().r + (int) ((readyLineColor.get().r - lineColor.get().r) * progressxx);
                int line_g = lineColor.get().g + (int) ((readyLineColor.get().g - lineColor.get().g) * progressxx);
                int line_b = lineColor.get().b + (int) ((readyLineColor.get().b - lineColor.get().b) * progressxx);
                int linr_a = lineColor.get().a + (int) ((readyLineColor.get().a - lineColor.get().a) * progressxx);
                SettingColor _sideColor = new SettingColor(side_r, side_g, side_b, side_a);
                SettingColor _lineColor = new SettingColor(line_r, line_g, line_b, linr_a);
                event.renderer.box(x1, y1, z1, x2, y2, z2, _sideColor, _lineColor, shapeMode.get(), 0);
            }

            if (rebreak.get()
                && rebreakBlockDate != null
                && mc.world.getBlockState(rebreakBlockDate.pos).getBlock() != Blocks.AIR
                && mc.world.getBlockState(rebreakBlockDate.pos).getBlock() != Blocks.WATER
                && mc.world.getBlockState(rebreakBlockDate.pos).getBlock() != Blocks.LAVA
                && firstBlockDate == null
                && secondBlockDate == null) {
                BlockPos blockPosxxx = rebreakBlockDate.pos;
                double x1 = blockPosxxx.getX();
                double y1 = blockPosxxx.getY();
                double z1 = blockPosxxx.getZ();
                double x2 = blockPosxxx.getX() + 1;
                double y2 = blockPosxxx.getY() + 1;
                double z2 = blockPosxxx.getZ() + 1;
                event.renderer.box(x1, y1, z1, x2, y2, z2, readySideColor.get(), readyLineColor.get(), shapeMode.get(), 0);
            }
        }
    }

    public int getBestTool(BlockState blockState) {
        double bestScore = -1.0;
        int bestSlot = -1;

        for (int i = 0; i < 9; i++) {
            double score = mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(blockState);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        Item item = mc.player.getInventory().getStack(bestSlot).getItem();
        return DV.of(ItemUtil.class).isToolOrWeapon(item) ? -1 : bestSlot;
    }

    public BlockDate getBlockDate(BlockPos pos, Direction direction) {
        return new BlockDate(pos, direction);
    }

    public BlockDate getBlockDate(BlockPos pos, Direction direction, boolean rebreak) {
        return new BlockDate(pos, direction, rebreak);
    }

    public class BlockDate {
        public BlockPos pos;
        public Direction direction;
        public boolean done = false;
        public double progress;
        public BlockState blockState;
        public boolean isMining = false;
        public boolean isBreaked = false;
        public boolean rebreak = true;

        public BlockDate(BlockPos pos, Direction direction) {
            this.pos = pos;
            this.direction = direction;
            this.done = false;
            this.progress = 0.0;
            blockState = MinecraftClient.getInstance().world.getBlockState(pos);
        }

        public BlockDate(BlockPos pos, Direction direction, boolean rebreak) {
            this.pos = pos;
            this.direction = direction;
            this.done = false;
            this.progress = 0.0;
            this.blockState = MinecraftClient.getInstance().world.getBlockState(pos);
            this.rebreak = rebreak;
        }

        public void freshProgress() {
            double bestScore = -1.0;
            int bestSlot = -1;

            for (int i = 0; i < 9; i++) {
                double score = MinecraftClient.getInstance().player.getInventory().getStack(i).getMiningSpeedMultiplier(blockState);
                if (score > bestScore) {
                    bestScore = score;
                    bestSlot = i;
                }
            }


            Item item = MinecraftClient.getInstance().player.getInventory().getStack(bestSlot).getItem();
            if (!DV.of(ItemUtil.class).isToolOrWeapon(item)) {
                bestSlot = -1;
            }

            if (progress <= 1.0 * GhostMine.this.speed.get()) {
                progress = progress
                    + BlockUtils.getBreakDelta(bestSlot != -1 ? bestSlot : DV.of(PlayerUtil.class).getSelectedSlot(MinecraftClient.getInstance().player.getInventory()), blockState);
            } else {
                if (bestSlot != -1 && swapModeSetting.get() == SwapMode.NORMAL) {
                    InvUtils.swap(bestSlot, false);
                }

                done = true;
                progress = 1.0;
            }
        }
    }

    public static enum SwapMode {
        SILENT,
        NORMAL;
    }
}
