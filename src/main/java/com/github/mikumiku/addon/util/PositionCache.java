package com.github.mikumiku.addon.util;

import net.minecraft.util.math.BlockPos;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 位置缓存工具类
 *
 * <p>提供带有过期时间的位置缓存功能，支持后台自动清理过期条目。</p>
 *
 * <p>主要特性：</p>
 * <ul>
 *   <li>可配置的缓存过期时间</li>
 *   <li>线程安全的缓存操作</li>
 *   <li>后台自动清理过期缓存</li>
 *   <li>防止内存泄漏的缓存管理</li>
 * </ul>
 *
 * @author MikuMiku
 * @since 1.0.0
 */
public class PositionCache {

    /**
     * 位置到时间戳的映射缓存
     * 使用 ConcurrentHashMap 保证线程安全
     */
    private final Map<BlockPos, Long> cacheMap = new ConcurrentHashMap<>();

    /**
     * 缓存过期时间（毫秒）
     */
    private volatile long expireTimeMs;

    /**
     * 后台清理线程
     */
    private Thread cleanupThread;

    /**
     * 控制清理线程运行的标志
     */
    private volatile boolean running = true;

    /**
     * 清理线程休眠时间（毫秒）
     */
    private static final long CLEANUP_SLEEP_MS = 10000L; // 10秒

    /**
     * 构造函数，使用默认过期时间1000毫秒
     */
    public PositionCache() {
        this(1000L);
    }

    /**
     * 构造函数
     *
     * @param expireTimeMs 缓存过期时间（毫秒）
     */
    public PositionCache(long expireTimeMs) {
        if (expireTimeMs <= 0) {
            throw new IllegalArgumentException("过期时间必须大于0");
        }
        this.expireTimeMs = expireTimeMs;
        startCleanupThread();
    }

    /**
     * 将位置加入缓存
     *
     * @param pos 要缓存的位置
     */
    public void addToCache(BlockPos pos) {
        if (pos == null) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        cacheMap.put(pos, currentTime);
    }

    /**
     * 检查位置是否在缓存中且未过期
     *
     * @param pos 要检查的位置
     * @return true 如果位置在缓存中且未过期
     */
    public boolean isInCache(BlockPos pos) {
        if (pos == null || !cacheMap.containsKey(pos)) {
            return false;
        }

        long cachedTime = cacheMap.get(pos);
        long currentTime = System.currentTimeMillis();

        // 如果已过期，从缓存中移除并返回false
        if (currentTime - cachedTime >= expireTimeMs) {
            cacheMap.remove(pos);
            return false;
        }

        return true;
    }

    /**
     * 清理所有过期的缓存条目
     * 此方法是线程安全的，可以随时调用
     */
    public void cleanExpiredCache() {
        long currentTime = System.currentTimeMillis();

        // 使用迭代器安全地删除过期条目
        Iterator<Map.Entry<BlockPos, Long>> iterator = cacheMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Long> entry = iterator.next();
            if (currentTime - entry.getValue() >= expireTimeMs) {
                iterator.remove();
            }
        }
    }

    /**
     * 清空所有缓存条目
     */
    public void clearCache() {
        cacheMap.clear();
    }

    /**
     * 获取当前缓存大小
     *
     * @return 缓存中条目的数量
     */
    public int size() {
        return cacheMap.size();
    }

    /**
     * 获取当前设置的过期时间
     *
     * @return 过期时间（毫秒）
     */
    public long getExpireTimeMs() {
        return expireTimeMs;
    }

    /**
     * 设置新的过期时间
     *
     * @param expireTimeMs 新的过期时间（毫秒）
     */
    public void setExpireTimeMs(long expireTimeMs) {
        if (expireTimeMs <= 0) {
            throw new IllegalArgumentException("过期时间必须大于0");
        }
        this.expireTimeMs = expireTimeMs;
    }

    /**
     * 启动后台清理线程
     */
    public void startCleanupThread() {
        shutdown();
        running = true;
        cleanupThread = new Thread(() -> {
            while (running) {
                try {
                    // 休眠指定时间
                    Thread.sleep(CLEANUP_SLEEP_MS);

                    // 如果还在运行，清理过期缓存
                    if (running) {
                        cleanExpiredCache();
                    }
                } catch (InterruptedException e) {
                    // 线程被中断，退出循环
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // 发生其他异常，记录错误但继续运行
                    System.err.println("PositionCache 清理线程发生异常: " + e.getMessage());
                }
            }
        });

        cleanupThread.setDaemon(true); // 设置为守护线程，不会阻止JVM退出
        cleanupThread.setName("PositionCache-CleanupThread");
        cleanupThread.start();
    }

    /**
     * 停止清理线程并清理资源
     * 应该在不再需要缓存时调用
     */
    public void shutdown() {
        running = false;
        if (cleanupThread != null && cleanupThread.isAlive()) {
            cleanupThread.interrupt();
            try {
                // 等待线程结束，最多等待1秒
                cleanupThread.join(1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        clearCache();
    }

    /**
     * 检查清理线程是否正在运行
     *
     * @return true 如果清理线程正在运行
     */
    public boolean isRunning() {
        return running && cleanupThread != null && cleanupThread.isAlive();
    }

    /**
     * 获取缓存统计信息
     *
     * @return 包含缓存统计信息的字符串
     */
    public String getStats() {
        return String.format("PositionCache[大小: %d, 过期时间: %dms, 清理线程运行: %s]",
            size(), expireTimeMs, isRunning());
    }
}
