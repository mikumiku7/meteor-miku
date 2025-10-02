package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.BagUtil;
import com.github.mikumiku.addon.util.BaritoneUtil;
import com.github.mikumiku.addon.util.WorldUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class RoadBuilder extends BaseModule {
    SettingGroup sgGeneral = settings.getDefaultGroup();
    SettingGroup renderGeneral = settings.createGroup("渲染");

    Setting<Double> range = sgGeneral
            .add(
                    new DoubleSetting.Builder()
                            .name("建造范围")
                            .description("自动建造的最大范围")
                            .defaultValue(4)
                            .range(0, 6)
                            .build()
            );
    Setting<Block> blockType = sgGeneral
            .add(new BlockSetting.Builder()
                    .name("方块")
                    .description("选择用于建造的方块")
                    .defaultValue(Blocks.COBBLESTONE)
                    .build()
            );

    Setting<SlapType> slapType = sgGeneral.add(new EnumSetting.Builder<SlapType>()
            .name("半砖方向(如果有)")
            .description("半砖方向")
            .defaultValue(SlapType.UP)
            .visible(() -> blockType.get() instanceof SlabBlock)
            .build()
    );

    Setting<Integer> blocksPer = sgGeneral
            .add(
                    new IntSetting.Builder()
                            .name("每次放置数量")
                            .description("每个游戏刻放置的方块数量")
                            .defaultValue(2)
                            .sliderRange(1, 6)
                            .build()
            );
    Setting<Integer> delay = sgGeneral
            .add(
                    new IntSetting.Builder()
                            .name("放置延迟")
                            .description("放置方块之间的延迟时间（单位：tick）")
                            .defaultValue(0)
                            .sliderRange(0, 20)
                            .build()
            );
    Setting<Boolean> render = renderGeneral
            .add(
                    new BoolSetting.Builder()
                            .name("显示渲染")
                            .description("是否显示待放置方块的渲染预览")
                            .defaultValue(true)
                            .build()
            );
    Setting<ShapeMode> shapeMode = renderGeneral
            .add(
                    ((EnumSetting.Builder) ((EnumSetting.Builder) ((EnumSetting.Builder) new EnumSetting.Builder()
                            .name("渲染模式"))
                            .description("选择渲染的显示模式"))
                            .defaultValue(ShapeMode.Both))
                            .build()
            );
    Setting<SettingColor> readySideColor = renderGeneral
            .add(
                    new ColorSetting.Builder()
                            .name("侧面颜色")
                            .description("待放置方块渲染的侧面填充颜色")
                            .defaultValue(new SettingColor(135, 206, 235, 30))//天空蓝色
                            .build()
            );
    Setting<SettingColor> readyLineColor = renderGeneral
            .add(
                    new ColorSetting.Builder()
                            .name("线条颜色")
                            .description("待放置方块渲染的边框线条颜色")
                            .defaultValue(new SettingColor(100, 149, 237, 80)) //矢车菊蓝色
                            .build()
            );
    List<BlockPos> placeList = new ArrayList<>();
    int tickCounter = 0;
    int tickBlockCount = 0;

    public RoadBuilder() {
        super(
                "脚下快速搭平台",
                "自动建造平台或脚下搭路，需要手动放第一块"
        );
    }

    @Override
    public void onActivate() {
        super.onActivate();

    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc == null) {
            mc = MinecraftClient.getInstance();
        }
        if (mc.player == null || mc.world == null) {
            return;
        }


        placeList.clear();
        tickBlockCount = 0;

        // 使用 tick 计时
        if (tickCounter < delay.get()) {
            tickCounter++;
            return;
        }
        tickCounter = 0;

        Vec3d eyePos = mc.player.getEyePos();
        BlockPos centerPos = new BlockPos((int) Math.round(eyePos.x), (int) Math.round(eyePos.y), (int) Math.round(eyePos.z));

        List<BlockPos> spheres = WorldUtils.getSphere(range.get());

        for (BlockPos pos : spheres) {

            if ((blockType.get() instanceof SlabBlock && slapType.get() == SlapType.DOWN)) {
                Direction direction = BaritoneUtil.getInteractDirection(pos, true);
                if (BaritoneUtil.canSeeBlockFace(pos, direction)
                        && mc.world.getBlockState(pos).getBlock().asItem() == Items.AIR
                        && isAdjacent(pos)
                        && pos.getY() == mc.player.getBlockPos().getY()) {
                    tryPlace(pos);
                }
            } else if (BaritoneUtil.canPlace(pos, true)
                    && isAdjacent(pos)
                    && pos.getY() == mc.player.getBlockPos().down().getY()
                    && mc.world.getBlockState(pos).getBlock().asItem() == Items.AIR) {
                tryPlace(pos);
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get()
                && placeList.size() > 0
                && (BagUtil.find(blockType.get().asItem()).found())) {
            for (int i = 0; i < placeList.size(); i++) {
                double x1 = placeList.get(i).getX();
                double y1 = placeList.get(i).getY();
                double z1 = placeList.get(i).getZ();
                double x2 = placeList.get(i).getX() + 1;
                double y2 = placeList.get(i).getY() + 1;
                double z2 = placeList.get(i).getZ() + 1;
                event.renderer.box(x1, y1, z1, x2, y2, z2, readySideColor.get(), readyLineColor.get(), shapeMode.get(), 0);
            }
        }
    }

    private void tryPlace(BlockPos pos) {
        if (pos == null) {
            return;
        }
        if (placeList.contains(pos)) {
            return;
        }
        if (tickBlockCount >= blocksPer.get()) {
            return;
        }
        double distance = BaritoneUtil.getEyesPos().squaredDistanceTo(pos.toCenterPos());

        if (MathHelper.sqrt((float) distance) > range.get()) {
            return;
        }

        int itemResult = BagUtil.findItemInventorySlotGrim(blockType.get().asItem());

        if (itemResult == -1) {
            return;
        }

        placeList.add(pos);
        BagUtil.doSwap(itemResult);

        // 根据方块类型和 slapType 设置选择放置方式
        if (blockType.get() instanceof SlabBlock) {
            if (slapType.get() == SlapType.UP) {
                BaritoneUtil.placeUpBlock(pos, true, true, true);
            } else {
                BaritoneUtil.placeDownBlock(pos, true, true, true);
            }
        } else {
            BaritoneUtil.placeBlock(pos, true, true, true);
        }

        BagUtil.doSwap(itemResult);
        BagUtil.sync();
        tickBlockCount++;
    }

    private boolean isAdjacent(BlockPos pos) {
        Block selectedBlock = blockType.get();
        SlapType slabTypeSetting = slapType.get();
        boolean isSlabBlock = selectedBlock instanceof SlabBlock;

        for (Direction direction : Direction.values()) {
            if (direction == Direction.DOWN || direction == Direction.UP) {
                continue;
            }

            BlockPos neighborPos = pos.offset(direction);
            Block neighborBlock = mc.world.getBlockState(neighborPos).getBlock();

            if (isSlabBlock) {
                if (neighborBlock instanceof SlabBlock) {
                    SlabType neighborSlabType = mc.world.getBlockState(neighborPos).get(Properties.SLAB_TYPE);

                    if (slabTypeSetting == SlapType.UP && neighborSlabType == SlabType.TOP) {
                        return true;
                    } else if (slabTypeSetting == SlapType.DOWN &&
                            (neighborSlabType == SlabType.BOTTOM || neighborSlabType == SlabType.DOUBLE)) {
                        return true;
                    }
                }
            } else {
                if (neighborBlock == selectedBlock) {
                    return true;
                }
            }
        }

        return false;
    }


    enum SlapType {
        UP,
        DOWN;
    }

}
