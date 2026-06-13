package com.github.mikumiku.addon.util.timer;

public final class Timers {
    private Timers() {}

    public static SyncedTickTimer tickTimer() {
        return new SyncedTickTimer();
    }

    public static StandardTimer unsyncedTickTimer() {
        return new StandardTimer(50L);
    }

    public static StandardTimer timer() {
        return new StandardTimer();
    }

    public static StandardTimer timer(long delay) {
        return new StandardTimer(delay);
    }
}
