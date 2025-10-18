package com.github.mikumiku.addon.util;

import com.github.mikumiku.addon.dynamic.DV;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.client.MinecraftClient;

/**
 * 旋转管理器 - 用于管理玩家视角旋转的核心组件
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>统一管理所有模块的视角旋转需求</li>
 *   <li>基于优先级系统处理旋转冲突</li>
 *   <li>提供平滑的视角过渡和同步机制</li>
 *   <li>确保旋转操作的线程安全性</li>
 * </ul>
 *
 * <p>使用场景：</p>
 * <ul>
 *   <li>自动建造模块需要精确朝向目标方块</li>
 *   <li>战斗模块需要快速转向敌人</li>
 *   <li>种植模块需要面向种植位置</li>
 *   <li>挖掘模块需要对准目标方块</li>
 * </ul>
 *
 * <p>工作原理：</p>
 * <ol>
 *   <li>模块通过 {@link #register(Rotation)} 注册旋转请求</li>
 *   <li>系统根据优先级决定是否接受请求</li>
 *   <li>发送网络包更新服务器端玩家朝向</li>
 *   <li>同步客户端摄像机视角</li>
 *   <li>操作完成后调用 {@link #sync()} 恢复原始朝向</li>
 * </ol>
 *
 * @author GGB Helper
 * @since 1.0.0
 */
public class RotationManager {
    MinecraftClient mc = MinecraftClient.getInstance();

    // 单例实例，使用 volatile 确保多线程环境下的可见性
    private static volatile RotationManager instance;

    public Rotation currentRotation = null;
    Timer timer = new Timer();

    // 私有构造函数，防止外部直接实例化
    private RotationManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
        mc = MinecraftClient.getInstance();

    }

    /**
     * 获取 RotationManager 的单例实例
     * 使用双重检查锁定模式实现线程安全的懒加载
     *
     * @return RotationManager 的唯一实例
     */
    public static RotationManager getInstance() {
        if (instance == null) {
            synchronized (RotationManager.class) {
                if (instance == null) {
                    instance = new RotationManager();
                }
            }
        }
        return instance;
    }

    /**
     * 注册一个旋转请求到旋转管理器
     *
     * <p>该方法是旋转系统的核心，负责处理所有模块的旋转需求。
     * 系统采用优先级机制来解决多个模块同时请求旋转的冲突。</p>
     *
     * <p>执行流程：</p>
     * <ol>
     *   <li>检查当前是否有更高优先级的旋转正在执行</li>
     *   <li>如果优先级足够，接受新的旋转请求</li>
     *   <li>向服务器发送 PlayerMoveC2SPacket 更新玩家朝向</li>
     *   <li>同步客户端摄像机视角，确保视觉一致性</li>
     *   <li>重置内部计时器，开始新的旋转周期</li>
     * </ol>
     *
     * <p>优先级说明：</p>
     * <ul>
     *   <li>数值越大优先级越高</li>
     *   <li>战斗相关操作通常具有最高优先级</li>
     *   <li>建造和种植操作具有中等优先级</li>
     *   <li>移动和导航操作具有较低优先级</li>
     * </ul>
     *
     * @param rotation 要注册的旋转对象，包含目标偏航角、俯仰角和优先级
     * @return {@code true} 如果旋转请求被成功接受并执行；
     * {@code false} 如果当前有更高优先级的旋转正在执行，请求被拒绝
     * @see Rotation#getPriority() 获取旋转优先级
     * @see #sync() 完成旋转后的同步操作
     */
    public boolean register(Rotation rotation) {
        if (this.currentRotation != null && this.currentRotation.getPriority() > rotation.getPriority()) {
            return false;
        } else {
            this.currentRotation = rotation;
            this.timer.reset();
            mc.player
                .networkHandler
                .sendPacket(
                    DV.of(VUtil.class).getFull(
                        mc.player.getX(),
                        mc.player.getY(),
                        mc.player.getZ(),
                        rotation.getYaw(),
                        rotation.getPitch(),
                        mc.player.isOnGround()
                    )
                );
            Rotations.setCamRotation(rotation.getYaw(), rotation.getPitch());
            return true;
        }
    }

    /**
     * 同步并重置旋转状态
     *
     * <p>该方法在完成旋转操作后调用，用于清理旋转状态并确保
     * 客户端和服务器的玩家朝向保持一致。</p>
     *
     * <p>执行操作：</p>
     * <ol>
     *   <li>向服务器发送当前玩家的真实朝向（而非旋转管理器设置的朝向）</li>
     *   <li>清除当前旋转状态，允许新的旋转请求</li>
     *   <li>恢复玩家的自然视角控制</li>
     * </ol>
     *
     * <p>调用时机：</p>
     * <ul>
     *   <li>方块放置操作完成后</li>
     *   <li>攻击动作执行完毕后</li>
     *   <li>交互操作结束后</li>
     *   <li>任何需要精确朝向的操作完成后</li>
     * </ul>
     *
     * <p><strong>重要提醒：</strong></p>
     * <p>每次调用 {@link #register(Rotation)} 后都应该调用此方法，
     * 否则可能导致玩家视角被锁定在特定方向，影响正常游戏体验。</p>
     *
     * @see #register(Rotation) 注册旋转请求
     */
    public void sync() {
        mc.player
            .networkHandler
            .sendPacket(
                DV.of(VUtil.class).getFull(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    mc.player.getYaw(),
                    mc.player.getPitch(),
                    mc.player.isOnGround()
                )
            );
        this.currentRotation = null;
    }
}
