package com.github.mikumiku.addon.mixin;

import meteordevelopment.meteorclient.systems.modules.render.blockesp.ESPChunk;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Mixin
 */
@Mixin(ESPChunk.class)
public abstract class BlockESPMixin {


    @Inject(method = "searchChunk",
        at = @At("HEAD"), cancellable = true)
    private static void injectSearchChunk(Chunk chunk, List<Block> blocks, CallbackInfoReturnable<ESPChunk> cir) {
        ESPChunk schunk = new ESPChunk(chunk.getPos().x, chunk.getPos().z);
        if (schunk.shouldBeDeleted()) {
            cir.setReturnValue(schunk);
            return;
        }

        BlockPos.Mutable blockPos = new BlockPos.Mutable();
        BlockPos playerPos = mc.player.getBlockPos();

        for (int x = chunk.getPos().getStartX(); x <= chunk.getPos().getEndX(); x++) {
            for (int z = chunk.getPos().getStartZ(); z <= chunk.getPos().getEndZ(); z++) {
                int height = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE).get(x - chunk.getPos().getStartX(), z - chunk.getPos().getStartZ());

                for (int y = mc.world.getBottomY(); y < height; y++) {
                    blockPos.set(x, y, z);
                    BlockState bs = chunk.getBlockState(blockPos);
                    Block block = bs.getBlock();

                    if (blocks.contains(block)) {
                        if (block instanceof FluidBlock) {
                            Integer level = bs.get(FluidBlock.LEVEL);
                            if (level != null && level == 0) {
                                // 水源距离限制
                                if (block == Blocks.WATER) {
                                    double distanceSq = blockPos.getSquaredDistance(playerPos);
                                    if (distanceSq > (64 * 64)) continue; // 跳过渲染
                                }
                                schunk.add(blockPos, false);
                            }
                        } else {
                            schunk.add(blockPos, false);
                        }
                    }
                }
            }
        }

        cir.setReturnValue(schunk);
    }
}
