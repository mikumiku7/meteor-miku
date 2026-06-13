package com.github.mikumiku.addon.util.timer;

import lombok.Getter;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;

public final class TickTimerManager {
    // before any modules
    public static final int TICK_PRIORITY = Integer.MAX_VALUE - 1;
    public static final TickTimerManager INSTANCE = new TickTimerManager();

    @Getter
    private volatile long tickTime = 0;

    private TickTimerManager() {
        // 注册到事件总线
        MeteorClient.EVENT_BUS.subscribe(this);

//        ClientTickEvents.END_CLIENT_TICK.register(client -> {
//            this.onClientTick();
//        });
    }

    private void onClientTick() {
        tickTime++;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        // 每 tick 执行
        this.onClientTick();
    }
}
