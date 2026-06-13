package com.github.mikumiku.addon.nerv_printer.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.mixinface.IClientPlayerInteractionManager;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.AnvilBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.RenameItemC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;

public class MapNamer extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> startX = sgGeneral.add(new IntSetting.Builder()
            .name("起始X坐标")
            .description("地图枚举应该从哪个X索引开始。")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 10)
            .build()
    );

    private final Setting<Integer> endX = sgGeneral.add(new IntSetting.Builder()
            .name("结束X坐标")
            .description("地图的最大X值。")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 10)
            .build()
    );

    private final Setting<Integer> startY = sgGeneral.add(new IntSetting.Builder()
            .name("起始Y坐标")
            .description("地图枚举应该从哪个Y索引开始。")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 10)
            .build()
    );

    private final Setting<Integer> endY = sgGeneral.add(new IntSetting.Builder()
            .name("结束Y坐标")
            .description("地图的最大Y值。")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 10)
            .build()
    );

    public final Setting<String> mapName = sgGeneral.add(new StringSetting.Builder()
            .name("地图名称")
            .description("地图物品的名称前缀。")
            .defaultValue("地图_")
            .wide()
            .renderer(StarscriptTextBoxRenderer.class)
            .build()
    );

    public final Setting<String> separator = sgGeneral.add(new StringSetting.Builder()
            .name("分隔符")
            .description("X和Y索引之间的分隔符。")
            .defaultValue("_")
            .wide()
            .renderer(StarscriptTextBoxRenderer.class)
            .build()
    );

    private final Setting<Integer> renameDelay = sgGeneral.add(new IntSetting.Builder()
            .name("重命名延迟")
            .description("地图重命名之间的延迟时间(刻)。")
            .defaultValue(10)
            .min(1)
            .sliderRange(1, 20)
            .build()
    );

    private final Setting<Order> order = sgGeneral.add(new EnumSetting.Builder<Order>()
            .name("命名顺序")
            .description("地图命名的顺序。槽位 = 快捷栏+物品栏从左到右。")
            .defaultValue(Order.Slot)
            .build()
    );

    public MapNamer() {
        super(BaseModule.CATEGORY_MIKU_PRO, "地图命名器",
                "使用格式自动命名物品栏中的地图:地图名称 + X + 分隔符 + Y。基于 nerv_printer");
    }

    ArrayList<Integer> mapSlots;
    State state;
    int ticks;
    int currentX;
    int currentY;

    @Override
    public void onActivate() {
        mapSlots = new ArrayList<>();
        state = State.AwaitInteract;
        ticks = 0;
        currentX = -1;
        currentY = -1;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (state == State.AwaitInteract && event.packet instanceof PlayerInteractBlockC2SPacket packet) {
            BlockPos blockPos = packet.getBlockHitResult().getBlockPos();
            if (mc.world.getBlockState(blockPos).getBlock() instanceof AnvilBlock) {
                state = State.AwaitScreen;
            }
        }
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (state == State.HandleMaps && event.packet instanceof CloseScreenS2CPacket) {
            info("Inventory screen closed. Interact with an anvil.");
            state = State.AwaitInteract;
            return;
        }
        if (state != State.AwaitScreen) return;
        if (event.packet instanceof RenameItemC2SPacket packet && !packet.getName().startsWith(mapName.get())) {
            event.cancel();
        }
        if (!(event.packet instanceof InventoryS2CPacket packet)) return;

        if (startX.get() > endX.get() || startY.get() > endY.get()) {
            warning("Start index is larger than end index.");
            return;
        }
        mapSlots.clear();
        for (int slot = 0; slot < mc.player.getInventory().size(); slot++) {
            int adjustedSlot = slot;
            if (order.get() == Order.ReversedSlot) {
                adjustedSlot = mc.player.getInventory().size() - slot - 1;
            }
            ItemStack itemStack = mc.player.getInventory().getStack(adjustedSlot);
            if (itemStack.getItem() == Items.FILLED_MAP) {
                // info("Map Name: " + itemStack.getName().getString());
                if (itemStack.getName().getString().equals("Map")) {
                    if (adjustedSlot < 9) {  //Stupid slot correction
                        adjustedSlot += 30;
                    } else {
                        adjustedSlot -= 6;
                    }
                    mapSlots.add(adjustedSlot);
                }
            }
        }
        ticks = renameDelay.get();
        state = State.HandleMaps;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (ticks == 0 || state != State.HandleMaps) return;
        ticks--;
        if (ticks == 0 && !mapSlots.isEmpty()) {
            if (mc.player.experienceLevel < 1) {
                info("Not enough XP.");
                state = State.AwaitInteract;
                if (mc.currentScreen != null) mc.player.closeHandledScreen();
                return;
            }
            int slot = mapSlots.remove(0);
            if (currentX == -1) currentX = startX.get();
            if (currentY == -1) currentY = startY.get();
            // info("Process map: " + slot + " with x: " + startX.get() + ", y: " + startY.get());

            IClientPlayerInteractionManager cim = (IClientPlayerInteractionManager) mc.interactionManager;
            cim.clickSlot(mc.player.currentScreenHandler.syncId, slot, 1, SlotActionType.QUICK_MOVE, mc.player);
            String newMapName = mapName.get() + currentX + separator + currentY;
            mc.getNetworkHandler().sendPacket(new RenameItemC2SPacket(newMapName));
            cim.clickSlot(mc.player.currentScreenHandler.syncId, 2, 1, SlotActionType.QUICK_MOVE, mc.player);

            currentY++;
            if (currentY > endY.get()) {
                currentY = 0;
                currentX++;
                if (currentX > endX.get()) {
                    if (mapSlots.isEmpty()) {
                        info("Complete map was successfully named.");
                        startX.set(0);
                        startY.set(0);
                    } else {
                        info("More maps found than with endX and endY described.");
                    }
                    if (mc.currentScreen != null) mc.player.closeHandledScreen();
                    toggle();
                    return;
                }
            }

            if (mapSlots.isEmpty()) {
                startX.set(currentX);
                startY.set(currentY);
                state = State.AwaitInteract;
                info("All maps in inventory named. Progress (x: " + currentX + ", y: " + currentY + ") saved. " +
                        "Interact with an anvil with the next batch in the inventory.");
                if (mc.currentScreen != null) mc.player.closeHandledScreen();
            } else {
                ticks = renameDelay.get();
            }
        }
    }

    private enum State {
        AwaitInteract,
        AwaitScreen,
        HandleMaps
    }

    private enum Order {
        Slot,
        ReversedSlot
        //MapID,
        //ReversedMapID
    }
}
