package com.github.mikumiku.addon.util;

import java.util.concurrent.atomic.AtomicLong;

public class DebounceLimiter {
    private final AtomicLong lastRun = new AtomicLong(0);
    private final long intervalNanos; // 防抖间隔，单位纳秒

    public DebounceLimiter(long intervalMillis) {
        this.intervalNanos = intervalMillis * 1_000_000;
    }

    /**
     * 尝试执行，如果1秒内已经执行过则跳过
     */
    public void run(Runnable action) {
        long now = System.nanoTime();
        long last = lastRun.get();

        // 如果距离上次执行不足 interval，则跳过
        if (now - last < intervalNanos) {
            return;
        }

        // CAS 确保并发下只有一个线程能通过
        if (lastRun.compareAndSet(last, now)) {
            try {
                action.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public void tt(Runnable action, long intervalMillis) {
        DebounceLimiter limiter = new DebounceLimiter(1000); // 1秒防抖

        // 假设多个线程都在调用：
        limiter.run(() -> {
            System.out.println("执行一次: " + System.currentTimeMillis());
        });
    }
}
