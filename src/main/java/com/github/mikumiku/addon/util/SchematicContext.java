package com.github.mikumiku.addon.util;

import com.google.gson.Gson;
import fi.dy.masa.litematica.world.WorldSchematic;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class SchematicContext {
    public final World world;
    public final WorldSchematic schematic;
    public final BlockPos blockPos;
    public final BlockState targetState;
    public final BlockState currentState;

    public SchematicContext(World world, WorldSchematic schematic, BlockPos blockPos) {
        this.world = world;
        this.schematic = schematic;
        this.blockPos = blockPos;
        this.targetState = schematic.getBlockState(blockPos);
        this.currentState = world.getBlockState(blockPos);
    }

    public SchematicContext offset(Direction direction) {
        return new SchematicContext(this.world, this.schematic, this.blockPos.offset(direction));
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}

