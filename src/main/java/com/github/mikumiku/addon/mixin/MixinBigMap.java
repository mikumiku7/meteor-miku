package com.github.mikumiku.addon.mixin;

import com.github.mikumiku.addon.mixinface.MagicMix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 世界地图显示坐标信息
 */
@Pseudo
@Mixin(targets = "xaero.map.graphics.MapRenderHelper")
public class MixinBigMap {

    @ModifyVariable(
        method = "drawCenteredStringWithBackground",
        at = @At("HEAD"),
        argsOnly = true,
        require = 0
    )
    private static String modifyCoordinatesStringOnly(String text) {

        if (MagicMix.coordinatesisActive() && text.contains("X:") && text.contains("Z:")) {
            text = text
                .replaceAll("X:\\s*-?\\d+", "X: " + ((int) MagicMix.x))
                .replaceAll("Z:\\s*-?\\d+", "Z: " + ((int) MagicMix.z));
        }

        return text; // 返回修改后的文本
    }
}
