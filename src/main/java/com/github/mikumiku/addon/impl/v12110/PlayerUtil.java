package com.github.mikumiku.addon.impl.v12110;

import net.minecraft.client.input.Input;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PlayerUtil implements com.github.mikumiku.addon.util.PlayerUtil {

    @Override
    public int getSelectedSlot(PlayerInventory playerInventory) {
        return playerInventory.getSelectedSlot();
    }

    @Override
    public void setSelectedSlot(PlayerInventory playerInventory, int selectedSlot) {
        playerInventory.setSelectedSlot(selectedSlot);
    }

    @Override
    public Iterable<ItemStack> getArmor(PlayerInventory playerInventory) {
        return getArmor(playerInventory.player);
    }

    @Override
    public Iterable<ItemStack> getArmor(LivingEntity entity) {
        return EquipmentSlot.VALUES.stream()
            .filter(e -> EquipmentSlot.Type.HUMANOID_ARMOR.equals(e.getType()))
            .map(entity::getEquippedStack).toList();
    }

    @Override
    public float movementForward(Input input) {
        return input.getMovementInput().y;
    }

    @Override
    public float movementSideways(Input input) {
        return input.getMovementInput().x;
    }

    @Override
    public Vec3d getEntityPos(Entity entity) {
        return entity.getEntityPos();
    }

    @Override
    public World getEntityWorld(Entity entity) {
        return entity.getEntityWorld();
    }

    @Override
    public String getGameProfileName(PlayerEntity entity) {
        return entity.getGameProfile().name();
    }
}
