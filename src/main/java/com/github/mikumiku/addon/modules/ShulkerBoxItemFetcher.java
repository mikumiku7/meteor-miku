package com.github.mikumiku.addon.modules;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.utils.BetterBlockPos;
import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.BagUtil;
import com.github.mikumiku.addon.util.BaritoneUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.NopPathManager;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ShulkerBoxItemFetcher extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSettings = settings.createGroup("设置");
    IBaritone baritone;

    // Settings
    public final Setting<Item> targetItem = sgGeneral.add(new ItemSetting.Builder()
        .name("目标物品")
        .description("要从潜影盒中获取的物品。")
        .defaultValue(Items.COBBLESTONE)
        .build()
    );

    public final Setting<Integer> delay = sgSettings.add(new IntSetting.Builder()
        .name("延迟")
        .description("操作之间的延迟（tick）。")
        .defaultValue(2)
        .min(1)
        .max(20)
        .build()
    );

    public final Setting<Boolean> autoClose = sgSettings.add(new BoolSetting.Builder()
        .name("自动关闭")
        .description("完成后自动关闭模块。")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> logActions = sgSettings.add(new BoolSetting.Builder()
        .name("记录操作")
        .description("在聊天栏记录操作日志。")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> autoRotate = sgSettings.add(new BoolSetting.Builder()
        .name("自动转头")
        .description("交互时自动转头看向方块。")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> debugMode = sgSettings.add(new BoolSetting.Builder()
        .name("调试模式")
        .description("启用详细的调试日志。")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> elytraExchange = sgSettings.add(new BoolSetting.Builder()
        .name("鞘翅交换")
        .description("鞘翅交换逻辑，旧的存起来。")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> specifyAmount = sgSettings.add(new BoolSetting.Builder()
        .name("指定数量")
        .description("是否只提取指定数量的物品。")
        .defaultValue(false)
        .build()
    );

    public final Setting<Integer> extractAmount = sgSettings.add(new IntSetting.Builder()
        .name("提取数量（组）")
        .description("要提取的物品组数（一组64个）。")
        .defaultValue(1)
        .min(1)
        .max(64)
        .visible(() -> specifyAmount.get())
        .build()
    );

    // Internal state
    public enum State {
        SEARCHING_SHULKER,
        PLACING_SHULKER,
        OPENING_SHULKER,
        EXTRACTING_ITEMS,
        CLOSING_CONTAINER,
        BREAKING_SHULKER,
        PICKING_UP_SHULKER,
        FINISHED
    }

    public State currentState = State.SEARCHING_SHULKER;
    public int tickCounter = 0;
    public int shulkerSlot = -1;
    public BlockPos shulkerPos = null;
    public ItemStack shulkerBoxItem = null;
    public boolean isProcessing = false;
    public int stateTimeout = 0;
    public static final int MAX_STATE_TIMEOUT = 100; // 5 seconds at 20 TPS
    public boolean hasRotated = false; // Track if we've rotated to look at the shulker box
    public int itemsExtracted = 0; // Track how many items have been extracted

    public ShulkerBoxItemFetcher() {
        super("一键补给", "自动从潜影盒中获取指定物品。自动放盒，取物，打盒，捡盒");
        baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
    }

    @Override
    public void onActivate() {
        if (mc.player == null || mc.world == null) {
            if (logActions.get()) error("玩家或世界为空！");
            toggle();
            return;
        }

        if (PathManagers.get() instanceof NopPathManager) {
            info("需要 Baritone 自动寻路");
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
        hasRotated = false;
        itemsExtracted = 0;

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
                error("状态超时: " + currentState.name() + " (已等待 " + stateTimeout + " 刻)");
            }
            changeState(State.FINISHED);
            return;
        }

        if (debugMode.get()) {
            info("当前状态: " + currentState.name() + " (Tick: " + tickCounter + ", 超时: " + stateTimeout + ")");
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

        shulkerSlot = BagUtil.findItemInventorySlot(stack -> {
            if (stack.isEmpty()) return false;
            if (stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                if (block instanceof ShulkerBoxBlock) {
                    // Check if this shulker box contains the target item
                    if (containsTargetItem(stack)) {
                        // Special handling for elytra exchange mode
                        if (targetItem.get() == Items.ELYTRA && elytraExchange.get()) {
                            // Check if shulker box contains high durability elytra
                            ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
                            if (container != null) {
                                boolean hasHighDurabilityElytra = false;
                                int elytraCount = 0;

                                for (ItemStack itemStack : container.stream().toList()) {
                                    if (!itemStack.isEmpty() && itemStack.getItem() == Items.ELYTRA) {
                                        elytraCount++;
                                        if (isElytraHighDurability(itemStack)) {
                                            hasHighDurabilityElytra = true;
                                        }
                                    }
                                }

                                if (hasHighDurabilityElytra) {
                                    if (logActions.get()) {
                                        info("找到潜影盒包含高耐久鞘翅 (共 " + elytraCount + " 个鞘翅)");
                                        if (specifyAmount.get()) {
                                            int availableGroups = countTargetItemGroups(stack, targetItem.get());
                                            info("可用组数: " + availableGroups + ", 需要提取: " + extractAmount.get() + " 组");
                                        }
                                    }
                                    shulkerBoxItem = stack.copy();
                                    changeState(State.PLACING_SHULKER);
                                    return true;
                                } else if (elytraCount > 0) {
                                    if (logActions.get()) {
                                        info("跳过低耐久鞘翅潜影盒");
                                    }
                                }
                            }
                        } else {
                            // Normal handling for non-elytra items or when elytra exchange is disabled
                            if (specifyAmount.get()) {
                                int availableGroups = countTargetItemGroups(stack, targetItem.get());
                                if (availableGroups > 0) {
                                    if (logActions.get()) {
                                        info("找到潜影盒包含 " + availableGroups + " 组 " + targetItem.get().getName().getString());
                                    }
                                    shulkerBoxItem = stack.copy();
                                    changeState(State.PLACING_SHULKER);
                                    return true;
                                }
                            } else {
                                // Not specifying amount, any shulker box with the target item is fine
                                shulkerBoxItem = stack.copy();
                                changeState(State.PLACING_SHULKER);
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        });

        // No suitable shulker box found

        if (shulkerSlot == -1) {
            if (targetItem.get() == Items.ELYTRA && elytraExchange.get()) {
                error("未找到包含高耐久鞘翅的潜影盒");
            } else {
                error("未找到包含 " + targetItem.get().getName().getString() + " 的潜影盒");
            }
            changeState(State.FINISHED);
        }

    }

    public boolean containsTargetItem(ItemStack shulkerBox) {

        return containsTargetItem(shulkerBox, targetItem.get());
    }

    public static boolean containsTargetItem(ItemStack shulkerBox, Item targetItem) {
        // Check container component for contained items (1.21+ data components)
        ContainerComponent container = shulkerBox.get(DataComponentTypes.CONTAINER);
        if (container == null) return false;

        // Check each item in the container
        for (ItemStack stack : container.stream().toList()) {
            if (!stack.isEmpty() && stack.getItem() == targetItem) {
                return true;
            }
        }
        return false;
    }

    public static int countTargetItemGroups(ItemStack shulkerBox, Item targetItem) {
        // Count how many groups (stacks) of target item are in the shulker box
        ContainerComponent container = shulkerBox.get(DataComponentTypes.CONTAINER);
        if (container == null) return 0;

        int groupCount = 0;
        for (ItemStack stack : container.stream().toList()) {
            if (!stack.isEmpty() && stack.getItem() == targetItem) {
                groupCount++;
            }
        }
        return groupCount;
    }

    public static int countTargetItems(ItemStack shulkerBox, Item targetItem) {
        // Count total number of target items in the shulker box
        ContainerComponent container = shulkerBox.get(DataComponentTypes.CONTAINER);
        if (container == null) return 0;

        int totalCount = 0;
        for (ItemStack stack : container.stream().toList()) {
            if (!stack.isEmpty() && stack.getItem() == targetItem) {
                totalCount += stack.getCount();
            }
        }
        return totalCount;
    }

    public static int getElytraDurability(ItemStack elytraStack) {
        if (elytraStack.isEmpty()) {
            return 0;
        }

        int maxDamage = elytraStack.getMaxDamage();
        int damage = elytraStack.getDamage();
        return maxDamage - damage; // Return remaining durability
    }

    public static boolean isElytraLowDurability(ItemStack elytraStack) {
        return getElytraDurability(elytraStack) <= 20;
    }

    public static boolean isElytraHighDurability(ItemStack elytraStack) {
        return getElytraDurability(elytraStack) > 20;
    }

    public int findLowDurabilityElytraInInventory() {
        // Find low durability elytra in player's inventory (main inventory only, not hotbar)
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.ELYTRA && isElytraLowDurability(stack)) {
                return i; // Return the slot index
            }
        }
        return -1; // No low durability elytra found
    }

    private void placeShulkerBox() {
        if (logActions.get()) info("正在放置潜影盒...");

        // Find a suitable position to place the shulker box
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos placePos = findSuitablePlacePosition(playerPos);

        if (placePos == null) {
            error("找不到合适的位置放置潜影盒");
            changeState(State.FINISHED);
            return;
        }

        if (shulkerSlot != -1) {
            BaritoneUtil.placeItem(placePos, shulkerSlot);
        }

        shulkerPos = placePos;
        changeState(State.OPENING_SHULKER);

        if (logActions.get()) {
            info("潜影盒已放置在: " + placePos.toShortString());
        }
    }

    public BlockPos findSuitablePlacePosition(BlockPos playerPos) {
        // Get player's facing direction
        Direction playerFacing = mc.player.getHorizontalFacing();

        // Priority order: facing direction first, then adjacent sides, then diagonals
        // 1. First try the direction player is facing
        BlockPos facingPos = playerPos.offset(playerFacing);
        if (isValidPlacePosition(facingPos)) {
            if (debugMode.get()) {
                info("找到合适位置(面朝方向): " + facingPos.toShortString());
            }
            return facingPos;
        }

        // 2. Try adjacent horizontal directions (not diagonal)
        Direction[] adjacentDirections = {
            playerFacing.rotateYClockwise(),
            playerFacing.rotateYCounterclockwise(),
            playerFacing.getOpposite()
        };

        for (Direction dir : adjacentDirections) {
            BlockPos testPos = playerPos.offset(dir);
            if (isValidPlacePosition(testPos)) {
                if (debugMode.get()) {
                    info("找到合适位置(相邻方向): " + testPos.toShortString());
                }
                return testPos;
            }
        }

        // 3. Try above and below current position
        for (int y = 1; y >= -1; y -= 2) { // +1 then -1
            BlockPos testPos = playerPos.add(0, y, 0);
            if (isValidPlacePosition(testPos)) {
                if (debugMode.get()) {
                    info("找到合适位置(上下方向): " + testPos.toShortString());
                }
                return testPos;
            }
        }

        // 4. Finally try diagonal positions if no direct adjacent positions work
        for (int distance = 1; distance <= 3; distance++) {
            for (int x = -distance; x <= distance; x++) {
                for (int z = -distance; z <= distance; z++) {
                    // Skip positions we already checked (direct adjacent)
                    if ((Math.abs(x) == 1 && z == 0) || (x == 0 && Math.abs(z) == 1) || (x == 0 && z == 0)) {
                        continue;
                    }

                    // Only check positions at the current distance boundary
                    if (Math.abs(x) == distance || Math.abs(z) == distance) {
                        for (int y = -1; y <= 1; y++) {
                            BlockPos testPos = playerPos.add(x, y, z);
                            if (isValidPlacePosition(testPos)) {
                                if (debugMode.get()) {
                                    double dist = Math.sqrt(x * x + y * y + z * z);
                                    info("找到合适位置(对角线): " + testPos.toShortString() + " (距离: " + String.format("%.1f", dist) + ")");
                                }
                                return testPos;
                            }
                        }
                    }
                }
            }
        }

        if (logActions.get()) {
            error("在3格范围内未找到合适的放置位置");
        }
        return null;
    }

    private boolean isValidPlacePosition(BlockPos pos) {
        return mc.world.getBlockState(pos).isAir() &&
            BlockUtils.canPlace(pos) &&
            !mc.world.getBlockState(pos.down()).isAir();
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

        // First, look at the shulker box
        if (!hasRotated) {
            lookAtBlock(shulkerPos);
            hasRotated = true;
            if (logActions.get()) info("转头看向潜影盒: " + shulkerPos.toShortString());
            return; // Wait one tick for rotation to complete
        }

        // Wait a few ticks after rotation to ensure it's complete
        if (stateTimeout < 3) {
            return; // Wait 3 ticks after rotation
        }

        // 确保目标位置确实是潜影盒
        Block block = mc.world.getBlockState(shulkerPos).getBlock();
        if (!(block instanceof ShulkerBoxBlock)) {
            warning("目标位置不是潜影盒: " + shulkerPos.toShortString());
            return;
        }

        // 计算点击位置
        Vec3d hitVec = Vec3d.ofCenter(shulkerPos);
        Direction side = Direction.UP; // 默认从上方点击

        // 创建方块命中结果
        BlockHitResult hitResult = new BlockHitResult(hitVec, side, shulkerPos, false);

        // 右键点击潜影盒
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
            if (logActions.get())
                info("等待容器界面打开... (当前界面: " + mc.currentScreen.getClass().getSimpleName() + ")");
            return;
        }

        HandledScreen<?> containerScreen = (HandledScreen<?>) mc.currentScreen;
        var screenHandler = containerScreen.getScreenHandler();

        if (logActions.get()) info("容器界面已打开: " + containerScreen.getClass().getSimpleName());

        // Special handling for elytra exchange
        boolean isElytraTarget = targetItem.get() == Items.ELYTRA && elytraExchange.get();
        boolean hasStoredLowDurabilityElytra = false;

        if (isElytraTarget) {
            // Check for low durability elytra in inventory and store it first
            int lowDurabilitySlot = findLowDurabilityElytraInInventory();
            if (lowDurabilitySlot != -1) {
                if (logActions.get()) {
                    ItemStack lowElytra = mc.player.getInventory().getStack(lowDurabilitySlot);
                    info("发现低耐久鞘翅 (耐久: " + getElytraDurability(lowElytra) + ")，先存入潜影盒");
                }
                // Move low durability elytra to container
                mc.interactionManager.clickSlot(
                    screenHandler.syncId,
                    36 + lowDurabilitySlot, // Player inventory slot
                    0,
                    SlotActionType.QUICK_MOVE,
                    mc.player
                );
                hasStoredLowDurabilityElytra = true;
                return; // Wait one tick for the move to complete
            }
        }

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
        int targetGroupsExtracted = 0; // Count how many groups of items we've extracted in this session
        int maxGroupsToExtract = specifyAmount.get() ? extractAmount.get() : Integer.MAX_VALUE;

        if (logActions.get()) {
            info("容器槽位数: " + containerSlots + ", 总槽位数: " + screenHandler.slots.size());
            if (specifyAmount.get()) {
                info("指定提取模式: 最多提取 " + maxGroupsToExtract + " 组物品");
            } else {
                info("全部提取模式: 提取所有目标物品");
            }
        }

        for (int i = 0; i < containerSlots; i++) {
            ItemStack stack = screenHandler.getSlot(i).getStack();
            if (logActions.get() && !stack.isEmpty()) {
                String durabilityInfo = "";
                if (stack.getItem() == Items.ELYTRA) {
                    durabilityInfo = " (耐久: " + getElytraDurability(stack) + ")";
                }
                info("槽位 " + i + ": " + stack.getItem().getName().getString() + " x" + stack.getCount() + durabilityInfo);
            }

            if (!stack.isEmpty() && stack.getItem() == targetItem.get()) {
                // Special elytra durability check
                if (isElytraTarget && !isElytraHighDurability(stack)) {
                    if (logActions.get()) {
                        info("跳过低耐久鞘翅 (耐久: " + getElytraDurability(stack) + ")");
                    }
                    continue;
                }

                // Check if we've reached the extraction limit
                if (specifyAmount.get() && targetGroupsExtracted >= maxGroupsToExtract) {
                    if (logActions.get()) {
                        info("已达到指定提取数量限制 (" + maxGroupsToExtract + " 组)，停止提取");
                    }
                    break;
                }

                // Click to take the item
                mc.interactionManager.clickSlot(
                    screenHandler.syncId,
                    i,
                    0,
                    SlotActionType.QUICK_MOVE,
                    mc.player
                );
                foundItems = true;
                targetGroupsExtracted++;
                itemsExtracted += stack.getCount();

                if (logActions.get()) {
                    String durabilityInfo = "";
                    if (stack.getItem() == Items.ELYTRA) {
                        durabilityInfo = " (耐久: " + getElytraDurability(stack) + ")";
                    }
                    info("提取了 " + stack.getCount() + " 个 " + stack.getName().getString() + durabilityInfo +
                        " (已提取 " + targetGroupsExtracted + "/" + maxGroupsToExtract + " 组)");
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
            if (logActions.get()) {
                if (specifyAmount.get()) {
                    info("已完成指定数量提取或潜影盒中没有更多目标物品");
                } else {
                    info("潜影盒中没有更多目标物品");
                }
            }
            changeState(State.CLOSING_CONTAINER);
        }
    }

    public static int countEmptyInventorySlots() {
        int count = 0;
        MinecraftClient mc = MinecraftClient.getInstance();
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

        // Check if the shulker box still exists
        if (mc.world.getBlockState(shulkerPos).isAir()) {
            if (logActions.get()) info("潜影盒已经不存在了");
            changeState(State.PICKING_UP_SHULKER);
            baritone.getPathingBehavior().cancelEverything();
            return;
        }
        // Look at the shulker box before breaking
        lookAtBlock(shulkerPos);

        BetterBlockPos betterBlockPos = BetterBlockPos.from(shulkerPos);
        baritone.getSelectionManager().addSelection(betterBlockPos, betterBlockPos);
        baritone.getBuilderProcess().clearArea(betterBlockPos, betterBlockPos);

        changeState(State.PICKING_UP_SHULKER);
        if (logActions.get()) info("潜影盒挖掘完成");
        if (logActions.get()) info("等待拾取潜影盒...");

        // Use the correct packet method to break the block
//        if (stateTimeout == 0) {
//            // Send start destroy packet
//            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
//                PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
//                shulkerPos,
//                WalkDirection.UP
//            ));
//            if (debugMode.get()) info("发送开始挖掘包: " + shulkerPos.toShortString());
//        } else if (stateTimeout >= 10) { // Wait a few ticks then send stop packet
//            // Send stop destroy packet
//            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
//                PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
//                shulkerPos,
//                WalkDirection.UP
//            ));
//            if (debugMode.get()) info("发送停止挖掘包");
//
//        }
        // Wait between start and stop packets
    }

    private void pickUpShulkerBox() {

        // Wait a few ticks for the item to drop and be picked up automatically
        // Use stateTimeout to wait for a reasonable amount of time
        if (stateTimeout > 20) { // Wait 1 second (20 ticks)
            changeState(State.FINISHED);
            if (logActions.get()) info("拾取等待完成");
        }
        // Otherwise stay in this state and wait
    }

    private void finishProcess() {
        if (logActions.get()) {
            if (specifyAmount.get()) {
                info("物品提取完成！共提取了 " + itemsExtracted + " 个物品");
            } else {
                info("物品提取完成！共提取了 " + itemsExtracted + " 个物品");
            }
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
        hasRotated = false;
        itemsExtracted = 0;

        baritone.getSelectionManager().removeAllSelections();
        baritone.getPathingBehavior().cancelEverything();

    }

    private void changeState(State newState) {
        if (currentState != newState) {
            if (debugMode.get()) {
                info("状态切换: " + currentState.name() + " -> " + newState.name());
            }
            currentState = newState;
            stateTimeout = 0; // Reset timeout when changing state
            hasRotated = false; // Reset rotation flag when changing state
        }
    }

    private void lookAtBlock(BlockPos pos) {
        if (mc.player == null || !autoRotate.get()) return;

        Vec3d playerPos = mc.player.getEyePos();
        Vec3d blockCenter = Vec3d.ofCenter(pos);
        Vec3d direction = blockCenter.subtract(playerPos).normalize();

        float yaw = (float) (MathHelper.atan2(direction.z, direction.x) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) -(MathHelper.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z)) * 180.0 / Math.PI);

//        mc.player.setYaw(yaw);
//        mc.player.setPitch(pitch);
        Rotations.rotate(yaw, pitch);
        if (debugMode.get()) {
            info("转头看向方块: " + pos.toShortString() + " (偏航: " + String.format("%.1f", yaw) + ", 俯仰: " + String.format("%.1f", pitch) + ")");
        }
    }
}
