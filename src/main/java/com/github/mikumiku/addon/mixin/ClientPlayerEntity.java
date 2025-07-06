package com.github.mikumiku.addon.mixin;

import com.github.mikumiku.addon.MoveEvent;
import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = net.minecraft.client.network.ClientPlayerEntity.class, priority = 800)
public abstract class ClientPlayerEntity extends AbstractClientPlayerEntity {

    @Shadow
    public abstract float getPitch(float tickDelta);

    private ClientPlayerEntity(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"), cancellable = true)
    public void onMoveHook(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        MoveEvent event = new MoveEvent(movement.x, movement.y, movement.z);
        MeteorClient.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            super.move(movementType, new Vec3d(event.getX(), event.getY(), event.getZ()));
            ci.cancel();
        }
    }

//    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
//    private void sendMovementPacketsHook(CallbackInfo info) {
//        SyncEvent event = new SyncEvent(getYaw(), getPitch());
//        MeteorClient.EVENT_BUS.post(event);
//        if (event.isCancelled()) info.cancel();
//    }
}
