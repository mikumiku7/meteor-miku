package com.github.mikumiku.addon.util;

import com.github.mikumiku.addon.BaseModule;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Baritone 工具类，提供方块放置、交互和实体检测等功能
 *
 * 通常
 * 放置范围最大为5
 * 挖掘范围最大为6
 *
 * @author MikuMiku
 */
public class BaritoneUtil {
    /**
     * Minecraft 客户端实例
     */
    public static MinecraftClient mc = MinecraftClient.getInstance();

    /**
     * 需要潜行才能交互的方块列表
     */
    public static final List<Block> SneakBlocks = Arrays.asList(
        Blocks.ENDER_CHEST,
        Blocks.CHEST,
        Blocks.TRAPPED_CHEST,
        Blocks.CRAFTING_TABLE,
        Blocks.BIRCH_TRAPDOOR,
        Blocks.BAMBOO_TRAPDOOR,
        Blocks.DARK_OAK_TRAPDOOR,
        Blocks.CHERRY_TRAPDOOR,
        Blocks.ANVIL,
        Blocks.BREWING_STAND,
        Blocks.HOPPER,
        Blocks.DROPPER,
        Blocks.DISPENSER,
        Blocks.ACACIA_TRAPDOOR,
        Blocks.ENCHANTING_TABLE,
        Blocks.WHITE_SHULKER_BOX,
        Blocks.ORANGE_SHULKER_BOX,
        Blocks.MAGENTA_SHULKER_BOX,
        Blocks.LIGHT_BLUE_SHULKER_BOX,
        Blocks.YELLOW_SHULKER_BOX,
        Blocks.LIME_SHULKER_BOX,
        Blocks.PINK_SHULKER_BOX,
        Blocks.GRAY_SHULKER_BOX,
        Blocks.CYAN_SHULKER_BOX,
        Blocks.PURPLE_SHULKER_BOX,
        Blocks.BLUE_SHULKER_BOX,
        Blocks.BROWN_SHULKER_BOX,
        Blocks.GREEN_SHULKER_BOX,
        Blocks.RED_SHULKER_BOX,
        Blocks.BLACK_SHULKER_BOX,
        Blocks.SCAFFOLDING
    );

    /**
     * 需要潜行才能交互的方块类列表
     */
    public static final List<Class> SneakBlockClass = Arrays.asList(SignBlock.class, HangingSignBlock.class, WallSignBlock.class);

    /**
     * 检查指定位置是否可以放置方块
     *
     * @param pos 要检查的方块位置
     * @return 如果可以放置方块则返回 true，否则返回 false
     */
    public static boolean canPlace(BlockPos pos) {
        return getInteractDirection(pos, true) != null;
    }

    /**
     * 检查指定位置是否可以放置方块，支持严格方向检查
     *
     * @param pos             要检查的方块位置
     * @param strictDirection 是否启用严格方向检查
     * @return 如果可以放置方块则返回 true，否则返回 false
     */
    public static boolean canPlace(BlockPos pos, boolean strictDirection) {
        return getInteractDirection(pos, strictDirection) != null;
    }

    /**
     * 检查指定方块是否属于需要潜行才能交互的方块类型
     *
     * @param block 要检查的方块
     * @return 如果是需要潜行的方块类型则返回 true，否则返回 false
     */
    public static boolean isSneakBlockClass(Block block) {
        if (block == null) {
            return false;
        } else {
            for (Class clazz : SneakBlockClass) {
                if (clazz.isInstance(block)) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * 检查在指定条件下是否可以放置方块
     *
     * @param pos             要检查的方块位置
     * @param strictDirection 是否启用严格方向检查
     * @param direction       指定的方向
     * @return 如果满足条件可以放置方块则返回 true，否则返回 false
     */
    public static boolean canPlaceIf(BlockPos pos, boolean strictDirection, Direction direction) {
        return getInteractDirectionIf(pos, strictDirection, direction) != null;
    }

    /**
     * 在指定位置放置方块（使用默认参数）
     *
     * @param pos 要放置方块的位置
     * @return 如果成功放置方块则返回 true，否则返回 false
     */
    public static boolean placeBlock(BlockPos pos) {
        return placeBlock(pos, true, true, true);
    }

    /**
     * 在指定位置放置方块，支持自定义参数
     *
     * @param pos             要放置方块的位置
     * @param strictDirection 是否启用严格方向检查
     * @param clientSwing     是否在客户端显示挥手动画
     * @param rotate          是否自动旋转视角
     * @return 如果成功放置方块则返回 true，否则返回 false
     */
    public static boolean placeBlock(BlockPos pos, boolean strictDirection, boolean clientSwing, boolean rotate) {
        Direction direction = getInteractDirection(pos, strictDirection);
        if (direction == null) {
            return false;
        } else {
            BlockPos neighbor = pos.offset(direction.getOpposite());
            return placeBlock(neighbor, direction, clientSwing, rotate);
        }
    }

    /**
     * 在指定位置向上放置方块（适用于台阶等方块）
     *
     * @param pos             要放置方块的位置
     * @param strictDirection 是否启用严格方向检查
     * @param clientSwing     是否在客户端显示挥手动画
     * @param rotate          是否自动旋转视角
     * @return 如果成功放置方块则返回 true，否则返回 false
     */
    public static boolean placeUpBlock(BlockPos pos, boolean strictDirection, boolean clientSwing, boolean rotate) {
        Direction direction = getInteractDirectionSlabBlock(pos, strictDirection);
        if (direction == null) {
            return false;
        } else {
            BlockPos neighbor = pos.offset(direction.getOpposite());
            return placeUpBlock(neighbor, direction, clientSwing, rotate);
        }
    }

    /**
     * 在指定位置向下放置方块（适用于台阶等方块）
     *
     * @param pos             要放置方块的位置
     * @param strictDirection 是否启用严格方向检查
     * @param clientSwing     是否在客户端显示挥手动画
     * @param rotate          是否自动旋转视角
     * @return 如果成功放置方块则返回 true，否则返回 false
     */
    public static boolean placeDownBlock(BlockPos pos, boolean strictDirection, boolean clientSwing, boolean rotate) {
        Direction direction = getInteractDirectionSlabBlock(pos, strictDirection);
        if (direction == null) {
            return false;
        } else if (!canSeeBlockFace(pos, direction)) {
            return false;
        } else {
            BlockPos neighbor = pos.offset(direction.getOpposite());
            return placeDownBlock(neighbor, direction, clientSwing, rotate);
        }
    }

    /**
     * 检查是否能看到方块的指定面（射线检测）
     *
     * @param pos  方块位置
     * @param side 要检查的方块面方向
     * @return 如果能看到指定面则返回 true，否则返回 false
     */
    public static boolean canSeeBlockFace(BlockPos pos, Direction side) {
        if (side == null) {
            return false;
        } else {
            Vec3d testVec = pos.toCenterPos()
                .add(
                    side.getVector().getX() * 0.5,
                    side.getVector().getY() * 0.5,
                    side.getVector().getZ() * 0.5
                );
            HitResult result = mc
                .world
                .raycast(new RaycastContext(getEyesPos(), testVec,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, MeteorClient.mc.player));
            return result == null || result.getType() == HitResult.Type.MISS;
        }
    }

    /**
     * 获取玩家眼部位置的坐标
     *
     * @return 玩家眼部位置的 Vec3d 坐标
     */
    public static Vec3d getEyesPos() {
        return mc.player.getEyePos();
    }

    /**
     * 按指定朝向放置方块
     *
     * @param pos             要放置方块的位置
     * @param strictDirection 是否启用严格方向检查
     * @param clientSwing     是否在客户端显示挥手动画
     * @param rotate          是否自动旋转视角
     * @param faceDirection   方块的朝向
     * @return 如果成功放置方块则返回 true，否则返回 false
     */
    public static boolean placeBlockByFaceDirection(BlockPos pos, boolean strictDirection, boolean clientSwing, boolean rotate, Direction faceDirection) {
        Direction direction = getInteractDirection(pos, strictDirection);
        if (direction == null) {
            return false;
        } else {
            BlockPos neighbor = pos.offset(direction.getOpposite());
            return placeBlockByFaceDirection(pos, neighbor, direction, clientSwing, rotate, faceDirection);
        }
    }

    /**
     * 按指定朝向放置方块（详细版本）
     *
     * @param initPos       初始位置
     * @param pos           目标位置
     * @param direction     放置方向
     * @param clientSwing   是否在客户端显示挥手动画
     * @param rotate        是否自动旋转视角
     * @param faceDirection 方块的朝向
     * @return 如果成功放置方块则返回 true，否则返回 false
     */
    public static boolean placeBlockByFaceDirection(
        BlockPos initPos, BlockPos pos, Direction direction, boolean clientSwing, boolean rotate, Direction faceDirection
    ) {
        Vec3d hitVec = pos.toCenterPos().add(new Vec3d(direction.getUnitVector()).multiply(0.5));
        if (rotate) {
            Rotation rotation = new Rotation(hitVec).setPriority(10);
            RotationManager.getInstance().register(rotation);
            rotation.setYaw(getDirectionYaw(faceDirection));
            rotation.setPitch(5.0F);
            boolean rot = RotationManager.getInstance().register(rotation);
            if (!rot) {
                return false;
            }
        }

        boolean placed = placeBlock(new BlockHitResult(hitVec, direction, pos, false), clientSwing);
        RotationManager.getInstance().sync();
        return placed;
    }

    /**
     * 根据方向获取对应的偏航角度
     *
     * @param direction 方向
     * @return 对应的偏航角度（度）
     */
    public static float getDirectionYaw(Direction direction) {
        if (direction == null) {
            return 0.0F;
        } else {
            switch (direction) {
                case NORTH:
                    return 180.0F;
                case SOUTH:
                    return 0.0F;
                case WEST:
                    return 90.0F;
                case EAST:
                    return -90.0F;
                default:
                    return 0.0F;
            }
        }
    }

    /**
     * 在指定位置和方向放置方块
     *
     * @param pos         方块位置
     * @param direction   放置方向
     * @param clientSwing 是否在客户端显示挥手动画
     * @param rotate      是否自动旋转视角
     * @return 如果成功放置方块则返回 true，否则返回 false
     */
    public static boolean placeBlock(BlockPos pos, Direction direction, boolean clientSwing, boolean rotate) {
        Vec3d hitVec = pos.toCenterPos().add(new Vec3d(direction.getUnitVector()).multiply(0.5));
        if (rotate) {
            boolean rot = RotationManager.getInstance().register(new Rotation(hitVec).setPriority(10));
            if (!rot) {
                return false;
            }
        }

        boolean placed = placeBlock(new BlockHitResult(hitVec, direction, pos, false), clientSwing);
        RotationManager.getInstance().sync();
        return placed;
    }

    /**
     * 在指定位置向上放置方块（台阶等）
     *
     * @param pos         方块位置
     * @param direction   放置方向
     * @param clientSwing 是否在客户端显示挥手动画
     * @param rotate      是否自动旋转视角
     * @return 如果成功放置方块则返回 true，否则返回 false
     */
    public static boolean placeUpBlock(BlockPos pos, Direction direction, boolean clientSwing, boolean rotate) {
        Vec3d hitVec = pos.toCenterPos().add(0.0, 0.3, 0.0);
        if (rotate) {
            boolean rot = RotationManager.getInstance().register(new Rotation(hitVec).setPriority(10));
            if (!rot) {
                return false;
            }
        }

        boolean placed = placeBlock(new BlockHitResult(hitVec, direction, pos, false), clientSwing);
        RotationManager.getInstance().sync();
        return placed;
    }

    /**
     * 在指定位置向下放置方块（台阶等）
     *
     * @param pos         方块位置
     * @param direction   放置方向
     * @param clientSwing 是否在客户端显示挥手动画
     * @param rotate      是否自动旋转视角
     * @return 如果成功放置方块则返回 true，否则返回 false
     */
    public static boolean placeDownBlock(BlockPos pos, Direction direction, boolean clientSwing, boolean rotate) {
        Vec3d hitVec = pos.toCenterPos().add(0.0, -0.2, 0.0);
        if (rotate) {
            boolean rot = RotationManager.getInstance().register(new Rotation(hitVec).setPriority(10));
            if (!rot) {
                return false;
            }
        }

        boolean placed = placeBlock(new BlockHitResult(hitVec, direction, pos, false), clientSwing);
        RotationManager.getInstance().sync();
        return placed;
    }

    /**
     * 使用方块命中结果放置方块
     *
     * @param hitResult   方块命中结果
     * @param clientSwing 是否在客户端显示挥手动画
     * @return 如果成功放置方块则返回 true，否则返回 false
     */
    public static boolean placeBlock(BlockHitResult hitResult, boolean clientSwing) {
        return placeBlockImmediately(hitResult, clientSwing);
    }

    /**
     * 立即放置方块，处理潜行和挥手逻辑
     *
     * @param result      方块命中结果
     * @param clientSwing 是否在客户端显示挥手动画
     * @return 如果成功放置方块则返回 true，否则返回 false
     */
    public static boolean placeBlockImmediately(BlockHitResult result, boolean clientSwing) {
        BlockState state = mc.world.getBlockState(result.getBlockPos());
        boolean shouldSneak = (SneakBlocks.contains(state.getBlock()) || isSneakBlockClass(mc.world.getBlockState(result.getBlockPos()).getBlock()))
            && !mc.player.isSneaking();
        if (shouldSneak) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.PRESS_SHIFT_KEY));
        }

        ActionResult actionResult = placeBlockInternally(result);
        if (actionResult.isAccepted()) {
            if (clientSwing) {
                mc.player.swingHand(Hand.MAIN_HAND);
            } else {
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
        }

        if (shouldSneak) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.RELEASE_SHIFT_KEY));
        }

        return actionResult.isAccepted();
    }

    /**
     * 内部方块放置方法
     *
     * @param hitResult 方块命中结果
     * @return 交互结果
     */
    private static ActionResult placeBlockInternally(BlockHitResult hitResult) {
        return mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
    }

    /**
     * 获取可以交互的方向
     *
     * @param blockPos        方块位置
     * @param strictDirection 是否启用严格方向检查
     * @return 可以交互的方向，如果没有则返回 null
     */
    public static Direction getInteractDirection(BlockPos blockPos, boolean strictDirection) {
        Set<Direction> ncpDirections = getPlaceDirectionsNCP(mc.player.getEyePos(), blockPos.toCenterPos());
        Direction interactDirection = null;

        for (Direction direction : Direction.values()) {
            BlockState state = mc.world.getBlockState(blockPos.offset(direction));
            if (!state.isAir() && state.getFluidState().isEmpty() && (!strictDirection || ncpDirections.contains(direction.getOpposite()))) {
                interactDirection = direction;
                break;
            }
        }

        return interactDirection == null ? null : interactDirection.getOpposite();
    }

    /**
     * 获取可以交互的方向（排除上下方向）
     *
     * @param blockPos        方块位置
     * @param strictDirection 是否启用严格方向检查
     * @return 可以交互的方向（不包括上下），如果没有则返回 null
     */
    public static Direction getInteractDirectionExitUpDown(BlockPos blockPos, boolean strictDirection) {
        Set<Direction> ncpDirections = getPlaceDirectionsNCP(mc.player.getEyePos(), blockPos.toCenterPos());
        Direction interactDirection = null;

        for (Direction direction : Direction.values()) {
            BlockState state = mc.world.getBlockState(blockPos.offset(direction));
            if (!state.isAir()
                && state.getFluidState().isEmpty()
                && (!strictDirection || ncpDirections.contains(direction.getOpposite()))
                && direction != Direction.UP
                && direction != Direction.DOWN) {
                interactDirection = direction;
                break;
            }
        }

        return interactDirection == null ? null : interactDirection.getOpposite();
    }

    /**
     * 根据条件获取可以交互的方向
     *
     * @param blockPos        方块位置
     * @param strictDirection 是否启用严格方向检查
     * @param direction_      指定的方向条件
     * @return 满足条件的交互方向，如果没有则返回 null
     */
    public static Direction getInteractDirectionIf(BlockPos blockPos, boolean strictDirection, Direction direction_) {
        Set<Direction> ncpDirections = getPlaceDirectionsNCP(mc.player.getEyePos(), blockPos.toCenterPos());
        Direction interactDirection = null;

        for (Direction direction : Direction.values()) {
            BlockState state = mc.world.getBlockState(blockPos.offset(direction));
            if ((!state.isAir() && state.getFluidState().isEmpty() || direction == direction_)
                && (!strictDirection || ncpDirections.contains(direction.getOpposite()))) {
                interactDirection = direction;
                break;
            }
        }

        return interactDirection == null ? null : interactDirection.getOpposite();
    }

    /**
     * 获取台阶方块的交互方向（仅水平方向）
     *
     * @param blockPos        方块位置
     * @param strictDirection 是否启用严格方向检查
     * @return 可以交互的水平方向，如果没有则返回 null
     */
    public static Direction getInteractDirectionSlabBlock(BlockPos blockPos, boolean strictDirection) {
        Set<Direction> ncpDirections = getPlaceDirectionsNCP(mc.player.getEyePos(), blockPos.toCenterPos());
        Direction interactDirection = null;

        for (Direction direction : Direction.values()) {
            if (direction != Direction.UP && direction != Direction.DOWN) {
                BlockState state = mc.world.getBlockState(blockPos.offset(direction));
                if (!state.isAir() && state.getFluidState().isEmpty() && (!strictDirection || ncpDirections.contains(direction.getOpposite()))) {
                    interactDirection = direction;
                    break;
                }
            }
        }

        return interactDirection == null ? null : interactDirection.getOpposite();
    }

    /**
     * 获取 NCP（NoCheatPlus）兼容的放置方向集合
     *
     * @param eyePos   眼部位置
     * @param blockPos 方块位置
     * @return 可用的放置方向集合
     */
    public static Set<Direction> getPlaceDirectionsNCP(Vec3d eyePos, Vec3d blockPos) {
        return getPlaceDirectionsNCP(eyePos.x, eyePos.y, eyePos.z, blockPos.x, blockPos.y, blockPos.z);
    }

    /**
     * 根据坐标差计算 NCP 兼容的放置方向集合
     *
     * @param x  眼部 X 坐标
     * @param y  眼部 Y 坐标
     * @param z  眼部 Z 坐标
     * @param dx 目标 X 坐标
     * @param dy 目标 Y 坐标
     * @param dz 目标 Z 坐标
     * @return 可用的放置方向集合
     */
    public static Set<Direction> getPlaceDirectionsNCP(double x, double y, double z, double dx, double dy, double dz) {
        double xdiff = x - dx;
        double ydiff = y - dy;
        double zdiff = z - dz;
        Set<Direction> dirs = new HashSet<>(6);
        if (ydiff > 0.5) {
            dirs.add(Direction.UP);
        } else if (ydiff < -0.5) {
            dirs.add(Direction.DOWN);
        } else {
            dirs.add(Direction.UP);
            dirs.add(Direction.DOWN);
        }

        if (xdiff > 0.5) {
            dirs.add(Direction.EAST);
        } else if (xdiff < -0.5) {
            dirs.add(Direction.WEST);
        } else {
            dirs.add(Direction.EAST);
            dirs.add(Direction.WEST);
        }

        if (zdiff > 0.5) {
            dirs.add(Direction.SOUTH);
        } else if (zdiff < -0.5) {
            dirs.add(Direction.NORTH);
        } else {
            dirs.add(Direction.SOUTH);
            dirs.add(Direction.NORTH);
        }

        return dirs;
    }


    /**
     * 目的是在指定方向点击一个方块，可选是否旋转视角，支持设置挥手方向。
     * pos	BlockPos	要点击的方块位置（block 坐标）
     * side	Direction	点击方块的哪一侧（如 Direction.UP）
     * rotate	boolean	是否旋转视角对准点击位置
     * hand	Hand	使用哪只手点击（MAIN_HAND 或 OFF_HAND）
     * swingSide	SwingSide	玩家动画挥手方向（仅客户端视觉）
     * <p>
     * BaritoneUtil.clickBlock(this.plantPos.get(i), Direction.UP, true, Hand.MAIN_HAND, SwingSide.All);
     *
     * @param pos
     * @param side
     * @param rotate
     * @param hand
     * @param swingSide
     */
    public static void clickBlock(BlockPos pos, Direction side, boolean rotate, Hand hand, SwingSide swingSide) {
        Vec3d directionVec = new Vec3d(
            pos.getX()
                + 0.5
                + side.getVector().getX() * 0.5,
            pos.getY()
                + 0.5
                + side.getVector().getY() * 0.5,
            pos.getZ()
                + 0.5
                + side.getVector().getZ() * 0.5
        );
        swingHand(hand, swingSide);
        BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
        if (rotate) {
            boolean rot = RotationManager.getInstance().register(new Rotation(directionVec).setPriority(10));
            if (!rot) {
                return;
            }
        }

        BaseModule.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(hand, result, id));
        RotationManager.getInstance().sync();
    }

    /**
     * 挥手动作
     *
     * @param hand 使用的手（主手或副手）
     * @param side 挥手方式（全部、仅客户端、仅服务器）
     */
    public static void swingHand(Hand hand, SwingSide side) {
        switch (side) {
            case All:
                MeteorClient.mc.player.swingHand(hand);
                break;
            case Client:
                MeteorClient.mc.player.swingHand(hand, false);
                break;
            case Server:
                MeteorClient.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
        }
    }

    /**
     * 检查指定位置是否与实体相交（排除末影水晶）
     *
     * @param pos 要检查的方块位置
     * @return 如果与实体相交则返回 true，否则返回 false
     */
    public static boolean intersectsEntity(BlockPos pos) {
        if (pos == null) {
            return true;
        } else {
            for (Entity entity : mc.world.getEntities()) {
                if (!(entity instanceof EndCrystalEntity)
                    && (
                    entity.getBoundingBox().intersects(new Box(pos)) && entity.isOnGround()
                        || entity instanceof ItemEntity && entity.getBoundingBox().intersects(new Box(pos.up()))
                )) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * 检查指定位置是否与任何实体相交
     *
     * @param pos 要检查的方块位置
     * @return 如果与任何实体相交则返回 true，否则返回 false
     */
    public static boolean intersectsAnyEntity(BlockPos pos) {
        if (pos == null) {
            return true;
        } else {
            for (Entity entity : mc.world.getEntities()) {
                if (entity.getBoundingBox().intersects(new Box(pos))) {
                    return true;
                }
            }

            return false;
        }
    }

    public static Direction getPlaceDirection(BlockPos pos, boolean ignoreContainers) {
        if (pos == null) {
            return null;
        }
        Direction best = null;
        if (mc.world != null && mc.player != null) {

            double cDist = -1;
            for (Direction dir : Direction.values()) {

                // Doesn't place on top of max height
                if (pos.offset(dir).getY() >= 319) {
                    continue;
                }

                // Checks if block is an entity (chests, shulkers)
                if (ignoreContainers && mc.world.getBlockState(pos.offset(dir)).hasBlockEntity()) {
                    continue;
                }

                // Test if there is block in the side and if predicate is valid
                Block b = mc.world.getBlockState(pos.offset(dir)).getBlock();
                if (b instanceof AbstractFireBlock || b instanceof FluidBlock || b instanceof AirBlock) {
                    continue;
                }


                // Only accepts if closer than previous accepted direction
                double dist = PlayerUtils.distanceTo(pos.offset(dir));
                if (dist >= 0 && (cDist < 0 || dist < cDist)) {
                    best = dir;
                    cDist = dist;
                }
            }
        }
        return best;
    }

    public static Direction getPlaceOnDirection(BlockPos pos) {
        if (pos == null) {
            return null;
        }
        Direction best = null;
        if (mc.world != null && mc.player != null) {
            double cDist = -1;
            for (Direction dir : Direction.values()) {

                // Doesn't place on top of max height
                if (pos.offset(dir).getY() >= 319) {
                    continue;
                }

                // Test if there is block in the side and if predicate is valid
                Block b = mc.world.getBlockState(pos.offset(dir)).getBlock();
                if ( !(b instanceof AbstractFireBlock || b instanceof FluidBlock || b instanceof AirBlock)) {
                    continue;
                }

                // Only accepts if closer than last accepted direction
                double dist = mc.player.getEyePos().distanceTo(pos.offset(dir).toCenterPos());
                if (dist >= 0 && (cDist < 0 || dist < cDist)) {
                    best = dir;
                    cDist = dist;
                }
            }
        }
        return best;
    }
    /**
     * 挥手方式枚举
     */
    public enum SwingSide {
        /**
         * 全部（客户端和服务器）
         */
        All,
        /**
         * 仅客户端
         */
        Client,
        /**
         * 仅服务器
         */
        Server,
        /**
         * 无挥手
         */
        None;
    }

}
