package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.dynamic.DV;
import com.github.mikumiku.addon.util.BagUtil;
import com.github.mikumiku.addon.util.BaritoneUtil;
import com.github.mikumiku.addon.util.VUtil;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class AutoWither extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("渲染");

    // 通用设置

    private final Setting<Integer> horizontalRadius = sgGeneral.add(new IntSetting.Builder()
        .name("水平半径")
        .description("放置的水平半径")
        .defaultValue(4)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Integer> verticalRadius = sgGeneral.add(new IntSetting.Builder()
        .name("垂直半径")
        .description("放置的垂直半径")
        .defaultValue(3)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Priority> priority = sgGeneral.add(new EnumSetting.Builder<Priority>()
        .name("优先级")
        .description("优先级")
        .defaultValue(Priority.随机)
        .build()
    );

    private final Setting<Integer> witherDelay = sgGeneral.add(new IntSetting.Builder()
        .name("凋零延迟")
        .description("凋零放置之间的延迟（刻）")
        .defaultValue(1)
        .min(1)
        .sliderMax(10)
        .build()
    );

    private final Setting<Integer> blockDelay = sgGeneral.add(new IntSetting.Builder()
        .name("方块延迟")
        .description("方块放置之间的延迟（刻）")
        .defaultValue(1)
        .min(0)
        .sliderMax(10)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("旋转")
        .description("建造时是否旋转")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> turnOff = sgGeneral.add(new BoolSetting.Builder()
        .name("自动关闭")
        .description("建造单个凋零怪后自动关闭")
        .defaultValue(true)
        .build()
    );

    // 渲染设置

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("形状模式")
        .description("形状的渲染方式")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("侧面颜色")
        .description("目标方块渲染的侧面颜色")
        .defaultValue(new SettingColor(197, 137, 232, 10))
        .build()
    );
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("线条颜色")
        .description("目标方块渲染的线条颜色")
        .defaultValue(new SettingColor(197, 137, 232))
        .build()
    );

    private final Pool<Wither> witherPool = new Pool<>(Wither::new);
    private final ArrayList<Wither> withers = new ArrayList<>();
    private Wither wither;

    private int witherTicksWaited, blockTicksWaited;

    public AutoWither() {
        super("自动放凋零", "自动建造凋零");
    }

    @Override
    public void onActivate() {
        super.onActivate();
    }

    @Override
    public void onDeactivate() {
        wither = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (wither == null) {
            // 延迟
            if (witherTicksWaited < witherDelay.get() - 1) {
                return;
            }

            // 清空池和列表
            for (Wither wither : withers) witherPool.free(wither);
            withers.clear();

            // 注册
            BlockIterator.register(horizontalRadius.get(), verticalRadius.get(), (blockPos, blockState) -> {
                Direction dir = DV.of(VUtil.class).getOppositeDirectionTo(blockPos);
                if (isValidSpawn(blockPos, dir)) withers.add(witherPool.get().set(blockPos, dir));
            });
        }
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (wither == null) {
            // 延迟
            if (witherTicksWaited < witherDelay.get() - 1) {
                witherTicksWaited++;
                return;
            }


            if (withers.isEmpty()) return;

            // 排序
            switch (priority.get()) {
                case 最近:
                    withers.sort(Comparator.comparingDouble(w -> PlayerUtils.distanceTo(w.foot)));
                case 最远:
                    withers.sort((w1, w2) -> {
                        int sort = Double.compare(PlayerUtils.distanceTo(w1.foot), PlayerUtils.distanceTo(w2.foot));
                        if (sort == 0) return 0;
                        return sort > 0 ? -1 : 1;
                    });
                case 随机:
                    Collections.shuffle(withers);
            }

            wither = withers.get(0);
        }

        // 灵魂沙/土和头颅槽位
        FindItemResult findSoulSand = InvUtils.findInHotbar(Items.SOUL_SAND);
        if (!findSoulSand.found()) findSoulSand = InvUtils.findInHotbar(Items.SOUL_SOIL);
        FindItemResult findWitherSkull = InvUtils.findInHotbar(Items.WITHER_SKELETON_SKULL);

        // 检查是否有足够的资源
        if (!findSoulSand.found() || !findWitherSkull.found()) {
            error("快捷栏中没有足够的资源");
            toggle();
            return;
        }


        // 建造
        if (blockDelay.get() == 0) {
            // 一 tick 内完成所有
            // 身体
            placeBlock(wither.foot, findSoulSand.slot());
            placeBlock(wither.foot.up(), findSoulSand.slot());
            placeBlock(wither.foot.up().offset(wither.axis, -1), findSoulSand.slot());
            placeBlock(wither.foot.up().offset(wither.axis, 1), findSoulSand.slot());

            placeBlock(wither.foot.up().up(), findWitherSkull.slot());
            placeBlock(wither.foot.up().up().offset(wither.axis, -1), findWitherSkull.slot());
            placeBlock(wither.foot.up().up().offset(wither.axis, 1), findWitherSkull.slot());


            // 自动关闭
            if (turnOff.get()) {
                wither = null;
                toggle();
            }

        } else {
            // 延迟
            if (blockTicksWaited < blockDelay.get() - 1) {
                blockTicksWaited++;
                return;
            }

            switch (wither.stage) {
                case 0:
                    placeBlock(wither.foot, findSoulSand.slot());
                    wither.stage++;
                    break;
                case 1:
                    placeBlock(wither.foot.up(), findSoulSand.slot());
                    wither.stage++;
                    break;
                case 2:
                    placeBlock(wither.foot.up().offset(wither.axis, -1), findSoulSand.slot());

                    wither.stage++;
                    break;
                case 3:
                    placeBlock(wither.foot.up().offset(wither.axis, 1), findSoulSand.slot());

                    wither.stage++;
                    break;
                case 4:
                    placeBlock(wither.foot.up().up(), findWitherSkull.slot());
                    wither.stage++;
                    break;
                case 5:
                    placeBlock(wither.foot.up().up().offset(wither.axis, -1), findWitherSkull.slot());
                    wither.stage++;
                    break;
                case 6:
                    placeBlock(wither.foot.up().up().offset(wither.axis, 1), findWitherSkull.slot());
                    wither.stage++;
                    break;
                case 7:
                    // 自动关闭
                    if (turnOff.get()) {
                        wither = null;
                        toggle();
                    }
                    break;
            }
        }


        witherTicksWaited = 0;
    }

    public void placeBlock(BlockPos blockPos, int slot) {
        BagUtil.doSwap(slot);
        BaritoneUtil.placeBlock(blockPos);
        BagUtil.doSwap(slot);
        BagUtil.sync();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (wither == null) return;

        // 身体
        event.renderer.box(wither.foot, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        event.renderer.box(wither.foot.up(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        event.renderer.box(wither.foot.up().offset(wither.axis, -1), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        event.renderer.box(wither.foot.up().offset(wither.axis, 1), sideColor.get(), lineColor.get(), shapeMode.get(), 0);

        // 头部
        BlockPos midHead = wither.foot.up().up();
        BlockPos leftHead = wither.foot.up().up().offset(wither.axis, -1);
        BlockPos rightHead = wither.foot.up().up().offset(wither.axis, 1);

        event.renderer.box((double) midHead.getX() + 0.2, midHead.getX(), (double) midHead.getX() + 0.2,
            (double) midHead.getX() + 0.8, (double) midHead.getX() + 0.7, (double) midHead.getX() + 0.8,
            sideColor.get(), lineColor.get(), shapeMode.get(), 0);

        event.renderer.box((double) leftHead.getX() + 0.2, leftHead.getX(), (double) leftHead.getX() + 0.2,
            (double) leftHead.getX() + 0.8, (double) leftHead.getX() + 0.7, (double) leftHead.getX() + 0.8,
            sideColor.get(), lineColor.get(), shapeMode.get(), 0);

        event.renderer.box((double) rightHead.getX() + 0.2, rightHead.getX(), (double) rightHead.getX() + 0.2,
            (double) rightHead.getX() + 0.8, (double) rightHead.getX() + 0.7, (double) rightHead.getX() + 0.8,
            sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    private boolean isValidSpawn(BlockPos blockPos, Direction direction) {
        // 凋零怪是 3x3x1

        // 检查 y > (255 - 3)
        // 因为凋零怪有 3 个方块高
        if (blockPos.getY() > 252) return false;

        // 根据方向确定宽度
        int widthX = 0;
        int widthZ = 0;

        if (direction == Direction.EAST || direction == Direction.WEST) widthZ = 1;
        if (direction == Direction.NORTH || direction == Direction.SOUTH) widthX = 1;


        // 检查非空气方块和实体
        BlockPos.Mutable bp = new BlockPos.Mutable();
        for (int x = blockPos.getX() - widthX; x <= blockPos.getX() + widthX; x++) {
            for (int z = blockPos.getZ() - widthZ; z <= blockPos.getZ(); z++) {
                for (int y = blockPos.getY(); y <= blockPos.getY() + 2; y++) {
                    bp.set(x, y, z);
                    if (!mc.world.getBlockState(bp).isReplaceable()) return false;
                    if (!mc.world.canPlace(Blocks.STONE.getDefaultState(), bp, ShapeContext.absent())) return false;
                }
            }
        }

        return true;
    }

    public enum Priority {
        最近,
        最远,
        随机
    }

    private static class Wither {
        public int stage;
        // 0 = 脚部
        // 1 = 身体中部
        // 2 = 左臂
        // 3 = 右臂
        // 4 = 中间头部
        // 5 = 左头部
        // 6 = 右头部
        // 7 = 结束
        public BlockPos.Mutable foot = new BlockPos.Mutable();
        public Direction facing;
        public Direction.Axis axis;

        public Wither set(BlockPos pos, Direction dir) {
            this.stage = 0;
            this.foot.set(pos);
            this.facing = dir;
            this.axis = dir.getAxis();

            return this;
        }
    }
}
