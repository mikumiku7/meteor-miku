package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.dynamic.DV;
import com.github.mikumiku.addon.util.PlayerUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

import java.util.concurrent.atomic.AtomicInteger;

public class AutoXP extends BaseModule {

    // 设置组
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // 多任务设置
    private final Setting<Boolean> multiTask = sgGeneral.add(new BoolSetting.Builder()
        .name("多任务")
        .description("在使用物品时也允许丢经验瓶")
        .defaultValue(false)
        .build());

    // 延迟设置
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("延迟")
        .description("丢经验瓶之间的延迟（tick）")
        .defaultValue(1)
        .min(1)
        .sliderMax(20)
        .build());

    // 每次丢瓶数量
    private final Setting<Integer> bottlesPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("每次丢瓶数量")
        .description("每个tick丢出的经验瓶数量")
        .defaultValue(1)
        .min(1)
        .sliderMax(64)
        .build());

    // 耐久度检查
    private final Setting<Boolean> durabilityCheck = sgGeneral.add(new BoolSetting.Builder()
        .name("耐久度检查")
        .description("检查装备和手持物品的耐久度，如果满耐久则自动禁用")
        .defaultValue(true)
        .build());

    // 旋转设置
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("旋转")
        .description("丢经验瓶时旋转玩家")
        .defaultValue(false)
        .build());

    // 挥手设置
    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
        .name("挥手")
        .description("丢经验瓶时挥手")
        .defaultValue(false)
        .build());

    // 仅在满耐久时启用
    private final Setting<Boolean> onlyFullDurability = sgGeneral.add(new BoolSetting.Builder()
        .name("仅满耐久时启用")
        .description("只有在装备满耐久时才启用丢经验瓶")
        .defaultValue(false)
        .build());

    // 内部状态
    private int delayTimer = 0;
    private AtomicInteger xpBottleCount = new AtomicInteger(0);

    public AutoXP() {
        super("自动丢XP", "自动丢经验瓶修装备");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        delayTimer = 0;
        updateXPBottleCount();
    }

    @Override
    public String getInfoString() {
        return String.valueOf(xpBottleCount.get());
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        // 延迟检查
        if (delayTimer > 0) {
            delayTimer--;
            return;
        }

        // 多任务检查
        if (mc.player.isUsingItem() && !multiTask.get()) {
            return;
        }

        // 更新经验瓶数量
        updateXPBottleCount();

        // 检查是否有经验瓶
        if (xpBottleCount.get() <= 0) {
            error("没有经验瓶，禁用模块");
            toggle();
            return;
        }

        // 耐久度检查
        if (durabilityCheck.get()) {
            boolean itemsFullDurability = areItemsFullDurability();

            if (onlyFullDurability.get()) {
                // 仅在满耐久时启用模式
                if (!itemsFullDurability) {
                    return;
                }
            } else {
                // 满耐久时禁用模式
                if (itemsFullDurability) {
                    info("所有装备耐久度已满，禁用模块");
                    toggle();
                    return;
                }
            }
        }

        // 查找经验瓶
        int slot = findXPBottleSlot();
        if (slot == -1) {
            error("没有找到经验瓶，禁用模块");
            toggle();
            return;
        }

        // 切换到经验瓶
        if (slot != PlayerInventory.getHotbarSize() - 1) {
            DV.of(PlayerUtil.class).setSelectedSlot(mc.player.getInventory(), slot);
        }

        // 旋转
        if (rotate.get()) {
            mc.player.setYaw(mc.player.getYaw());
            mc.player.setPitch(90.0f);
        }

        // 丢经验瓶
        throwXPBottles();

        // 重置延迟
        delayTimer = delay.get();
    }

    /**
     * 更新经验瓶数量
     */
    private void updateXPBottleCount() {
        int count = 0;
        for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof ExperienceBottleItem) {
                count += stack.getCount();
            }
        }
        xpBottleCount.set(count);
    }

    /**
     * 查找经验瓶槽位
     */
    private int findXPBottleSlot() {
        for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof ExperienceBottleItem) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 丢经验瓶
     */
    private void throwXPBottles() {
        int bottlesToThrow = Math.min(bottlesPerTick.get(), xpBottleCount.get());

        for (int i = 0; i < bottlesToThrow; i++) {
            // 发送使用物品包
            BaseModule.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            // 挥手
            if (swing.get()) {
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    /**
     * 检查所有物品是否满耐久
     */
    private boolean areItemsFullDurability() {
        // 检查主手和副手
        if (!isItemFullDurability(mc.player.getMainHandStack()) ||
            !isItemFullDurability(mc.player.getOffHandStack())) {
            return false;
        }

        // 检查盔甲
        for (ItemStack stack : DV.of(PlayerUtil.class).getArmor(mc.player.getInventory())) {
            if (!isItemFullDurability(stack)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查单个物品是否满耐久
     */
    private boolean isItemFullDurability(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        // 检查是否有经验修补附魔
        boolean hasMending = stack.hasEnchantments() &&
            stack.getEnchantments().toString().contains(Enchantments.MENDING.toString().split(" ")[0]);

        // 如果没有经验修补，认为不需要修复
        if (!hasMending) {
            return true;
        }

        int maxDamage = stack.getMaxDamage();
        int currentDamage = stack.getDamage();

        // 检查是否满耐久（损伤为0或物品无法损坏）
        return currentDamage == 0 || maxDamage == 0;
    }
}
