package com.github.mikumiku.addon.impl.v1211;

import meteordevelopment.meteorclient.mixininterface.IRaycastContext;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Optional;

public class VUtil implements com.github.mikumiku.addon.util.VUtil {
    public ItemStack getEnchantedBookWith(Optional<RegistryEntry.Reference<Enchantment>> en) {


        return EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(en.get(), en.get().value().getMaxLevel()));
    }

    public Registry<Enchantment> getEnchantmentRegistry() {
        DynamicRegistryManager registryManager = MinecraftClient.getInstance().world.getRegistryManager();
        return registryManager.get(RegistryKeys.ENCHANTMENT);
    }


    public PlayerMoveC2SPacket.LookAndOnGround get(float currentYaw, float pitch, boolean onGround) {
        return new PlayerMoveC2SPacket.LookAndOnGround(currentYaw, pitch, onGround);
    }


    public PlayerMoveC2SPacket.Full getFull(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        return new PlayerMoveC2SPacket.Full(
            x,
            y,
            z,
            yaw,
            pitch,
            onGround
        );
    }

    public boolean isFallFlying(MinecraftClient mc) {
        return mc.player.isFallFlying();
    }

    public boolean isJumping(MinecraftClient mc) {
        return mc.player.input.jumping;
    }

    public boolean isSneaking(MinecraftClient mc) {
        return mc.player.input.sneaking;

    }

    public Direction getOppositeDirectionTo(BlockPos blockPos) {
        return Direction.fromRotation(Rotations.getYaw(blockPos)).getOpposite();
    }

    public double getToughness(LivingEntity entity) {
        return entity.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
    }

    public void setRaycast(IRaycastContext raycastContext, Vec3d source, Vec3d vec3d, RaycastContext.ShapeType shapeType, RaycastContext.FluidHandling fluidHandling, ClientPlayerEntity player) {
        raycastContext.set(source, vec3d, shapeType, fluidHandling, player);
    }

    @Override
    public void setMovement(IRaycastContext raycastContext, Vec3d source, Vec3d vec3d, RaycastContext.ShapeType shapeType, RaycastContext.FluidHandling fluidHandling, ClientPlayerEntity player) {
        raycastContext.set(source, vec3d, shapeType, fluidHandling, player);
    }

    @Override
    public void setMovement(IVec3d movement, double x, double y, double z) {
        movement.set(x, y, z);
    }

}
