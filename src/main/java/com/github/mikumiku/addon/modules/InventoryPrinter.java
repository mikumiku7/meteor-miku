package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class InventoryPrinter extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // Settings
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("What to print.")
        .defaultValue(Mode.PlayerInventory)
        .build()
    );

    private final Setting<Boolean> includeEmpty = sgGeneral.add(new BoolSetting.Builder()
        .name("include-empty")
        .description("Include empty slots in the output.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> showCount = sgGeneral.add(new BoolSetting.Builder()
        .name("show-count")
        .description("Show item count for each slot.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showSlotType = sgGeneral.add(new BoolSetting.Builder()
        .name("show-slot-type")
        .description("Show slot type information.")
        .defaultValue(false)
        .build()
    );

    // Internal state
    private boolean hasExecuted = false;
    private int delayTicks = 0;


    public InventoryPrinter() {
        super("库存打印", "打印库存槽位信息到聊天栏");
        mc = MinecraftClient.getInstance();
    }

    @Override
    public void onActivate() {
        if (mc.player == null || mc.world == null) {
            error("玩家或世界为空！");
            toggle();
            return;
        }

        hasExecuted = false;
        delayTicks = 0;
        info("库存打印器已激活 - 模式: " + mode.get().name());
    }

    @Override
    public void onDeactivate() {
        info("库存打印器已关闭");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || hasExecuted) return;

        // Wait a few ticks to ensure everything is loaded
        delayTicks++;
        if (delayTicks < 3) return;

        try {
            switch (mode.get()) {
                case PlayerInventory -> printPlayerInventory();
                case CurrentContainer -> printCurrentContainer();
                case Both -> {
                    printPlayerInventory();
                    info("--- 分隔线 ---");
                    printCurrentContainer();
                }
            }
        } catch (Exception e) {
            error("打印库存时出错: " + e.getMessage());
        }

        hasExecuted = true;

        // Auto-close after printing
        info("打印完成，自动关闭模块");
        toggle();
    }

    private void printPlayerInventory() {
        info("=== 玩家库存信息 ===");

        var inventory = mc.player.getInventory();
        int totalSlots = inventory.size();
        int itemCount = 0;

        info("总槽位数: " + totalSlots);

        // Print hotbar (slots 0-8)
        info("--- 快捷栏 (0-8) ---");
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() || includeEmpty.get()) {
                printSlotInfo(i, stack, "快捷栏");
                if (!stack.isEmpty()) itemCount++;
            }
        }

        // Print main inventory (slots 9-35)
        info("--- 主库存 (9-35) ---");
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() || includeEmpty.get()) {
                printSlotInfo(i, stack, "主库存");
                if (!stack.isEmpty()) itemCount++;
            }
        }

        // Print armor slots (slots 36-39)
        info("--- 装备栏 (36-39) ---");
        for (int i = 36; i < 40; i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() || includeEmpty.get()) {
                String armorType = getArmorSlotName(i);
                printSlotInfo(i, stack, armorType);
                if (!stack.isEmpty()) itemCount++;
            }
        }

        // Print offhand slot (slot 40)
        info("--- 副手 (40) ---");
        ItemStack offhandStack = inventory.getStack(40);
        if (!offhandStack.isEmpty() || includeEmpty.get()) {
            printSlotInfo(40, offhandStack, "副手");
            if (!offhandStack.isEmpty()) itemCount++;
        }

        info("玩家库存统计: " + itemCount + " 个非空槽位 / " + totalSlots + " 总槽位");
    }

    private void printCurrentContainer() {
        if (!(mc.currentScreen instanceof HandledScreen)) {
            info("=== 当前容器信息 ===");
            info("没有打开的容器界面");
            return;
        }

        HandledScreen<?> screen = (HandledScreen<?>) mc.currentScreen;
        ScreenHandler handler = screen.getScreenHandler();

        info("=== 当前容器信息 ===");
        info("容器类型: " + screen.getClass().getSimpleName());
        info("总槽位数: " + handler.slots.size());

        int containerSlots = handler.slots.size() - 36; // Exclude player inventory slots
        int itemCount = 0;

        info("--- 容器槽位 (0-" + (containerSlots - 1) + ") ---");
        for (int i = 0; i < containerSlots; i++) {
            if (i < handler.slots.size()) {
                Slot slot = handler.slots.get(i);
                ItemStack stack = slot.getStack();

                if (!stack.isEmpty() || includeEmpty.get()) {
                    String slotInfo = "容器";
                    if (showSlotType.get()) {
                        slotInfo += " [" + slot.getClass().getSimpleName() + "]";
                    }
                    printSlotInfo(i, stack, slotInfo);
                    if (!stack.isEmpty()) itemCount++;
                }
            }
        }

        info("--- 玩家库存部分 (" + containerSlots + "-" + (handler.slots.size() - 1) + ") ---");
        for (int i = containerSlots; i < handler.slots.size(); i++) {
            if (i < handler.slots.size()) {
                Slot slot = handler.slots.get(i);
                ItemStack stack = slot.getStack();

                if (!stack.isEmpty() || includeEmpty.get()) {
                    String slotInfo = "玩家库存";
                    if (showSlotType.get()) {
                        slotInfo += " [" + slot.getClass().getSimpleName() + "]";
                    }
                    printSlotInfo(i, stack, slotInfo);
                }
            }
        }

        info("容器统计: " + itemCount + " 个非空槽位 / " + containerSlots + " 容器槽位");
    }

    private void printSlotInfo(int slotIndex, ItemStack stack, String slotType) {
        StringBuilder info = new StringBuilder();
        info.append("槽位 ").append(slotIndex).append(" [").append(slotType).append("]: ");

        if (stack.isEmpty()) {
            info.append("空");
        } else {
            info.append(stack.getItem().getName().getString());

            if (showCount.get()) {
                info.append(" x").append(stack.getCount());
            }

            // Add additional item info if available
            if (!stack.getComponents().isEmpty()) {
                info.append(" (有数据)");
            }

            if (stack.isDamaged()) {
                info.append(" (耐久: ").append(stack.getMaxDamage() - stack.getDamage())
                    .append("/").append(stack.getMaxDamage()).append(")");
            }
        }

        info(info.toString());
    }

    private String getArmorSlotName(int slot) {
        return switch (slot) {
            case 36 -> "靴子";
            case 37 -> "护腿";
            case 38 -> "胸甲";
            case 39 -> "头盔";
            default -> "装备";
        };
    }

    public enum Mode {
        PlayerInventory("玩家库存"),
        CurrentContainer("当前容器"),
        Both("两者都打印");

        private final String displayName;

        Mode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
