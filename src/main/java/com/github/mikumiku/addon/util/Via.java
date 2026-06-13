package com.github.mikumiku.addon.util;

import com.github.mikumiku.addon.modules.VillagerRoller;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.mixininterface.IRaycastContext;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public class Via {
    public static ItemStack getEnchantedBookWith(Optional<RegistryEntry.Reference<Enchantment>> en) {


        return EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(en.get(), en.get().value().getMaxLevel()));
    }

    public static Registry<Enchantment> getEnchantmentRegistry() {
        DynamicRegistryManager registryManager = MinecraftClient.getInstance().world.getRegistryManager();
        return registryManager.get(RegistryKeys.ENCHANTMENT);
    }


    public static PlayerMoveC2SPacket.LookAndOnGround get(float currentYaw, float pitch, boolean onGround) {
        return new PlayerMoveC2SPacket.LookAndOnGround(currentYaw, pitch, onGround);
    }


    public static PlayerMoveC2SPacket.Full getFull(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        return new PlayerMoveC2SPacket.Full(
            x,
            y,
            z,
            yaw,
            pitch,
            onGround
        );
    }


    public static PlayerMoveC2SPacket.PositionAndOnGround getPositionAndOnGround(double x, double y, double z, boolean onGround) {
        return new PlayerMoveC2SPacket.PositionAndOnGround(
            x,
            y,
            z,
            onGround
        );
    }

    public static PlayerMoveC2SPacket.OnGroundOnly getOnGroundOnly(boolean onGround) {
        return new PlayerMoveC2SPacket.OnGroundOnly(
            onGround
        );
    }

    public static boolean isFallFlying(MinecraftClient mc) {
        return mc.player.isFallFlying();
    }

    public static boolean isJumping(MinecraftClient mc) {
        return mc.player.input.jumping;
    }

    public static boolean isSneaking(MinecraftClient mc) {
        return mc.player.input.sneaking;

    }

    public static int getTopY(MinecraftClient mc) {
        int y = mc.world.getTopY();
        return y;

    }

    public static Direction getOppositeDirectionTo(BlockPos blockPos) {
        Direction dir = Direction.fromRotation(Rotations.getYaw(blockPos)).getOpposite();

        return dir;
    }

    public static double getToughness(LivingEntity entity) {
        double value = entity.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
        return value;
    }

    public static void setRaycast(IRaycastContext raycastContext, Vec3d source, Vec3d vec3d, RaycastContext.ShapeType shapeType, RaycastContext.FluidHandling fluidHandling, ClientPlayerEntity player) {
        raycastContext.set(source, vec3d, shapeType, fluidHandling, player);
    }

    public static void setMovement(IVec3d movement, double x, double y, double z) {
        movement.set(x, y, z);
    }

    public static void drawTexture(DrawContext drawContext, Identifier texture2, int x, int y, int w, int h) {
        drawContext.drawTexture(texture2, x, y, 0, 0, w, h);
    }

    public static Vec3d playerKnockback(ExplosionS2CPacket packet) {
        return new Vec3d(packet.getPlayerVelocityX(), packet.getPlayerVelocityY(), packet.getPlayerVelocityZ());
    }

    public static int getSelectedSlot() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player.getInventory().selectedSlot;
    }

    public static void setSelectedSlot(int selectedSlot) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.player.getInventory().selectedSlot = selectedSlot;
    }

    public static NbtCompound getNbtCompound(NbtCompound tag, String key) {
        return tag.getCompound(key);
    }

    public static NbtList getNbtList(NbtCompound tag, String key) {
        return tag.getList(key, NbtElement.COMPOUND_TYPE);
    }
    public static boolean isNoneProfession(VillagerData data) {
        return data.getProfession() == VillagerProfession.NONE;
    }
    public static void sendPressShift() {

        MeteorClient.mc.getNetworkHandler()
            .sendPacket(new ClientCommandC2SPacket(MeteorClient.mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
    }

    public static void sendReleaseShift() {
        MeteorClient.mc.getNetworkHandler()
            .sendPacket(new ClientCommandC2SPacket(MeteorClient.mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
    }

    public static int getSyncId(InventoryS2CPacket inventoryS2CPacket) {
        return inventoryS2CPacket.getSyncId();
    }
    public static List<ItemStack> getInvContent(InventoryS2CPacket inventoryS2CPacket) {
        return inventoryS2CPacket.getContents();
    }
    public static VillagerProfession getVillagerProfession(VillagerEntity villager) {
        return villager.getVillagerData().getProfession();
    }

    public static float movementForward(Input input) {
        return input.movementForward;
    }


    public static float movementSideways(Input input) {
        return input.movementSideways;
    }

    public static void tagRollingEnchantment(NbtCompound tag, VillagerRoller.RollingEnchantment rolling) {
        rolling.enchantment = Identifier.tryParse(tag.getString("enchantment"));
        rolling.minLevel = tag.getInt("minLevel");
        rolling.maxCost = tag.getInt("maxCost");
        rolling.enabled = tag.getBoolean("enabled");
    }

    public static Vec3d getEntityPos(Entity entity) {
        return entity.getPos();
    }

    public  static World getEntityWorld(Entity entity) {
        return entity.getWorld();
    }

    public  static String getGameProfileName(PlayerEntity entity) {
        return entity.getGameProfile().getName();
    }

    public static int getKeyEventKey(KeyEvent event) {
        return event.key;
    }
}
