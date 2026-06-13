package com.github.mikumiku.addon.modules;


import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.BagUtil;
import com.github.mikumiku.addon.util.Via;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class AutoChorusFruit extends BaseModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder()
        .name("落地后关闭")
        .description("落地后自动关闭模块")
        .defaultValue(true)
        .build()
    );


    private boolean wasInAir = false;
    private boolean eating = false;

    public AutoChorusFruit() {
        super("紫颂果降落", "在空中自动吃紫颂果直到落地");
    }


    @Override
    public void onActivate() {
        super.onActivate();
        int slot = BagUtil.findItemInventorySlot(Items.CHORUS_FRUIT);

        if (slot == -1) {
            this.error("没有紫颂果了");
            toggle();
        }

    }

    @Override
    public void onDeactivate() {
        stopEating();
        wasInAir = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        boolean isInAir = !mc.player.isOnGround();

        // 检测是否在空中
        if (isInAir) {
            wasInAir = true;

            if (eating) {
                startEating();
                return;
            }
            // 查找紫颂果
            boolean useItem = findAndUseItem(Items.CHORUS_FRUIT);

            if (!useItem) {
                int slot = BagUtil.findItemInventorySlot(Items.CHORUS_FRUIT);
                if (slot != -1) {
                    BagUtil.doSwap(slot);
                    mc.interactionManager.interactItem(mc.player, mc.player.getActiveHand());
                    startEating();
                } else {
                    stopEating();

                }
            } else {
                startEating();
            }

        } else {
            // 落地了
            if (wasInAir && eating) {
                stopEating();
                wasInAir = false;

                // 如果设置了自动关闭，则关闭模块
                if (autoDisable.get()) {
                    toggle();
                }
            }
        }
    }

    private boolean findAndUseItem(Item item) {
        // 查找并使用物品的实现
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                Via.setSelectedSlot(i);
                return true;
            }
        }
        return false;
    }

    private void startEating() {
        if (!eating) {
            mc.options.useKey.setPressed(true);
            eating = true;
        }
    }

    private void stopEating() {
        if (eating) {
            mc.options.useKey.setPressed(false);
            eating = false;
        }
    }
}
