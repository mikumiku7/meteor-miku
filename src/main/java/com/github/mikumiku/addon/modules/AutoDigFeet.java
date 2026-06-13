package com.github.mikumiku.addon.modules;


import com.github.mikumiku.addon.BaseModule;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.CobwebBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class AutoDigFeet extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> burrow = sgGeneral.add(new BoolSetting.Builder()
        .name("钻地破坏")
        .description("破坏敌人钻地时的方块。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> face = sgGeneral.add(new BoolSetting.Builder()
        .name("面部破坏")
        .description("破坏敌人面部高度的方块。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> surround = sgGeneral.add(new BoolSetting.Builder()
        .name("围脚破坏")
        .description("破坏敌人周围的黑曜石。")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> down = sgGeneral.add(new BoolSetting.Builder()
        .name("脚下破坏")
        .description("破坏敌人脚下的方块。")
        .defaultValue(false)
        .build()
    );


    private final Setting<Boolean> lowVersion = sgGeneral.add(new BoolSetting.Builder()
        .name("1.12模式")
        .description("使用 1.12 版本的水晶放置规则。")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("目标范围")
        .description("搜索目标的范围。")
        .defaultValue(5.0)
        .min(1.0)
        .max(8.0)
        .sliderMax(8.0)
        .build()
    );
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("挖掘范围")
        .description("挖掘方块的最大范围。")
        .defaultValue(5.0)
        .min(1.0)
        .max(8.0)
        .sliderMax(8.0)
        .build()
    );

    private BlockPos breakPos = null;
    private BlockPos secondPos = null;

    public AutoDigFeet() {
        super(BaseModule.CATEGORY_MIKU_COMBAT, "挖脚", "自动破坏敌人周围的防护方块，包括钻地、面部和围城黑曜石。");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        PlayerEntity target = getClosestEnemy(targetRange.get());
        if (target == null) return;
        doBreak(target);
    }

    private void doBreak(PlayerEntity player) {
        BlockPos pos = player.getBlockPos();

        // Check if already mining nearby enemy blocks
        double[] yOffset = new double[]{-0.8, 0.5, 1.1};
        double[] xzOffset = new double[]{0.3, -0.3};

        for (PlayerEntity entity : getEnemies(targetRange.get())) {
            for (double y : yOffset) {
                for (double x : xzOffset) {
                    for (double z : xzOffset) {
                        BlockPos offsetPos = new BlockPos((int) (entity.getX() + x), (int) (entity.getY() + y), (int) (entity.getZ() + z));
                        if (canBreak(offsetPos) && offsetPos.equals(breakPos)) {
                            return;
                        }
                    }
                }
            }
        }

        // Mine burrow, face, or down blocks
        List<Double> yList = new ArrayList<>();
        if (down.get()) yList.add(-0.8);
        if (burrow.get()) yList.add(0.5);
        if (face.get()) yList.add(1.1);

        for (double y : yList) {
            for (double offset : xzOffset) {
                BlockPos offsetPos = new BlockPos((int) (player.getX() + offset), (int) (player.getY() + y), (int) (player.getZ() + offset));
                if (canBreak(offsetPos)) {
                    mine(offsetPos);
                    return;
                }
            }
        }

        for (double y : yList) {
            for (double offset : xzOffset) {
                for (double offset2 : xzOffset) {
                    BlockPos offsetPos = new BlockPos((int) (player.getX() + offset2), (int) (player.getY() + y), (int) (player.getZ() + offset));
                    if (canBreak(offsetPos)) {
                        mine(offsetPos);
                        return;
                    }
                }
            }
        }

        // Handle surround breaking
        if (surround.get()) {
            if (!lowVersion.get()) {
                // Check if can place crystal
                for (Direction dir : Direction.values()) {
                    if (dir == Direction.UP || dir == Direction.DOWN) continue;
                    BlockPos offsetPos = pos.offset(dir);

                    if (getDistance(offsetPos) > range.get()) continue;

                    if ((mc.world.isAir(offsetPos) || offsetPos.equals(breakPos)) && canPlaceCrystal(offsetPos, false)) {
                        return;
                    }
                }

                // Find blocks to mine
                ArrayList<BlockPos> list = new ArrayList<>();
                for (Direction dir : Direction.values()) {
                    if (dir == Direction.UP || dir == Direction.DOWN) continue;
                    BlockPos offsetPos = pos.offset(dir);

                    if (getDistance(offsetPos) > range.get()) continue;

                    if (canBreak(offsetPos) && canPlaceCrystal(offsetPos, true)) {
                        list.add(offsetPos);
                    }
                }

                if (!list.isEmpty()) {
                    BlockPos closest = list.stream()
                        .min(Comparator.comparingDouble(this::getDistance))
                        .orElse(null);
                    if (closest != null) mine(closest);
                } else {
                    list.clear();
                    for (Direction dir : Direction.values()) {
                        if (dir == Direction.UP || dir == Direction.DOWN) continue;
                        BlockPos offsetPos = pos.offset(dir);

                        if (getDistance(offsetPos) > range.get()) continue;

                        if (canBreak(offsetPos) && canPlaceCrystal(offsetPos, false)) {
                            list.add(offsetPos);
                        }
                    }

                    if (!list.isEmpty()) {
                        BlockPos closest = list.stream()
                            .min(Comparator.comparingDouble(this::getDistance))
                            .orElse(null);
                        if (closest != null) mine(closest);
                    }
                }
            } else {
                // 1.12 mode
                for (Direction dir : Direction.values()) {
                    if (dir == Direction.UP || dir == Direction.DOWN) continue;
                    BlockPos offsetPos = pos.offset(dir);

                    if (getDistance(offsetPos) > range.get()) continue;

                    if (mc.world.isAir(offsetPos) && mc.world.isAir(offsetPos.up()) && canPlaceCrystal(offsetPos, false)) {
                        return;
                    }
                }

                ArrayList<BlockPos> list = new ArrayList<>();
                for (Direction dir : Direction.values()) {
                    if (dir == Direction.UP || dir == Direction.DOWN) continue;
                    BlockPos offsetPos = pos.offset(dir);

                    if (getDistance(offsetPos) > range.get()) continue;

                    if (canCrystal(offsetPos)) {
                        list.add(offsetPos);
                    }
                }

                int max = 0;
                BlockPos minePos = null;
                for (BlockPos cPos : list) {
                    int air = getAir(cPos);
                    if (air >= max) {
                        max = air;
                        minePos = cPos;
                    }
                }

                if (minePos != null) doMine(minePos);
            }
        }

        // Fallback obsidian breaking
        if (breakPos == null && burrow.get()) {
            double[] xzOffset2 = new double[]{0, 0.3, -0.3};
            double[] yOffset2 = new double[]{0.5, 1.1};

            for (double y : yOffset2) {
                for (double offset : xzOffset2) {
                    BlockPos offsetPos = new BlockPos((int) (player.getX() + offset), (int) (player.getY() + y), (int) (player.getZ() + offset));
                    if (isObsidian(offsetPos)) {
                        mine(offsetPos);
                        return;
                    }
                }
            }

            for (double y : yOffset2) {
                for (double offset : xzOffset2) {
                    for (double offset2 : xzOffset2) {
                        BlockPos offsetPos = new BlockPos((int) (player.getX() + offset2), (int) (player.getY() + y), (int) (player.getZ() + offset));
                        if (isObsidian(offsetPos)) {
                            mine(offsetPos);
                            return;
                        }
                    }
                }
            }
        }
    }

    private void mine(BlockPos pos) {
        breakPos = pos;
        BlockUtils.breakBlock(pos, false);
    }

    private void doMine(BlockPos pos) {
        if (canBreak(pos)) {
            mine(pos);
        } else if (canBreak(pos.up())) {
            mine(pos.up());
        }
    }

    private boolean canCrystal(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        Block blockUp = mc.world.getBlockState(pos.up()).getBlock();

        if (isGodBlock(block) || block instanceof BedBlock || block instanceof CobwebBlock) {
            return false;
        }
        if (isGodBlock(blockUp) || blockUp instanceof BedBlock || blockUp instanceof CobwebBlock) {
            return false;
        }

        return canPlaceCrystal(pos, true);
    }

    private int getAir(BlockPos pos) {
        int value = 0;
        if (!canBreak(pos)) value++;
        if (!canBreak(pos.up())) value++;
        return value;
    }

    private boolean canPlaceCrystal(BlockPos pos, boolean block) {
        BlockPos obsPos = pos.down();
        BlockPos boost = obsPos.up();
        Block obsBlock = mc.world.getBlockState(obsPos).getBlock();

        boolean validBase = obsBlock == Blocks.BEDROCK || obsBlock == Blocks.OBSIDIAN || !block;
        boolean noEntity1 = !hasEntity(boost);
        boolean noEntity2 = !hasEntity(boost.up());
        boolean airCheck = !lowVersion.get() || mc.world.isAir(boost.up());

        return validBase && noEntity1 && noEntity2 && airCheck;
    }

    private boolean isObsidian(BlockPos pos) {
        if (getDistance(pos) > range.get()) return false;
        Block block = mc.world.getBlockState(pos).getBlock();
        return block == Blocks.OBSIDIAN || block == Blocks.ENDER_CHEST ||
            block == Blocks.NETHERITE_BLOCK || block == Blocks.RESPAWN_ANCHOR;
    }

    private boolean canBreak(BlockPos pos) {
        if (!isObsidian(pos)) return false;
        if (pos.equals(secondPos) && Set.of(
            Items.WOODEN_PICKAXE,
            Items.STONE_PICKAXE,
            Items.IRON_PICKAXE,
            Items.GOLDEN_PICKAXE,
            Items.DIAMOND_PICKAXE,
            Items.NETHERITE_PICKAXE
        ).contains(mc.player.getMainHandStack().getItem())) {

            return false;
        }
        return true;
    }

    private boolean isGodBlock(Block block) {
        return block == Blocks.BEDROCK || block == Blocks.COMMAND_BLOCK ||
            block == Blocks.BARRIER || block == Blocks.END_PORTAL_FRAME;
    }

    private boolean hasEntity(BlockPos pos) {
        return mc.world.getOtherEntities(null,
                new net.minecraft.util.math.Box(pos)).stream()
            .anyMatch(entity -> !entity.isSpectator());
    }

    private double getDistance(BlockPos pos) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d blockCenter = Vec3d.ofCenter(pos);
        return Math.sqrt(eyePos.squaredDistanceTo(blockCenter));
    }

    private PlayerEntity getClosestEnemy(double range) {
        PlayerEntity closest = null;
        double minDist = range;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || player.isDead() || player.getHealth() <= 0) continue;

            double dist = mc.player.distanceTo(player);
            if (dist < minDist) {
                minDist = dist;
                closest = player;
            }
        }

        return closest;
    }

    private List<PlayerEntity> getEnemies(double range) {
        List<PlayerEntity> enemies = new ArrayList<>();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || player.isDead() || player.getHealth() <= 0) continue;
            if (mc.player.distanceTo(player) <= range) {
                enemies.add(player);
            }
        }

        return enemies;
    }
}
