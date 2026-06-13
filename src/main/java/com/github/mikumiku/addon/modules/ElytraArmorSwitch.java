package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.BagUtil;
import com.github.mikumiku.addon.util.MikuUtil;
import com.github.mikumiku.addon.util.Via;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.ArrayList;
import java.util.List;

public class ElytraArmorSwitch extends BaseModule {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final Object2IntMap<RegistryEntry<Enchantment>> enchantments = new Object2IntOpenHashMap();
    private final Setting<Double> delay = this.sgGeneral
        .add(
            new DoubleSetting.Builder()
                .name("切换延迟")
                .description("切换延迟")
                .defaultValue(0.3)
                .min(0.2)
                .range(0.2, 2)
                .build()
        );
    private long lastPressTime = 0L;
    private boolean shouldSneak = false;
    private boolean sneaked = false;

    public ElytraArmorSwitch() {
        super(
            "自动鞘翅切换",
            "在地面时自动穿盔甲，在空中使用鞘翅。当你双击空格时会自动切换鞘翅与盔甲"
        );
    }

    @Override
    public void onActivate() {

        this.shouldSneak = false;
        this.sneaked = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (this.shouldSneak) {
            if (!this.sneaked) {
                mc.options.sneakKey.setPressed(true);
                this.sneaked = true;
            } else if (this.sneaked) {
                this.sneaked = false;
                this.shouldSneak = false;
                mc.options.sneakKey.setPressed(false);
            }
        }

        if (mc.player != null) {
            Item item = mc.player.getInventory().getStack(36 + 2).getItem();
            if (!MikuUtil.isArmor(item)) {
                if (mc.player.isOnGround()
                    && (item == Items.AIR
                    || item == Items.ELYTRA
                )) {
                    int slot = this.getBestArmor();
                    if (slot != -1) {
                        InvUtils.move().from(slot).toArmor(2);
                        Via.sendReleaseShift();
                        this.shouldSneak = true;
                    }
                }
            }
        }
    }

    private int getBestArmor() {
        List<Integer> ArmorSlots = BagUtil.findSolts(input -> MikuUtil.isArmor(input.getItem()));
        if (ArmorSlots.size() == 0) {
            return -1;
        } else {
            List<Integer> CHESTPLATE_Slots = new ArrayList<>();


            // 原来遍历 ArmorSlots 的位置，替换为下面代码：
            for (int i = 0; i < ArmorSlots.size(); i++) {
                ItemStack stack = mc.player.getInventory().getStack(ArmorSlots.get(i));
                Item item = stack.getItem();

                // 如果是盔甲或鞘翅，进一步判断是否为胸甲/鞘翅位
                if (MikuUtil.isArmor(item) || item == Items.ELYTRA) {
                    if (isChestplateItem(item)) {
                        CHESTPLATE_Slots.add(ArmorSlots.get(i));
                    }
                }
            }

            if (CHESTPLATE_Slots.size() == 0) {
                return -1;
            } else {
                int bestArmorSlot = -1;
                int bestArmorScore = 0;

                for (int ix = 0; ix < CHESTPLATE_Slots.size(); ix++) {
                    int score = this.getScore(mc.player.getInventory().getStack(CHESTPLATE_Slots.get(ix)));
                    if (score > bestArmorScore) {
                        bestArmorScore = score;
                        bestArmorSlot = CHESTPLATE_Slots.get(ix);
                    }
                }

                return bestArmorSlot;
            }
        }
    }

    // 在类中添加这个辅助方法（放在 getBestArmor() 和 getScore() 之间合适的位置）
    private boolean isChestplateItem(Item item) {
        return item == Items.NETHERITE_CHESTPLATE
            || item == Items.DIAMOND_CHESTPLATE
            || item == Items.IRON_CHESTPLATE
            || item == Items.CHAINMAIL_CHESTPLATE
            || item == Items.GOLDEN_CHESTPLATE
            || item == Items.LEATHER_CHESTPLATE
            || item == Items.ELYTRA; // 将鞘翅也视为胸部装备以方便切换逻辑
    }

    private int getScore(ItemStack itemStack) {
        Item item = itemStack.getItem();
        if (!(MikuUtil.isArmor(item))) return -1;

        int baseScore = 0;

        // 根据具体物品判定基础防御与材质品质
        if (item == Items.NETHERITE_CHESTPLATE) baseScore = 10;
        else if (item == Items.DIAMOND_CHESTPLATE) baseScore = 9;
        else if (item == Items.GOLDEN_CHESTPLATE) baseScore = 7;
        else if (item == Items.IRON_CHESTPLATE) baseScore = 8;
        else if (item == Items.CHAINMAIL_CHESTPLATE) baseScore = 6;
        else if (item == Items.LEATHER_CHESTPLATE) baseScore = 5;
        else if (item == Items.TURTLE_HELMET) baseScore = 5; // 以防某些特例
        else baseScore = 4; // 其他未知装备

        // 附魔加成
        Utils.getEnchantments(itemStack, this.enchantments);
        int protection = Utils.getEnchantmentLevel(this.enchantments, Enchantments.PROTECTION);
        int blastProt = Utils.getEnchantmentLevel(this.enchantments, Enchantments.BLAST_PROTECTION);
        int fireProt = Utils.getEnchantmentLevel(this.enchantments, Enchantments.FIRE_PROTECTION);
        int projProt = Utils.getEnchantmentLevel(this.enchantments, Enchantments.PROJECTILE_PROTECTION);
        int unbreaking = Utils.getEnchantmentLevel(this.enchantments, Enchantments.UNBREAKING);
        int mending = Utils.getEnchantmentLevel(this.enchantments, Enchantments.MENDING);

        // 每种附魔都为盔甲加分
        int enchantScore = protection + blastProt + fireProt + projProt + unbreaking + mending * 2;

        return baseScore + enchantScore;
    }

    @EventHandler
    private void onKeyEvent(KeyEvent event) {
        if (Via.getKeyEventKey(event) == 32 && event.action == KeyAction.Press) {
            if (this.lastPressTime == 0L) {
                this.lastPressTime = System.currentTimeMillis();
                return;
            }

            long now = System.currentTimeMillis();
            if (now - this.lastPressTime > this.delay.get() * 1000) {
                this.lastPressTime = System.currentTimeMillis();
                return;
            }

            Item item = mc.player.getInventory().getStack(36 + 2).getItem();
            if (Registries.ITEM.getId(item).getPath().equals("elytra")) {
                this.lastPressTime = now;
                return;
            }

            if (mc.player.getInventory().getStack(36 + 2).isEmpty() || MikuUtil.isArmor(item)) {
                if (mc.currentScreen != null) {
                    mc.currentScreen.close();
                }

                if (InvUtils.find(Items.ELYTRA).found()) {
                    int elySlot = InvUtils.find(Items.ELYTRA).slot();
                    InvUtils.move().from(elySlot).toArmor(2);
                    Via.sendReleaseShift();
                }
            }
        }
    }
}
