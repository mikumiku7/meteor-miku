package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.dynamic.DV;
import com.github.mikumiku.addon.util.PlayerUtil;
import com.github.mikumiku.addon.util.VUtil;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ElytraFlyPlus extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSpeed = settings.createGroup("速度");

    //--------------------通用--------------------//
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("模式")
        .description("选择飞行模式")
        .defaultValue(Mode.Wasp)
        .build()
    );

    private final Setting<Boolean> stopWater = sgGeneral.add(new BoolSetting.Builder()
        .name("水中停用")
        .description("在水中不修改移动方式。")
        .defaultValue(true)
        .build()
    );


    private final Setting<Boolean> stopLava = sgGeneral.add(new BoolSetting.Builder()
        .name("岩浆中停用")
        .description("在岩浆中不修改移动方式。")
        .defaultValue(true)
        .build()
    );

    //--------------------速度--------------------//
    private final Setting<Double> horizontal = sgSpeed.add(new DoubleSetting.Builder()
        .name("水平速度")
        .description("每tick水平移动多少格。")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 5)
        .visible(() -> mode.get() == Mode.Wasp)
        .build()
    );

    private final Setting<Double> up = sgSpeed.add(new DoubleSetting.Builder()
        .name("上升速度")
        .description("每个刻向上移动多少格。")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 5)
        .visible(() -> mode.get() == Mode.Wasp)
        .build()
    );


    private final Setting<Double> speed = sgSpeed.add(new DoubleSetting.Builder()
        .name("速度")
        .description("每个刻移动多少格。")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 5)
        .visible(() -> mode.get() == Mode.Control)
        .build()
    );

    private final Setting<Double> upMultiplier = sgSpeed.add(new DoubleSetting.Builder()
        .name("上升倍率")
        .description("上升时加快多少倍。")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 5)
        .visible(() -> mode.get() == Mode.Control)
        .build()
    );


    private final Setting<Double> down = sgSpeed.add(new DoubleSetting.Builder()
        .name("下降速度")
        .description("每个刻向下移动多少格。")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 5)
        .visible(() -> mode.get() == Mode.Control || mode.get() == Mode.Wasp)
        .build()
    );


    private final Setting<Boolean> smartFall = sgSpeed.add(new BoolSetting.Builder()
        .name("智能下落")
        .description("只有在低头时才会下降。")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Wasp)
        .build()
    );


    private final Setting<Double> fallSpeed = sgSpeed.add(new DoubleSetting.Builder()
        .name("下落速度")
        .description("每个刻下落多少格。")
        .defaultValue(0.01)
        .min(0)
        .sliderRange(0, 1)
        .visible(() -> mode.get() == Mode.Control || mode.get() == Mode.Wasp)
        .build()
    );

    private final Setting<Double> constSpeed = sgSpeed.add(new DoubleSetting.Builder()
        .name("恒速飞行速度")
        .description("在 Constantiam 模式下的最大速度。")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 5)
        .visible(() -> mode.get() == Mode.Constantiam)
        .build()
    );

    private final Setting<Double> constAcceleration = sgSpeed.add(new DoubleSetting.Builder()
        .name("恒速加速度")
        .description("在 Constantiam 模式下的最大加速度。")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 5)
        .visible(() -> mode.get() == Mode.Constantiam)
        .build()
    );

    private final Setting<Boolean> constStop = sgSpeed.add(new BoolSetting.Builder()
        .name("恒速停用")
        .description("在没有输入时停止移动。")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Constantiam)
        .build()
    );
    private boolean moving;
    private float yaw;
    private float pitch;
    private float p;
    private double velocity;
    private int activeFor;

    public ElytraFlyPlus() {
        super("鞘翅平飞", "更多控制鞘翅方法。来自BlackOut");
    }

    @EventHandler(
        priority = 200
    )
    private void onMove(PlayerMoveEvent event) {
        if (active()) {
            activeFor++;
            if (activeFor >= 5) {
                switch (mode.get()) {
                    case Wasp:
                        waspTick(event);
                        break;
                    case Control:
                        controlTick(event);
                        break;
                    case Constantiam:
                        constantiamTick(event);
                }
            }
        }
    }

    private void constantiamTick(PlayerMoveEvent event) {
        Vec3d motion = getMotion(mc.player.getVelocity());
        if (motion != null) {
            DV.of(VUtil.class).setMovement(((IVec3d) event.movement), motion.getX(), motion.getY(), motion.getZ());
            event.movement = motion;
        }
    }

    private Vec3d getMotion(Vec3d velocity) {
        if (DV.of(PlayerUtil.class).movementForward(mc.player.input) == 0.0F) {
            return constStop.get() ? new Vec3d(0.0, 0.0, 0.0) : null;
        } else {
            boolean forward = DV.of(PlayerUtil.class).movementForward(mc.player.input) > 0.0F;
            double yaw = Math.toRadians(mc.player.getYaw() + (forward ? 90 : -90));
            double x = Math.cos(yaw);
            double z = Math.sin(yaw);
            double maxAcc = calcAcceleration(velocity.x, velocity.z, x, z);
            double delta = Math.clamp(MathHelper.getLerpProgress(velocity.horizontalLength(), 0.0, 0.5), 0.0, 1.0);
            double acc = Math.min(maxAcc, constAcceleration.get() / 20.0 * (0.1 + delta * 0.9));
            return new Vec3d(velocity.getX() + x * acc, velocity.getY(), velocity.getZ() + z * acc);
        }
    }

    private double calcAcceleration(double vx, double vz, double x, double z) {
        double xz = x * x + z * z;
        return (
            Math.sqrt(xz * constSpeed.get() * constSpeed.get() - x * x * vz * vz - z * z * vx * vx + 2.0 * x * z * vx * vz)
                - x * vx
                - z * vz
        )
            / xz;
    }

    private void waspTick(PlayerMoveEvent event) {
        if (DV.of(VUtil.class).isFallFlying(mc)) {
            updateWaspMovement();
            pitch = mc.player.getPitch();
            double cos = Math.cos(Math.toRadians(yaw + 90.0F));
            double sin = Math.sin(Math.toRadians(yaw + 90.0F));
            double x = moving ? cos * horizontal.get() : 0.0;
            double y = -(Double) fallSpeed.get();
            double z = moving ? sin * horizontal.get() : 0.0;
            if (smartFall.get()) {
                y *= Math.abs(Math.sin(Math.toRadians(pitch)));
            }

            if (mc.options.sneakKey.isPressed() && !mc.options.jumpKey.isPressed()) {
                y = -(Double) down.get();
            }

            if (!mc.options.sneakKey.isPressed() && mc.options.jumpKey.isPressed()) {
                y = up.get();
            }
            DV.of(VUtil.class).setMovement(((IVec3d) event.movement), x, y, z);

            mc.player.setVelocity(0.0, 0.0, 0.0);
        }
    }

    private void updateWaspMovement() {
        float yaw = mc.player.getYaw();
        float f = DV.of(PlayerUtil.class).movementForward(mc.player.input);
        float s = DV.of(PlayerUtil.class).movementSideways(mc.player.input);
        if (f > 0.0F) {
            moving = true;
            yaw += s > 0.0F ? -45.0F : (s < 0.0F ? 45.0F : 0.0F);
        } else if (f < 0.0F) {
            moving = true;
            yaw += s > 0.0F ? -135.0F : (s < 0.0F ? 135.0F : 180.0F);
        } else {
            moving = s != 0.0F;
            yaw += s > 0.0F ? -90.0F : (s < 0.0F ? 90.0F : 0.0F);
        }

        this.yaw = yaw;
    }

    private void controlTick(PlayerMoveEvent event) {
        if (DV.of(VUtil.class).isFallFlying(mc)) {
            updateControlMovement();
            pitch = 0.0F;
            boolean movingUp = false;
            if (!mc.options.sneakKey.isPressed() && mc.options.jumpKey.isPressed() && velocity > speed.get() * 0.4) {
                p = (float) Math.min(p + 0.1 * (1.0F - p) * (1.0F - p) * (1.0F - p), 1.0);
                pitch = Math.max(Math.max(p, 0.0F) * -90.0F, -90.0F);
                movingUp = true;
                moving = false;
            } else {
                velocity = speed.get();
                p = -0.2F;
            }

            velocity = moving
                ? speed.get()
                : Math.min(velocity + Math.sin(Math.toRadians(pitch)) * 0.08, speed.get());
            double cos = Math.cos(Math.toRadians(yaw + 90.0F));
            double sin = Math.sin(Math.toRadians(yaw + 90.0F));
            double x = moving && !movingUp ? cos * speed.get() : (movingUp ? velocity * Math.cos(Math.toRadians(pitch)) * cos : 0.0);
            double y = pitch < 0.0F
                ? velocity * upMultiplier.get() * -Math.sin(Math.toRadians(pitch)) * velocity
                : -(Double) fallSpeed.get();
            double z = moving && !movingUp ? sin * speed.get() : (movingUp ? velocity * Math.cos(Math.toRadians(pitch)) * sin : 0.0);
            y *= Math.abs(Math.sin(Math.toRadians(movingUp ? pitch : mc.player.getPitch())));
            if (mc.options.sneakKey.isPressed() && !mc.options.jumpKey.isPressed()) {
                y = -(Double) down.get();
            }
            DV.of(VUtil.class).setMovement(((IVec3d) event.movement), x, y, z);

            mc.player.setVelocity(0.0, 0.0, 0.0);
        }
    }

    private void updateControlMovement() {
        float yaw = mc.player.getYaw();
        float f = DV.of(PlayerUtil.class).movementForward(mc.player.input);
        float s = DV.of(PlayerUtil.class).movementSideways(mc.player.input);
        if (f > 0.0F) {
            moving = true;
            yaw += s > 0.0F ? -45.0F : (s < 0.0F ? 45.0F : 0.0F);
        } else if (f < 0.0F) {
            moving = true;
            yaw += s > 0.0F ? -135.0F : (s < 0.0F ? 135.0F : 180.0F);
        } else {
            moving = s != 0.0F;
            yaw += s > 0.0F ? -90.0F : (s < 0.0F ? 90.0F : 0.0F);
        }

        this.yaw = yaw;
    }

    public boolean active() {
        if (stopWater.get() && mc.player.isTouchingWater()) {
            activeFor = 0;
            return false;
        } else if (stopLava.get() && mc.player.isInLava()) {
            activeFor = 0;
            return false;
        } else {
            return DV.of(VUtil.class).isFallFlying(mc);
        }
    }

    public enum Mode {
        Wasp("黄蜂模式"),
        Control("操控模式"),
        Constantiam("恒速模式");

        final String displayName;

        Mode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

}
