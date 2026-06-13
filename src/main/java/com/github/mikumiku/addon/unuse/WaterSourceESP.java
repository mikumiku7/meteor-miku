//package com.github.mikumiku.addon.modules;
//
//
//import com.github.mikumiku.addon.MikuMikuAddon;
//import meteordevelopment.meteorclient.events.render.Render3DEvent;
//import meteordevelopment.meteorclient.settings.ColorSetting;
//import meteordevelopment.meteorclient.systems.modules.Module;
//import meteordevelopment.meteorclient.utils.render.RenderUtils;
//import meteordevelopment.meteorclient.utils.render.color.Color;
//import net.minecraft.block.BlockState;
//import net.minecraft.block.Blocks;
//import net.minecraft.block.FluidBlock;
//import net.minecraft.util.math.BlockPos;
//
//@Deprecated
//public class WaterSourceESP extends Module {
//    private final ColorSetting waterColor = new ColorSetting.Builder()
//        .name("water-color")
//        .description("Color for water source blocks.")
//        .defaultValue(new Color(0, 0, 255, 50))
//        .build();
//
//    private final ColorSetting lavaColor = new ColorSetting.Builder()
//        .name("lava-color")
//        .description("Color for lava source blocks.")
//        .defaultValue(new Color(255, 100, 0, 50))
//        .build();
//
//    public WaterSourceESP() {
//        super(MikuMikuAddon.CATEGORY, "water-esp", "Renders water and lava source blocks.");
//    }
//
//    @Override
//    public void onRender3D(Render3DEvent event) {
//        // 循环渲染玩家周围范围
//        BlockPos playerPos = mc.player.getBlockPos();
//        int range = 10; // 自定义渲染范围
//
//        for (int x = -range; x <= range; x++) {
//            for (int y = -range; y <= range; y++) {
//                for (int z = -range; z <= range; z++) {
//                    BlockPos pos = playerPos.add(x, y, z);
//                    BlockState state = mc.world.getBlockState(pos);
//
//                    // 判断是否为 FluidBlock
//                    if (state.getBlock() instanceof FluidBlock) {
//                        Integer level = state.get(FluidBlock.LEVEL);
//                        if (level != null && level == 0) {
//                            // 判断水源
//                            if (state.isOf(Blocks.WATER)) {
//                                RenderUtils.drawBoxOutline(pos, waterColor.get(), 1);
//                            }
//
//                            // 判断岩浆源
//                            if (state.isOf(Blocks.LAVA)) {
//                                RenderUtils.drawBoxOutline(pos, lavaColor.get(), 1);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
