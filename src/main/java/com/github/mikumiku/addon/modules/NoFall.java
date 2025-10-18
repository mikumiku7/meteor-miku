package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.dynamic.DV;
import com.github.mikumiku.addon.util.VUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;

public class NoFall extends BaseModule {

    public NoFall() {
        super("Grim无摔伤", "防止摔落伤害++");
    }


    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (!isFalling()) return;

        // Grim 模式：发送微小位移数据包并调用着陆
        mc.getNetworkHandler().sendPacket(DV.of(VUtil.class).getFull(
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
            && !DV.of(VUtil.class).isFallFlying(mc);
    }
}
