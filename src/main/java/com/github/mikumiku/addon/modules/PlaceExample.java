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
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动建造脚下放置方块的示例, 展示 BaritoneUtil, BagUtil, PositionCache 用法
 */
public class PlaceExample extends BaseModule {
    SettingGroup sgGeneral = settings.getDefaultGroup();
    SettingGroup renderGeneral = settings.createGroup("渲染");

    Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("建造范围")
        .description("自动建造的最大范围")
        .defaultValue(4)
        .range(0, 6)
        .build()
    );
    Setting<Block> blockType = sgGeneral.add(new BlockSetting.Builder()
        .name("方块")
        .description("选择用于建造的方块")
        .defaultValue(Blocks.COBBLESTONE)
        .build()
    );

    Setting<Integer> blocksPer = sgGeneral.add(new IntSetting.Builder()
        .name("每次放置数量")
        .description("每个游戏刻放置的方块数量")
        .defaultValue(2)
        .sliderRange(1, 6)
        .build()
    );
    Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("放置延迟")
        .description("放置方块之间的延迟时间（单位：tick）")
        .defaultValue(0)
        .sliderRange(0, 20)
        .build()
    );
    Setting<Boolean> render = renderGeneral.add(new BoolSetting.Builder()
        .name("显示渲染")
        .description("是否显示待放置方块的渲染预览")
        .defaultValue(true)
        .build()
    );
    Setting<ShapeMode> shapeMode = renderGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("渲染模式")
        .description("选择渲染的显示模式")
        .defaultValue(ShapeMode.Both)
        .build()
    );
    Setting<SettingColor> readySideColor = renderGeneral.add(new ColorSetting.Builder()
        .name("侧面颜色")
        .description("待放置方块渲染的侧面填充颜色")
        .defaultValue(new SettingColor(135, 206, 235, 30))//天空蓝色
        .build()
    );
    Setting<SettingColor> readyLineColor = renderGeneral.add(new ColorSetting.Builder()
        .name("线条颜色")
        .description("待放置方块渲染的边框线条颜色")
        .defaultValue(new SettingColor(100, 149, 237, 80)) //矢车菊蓝色
        .build()
    );

    List<BlockPos> placeList = new ArrayList<>();
    int tickCounter = 0;
    int tickBlockCount = 0;

    /**
     * 位置缓存管理器，防止短时间重复放置
     */
    private final PositionCache positionCache = new PositionCache(1000L); // 1秒过期

    public PlaceExample() {
        super(
            "示例放置方块",
            "自动建造脚下放置方块的示例, 展示 BaritoneUtil, BagUtil, PositionCache 用法"
        );
    }

    @Override
    public void onActivate() {
        super.onActivate();
        positionCache.startCleanupThread();
    }

    @Override
    public void onDeactivate() {
        positionCache.shutdown(); // 关闭缓存管理器
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {

        placeList.clear();
        tickBlockCount = 0;

        // 使用 tick 计时
        if (tickCounter < delay.get()) {
            tickCounter++;
            return;
        }
        tickCounter = 0;

        List<BlockPos> spheres = WorldUtils.getSphere(range.get());

        for (BlockPos pos : spheres) {
            if (BaritoneUtil.canPlace(pos, true)
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

        // 检查位置是否在缓存中（防止短时间重复放置）
        if (positionCache.isInCache(pos)) {
            return;
        }

        double distance = BaritoneUtil.getEyesPos().squaredDistanceTo(pos.toCenterPos());

        if (MathHelper.sqrt((float) distance) > range.get()) {
            return;
        }

        int slotGrim = BagUtil.findItemInventorySlotGrim(blockType.get().asItem());

        if (slotGrim == -1) {
            return;
        }

        placeList.add(pos);
        BagUtil.doSwap(slotGrim);

        BaritoneUtil.placeBlock(pos);

        BagUtil.doSwap(slotGrim);
        BagUtil.sync();
        tickBlockCount++;

        // 将位置添加到缓存，记录放置尝试（无论成功或失败）
        positionCache.addToCache(pos);
    }

    private boolean isAdjacent(BlockPos pos) {
        Block selectedBlock = blockType.get();

        for (Direction direction : Direction.values()) {
            if (direction == Direction.DOWN || direction == Direction.UP) {
                continue;
            }

            BlockPos neighborPos = pos.offset(direction);
            Block neighborBlock = mc.world.getBlockState(neighborPos).getBlock();


            if (neighborBlock == selectedBlock) {
                return true;
            }
        }

        return false;
    }

}
