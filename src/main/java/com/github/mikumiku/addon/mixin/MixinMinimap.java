package com.github.mikumiku.addon.mixin;

import com.github.mikumiku.addon.mixinface.MagicMix;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.hud.minimap.Minimap;
import xaero.hud.minimap.module.MinimapSession;

/**
 * 小地图
 */
@Pseudo
@Mixin(targets = "xaero.hud.minimap.info.render.InfoDisplayRenderer")
public class MixinMinimap {

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void modifyPlayerPosForCompile(
        DrawContext guiGraphics,
        MinimapSession session,
        Minimap minimap,
        int height,
        int size,
        BlockPos playerPos,
        int scaledX,
        int scaledY,
        float mapScale,
        VertexConsumerProvider.Immediate renderTypeBuffer,
        CallbackInfo ci) {

        if (MagicMix.coordinatesisActive()) {
            // 强转为 mutableBlockPos 并设置固定坐标值
            if (playerPos instanceof BlockPos.Mutable) {
                BlockPos.Mutable mutablePos = (BlockPos.Mutable) playerPos;
                mutablePos.set(MagicMix.x, playerPos.getY(), MagicMix.z); // 设置为固定坐标
            }
        }
    }
}
