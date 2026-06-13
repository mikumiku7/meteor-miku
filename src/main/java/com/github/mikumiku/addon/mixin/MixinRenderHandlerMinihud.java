package com.github.mikumiku.addon.mixin;

import com.github.mikumiku.addon.mixinface.MagicMix;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Pseudo
@Mixin(targets = "fi.dy.masa.minihud.event.RenderHandler")
public class MixinRenderHandlerMinihud {

    @Redirect(method = "addLine(Lfi/dy/masa/minihud/config/InfoToggle;)V",
        slice = @Slice(
            from = @At(value = "FIELD", target = "Lfi/dy/masa/minihud/config/InfoToggle;COORDINATES:Lfi/dy/masa/minihud/config/InfoToggle;"),
            to = @At(value = "INVOKE", target = "Ljava/lang/StringBuilder;toString()Ljava/lang/String;")
        ),
        require = 0,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getX()D", ordinal = 0))
    private double redirectCoordinateX(Entity entity) {

        if (MagicMix.coordinatesisActive()) {
            return MagicMix.x; // 修改X坐标为固定值
        } else {
            return entity.getX();
        }
    }

    @Redirect(method = "addLine(Lfi/dy/masa/minihud/config/InfoToggle;)V",
        slice = @Slice(
            from = @At(value = "FIELD", target = "Lfi/dy/masa/minihud/config/InfoToggle;COORDINATES:Lfi/dy/masa/minihud/config/InfoToggle;"),
            to = @At(value = "INVOKE", target = "Ljava/lang/StringBuilder;toString()Ljava/lang/String;")
        ),
        require = 0,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getZ()D"))
    private double redirectCoordinateZ(Entity entity) {
        if (MagicMix.coordinatesisActive()) {
            return MagicMix.z; // 修改X坐标为固定值
        } else {
            return entity.getZ();
        }
    }


}
