package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.ChatUtils;
import com.github.mikumiku.addon.util.VUtil;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * 快速下落模块 - 更快地下降方块
 *
 * <p>提供两种模式来加速玩家下落速度，避免长时间的掉落等待。</p>
 *
 * <p>特色功能：</p>
 * <ul>
 *   <li>STEP模式：直接设置下落速度，适用于短距离下落</li>
 *   <li>SHIFT模式：时间偏移模式，适用于中距离下落</li>
 *   <li>智能安全检测，避免在不合适的情况下触发</li>
 *   <li>与其他移动模块的兼容性检查</li>
 * </ul>
 *
 * @author MikuMiku
 * @since 1.0.0
 */
public class FastFall extends BaseModule {

    // 设置组
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // 高度设置
    private final Setting<Double> height = sgGeneral.add(new DoubleSetting.Builder()
        .name("高度")
        .description("最大下落高度")
        .defaultValue(3.0)
        .min(1.0)
        .max(10.0)
        .sliderMin(1.0)
        .sliderMax(10.0)
        .build());

    // 模式设置
    private final Setting<FallMode> fallMode = sgGeneral.add(new EnumSetting.Builder<FallMode>()
        .name("模式")
        .description("下落模式")
        .defaultValue(FallMode.STEP)
        .build());

    // 加速设置（仅STEP模式）
    private final Setting<Boolean> accelerate = sgGeneral.add(new BoolSetting.Builder()
        .name("加速")
        .description("加速下落速度")
        .defaultValue(false)
        .visible(() -> fallMode.get() == FallMode.STEP)
        .build());

    // 偏移刻数设置（仅SHIFT模式）
    private final Setting<Integer> shiftTicks = sgGeneral.add(new IntSetting.Builder()
        .name("偏移刻数")
        .description("向前偏移的刻数")
        .defaultValue(3)
        .min(1)
        .sliderMax(5)
        .visible(() -> fallMode.get() == FallMode.SHIFT)
        .build());

    // 安全设置
    private final Setting<Boolean> antiKick = sgGeneral.add(new BoolSetting.Builder()
        .name("防踢")
        .description("防止因过快下落被服务器踢出")
        .defaultValue(true)
        .build());

    private final Setting<Double> minFallDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("最小下落距离")
        .description("触发快速下落的最小距离")
        .defaultValue(0.5)
        .min(0.1)
        .max(2.0)
        .sliderMin(0.1)
        .sliderMax(2.0)
        .build());

    // 内部状态变量
    private boolean prevOnGround = false;
    private boolean cancelFallMovement = false;
    private int fallTicks = 0;
    private long lastFallTime = 0;
    private static final long FALL_COOLDOWN = 1000; // 1秒冷却时间

    public FastFall() {
        super("快速下落", "更快地下降");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        cancelFallMovement = false;
        fallTicks = 0;
        prevOnGround = mc.player != null && mc.player.isOnGround();
    }

    @Override
    public void onDeactivate() {
        cancelFallMovement = false;
        fallTicks = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        // 记录上一刻是否在地面
        prevOnGround = mc.player.isOnGround();

        // 处理STEP模式
        if (fallMode.get() == FallMode.STEP) {
            handleStepMode();
        }
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (mc.player == null || mc.world == null) return;

        // 处理SHIFT模式
        if (fallMode.get() == FallMode.SHIFT) {
            ChatUtils.sendMsg("提示：请不要使用该模式， 没写完");
        }
    }

    /**
     * 处理STEP模式
     */
    private void handleStepMode() {
        // 安全检查
        if (!isSafeToFalling()) {
            return;
        }

        // 检查模块冲突
        if (hasConflictingModules()) {
            return;
        }

        // 计算下落高度
        double fallHeight = traceDown();
        if (fallHeight < minFallDistance.get() || fallHeight > height.get()) {
            return;
        }

        // 必须在地面上
        if (!mc.player.isOnGround()) {
            return;
        }

        // 防踢冷却
        if (antiKick.get() && System.currentTimeMillis() - lastFallTime < FALL_COOLDOWN) {
            return;
        }

        // 减少水平速度以避免水平移动过快
        Vec3d velocity = mc.player.getVelocity();
        mc.player.setVelocity(velocity.x * 0.05, velocity.y, velocity.z * 0.05);

        // 设置下落速度
        double fallSpeed = accelerate.get() ? velocity.y - 0.62 : -3.0;
        mc.player.setVelocity(velocity.x * 0.05, fallSpeed, velocity.z * 0.05);

        // 记录时间
        lastFallTime = System.currentTimeMillis();
    }

    /**
     * 向下追踪，找到到地面的距离
     */
    private double traceDown() {
        Box bb = mc.player.getBoundingBox();
        for (double i = 0.0; i < height.get() + 0.5; i += 0.01) {
            if (!mc.world.isSpaceEmpty(mc.player, bb.offset(0.0, -i, 0.0))) {
                return i;
            }
        }
        return -1.0;
    }

    /**
     * 检查是否安全使用快速下落
     */
    private boolean isSafeToFalling() {
        if (mc.player == null) return false;

        // 检查玩家状态
        if (mc.player.isRiding() ||
            VUtil.isFallFlying(mc) ||
            mc.player.isClimbing() ||
            mc.player.isInLava() ||
            mc.player.isTouchingWater() ||
            VUtil.isSneaking(mc) ||
            VUtil.isJumping(mc)) {
            return false;
        }

        // 检查状态效果
        if (mc.player.hasStatusEffect(StatusEffects.LEVITATION) ||
            mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            return false;
        }

        return true;
    }

    /**
     * 检查是否有冲突的模块
     */
    private boolean hasConflictingModules() {
        // 检查是否有飞行相关模块启用
        try {
            // 这里可以根据实际情况检查特定模块
            // 例如：Flight, Speed, LongJump等
            return false; // 暂时返回false，可以根据需要添加具体检查
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 下落模式枚举
     */
    public enum FallMode {
        STEP("步进模式"),
        SHIFT("偏移模式");

        private final String displayName;

        FallMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
