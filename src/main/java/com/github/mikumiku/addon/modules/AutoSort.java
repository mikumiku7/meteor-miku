package com.github.mikumiku.addon.modules;


import com.github.mikumiku.addon.BaseModule;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class AutoSort extends BaseModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgChests = settings.createGroup("箱子设置");
    private final SettingGroup sgClassification = settings.createGroup("分类设置");
    private final SettingGroup sgAdvanced = settings.createGroup("高级设置");

    // ============ 基本设置 ============

    // 处理间隔时间（tick）
    private final Setting<Integer> processingInterval = sgGeneral.add(new IntSetting.Builder()
        .name("处理间隔")
        .description("每次处理的间隔时间（tick），60tick = 3秒")
        .defaultValue(60)
        .min(1)
        .max(600)
        .sliderMin(20)
        .sliderMax(200)
        .build()
    );

    // 是否启用模块
    private final Setting<Boolean> autoEnable = sgGeneral.add(new BoolSetting.Builder()
        .name("自动启用")
        .description("进入世界时自动启用模块")
        .defaultValue(false)
        .build()
    );

    // ============ 箱子设置 ============

    // 动态箱子搜索半径
    private final Setting<Integer> searchRadius = sgChests.add(new IntSetting.Builder()
        .name("搜索半径")
        .description("动态搜索箱子的半径范围（方块）")
        .defaultValue(20)
        .min(1)
        .max(100)
        .sliderMin(5)
        .sliderMax(50)
        .build()
    );

    // 中心坐标 X
    private final Setting<Integer> centerX = sgChests.add(new IntSetting.Builder()
        .name("中心X坐标")
        .description("箱子搜索的中心X坐标（0为玩家当前位置）")
        .defaultValue(0)
        .min(-30000000)
        .max(30000000)
        .sliderMin(-100)
        .sliderMax(100)
        .build()
    );

    // 中心坐标 Y
    private final Setting<Integer> centerY = sgChests.add(new IntSetting.Builder()
        .name("中心Y坐标")
        .description("箱子搜索的中心Y坐标（0为玩家当前位置）")
        .defaultValue(0)
        .min(-64)
        .max(320)
        .sliderMin(-64)
        .sliderMax(320)
        .build()
    );

    // 中心坐标 Z
    private final Setting<Integer> centerZ = sgChests.add(new IntSetting.Builder()
        .name("中心Z坐标")
        .description("箱子搜索的中心Z坐标（0为玩家当前位置）")
        .defaultValue(0)
        .min(-30000000)
        .max(30000000)
        .sliderMin(-100)
        .sliderMax(100)
        .build()
    );

    // 使用玩家位置作为中心
    private final Setting<Boolean> usePlayerPos = sgChests.add(new BoolSetting.Builder()
        .name("使用玩家位置")
        .description("使用玩家当前位置作为搜索中心")
        .defaultValue(true)
        .build()
    );

    // 箱子缓存更新间隔
    private final Setting<Integer> cacheUpdateInterval = sgChests.add(new IntSetting.Builder()
        .name("缓存更新间隔")
        .description("箱子缓存更新的间隔时间（tick）")
        .defaultValue(600)
        .min(100)
        .max(6000)
        .sliderMin(200)
        .sliderMax(2000)
        .build()
    );

    // ============ 分类设置 ============

    // 启用智能分类
    private final Setting<Boolean> smartClassification = sgClassification.add(new BoolSetting.Builder()
        .name("智能分类")
        .description("基于物品属性进行智能分类")
        .defaultValue(true)
        .build()
    );

    // 分类模式
    private final Setting<ClassificationMode> classificationMode = sgClassification.add(new EnumSetting.Builder<ClassificationMode>()
        .name("分类模式")
        .description("选择物品分类的模式")
        .defaultValue(ClassificationMode.ByCategory)
        .build()
    );

    // 处理创造模式物品
    private final Setting<Boolean> processCreative = sgClassification.add(new BoolSetting.Builder()
        .name("处理创造物品")
        .description("是否处理创造模式的物品")
        .defaultValue(false)
        .build()
    );

    // 忽略NBT数据
    private final Setting<Boolean> ignoreNBT = sgClassification.add(new BoolSetting.Builder()
        .name("忽略NBT")
        .description("分类时忽略物品的NBT数据")
        .defaultValue(false)
        .build()
    );

    // ============ 高级设置 ============

    // 最大处理物品数量
    private final Setting<Integer> maxItemsPerTick = sgAdvanced.add(new IntSetting.Builder()
        .name("单次处理数量")
        .description("每个tick最多处理的物品数量")
        .defaultValue(8)
        .min(1)
        .max(64)
        .sliderMin(1)
        .sliderMax(32)
        .build()
    );

    // 调试模式
    private final Setting<Boolean> debugMode = sgAdvanced.add(new BoolSetting.Builder()
        .name("调试模式")
        .description("显示详细的调试信息")
        .defaultValue(false)
        .build()
    );

    // 分类优先级
    private final Setting<SortPriority> sortPriority = sgAdvanced.add(new EnumSetting.Builder<SortPriority>()
        .name("分类优先级")
        .description("物品分类的优先级策略")
        .defaultValue(SortPriority.NearestFirst)
        .build()
    );

    // ============ 数据存储 ============

    // 箱子位置列表
    private final List<BlockPos> chestLocations = new ArrayList<>();

    // 箱子缓存
    private final Map<BlockPos, ChestInfo> chestCache = new LinkedHashMap<>();

    // 自定义分类规则
    private final Map<String, List<String>> customCategories = new HashMap<>();

    // 计时器
    private int tickCounter = 0;
    private int cacheUpdateCounter = 0;

    // ============ 枚举类型 ============

    public enum ClassificationMode {
        ByCategory("按类别"),
        ByMaterial("按材质"),
        ByRarity("按稀有度"),
        Custom("自定义");

        private final String title;

        ClassificationMode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public enum SortPriority {
        NearestFirst("最近优先"),
        FullestFirst("最满优先"),
        EmptyFirst("最空优先"),
        Sequential("顺序处理");

        private final String title;

        SortPriority(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    // ============ 内部类 ============

    public static class ChestInfo {
        public BlockPos pos;
        public String customName;
        public List<String> itemCategories;
        public int usedSlots;
        public int totalSlots;
        public long lastUpdated;

        public ChestInfo(BlockPos pos) {
            this.pos = pos;
            this.itemCategories = new ArrayList<>();
            this.usedSlots = 0;
            this.totalSlots = 27;
            this.lastUpdated = System.currentTimeMillis();
        }

        public boolean isFull() {
            return usedSlots >= totalSlots;
        }

        public float getFillPercentage() {
            return (float) usedSlots / totalSlots;
        }
    }

    // ============ 构造函数 ============

    public AutoSort() {
        super(BaseModule.CATEGORY_MIKU_PRO, "全物品分类BOT", "自动将物品分类到箱子中");
        initializeDefaultCategories();
    }

    // ============ Getter 方法 ============

    public int getProcessingInterval() {
        return processingInterval.get();
    }

    public int getSearchRadius() {
        return searchRadius.get();
    }

    public BlockPos getCenterPos() {
        if (usePlayerPos.get() && mc.player != null) {
            return mc.player.getBlockPos();
        }
        return new BlockPos(centerX.get(), centerY.get(), centerZ.get());
    }

    public boolean isSmartClassificationEnabled() {
        return smartClassification.get();
    }

    public boolean shouldProcessCreative() {
        return processCreative.get();
    }

    public int getCacheUpdateInterval() {
        return cacheUpdateInterval.get();
    }

    public boolean isDebugMode() {
        return debugMode.get();
    }

    public ClassificationMode getClassificationMode() {
        return classificationMode.get();
    }

    public Map<String, List<String>> getCustomCategories() {
        return customCategories;
    }

    public Map<BlockPos, ChestInfo> getChestCache() {
        return chestCache;
    }

    // ============ 初始化默认分类 ============

    private void initializeDefaultCategories() {
        customCategories.put("工具", Arrays.asList(
            "minecraft:wooden_pickaxe", "minecraft:stone_pickaxe",
            "minecraft:iron_pickaxe", "minecraft:diamond_pickaxe",
            "minecraft:netherite_pickaxe", "minecraft:golden_pickaxe"
        ));

        customCategories.put("武器", Arrays.asList(
            "minecraft:wooden_sword", "minecraft:stone_sword",
            "minecraft:iron_sword", "minecraft:diamond_sword",
            "minecraft:netherite_sword", "minecraft:bow", "minecraft:crossbow"
        ));

        customCategories.put("方块", Arrays.asList(
            "minecraft:stone", "minecraft:dirt", "minecraft:cobblestone",
            "minecraft:oak_planks", "minecraft:glass"
        ));
    }

    // ============ 调试输出 ============

    private void debug(String message) {
        if (debugMode.get()) {
            ChatUtils.info("(AutoSort) " + message);
        }
    }
}
