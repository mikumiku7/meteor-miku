package com.github.mikumiku.addon.impl.v1211;

import net.minecraft.client.input.Input;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PlayerUtil implements com.github.mikumiku.addon.util.PlayerUtil {

    @Override
    public int getSelectedSlot(PlayerInventory playerInventory) {
        return playerInventory.selectedSlot;
    }

    @Override
    public void setSelectedSlot(PlayerInventory playerInventory, int selectedSlot) {
        playerInventory.selectedSlot = selectedSlot;
    }

    @Override
    public Iterable<ItemStack> getArmor(PlayerInventory playerInventory) {
        return playerInventory.armor;
    }

    @Override
    public Iterable<ItemStack> getArmor(LivingEntity entity) {
        return entity.getArmorItems();
    }

    @Override
    public float movementForward(Input input) {
        return input.movementForward;
    }

    @Override
    public float movementSideways(Input input) {
        return input.movementSideways;
    }

    @Override
    public Vec3d getEntityPos(Entity entity) {
        return entity.getPos();
    }

    @Override
    public World getEntityWorld(Entity entity) {
        return entity.getWorld();
    }

    @Override
    public String getGameProfileName(PlayerEntity entity) {
        return entity.getGameProfile().getName();
    }
}
