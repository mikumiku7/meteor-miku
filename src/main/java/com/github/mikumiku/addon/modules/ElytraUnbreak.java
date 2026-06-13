package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.Via;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;

/**
 * 无限耐久鞘翅模块
 * 通过在飞行期间自动切换鞘翅来防止耐久度消耗
 * <p>
 * 参考实现: ElytraExtra.java
 * 原始来源: https://github.com/etianl/Trouser-Streak/blob/1.21.4/src/main/java/pwn/noobs/trouserstreak/modules/InfiniteElytra.java
 */
public class ElytraUnbreak extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> period = sgGeneral.add(new IntSetting.Builder()
        .name("切换周期")
        .description("鞘翅装备/卸下的切换周期（tick数）。")
        .defaultValue(16)
        .min(1)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<Boolean> autoRocket = sgGeneral.add(new BoolSetting.Builder()
        .name("自动发射烟花")
        .description("飞行时自动使用烟花火箭维持飞行。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> rocketDelay = sgGeneral.add(new IntSetting.Builder()
        .name("烟花发射间隔")
        .description("两次烟花发射之间的最小间隔（tick数）。")
        .defaultValue(40)
        .min(10)
        .sliderRange(10, 100)
        .build()
    );

    private final Setting<Boolean> antiKick = sgGeneral.add(new BoolSetting.Builder()
        .name("防踢飞")
        .description("在无法继续滑翔时发送跳跃包防止被踢。")
        .defaultValue(true)
        .build()
    );

    // 状态变量
    private int tickCounter = 0;
    private int globalTickCounter = 0;
    private boolean wasFallFlying = false;
    private int lastRocketTick = 0;
    private boolean nextTickShouldStartFly = false;

    public ElytraUnbreak() {
        super("无限耐久鞘翅", "通过自动切换鞘翅来防止耐久度消耗，并可选自动使用烟花火箭。");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        resetState();
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        resetState();
    }

    private void resetState() {
        tickCounter = 0;
        globalTickCounter = 0;
        wasFallFlying = false;
        lastRocketTick = 0;
        nextTickShouldStartFly = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        globalTickCounter++;
        boolean isFlying = Via.isFallFlying(mc);

        // 检测是否刚开始滑翔
        if (isFlying && !wasFallFlying) {
            tickCounter = 0;
        }
        wasFallFlying = isFlying;

        if (!isFlying) {
            // 不在滑翔时，检查是否需要自动装备鞘翅开始滑翔
            if (nextTickShouldStartFly) {
                nextTickShouldStartFly = false;
                if (canContinueGliding() && !mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
                    equipElytra();
                    sendStartFallFlying();
                }
            }
            return;
        }

        // 正在滑翔中
        tickCounter++;

        // 检查是否需要切换鞘翅
        if (shouldSwitchElytra()) {
            performElytraSwitch();
        }

        // 自动发射烟花
        if (autoRocket.get()) {
            Modules.get().get(OnekeyFireWork.class).toggle();
        }
    }

    /**
     * 检查是否应该切换鞘翅状态
     */
    private boolean shouldSwitchElytra() {
        return tickCounter >= period.get();
    }

    /**
     * 执行鞘翅切换操作
     * 核心逻辑：卸下鞘翅 -> 下一tick重新装备 -> 发送开始滑翔包
     */
    private void performElytraSwitch() {
        if (!canContinueGliding()) {
            if (antiKick.get()) {
                // 无法继续滑翔时，发送跳跃防止被踢
                mc.player.jump();
            }
            return;
        }

        ItemStack chestStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);

        // 当前装备的是鞘翅，需要卸下
        if (chestStack.isOf(Items.ELYTRA)) {
            unequipElytra();
            // 标记下一tick需要重新装备并开始滑翔
            nextTickShouldStartFly = true;
        }

        tickCounter = 0;
    }

    /**
     * 装备鞘翅到胸甲槽
     */
    private void equipElytra() {
        // 查找可用的鞘翅
        int elytraSlot = findElytra();
        if (elytraSlot == -1) return;

        // 如果鞘翅不在快捷栏，先移到快捷栏
        if (elytraSlot >= 9) {
            // 找一个空的快捷栏位置或非鞘翅位置
            int targetSlot = findSwapSlot(elytraSlot);
            if (targetSlot != -1) {
                InvUtils.move().from(elytraSlot).to(targetSlot);
                // 装备到胸甲槽
                InvUtils.move().from(targetSlot).toArmor(2);
            }
        } else {
            // 直接装备到胸甲槽
            InvUtils.move().from(elytraSlot).toArmor(2);
        }
    }

    /**
     * 卸下鞘翅到背包
     */
    private void unequipElytra() {
        // 查找空的背包位置
        int emptySlot = findEmptySlot();
        if (emptySlot != -1) {
            InvUtils.move().fromArmor(2).to(emptySlot);
        }
    }

    /**
     * 在背包中查找可用的鞘翅
     */
    private int findElytra() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(Items.ELYTRA)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 查找空的背包位置
     */
    private int findEmptySlot() {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 查找可以交换的快捷栏位置
     */
    private int findSwapSlot(int excludeSlot) {
        // 优先找空位
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty() && i != excludeSlot) {
                return i;
            }
        }
        // 找非鞘翅的位置
        for (int i = 0; i < 9; i++) {
            if (!mc.player.getInventory().getStack(i).isOf(Items.ELYTRA) && i != excludeSlot) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 发送开始滑翔数据包
     */
    private void sendStartFallFlying() {
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(
                new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING)
            );
        }
    }

    /**
     * 检查是否可以继续滑翔
     */
    private boolean canContinueGliding() {
        if (mc.player == null) return false;

        // 基本条件检查
        if (mc.player.isOnGround() || mc.player.getAbilities().flying || mc.player.hasVehicle()) {
            return false;
        }

        // 液体检查
        if (mc.player.isTouchingWater() || mc.player.isInLava()) {
            return false;
        }

        // 效果检查
        if (mc.player.hasStatusEffect(StatusEffects.LEVITATION)) {
            return false;
        }

        return true;
    }


}
