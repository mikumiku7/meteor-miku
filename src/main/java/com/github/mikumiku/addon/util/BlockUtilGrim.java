//package com.github.mikumiku.addon.util;
//
//
//import meteordevelopment.meteorclient.MeteorClient;
//import meteordevelopment.meteorclient.utils.player.PlayerUtils;
//import meteordevelopment.meteorclient.utils.player.Rotations;
//import net.minecraft.block.*;
//import net.minecraft.client.MinecraftClient;
//import net.minecraft.entity.Entity;
//import net.minecraft.entity.ItemEntity;
//import net.minecraft.entity.EndCrystalEntity;
//import net.minecraft.util.ActionResult;
//import net.minecraft.util.Hand;
//import net.minecraft.util.hit.BlockHitResult;
//import net.minecraft.util.math.*;
//import net.minecraft.network.packet.c2s.play.*;
//import net.minecraft.state.property.Property;
//
//import java.util.Arrays;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//public class BlockUtilGrim {
//    private static final MinecraftClient mc = MinecraftClient.getInstance();
//
//    public static final List<Block> SneakBlocks = Arrays.asList(
//        Blocks.CHEST,
//        Blocks.ENDER_CHEST,
//        Blocks.TRAPPED_CHEST,
//        Blocks.SHULKER_BOX,
//        Blocks.BARREL,
//        Blocks.FURNACE,
//        Blocks.BLAST_FURNACE,
//        Blocks.SMOKER,
//        Blocks.CRAFTING_TABLE,
//        Blocks.ANVIL,
//        Blocks.CHIPPED_ANVIL,
//        Blocks.DAMAGED_ANVIL,
//        Blocks.ENCHANTING_TABLE,
//        Blocks.LECTERN
//        // 如 mappings 中还有其他 field_xxx，继续在此添加
//    );
//
//    public static final List<Class<?>> SneakBlockClass = Arrays.asList(
//        ShulkerBoxBlock.class,
//        LecternBlock.class,
//        AnvilBlock.class
//    );
//
//    public static boolean canPlace(BlockPos pos, boolean strictDirection) {
//        return getInteractDirection(pos, strictDirection) != null;
//    }
//
//    public static boolean isSneakBlockClass(Block block) {
//        if (block == null) return false;
//        for (Class<?> clazz : SneakBlockClass) {
//            if (clazz.isInstance(block)) return true;
//        }
//        return false;
//    }
//
//    public static boolean placeBlock(BlockPos pos) {
//        return placeBlock(pos, true, true, true);
//    }
//
//    public static boolean placeBlock(BlockPos pos, boolean strictDirection, boolean clientSwing, boolean rotate) {
//        Direction direction = getInteractDirection(pos, strictDirection);
//        if (direction == null) return false;
//
//        BlockPos neighbor = pos.offset(direction.getOpposite());
//        return placeBlock(neighbor, direction, clientSwing, rotate);
//    }
//
//    public static boolean placeBlock(BlockPos pos, Direction direction, boolean clientSwing, boolean rotate) {
//        Vec3d hitVec = Vec3d.ofCenter(pos).add(Vec3d.of(direction.getVector()).multiply(0.5));
//
//        if (rotate) {
//            boolean rot = MeteorClient.ROTATIONGRIM.register(new Rotation(hitVec).setPriority(10));
//            if (!rot) return false;
//        }
//
//        boolean placed = placeBlock(new BlockHitResult(hitVec, direction, pos, false), clientSwing);
//        MeteorClient.ROTATIONGRIM.sync();
//        return placed;
//    }
//
//    public static boolean placeBlock(BlockHitResult hitResult, boolean clientSwing) {
//        return placeBlockImmediately(hitResult, clientSwing);
//    }
//
//    public static boolean placeBlockImmediately(BlockHitResult result, boolean clientSwing) {
//        BlockState state = mc.world.getBlockState(result.getBlockPos());
//        boolean shouldSneak = (SneakBlocks.contains(state.getBlock()) || isSneakBlockClass(state.getBlock()))
//            && !mc.player.isSneaking();
//
//        if (shouldSneak) {
//            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
//        }
//
//        ActionResult actionResult = placeBlockInternally(result);
//        if (actionResult.isAccepted() && actionResult.shouldSwingHand()) {
//            if (clientSwing) {
//                mc.player.swingHand(Hand.MAIN_HAND);
//            } else {
//                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
//            }
//        }
//
//        if (shouldSneak) {
//            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
//        }
//
//        return actionResult.isAccepted();
//    }
//
//    private static ActionResult placeBlockInternally(BlockHitResult hitResult) {
//        return mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
//    }
//
//    public static Direction getInteractDirection(BlockPos blockPos, boolean strictDirection) {
//        Set<Direction> ncpDirections = getPlaceDirectionsNCP(mc.player.getEyePos(), Vec3d.ofCenter(blockPos));
//        for (Direction direction : Direction.values()) {
//            BlockState state = mc.world.getBlockState(blockPos.offset(direction));
//            if (!state.isAir() && state.getMaterial().isReplaceable()
//                && (!strictDirection || ncpDirections.contains(direction.getOpposite()))) {
//                return direction.getOpposite();
//            }
//        }
//        return null;
//    }
//
//    public static Set<Direction> getPlaceDirectionsNCP(Vec3d eyePos, Vec3d blockPos) {
//        double xdiff = eyePos.x - blockPos.x;
//        double ydiff = eyePos.y - blockPos.y;
//        double zdiff = eyePos.z - blockPos.z;
//
//        Set<Direction> dirs = new HashSet<>(6);
//
//        if (ydiff > 2.5) {
//            dirs.add(Direction.UP);
//        } else if (ydiff < -2.5) {
//            dirs.add(Direction.DOWN);
//        } else {
//            dirs.add(Direction.UP);
//            dirs.add(Direction.DOWN);
//        }
//
//        if (xdiff > 2.5) {
//            dirs.add(Direction.WEST);
//        } else if (xdiff < -2.5) {
//            dirs.add(Direction.EAST);
//        } else {
//            dirs.add(Direction.WEST);
//            dirs.add(Direction.EAST);
//        }
//
//        if (zdiff > 2.5) {
//            dirs.add(Direction.NORTH);
//        } else if (zdiff < -2.5) {
//            dirs.add(Direction.SOUTH);
//        } else {
//            dirs.add(Direction.NORTH);
//            dirs.add(Direction.SOUTH);
//        }
//
//        return dirs;
//    }
//
//    public static boolean intersectsEntity(BlockPos pos) {
//        if (pos == null) return true;
//        for (Entity entity : mc.world.getEntities()) {
//            if (!(entity instanceof ItemEntity)
//                && (entity.getBoundingBox().intersects(new Box(pos))
//                || entity instanceof EndCrystalEntity && entity.getBoundingBox().intersects(new Box(pos.up())))) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    public static boolean intersectsAnyEntity(BlockPos pos) {
//        if (pos == null) return true;
//        for (Entity entity : mc.world.getEntities()) {
//            if (entity.getBoundingBox().intersects(new Box(pos))) return true;
//        }
//        return false;
//    }
//}
