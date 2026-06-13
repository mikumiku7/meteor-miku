package com.github.mikumiku.addon.util;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

import java.util.Arrays;
import java.util.List;

public interface MikuConstant {

    List<Block> FUC_BLOCKS = Arrays.asList(
        // 容器类
        Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST, Blocks.BARREL,
        Blocks.HOPPER, Blocks.DROPPER, Blocks.DISPENSER,
        Blocks.WHITE_SHULKER_BOX, Blocks.ORANGE_SHULKER_BOX, Blocks.MAGENTA_SHULKER_BOX,
        Blocks.LIGHT_BLUE_SHULKER_BOX, Blocks.YELLOW_SHULKER_BOX, Blocks.LIME_SHULKER_BOX,
        Blocks.PINK_SHULKER_BOX, Blocks.GRAY_SHULKER_BOX, Blocks.CYAN_SHULKER_BOX,
        Blocks.PURPLE_SHULKER_BOX, Blocks.BLUE_SHULKER_BOX, Blocks.BROWN_SHULKER_BOX,
        Blocks.GREEN_SHULKER_BOX, Blocks.RED_SHULKER_BOX, Blocks.BLACK_SHULKER_BOX,

        // 功能方块
        Blocks.CRAFTING_TABLE, Blocks.ENCHANTING_TABLE, Blocks.ANVIL,
        Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL,
        Blocks.BREWING_STAND, Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER,
        Blocks.STONECUTTER, Blocks.GRINDSTONE, Blocks.LOOM, Blocks.SMITHING_TABLE,
        Blocks.COMPOSTER, Blocks.BEACON, Blocks.LECTERN, Blocks.BELL,

        // 红石交互
        Blocks.LEVER,
        Blocks.STONE_BUTTON, Blocks.OAK_BUTTON, Blocks.SPRUCE_BUTTON, Blocks.BIRCH_BUTTON,
        Blocks.JUNGLE_BUTTON, Blocks.ACACIA_BUTTON, Blocks.DARK_OAK_BUTTON,
        Blocks.MANGROVE_BUTTON, Blocks.CHERRY_BUTTON, Blocks.BAMBOO_BUTTON,
        Blocks.WARPED_BUTTON, Blocks.CRIMSON_BUTTON,
        Blocks.NOTE_BLOCK,

        // 门与活板门类
        Blocks.OAK_DOOR, Blocks.SPRUCE_DOOR, Blocks.BIRCH_DOOR, Blocks.JUNGLE_DOOR,
        Blocks.ACACIA_DOOR, Blocks.DARK_OAK_DOOR, Blocks.MANGROVE_DOOR,
        Blocks.CHERRY_DOOR, Blocks.BAMBOO_DOOR, Blocks.WARPED_DOOR, Blocks.CRIMSON_DOOR,
        Blocks.OAK_TRAPDOOR, Blocks.SPRUCE_TRAPDOOR, Blocks.BIRCH_TRAPDOOR,
        Blocks.JUNGLE_TRAPDOOR, Blocks.ACACIA_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR,
        Blocks.MANGROVE_TRAPDOOR, Blocks.CHERRY_TRAPDOOR, Blocks.BAMBOO_TRAPDOOR,
        Blocks.WARPED_TRAPDOOR, Blocks.CRIMSON_TRAPDOOR,

        // 建筑/杂项
        Blocks.SCAFFOLDING,
        Blocks.OAK_SIGN, Blocks.OAK_WALL_SIGN,
        Blocks.OAK_HANGING_SIGN, Blocks.OAK_WALL_HANGING_SIGN
    );

}
