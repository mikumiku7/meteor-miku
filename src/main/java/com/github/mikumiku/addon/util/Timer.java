package com.github.mikumiku.addon.util;

public class Timer {
    private long time = -1;

    public Timer() {
        this.reset();
    }

    public Timer reset() {
        this.time = System.nanoTime();
        return this;
    }

    public boolean tick(int tick) {
        return this.passedMs(tick * 50);
    }

    public boolean passedTicks(int tick) {
        return this.passedMs(tick * 50);
    }

    public boolean passedS(double s) {
        return this.passedMs(s * 1000);
    }

    public boolean passedMs(long ms) {
        return this.passedNS(this.convertToNS(ms));
    }

    public boolean passedMs(double ms) {
        return this.passedMs((long) ms);
    }

    public boolean passed(long ms) {
        return this.passedMs(ms);
    }

    public boolean passed(double ms) {
        return this.passedMs((long) ms);
    }

    public void setMs(long ms) {
        this.time = System.nanoTime() - this.convertToNS(ms);
    }

    public boolean passedNS(long ns) {
        return System.nanoTime() - this.time >= ns;
    }

    public long getPassedTimeMs() {
        return this.getMs(System.nanoTime() - this.time);
    }

    public long getMs(long time) {
        return time / 1000000;
    }

    public long convertToNS(long time) {
        return time * 1000000;
    }
}
