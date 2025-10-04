package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.BaritoneUtil;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.InventoryEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.InventoryTweaks;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class ChestAura extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("范围")
        .description("交互范围")
        .defaultValue(4)
        .min(0)
        .build()
    );

    private final Setting<List<BlockEntityType<?>>> blocks = sgGeneral.add(new StorageBlockListSetting.Builder()
        .name("方块类型")
        .description("要打开的方块类型")
        .defaultValue(BlockEntityType.CHEST, BlockEntityType.SHULKER_BOX)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("延迟")
        .description("打开箱子之间的延迟")
        .defaultValue(5)
        .sliderMax(40)
        .build()
    );

    private final Setting<Integer> forget = sgGeneral.add(new IntSetting.Builder()
        .name("遗忘时间")
        .description("等待多少tick后遗忘已打开的箱子。0表示永不遗忘")
        .defaultValue(0)
        .min(0)
        .sliderMax(3600)
        .build()
    );

    private final Setting<CloseCondition> closeCondition = sgGeneral.add(new EnumSetting.Builder<CloseCondition>()
        .name("关闭条件")
        .defaultValue(CloseCondition.始终关闭)
        .description("何时关闭箱子界面")
        .build()
    );

    private final Setting<Boolean> dropAll = sgGeneral.add(new BoolSetting.Builder()
        .name("全部丢出")
        .description("打开箱子后无延迟丢出全部物品")
        .defaultValue(false)
        .build()
    );

    private final Map<BlockPos, Integer> openedBlocks = new HashMap<>();
    private final CloseListener closeListener = new CloseListener();

    private int timer = 0;

    public ChestAura() {
        super("箱子光环", "自动开箱");
    }

    @Override
    public void onActivate() {
        timer = 0;
        openedBlocks.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (forget.get() != 0) {
            for (Map.Entry<BlockPos, Integer> e : new HashMap<>(openedBlocks).entrySet()) {
                int time = e.getValue();
                if (time > forget.get()) openedBlocks.remove(e.getKey());
                else openedBlocks.replace(e.getKey(), time + 1);
            }
        }

        if (timer > 0 && mc.currentScreen != null) return;

        for (BlockEntity block : Utils.blockEntities()) {
            if (!blocks.get().contains(block.getType())) continue;
            if (mc.player.getEyePos().distanceTo(Vec3d.ofCenter(block.getPos())) >= range.get()) continue;

            BlockPos pos = block.getPos();
            if (openedBlocks.containsKey(pos)) continue;

            Direction side = BaritoneUtil.getInteractDirection(pos, true);
            if (side == null) {
                side = Direction.UP;
            }

            BaritoneUtil.clickBlock(pos, side, true, Hand.MAIN_HAND, BaritoneUtil.SwingSide.All);

            // Double chest compatibility
            BlockState state = block.getCachedState();
            if (state.contains(ChestBlock.CHEST_TYPE)) {
                Direction direction = state.get(ChestBlock.FACING);
                switch (state.get(ChestBlock.CHEST_TYPE)) {
                    case LEFT -> openedBlocks.put(pos.offset(direction.rotateYClockwise()), 0);
                    case RIGHT -> openedBlocks.put(pos.offset(direction.rotateYCounterclockwise()), 0);
                }
            }

            openedBlocks.put(pos, 0);
            timer = delay.get();
            MeteorClient.EVENT_BUS.subscribe(closeListener);
            break;
        }
        timer--;
    }

    public class CloseListener {
        @EventHandler(priority = EventPriority.HIGH)
        private void onInventory(InventoryEvent event) {
            ScreenHandler handler = mc.player.currentScreenHandler;
            if (event.packet.getSyncId() == handler.syncId) {
                // 如果启用全部丢出功能，先丢出所有物品
                if (dropAll.get()) {
                    dropAllItems(handler);
                }

                switch (closeCondition.get()) {
                    case 空时关闭 -> {
                        DefaultedList<ItemStack> stacks = DefaultedList.of();
                        IntStream.range(0, SlotUtils.indexToId(SlotUtils.MAIN_START)).mapToObj(handler.slots::get).map(Slot::getStack).forEach(stacks::add);
                        if (stacks.stream().allMatch(ItemStack::isEmpty)) mc.player.closeHandledScreen();
                    }
                    case 始终关闭 -> mc.player.closeHandledScreen();
                    case 偷窃后关闭 -> Modules.get().get(InventoryTweaks.class);
                }
            }
            MeteorClient.EVENT_BUS.unsubscribe(this);
        }

        /**
         * 丢出箱子中的所有物品
         */
        private void dropAllItems(ScreenHandler handler) {
            // 获取箱子的物品槽位（通常是前27个槽位）
            int inventorySize = handler.slots.size();
            int chestSize = Math.min(inventorySize, 54); // 最多54个槽位（双大箱子）

            // 遍历箱子中的所有物品槽位
            for (int i = 0; i < chestSize; i++) {
                Slot slot = handler.slots.get(i);
                ItemStack stack = slot.getStack();

                // 如果物品槽不为空，则丢出物品
                if (!stack.isEmpty()) {
                    // 使用鼠标点击丢出物品（Ctrl+点击丢出整个堆）
                    mc.player.currentScreenHandler.onSlotClick(i, 0, SlotActionType.PICKUP, mc.player);
                    // 立即丢出
                    mc.player.currentScreenHandler.onSlotClick(i, 1, SlotActionType.THROW, mc.player);
                }
            }
        }
    }

    public enum CloseCondition {
        始终关闭("始终关闭"),
        空时关闭("空时关闭"),
        偷窃后关闭("偷窃后关闭"),
        从不关闭("从不关闭");

        private final String displayName;

        CloseCondition(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
