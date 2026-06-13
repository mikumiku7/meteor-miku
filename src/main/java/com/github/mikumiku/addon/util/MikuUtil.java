package com.github.mikumiku.addon.util;


import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalNear;
import lombok.experimental.UtilityClass;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.util.Set;

@UtilityClass
public class MikuUtil {

    public void printInv() {
        String inv = """
            ╔═══╦═══════════╗
            ║ 5 ║    ███    ║   ╔═══╦═══╗
            ╠═══╣    ███    ║   ║ 1 ║ 2 ║   ╔═══╗
            ║ 6 ║  █████   ║   ╠═══╬═══╣   ║ 0 ║
            ╠═══╣  █████   ║   ║ 3 ║ 4 ║   ╚═══╝
            ║ 7 ║  █████   ║   ╚═══╩═══╝
            ╠═══╣    ███    ╠═══╗
            ║ 8 ║    ███    ║45 ║
            ╚═══╩══════════  ═╩═══╝
            ╔═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╗
            ║ 9 ║10 ║11 ║12 ║13 ║14 ║15 ║16 ║17 ║
            ╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
            ║18 ║19 ║20 ║21 ║22 ║23 ║24 ║25 ║26 ║
            ╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
            ║27 ║28 ║29 ║30 ║31 ║32 ║33 ║34 ║35 ║
            ╚═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╝
            ╔═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╗
            ║36 ║37 ║38 ║39 ║40 ║41 ║42 ║43 ║44 ║
            ╚═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╝

            """;
        String inv2 = """
            ╔═══╦═══════════╗
            ║ 5 ║    ███    ║    ╔═══╦═══╗
            ╠═══╣    ███    ║    ║ 1 ║ 2 ║   ╔═══╗
            ║ 6 ║  ██████  ║   ╠═══╬═══╣   ║ 0 ║
            ╠═══╣  ██████  ║   ║ 3 ║ 4 ║   ╚═══╝
            ║ 7 ║  ██████  ║   ╚═══╩═══╝
            ╠═══╣    ███    ╠═══╗
            ║ 8 ║    ███    ║45 ║ ← 45 = 合成结果(Result slot)
            ╚═══╩═════════════╩═══╝
            ╔═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╗
            ║ 9 ║10 ║11 ║12 ║13 ║14 ║15 ║16 ║17 ║  ← 背包第一行
            ╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
            ║18 ║19 ║20 ║21 ║22 ║23 ║24 ║25 ║26 ║  ← 背包第二行
            ╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
            ║27 ║28 ║29 ║30 ║31 ║32 ║33 ║34 ║35 ║  ← 背包第三行
            ╚═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╝
            ╔═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╗
            ║36 ║37 ║38 ║39 ║40 ║41 ║42 ║43 ║44 ║  ← 快捷栏 (0~8)
            ╚═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╝
            """;
        String inv3 = """
            ╔══════════════════════════════════════════════════════╗
            ║                 玩家物品栏 PlayerInventory            ║
            ╠══════════════════════════════════════════════════════╣
            ║                     ↑ 背包部分 ↑                     ║
            ║╔═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╗
            ║║ 9 ║10 ║11 ║12 ║13 ║14 ║15 ║16 ║17 ║
            ║╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
            ║║18 ║19 ║20 ║21 ║22 ║23 ║24 ║25 ║26 ║
            ║╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
            ║║27 ║28 ║29 ║30 ║31 ║32 ║33 ║34 ║35 ║
            ║╚═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╝
            ║                     ↓ 快捷栏 ↓                     ║
            ║╔═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╗
            ║║ 0 ║ 1 ║ 2 ║ 3 ║ 4 ║ 5 ║ 6 ║ 7 ║ 8 ║
            ║╚═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╝
            ║   ↑副手 40↑                                        ║
            ╚══════════════════════════════════════════════════════╝
            ╔═════╦═════╦═════╦═════╗
            ║ 36  ║ 37  ║ 38  ║ 39  ║ 40
            ╚═════╩═════╩═════╩═════╝
            头盔 胸甲 护腿 靴子
            ╚═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╝
            """;
        MinecraftClient mc = MinecraftClient.getInstance();
        //3
        for (int i = 0; i <= 45; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            String name = stack.getItem().getName().getString();
            ChatUtils.success(i + ": " + name);
        }


    }

    public boolean pathTo(BlockPos pos, double distance) {

        MinecraftClient mc = MinecraftClient.getInstance();
        // 玩家是否存在与世界中
        if (mc.player == null || mc.world == null) {
            return false;
        }
        BlockPos playerBlockPos = mc.player.getBlockPos();
        BlockPos.Mutable posMutable = new BlockPos.Mutable();
        posMutable.set(pos);
        posMutable.setY(playerBlockPos.getY());
        double currentDistance = playerBlockPos.getSquaredDistance(pos);
        double requiredDistanceSq = distance * distance;

        // 如果距离足够近，则取消寻路并返回 true
        if (currentDistance <= requiredDistanceSq) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
            return true;
        }

        if (!BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
            disableBlockActions();
            // 如果 Baritone 当前没有在朝该目标寻路，则设置新的路径
            Goal goal = new GoalNear(pos, (int) distance - 1);
            if (distance <= 0) {
                goal = new GoalBlock(pos);
            }
            BaritoneAPI.getProvider().getPrimaryBaritone()
                .getCustomGoalProcess()
                .setGoalAndPath(goal);
        }

        return false;
    }

    public static void disableBlockActions() {
        var settings = BaritoneAPI.getSettings();
        // 禁止破坏方块
        settings.allowBreak.value = false;

        // 禁止放置方块
        settings.allowPlace.value = false;
    }

    public static void cancelBaritone() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
    }

    public int countItem(Item item) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player.getInventory().count(item);
    }

    public static String getItemName(ItemStack item) {

        return Registries.ITEM.getId(item.getItem()).getPath();
    }

    public static String getItemName(Item item) {

        return Registries.ITEM.getId(item).getPath();
    }

    public boolean isBlockAt(BlockPos pos, Block expectedBlock) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.world.getBlockState(pos).isOf(expectedBlock);
    }

//    public static void renderSign(String text, double x, double y, double z, float scaling, int color) {
//        MinecraftClient mc = MinecraftClient.getInstance();
//        Camera camera = mc.gameRenderer.getCamera();
//        Vec3d camPos = camera.getPos();
//
//        MatrixStack matrices = new MatrixStack();
//
//        // === 正确顺序 ===
//        matrices.push();
//
//        // 1. 平移到目标点
//        matrices.translate(x - camPos.x, y - camPos.y, z - camPos.z);
//
//        // 2. 旋转以面向玩家
//        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
//        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
//
//        // 3. 缩放（负号保证文本朝向正确）
//        matrices.scale(-scaling, -scaling, scaling);
//
//        float hwidth = mc.textRenderer.getWidth(text) / 2.0f;
//
//        // === 实际绘制 ===
//        GL11.glDepthFunc(GL11.GL_ALWAYS); // 始终绘制在最前（无遮挡）
//
//
//        VertexConsumerProvider.Immediate vertexConsumers = mc.getBufferBuilders().getEntityVertexConsumers();
//
////        ((AccessorTextRenderer) mc.textRenderer).hookDrawLayer(
////            text, -hwidth, 0.0f,
////            TextRenderer.tweakTransparency(color), true,
////            matrices.peek().getPositionMatrix(),
////            vertexConsumers, TextRenderer.TextLayerType.SEE_THROUGH,
////            0, 0xF000F0
////        );
////        vertexConsumers.draw();
////
////        ((AccessorTextRenderer) mc.textRenderer).hookDrawLayer(
////            text, -hwidth, 0.0f,
////            TextRenderer.tweakTransparency(color), false,
////            matrices.peek().getPositionMatrix(),
////            vertexConsumers, TextRenderer.TextLayerType.SEE_THROUGH,
////            0, 0xF000F0
////        );
//        vertexConsumers.draw();
//
//        GL11.glDepthFunc(GL11.GL_LEQUAL); // 恢复深度模式
//
//        matrices.pop();
//    }

    /**
     * 判断指定名称的类是否存在（可选是否初始化）
     *
     * @param className  完全限定类名，例如 "java.util.ArrayList"
     * @param initialize 是否在加载时初始化该类（执行 static 块等）
     * @return 如果类存在则返回 true，否则 false
     */
    public static boolean isClassExists(String className, boolean initialize) {
        try {
            Class.forName(className, initialize, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // 重载方法：默认不初始化类（更安全，避免副作用）
    public static boolean isClassExists(String className) {
        return isClassExists(className, false);
    }


    public static boolean isPickaxeItem(Item item) {
        if (Set.of(
            Items.WOODEN_PICKAXE,
            Items.STONE_PICKAXE,
            Items.IRON_PICKAXE,
            Items.GOLDEN_PICKAXE,
            Items.DIAMOND_PICKAXE,
            Items.NETHERITE_PICKAXE
        ).contains(item)) {
            return true;
        }
        return false;
    }

    public static boolean isSwordItem(Item item) {
        if (Set.of(
            Items.WOODEN_SWORD,
            Items.STONE_SWORD,
            Items.IRON_SWORD,
            Items.GOLDEN_SWORD,
            Items.DIAMOND_SWORD,
            Items.NETHERITE_SWORD
        ).contains(item)) {
            // 是任意剑

            return true;
        }
        return false;
    }

    public static boolean isArmor(Item item) {
        if (Set.of(
            // 皮革
            Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS,
            // 链甲
            Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS,
            // 铁
            Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
            // 金
            Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS,
            // 钻石
            Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS,
            // 下界合金
            Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS,
            // 乌龟壳（也算头盔）
            Items.TURTLE_HELMET
        ).contains(item)) {
            // 是任意剑

            return true;
        }
        return false;
    }


    public BlockPos findSuitablePlacePosition() {
        MinecraftClient mc = MinecraftClient.getInstance();

        // Get player's facing direction
        Direction playerFacing = mc.player.getHorizontalFacing();

        // Priority order: facing direction first, then adjacent sides, then diagonals
        // 1. First try the direction player is facing
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos facingPos = playerPos.offset(playerFacing);
        if (isValidPlacePosition(facingPos)) {
            return facingPos;
        }

        // 2. Try adjacent horizontal directions (not diagonal)
        Direction[] adjacentDirections = {
            playerFacing.rotateYClockwise(),
            playerFacing.rotateYCounterclockwise(),
            playerFacing.getOpposite()
        };

        for (Direction dir : adjacentDirections) {
            BlockPos testPos = playerPos.offset(dir);
            if (isValidPlacePosition(testPos)) {

                return testPos;
            }
        }

        // 3. Try above and below current position
        for (int y = 1; y >= -1; y -= 2) { // +1 then -1
            BlockPos testPos = playerPos.add(0, y, 0);
            if (isValidPlacePosition(testPos)) {

                return testPos;
            }
        }

        // 4. Finally try diagonal positions if no direct adjacent positions work
        for (int distance = 1; distance <= 3; distance++) {
            for (int x = -distance; x <= distance; x++) {
                for (int z = -distance; z <= distance; z++) {
                    // Skip positions we already checked (direct adjacent)
                    if ((Math.abs(x) == 1 && z == 0) || (x == 0 && Math.abs(z) == 1) || (x == 0 && z == 0)) {
                        continue;
                    }

                    // Only check positions at the current distance boundary
                    if (Math.abs(x) == distance || Math.abs(z) == distance) {
                        for (int y = -1; y <= 1; y++) {
                            BlockPos testPos = playerPos.add(x, y, z);
                            if (isValidPlacePosition(testPos)) {

                                return testPos;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }


    public boolean isValidPlacePosition(BlockPos pos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.world.getBlockState(pos).isAir() &&
            BlockUtils.canPlace(pos) &&
            !mc.world.getBlockState(pos.down()).isAir();
    }


    public static boolean isInEnd() {
        return PlayerUtils.getDimension().equals(Dimension.End);
    }

    public static boolean isInNether() {
        return PlayerUtils.getDimension().equals(Dimension.Nether);
    }

    public static boolean isInOverworld() {
        return PlayerUtils.getDimension().equals(Dimension.Overworld);
    }



}
