package com.github.mikumiku.addon.util;

import meteordevelopment.meteorclient.mixininterface.IRaycastContext;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Optional;

public interface VUtil {
    ItemStack getEnchantedBookWith(Optional<RegistryEntry.Reference<Enchantment>> en);

    Registry<Enchantment> getEnchantmentRegistry();


    PlayerMoveC2SPacket.LookAndOnGround get(float currentYaw, float pitch, boolean onGround);

    PlayerMoveC2SPacket.Full getFull(double x, double y, double z, float yaw, float pitch, boolean onGround);

    boolean isFallFlying(MinecraftClient mc);

    boolean isJumping(MinecraftClient mc);

    boolean isSneaking(MinecraftClient mc);

    Direction getOppositeDirectionTo(BlockPos blockPos);

    double getToughness(LivingEntity entity);

    void setRaycast(IRaycastContext raycastContext, Vec3d source, Vec3d vec3d, RaycastContext.ShapeType shapeType, RaycastContext.FluidHandling fluidHandling, ClientPlayerEntity player);

    void setMovement(IRaycastContext raycastContext, Vec3d source, Vec3d vec3d, RaycastContext.ShapeType shapeType, RaycastContext.FluidHandling fluidHandling, ClientPlayerEntity player);

    void setMovement(IVec3d movement, double x, double y, double z);
}
