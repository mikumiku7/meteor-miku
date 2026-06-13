package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.BagUtil;
import com.github.mikumiku.addon.util.Via;
import meteordevelopment.meteorclient.events.world.PlaySoundEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * 甲飞++ - 使用胸甲飞行，不消耗鞘翅耐久
 * 实现 LAZY 模式: 拦截服务器的停止滑翔信号，保持客户端滑翔状态
 */
public class ElytraFlyPlusPlus extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> autoRocket = sgGeneral.add(new BoolSetting.Builder()
        .name("自动烟花")
        .description("飞行时自动使用烟花火箭加速。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> rocketDelay = sgGeneral.add(new IntSetting.Builder()
        .name("烟花间隔")
        .description("两次烟花发射之间的最小间隔（tick数）。")
        .defaultValue(40)
        .min(10)
        .sliderRange(10, 100)
        .build()
    );

    private final Setting<Boolean> cancelSounds = sgGeneral.add(new BoolSetting.Builder()
        .name("静音")
        .description("取消装备声音。")
        .defaultValue(true)
        .build()
    );

    // 胸甲物品列表（不含鞘翅）
    private static final List<Item> CHESTPLATES = List.of(
        Items.NETHERITE_CHESTPLATE,
        Items.DIAMOND_CHESTPLATE,
        Items.GOLDEN_CHESTPLATE,
        Items.IRON_CHESTPLATE,
        Items.CHAINMAIL_CHESTPLATE,
        Items.LEATHER_CHESTPLATE
    );

    // 需要取消的声音
    private static final List<Identifier> CANCEL_SOUNDS = List.of(
        Identifier.of("minecraft:item.armor.equip_generic"),
        Identifier.of("minecraft:item.armor.equip_netherite"),
        Identifier.of("minecraft:item.armor.equip_elytra"),
        Identifier.of("minecraft:item.armor.equip_diamond"),
        Identifier.of("minecraft:item.armor.equip_gold"),
        Identifier.of("minecraft:item.armor.equip_iron"),
        Identifier.of("minecraft:item.armor.equip_chain"),
        Identifier.of("minecraft:item.armor.equip_leather"),
        Identifier.of("minecraft:item.elytra.flying")
    );

    // 状态变量
    private boolean wasSprinting = false;
    private int globalTickCounter = 0;
    private int lastRocketTick = 0;
    private int flyStartTick = 0;

    public ElytraFlyPlusPlus() {
        super(
            CATEGORY_MIKU_PRO,
            "甲飞++",
            "使用胸甲飞行，不消耗鞘翅耐久。需在快捷栏放置胸甲。"
        );
    }

    @Override
    public void onActivate() {
        super.onActivate();
        if (mc.player == null || mc.player.getAbilities().allowFlying) return;

        wasSprinting = mc.player.isSprinting();
        globalTickCounter = 0;
        lastRocketTick = 0;
        flyStartTick = 0;

        // 确保胸甲在快捷栏
        ensureChestplateInHotbar();

        // 如果在地面，先跳起来
        if (mc.player.isOnGround()) {
            mc.player.jump();
        }
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        if (mc.player == null) return;

        // 恢复冲刺状态
        mc.player.setSprinting(wasSprinting);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.player.getAbilities().allowFlying) return;
        if (mc.player.isOnGround()) return;

        globalTickCounter++;

        // 核心 LAZY 模式逻辑
        doLazyArmorFly();

        // 保持冲刺状态
        if (Via.isFallFlying(mc)) {
            mc.player.setSprinting(true);
        }

        // 自动烟花
        if (autoRocket.get()) {
            Modules.get().get(OnekeyFireWork.class).toggle();
        }
    }

    @EventHandler
    private void onPlaySound(PlaySoundEvent event) {
        if (!cancelSounds.get()) return;

        for (Identifier id : CANCEL_SOUNDS) {
            if (id.equals(event.sound.getId())) {
                event.cancel();
                return;
            }
        }
    }

    /**
     * LAZY 模式核心逻辑
     * 1. 如果没有在飞行，装备胸甲并发送开始滑翔包
     * 2. 如果已经在飞行，定期发送开始滑翔包保持状态
     */
    private void doLazyArmorFly() {
        // 检查是否可以继续滑翔
        if (!canContinueGliding()) return;

        // 查找快捷栏中的胸甲
        int chestplateSlot = findChestplateInHotbar();
        if (chestplateSlot == -1) return;

        if (!Via.isFallFlying(mc)) {
            // 没有在飞行 - 装备胸甲并开始飞行
            equipChestplate(chestplateSlot);
            sendStartFallFlying();
            flyStartTick = globalTickCounter;
        } else {
            // 已经在飞行 - 定期发送开始滑翔包保持状态
            // 每 40 tick 发送一次（参考 ElytraExtra 的实现）
            if (globalTickCounter - flyStartTick >= 40) {
                sendStartFallFlying();
                flyStartTick = globalTickCounter;
            }
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

    /**
     * 确保胸甲在快捷栏中
     */
    private void ensureChestplateInHotbar() {
        // 检查快捷栏是否已有胸甲
        if (findChestplateInHotbar() != -1) return;

        // 在背包中查找胸甲
        int invSlot = findChestplateInInventory();
        if (invSlot == -1) {
            error("未找到可用胸甲！");
            return;
        }

        // 查找快捷栏空位
        int hotbarSlot = findEmptyHotbarSlot();
        if (hotbarSlot == -1) {
            hotbarSlot = 0;
        }

        // 交换到快捷栏
        BagUtil.inventorySwap(invSlot, hotbarSlot + 36);
    }

    /**
     * 装备胸甲到胸甲槽
     */
    private void equipChestplate(int hotbarSlot) {
        mc.interactionManager.clickSlot(
            mc.player.currentScreenHandler.syncId,
            hotbarSlot + 36,
            6,
            SlotActionType.SWAP,
            mc.player
        );
    }

    /**
     * 在快捷栏查找胸甲
     */
    private int findChestplateInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isChestplate(stack)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 在背包中查找胸甲（不含快捷栏）
     */
    private int findChestplateInInventory() {
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isChestplate(stack)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 查找快捷栏空位
     */
    private int findEmptyHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 判断是否为可用的胸甲
     */
    private boolean isChestplate(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (Item item : CHESTPLATES) {
            if (stack.isOf(item)) return true;
        }
        return false;
    }

    /**
     * 检查是否正在装甲飞行（供外部调用）
     * 注意：不能调用 isFallFlying()，会导致与 LivingEntityMixin 死循环
     */
    public boolean enabled() {
        return this.isActive() && mc.player != null && !mc.player.isOnGround();
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

}
