package com.github.mikumiku.addon.mixin;

import baritone.api.utils.BlockOptionalMetaLookup;
import baritone.pathing.movement.CalculationContext;
import baritone.process.MineProcess;
import com.github.mikumiku.addon.modules.SeedMine;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(MineProcess.class)
public class MineProcessMixin {

    @Shadow(remap = false)
    private List<BlockPos> a; // knownOreLocations

    @Inject(method = "a(Ljava/util/List;Lbaritone/pathing/movement/CalculationContext;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void onRescan(List<BlockPos> already, CalculationContext context, CallbackInfo ci) {
        SeedMine oreSim = Modules.get().get(SeedMine.class);
        if (oreSim == null || !oreSim.baritone())
            return;
        a = oreSim.oreGoals;
        ci.cancel();
    }

    @Redirect(method = "a(Lbaritone/pathing/movement/CalculationContext;Lbaritone/api/utils/BlockOptionalMetaLookup;Ljava/util/List;Lnet/minecraft/util/math/BlockPos;)Z",
        at = @At(value = "INVOKE", target = "Lbaritone/api/utils/BlockOptionalMetaLookup;has(Lnet/minecraft/block/BlockState;)Z"))
    private static boolean onPruneStream(BlockOptionalMetaLookup instance, BlockState blockState) {
        SeedMine oreSim = Modules.get().get(SeedMine.class);
        if (oreSim == null || !oreSim.baritone())
            return instance.has(blockState);
        return !blockState.isAir();
    }
}
