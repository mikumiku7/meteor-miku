package com.github.mikumiku.addon.util;

import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;

public class VUtil {
    public static ItemStack getEnchantedBookWith(Optional<RegistryEntry.Reference<Enchantment>> en) {
        return EnchantmentHelper.getEnchantedBookWith(new EnchantmentLevelEntry(en.get(), en.get().value().getMaxLevel()));
    }

    public static Registry<Enchantment> getEnchantmentRegistry() {
        DynamicRegistryManager registryManager = MinecraftClient.getInstance().world.getRegistryManager();
        return registryManager.getOrThrow(RegistryKeys.ENCHANTMENT);
    }


    public static PlayerMoveC2SPacket.LookAndOnGround get(float currentYaw, float pitch, boolean onGround) {
        return new PlayerMoveC2SPacket.LookAndOnGround(currentYaw, pitch, onGround, false);
    }

    public static PlayerMoveC2SPacket.Full getFull(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        return new PlayerMoveC2SPacket.Full(
            x,
            y,
            z,
            yaw,
            pitch,
            onGround, false
        );
    }

    public static boolean isFallFlying(MinecraftClient mc) {
        return mc.player.isGliding();
    }
    public static Direction getOppositeDirectionTo(BlockPos blockPos) {
        Direction dir = Direction.fromRotation(Rotations.getYaw(blockPos)).getOpposite();

        return dir;
    }

}
