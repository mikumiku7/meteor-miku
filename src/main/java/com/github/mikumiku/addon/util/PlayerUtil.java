package com.github.mikumiku.addon.util;

import net.minecraft.client.input.Input;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public interface PlayerUtil {
    int getSelectedSlot(PlayerInventory playerInventory);

    void setSelectedSlot(PlayerInventory playerInventory, int selectedSlot);

    Iterable<ItemStack> getArmor(PlayerInventory playerInventory);

    Iterable<ItemStack> getArmor(LivingEntity entity);

    float movementForward(Input input);

    float movementSideways(Input input);

    Vec3d getEntityPos(Entity entity);

    World getEntityWorld(Entity entity);

    String getGameProfileName(PlayerEntity entity);
}
