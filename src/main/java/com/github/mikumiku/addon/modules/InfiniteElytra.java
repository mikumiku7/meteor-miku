package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.VUtil;
import meteordevelopment.meteorclient.events.entity.player.InteractItemEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;

/**
 * source
 * <p>
 * https://github.com/etianl/Trouser-Streak/blob/1.21.4/src/main/java/pwn/noobs/trouserstreak/modules/InfiniteElytra.java
 */
public class InfiniteElytra extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> elytraOnTicks = sgGeneral.add(new IntSetting.Builder()
        .name("鞘翅开启时长")
        .description("保持鞘翅装备状态的游戏刻数。")
        .defaultValue(15)
        .min(1)
        .sliderRange(1, 200)
        .build()
    );

    private final Setting<Integer> elytraOffTicks = sgGeneral.add(new IntSetting.Builder()
        .name("鞘翅关闭时长")
        .description("保持鞘翅卸下状态的游戏刻数。")
        .defaultValue(2)
        .min(1)
        .sliderRange(1, 200)
        .build()
    );
    private final Setting<Boolean> fireRockets = sgGeneral.add(new BoolSetting.Builder()
        .name("自动发射烟花")
        .description("自动使用烟花火箭维持飞行。")
        .defaultValue(true)
        .build()
    );
    private final Setting<Integer> timebetweenfires = sgGeneral.add(new IntSetting.Builder()
        .name("烟花发射间隔")
        .description("两次烟花发射之间的游戏刻数间隔。")
        .defaultValue(40)
        .min(1)
        .sliderRange(1, 200)
        .build()
    );
    private int tickCounter = 0;
    private boolean playerWasFlying = false;
    private boolean glidingTime = false;
    private int timeBetweenRockets = 0;
    private int ticksSinceLastRocket = 0;

    public InfiniteElytra() {
        super("无限耐久鞘翅", "自动切换鞘翅开关以节省耐久度，并自动使用烟花火箭维持飞行。");
    }

    @Override
    public void onActivate() {
        if (mc == null) {
            mc = MinecraftClient.getInstance();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        if (!playerWasFlying) playerWasFlying = VUtil.isFallFlying(mc);
        if (!playerWasFlying) return;

        tickCounter++;

        if (fireRockets.get()) {
            timeBetweenRockets = timebetweenfires.get();
            ticksSinceLastRocket++;
        }

        int totalCycleTicks = elytraOnTicks.get() + elytraOffTicks.get();
        int currentPhaseTick = tickCounter % totalCycleTicks;

        if (currentPhaseTick == 0) {
            tickCounter = 0;
        }

        ItemStack chestStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);

        if (currentPhaseTick < elytraOnTicks.get()) {
            glidingTime = true;
            if (chestStack.getItem() != Items.ELYTRA) {
                for (int i = 0; i < mc.player.getInventory().size(); i++) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    if (stack.getItem() == Items.ELYTRA) {
                        InvUtils.move().from(i).toArmor(2);
                        break;
                    }
                }
            }

            if (chestStack.getItem() == Items.ELYTRA && !mc.player.isOnGround() && !VUtil.isFallFlying(mc)) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            }
        } else {
            glidingTime = false;
            if (chestStack.getItem() == Items.ELYTRA) {
                int emptySlot = -1;
                for (int i = 0; i < 36; i++) {
                    if (mc.player.getInventory().getStack(i).isEmpty()) {
                        emptySlot = i;
                        break;
                    }
                }

                if (emptySlot != -1) {
                    InvUtils.move().fromArmor(2).to(emptySlot);
                }
            }
        }
        if (glidingTime && fireRockets.get()) {
            if (ticksSinceLastRocket >= timeBetweenRockets) {
                int rocketSlot = -1;

                for (int i = 0; i < 9; i++) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    if (stack.getItem() == Items.FIREWORK_ROCKET) {
                        rocketSlot = i;
                        break;
                    }
                }

                if (rocketSlot != -1) {
                    int currentSlot = mc.player.getInventory().selectedSlot;

                    if (rocketSlot != currentSlot) {
                        mc.player.getInventory().selectedSlot = rocketSlot;
                        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                        mc.player.getInventory().selectedSlot = currentSlot;
                    } else {
                        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    }
                }
            }
        }
    }

    @EventHandler
    private void onInteractItem(InteractItemEvent event) {
        if (mc.player == null) return;

        ItemStack stack = mc.player.getStackInHand(event.hand);

        if (stack != null && stack.getItem() == Items.FIREWORK_ROCKET) {
            ticksSinceLastRocket = 0;
        }
    }
}
