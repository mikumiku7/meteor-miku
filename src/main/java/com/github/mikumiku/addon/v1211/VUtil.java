package com.github.mikumiku.addon.v1211;

import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.Optional;

public class VUtil {
    public static ItemStack getEnchantedBookWith(Optional<RegistryEntry.Reference<Enchantment>> en) {


        return EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(en.get(), en.get().value().getMaxLevel()));
    }
    public static Registry<Enchantment> getEnchantmentRegistry( ) {
        DynamicRegistryManager registryManager = MinecraftClient.getInstance().world.getRegistryManager();
        return registryManager.get(RegistryKeys.ENCHANTMENT);
    }

}
