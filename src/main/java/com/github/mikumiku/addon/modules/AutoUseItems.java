package com.github.mikumiku.addon.modules;

import baritone.api.BaritoneAPI;
import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.MikuMikuAddon;
import com.github.mikumiku.addon.dynamic.DV;
import com.github.mikumiku.addon.util.PlayerUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.AnchorAura;
import meteordevelopment.meteorclient.systems.modules.combat.BedAura;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AutoUseItems extends BaseModule {
    @SuppressWarnings("unchecked")
    private static final Class<? extends Module>[] AURAS = new Class[]{KillAura.class, CrystalAura.class, AnchorAura.class, BedAura.class};

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgHealth = settings.createGroup("生命值触发");
    private final SettingGroup sgHunger = settings.createGroup("饥饿值触发");
    private final SettingGroup sgTimed = settings.createGroup("定时触发");
    private final SettingGroup sgRefill = settings.createGroup("自动补充");

    // 通用设置
    private final Setting<Integer> useDelay = sgGeneral.add(new IntSetting.Builder()
        .name("使用延迟")
        .description("使用物品之间的延迟（游戏刻）")
        .defaultValue(20)
        .min(1)
        .max(200)
        .build()
    );

    private final Setting<Boolean> pauseOnUse = sgGeneral.add(new BoolSetting.Builder()
        .name("使用时暂停其他动作")
        .description("使用物品时暂停其他动作")
        .defaultValue(true)
        .build()
    );

    // 生命值触发设置
    private final Setting<Boolean> healthTrigger = sgHealth.add(new BoolSetting.Builder()
        .name("启用生命值触发")
        .description("当生命值低于阈值时自动使用治疗物品")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> healthThreshold = sgHealth.add(new IntSetting.Builder()
        .name("生命值阈值")
        .description("触发使用治疗物品的生命值")
        .defaultValue(10)
        .min(1)
        .max(20)
        .build()
    );

    private final Setting<List<Item>> healthItems = sgHealth.add(new ItemListSetting.Builder()
        .name("治疗物品")
        .description("用于恢复生命值的物品")
        .defaultValue(Arrays.asList(
            Items.GOLDEN_APPLE,
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.COOKED_BEEF,
            Items.COOKED_PORKCHOP
        ))
        .build()
    );

    // 饥饿值触发设置
    private final Setting<Boolean> hungerTrigger = sgHunger.add(new BoolSetting.Builder()
        .name("启用饥饿值触发")
        .description("当饥饿值低于阈值时自动使用食物")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> hungerThreshold = sgHunger.add(new IntSetting.Builder()
        .name("饥饿值阈值")
        .description("触发使用食物的饥饿值")
        .defaultValue(14)
        .min(1)
        .max(20)
        .build()
    );

    private final Setting<List<Item>> hungerItems = sgHunger.add(new ItemListSetting.Builder()
        .name("食物物品")
        .description("用于恢复饥饿值的物品")
        .defaultValue(Arrays.asList(
            Items.COOKED_BEEF,
            Items.COOKED_PORKCHOP,
            Items.BREAD,
            Items.GOLDEN_CARROT
        ))
        .build()
    );

    // 定时触发设置
    private final Setting<Boolean> timedTrigger = sgTimed.add(new BoolSetting.Builder()
        .name("启用定时触发")
        .description("定时自动使用指定物品")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> timedInterval = sgTimed.add(new IntSetting.Builder()
        .name("定时间隔（秒）")
        .description("定时使用物品的间隔（秒）")
        .defaultValue(60)
        .min(1)
        .max(3600)
        .build()
    );

    private final Setting<List<Item>> timedItems = sgTimed.add(new ItemListSetting.Builder()
        .name("定时物品")
        .description("定时使用的物品")
        .defaultValue(Collections.singletonList(Items.OMINOUS_BOTTLE))
        .build()
    );

    // 自动补充设置
    private final Setting<Boolean> autoRefill = sgRefill.add(new BoolSetting.Builder()
        .name("自动补充快捷栏")
        .description("自动从背包补充快捷栏中的消耗品")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> refillThreshold = sgRefill.add(new IntSetting.Builder()
        .name("补充阈值")
        .description("当物品数量低于此值时进行补充")
        .defaultValue(1)
        .min(1)
        .max(64)
        .build()
    );

    // 内部变量
    private int useTimer = 0;
    private int timedTimer = 0;
    private int prevSlot = -1;
    private boolean isUsing = false;
    private final List<Class<? extends Module>> wasAura = new ArrayList<>();
    private boolean wasBaritone;

    public AutoUseItems() {
        super(MikuMikuAddon.CATEGORY, "自动使用物品", "自动使用物品，支持多种触发条件");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        useTimer = 0;
        timedTimer = 0;
        isUsing = false;
        info("自动使用物品模块已启动");
    }

    @Override
    public void onDeactivate() {
        if (isUsing) {
            stopUsing();
        }
        info("自动使用物品模块已关闭");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // 更新计时器
        if (useTimer > 0) {
            useTimer--;
        }
        timedTimer++;

        // 如果正在使用物品且还没完成，继续等待
        if (isUsing && mc.player.isUsingItem()) return;
        if (isUsing && !mc.player.isUsingItem()) {
            stopUsing();
        }

        // 检查是否可以使用物品
        if (useTimer > 0) return;

        // 自动补充快捷栏
        if (autoRefill.get()) {
            refillHotbar();
        }

        // 生命值触发
        if (healthTrigger.get() && shouldUseHealthItem()) {
            Item item = findBestItem(healthItems.get());
            if (item != null) {
                useItem(item);
                return;
            }
        }

        // 饥饿值触发
        if (hungerTrigger.get() && shouldUseHungerItem()) {
            Item item = findBestItem(hungerItems.get());
            if (item != null) {
                useItem(item);
                return;
            }
        }

        // 定时触发
        if (timedTrigger.get() && shouldUseTimedItem()) {
            Item item = findBestItem(timedItems.get());
            if (item != null) {
                useItem(item);
                timedTimer = 0;
            }
        }
    }

    private boolean shouldUseHealthItem() {
        return mc.player.getHealth() < healthThreshold.get();
    }

    private boolean shouldUseHungerItem() {
        return mc.player.getHungerManager().getFoodLevel() < hungerThreshold.get();
    }

    private boolean shouldUseTimedItem() {
        return timedTimer >= timedInterval.get() * 20; // 转换为游戏刻
    }

    private Item findBestItem(List<Item> items) {
        for (Item item : items) {
            FindItemResult result = InvUtils.findInHotbar(item);
            if (result.found()) {
                return item;
            }
        }
        return null;
    }

    private void useItem(Item item) {
        FindItemResult result = InvUtils.findInHotbar(item);
        if (!result.found()) return;

        prevSlot = DV.of(PlayerUtil.class).getSelectedSlot(mc.player.getInventory());
        InvUtils.swap(result.slot(), false);

        // 暂停光环和Baritone
        pauseModules();

        mc.options.useKey.setPressed(true);
        if (!mc.player.isUsingItem()) {
            Utils.rightClick();
        }

        isUsing = true;
        useTimer = useDelay.get();

        info("使用物品: %s", item.getName().getString());
    }

    private void stopUsing() {
        mc.options.useKey.setPressed(false);
        if (prevSlot != -1) {
            InvUtils.swap(prevSlot, false);
            prevSlot = -1;
        }
        isUsing = false;

        // 恢复光环和Baritone
        resumeModules();
    }

    private void refillHotbar() {
        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            ItemStack hotbarStack = mc.player.getInventory().getStack(hotbarSlot);

            if (hotbarStack.isEmpty()) continue;
            if (hotbarStack.getCount() >= refillThreshold.get()) continue;

            // 在背包中寻找相同物品
            for (int invSlot = 9; invSlot < mc.player.getInventory().size(); invSlot++) {
                ItemStack invStack = mc.player.getInventory().getStack(invSlot);

                if (invStack.isEmpty()) continue;
                if (!ItemStack.areItemsEqual(hotbarStack, invStack)) continue;

                // 执行补充
                InvUtils.move().from(invSlot).to(hotbarSlot);
                info("补充快捷栏: %s", hotbarStack.getItem().getName().getString());
                return; // 一次只补充一个物品
            }
        }
    }

    private void pauseModules() {
        wasAura.clear();

        // 暂停光环
        if (pauseOnUse.get()) {
            for (Class<? extends Module> klass : AURAS) {
                Module module = Modules.get().get(klass);
                if (module.isActive()) {
                    wasAura.add(klass);
                    module.toggle();
                }
            }
        }

        // 暂停Baritone
        wasBaritone = false;
        try {
            if (pauseOnUse.get() && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
                wasBaritone = true;
                BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause");
            }
        } catch (Exception e) {
            info("error:" + e);
        }
    }

    private void resumeModules() {
        // 恢复光环
        if (pauseOnUse.get()) {
            for (Class<? extends Module> klass : AURAS) {
                Module module = Modules.get().get(klass);
                if (wasAura.contains(klass) && !module.isActive()) {
                    module.toggle();
                }
            }
        }
        try {

            // 恢复Baritone
            if (pauseOnUse.get() && wasBaritone) {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume");
            }
        } catch (Exception e) {
            info("error:" + e);
        }
    }


}
