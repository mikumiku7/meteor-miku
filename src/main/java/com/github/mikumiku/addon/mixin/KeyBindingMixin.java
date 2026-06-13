package com.github.mikumiku.addon.mixin;

import com.github.mikumiku.addon.mixinface.MagicMix;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin {

//    @Final
//    @Shadow
//    private String translationKey;
//
//    @Inject(at = @At("RETURN"), method = "isPressed", require = 0, cancellable = true)
//    public void isPressed(CallbackInfoReturnable<Boolean> cir) {
//        if (MagicMix.eflyenabled() && translationKey.equals("key.forward")) {
//            cir.setReturnValue(true);
//        }
//    }
}
