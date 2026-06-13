package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.Via;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.systems.modules.combat.Surround;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.Random;

public class Criticals extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("模式")
        .description("暴击攻击的触发方式")
        .defaultValue(Mode.GrimV3)
        .build()
    );

    private final Setting<Boolean> multitask = sgGeneral.add(new BoolSetting.Builder()
        .name("多任务")
        .description("其他模块启用时也能触发暴击")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> phasedOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("仅卡墙时")
        .description("仅在相位状态下尝试暴击")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.Grim || mode.get() == Mode.GrimV3)
        .build()
    );

    private final Setting<Boolean> wallsOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("仅墙内")
        .description("只在完全卡在墙里时触发暴击")
        .defaultValue(false)
        .visible(() -> (mode.get() == Mode.Grim || mode.get() == Mode.GrimV3) && phasedOnly.get())
        .build()
    );

    private final Setting<Boolean> moveFix = sgGeneral.add(new BoolSetting.Builder()
        .name("移动时暂停")
        .description("移动时不触发暴击")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.Grim || mode.get() == Mode.GrimV3)
        .build()
    );

    private final Setting<Boolean> pauseOnCA = sgGeneral.add(new BoolSetting.Builder()
        .name("水晶时暂停")
        .description("使用水晶光环时不触发暴击")
        .defaultValue(true)
        .build()
    );

    private final Random random = new Random();
    private long lastAttackTime = 0;
    private boolean postUpdateGround = false;
    private boolean postUpdateSprint = false;

    public Criticals() {
        super(BaseModule.CATEGORY_MIKU_COMBAT, "刀暴击+", "让每次攻击都打出暴击伤害");
    }

    @Override
    public String getInfoString() {
        return mode.get().name();
    }

    @EventHandler
    private void onAttackEntity(AttackEntityEvent event) {

        // Pause on Crystal Aura
        if (pauseOnCA.get()) {
            CrystalAura ca = Modules.get().get(CrystalAura.class);
            if (ca != null && ca.isActive()) return;
        }

        // Multitask check
        if (!multitask.get()) {
            if (Modules.get().isActive(Surround.class) ||
                Modules.get().isActive(SelfTrapPlusPlus.class)) {
                return;
            }
        }

        Entity target = event.entity;
        if (target == null || !target.isAlive() || !(target instanceof LivingEntity)) return;

        // Invalid states
        if (mc.player.isRiding() ||
            Via.isFallFlying(mc) ||
            mc.player.isTouchingWater() || mc.player.isInLava() ||
            mc.player.isClimbing() || mc.player.hasStatusEffect(StatusEffects.BLINDNESS)) {
            return;
        }

        postUpdateSprint = mc.player.isSprinting();
        if (postUpdateSprint) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }

        doCritical(target);
    }

    @EventHandler
    private void onPacketSent(PacketEvent.Sent event) {
        if (mc.player == null) return;

        if (event.packet instanceof PlayerInteractEntityC2SPacket) {
            if (postUpdateGround) {
                mc.player.networkHandler.sendPacket(Via.getPositionAndOnGround(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
                postUpdateGround = false;
            }

            if (postUpdateSprint) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(
                    mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
                postUpdateSprint = false;
            }
        }
    }

    private void doCritical(Entity target) {
        if (!mc.player.isOnGround() || Via.isJumping(mc)) {
            return;
        }


        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();

        switch (mode.get()) {
            case Vanilla -> {
                double d = 1.0e-7 + 1.0e-7 * (1.0 + random.nextInt(random.nextBoolean() ? 34 : 43));
                sendPosition(x, y + 0.1016 + d * 3.0, z, false);
                sendPosition(x, y + 0.0202 + d * 2.0, z, false);
                sendPosition(x, y + 3.239e-4 + d, z, false);
                mc.player.addCritParticles(target);
            }

            case Packet -> {
                sendPosition(x, y + 0.0625, z, false);
                sendPosition(x, y, z, false);
                mc.player.addCritParticles(target);
            }

            case PacketStrict -> {
                if (System.currentTimeMillis() - lastAttackTime >= 500) {
                    sendPosition(x, y + 1.1e-7, z, false);
                    sendPosition(x, y + 1.0e-8, z, false);
                    postUpdateGround = true;
                    lastAttackTime = System.currentTimeMillis();
                }
            }

            case Grim -> {
                if (phasedOnly.get() && (wallsOnly.get() ? !isDoublePhased() : !isPhased())) return;
                if (moveFix.get() && PlayerUtils.isMoving()) return;

                if (System.currentTimeMillis() - lastAttackTime >= 250 && !mc.player.isCrawling()) {
                    sendPositionFull(x, y + 0.0625, z, yaw, pitch, false);
                    sendPositionFull(x, y + 0.0625013579, z, yaw, pitch, false);
                    sendPositionFull(x, y + 1.3579e-6, z, yaw, pitch, false);
                    lastAttackTime = System.currentTimeMillis();
                }
            }

            case GrimV3 -> {
                if (phasedOnly.get() && (wallsOnly.get() ? !isDoublePhased() : !isPhased())) return;
                if (moveFix.get() && PlayerUtils.isMoving()) return;

                if (!mc.player.isCrawling()) {
                    sendPositionFull(x, y, z, yaw, pitch, true);
                    sendPositionFull(x, y + 0.0625, z, yaw, pitch, false);
                    sendPositionFull(x, y + 0.04535, z, yaw, pitch, false);
                }
            }

            case LowHop -> {
                mc.player.setVelocity(mc.player.getVelocity().x, 0.3425, mc.player.getVelocity().z);
            }
        }
    }

    private void sendPosition(double x, double y, double z, boolean onGround) {
        mc.player.networkHandler.sendPacket(Via.getPositionAndOnGround(x, y, z, onGround));
    }

    private void sendPositionFull(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        mc.player.networkHandler.sendPacket(Via.getFull(x, y, z, yaw, pitch, onGround));
    }

    private boolean isPhased() {
        Box box = mc.player.getBoundingBox();
        BlockPos.Mutable blockPos = new BlockPos.Mutable();

        for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                    blockPos.set(x, y, z);
                    if (!mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isDoublePhased() {
        Box box = mc.player.getBoundingBox();
        BlockPos.Mutable blockPos = new BlockPos.Mutable();

        for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                    blockPos.set(x, y, z);
                    blockPos.set(x, y + 1, z);
                    if (!mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty() &&
                        !mc.world.getBlockState(blockPos.up()).getCollisionShape(mc.world, blockPos.up()).isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public enum Mode {
        Vanilla,
        Packet,
        PacketStrict,
        Grim,
        GrimV3,
        LowHop
    }
}
