package com.github.mikumiku.addon.mixin;

import com.github.mikumiku.addon.mixinface.MagicMix;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow
    public abstract Brain<?> getBrain();

    @Inject(at = @At("HEAD"), method = "isFallFlying", require = 0, cancellable = true)
    private void isFallFlying(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.player.getBrain().equals(this.getBrain()) && MagicMix.eflyenabled()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(at = @At("HEAD"), method = "isGliding", require = 0, cancellable = true)
    private void isGliding2(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.player.getBrain().equals(this.getBrain()) && MagicMix.eflyenabled()) {
            cir.setReturnValue(true);
        }
    }
}
