package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.ChatUtils;
import com.github.mikumiku.addon.util.Via;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class NoFall extends BaseModule {

    private static final double DELTA_Y = 1.0E-7;
    private static final int LATENCY = 10;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAdvanced = settings.createGroup("高级设置");

    // 基础设置
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("模式")
        .description("选择无摔伤模式")
        .defaultValue(Mode.GRIMA)
        .build()
    );

    private final Setting<Boolean> disableOnFly = sgGeneral.add(new BoolSetting.Builder()
        .name("飞行时禁用")
        .description("当服务器允许飞行时禁用无摔伤")
        .defaultValue(true)
        .build()
    );

    // 高级设置
    private final Setting<Integer> safeDistance = sgAdvanced.add(new IntSetting.Builder()
        .name("安全距离修正")
        .description("修正安全下落距离（格）")
        .defaultValue(0)
        .min(-10)
        .max(10)
        .visible(() -> mode.get() != Mode.GRIMB)
        .build()
    );

    // LAZY 模式状态
    private int lastNoFall = -1;
    private boolean afterSetbackFlag = true;

    // LAZY_GRIM_PLUS 模式状态
    private Step step = Step.COMMON;

    // 通用状态
    private double lastOnGroundHeight = Double.NEGATIVE_INFINITY;
    private double lastHeight = 0;
    private int counter = 0;

    public NoFall() {
        super("Grim无摔伤", "防止摔落伤害，支持多种绕过模式");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        ChatUtils.sendMsg("无摔伤需要 Via 调整到1.20.6或以下版本");
        ChatUtils.sendMsg("模式: " + mode.get().toString());
        resetState();
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        resetState();
    }

    private void resetState() {
        lastOnGroundHeight = Double.NEGATIVE_INFINITY;
        lastHeight = 0;
        counter = 0;
        lastNoFall = -1;
        afterSetbackFlag = true;
        step = Step.COMMON;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (mode.get() == Mode.GRIMB) {
            handleViaFabricMode();
        }
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (mc.player == null || mc.world == null) return;

        Mode currentMode = mode.get();
        if (currentMode == Mode.GRIMA || currentMode == Mode.GRIMC) {
            updateCommonState();
            handleLazyBypassMode();
        }
    }

    // ==================== Via Fabric 原版模式 ====================

    private void handleViaFabricMode() {
        if (!isFalling()) return;

        mc.getNetworkHandler().sendPacket(Via.getFull(
            mc.player.getX(),
            mc.player.getY() + 0.000000001,
            mc.player.getZ(),
            mc.player.getYaw(),
            mc.player.getPitch(),
            false
        ));
        mc.player.onLanding();
    }

    private boolean isFalling() {
        return mc.player.fallDistance > mc.player.getSafeFallDistance()
            && !mc.player.isOnGround()
            && !Via.isFallFlying(mc);
    }

    // ==================== Lazy Bypass Grim 模式 ====================

    private void updateCommonState() {
        if (mc.player == null) return;

        // 检查是否无敌或允许飞行
        if (mc.player.getAbilities().invulnerable) return;
        if (disableOnFly.get() && mc.player.getAbilities().allowFlying) return;
        if (!mc.player.isAlive()) return;

        lastHeight = mc.player.getY();

        // 重置高度记录
        if (mc.player.isOnGround() || mc.player.isTouchingWater()) {
            lastOnGroundHeight = lastHeight;
            counter = 0;
        } else if (lastHeight > lastOnGroundHeight) {
            lastOnGroundHeight = lastHeight;
            counter = 0;
        }
    }

    private void handleLazyBypassMode() {
        counter += 1;
        if (counter > 100) {
            afterSetbackFlag = false;
        }

        if (shouldApplyNoFall()) {
            applyNoFallPacket();
        }
    }

    // ==================== Lazy Grim Plus 模式 ====================

    private void handleLazyGrimPlusMode() {
        counter += 1;
        if (counter > 100) {
            afterSetbackFlag = false;
        }

        switch (step) {
            case COMMON -> {
                if (shouldApplyNoFall()) {
                    step = Step.REAPPLY_MOVEMENT;
                    applyNoFallPacket();
                }
            }
            case REAPPLY_MOVEMENT -> step = Step.WAIT_FOR_RESYNC;
            case WAIT_FOR_RESYNC -> {
                if (lastNoFall + 2 * LATENCY <= getTick()) {
                    step = Step.COMMON;
                }
            }
            case HANDLE_RESYNC -> {
                if (lastNoFall + LATENCY <= getTick()) {
                    step = Step.COMMON;
                }
            }
        }
    }

    // ==================== 通用逻辑 ====================

    private boolean shouldApplyNoFall() {
        if (mc.player == null) return false;

        // 检查是否在不安全的下落距离内
        double safeDist = 3 + safeDistance.get();
        if (mc.player.getY() > lastOnGroundHeight - safeDist) return false;

        // 检查是否在地面上
        if (mc.player.isOnGround()) return false;

        // 检查时间间隔
        return lastNoFall + LATENCY <= getTick();
    }

    private void applyNoFallPacket() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        lastNoFall = getTick();
        counter = 0;

        // 发送无摔伤数据包（位置+着陆状态，onGround=false 让客户端认为在地面）
        mc.getNetworkHandler().sendPacket(Via.getPositionAndOnGround(
            mc.player.getX(),
            mc.player.getY() + DELTA_Y,
            mc.player.getZ(),
            false
        ));

        lastOnGroundHeight = mc.player.getY();
    }

    private int getTick() {
        return mc.player != null ? mc.player.age : 0;
    }

    // ==================== 枚举 ====================

    public enum Mode {
        GRIMA("Grim模式A"),
        GRIMB("Grim模式B"),
        GRIMC("Grim模式C");

        private final String displayName;

        Mode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private enum Step {
        COMMON,
        REAPPLY_MOVEMENT,
        WAIT_FOR_RESYNC,
        HANDLE_RESYNC
    }
}
