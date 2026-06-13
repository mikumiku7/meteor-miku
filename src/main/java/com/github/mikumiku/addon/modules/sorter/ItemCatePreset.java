package com.github.mikumiku.addon.modules.sorter;

import com.google.common.collect.Lists;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ItemCatePreset {


    public static Map<String, List<Item>> customCategoriesMap = new LinkedHashMap<>() {
        {

            // === 垃圾物品 ===
            put("垃圾物品", Lists.newArrayList(
                Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.FERMENTED_SPIDER_EYE, Items.BONE, Items.STRING, Items.FEATHER,
                Items.LEATHER, Items.RABBIT_HIDE, Items.SLIME_BALL, Items.PHANTOM_MEMBRANE, Items.STICK
            ));

            // === 食物类 ===
            put("土豆", Lists.newArrayList(
                Items.POTATO, Items.BAKED_POTATO, Items.POISONOUS_POTATO
            ));

            put("苹果", Lists.newArrayList(
                Items.APPLE, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE
            ));

            put("牛肉", Lists.newArrayList(
                Items.BEEF, Items.COOKED_BEEF
            ));

            put("猪肉", Lists.newArrayList(
                Items.PORKCHOP, Items.COOKED_PORKCHOP
            ));

            put("鸡肉", Lists.newArrayList(
                Items.CHICKEN, Items.COOKED_CHICKEN
            ));

            put("羊肉", Lists.newArrayList(
                Items.MUTTON, Items.COOKED_MUTTON
            ));

            put("兔肉", Lists.newArrayList(
                Items.RABBIT, Items.COOKED_RABBIT
            ));

            put("鳕鱼", Lists.newArrayList(
                Items.COD, Items.COOKED_COD
            ));

            put("鲑鱼", Lists.newArrayList(
                Items.SALMON, Items.COOKED_SALMON
            ));

            put("食物", Lists.newArrayList(
                // 常见农作物
                Items.APPLE, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE,
                Items.CARROT, Items.GOLDEN_CARROT,
                Items.POTATO, Items.BAKED_POTATO, Items.POISONOUS_POTATO,
                Items.BEETROOT, Items.BEETROOT_SOUP,
                Items.MELON_SLICE, Items.GLISTERING_MELON_SLICE,
                Items.PUMPKIN_PIE,
                Items.SWEET_BERRIES, Items.GLOW_BERRIES,
                // 小麦制品
                Items.BREAD, Items.COOKIE, Items.CAKE,
                // 蘑菇食物
                Items.MUSHROOM_STEW, Items.SUSPICIOUS_STEW, Items.RABBIT_STEW,
                // 鱼类
                Items.COD, Items.COOKED_COD,
                Items.SALMON, Items.COOKED_SALMON,
                Items.PUFFERFISH, Items.TROPICAL_FISH,
                // 肉类
                Items.BEEF, Items.COOKED_BEEF,
                Items.PORKCHOP, Items.COOKED_PORKCHOP,
                Items.MUTTON, Items.COOKED_MUTTON,
                Items.CHICKEN, Items.COOKED_CHICKEN,
                Items.RABBIT, Items.COOKED_RABBIT,
                // 特殊生物掉落
                Items.CHORUS_FRUIT
            ));

            // === 矿物类 ===
            put("煤炭", Lists.newArrayList(
                Items.COAL, Items.CHARCOAL
            ));

            put("铁", Lists.newArrayList(
                Items.IRON_INGOT, Items.RAW_IRON, Items.IRON_NUGGET
            ));

            put("金", Lists.newArrayList(
                Items.GOLD_INGOT, Items.RAW_GOLD, Items.GOLD_NUGGET
            ));

            put("铜", Lists.newArrayList(
                Items.COPPER_INGOT, Items.RAW_COPPER
            ));

            put("下界合金", Lists.newArrayList(
                Items.NETHERITE_INGOT, Items.NETHERITE_SCRAP, Items.ANCIENT_DEBRIS
            ));

            // === 方块类 ===
            put("石头", Lists.newArrayList(
                Items.STONE, Items.COBBLESTONE
            ));

            put("泥土", Lists.newArrayList(
                Items.DIRT, Items.COARSE_DIRT, Items.GRASS_BLOCK
            ));

            put("沙子", Lists.newArrayList(
                Items.SAND, Items.RED_SAND
            ));

            put("圆石", Lists.newArrayList(
                Items.COBBLESTONE, Items.MOSSY_COBBLESTONE
            ));

            put("原木", Lists.newArrayList(
                Items.OAK_LOG, Items.SPRUCE_LOG, Items.BIRCH_LOG, Items.JUNGLE_LOG, Items.ACACIA_LOG, Items.DARK_OAK_LOG,
                Items.MANGROVE_LOG, Items.CHERRY_LOG, Items.BAMBOO_BLOCK, Items.CRIMSON_STEM, Items.WARPED_STEM,
                Items.STRIPPED_OAK_LOG, Items.STRIPPED_SPRUCE_LOG, Items.STRIPPED_BIRCH_LOG, Items.STRIPPED_JUNGLE_LOG,
                Items.STRIPPED_ACACIA_LOG, Items.STRIPPED_DARK_OAK_LOG, Items.STRIPPED_MANGROVE_LOG, Items.STRIPPED_CHERRY_LOG,
                Items.STRIPPED_BAMBOO_BLOCK, Items.STRIPPED_CRIMSON_STEM, Items.STRIPPED_WARPED_STEM
            ));

            // === 颜色变种 ===
            put("羊毛", Lists.newArrayList(
                Items.WHITE_WOOL, Items.ORANGE_WOOL, Items.MAGENTA_WOOL, Items.LIGHT_BLUE_WOOL, Items.YELLOW_WOOL, Items.LIME_WOOL,
                Items.PINK_WOOL, Items.GRAY_WOOL, Items.LIGHT_GRAY_WOOL, Items.CYAN_WOOL, Items.PURPLE_WOOL, Items.BLUE_WOOL,
                Items.BROWN_WOOL, Items.GREEN_WOOL, Items.RED_WOOL, Items.BLACK_WOOL
            ));

            put("混凝土", Lists.newArrayList(
                Items.WHITE_CONCRETE, Items.ORANGE_CONCRETE, Items.MAGENTA_CONCRETE, Items.LIGHT_BLUE_CONCRETE, Items.YELLOW_CONCRETE, Items.LIME_CONCRETE,
                Items.PINK_CONCRETE, Items.GRAY_CONCRETE, Items.LIGHT_GRAY_CONCRETE, Items.CYAN_CONCRETE, Items.PURPLE_CONCRETE, Items.BLUE_CONCRETE,
                Items.BROWN_CONCRETE, Items.GREEN_CONCRETE, Items.RED_CONCRETE, Items.BLACK_CONCRETE
            ));

            put("陶瓦", Lists.newArrayList(
                Items.TERRACOTTA, Items.WHITE_TERRACOTTA, Items.ORANGE_TERRACOTTA, Items.MAGENTA_TERRACOTTA, Items.LIGHT_BLUE_TERRACOTTA,
                Items.YELLOW_TERRACOTTA, Items.LIME_TERRACOTTA, Items.PINK_TERRACOTTA, Items.GRAY_TERRACOTTA, Items.LIGHT_GRAY_TERRACOTTA,
                Items.CYAN_TERRACOTTA, Items.PURPLE_TERRACOTTA, Items.BLUE_TERRACOTTA, Items.BROWN_TERRACOTTA, Items.GREEN_TERRACOTTA,
                Items.RED_TERRACOTTA, Items.BLACK_TERRACOTTA
            ));

            put("玻璃", Lists.newArrayList(
                Items.GLASS, Items.TINTED_GLASS, Items.GLASS_PANE,
                Items.WHITE_STAINED_GLASS, Items.ORANGE_STAINED_GLASS, Items.MAGENTA_STAINED_GLASS, Items.LIGHT_BLUE_STAINED_GLASS,
                Items.YELLOW_STAINED_GLASS, Items.LIME_STAINED_GLASS, Items.PINK_STAINED_GLASS, Items.GRAY_STAINED_GLASS,
                Items.LIGHT_GRAY_STAINED_GLASS, Items.CYAN_STAINED_GLASS, Items.PURPLE_STAINED_GLASS, Items.BLUE_STAINED_GLASS,
                Items.BROWN_STAINED_GLASS, Items.GREEN_STAINED_GLASS, Items.RED_STAINED_GLASS, Items.BLACK_STAINED_GLASS,
                Items.WHITE_STAINED_GLASS_PANE, Items.ORANGE_STAINED_GLASS_PANE, Items.MAGENTA_STAINED_GLASS_PANE, Items.LIGHT_BLUE_STAINED_GLASS_PANE,
                Items.YELLOW_STAINED_GLASS_PANE, Items.LIME_STAINED_GLASS_PANE, Items.PINK_STAINED_GLASS_PANE, Items.GRAY_STAINED_GLASS_PANE,
                Items.LIGHT_GRAY_STAINED_GLASS_PANE, Items.CYAN_STAINED_GLASS_PANE, Items.PURPLE_STAINED_GLASS_PANE, Items.BLUE_STAINED_GLASS_PANE,
                Items.BROWN_STAINED_GLASS_PANE, Items.GREEN_STAINED_GLASS_PANE, Items.RED_STAINED_GLASS_PANE, Items.BLACK_STAINED_GLASS_PANE
            ));

            put("地毯", Lists.newArrayList(
                Items.WHITE_CARPET, Items.ORANGE_CARPET, Items.MAGENTA_CARPET, Items.LIGHT_BLUE_CARPET, Items.YELLOW_CARPET, Items.LIME_CARPET,
                Items.PINK_CARPET, Items.GRAY_CARPET, Items.LIGHT_GRAY_CARPET, Items.CYAN_CARPET, Items.PURPLE_CARPET, Items.BLUE_CARPET,
                Items.BROWN_CARPET, Items.GREEN_CARPET, Items.RED_CARPET, Items.BLACK_CARPET
            ));

            put("床", Lists.newArrayList(
                Items.WHITE_BED, Items.ORANGE_BED, Items.MAGENTA_BED, Items.LIGHT_BLUE_BED, Items.YELLOW_BED, Items.LIME_BED,
                Items.PINK_BED, Items.GRAY_BED, Items.LIGHT_GRAY_BED, Items.CYAN_BED, Items.PURPLE_BED, Items.BLUE_BED,
                Items.BROWN_BED, Items.GREEN_BED, Items.RED_BED, Items.BLACK_BED
            ));

            // === 收藏品 ===
            put("音乐唱片", Lists.newArrayList(
                Items.MUSIC_DISC_13, Items.MUSIC_DISC_CAT, Items.MUSIC_DISC_BLOCKS, Items.MUSIC_DISC_CHIRP,
                Items.MUSIC_DISC_FAR, Items.MUSIC_DISC_MALL, Items.MUSIC_DISC_MELLOHI, Items.MUSIC_DISC_STAL,
                Items.MUSIC_DISC_STRAD, Items.MUSIC_DISC_WARD, Items.MUSIC_DISC_11, Items.MUSIC_DISC_WAIT,
                Items.MUSIC_DISC_OTHERSIDE, Items.MUSIC_DISC_5, Items.MUSIC_DISC_PIGSTEP
            ));

            put("陶片", Lists.newArrayList(
                Items.ANGLER_POTTERY_SHERD, Items.ARCHER_POTTERY_SHERD, Items.ARMS_UP_POTTERY_SHERD, Items.BLADE_POTTERY_SHERD,
                Items.BREWER_POTTERY_SHERD, Items.BURN_POTTERY_SHERD, Items.DANGER_POTTERY_SHERD, Items.EXPLORER_POTTERY_SHERD,
                Items.FRIEND_POTTERY_SHERD, Items.HEART_POTTERY_SHERD, Items.HEARTBREAK_POTTERY_SHERD, Items.HOWL_POTTERY_SHERD,
                Items.MINER_POTTERY_SHERD, Items.MOURNER_POTTERY_SHERD, Items.PLENTY_POTTERY_SHERD, Items.PRIZE_POTTERY_SHERD,
                Items.SCRAPE_POTTERY_SHERD, Items.SHEAF_POTTERY_SHERD, Items.SHELTER_POTTERY_SHERD, Items.SKULL_POTTERY_SHERD,
                Items.SNORT_POTTERY_SHERD
            ));

            put("超稀有", Lists.newArrayList(
                Items.NETHER_STAR, Items.BEACON, Items.CONDUIT, Items.DRAGON_HEAD
            ));

            put("不死图腾", Lists.newArrayList(
                Items.TOTEM_OF_UNDYING
            ));

            put("鞘翅", Lists.newArrayList(
                Items.ELYTRA
            ));

            put("附魔书", Lists.newArrayList(
                Items.ENCHANTED_BOOK
            ));

            put("药水", Lists.newArrayList(
                Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION
            ));


            // === 建筑材料 ===
            put("木制品", Lists.newArrayList(
                // 木板
                Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS, Items.JUNGLE_PLANKS, Items.ACACIA_PLANKS, Items.DARK_OAK_PLANKS,
                Items.MANGROVE_PLANKS, Items.CHERRY_PLANKS, Items.BAMBOO_PLANKS, Items.CRIMSON_PLANKS, Items.WARPED_PLANKS,
                // 木材
                Items.OAK_WOOD, Items.SPRUCE_WOOD, Items.BIRCH_WOOD, Items.JUNGLE_WOOD, Items.ACACIA_WOOD, Items.DARK_OAK_WOOD,
                Items.MANGROVE_WOOD, Items.CHERRY_WOOD, Items.BAMBOO_MOSAIC, Items.CRIMSON_HYPHAE, Items.WARPED_HYPHAE,
                Items.STRIPPED_OAK_WOOD, Items.STRIPPED_SPRUCE_WOOD, Items.STRIPPED_BIRCH_WOOD, Items.STRIPPED_JUNGLE_WOOD,
                Items.STRIPPED_ACACIA_WOOD, Items.STRIPPED_DARK_OAK_WOOD, Items.STRIPPED_MANGROVE_WOOD, Items.STRIPPED_CHERRY_WOOD,
                Items.STRIPPED_CRIMSON_HYPHAE, Items.STRIPPED_WARPED_HYPHAE,
                // 制品
                Items.OAK_SLAB, Items.SPRUCE_SLAB, Items.BIRCH_SLAB, Items.JUNGLE_SLAB, Items.ACACIA_SLAB, Items.DARK_OAK_SLAB,
                Items.MANGROVE_SLAB, Items.CHERRY_SLAB, Items.BAMBOO_SLAB, Items.CRIMSON_SLAB, Items.WARPED_SLAB,
                Items.OAK_STAIRS, Items.SPRUCE_STAIRS, Items.BIRCH_STAIRS, Items.JUNGLE_STAIRS, Items.ACACIA_STAIRS, Items.DARK_OAK_STAIRS,
                Items.MANGROVE_STAIRS, Items.CHERRY_STAIRS, Items.BAMBOO_STAIRS, Items.CRIMSON_STAIRS, Items.WARPED_STAIRS,
                // 栅栏 & 门类
                Items.OAK_FENCE, Items.SPRUCE_FENCE, Items.BIRCH_FENCE, Items.JUNGLE_FENCE, Items.ACACIA_FENCE, Items.DARK_OAK_FENCE,
                Items.MANGROVE_FENCE, Items.CHERRY_FENCE, Items.BAMBOO_FENCE, Items.CRIMSON_FENCE, Items.WARPED_FENCE,
                Items.OAK_FENCE_GATE, Items.SPRUCE_FENCE_GATE, Items.BIRCH_FENCE_GATE, Items.JUNGLE_FENCE_GATE, Items.ACACIA_FENCE_GATE,
                Items.DARK_OAK_FENCE_GATE, Items.MANGROVE_FENCE_GATE, Items.CHERRY_FENCE_GATE, Items.BAMBOO_FENCE_GATE,
                Items.CRIMSON_FENCE_GATE, Items.WARPED_FENCE_GATE,
                Items.OAK_DOOR, Items.SPRUCE_DOOR, Items.BIRCH_DOOR, Items.JUNGLE_DOOR, Items.ACACIA_DOOR, Items.DARK_OAK_DOOR,
                Items.MANGROVE_DOOR, Items.CHERRY_DOOR, Items.BAMBOO_DOOR, Items.CRIMSON_DOOR, Items.WARPED_DOOR,
                Items.OAK_TRAPDOOR, Items.SPRUCE_TRAPDOOR, Items.BIRCH_TRAPDOOR, Items.JUNGLE_TRAPDOOR, Items.ACACIA_TRAPDOOR,
                Items.DARK_OAK_TRAPDOOR, Items.MANGROVE_TRAPDOOR, Items.CHERRY_TRAPDOOR, Items.BAMBOO_TRAPDOOR,
                Items.CRIMSON_TRAPDOOR, Items.WARPED_TRAPDOOR,
                // 特殊木制品
                Items.LADDER, Items.CRAFTING_TABLE, Items.CARTOGRAPHY_TABLE, Items.FLETCHING_TABLE, Items.SMITHING_TABLE, Items.LOOM,
                Items.BARREL, Items.COMPOSTER, Items.CHEST, Items.TRAPPED_CHEST, Items.BOOKSHELF, Items.CHISELED_BOOKSHELF,
                Items.JUKEBOX, Items.NOTE_BLOCK
            ));

            put("石制品", Lists.newArrayList(
                // 基础石材
                Items.MOSSY_COBBLESTONE, Items.GRANITE, Items.POLISHED_GRANITE, Items.DIORITE, Items.POLISHED_DIORITE,
                Items.ANDESITE, Items.POLISHED_ANDESITE, Items.TUFF, Items.POLISHED_TUFF, Items.CALCITE, Items.DRIPSTONE_BLOCK,
                // 石砖
                Items.STONE_BRICKS, Items.MOSSY_STONE_BRICKS, Items.CRACKED_STONE_BRICKS, Items.CHISELED_STONE_BRICKS,
                // 黑石
                Items.BLACKSTONE, Items.POLISHED_BLACKSTONE, Items.CHISELED_POLISHED_BLACKSTONE,
                Items.POLISHED_BLACKSTONE_BRICKS, Items.CRACKED_POLISHED_BLACKSTONE_BRICKS,
                // 深板岩
                Items.DEEPSLATE, Items.COBBLED_DEEPSLATE, Items.POLISHED_DEEPSLATE,
                Items.DEEPSLATE_BRICKS, Items.CRACKED_DEEPSLATE_BRICKS,
                Items.DEEPSLATE_TILES, Items.CRACKED_DEEPSLATE_TILES, Items.CHISELED_DEEPSLATE,
                // 石英
                Items.QUARTZ_BLOCK, Items.CHISELED_QUARTZ_BLOCK, Items.QUARTZ_BRICKS, Items.SMOOTH_QUARTZ, Items.QUARTZ_PILLAR,
                // 沙岩
                Items.SANDSTONE, Items.CHISELED_SANDSTONE, Items.CUT_SANDSTONE, Items.SMOOTH_SANDSTONE,
                Items.RED_SANDSTONE, Items.CHISELED_RED_SANDSTONE, Items.CUT_RED_SANDSTONE, Items.SMOOTH_RED_SANDSTONE,
                // 玄武岩
                Items.BASALT, Items.POLISHED_BASALT,
                // 制品（台阶/楼梯/墙）
                Items.STONE_SLAB, Items.COBBLESTONE_SLAB, Items.MOSSY_COBBLESTONE_SLAB,
                Items.GRANITE_SLAB, Items.POLISHED_GRANITE_SLAB, Items.DIORITE_SLAB, Items.POLISHED_DIORITE_SLAB,
                Items.ANDESITE_SLAB, Items.POLISHED_ANDESITE_SLAB, Items.TUFF_SLAB, Items.POLISHED_TUFF_SLAB,
                Items.STONE_BRICK_SLAB, Items.MOSSY_STONE_BRICK_SLAB, Items.BLACKSTONE_SLAB, Items.POLISHED_BLACKSTONE_SLAB,
                Items.POLISHED_BLACKSTONE_BRICK_SLAB, Items.DEEPSLATE_BRICK_SLAB, Items.DEEPSLATE_TILE_SLAB, Items.QUARTZ_SLAB,
                Items.SMOOTH_QUARTZ_SLAB, Items.SANDSTONE_SLAB, Items.CUT_SANDSTONE_SLAB, Items.SMOOTH_SANDSTONE_SLAB,
                Items.RED_SANDSTONE_SLAB, Items.CUT_RED_SANDSTONE_SLAB, Items.SMOOTH_RED_SANDSTONE_SLAB,
                Items.STONE_STAIRS, Items.COBBLESTONE_STAIRS, Items.MOSSY_COBBLESTONE_STAIRS,
                Items.GRANITE_STAIRS, Items.POLISHED_GRANITE_STAIRS, Items.DIORITE_STAIRS, Items.POLISHED_DIORITE_STAIRS,
                Items.ANDESITE_STAIRS, Items.POLISHED_ANDESITE_STAIRS, Items.TUFF_STAIRS, Items.POLISHED_TUFF_STAIRS,
                Items.STONE_BRICK_STAIRS, Items.MOSSY_STONE_BRICK_STAIRS, Items.BLACKSTONE_STAIRS, Items.POLISHED_BLACKSTONE_STAIRS,
                Items.POLISHED_BLACKSTONE_BRICK_STAIRS, Items.DEEPSLATE_BRICK_STAIRS, Items.DEEPSLATE_TILE_STAIRS, Items.QUARTZ_STAIRS,
                Items.SMOOTH_QUARTZ_STAIRS, Items.SANDSTONE_STAIRS, Items.SMOOTH_SANDSTONE_STAIRS,
                Items.RED_SANDSTONE_STAIRS, Items.SMOOTH_RED_SANDSTONE_STAIRS,
                Items.COBBLESTONE_WALL, Items.MOSSY_COBBLESTONE_WALL, Items.GRANITE_WALL, Items.DIORITE_WALL, Items.ANDESITE_WALL,
                Items.TUFF_WALL, Items.STONE_BRICK_WALL, Items.MOSSY_STONE_BRICK_WALL, Items.BLACKSTONE_WALL,
                Items.POLISHED_BLACKSTONE_WALL, Items.POLISHED_BLACKSTONE_BRICK_WALL, Items.DEEPSLATE_BRICK_WALL,
                Items.DEEPSLATE_TILE_WALL, Items.SANDSTONE_WALL, Items.RED_SANDSTONE_WALL
            ));

            // === 功能性方块 ===
            put("活板门", Lists.newArrayList(
                Items.IRON_TRAPDOOR, Items.OAK_TRAPDOOR, Items.SPRUCE_TRAPDOOR, Items.BIRCH_TRAPDOOR, Items.JUNGLE_TRAPDOOR,
                Items.ACACIA_TRAPDOOR, Items.DARK_OAK_TRAPDOOR, Items.MANGROVE_TRAPDOOR, Items.CHERRY_TRAPDOOR,
                Items.BAMBOO_TRAPDOOR, Items.CRIMSON_TRAPDOOR, Items.WARPED_TRAPDOOR
            ));

            put("门", Lists.newArrayList(
                Items.IRON_DOOR, Items.OAK_DOOR, Items.SPRUCE_DOOR, Items.BIRCH_DOOR, Items.JUNGLE_DOOR,
                Items.ACACIA_DOOR, Items.DARK_OAK_DOOR, Items.MANGROVE_DOOR, Items.CHERRY_DOOR,
                Items.BAMBOO_DOOR, Items.CRIMSON_DOOR, Items.WARPED_DOOR
            ));

            put("栅栏门", Lists.newArrayList(
                Items.OAK_FENCE_GATE, Items.SPRUCE_FENCE_GATE, Items.BIRCH_FENCE_GATE, Items.JUNGLE_FENCE_GATE,
                Items.ACACIA_FENCE_GATE, Items.DARK_OAK_FENCE_GATE, Items.MANGROVE_FENCE_GATE, Items.CHERRY_FENCE_GATE,
                Items.BAMBOO_FENCE_GATE, Items.CRIMSON_FENCE_GATE, Items.WARPED_FENCE_GATE
            ));

            put("铁轨", Lists.newArrayList(
                Items.RAIL, Items.POWERED_RAIL, Items.DETECTOR_RAIL, Items.ACTIVATOR_RAIL
            ));

            // === 红石 ===
            put("红石", Lists.newArrayList(
                // 基础红石
                Items.REDSTONE, Items.REDSTONE_BLOCK, Items.REDSTONE_TORCH, Items.REDSTONE_LAMP,
                // 信号逻辑
                Items.REPEATER, Items.COMPARATOR, Items.OBSERVER,
                // 动力装置
                Items.PISTON, Items.STICKY_PISTON,
                // 交互容器
                Items.DROPPER, Items.DISPENSER, Items.HOPPER, Items.LECTERN, Items.TRAPPED_CHEST, Items.TARGET,
                // 触发器 - 压力板
                Items.STONE_PRESSURE_PLATE, Items.OAK_PRESSURE_PLATE, Items.SPRUCE_PRESSURE_PLATE, Items.BIRCH_PRESSURE_PLATE,
                Items.JUNGLE_PRESSURE_PLATE, Items.ACACIA_PRESSURE_PLATE, Items.DARK_OAK_PRESSURE_PLATE, Items.MANGROVE_PRESSURE_PLATE,
                Items.CHERRY_PRESSURE_PLATE, Items.BAMBOO_PRESSURE_PLATE, Items.CRIMSON_PRESSURE_PLATE, Items.WARPED_PRESSURE_PLATE,
                Items.POLISHED_BLACKSTONE_PRESSURE_PLATE, Items.LIGHT_WEIGHTED_PRESSURE_PLATE, Items.HEAVY_WEIGHTED_PRESSURE_PLATE,
                // 触发器 - 按钮
                Items.STONE_BUTTON, Items.OAK_BUTTON, Items.SPRUCE_BUTTON, Items.BIRCH_BUTTON, Items.JUNGLE_BUTTON,
                Items.ACACIA_BUTTON, Items.DARK_OAK_BUTTON, Items.MANGROVE_BUTTON, Items.CHERRY_BUTTON, Items.BAMBOO_BUTTON,
                Items.CRIMSON_BUTTON, Items.WARPED_BUTTON, Items.POLISHED_BLACKSTONE_BUTTON,
                // 触发器 - 其他
                Items.TRIPWIRE_HOOK, Items.LEVER, Items.DAYLIGHT_DETECTOR,
                // 其他
                Items.NOTE_BLOCK, Items.TNT,
                // sculk 系列
                Items.SCULK_SENSOR, Items.CALIBRATED_SCULK_SENSOR, Items.SCULK_SHRIEKER
            ));

            // === 装备 ===
            put("装备", Lists.newArrayList(
                // 木质工具
                Items.WOODEN_SWORD, Items.WOODEN_PICKAXE, Items.WOODEN_AXE, Items.WOODEN_SHOVEL, Items.WOODEN_HOE,
                // 石质工具
                Items.STONE_SWORD, Items.STONE_PICKAXE, Items.STONE_AXE, Items.STONE_SHOVEL, Items.STONE_HOE,
                // 铁质工具
                Items.IRON_SWORD, Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_HOE,
                // 金质工具
                Items.GOLDEN_SWORD, Items.GOLDEN_PICKAXE, Items.GOLDEN_AXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_HOE,
                // 钻石工具
                Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE,
                // 下界合金工具
                Items.NETHERITE_SWORD, Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE,
                // 远程武器
                Items.BOW, Items.CROSSBOW, Items.TRIDENT,
                // 盔甲 - 皮革
                Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS,
                // 盔甲 - 铁
                Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
                // 盔甲 - 金
                Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS,
                // 盔甲 - 钻石
                Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS,
                // 盔甲 - 下界合金
                Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS,
                // 马铠
                Items.IRON_HORSE_ARMOR, Items.GOLDEN_HORSE_ARMOR, Items.DIAMOND_HORSE_ARMOR, Items.LEATHER_HORSE_ARMOR,
                // 其他装备
                Items.SHIELD, Items.FISHING_ROD, Items.CARROT_ON_A_STICK, Items.WARPED_FUNGUS_ON_A_STICK, Items.FLINT_AND_STEEL
            ));
        }
    };

}
