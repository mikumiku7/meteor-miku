package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.Via;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.math.Vec3d;

public class Hover extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> hoverSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("悬停速度")
        .description("悬停时的移动速度")
        .defaultValue(0.1)
        .min(0.01)
        .sliderRange(0.01, 1)
        .build()
    );

    private final Setting<Boolean> disableOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("地面禁用")
        .description("在玩家接触地面时禁用悬停")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoElytra = sgGeneral.add(new BoolSetting.Builder()
        .name("自动鞘翅")
        .description("自动激活鞘翅飞行以保持悬停状态")
        .defaultValue(true)
        .build()
    );

    // 是否启用移动检测
    private final Setting<Boolean> pauseOnMovement = sgGeneral.add(new BoolSetting.Builder()
        .name("移动时暂停")
        .description("当玩家移动时暂停悬停")
        .defaultValue(true)
        .build());

    public Hover() {
        super("悬停", "允许玩家在空中保持位置不变");
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (mc.player == null || mc.world == null) return;

        // 检查是否应该禁用悬停
        if (disableOnGround.get() && mc.player.isOnGround()) {
            return;
        }

        // 自动激活鞘翅飞行
        if (autoElytra.get() && !Via.isFallFlying(mc) && !mc.player.isOnGround()) {
            mc.player.networkHandler.sendPacket(
                new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING)
            );
        }

        // 如果正在鞘翅飞行，控制移动以实现悬停
        if (Via.isFallFlying(mc)) {
            // 获取当前移动输入
            float forward = Via.movementForward(mc.player.input);
            float sideways = Via.movementSideways(mc.player.input);
            float yaw = mc.player.getYaw();

            // 计算方向向量
            double cos = Math.cos(Math.toRadians(yaw + 90));
            double sin = Math.sin(Math.toRadians(yaw + 90));

            // 计算移动向量
            double moveX = (forward * cos + sideways * sin) * hoverSpeed.get();
            double moveZ = (forward * sin - sideways * cos) * hoverSpeed.get();

            // 垂直移动控制
            double moveY = 0;
            if (mc.options.jumpKey.isPressed()) {
                moveY = hoverSpeed.get();
            } else if (mc.options.sneakKey.isPressed()) {
                moveY = -hoverSpeed.get();
            }

            // 应用移动
            Via.setMovement(((IVec3d) event.movement), moveX, moveY, moveZ);

            // 减少下落速度以实现更好的悬停效果
            Vec3d velocity = mc.player.getVelocity();
            mc.player.setVelocity(velocity.x, velocity.y * 0.9, velocity.z);
        }


        // 检查是否需要暂停（玩家移动检测）
        boolean shouldPause = false;
        if (pauseOnMovement.get()) {
            shouldPause = isPlayerMoving();
        }

        if (!shouldPause) {
            performSpin();
        }
    }


    /**
     * 检测玩家是否在移动
     *
     * @return true 如果玩家正在移动
     */
    public boolean isPlayerMoving() {
        // 检查按键输入
        boolean keyPressed =
            Input.isPressed(mc.options.forwardKey) ||
                Input.isPressed(mc.options.backKey) ||
                Input.isPressed(mc.options.leftKey) ||
                Input.isPressed(mc.options.rightKey) ||
                Input.isPressed(mc.options.jumpKey) ||
                Input.isPressed(mc.options.sneakKey);

        return keyPressed;
    }

    /**
     * 执行旋转操作
     */
    private void performSpin() {

        float currentYaw = mc.player.getYaw();

        // 同时发送到服务器确保其他玩家也能看到
        mc.player.networkHandler.sendPacket(
            Via.get(currentYaw,
                mc.player.getPitch(),
                mc.player.isOnGround())

        );

    }
}
