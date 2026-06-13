package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.Via;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.math.Vec3d;

/**
 * 反击退模块 - 适配 GrimAC
 * 作者: leo
 * 版本: 1.21.1
 */
public class Velocity extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // 主动设置
    private final Setting<Double> horizontal = sgGeneral.add(new DoubleSetting.Builder()
        .name("水平击退比例")
        .description("调整来自攻击的水平击退强度 (0 = 无击退, 1 = 原始强度)")
        .defaultValue(0.0)
        .min(0.0)
        .max(1.0)
        .sliderMax(1.0)
        .build()
    );

    private final Setting<Double> vertical = sgGeneral.add(new DoubleSetting.Builder()
        .name("垂直击退比例")
        .description("调整来自攻击的垂直击退强度")
        .defaultValue(0.0)
        .min(0.0)
        .max(1.0)
        .sliderMax(1.0)
        .build()
    );

    private final Setting<Boolean> explosions = sgGeneral.add(new BoolSetting.Builder()
        .name("爆炸影响")
        .description("是否对 TNT / 爆炸类伤害应用反击退")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> moveStop = sgGeneral.add(new BoolSetting.Builder()
        .name("防卡脚")
        .description("防卡脚， 移动时不反击退")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> onlyGround = sgGeneral.add(new BoolSetting.Builder()
        .name("仅在地面生效")
        .description("仅当你站在地面上时才减少击退，防止空中行为被Grim检测")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> spoofLegit = sgGeneral.add(new BoolSetting.Builder()
        .name("伪装自然动作")
        .description("伪造轻微移动以绕过 Grim 检测 (推荐开启)")
        .defaultValue(true)
        .build()
    );

    public Velocity() {
        super(CATEGORY_MIKU_COMBAT, "反击退+", "在被攻击或爆炸时减少或抵抗击退效果 (兼容 GrimAC)");
    }

    @EventHandler
    private void onVelocityPacket(PacketEvent.Receive event) {
        if (moveStop.get()) {
            if (mc.options.backKey.isPressed()
                || mc.options.forwardKey.isPressed()
                || mc.options.leftKey.isPressed()
                || mc.options.rightKey.isPressed()) {
                return;
            }
        }

        if (mc.player == null || mc.world == null) return;
        if (onlyGround.get() && !mc.player.isOnGround()) return;

        if (event.packet instanceof EntityVelocityUpdateS2CPacket packet) {
            if (packet.getEntityId() == mc.player.getId()) {
                double h = horizontal.get();
                double v = vertical.get();

                if (h == 1.0 && v == 1.0) return;

                // 阻止原始击退包生效
                event.cancel();

                // 重新设置经过比例缩放后的速度
//                double motionX = packet.getVelocityX() / 8000.0 * h;
//                double motionY = packet.getVelocityY() / 8000.0 * v;
//                double motionZ = packet.getVelocityZ() / 8000.0 * h;
//
//                applyVelocity(new Vec3d(motionX, motionY, motionZ));
            }
        } else if (event.packet instanceof ExplosionS2CPacket packet && explosions.get()) {
            double h = horizontal.get();
            double v = vertical.get();

            event.cancel();
            Vec3d vec3d = Via.playerKnockback(packet);
            applyVelocity(new Vec3d(vec3d.x * h, vec3d.y * v, vec3d.z * h));
        }
    }

    private void applyVelocity(Vec3d vec) {
        PlayerEntity player = mc.player;
        if (player == null) return;

        if (spoofLegit.get()) {
            // 伪装行为：让玩家微微晃动，避免反作弊检测
            player.setVelocity(player.getVelocity().add(vec.multiply(0.98)));
        } else {
            player.setVelocity(player.getVelocity().add(vec));
        }
    }

//    @EventHandler
//    private void onEntityVelocity(EntityVelocityEvent event) {
//        if (event.entity == mc.player) {
//            double h = horizontal.get();
//            double v = vertical.get();
//
//            event.velocity = new Vec3d(
//                event.velocity.x * h,
//                event.velocity.y * v,
//                event.velocity.z * h
//            );
//        }
//    }
}
