package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.concurrent.atomic.AtomicInteger;

public class AutoExtinguish extends BaseModule {
    private final SettingGroup sgGeneral = settings.createGroup("扑灭你周围的火");
    private final SettingGroup sgBucket = settings.createGroup("扑灭你身上的火");

    private final Setting<Boolean> extinguish = sgGeneral.add(new BoolSetting.Builder()
        .name("灭火")
        .description("自动扑灭你周围的火。")
        .defaultValue(false)
        .build()
    );
    private final Setting<Integer> horizontalRadius = sgGeneral.add(new IntSetting.Builder()
        .name("水平半径")
        .description("搜索火的水平半径。")
        .defaultValue(4)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Integer> verticalRadius = sgGeneral.add(new IntSetting.Builder()
        .name("垂直半径")
        .description("搜索火的垂直半径。")
        .defaultValue(4)
        .min(0)
        .sliderMax(6)
        .build()
    );
    private final Setting<Integer> maxBlockPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("数量")
        .description("每刻最多扑灭的方块数量。")
        .defaultValue(5)
        .min(1)
        .sliderMax(50)
        .build()
    );
    private final Setting<Boolean> waterBucket = sgBucket.add(new BoolSetting.Builder()
        .name("自动放水")
        .description("当你身上着火时（且没有抗火效果）自动放置水。")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> center = sgBucket.add(new BoolSetting.Builder()
        .name("居中放水")
        .description("放置水时自动将你居中。")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> onGround = sgBucket.add(new BoolSetting.Builder()
        .name("地面才水")
        .description("只有在地面上时才放置水。")
        .defaultValue(false)
        .build()
    );

    private boolean hasPlacedWater = false;
    private BlockPos blockPos = null;
    private boolean doesWaterBucketWork = true;

    private static final StatusEffect FIRE_RESISTANCE = Registries.STATUS_EFFECT.get(Identifier.of("fire_resistance"));

    public AutoExtinguish() {
        super("自动消防", "自动扑灭你周围的火");
    }

    @Override
    public void onActivate() {
        super.onActivate();

    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.world.getDimension().respawnAnchorWorks()) {
            if (doesWaterBucketWork) {
                warning("水桶在此维度不起作用！");
                doesWaterBucketWork = false;

            }
        } else {
            if (!doesWaterBucketWork) {
                warning("启用水桶！");
                doesWaterBucketWork = true;
            }
        }
        if (onGround.get() && !mc.player.isOnGround()) {
            return;
        }

        if (waterBucket.get() && doesWaterBucketWork) {
            if (hasPlacedWater) {
                final int slot = findSlot(Items.BUCKET);
                blockPos = mc.player.getBlockPos();
                place(slot);
                hasPlacedWater = false;

            } else if (!mc.player.hasStatusEffect(Registries.STATUS_EFFECT.getEntry(FIRE_RESISTANCE)) && mc.player.isOnFire()) {
                blockPos = mc.player.getBlockPos();
                final int slot = findSlot(Items.WATER_BUCKET);
                if (mc.world.getBlockState(blockPos).getBlock() == Blocks.FIRE || mc.world.getBlockState(blockPos).getBlock() == Blocks.SOUL_FIRE) {
                    float yaw = mc.gameRenderer.getCamera().getYaw() % 360;
                    float pitch = mc.gameRenderer.getCamera().getPitch() % 360;
                    if (center.get()) {
                        PlayerUtils.centerPlayer();
                    }
                    Rotations.rotate(yaw, 90);
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
                    mc.player.swingHand(Hand.MAIN_HAND);
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));

                    Rotations.rotate(yaw, pitch);
                }
                place(slot);
                hasPlacedWater = true;
            }
        }

        if (extinguish.get()) {
            AtomicInteger blocksPerTick = new AtomicInteger();
            BlockIterator.register(horizontalRadius.get(), verticalRadius.get(), (blockPos, blockState) -> {
                if (blocksPerTick.get() <= maxBlockPerTick.get()) {
                    if (blockState.getBlock() == Blocks.FIRE || mc.world.getBlockState(blockPos).getBlock() == Blocks.SOUL_FIRE) {
                        extinguishFire(blockPos);
                        blocksPerTick.getAndIncrement();
                    }
                }
            });
        }
    }

    private void place(int slot) {
        if (slot != -1) {
            final int preSlot = mc.player.getInventory().selectedSlot;
            if (center.get()) {
                PlayerUtils.centerPlayer();
            }
            mc.player.getInventory().selectedSlot = slot;
            float yaw = mc.gameRenderer.getCamera().getYaw() % 360;
            float pitch = mc.gameRenderer.getCamera().getPitch() % 360;

            Rotations.rotate(yaw, 90);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.getInventory().selectedSlot = preSlot;
            Rotations.rotate(yaw, pitch);

        }
    }

    private void extinguishFire(BlockPos blockPos) {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
    }

    private int findSlot(Item item) {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack block = mc.player.getInventory().getStack(i);
            if (block.getItem() == item) {
                slot = i;
                break;
            }
        }

        return slot;
    }

}
