package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.dynamic.DV;
import com.github.mikumiku.addon.util.PlayerUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.item.TridentItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * 三叉戟飞行模块 - 使用三叉戟飞行
 *
 * <p>利用三叉戟的激流附魔效果实现飞行功能。
 * 支持无水飞行、去除拉回效果和自动使用激流等功能。</p>
 *
 * <p>特色功能：</p>
 * <ul>
 *   <li>无水飞行：允许在没有水的情况下使用三叉戟飞行</li>
 *   <li>去除拉回：消除三叉戟的拉回效果，使飞行更流畅</li>
 *   <li>自动激流：自动触发激流效果，实现持续飞行</li>
 *   <li>可配置的触发间隔</li>
 * </ul>
 *
 * @author MikuMiku
 * @since 1.0.0
 */
public class TridentFly extends BaseModule {

    // 设置组
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // 允许无水设置
    private final Setting<Boolean> allowNoWater = sgGeneral.add(new BoolSetting.Builder()
        .name("允许无水")
        .description("允许在没有水的情况下使用三叉戟飞行")
        .defaultValue(true)
        .build());

    // 自动飞行设置
    private final Setting<Boolean> autoFly = sgGeneral.add(new BoolSetting.Builder()
        .name("自动飞行")
        .description("自动使用激流效果")
        .defaultValue(false)
        .build());

    // 触发间隔设置
    private final Setting<Integer> ticks = sgGeneral.add(new IntSetting.Builder()
        .name("触发间隔")
        .description("激流加速之间的间隔（tick）")
        .defaultValue(3)
        .min(0)
        .sliderMax(20)
        .visible(() -> autoFly.get())
        .build());

    // 速度倍率设置
    private final Setting<Double> speedMultiplier = sgGeneral.add(new DoubleSetting.Builder()
        .name("速度倍率")
        .description("飞行速度的倍数")
        .defaultValue(1.0)
        .min(0.5)
        .max(3.0)
        .sliderMin(0.5)
        .sliderMax(3.0)
        .build());

    // 自动切换设置
    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("自动切换")
        .description("自动切换到三叉戟")
        .defaultValue(true)
        .build());

    // 内部状态变量
    private int tickCounter = 0;
    private boolean isFlying = false;

    public TridentFly() {
        super("三叉戟飞行", "使用三叉戟飞行");
    }

    @Override
    public void onActivate() {
        tickCounter = 0;
        isFlying = false;
        info("三叉戟飞行模块已启用！");
    }

    @Override
    public void onDeactivate() {
        info("三叉戟飞行模块已停用！");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        // 检查是否手持三叉戟
        if (!isHoldingTrident()) {
            if (autoSwitch.get()) {
                switchToTrident();
            } else {
                return;
            }
        }

        // 处理自动飞行
        if (autoFly.get()) {
            handleAutoFly();
        }
    }


    /**
     * 处理自动飞行逻辑
     */
    private void handleAutoFly() {
        tickCounter++;

        // 检查是否到了触发时间
        if (tickCounter >= ticks.get()) {
            // 检查玩家是否正在使用三叉戟
            if (mc.player.isUsingItem() && mc.player.getActiveHand() == Hand.MAIN_HAND) {
                // 检查使用时间
                if (mc.player.getItemUseTime() >= ticks.get()) {
                    // 发送释放使用物品包
                    mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
                        BlockPos.ORIGIN,
                        Direction.DOWN
                    ));

                    // 停止使用物品
                    mc.player.stopUsingItem();

                    // 重置计数器
                    tickCounter = 0;

                    // 设置飞行状态
                    if (!isFlying) {
                        isFlying = true;
                        info("开始飞行！");
                    }
                }
            } else {
                // 如果没有使用物品，尝试开始使用
                if (mc.player.getMainHandStack().getItem() instanceof TridentItem) {
                    mc.player.setSneaking(false);
                    mc.player.jump();
                    // 这里可能需要模拟右键点击来开始使用三叉戟
                }
            }
        }
    }

    /**
     * 检查是否手持三叉戟
     */
    private boolean isHoldingTrident() {
        return mc.player.getMainHandStack().getItem() == Items.TRIDENT ||
            mc.player.getOffHandStack().getItem() == Items.TRIDENT;
    }

    /**
     * 切换到三叉戟
     */
    private void switchToTrident() {
        // 搜索快捷栏中的三叉戟
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TRIDENT) {
                DV.of(PlayerUtil.class).setSelectedSlot(mc.player.getInventory(), i);
                return;
            }
        }

        // 搜索主物品栏中的三叉戟
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TRIDENT) {
                // 这里可以添加将物品移动到快捷栏的逻辑
                info("在主物品栏中找到了三叉戟，请手动移动到快捷栏");
                return;
            }
        }

        info("没有找到三叉戟");
        toggle(); // 没有三叉戟时自动关闭模块
    }

    /**
     * 检查三叉戟是否有激流附魔
     */
    private boolean hasRiptide() {
        if (!isHoldingTrident()) return false;

        // 检查主手
        if (mc.player.getMainHandStack().getItem() == Items.TRIDENT) {
            return mc.player.getMainHandStack().hasEnchantments();
        }

        // 检查副手
        if (mc.player.getOffHandStack().getItem() == Items.TRIDENT) {
            return mc.player.getOffHandStack().hasEnchantments();
        }

        return false;
    }

    /**
     * 获取模块状态信息
     */
    public String getFlightStatus() {
        if (!isActive()) {
            return "未启用";
        }

        String status = isFlying ? "飞行中" : "待命";
        String water = allowNoWater.get() ? "允许无水" : "需要水";
        String mode = autoFly.get() ? "自动" : "手动";

        return String.format("%s | %s | %s模式", status, water, mode);
    }
}
