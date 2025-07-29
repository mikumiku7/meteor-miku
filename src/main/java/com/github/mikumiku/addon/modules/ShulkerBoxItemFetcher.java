package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.MikuMikuAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class ShulkerBoxItemFetcher extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSettings = settings.createGroup("Settings");

    // Settings
    private final Setting<Item> targetItem = sgGeneral.add(new ItemSetting.Builder()
        .name("target-item")
        .description("The item to fetch from shulker boxes.")
        .defaultValue(Items.COBBLESTONE)
        .build()
    );

    private final Setting<Integer> delay = sgSettings.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay between actions in ticks.")
        .defaultValue(5)
        .min(1)
        .max(20)
        .build()
    );

    private final Setting<Boolean> autoClose = sgSettings.add(new BoolSetting.Builder()
        .name("auto-close")
        .description("Automatically close the module when finished.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> logActions = sgSettings.add(new BoolSetting.Builder()
        .name("log-actions")
        .description("Log actions to chat.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> debugMode = sgSettings.add(new BoolSetting.Builder()
        .name("debug-mode")
        .description("Enable detailed debug logging.")
        .defaultValue(false)
        .build()
    );

    // Internal state
    private enum State {
        SEARCHING_SHULKER,
        PLACING_SHULKER,
        OPENING_SHULKER,
        EXTRACTING_ITEMS,
        CLOSING_CONTAINER,
        BREAKING_SHULKER,
        PICKING_UP_SHULKER,
        FINISHED
    }

    private State currentState = State.SEARCHING_SHULKER;
    private int tickCounter = 0;
    private int shulkerSlot = -1;
    private BlockPos shulkerPos = null;
    private ItemStack shulkerBoxItem = null;
    private boolean isProcessing = false;
    private int stateTimeout = 0;
    private static final int MAX_STATE_TIMEOUT = 100; // 5 seconds at 20 TPS

    protected MinecraftClient mc;

    public ShulkerBoxItemFetcher() {
        super(MikuMikuAddon.CATEGORY, "shulker-item-fetcher", "自动从潜影盒中获取指定物品");
        mc = MinecraftClient.getInstance();
    }

    @Override
    public void onActivate() {
        if (mc.player == null || mc.world == null) {
            if (logActions.get()) error("Player or world is null!");
            toggle();
            return;
        }

        currentState = State.SEARCHING_SHULKER;
        tickCounter = 0;
        shulkerSlot = -1;
        shulkerPos = null;
        shulkerBoxItem = null;
        isProcessing = false;
        stateTimeout = 0;

        if (logActions.get()) {
            info("开始寻找潜影盒中的 " + targetItem.get().getName().getString());
        }
    }

    @Override
    public void onDeactivate() {
        if (logActions.get()) {
            info("潜影盒物品获取器已关闭");
        }
        resetState();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        tickCounter++;
        if (tickCounter < delay.get()) return;
        tickCounter = 0;

        if (isProcessing) return;

        // Check for state timeout
        stateTimeout++;
        if (stateTimeout > MAX_STATE_TIMEOUT) {
            if (logActions.get()) {
                error("状态超时: " + currentState.name() + " (已等待 " + stateTimeout + " ticks)");
            }
            changeState(State.FINISHED);
            return;
        }

        if (debugMode.get()) {
            info("当前状态: " + currentState.name() + " (Tick: " + tickCounter + ", Timeout: " + stateTimeout + ")");
        }

        switch (currentState) {
            case SEARCHING_SHULKER -> searchForShulkerBox();
            case PLACING_SHULKER -> placeShulkerBox();
            case OPENING_SHULKER -> openShulkerBox();
            case EXTRACTING_ITEMS -> extractItems();
            case CLOSING_CONTAINER -> closeContainer();
            case BREAKING_SHULKER -> breakShulkerBox();
            case PICKING_UP_SHULKER -> pickUpShulkerBox();
            case FINISHED -> finishProcess();
        }
    }

    private void searchForShulkerBox() {
        if (logActions.get()) info("正在搜索库存中的潜影盒...");

        // Search for shulker box in inventory
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                if (block instanceof ShulkerBoxBlock) {
                    // Check if this shulker box contains the target item
                    if (containsTargetItem(stack)) {
                        shulkerSlot = i;
                        shulkerBoxItem = stack.copy();
                        changeState(State.PLACING_SHULKER);
                        if (logActions.get()) {
                            info("找到包含目标物品的潜影盒，位置: " + i);
                        }
                        return;
                    }
                }
            }
        }

        // No suitable shulker box found
        if (logActions.get()) {
            error("未找到包含 " + targetItem.get().getName().getString() + " 的潜影盒");
        }
        changeState(State.FINISHED);
    }

    private boolean containsTargetItem(ItemStack shulkerBox) {
        // Check container component for contained items (1.21+ data components)
        ContainerComponent container = shulkerBox.get(DataComponentTypes.CONTAINER);
        if (container == null) return false;

        // Check each item in the container
        for (ItemStack stack : container.stream().toList()) {
            if (!stack.isEmpty() && stack.getItem() == targetItem.get()) {
                return true;
            }
        }
        return false;
    }

    private void placeShulkerBox() {
        if (logActions.get()) info("正在放置潜影盒...");

        // Find a suitable position to place the shulker box
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos placePos = findSuitablePlacePosition(playerPos);

        if (placePos == null) {
            if (logActions.get()) error("找不到合适的位置放置潜影盒");
            changeState(State.FINISHED);
            return;
        }

        // Switch to shulker box in hotbar
        if (shulkerSlot >= 9) {
            // Move shulker box to hotbar
            moveItemToHotbar(shulkerSlot);
            return;
        }

        // Select the shulker box
        mc.player.getInventory().selectedSlot = shulkerSlot;

        // Place the shulker box
        Direction facing = Direction.UP;
        Vec3d hitPos = Vec3d.ofCenter(placePos).add(0, 1, 0);
        BlockHitResult hitResult = new BlockHitResult(hitPos, facing, placePos.down(), false);

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);

        shulkerPos = placePos;
        changeState(State.OPENING_SHULKER);

        if (logActions.get()) {
            info("潜影盒已放置在: " + placePos.toShortString());
        }
    }

    private BlockPos findSuitablePlacePosition(BlockPos playerPos) {
        // Try positions around the player
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = -1; y <= 1; y++) {
                    BlockPos testPos = playerPos.add(x, y, z);
                    if (mc.world.getBlockState(testPos).isAir() &&
                        !mc.world.getBlockState(testPos.down()).isAir()) {
                        return testPos;
                    }
                }
            }
        }
        return null;
    }

    private void moveItemToHotbar(int sourceSlot) {
        // Find empty hotbar slot
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                // Move item to hotbar
                mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    sourceSlot,
                    i,
                    SlotActionType.SWAP,
                    mc.player
                );
                shulkerSlot = i;
                return;
            }
        }

        // If no empty slot, swap with first hotbar slot
        mc.interactionManager.clickSlot(
            mc.player.currentScreenHandler.syncId,
            sourceSlot,
            0,
            SlotActionType.SWAP,
            mc.player
        );
        shulkerSlot = 0;
    }

    private void openShulkerBox() {
        if (logActions.get()) info("正在打开潜影盒...");

        if (shulkerPos == null) {
            changeState(State.FINISHED);
            return;
        }

        // Right-click on the shulker box to open it
        Direction facing = Direction.UP;
        Vec3d hitPos = Vec3d.ofCenter(shulkerPos);
        BlockHitResult hitResult = new BlockHitResult(hitPos, facing, shulkerPos, false);

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);

        changeState(State.EXTRACTING_ITEMS);
        if (logActions.get()) info("潜影盒已打开");
    }

    private void extractItems() {
        if (logActions.get()) info("正在提取物品...");

        // Check if container screen is open
        if (mc.currentScreen == null) {
            if (logActions.get()) info("等待容器界面打开... (界面为空)");
            return;
        }

        if (!(mc.currentScreen instanceof HandledScreen)) {
            if (logActions.get()) info("等待容器界面打开... (当前界面: " + mc.currentScreen.getClass().getSimpleName() + ")");
            return;
        }

        HandledScreen<?> containerScreen = (HandledScreen<?>) mc.currentScreen;
        var screenHandler = containerScreen.getScreenHandler();

        if (logActions.get()) info("容器界面已打开: " + containerScreen.getClass().getSimpleName());

        // Count empty slots in player inventory (excluding hotbar slot 0 which we want to keep)
        int emptySlots = countEmptyInventorySlots();
        if (emptySlots <= 1) {
            if (logActions.get()) info("库存空间不足，仅保留一格空位");
            changeState(State.CLOSING_CONTAINER);
            return;
        }

        // Extract target items from container
        boolean foundItems = false;
        int containerSlots = screenHandler.slots.size() - 36; // Container slots only (excluding player inventory)

        if (logActions.get()) {
            info("容器槽位数: " + containerSlots + ", 总槽位数: " + screenHandler.slots.size());
        }

        for (int i = 0; i < containerSlots; i++) {
            ItemStack stack = screenHandler.getSlot(i).getStack();
            if (logActions.get() && !stack.isEmpty()) {
                info("槽位 " + i + ": " + stack.getItem().getName().getString() + " x" + stack.getCount());
            }

            if (!stack.isEmpty() && stack.getItem() == targetItem.get()) {
                // Click to take the item
                mc.interactionManager.clickSlot(
                    screenHandler.syncId,
                    i,
                    0,
                    SlotActionType.QUICK_MOVE,
                    mc.player
                );
                foundItems = true;

                if (logActions.get()) {
                    info("提取了 " + stack.getCount() + " 个 " + stack.getName().getString());
                }

                // Check if we have enough space for more items
                emptySlots = countEmptyInventorySlots();
                if (emptySlots <= 1) {
                    if (logActions.get()) info("库存空间不足，停止提取");
                    break;
                }
            }
        }

        if (!foundItems) {
            if (logActions.get()) info("潜影盒中没有更多目标物品");
            changeState(State.CLOSING_CONTAINER);
        }
    }

    private int countEmptyInventorySlots() {
        int count = 0;
        for (int i = 9; i < 36; i++) { // Main inventory slots only
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private void closeContainer() {
        if (logActions.get()) info("正在关闭容器...");

        if (mc.currentScreen != null) {
            mc.currentScreen.close();
        }

        changeState(State.BREAKING_SHULKER);
    }

    private void breakShulkerBox() {
        if (logActions.get()) info("正在挖掘潜影盒...");

        if (shulkerPos == null) {
            changeState(State.FINISHED);
            return;
        }

        // Break the shulker box
        mc.interactionManager.attackBlock(shulkerPos, Direction.UP);
        mc.interactionManager.breakBlock(shulkerPos);

        changeState(State.PICKING_UP_SHULKER);
        if (logActions.get()) info("潜影盒已挖掘");
    }

    private void pickUpShulkerBox() {
        if (logActions.get()) info("等待拾取潜影盒...");

        // Wait a moment for the item to drop and be picked up automatically
        // The item should be picked up automatically by the game
        changeState(State.FINISHED);
    }

    private void finishProcess() {
        if (logActions.get()) {
            info("物品提取完成！");
        }

        if (autoClose.get()) {
            toggle();
        } else {
            resetState();
        }
    }

    private void resetState() {
        currentState = State.SEARCHING_SHULKER;
        tickCounter = 0;
        shulkerSlot = -1;
        shulkerPos = null;
        shulkerBoxItem = null;
        isProcessing = false;
        stateTimeout = 0;
    }

    private void changeState(State newState) {
        if (currentState != newState) {
            if (debugMode.get()) {
                info("状态切换: " + currentState.name() + " -> " + newState.name());
            }
            currentState = newState;
            stateTimeout = 0; // Reset timeout when changing state
        }
    }
}
