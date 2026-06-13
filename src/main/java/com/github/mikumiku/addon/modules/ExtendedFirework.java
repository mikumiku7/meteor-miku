package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.mixin.AccessorFireworkRocketEntity;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 延长烟花助推时间的 Meteor 模块
 * 原理：拦截烟花销毁与 Pong 包，让客户端保持烟花存在
 * super("烟花延长", "延长烟花持续时间");
 */
public class ExtendedFirework extends BaseModule {
    private final List<CommonPongC2SPacket> packetList = new CopyOnWriteArrayList<>();

    private boolean extendFirework;
    private long extendFireworkTimer;
    private FireworkRocketEntity firework;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> maxDuration = sgGeneral.add(new IntSetting.Builder()
        .name("最大持续时间")
        .description("烟花推进的最大持续时间(秒)")
        .defaultValue(44)
        .min(1)
        .max(120)
        .sliderMax(120)
        .build()
    );

    private final Setting<Boolean> autoDisableOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("着陆自动停止")
        .description("着陆时自动停止延长烟花推进")
        .defaultValue(true)
        .build()
    );


    public ExtendedFirework() {
        super(BaseModule.CATEGORY_MIKU_PRO, "烟花延长", "延长烟花持续时间， 在高反作弊服可能无用。");
    }

    @Override
    public String getInfoString() {
        if (!extendFirework) {
            return null;
        }
        float elapsed = (System.currentTimeMillis() - extendFireworkTimer) / 1000.0f;
        return new DecimalFormat("0.0").format(elapsed) + "s";
    }

    @Override
    public void onDeactivate() {
        cleanupFirework();
        extendFirework = false;
        extendFireworkTimer = 0;
        sendPongPackets();
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        cleanupFirework();
        extendFirework = false;
        extendFireworkTimer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!extendFirework) {
            // Look for player's firework rocket
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof FireworkRocketEntity rocket) {
                    AccessorFireworkRocketEntity accessor = (AccessorFireworkRocketEntity) rocket;
                    if (accessor.hookWasShotByEntity() && accessor.hookGetShooter() == mc.player) {
                        firework = rocket;
                        break;
                    }
                }
            }
            extendFireworkTimer = System.currentTimeMillis();
            return;
        }

        // Check if should stop extending
        long elapsed = System.currentTimeMillis() - extendFireworkTimer;
        boolean shouldStop = elapsed >= maxDuration.get() * 1000L;

        if (autoDisableOnGround.get() && mc.player.isOnGround()) {
            shouldStop = true;
        }

        if (shouldStop) {
            extendFirework = false;
            cleanupFirework();
            sendPongPackets();
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof CommonPongC2SPacket packet && extendFirework) {
            event.cancel();
            packetList.add(packet);
        }
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        // Cancel firework destroy packet
        if (event.packet instanceof EntitiesDestroyS2CPacket packet && firework != null) {
            for (int id : packet.getEntityIds()) {
                if (id == firework.getId()) {
                    event.cancel();
                    extendFirework = true;
                    extendFireworkTimer = System.currentTimeMillis();
                    return;
                }
            }
        }

        // Handle teleport/position update
        if (event.packet instanceof PlayerPositionLookS2CPacket && extendFirework) {
            extendFirework = false;
            cleanupFirework();
            sendPongPackets();
        }
    }

    private void cleanupFirework() {
        if (firework != null) {
            // Simply discard the firework, let the server handle removal
            // The entity will be removed naturally when we stop canceling destroy packets
            firework.discard();
            firework = null;
        }
    }

    private void sendPongPackets() {
        for (CommonPongC2SPacket packet : packetList) {
            mc.getNetworkHandler().sendPacket(packet);
        }
        packetList.clear();
    }

    public boolean isExtendingFirework() {
        return extendFirework;
    }
}
