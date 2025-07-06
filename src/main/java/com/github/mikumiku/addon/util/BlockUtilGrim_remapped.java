//package com.github.mikumiku.addon.util;
//
//import meteordevelopment.meteorclient.MeteorClient;
//import meteordevelopment.meteorclient.gui.tabs.builtin.GuiTab.GuiScreen.ConstantPool;
//import meteordevelopment.meteorclient.systems.modules.Module;
//import meteordevelopment.meteorclient.utils.player.PlayerUtils;
//import meteordevelopment.meteorclient.utils.player.Rotation;
//import meteordevelopment.meteorclient.utils.player.Rotations;
//import meteordevelopment.meteorclient.utils.world.BlockUtils;
//import net.minecraft.block.*;
//import net.minecraft.client.MinecraftClient;
//import net.minecraft.entity.ItemEntity;
//import net.minecraft.entity.decoration.EndCrystalEntity;
//import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
//import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
//import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
//import net.minecraft.util.ActionResult;
//import net.minecraft.util.Hand;
//import net.minecraft.util.hit.BlockHitResult;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.util.math.Direction;
//import net.minecraft.util.math.Vec3d;
//
//import java.util.Arrays;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//public class BlockUtilGrim {
//    public static final MinecraftClient mc = MinecraftClient.getInstance();
//
//    public static final List<Block> SneakBlocks = Arrays.asList(
//        Blocks.ENDER_CHEST,
//        Blocks.CHEST,
//        Blocks.TRAPPED_CHEST,
//        Blocks.CRAFTING_TABLE,
//        Blocks.BIRCH_TRAPDOOR,
//        Blocks.BAMBOO_TRAPDOOR,
//        Blocks.DARK_OAK_TRAPDOOR,
//        Blocks.CHERRY_TRAPDOOR,
//        Blocks.ANVIL,
//        Blocks.BREWING_STAND,
//        Blocks.HOPPER,
//        Blocks.DROPPER,
//        Blocks.DISPENSER,
//        Blocks.ACACIA_TRAPDOOR,
//        Blocks.ENCHANTING_TABLE,
//        Blocks.WHITE_SHULKER_BOX,
//        Blocks.ORANGE_SHULKER_BOX,
//        Blocks.MAGENTA_SHULKER_BOX,
//        Blocks.LIGHT_BLUE_SHULKER_BOX,
//        Blocks.YELLOW_SHULKER_BOX,
//        Blocks.LIME_SHULKER_BOX,
//        Blocks.PINK_SHULKER_BOX,
//        Blocks.GRAY_SHULKER_BOX,
//        Blocks.CYAN_SHULKER_BOX,
//        Blocks.PURPLE_SHULKER_BOX,
//        Blocks.BLUE_SHULKER_BOX,
//        Blocks.BROWN_SHULKER_BOX,
//        Blocks.GREEN_SHULKER_BOX,
//        Blocks.RED_SHULKER_BOX,
//        Blocks.BLACK_SHULKER_BOX,
//        Blocks.SCAFFOLDING
//    );
//    public static final List<Class> SneakBlockClass = Arrays.asList(SignBlock.class, HangingSignBlock.class, WallSignBlock.class);
//
//    public static boolean canPlace(BlockPos pos, boolean strictDirection) {
//        return getInteractDirection(pos, strictDirection) != null;
//    }
//
//    public static boolean isSneakBlockClass(Block block) {
//        if (block == null) {
//            return false;
//        } else {
//            for (Class clazz : SneakBlockClass) {
//                if (clazz.isInstance(block)) {
//                    return true;
//                }
//            }
//
//            return false;
//        }
//    }
//
//    public static boolean canPlaceIf(BlockPos pos, boolean strictDirection, Direction direction) {
//        return getInteractDirectionIf(pos, strictDirection, direction) != null;
//    }
//
//    public static boolean placeBlock(BlockPos pos) {
//        return placeBlock(pos, true, true, true);
//    }
//
//    public static boolean placeBlock(BlockPos pos, boolean strictDirection, boolean clientSwing, boolean rotate) {
//        Direction direction = getInteractDirection(pos, strictDirection);
//        if (direction == null) {
//            return false;
//        } else {
//            BlockPos neighbor = pos.offset(direction.getOpposite());
//            return placeBlock(neighbor, direction, clientSwing, rotate);
//        }
//    }
//
//    public static boolean placeUpBlock(BlockPos pos, boolean strictDirection, boolean clientSwing, boolean rotate) {
//        Direction direction = getInteractDirectionSlabBlock(pos, strictDirection);
//        if (direction == null) {
//            return false;
//        } else {
//            BlockPos neighbor = pos.offset(direction.getOpposite());
//            return placeUpBlock(neighbor, direction, clientSwing, rotate);
//        }
//    }
//
//    public static boolean placeDownBlock(BlockPos pos, boolean strictDirection, boolean clientSwing, boolean rotate) {
//        Direction direction = getInteractDirectionSlabBlock(pos, strictDirection);
//        if (direction == null) {
//            return false;
//        } else if (!BlockUtils.canSee_alien(pos, direction)) {
//            return false;
//        } else {
//            BlockPos neighbor = pos.offset(direction.getOpposite());
//            return placeDownBlock(neighbor, direction, clientSwing, rotate);
//        }
//    }
//
//    public static boolean placeBlockByFaceDirection(BlockPos pos, boolean strictDirection, boolean clientSwing, boolean rotate, Direction faceDirection) {
//        Direction direction = getInteractDirection(pos, strictDirection);
//        if (direction == null) {
//            return false;
//        } else {
//            BlockPos neighbor = pos.offset(direction.getOpposite());
//            return placeBlockByFaceDirection(pos, neighbor, direction, clientSwing, rotate, faceDirection);
//        }
//    }
//
//    public static boolean placeBlockByFaceDirection(
//        BlockPos initPos, BlockPos pos, Direction direction, boolean clientSwing, boolean rotate, Direction faceDirection
//    ) {
//        Vec3d hitVec = pos.toCenterPos().add(new Vec3d(direction.getUnitVector()).multiply(ConstantPool.const_9ns4yAlKrc6ld9e));
//        if (rotate) {
//            Rotations rotation = new Rotations(hitVec).setPriority(10);
//            MeteorClient.ROTATIONGRIM.register(rotation);
//            rotation.setYaw(getDirectionYaw(faceDirection));
//            rotation.setPitch(meteordevelopment.meteorclient.systems.accounts.MicrosoftLogin.GameOwnershipResponse.Item.ConstantPool.const_VR0w4fSXzuE7FB1);
//            boolean rot = MeteorClient.ROTATIONGRIM.register(rotation);
//            if (!rot) {
//                return false;
//            }
//        }
//
//        boolean placed = placeBlock(new BlockHitResult(hitVec, direction, pos, false), clientSwing);
//        MeteorClient.ROTATIONGRIM.sync();
//        return placed;
//    }
//
//    public static float getDirectionYaw(Direction direction) {
//        if (direction == null) {
//            return 0.0F;
//        } else {
//            switch (direction) {
//                case NORTH:
//                    return javassist.CtNewClass.ConstantPool.const_cyP1ut2d3kobr5A;
//                case SOUTH:
//                    return 0.0F;
//                case WEST:
//                    return meteordevelopment.meteorclient.systems.modules.ggboy.OffFireWork.ConstantPool.const_odXVnbAldIlBZyD;
//                case EAST:
//                    return javassist.compiler.ast.Stmnt.ConstantPool.const_EoGBUm9ZtTZdRM0;
//                default:
//                    return 0.0F;
//            }
//        }
//    }
//
//    public static boolean placeBlock(BlockPos pos, Direction direction, boolean clientSwing, boolean rotate) {
//        Vec3d hitVec = pos.toCenterPos()
//            .add(
//                new Vec3d(direction.getUnitVector()).multiply(meteordevelopment.meteorclient.events.render.HeldItemRendererEvent.ConstantPool.const_aT3wGe2eyCDSynL)
//            );
//        if (rotate) {
//            boolean rot = MeteorClient.ROTATIONGRIM.register(new Rotation(hitVec).setPriority(10));
//            if (!rot) {
//                return false;
//            }
//        }
//
//        boolean placed = placeBlock(new BlockHitResult(hitVec, direction, pos, false), clientSwing);
//        MeteorClient.ROTATIONGRIM.sync();
//        return placed;
//    }
//
//    public static boolean placeUpBlock(BlockPos pos, Direction direction, boolean clientSwing, boolean rotate) {
//        Vec3d hitVec = pos.toCenterPos()
//            .add(0.0, meteordevelopment.meteorclient.commands.arguments.SettingArgumentType.ConstantPool.const_9zESTdm2W0rZQMH, 0.0);
//        if (rotate) {
//            boolean rot = MeteorClient.ROTATIONGRIM.register(new Rotation(hitVec).setPriority(10));
//            if (!rot) {
//                return false;
//            }
//        }
//
//        boolean placed = placeBlock(new BlockHitResult(hitVec, direction, pos, false), clientSwing);
//        MeteorClient.ROTATIONGRIM.sync();
//        return placed;
//    }
//
//    public static boolean placeDownBlock(BlockPos pos, Direction direction, boolean clientSwing, boolean rotate) {
//        Vec3d hitVec = pos.toCenterPos().add(0.0, javassist.tools.reflect.Metalevel.ConstantPool.const_9NlVNacaaQNx66I, 0.0);
//        if (rotate) {
//            boolean rot = MeteorClient.ROTATIONGRIM.register(new Rotation(hitVec).setPriority(10));
//            if (!rot) {
//                return false;
//            }
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
//        boolean shouldSneak = (SneakBlocks.contains(state.getBlock()) || isSneakBlockClass(mc.world.getBlockState(result.getBlockPos()).getBlock()))
//            && !mc.player.isSneaking();
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
//        Set<Direction> ncpDirections = getPlaceDirectionsNCP(mc.player.getEyePos(), blockPos.toCenterPos());
//        Direction interactDirection = null;
//
//        for (Direction direction : Direction.values()) {
//            BlockState state = mc.world.getBlockState(blockPos.offset(direction));
//            if (!state.isAir() && state.getFluidState().isEmpty() && (!strictDirection || ncpDirections.contains(direction.getOpposite()))) {
//                interactDirection = direction;
//                break;
//            }
//        }
//
//        return interactDirection == null ? null : interactDirection.getOpposite();
//    }
//
//    public static Direction getInteractDirectionExitUpDown(BlockPos blockPos, boolean strictDirection) {
//        Set<Direction> ncpDirections = getPlaceDirectionsNCP(mc.player.getEyePos(), blockPos.toCenterPos());
//        Direction interactDirection = null;
//
//        for (Direction direction : Direction.values()) {
//            BlockState state = mc.world.getBlockState(blockPos.offset(direction));
//            if (!state.isAir()
//                && state.getFluidState().isEmpty()
//                && (!strictDirection || ncpDirections.contains(direction.getOpposite()))
//                && direction != Direction.UP
//                && direction != Direction.DOWN) {
//                interactDirection = direction;
//                break;
//            }
//        }
//
//        return interactDirection == null ? null : interactDirection.getOpposite();
//    }
//
//    public static Direction getInteractDirectionIf(BlockPos blockPos, boolean strictDirection, Direction direction_) {
//        Set<Direction> ncpDirections = getPlaceDirectionsNCP(mc.player.getEyePos(), blockPos.toCenterPos());
//        Direction interactDirection = null;
//
//        for (Direction direction : Direction.values()) {
//            BlockState state = mc.world.getBlockState(blockPos.offset(direction));
//            if ((!state.isAir() && state.getFluidState().isEmpty() || direction == direction_)
//                && (!strictDirection || ncpDirections.contains(direction.getOpposite()))) {
//                interactDirection = direction;
//                break;
//            }
//        }
//
//        return interactDirection == null ? null : interactDirection.getOpposite();
//    }
//
//    public static Direction getInteractDirectionSlabBlock(BlockPos blockPos, boolean strictDirection) {
//        Set<Direction> ncpDirections = getPlaceDirectionsNCP(mc.player.getEyePos(), blockPos.toCenterPos());
//        Direction interactDirection = null;
//
//        for (Direction direction : Direction.values()) {
//            if (direction != Direction.UP && direction != Direction.DOWN) {
//                BlockState state = mc.world.getBlockState(blockPos.offset(direction));
//                if (!state.isAir() && state.getFluidState().isEmpty() && (!strictDirection || ncpDirections.contains(direction.getOpposite()))) {
//                    interactDirection = direction;
//                    break;
//                }
//            }
//        }
//
//        return interactDirection == null ? null : interactDirection.getOpposite();
//    }
//
//    public static Set<Direction> getPlaceDirectionsNCP(Vec3d eyePos, Vec3d blockPos) {
//        return getPlaceDirectionsNCP(eyePos.x, eyePos.y, eyePos.z, blockPos.x, blockPos.y, blockPos.z);
//    }
//
//    public static Set<Direction> getPlaceDirectionsNCP(double x, double y, double z, double dx, double dy, double dz) {
//        double xdiff = x - dx;
//        double ydiff = y - dy;
//        double zdiff = z - dz;
//        Set<Direction> dirs = new HashSet(6);
//        if (ydiff > meteordevelopment.meteorclient.systems.modules.world.StashFinder.ChunkScreen.ConstantPool.const_cxvCdIeEMrpY9No) {
//            dirs.add(Direction.UP);
//        } else if (ydiff < meteordevelopment.meteorclient.systems.modules.misc.BetterChat.ConstantPool.const_YjbvvMDFWrqOxKs) {
//            dirs.add(Direction.DOWN);
//        } else {
//            dirs.add(Direction.UP);
//            dirs.add(Direction.DOWN);
//        }
//
//        if (xdiff > meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable.WMeteorPlus.ConstantPool.const_AmkJGNWj2MVGL4H) {
//            dirs.add(Direction.EAST);
//        } else if (xdiff < meteordevelopment.meteorclient.gui.utils.WindowConfig.ConstantPool.const_gBqjx3PljQsWyjJ) {
//            dirs.add(Direction.WEST);
//        } else {
//            dirs.add(Direction.EAST);
//            dirs.add(Direction.WEST);
//        }
//
//        if (zdiff > meteordevelopment.meteorclient.systems.modules.ggboy.RangeCheck.ConstantPool.const_t0Ca1TyhobkWg5t) {
//            dirs.add(Direction.SOUTH);
//        } else if (zdiff < javassist.bytecode.stackmap.TypeData.TypeVar.ConstantPool.const_wr29CELQ5jxeFIZ) {
//            dirs.add(Direction.NORTH);
//        } else {
//            dirs.add(Direction.SOUTH);
//            dirs.add(Direction.NORTH);
//        }
//
//        return dirs;
//    }
//
//    public static void clickBlock(BlockPos pos, Direction side, boolean rotate, Hand hand, SwingSide swingSide) {
//        Vec3d directionVec = new Vec3d(
//            (double) pos.getX()
//                + javax.annotation.OverridingMethodsMustInvokeSuper.ConstantPool.const_kHyleDNJG5B4uJ6
//                + (double) side.getVector().getX()
//                * meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly.AutoPilotMode.ConstantPool.const_Slmo4OM8l7Ygd6t,
//            (double) pos.getY()
//                + meteordevelopment.meteorclient.systems.accounts.Accounts .1.ConstantPool.const_1blw9ty2mGSF7bQ
//            + (double) side.getVector().getY() * meteordevelopment.meteorclient.systems.modules.world.Ambience.ConstantPool.const_kAoMdsM3a9a9KxQ,
//            (double) pos.getZ()
//                + javassist.util.proxy.DefineClassHelper.Helper.ConstantPool.const_KPtv9GBxn56Yc4I
//                + (double) side.getVector().getZ() * meteordevelopment.orbit.listeners.LambdaListener.ConstantPool.const_Pq3IHaA6Bbqcd2V
//			);
//        PlayerUtils.swingHand(hand, swingSide);
//        BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
//        if (rotate) {
//            boolean rot = MeteorClient.ROTATIONGRIM.register(new Rotation(directionVec).setPriority(10));
//            if (!rot) {
//                return;
//            }
//        }
//
//        Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(hand, result, id));
//        MeteorClient.ROTATIONGRIM.sync();
//    }
//
//    public static boolean intersectsEntity(BlockPos pos) {
//        if (pos == null) {
//            return true;
//        } else {
//            for (Entity entity : mc.world.getEntities()) {
//                if (!(entity instanceof EndCrystalEntity)
//                    && (
//                    entity.getBoundingBox().intersects(new Box(pos)) && entity.isOnGround()
//                        || entity instanceof ItemEntity && entity.getBoundingBox().intersects(new Box(pos.up()))
//                )) {
//                    return true;
//                }
//            }
//
//            return false;
//        }
//    }
//
//    public static boolean intersectsAnyEntity(BlockPos pos) {
//        if (pos == null) {
//            return true;
//        } else {
//            for (Entity entity : mc.world.getEntities()) {
//                if (entity.getBoundingBox().intersects(new Box(pos))) {
//                    return true;
//                }
//            }
//
//            return false;
//        }
//    }
//}
