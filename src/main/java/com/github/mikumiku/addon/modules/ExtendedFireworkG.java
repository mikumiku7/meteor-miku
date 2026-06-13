//package com.github.mikumiku.addon.modules;
//
//import com.github.mikumiku.addon.BaseModule;
//import com.github.mikumiku.addon.mixin.AccessorFireworkRocketEntity;
//import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
//import meteordevelopment.meteorclient.events.packets.PacketEvent;
//import meteordevelopment.meteorclient.events.world.TickEvent;
//import meteordevelopment.orbit.EventHandler;
//import meteordevelopment.orbit.EventPriority;
//import net.minecraft.entity.Entity;
//import net.minecraft.entity.projectile.FireworkRocketEntity;
//import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
//import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
//import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
//
//import java.text.DecimalFormat;
//import java.util.List;
//import java.util.concurrent.CopyOnWriteArrayList;
//
/// **
// * 延长烟花助推时间的 Meteor 模块
// * 原理：拦截烟花销毁与 Pong 包，让客户端保持烟花存在
// */
//public class ExtendedFireworkG extends BaseModule {
//
//    private final List<CommonPongC2SPacket> packetList = new CopyOnWriteArrayList<>();
//    private boolean extendFirework = false;
//    private final Timer extendFireworkTimer = new Timer();
//    private FireworkRocketEntity firework;
//
//    public ExtendedFireworkG() {
//        super("extended-firework", "Extends elytra firework boost duration");
//    }
//
//
//    @Override
//    public String getInfoString() {
//        if (!extendFirework) extendFireworkTimer.reset();
//        return new DecimalFormat("0.0").format(extendFireworkTimer.getTime() / 1000.0f) + "s";
//    }
//
//    @Override
//    public void onDeactivate() {
//        if (firework != null) {
//            ((AccessorFireworkRocketEntity) firework).hookExplodeAndRemove();
//            firework = null;
//        }
//        extendFirework = false;
//        extendFireworkTimer.reset();
//        sendPongPackets();
//    }
//
//    @EventHandler
//    private void onGameJoin(GameJoinedEvent event) {
//        if (firework != null) {
//            ((AccessorFireworkRocketEntity) firework).hookExplodeAndRemove();
//            firework = null;
//        }
//        extendFirework = false;
//        extendFireworkTimer.reset();
//    }
//
//    @EventHandler(priority = EventPriority.HIGH)
//    private void onTick(TickEvent.Pre event) {
//        if (mc.player == null || mc.world == null) return;
//
//        // 未处于延长状态：寻找刚发射的烟花
//        if (!extendFirework) {
//            for (Entity entity : mc.world.getEntities()) {
//                if (entity instanceof FireworkRocketEntity rocket
//                    && ((AccessorFireworkRocketEntity) rocket).hookWasShotByEntity()
//                    && ((AccessorFireworkRocketEntity) rocket).hookGetShooter() == mc.player) {
//                    firework = rocket;
//                    break;
//                }
//            }
//            extendFireworkTimer.reset();
//            return;
//        }
//
//        // 延长中，但超时或落地则停止
//        if (mc.player.isOnGround() || extendFireworkTimer.passed(44000)) {
//            extendFirework = false;
//            if (firework != null) {
//                ((AccessorFireworkRocketEntity) firework).hookExplodeAndRemove();
//                firework = null;
//            }
//            sendPongPackets();
//        }
//    }
//
//    @EventHandler
//    private void onPacketSend(PacketEvent.Send event) {
//        if (mc.player == null || mc.world == null) return;
//
//        if (event.packet instanceof CommonPongC2SPacket packet && extendFirework) {
//            event.cancel();
//            packetList.add(packet);
//        }
//    }
//
//    @EventHandler
//    private void onPacketReceive(PacketEvent.Receive event) {
//        if (mc.player == null || mc.world == null) return;
//
//        // 拦截烟花销毁包
//        if (event.packet instanceof EntitiesDestroyS2CPacket packet && firework != null) {
//            for (int id : packet.getEntityIds()) {
//                if (id == firework.getId()) {
//                    event.cancel();
//                    extendFirework = true;
//                    extendFireworkTimer.reset();
//                    return;
//                }
//            }
//        }
//
//        // 服务器强制校正位置时终止
//        if (event.packet instanceof PlayerPositionLookS2CPacket && extendFirework) {
//            extendFirework = false;
//            if (firework != null) {
//                ((AccessorFireworkRocketEntity) firework).hookExplodeAndRemove();
//                firework = null;
//            }
//            sendPongPackets();
//        }
//    }
//
//    private void sendPongPackets() {
//        for (CommonPongC2SPacket packet : packetList) {
//            mc.getNetworkHandler().sendPacket(packet);
//        }
//        packetList.clear();
//    }
//
//    public boolean isExtending() {
//        return extendFirework;
//    }
//
//    // 简单 Timer 工具类（Meteor 没有直接的 CacheTimer）
//    private static class Timer {
//        private long lastTime = System.currentTimeMillis();
//
//        public void reset() {
//            lastTime = System.currentTimeMillis();
//        }
//
//        public boolean passed(long ms) {
//            return System.currentTimeMillis() - lastTime >= ms;
//        }
//
//        public long getTime() {
//            return System.currentTimeMillis() - lastTime;
//        }
//    }
//}


//  "进基岩坑", "自动进附近的安全基岩坑。");
//
