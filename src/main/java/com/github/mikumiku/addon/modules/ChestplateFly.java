package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.Via;
import meteordevelopment.meteorclient.events.world.PlaySoundEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class ChestplateFly extends BaseModule {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final Setting<Mode> mode = this.sgGeneral.add(
        new EnumSetting.Builder<Mode>()
            .name("模式")
            .description("决定模块在飞行时如何处理装备：\n" +
                " - 胸甲模式：每次使用鞘翅飞行后会立即换回胸甲，用于在飞行与战斗间快速切换。\n" +
                " - 鞘翅模式：保持鞘翅不换回胸甲，适合长时间飞行或远距离探索。")
            .defaultValue(Mode.胸甲模式)
            .build()
    );
    public final Setting<Integer> fireworkDelay = this.sgGeneral.add(new IntSetting.Builder()
        .name("烟花延迟")
        .description("控制自动使用烟花火箭的时间间隔（单位：秒）。\n" +
            "数值越低，烟花使用越频繁，飞行速度越平稳；数值越高，则使用间隔越长，节省烟花。\n" +
            "默认值 5 表示每隔约 5 秒使用一次烟花推进飞行。")
        .defaultValue(5)
        .sliderRange(0, 10)
        .build());
    private int fireworkTicksLeft = 0;
    private boolean needsFirework = false;
    private Vec3d currentVelocity = Vec3d.ZERO;
    private InventorySlotSwap slotSwap = null;

    public ChestplateFly() {
        super("甲飞", "再一次。");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        this.needsFirework = this.getIsUsingFirework();
        this.currentVelocity = this.mc.player.getVelocity();
        this.mc.player.jump();
        this.mc.player.setOnGround(false);
    }

    @Override
    public void onDeactivate() {
        this.equipChestplate(this.slotSwap);
        Via.sendReleaseShift();
        this.mc.player.setSneaking(false);
        this.fireworkTicksLeft = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        boolean isUsingFirework = this.getIsUsingFirework();
        if (isUsingFirework || InvUtils.find(Items.FIREWORK_ROCKET).found()) {
            Box boundingBox = this.mc.player.getBoundingBox();
            double playerFeetY = boundingBox.minY;
            Box groundBox = new Box(boundingBox.minX, playerFeetY - 0.1, boundingBox.minZ, boundingBox.maxX, playerFeetY, boundingBox.maxZ);

            for (BlockPos pos : BlockPos.iterate(
                (int) Math.floor(groundBox.minX),
                (int) Math.floor(groundBox.minY),
                (int) Math.floor(groundBox.minZ),
                (int) Math.floor(groundBox.maxX),
                (int) Math.floor(groundBox.maxY),
                (int) Math.floor(groundBox.maxZ)
            )) {
                BlockState blockState = this.mc.world.getBlockState(pos);
                if (blockState.isSolidBlock(this.mc.world, pos)) {
                    double blockTopY = pos.getY() + 1.0;
                    double distanceToBlock = playerFeetY - blockTopY;
                    if (distanceToBlock >= 0.0 && distanceToBlock < 0.1 && this.currentVelocity.y < 0.0) {
                        this.currentVelocity = new Vec3d(this.currentVelocity.x, 0.1, this.currentVelocity.z);
                    }
                }
            }

            this.slotSwap = this.equipElytra();
            this.mc.player
                .networkHandler
                .sendPacket(new ClientCommandC2SPacket(this.mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            if (this.fireworkTicksLeft <= 0) {
                this.needsFirework = true;
            }

            if (this.needsFirework && this.currentVelocity.length() > 1.0E-7) {
                this.useFirework();
                this.needsFirework = false;
            }

            if (this.fireworkTicksLeft >= 0) {
                this.fireworkTicksLeft--;
            } else {
                this.fireworkTicksLeft = this.fireworkDelay.get();
            }

            if (this.mode.get() == Mode.胸甲模式) {
                this.equipChestplate(this.slotSwap);
                this.slotSwap = null;
            }
        }
    }

    private boolean getIsUsingFirework() {
        boolean usingFirework = false;

        for (Entity entity : this.mc.world.getEntities()) {
            if (entity instanceof FireworkRocketEntity firework && firework.getOwner() != null && firework.getOwner().equals(this.mc.player)) {
                usingFirework = true;
            }
        }

        return usingFirework;
    }

    public void equipChestplate(InventorySlotSwap slotSwap) {
        if (!this.mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.DIAMOND_CHESTPLATE)
            && !this.mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.NETHERITE_CHESTPLATE)) {
            FindItemResult result = InvUtils.findInHotbar(Items.NETHERITE_CHESTPLATE);
            if (!result.found()) {
                result = InvUtils.findInHotbar(Items.DIAMOND_CHESTPLATE);
            }

            if (result.found()) {
                this.mc.interactionManager.clickSlot(this.mc.player.playerScreenHandler.syncId, 6, result.slot(), SlotActionType.SWAP, this.mc.player);
                if (slotSwap != null) {
                    this.mc
                        .interactionManager
                        .clickSlot(this.mc.player.playerScreenHandler.syncId, slotSwap.inventorySlot, result.slot(), SlotActionType.SWAP, this.mc.player);
                }
            } else {
                result = InvUtils.find(Items.NETHERITE_CHESTPLATE);
                if (!result.found()) {
                    result = InvUtils.find(Items.DIAMOND_CHESTPLATE);
                }

                if (result.found()) {
                    InvUtils.move().from(result.slot()).toArmor(2);
                }
            }
        }
    }

    public InventorySlotSwap equipElytra() {
        if (this.mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA)) {
            return null;
        } else {
            FindItemResult result = InvUtils.findInHotbar(Items.ELYTRA);
            if (result.found()) {
                this.mc.interactionManager.clickSlot(this.mc.player.playerScreenHandler.syncId, 6, result.slot(), SlotActionType.SWAP, this.mc.player);
                return null;
            } else {
                result = InvUtils.find(Items.ELYTRA);
                if (!result.found()) {
                    return null;
                } else {
                    FindItemResult hotbarSlot = InvUtils.findInHotbar(x -> x.getItem() != Items.TOTEM_OF_UNDYING);
                    this.mc
                        .interactionManager
                        .clickSlot(
                            this.mc.player.playerScreenHandler.syncId,
                            result.slot(),
                            hotbarSlot.found() ? hotbarSlot.slot() : 0,
                            SlotActionType.SWAP,
                            this.mc.player
                        );
                    this.mc
                        .interactionManager
                        .clickSlot(
                            this.mc.player.playerScreenHandler.syncId, 6, hotbarSlot.found() ? hotbarSlot.slot() : 0, SlotActionType.SWAP, this.mc.player
                        );
                    InventorySlotSwap slotSwap = new InventorySlotSwap();
                    slotSwap.hotbarSlot = hotbarSlot.found() ? hotbarSlot.slot() : 0;
                    slotSwap.inventorySlot = result.slot();
                    return slotSwap;
                }
            }
        }
    }

    private void useFirework() {
        this.fireworkTicksLeft = (int) (this.fireworkDelay.get().intValue() * 20.0);
        int hotbarSilentSwapSlot = -1;
        int inventorySilentSwapSlot = -1;
        FindItemResult itemResult = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
        if (!itemResult.found()) {
            FindItemResult invResult = InvUtils.find(Items.FIREWORK_ROCKET);
            if (!invResult.found()) {
                return;
            }

            FindItemResult hotbarSlotToSwapToResult = InvUtils.findInHotbar(x -> x.getItem() != Items.TOTEM_OF_UNDYING);
            inventorySilentSwapSlot = invResult.slot();
            hotbarSilentSwapSlot = hotbarSlotToSwapToResult.found() ? hotbarSlotToSwapToResult.slot() : 0;
            this.mc
                .interactionManager
                .clickSlot(this.mc.player.playerScreenHandler.syncId, inventorySilentSwapSlot, hotbarSilentSwapSlot, SlotActionType.SWAP, this.mc.player);
            itemResult = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
        }

        if (itemResult.found()) {
            if (itemResult.isOffhand()) {
                this.mc.interactionManager.interactItem(this.mc.player, Hand.OFF_HAND);
                this.mc.player.swingHand(Hand.OFF_HAND);
            } else {
                InvUtils.swap(itemResult.slot(), true);
                this.mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND);
                this.mc.player.swingHand(Hand.MAIN_HAND);
                InvUtils.swapBack();
            }

            if (inventorySilentSwapSlot != -1 && hotbarSilentSwapSlot != -1) {
                this.mc.interactionManager
                    .clickSlot(
                        this.mc.player.playerScreenHandler.syncId, inventorySilentSwapSlot, hotbarSilentSwapSlot, SlotActionType.SWAP, this.mc.player
                    );
            }
        }
    }

    @EventHandler
    public void onPlaySound(PlaySoundEvent event) {
        List<Identifier> armorEquipSounds = List.of(
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
        for (Identifier identifier : armorEquipSounds) {
            if (identifier.equals(event.sound.getId())) {
                event.cancel();
                break;
            }
        }
    }
    private class InventorySlotSwap {
        public int hotbarSlot;
        public int inventorySlot;
    }

    public static enum Mode {
        胸甲模式,
        鞘翅模式;
    }
}
