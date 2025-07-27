package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.MikuMikuAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

public class OnekeyFireWork extends Module {
    private int delay;
    private int slotBefore;
    private final SettingGroup sgGeneral;
    private final Setting<Integer> closeDelay;

    public OnekeyFireWork() {
        super(MikuMikuAddon.CATEGORY,
            "一键烟花",
            "快捷键一键放烟花"
        );

        this.delay = 0;
        this.slotBefore = mc.player == null ? 0 : mc.player.getInventory().selectedSlot;
        this.sgGeneral = this.settings.getDefaultGroup();
        this.closeDelay = this.sgGeneral
            .add(
                new IntSetting.Builder()
                    .name("关闭延迟")
                    .description("使用烟花后关闭界面的延迟（游戏刻）")
                    .defaultValue(4)
                    .sliderRange(1, 40)
                    .build()
            );
    }

    @Override
    public void onActivate() {
        this.fire();
        this.delay = this.closeDelay.get();
        this.slotBefore = mc.player == null ? 0 : mc.player.getInventory().selectedSlot;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (this.delay == 0) {
            InvUtils.swap(this.slotBefore, false);
            this.toggle();
        } else {
            this.delay--;
        }
    }


    public void fire() {

        try {
            if (!mc.player.isOnGround()) {
                if ("elytra".equals(mc.player.getInventory().getStack(38).getItem().getName())) {
                    if (!(mc.player.getMainHandStack().getItem() instanceof ArmorItem)) {
                        if (mc.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET) {
                            Rotations.rotate(mc.player.getYaw(), mc.player.getPitch(), -100, () ->
                                mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 1, mc.player.getYaw(), mc.player.getPitch())));
                        } else {
                            int firework;
                            if ((firework = InvUtils.findInHotbar(Items.FIREWORK_ROCKET).slot()) != -1) {
                                int old = mc.player.getInventory().selectedSlot;
                                InvUtils.swap(firework, false);
                                Rotations.rotate(mc.player.getYaw(), mc.player.getPitch(), -100, () ->
                                    mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 1, mc.player.getYaw(), mc.player.getPitch())));

                                InvUtils.swap(old, false);
                                mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
                            } else if ((firework = InvUtils.find(Items.FIREWORK_ROCKET).slot()) != -1) {
                                InvUtils.move().from(firework).to(mc.player.getInventory().selectedSlot);
                                Rotations.rotate(mc.player.getYaw(), mc.player.getPitch(), -100, () ->
                                    mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 1, mc.player.getYaw(), mc.player.getPitch())));
                                InvUtils.move().from(firework).to(mc.player.getInventory().selectedSlot);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {

        }
    }
}
