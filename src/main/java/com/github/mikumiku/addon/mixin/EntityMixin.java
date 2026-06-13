package com.github.mikumiku.addon.mixin;

import com.github.mikumiku.addon.mixinface.MagicMix;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;


@Mixin(Entity.class)
public class EntityMixin {
    @Shadow
    protected UUID uuid;


    @Inject(at = @At("HEAD"), method = "getPose()Lnet/minecraft/entity/EntityPose;", require = 0, cancellable = true)
    private void getPose(CallbackInfoReturnable<EntityPose> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (MagicMix.eflyenabled() && this.uuid == mc.player.getUuid()) {
            cir.setReturnValue(EntityPose.STANDING);
        }
    }

    @Inject(at = @At("HEAD"), method = "isSprinting()Z", require = 0, cancellable = true)
    private void isSprinting(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (MagicMix.eflyenabled() && this.uuid == mc.player.getUuid()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(at = @At("HEAD"), method = "pushAwayFrom", require = 0, cancellable = true)
    private void pushAwayFrom(Entity entity, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && this.uuid == mc.player.getUuid() && MagicMix.eflyenabled() && !entity.getUuid().equals(this.uuid)) {
            ci.cancel();
        }
    }
}
