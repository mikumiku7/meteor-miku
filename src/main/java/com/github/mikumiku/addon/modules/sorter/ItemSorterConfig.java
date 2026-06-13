package com.github.mikumiku.addon.modules.sorter;


import java.util.*;

public class ItemSorterConfig {

    // 要处理的箱子坐标列表
    public List<ChestLocation> chestLocations = new ArrayList<>();


    // 箱子缓存更新间隔s
    public int chestCacheUpdateInterval = 600; // 10秒

    // 用户自定义分类规则
    public Map<String, List<String>> customCategories = new HashMap<>();



    // 初始化默认配置
    public ItemSorterConfig() {
//        initializeDefaultCategories();
    }

    public void init() {
        if (!customCategories.isEmpty()) {
            return;
        }

        // === 多物品分类组（只保留包含多个物品的分类）===

        // 土豆类（包含变种）
        customCategories.put("potato", Arrays.asList("potato", "baked_potato", "poisonous_potato"));

        // 苹果类（包含变种）
        customCategories.put("apple", Arrays.asList("apple", "golden_apple", "enchanted_golden_apple"));

        // 牛肉类（生熟一起）
        customCategories.put("beef", Arrays.asList("beef", "cooked_beef"));

        // 猪肉类（生熟一起）
        customCategories.put("porkchop", Arrays.asList("porkchop", "cooked_porkchop"));

        // 鸡肉类（生熟一起）
        customCategories.put("chicken", Arrays.asList("chicken", "cooked_chicken"));

        // 羊肉类（生熟一起）
        customCategories.put("mutton", Arrays.asList("mutton", "cooked_mutton"));

        // 兔肉类（生熟一起）
        customCategories.put("rabbit", Arrays.asList("rabbit", "cooked_rabbit"));

        // 鳕鱼类（生熟一起）
        customCategories.put("cod", Arrays.asList("cod", "cooked_cod"));

        // 鲑鱼类（生熟一起）
        customCategories.put("salmon", Arrays.asList("salmon", "cooked_salmon"));

        customCategories.put("food", Arrays.asList(
            // 常见农作物
            "apple", "golden_apple", "enchanted_golden_apple",
            "carrot", "golden_carrot",
            "potato", "baked_potato", "poisonous_potato",
            "beetroot", "beetroot_soup",
            "melon_slice", "glistering_melon_slice",
            "pumpkin_pie",
            "sweet_berries", "glow_berries",

            // 小麦制品
            "bread", "cookie", "cake",

            // 蘑菇食物
            "mushroom_stew", "suspicious_stew", "rabbit_stew",

            // 鱼类
            "cod", "cooked_cod",
            "salmon", "cooked_salmon",
            "pufferfish", "tropical_fish",

            // 肉类
            "beef", "cooked_beef", // 牛排
            "porkchop", "cooked_porkchop",
            "mutton", "cooked_mutton",
            "chicken", "cooked_chicken",
            "rabbit", "cooked_rabbit",

            // 特殊生物掉落
            "chorus_fruit"
        ));


        // 煤炭类（包含木炭）
        customCategories.put("coal", Arrays.asList("coal", "charcoal"));

        // 铁类（包含相关物品）
        customCategories.put("iron", Arrays.asList("iron_ingot", "raw_iron", "iron_nugget"));

        // 金类（包含相关物品）
        customCategories.put("gold", Arrays.asList("gold_ingot", "raw_gold", "gold_nugget"));

        // 铜类（包含相关物品）
        customCategories.put("copper", Arrays.asList("copper_ingot", "raw_copper"));

        // 下界合金类（包含相关物品）
        customCategories.put("netherite", Arrays.asList("netherite_ingot", "netherite_scrap", "ancient_debris"));

        // 石头类（包含圆石）
        customCategories.put("stone", Arrays.asList("stone", "cobblestone"));

        // 泥土类（包含变种）
        customCategories.put("dirt", Arrays.asList("dirt", "coarse_dirt", "grass_block"));

        // 沙子类（包含红沙）
        customCategories.put("sand", Arrays.asList("sand", "red_sand"));

        // 圆石类（包含苔石圆石）
        customCategories.put("cobblestone", Arrays.asList("cobblestone", "mossy_cobblestone"));

        // 原木类（包含去皮版本）
        customCategories.put("log", Arrays.asList(   // 原木、去皮木
            "oak_log", "spruce_log", "birch_log", "jungle_log", "acacia_log", "dark_oak_log",
            "mangrove_log", "cherry_log", "bamboo_block", "crimson_stem", "warped_stem",
            "stripped_oak_log", "stripped_spruce_log", "stripped_birch_log", "stripped_jungle_log", "stripped_acacia_log", "stripped_dark_oak_log",
            "stripped_mangrove_log", "stripped_cherry_log", "stripped_bamboo_block", "stripped_crimson_stem", "stripped_warped_stem"));

//        customCategories.put("spruce_log", Arrays.asList("spruce_log", "stripped_spruce_log"));
//        customCategories.put("birch_log", Arrays.asList("birch_log", "stripped_birch_log"));
//        customCategories.put("jungle_log", Arrays.asList("jungle_log", "stripped_jungle_log"));
//        customCategories.put("acacia_log", Arrays.asList("acacia_log", "stripped_acacia_log"));
//        customCategories.put("dark_oak_log", Arrays.asList("dark_oak_log", "stripped_dark_oak_log"));

        // === 颜色变种合并（同种物品不同颜色放一起）===

        // 羊毛（所有颜色）
        customCategories.put("wool", Arrays.asList(
            "white_wool", "orange_wool", "magenta_wool", "light_blue_wool", "yellow_wool", "lime_wool",
            "pink_wool", "gray_wool", "light_gray_wool", "cyan_wool", "purple_wool", "blue_wool",
            "brown_wool", "green_wool", "red_wool", "black_wool"
        ));

        // 混凝土（所有颜色）
        customCategories.put("concrete", Arrays.asList(
            "white_concrete", "orange_concrete", "magenta_concrete", "light_blue_concrete", "yellow_concrete", "lime_concrete",
            "pink_concrete", "gray_concrete", "light_gray_concrete", "cyan_concrete", "purple_concrete", "blue_concrete",
            "brown_concrete", "green_concrete", "red_concrete", "black_concrete"
        ));

        // 陶瓦（所有颜色）
        customCategories.put("terracotta", Arrays.asList(
            "terracotta", "white_terracotta", "orange_terracotta", "magenta_terracotta", "light_blue_terracotta", "yellow_terracotta", "lime_terracotta",
            "pink_terracotta", "gray_terracotta", "light_gray_terracotta", "cyan_terracotta", "purple_terracotta", "blue_terracotta",
            "brown_terracotta", "green_terracotta", "red_terracotta", "black_terracotta"
        ));

        // 玻璃（所有颜色）
        customCategories.put("glass", Arrays.asList(
            // 玻璃
            "glass", "tinted_glass", "glass_pane",
            "white_stained_glass", "orange_stained_glass", "magenta_stained_glass", "light_blue_stained_glass",
            "yellow_stained_glass", "lime_stained_glass", "pink_stained_glass", "gray_stained_glass",
            "light_gray_stained_glass", "cyan_stained_glass", "purple_stained_glass", "blue_stained_glass",
            "brown_stained_glass", "green_stained_glass", "red_stained_glass", "black_stained_glass",
            "white_stained_glass_pane", "orange_stained_glass_pane", "magenta_stained_glass_pane", "light_blue_stained_glass_pane",
            "yellow_stained_glass_pane", "lime_stained_glass_pane", "pink_stained_glass_pane", "gray_stained_glass_pane",
            "light_gray_stained_glass_pane", "cyan_stained_glass_pane", "purple_stained_glass_pane", "blue_stained_glass_pane",
            "brown_stained_glass_pane", "green_stained_glass_pane", "red_stained_glass_pane", "black_stained_glass_pane",
            "tinted_glass", "white_stained_glass", "orange_stained_glass", "magenta_stained_glass", "light_blue_stained_glass",
            "yellow_stained_glass", "lime_stained_glass", "pink_stained_glass", "gray_stained_glass", "light_gray_stained_glass",
            "cyan_stained_glass", "purple_stained_glass", "blue_stained_glass", "brown_stained_glass", "green_stained_glass", "red_stained_glass", "black_stained_glass"
        ));

        // 地毯（所有颜色）
        customCategories.put("carpet", Arrays.asList(
            "white_carpet", "orange_carpet", "magenta_carpet", "light_blue_carpet", "yellow_carpet", "lime_carpet",
            "pink_carpet", "gray_carpet", "light_gray_carpet", "cyan_carpet", "purple_carpet", "blue_carpet",
            "brown_carpet", "green_carpet", "red_carpet", "black_carpet"
        ));

        // 床（所有颜色）
        customCategories.put("bed", Arrays.asList(
            "white_bed", "orange_bed", "magenta_bed", "light_blue_bed", "yellow_bed", "lime_bed",
            "pink_bed", "gray_bed", "light_gray_bed", "cyan_bed", "purple_bed", "blue_bed",
            "brown_bed", "green_bed", "red_bed", "black_bed"
        ));


        // 音乐唱片（生存可获得）
        customCategories.put("music_discs", Arrays.asList(
            "music_disc_13", "music_disc_cat", "music_disc_blocks", "music_disc_chirp",
            "music_disc_far", "music_disc_mall", "music_disc_mellohi", "music_disc_stal", "music_disc_strd", "music_disc_ward",
            "music_disc_11", "music_disc_wait", "music_disc_otherside", "music_disc_5", "music_disc_pigstep"
        ));

        // 陶片（生存可获得）
        customCategories.put("pottery_sherds", Arrays.asList(
            "angler_pottery_sherd", "archer_pottery_sherd", "arms_up_pottery_sherd", "blade_pottery_sherd", "brewer_pottery_sherd",
            "burn_pottery_sherd", "danger_pottery_sherd", "explorer_pottery_sherd", "friend_pottery_sherd",
            "heart_pottery_sherd", "heartbreak_pottery_sherd", "howl_pottery_sherd", "miner_pottery_sherd",
            "mourner_pottery_sherd", "plenty_pottery_sherd", "prize_pottery_sherd", "scrape_pottery_sherd", "sheaf_pottery_sherd",
            "shelter_pottery_sherd", "skull_pottery_sherd", "snort_pottery_sherd"
        ));

        // 超稀有独特物品（生存可获得）
        customCategories.put("ultra_rare", Arrays.asList("nether_star", "beacon", "conduit", "dragon_head"
        ));

        customCategories.put("totem_of_undying", Arrays.asList("totem_of_undying"));
        customCategories.put("elytra", Arrays.asList("elytra"));
        // 附魔书
        customCategories.put("enchanted_books", Arrays.asList("enchanted_book"));

        // 药水
        customCategories.put("potions", Arrays.asList("potion", "splash_potion", "lingering_potion"));

        // 垃圾物品
        customCategories.put("trash", Arrays.asList(
            "rotten_flesh", "spider_eye", "fermented_spider_eye", "bone", "string", "feather",
            "leather", "rabbit_hide", "slime_ball", "phantom_membrane", "stick"
        ));

        customCategories.put("wooden", Arrays.asList(

            // 木板
            "oak_planks", "spruce_planks", "birch_planks", "jungle_planks", "acacia_planks", "dark_oak_planks",
            "mangrove_planks", "cherry_planks", "bamboo_planks", "crimson_planks", "warped_planks",

            // 木材（6面都有树皮）
            "oak_wood", "spruce_wood", "birch_wood", "jungle_wood", "acacia_wood", "dark_oak_wood",
            "mangrove_wood", "cherry_wood", "bamboo_mosaic", "crimson_hyphae", "warped_hyphae",
            "stripped_oak_wood", "stripped_spruce_wood", "stripped_birch_wood", "stripped_jungle_wood", "stripped_acacia_wood", "stripped_dark_oak_wood",
            "stripped_mangrove_wood", "stripped_cherry_wood", "stripped_bamboo_mosaic", "stripped_crimson_hyphae", "stripped_warped_hyphae",

            // 制品
            "oak_slab", "spruce_slab", "birch_slab", "jungle_slab", "acacia_slab", "dark_oak_slab",
            "mangrove_slab", "cherry_slab", "bamboo_slab", "crimson_slab", "warped_slab",
            "oak_stairs", "spruce_stairs", "birch_stairs", "jungle_stairs", "acacia_stairs", "dark_oak_stairs",
            "mangrove_stairs", "cherry_stairs", "bamboo_stairs", "crimson_stairs", "warped_stairs",

            // 栅栏 & 门类
            "oak_fence", "spruce_fence", "birch_fence", "jungle_fence", "acacia_fence", "dark_oak_fence",
            "mangrove_fence", "cherry_fence", "bamboo_fence", "crimson_fence", "warped_fence",
            "oak_fence_gate", "spruce_fence_gate", "birch_fence_gate", "jungle_fence_gate", "acacia_fence_gate", "dark_oak_fence_gate",
            "mangrove_fence_gate", "cherry_fence_gate", "bamboo_fence_gate", "crimson_fence_gate", "warped_fence_gate",

            "oak_door", "spruce_door", "birch_door", "jungle_door", "acacia_door", "dark_oak_door",
            "mangrove_door", "cherry_door", "bamboo_door", "crimson_door", "warped_door",
            "oak_trapdoor", "spruce_trapdoor", "birch_trapdoor", "jungle_trapdoor", "acacia_trapdoor", "dark_oak_trapdoor",
            "mangrove_trapdoor", "cherry_trapdoor", "bamboo_trapdoor", "crimson_trapdoor", "warped_trapdoor",

            // 特殊木制品
            "ladder", "crafting_table", "cartography_table", "fletching_table", "smithing_table", "loom",
            "barrel", "composter", "chest", "trapped_chest", "bookshelf", "chiseled_bookshelf",
            "jukebox", "note_block"
        ));


        customCategories.put("stones", Arrays.asList(
            // 基础石材
            "mossy_cobblestone",
            "granite", "polished_granite",
            "diorite", "polished_diorite",
            "andesite", "polished_andesite",
            "tuff", "polished_tuff",
            "calcite",
            "dripstone_block",

            // 石砖
            "stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks", "chiseled_stone_bricks",

            // 黑石
            "blackstone", "polished_blackstone", "chiseled_polished_blackstone",
            "polished_blackstone_bricks", "cracked_polished_blackstone_bricks",

            // 深板岩
            "deepslate", "cobbled_deepslate", "polished_deepslate",
            "deepslate_bricks", "cracked_deepslate_bricks",
            "deepslate_tiles", "cracked_deepslate_tiles",
            "chiseled_deepslate",

            // 石英
            "quartz_block", "chiseled_quartz_block", "quartz_bricks",
            "smooth_quartz", "quartz_pillar",

            // 沙岩
            "sandstone", "chiseled_sandstone", "cut_sandstone", "smooth_sandstone",
            "red_sandstone", "chiseled_red_sandstone", "cut_red_sandstone", "smooth_red_sandstone",

            // 玄武岩
            "basalt", "polished_basalt",

            // 制品（台阶/楼梯/墙）
            "stone_slab", "cobblestone_slab", "mossy_cobblestone_slab",
            "granite_slab", "polished_granite_slab", "diorite_slab", "polished_diorite_slab",
            "andesite_slab", "polished_andesite_slab", "tuff_slab", "polished_tuff_slab",
            "stone_brick_slab", "mossy_stone_brick_slab", "blackstone_slab", "polished_blackstone_slab",
            "polished_blackstone_brick_slab", "deepslate_brick_slab", "deepslate_tile_slab", "quartz_slab",
            "smooth_quartz_slab", "sandstone_slab", "cut_sandstone_slab", "smooth_sandstone_slab",
            "red_sandstone_slab", "cut_red_sandstone_slab", "smooth_red_sandstone_slab",
            "basalt_slab",

            "stone_stairs", "cobblestone_stairs", "mossy_cobblestone_stairs",
            "granite_stairs", "polished_granite_stairs", "diorite_stairs", "polished_diorite_stairs",
            "andesite_stairs", "polished_andesite_stairs", "tuff_stairs", "polished_tuff_stairs",
            "stone_brick_stairs", "mossy_stone_brick_stairs", "blackstone_stairs", "polished_blackstone_stairs",
            "polished_blackstone_brick_stairs", "deepslate_brick_stairs", "deepslate_tile_stairs", "quartz_stairs",
            "smooth_quartz_stairs", "sandstone_stairs", "smooth_sandstone_stairs",
            "red_sandstone_stairs", "smooth_red_sandstone_stairs",
            "basalt_stairs",

            "cobblestone_wall", "mossy_cobblestone_wall",
            "granite_wall", "diorite_wall", "andesite_wall", "tuff_wall",
            "stone_brick_wall", "mossy_stone_brick_wall",
            "blackstone_wall", "polished_blackstone_wall", "polished_blackstone_brick_wall",
            "deepslate_brick_wall", "deepslate_tile_wall",
            "sandstone_wall", "red_sandstone_wall"
        ));


        customCategories.put("trapdoors", Arrays.asList(
            "iron_trapdoor",
            "oak_trapdoor",
            "spruce_trapdoor",
            "birch_trapdoor",
            "jungle_trapdoor",
            "acacia_trapdoor",
            "dark_oak_trapdoor",
            "mangrove_trapdoor",
            "cherry_trapdoor",
            "bamboo_trapdoor",
            "crimson_trapdoor",
            "warped_trapdoor"
        ));

        customCategories.put("doors", Arrays.asList(
            "iron_door",
            "oak_door",
            "spruce_door",
            "birch_door",
            "jungle_door",
            "acacia_door",
            "dark_oak_door",
            "mangrove_door",
            "cherry_door",
            "bamboo_door",
            "crimson_door",
            "warped_door"
        ));

        customCategories.put("fence_gates", Arrays.asList(
            "oak_fence_gate",
            "spruce_fence_gate",
            "birch_fence_gate",
            "jungle_fence_gate",
            "acacia_fence_gate",
            "dark_oak_fence_gate",
            "mangrove_fence_gate",
            "cherry_fence_gate",
            "bamboo_fence_gate",
            "crimson_fence_gate",
            "warped_fence_gate"
        ));

        customCategories.put("rails", Arrays.asList(
            "rail",
            "powered_rail",
            "detector_rail",
            "activator_rail"
        ));


        customCategories.put("redstone", Arrays.asList(
            // 基础红石
            "redstone",
            "redstone_block",
            "redstone_wire",
            "redstone_torch",
            "redstone_wall_torch",
            "redstone_lamp",

            // 信号逻辑
            "repeater",
            "comparator",
            "observer",

            // 动力装置
            "piston",
            "sticky_piston",
            "moving_piston",
            "piston_head",

            // 交互容器
            "dropper",
            "dispenser",
            "hopper",
            "lectern",
            "trapped_chest",
            "target",

            // 触发器
            "stone_pressure_plate",
            "oak_pressure_plate",
            "spruce_pressure_plate",
            "birch_pressure_plate",
            "jungle_pressure_plate",
            "acacia_pressure_plate",
            "dark_oak_pressure_plate",
            "mangrove_pressure_plate",
            "cherry_pressure_plate",
            "bamboo_pressure_plate",
            "crimson_pressure_plate",
            "warped_pressure_plate",
            "polished_blackstone_pressure_plate",
            "light_weighted_pressure_plate",
            "heavy_weighted_pressure_plate",

            "stone_button",
            "oak_button",
            "spruce_button",
            "birch_button",
            "jungle_button",
            "acacia_button",
            "dark_oak_button",
            "mangrove_button",
            "cherry_button",
            "bamboo_button",
            "crimson_button",
            "warped_button",
            "polished_blackstone_button",

            "tripwire",
            "tripwire_hook",
            "lever",
            "daylight_detector",

            // 其他
            "note_block",
            "tnt",

            // 命令与特殊方块
            "command_block",
            "chain_command_block",
            "repeating_command_block",
            "structure_block",
            "jigsaw",
            "light",

            // sculk 系列
            "sculk_sensor",
            "calibrated_sculk_sensor",
            "sculk_shrieker"
        ));

        customCategories.put("equipment", Arrays.asList(
            // 木质工具
            "wooden_sword", "wooden_pickaxe", "wooden_axe", "wooden_shovel", "wooden_hoe",

            // 石质工具
            "stone_sword", "stone_pickaxe", "stone_axe", "stone_shovel", "stone_hoe",

            // 铁质工具
            "iron_sword", "iron_pickaxe", "iron_axe", "iron_shovel", "iron_hoe",

            // 金质工具
            "golden_sword", "golden_pickaxe", "golden_axe", "golden_shovel", "golden_hoe",

            // 钻石工具
            "diamond_sword", "diamond_pickaxe", "diamond_axe", "diamond_shovel", "diamond_hoe",

            // 下界合金工具
            "netherite_sword", "netherite_pickaxe", "netherite_axe", "netherite_shovel", "netherite_hoe",

            // 远程武器
            "bow", "crossbow", "trident",

            // 盔甲 - 皮革
            "leather_helmet", "leather_chestplate", "leather_leggings", "leather_boots",

            // 盔甲 - 铁
            "iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots",

            // 盔甲 - 金
            "golden_helmet", "golden_chestplate", "golden_leggings", "golden_boots",

            // 盔甲 - 钻石
            "diamond_helmet", "diamond_chestplate", "diamond_leggings", "diamond_boots",

            // 盔甲 - 下界合金
            "netherite_helmet", "netherite_chestplate", "netherite_leggings", "netherite_boots",

            // 马铠
            "iron_horse_armor", "golden_horse_armor", "diamond_horse_armor", "leather_horse_armor",

            // 其他装备
            "shield", "fishing_rod", "carrot_on_a_stick", "warped_fungus_on_a_stick", "flint_and_steel"
        ));


//        customCategories.put("building", Arrays.asList(
//                // 基础自然方块
//                "dirt", "coarse_dirt", "podzol", "rooted_dirt", "grass_block",
//                "mycelium", "farmland", "mud", "packed_mud", "mud_bricks",
//
//                // 沙类
//                "sand", "red_sand", "gravel", "suspicious_sand", "suspicious_gravel",
//
//                // 黏土 & 陶瓦
//                "clay", "terracotta", "white_terracotta", "orange_terracotta", "magenta_terracotta",
//                "light_blue_terracotta", "yellow_terracotta", "lime_terracotta", "pink_terracotta",
//                "gray_terracotta", "light_gray_terracotta", "cyan_terracotta", "purple_terracotta",
//                "blue_terracotta", "brown_terracotta", "green_terracotta", "red_terracotta", "black_terracotta",
//                "glazed_terracotta", "white_glazed_terracotta", "orange_glazed_terracotta", "magenta_glazed_terracotta",
//                "light_blue_glazed_terracotta", "yellow_glazed_terracotta", "lime_glazed_terracotta", "pink_glazed_terracotta",
//                "gray_glazed_terracotta", "light_gray_glazed_terracotta", "cyan_glazed_terracotta", "purple_glazed_terracotta",
//                "blue_glazed_terracotta", "brown_glazed_terracotta", "green_glazed_terracotta", "red_glazed_terracotta",
//                "black_glazed_terracotta",
//
//                // 混凝土 & 粉末
//                "white_concrete", "orange_concrete", "magenta_concrete", "light_blue_concrete",
//                "yellow_concrete", "lime_concrete", "pink_concrete", "gray_concrete",
//                "light_gray_concrete", "cyan_concrete", "purple_concrete", "blue_concrete",
//                "brown_concrete", "green_concrete", "red_concrete", "black_concrete",
//
//                "white_concrete_powder", "orange_concrete_powder", "magenta_concrete_powder", "light_blue_concrete_powder",
//                "yellow_concrete_powder", "lime_concrete_powder", "pink_concrete_powder", "gray_concrete_powder",
//                "light_gray_concrete_powder", "cyan_concrete_powder", "purple_concrete_powder", "blue_concrete_powder",
//                "brown_concrete_powder", "green_concrete_powder", "red_concrete_powder", "black_concrete_powder",
//
//
//                // 羊毛 & 地毯
//                "white_wool", "orange_wool", "magenta_wool", "light_blue_wool", "yellow_wool",
//                "lime_wool", "pink_wool", "gray_wool", "light_gray_wool", "cyan_wool", "purple_wool",
//                "blue_wool", "brown_wool", "green_wool", "red_wool", "black_wool",
//
//                "white_carpet", "orange_carpet", "magenta_carpet", "light_blue_carpet", "yellow_carpet",
//                "lime_carpet", "pink_carpet", "gray_carpet", "light_gray_carpet", "cyan_carpet", "purple_carpet",
//                "blue_carpet", "brown_carpet", "green_carpet", "red_carpet", "black_carpet",
//
//                // 冰类
//                "ice", "packed_ice", "blue_ice", "frosted_ice", "snow_block",
//
//                // 植被类（可装饰）
//                "hay_block", "moss_block", "moss_carpet",
//                "leaves", "oak_leaves", "spruce_leaves", "birch_leaves", "jungle_leaves",
//                "acacia_leaves", "dark_oak_leaves", "mangrove_leaves", "cherry_leaves", "azalea_leaves", "flowering_azalea_leaves",
//
//                // 金属 & 工业
//                "iron_block", "gold_block", "copper_block", "exposed_copper", "weathered_copper", "oxidized_copper",
//                "waxed_copper_block", "waxed_exposed_copper", "waxed_weathered_copper", "waxed_oxidized_copper",
//                "cut_copper", "exposed_cut_copper", "weathered_cut_copper", "oxidized_cut_copper",
//                "waxed_cut_copper", "waxed_exposed_cut_copper", "waxed_weathered_cut_copper", "waxed_oxidized_cut_copper",
//                "cut_copper_slab", "exposed_cut_copper_slab", "weathered_cut_copper_slab", "oxidized_cut_copper_slab",
//                "waxed_cut_copper_slab", "waxed_exposed_cut_copper_slab", "waxed_weathered_cut_copper_slab", "waxed_oxidized_cut_copper_slab",
//                "cut_copper_stairs", "exposed_cut_copper_stairs", "weathered_cut_copper_stairs", "oxidized_cut_copper_stairs",
//                "waxed_cut_copper_stairs", "waxed_exposed_cut_copper_stairs", "waxed_weathered_cut_copper_stairs", "waxed_oxidized_cut_copper_stairs",
//
//                // 其他常见建材
//                "obsidian", "crying_obsidian", "end_stone", "end_stone_bricks",
//                "purpur_block", "purpur_pillar", "purpur_slab", "purpur_stairs"
//        ));

    }


}
