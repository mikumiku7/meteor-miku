package com.github.mikumiku.addon.modules.sorter;


import baritone.api.BaritoneAPI;
import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.ItemListMapSetting;
import com.github.mikumiku.addon.util.BaritoneUtil;
import com.github.mikumiku.addon.util.MikuUtil;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ItemSorterModule extends BaseModule {


    public ItemSorterModule() {
        super(BaseModule.CATEGORY_MIKU_PRO, "BOT全物品分类", "自动整理和分类箱子中的物品。类似铜傀儡");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgChests = settings.createGroup("箱子配置");
    private final SettingGroup sgClassification = settings.createGroup("分类设置");
    private final SettingGroup sgTiming = settings.createGroup("时间控制");

    // ===== 通用设置 =====
    private final Setting<String> guidestr = sgGeneral.add(new StringSetting.Builder()
        .name("启用引导模式")
        .description("启用引导模式，通过交互箱子来自动配置源箱子列表。")
        .defaultValue("类似铜傀儡。需要准备一个或者多个杂物箱，一个大型箱子仓库，箱子里留一个物品或者留空。 BOT从杂物箱取物，然后自动搜索周围的箱子，自动放到合适的箱子里或者空箱子。")
        .build()
    );
    // ===== 通用设置 =====
    private final Setting<Boolean> enableGuide = sgGeneral.add(new BoolSetting.Builder()
        .name("启用引导模式")
        .description("启用引导模式，通过交互箱子来自动配置源箱子列表。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> chestSearchRadius = sgGeneral.add(new IntSetting.Builder()
        .name("箱子搜索半径")
        .description("搜索附近箱子的半径范围（方块）。")
        .defaultValue(32)
        .min(8)
        .max(200)
        .sliderMin(8)
        .sliderMax(200)
        .build()
    );

    private final Setting<BlockPos> centerPosition = sgGeneral.add(new BlockPosSetting.Builder()
        .name("中心坐标")
        .description("整理系统的中心坐标，用于返回和导航。")
        .defaultValue(new BlockPos(0, 0, 0))
        .build()
    );

    // ===== 箱子配置 =====
    private final Setting<List<String>> sourceChests = sgChests.add(new StringListSetting.Builder()
        .name("源箱子列表")
        .description("从这些箱子中取出物品进行分类。支持添加多个箱子。")
        .defaultValue(new ArrayList<>())
        .build()
    );

    private final Setting<Integer> cacheUpdateInterval = sgChests.add(new IntSetting.Builder()
        .name("缓存更新间隔")
        .description("箱子缓存更新的时间间隔（秒）。")
        .defaultValue(600)
        .min(5)
        .sliderMin(5)
        .sliderMax(6000)
        .build()
    );

    // ===== 分类设置 =====


    private final Setting<Boolean> enableSmartClassification = sgClassification.add(new BoolSetting.Builder()
        .name("智能分类")
        .description("启用基于物品属性的智能分类。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> groupColorVariants = sgClassification.add(new BoolSetting.Builder()
        .name("合并颜色变种")
        .description("将同类物品的不同颜色版本分类到一起（如羊毛、混凝土等）。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> separateEnchanted = sgClassification.add(new BoolSetting.Builder()
        .name("分离附魔物品")
        .description("将附魔物品单独分类存储。")
        .defaultValue(false)
        .build()
    );

    private final Setting<Map<String, List<Item>>> customCategories = sgClassification.add(new ItemListMapSetting.Builder()
        .name("自定义分类")
        .description("优先使用这里的分类，没找到则自成一类。")
        .defaultValue(ItemCatePreset.customCategoriesMap)
        .build()
    );

    private final Setting<String> menCache = sgClassification.add(new StringSetting.Builder()
        .name("记忆缓存")
        .description("记忆缓存")
        .defaultValue("{}")
        .visible(() -> false)
        .build()
    );

    // ===== 时间控制 =====
    private final Setting<Integer> processingInterval = sgTiming.add(new IntSetting.Builder()
        .name("处理间隔")
        .description("每轮物品整理之间的时间间隔（tick，20tick = 1秒）。")
        .defaultValue(600)
        .min(100)
        .max(6000)
        .sliderMin(100)
        .sliderMax(6000)
        .build()
    );

    private final Setting<Integer> chestDelay = sgTiming.add(new IntSetting.Builder()
        .name("箱子间延迟")
        .description("处理箱子之间的延迟时间（毫秒）。")
        .defaultValue(100)
        .min(50)
        .max(2000)
        .sliderMin(50)
        .sliderMax(2000)
        .build()
    );

    private final Setting<Integer> categoryDelay = sgTiming.add(new IntSetting.Builder()
        .name("分类间延迟")
        .description("存储不同分类物品之间的延迟时间（毫秒）。")
        .defaultValue(500)
        .min(100)
        .max(3000)
        .sliderMin(100)
        .sliderMax(3000)
        .build()
    );

    private final Setting<Integer> roundDelay = sgTiming.add(new IntSetting.Builder()
        .name("轮次间延迟")
        .description("完成一轮整理后，开始下一轮前的等待时间（毫秒）。")
        .defaultValue(2000)
        .min(100)
        .max(60000)
        .sliderMin(100)
        .sliderMax(60000)
        .build()
    );

    private final Setting<Integer> stateTimeout = sgTiming.add(new IntSetting.Builder()
        .name("状态超时")
        .description("单个状态的最大执行时间（秒），超时后自动重置。")
        .defaultValue(120)
        .min(30)
        .max(300)
        .sliderMin(30)
        .sliderMax(300)
        .build()
    );

    // ===== 状态管理 =====
    private ProcessingState currentState = ProcessingState.IDLE;
    private List<ChestLocation> chestsToProcess = new ArrayList<>();

    private int currentChestIndex = 0;
    private long stateStartTime = 0;
    private long lastProcessTime = 0;
    private long nextChestTime = 0;
    private long nextRoundTime = 0;
    private BlockPos currentOpenChest = null;

    // 物品分类缓存
    private Map<String, List<ItemStack>> categorizedItems = new ConcurrentHashMap<>();
    private List<String> categoriesToProcess = new ArrayList<>();
    private int currentCategoryIndex = 0;

    // 辅助管理器
    private ItemClassifier classifier;
    private ChestCacheManager chestCache;

    String lastLog = "";

    // 处理状态枚举
    private enum ProcessingState {
        GUIDE("引导设置"),
        IDLE("空闲"),
        RESETTING("重置中"),
        MOVING_TO_SOURCE_CHEST("移动到源箱子"),
        OPENING_SOURCE_CHEST("打开源箱子"),
        WITHDRAWING_FROM_CHEST("提取物品"),
        CLOSING_SOURCE_CHEST("关闭源箱子"),
        WAITING_NEXT_CHEST("等待下一个箱子"),
        CATEGORIZING_ITEMS("物品分类中"),
        MOVING_TO_TARGET_CONTAINER("移动到目标箱子"),
        OPENING_TARGET_CONTAINER("打开目标箱子"),
        DEPOSITING_ITEMS("存储物品中"),
        CLOSING_TARGET_CONTAINER("关闭目标箱子"),
        WAITING_NEXT_CATEGORY("等待下一个分类"),
        PROCESSING_COMPLETE("处理完成"),
        WAITING_NEXT_ROUND("等待下一轮");

        private final String displayName;

        ProcessingState(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }


    @Override
    public void onActivate() {
        classifier = new ItemClassifier(customCategories.get(), enableSmartClassification.get());
        chestCache = new ChestCacheManager(getSources(), chestSearchRadius.get(), classifier);

        info("物品整理模块已启动");
        resetState();
    }

    private List<ChestLocation> getSources() {
        List<String> locations = sourceChests.get();
        List<ChestLocation> chestLocations = locations.stream()
            .map(location -> ChestLocation.fromJson(location))
            .filter(location -> location != null)
            .toList();

        return chestLocations;
    }

    @Override
    public void onDeactivate() {
        info("物品整理模块已停用");
        resetState();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        // 检查玩家是否在有效范围内
        if (!isPlayerInRange()) {
            return;
        }

        // 更新箱子缓存
        chestCache.updateCache();

        // 检查状态超时
        if (currentState != ProcessingState.GUIDE &&
            currentState != ProcessingState.IDLE &&
            System.currentTimeMillis() - stateStartTime > stateTimeout.get() * 1000L) {
            error("状态 " + currentState + " 超时，正在重置...");
            resetState();
            return;
        }

        // 状态机处理
        switch (currentState) {
            case GUIDE -> handleGuideState();
            case IDLE -> handleIdleState();
            case RESETTING -> handleResettingState();
            case MOVING_TO_SOURCE_CHEST -> handleMovingToSourceChest();
            case OPENING_SOURCE_CHEST -> handleOpeningSourceChest();
            case WITHDRAWING_FROM_CHEST -> handleWithdrawingFromChest();
            case CLOSING_SOURCE_CHEST -> handleClosingSourceChest();
            case WAITING_NEXT_CHEST -> handleWaitingNextChest();
            case CATEGORIZING_ITEMS -> handleCategorizingItems();
            case MOVING_TO_TARGET_CONTAINER -> handleMovingToTargetContainer();
            case OPENING_TARGET_CONTAINER -> handleOpeningTargetContainer();
            case DEPOSITING_ITEMS -> handleDepositingItems();
            case CLOSING_TARGET_CONTAINER -> handleClosingTargetContainer();
            case WAITING_NEXT_CATEGORY -> handleWaitingNextCategory();
            case PROCESSING_COMPLETE -> handleProcessingComplete();
            case WAITING_NEXT_ROUND -> handleWaitingNextRound();
        }
    }


    @EventHandler
    private void onInteractBlock(InteractBlockEvent event) {
        // 只在引导阶段处理交互
        if (currentState != ProcessingState.GUIDE) return;

        BlockPos pos = event.result.getBlockPos();
        Block block = mc.world.getBlockState(pos).getBlock();

        // 检查是否是存储方块（箱子、潜影盒等）
        if (isStorageBlock(block)) {
            handleStorageBlockInteraction(pos, block);
            // 取消交互事件，防止打开箱子
            event.cancel();
        }
    }

    /**
     * 检查方块是否是存储方块
     */
    private boolean isStorageBlock(Block block) {
        return block instanceof ChestBlock ||
            block instanceof ShulkerBoxBlock ||
            block instanceof BarrelBlock ||
            block instanceof EnderChestBlock ||
            block instanceof TrappedChestBlock;
    }

    /**
     * 处理存储方块交互
     */
    private void handleStorageBlockInteraction(BlockPos pos, Block block) {
        String posString = ChestLocation.fromPos(pos).toJson();
        List<String> currentSourceChests = new ArrayList<>(sourceChests.get());

        // 检查是否已经存在该坐标
        if (currentSourceChests.contains(posString)) {
            info("箱子 " + pos + " 已在源箱子列表中");
            return;
        }

        // 添加到源箱子列表
        currentSourceChests.add(posString);
        sourceChests.set(currentSourceChests);

        String blockName = block.getName().getString();
        info("已添加 " + blockName + " 到源箱子列表：" + posString);
        info("当前源箱子数量：" + currentSourceChests.size());

        // 检查是否应该关闭引导模式
        if (currentSourceChests.size() >= 1) {
            info("已配置源箱子，可以在设置中关闭引导模式开始整理");
            info("或继续添加更多源箱子");
        }
    }

    // ===== 状态处理方法 =====

    private void handleGuideState() {
        // 检查引导设置是否被关闭
        if (!enableGuide.get()) {
            info("引导模式已关闭，进入状态");

            // 检查是否已有源箱子配置
            List<String> currentSourceChests = sourceChests.get();
            if (currentSourceChests != null && !currentSourceChests.isEmpty()) {
                info("已配置 " + currentSourceChests.size() + " 个源箱子");
                info("如需添加更多箱子，请继续交互；如需开始整理，请在设置中关闭引导模式");
            } else {
                info("请右键点击箱子来添加到源箱子列表");
            }

            setState(ProcessingState.IDLE);
            return;
        }


    }

    private void handleIdleState() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastProcessTime >= processingInterval.get() * 50L) {
            lastProcessTime = currentTime;
            info("开始新的物品整理周期");
            startItemSorting();
        }
    }

    private void handleResettingState() {
        // 重置完成后返回空闲状态
        setState(ProcessingState.IDLE);
    }


    private void handleMovingToSourceChest() {
        if (currentChestIndex >= chestsToProcess.size()) {
            setState(ProcessingState.CATEGORIZING_ITEMS);
            return;
        }

        BlockPos targetChest = chestsToProcess.get(currentChestIndex).toBlockPos();
        info("正在移动到源箱子：" + targetChest.toShortString());

        // 尝试寻路到目标箱子
        if (MikuUtil.pathTo(targetChest, 3.0)) {
            info("已到达源箱子位置");
            openChest(chestsToProcess.get(currentChestIndex).toBlockPos());

            setState(ProcessingState.OPENING_SOURCE_CHEST);
        }
    }

    private void handleOpeningSourceChest() {
        if (mc.player == null || mc.player.currentScreenHandler == null) return;

        // 检查是否成功打开容器
        if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler) {
            info("源箱子已打开，容器大小：" + handler.getInventory().size());

            // 提取所有物品到玩家库存
            withdrawAllItems(handler);

            setState(ProcessingState.WITHDRAWING_FROM_CHEST);
        }
    }

    private void handleWithdrawingFromChest() {

        // 检查是否仍然有打开的容器
        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) {
            info("容器已关闭，继续处理下一个箱子");
            setState(ProcessingState.CLOSING_SOURCE_CHEST);
            return;
        }

        // 检查容器是否为空
        boolean containerEmpty = true;
        int totalItems = 0;

        for (int slot = 0; slot < handler.getInventory().size(); slot++) {
            ItemStack stack = handler.getInventory().getStack(slot);
            if (!stack.isEmpty()) {
                containerEmpty = false;
                totalItems += stack.getCount();
            }
        }

        if (containerEmpty) {
            info("容器已空，共提取了之前的物品");
            setState(ProcessingState.CLOSING_SOURCE_CHEST);
            return;
        }

        // 继续提取剩余物品
        int itemsExtracted = 0;
        for (int slot = 0; slot < handler.getInventory().size(); slot++) {
            ItemStack stack = handler.getInventory().getStack(slot);

            if (!stack.isEmpty()) {
                // 检查玩家库存是否有空间
                if (mc.player.getInventory().getEmptySlot() == -1) {
                    info("玩家库存已满，无法继续提取物品");
                    setState(ProcessingState.CLOSING_SOURCE_CHEST);
                    return;
                }

                // 使用快速移动提取物品
                InvUtils.shiftClick().slotId(slot);
                itemsExtracted += stack.getCount();
            }
        }

        if (itemsExtracted > 0) {
            info("本次提取了 " + itemsExtracted + " 个物品");
        }

        // 检查是否所有物品都已提取完成
        boolean allItemsExtracted = true;
        for (int slot = 0; slot < handler.getInventory().size(); slot++) {
            if (!handler.getInventory().getStack(slot).isEmpty()) {
                allItemsExtracted = false;
                break;
            }
        }

        if (allItemsExtracted) {
            info("所有物品提取完成");
            setState(ProcessingState.CLOSING_SOURCE_CHEST);
        } else {
            // 还有物品未提取，继续等待
            if (System.currentTimeMillis() - stateStartTime > 10000) { // 10秒超时
                error("物品提取超时，可能有物品无法提取");
                setState(ProcessingState.CLOSING_SOURCE_CHEST);
            }
        }
    }

    private void handleClosingSourceChest() {
        // 确保容器已关闭
        if (mc.player == null || mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
            info("源箱子已关闭");
            moveToNextChest();
        } else {
            // 尝试关闭容器
            mc.player.closeHandledScreen();
            stateStartTime = System.currentTimeMillis();
            info("正在关闭源箱子");
        }
    }

    private void handleWaitingNextChest() {
        if (System.currentTimeMillis() >= nextChestTime) {
            processNextChest();
        }
    }

    private void handleCategorizingItems() {
        info("开始分类玩家库存中的物品");

        categorizedItems.clear();
        categoriesToProcess.clear();
        currentCategoryIndex = 0;

        if (mc.player == null) {
            setState(ProcessingState.PROCESSING_COMPLETE);
            return;
        }

        // 分类主库存中的物品（不包括快捷栏）
        for (int slot = 9; slot < 36; slot++) {
            ItemStack item = mc.player.getInventory().getStack(slot);

            if (!item.isEmpty()) {
                String category = classifier.classifyItem(item);
                categorizedItems.computeIfAbsent(category, k -> new ArrayList<>()).add(item);

                info("物品 " + item.getName().getString() + " 分类为：" + category);
            }
        }

        if (categorizedItems.isEmpty()) {
            info("没有物品需要分类");
            setState(ProcessingState.PROCESSING_COMPLETE);
            return;
        }

        categoriesToProcess.addAll(categorizedItems.keySet());
        info("共分类出 " + categoriesToProcess.size() + " 个类别：" + String.join(", ", categoriesToProcess));

        processNextCategory();
    }

    private void handleMovingToTargetContainer() {
        if (currentCategoryIndex >= categoriesToProcess.size()) {
            setState(ProcessingState.PROCESSING_COMPLETE);
            return;
        }

        String category = categoriesToProcess.get(currentCategoryIndex);
        ChestLocation targetChest = chestCache.findChestForItem(category);

        if (targetChest == null) {
            info("分类 " + category + " 找不到合适的存储箱子，跳过");
            moveToNextCategory();
            return;
        }

        info("正在移动到目标箱子：" + targetChest + " 用于分类：" + category);
        BlockPos blockPos = new BlockPos(targetChest.x, targetChest.y, targetChest.z);

        // 尝试寻路到目标箱子
        if (MikuUtil.pathTo(blockPos, 3.0)) {
            info("已到达目标箱子位置");
            currentOpenChest = blockPos;
            openChest(targetChest.toBlockPos());

            setState(ProcessingState.OPENING_TARGET_CONTAINER);
        }
    }

    private void handleOpeningTargetContainer() {
        if (mc.player == null || mc.player.currentScreenHandler == null) return;

        if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler) {
            String category = categoriesToProcess.get(currentCategoryIndex);
            info("目标容器已打开，容器大小：" + handler.getInventory().size() + "，开始存储分类：" + category);

            // 检查箱子是否适合存储当前分类
            if (currentOpenChest != null) {
                boolean shouldSkip = chestCache.onChestOpened(currentOpenChest.getX(), currentOpenChest.getY(), currentOpenChest.getZ(), handler, category);
                if (shouldSkip) {
                    info("箱子不适合存储当前分类，跳过");
                    mc.player.closeHandledScreen();
                    setState(ProcessingState.CLOSING_TARGET_CONTAINER);
                    return;
                }
            }

            // 存储当前分类的物品
            depositItemsByCategory(handler, category);

            setState(ProcessingState.DEPOSITING_ITEMS);
        }
    }

    private void handleDepositingItems() {
        // 等待物品存储完成
        if (isInventoryOperationComplete()) {
            String category = categoriesToProcess.get(currentCategoryIndex);
            info("分类 " + category + " 的物品存储完成");

            // 关闭容器
            if (mc.player != null) {
                mc.player.closeHandledScreen();
            }

            setState(ProcessingState.CLOSING_TARGET_CONTAINER);
        }
    }

    private void handleClosingTargetContainer() {
        // 确保容器已关闭
        if (mc.player == null || mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
            info("目标箱子已关闭");
            moveToNextCategory();
        }
    }

    private void handleWaitingNextCategory() {
        if (System.currentTimeMillis() >= nextChestTime) {
            processNextCategory();
        }
    }

    private void handleProcessingComplete() {
        info("所有物品分类完成，等待 " + (roundDelay.get() / 1000) + " 秒后开始下一轮");
        nextRoundTime = System.currentTimeMillis() + roundDelay.get();
        setState(ProcessingState.WAITING_NEXT_ROUND);
    }

    private void handleWaitingNextRound() {
        if (System.currentTimeMillis() >= nextRoundTime) {
            info("等待结束，重置状态机准备下一轮");
            resetState();
        }
    }

    // ===== 箱子处理方法 =====

    private void processNextChest() {
        if (currentChestIndex >= chestsToProcess.size()) {
            // 所有箱子处理完成，开始分类物品
            info("所有源箱子处理完成，开始物品分类");
            setState(ProcessingState.CATEGORIZING_ITEMS);
            return;
        }

        // 检查玩家库存是否有足够空间
        if (mc.player != null) {
            int emptySlots = countEmptyInventorySlots();
            if (emptySlots < 2) { // 保留至少2个空位
                info("玩家库存空间不足（剩余" + emptySlots + "个空位），需要先整理库存");
                setState(ProcessingState.CATEGORIZING_ITEMS); // 直接跳到分类阶段
                return;
            }
            info("玩家库存剩余" + emptySlots + "个空位，继续处理源箱子");
        }
        BlockPos chest = chestsToProcess.get(currentChestIndex).toBlockPos();
        info("处理箱子 " + (currentChestIndex + 1) + "/" + chestsToProcess.size() +
            " 坐标：" + chest.toShortString());

        // 确保没有打开的容器
        if (mc.player != null && mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
            mc.player.closeHandledScreen();
            setState(ProcessingState.CLOSING_SOURCE_CHEST);
            return;
        }

        // 设置当前目标箱子并开始移动
        currentOpenChest = chest;
        setState(ProcessingState.MOVING_TO_SOURCE_CHEST);
    }

    private void moveToNextChest() {
        currentChestIndex++;

        if (currentChestIndex >= chestsToProcess.size()) {
            setState(ProcessingState.CATEGORIZING_ITEMS);
        } else {
            nextChestTime = System.currentTimeMillis() + chestDelay.get();
            setState(ProcessingState.WAITING_NEXT_CHEST);
        }
    }

    private void processNextCategory() {
        if (currentCategoryIndex >= categoriesToProcess.size()) {
            info("所有分类处理完成");
            setState(ProcessingState.PROCESSING_COMPLETE);
            return;
        }

        String category = categoriesToProcess.get(currentCategoryIndex);
        List<ItemStack> items = categorizedItems.get(category);

        info("处理分类 " + (currentCategoryIndex + 1) + "/" + categoriesToProcess.size() +
            "：" + category + "（" + items.size() + " 个物品）");

        // 使用智能箱子缓存查找目标容器
        ChestLocation targetChest = chestCache.findChestForItem(category);

        if (targetChest == null) {
            info("分类 " + category + " 找不到合适的存储箱子，跳过");
            moveToNextCategory();
            return;
        }

        info("为分类 " + category + " 找到目标箱子：" + targetChest);

        // 开始移动到目标箱子
        setState(ProcessingState.MOVING_TO_TARGET_CONTAINER);
    }

    private void moveToNextCategory() {
        currentCategoryIndex++;

        if (currentCategoryIndex >= categoriesToProcess.size()) {
            setState(ProcessingState.PROCESSING_COMPLETE);
        } else {
            nextChestTime = System.currentTimeMillis() + categoryDelay.get();
            setState(ProcessingState.WAITING_NEXT_CATEGORY);
        }
    }

    public static void disableBlockActions() {
        var settings = BaritoneAPI.getSettings();
        // 禁止破坏方块
        settings.allowBreak.value = false;

        // 禁止放置方块
        settings.allowPlace.value = false;
    }

    // ===== 库存操作方法 =====

    private void openChest(BlockPos pos) {
        if (mc.interactionManager == null || mc.player == null) return;

        // 使用交互工具打开箱子
        BaritoneUtil.clickBlock(pos, Direction.UP, true, Hand.MAIN_HAND, BaritoneUtil.SwingSide.All);
    }

    private void withdrawAllItems(GenericContainerScreenHandler handler) {
        if (mc.player == null) return;

        int containerSize = handler.getInventory().size();

        // 从容器中提取所有物品
        for (int slot = 0; slot < containerSize; slot++) {
            ItemStack stack = handler.getInventory().getStack(slot);

            if (!stack.isEmpty()) {
                // 使用 Meteor 的库存工具进行快速移动
                InvUtils.shiftClick().slotId(slot);
            }
        }
    }

    private void depositItemsByCategory(GenericContainerScreenHandler handler, String category) {
        if (mc.player == null) return;

        // 从玩家主库存存储物品（槽位 0-35）
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);

            if (!stack.isEmpty()) {
                String itemCategory = classifier.classifyItem(stack);

                if (category.equals(itemCategory)) {
                    info("存储物品：" + stack.getName().getString() + " 到分类：" + category);

                    // 使用快速移动将物品放入容器
                    InvUtils.shiftClick().slot(slot);
                }
            }
        }
    }

    private boolean isInventoryOperationComplete() {
        // 简单的完成检测：等待一小段时间
        // 更复杂的实现可以检查物品是否真的移动了
        try {
            Thread.sleep(100);
            return true;
        } catch (InterruptedException e) {
            return true;
        }
    }

    // ===== 辅助方法 =====

    private boolean isPlayerInRange() {
        if (mc.player == null) return false;

        BlockPos center = centerPosition.get();
        boolean isWithinDistance = mc.player.getBlockPos().isWithinDistance(new BlockPos(
                (int) (center.getX() + 0.5),
                (int) (center.getY() + 0.5),
                (int) (center.getZ() + 0.5)
            ), 200
        );

        return isWithinDistance;
    }

    private int countEmptyInventorySlots() {
        if (mc.player == null) return 0;

        int emptySlots = 0;
        // 计算主库存空位（槽位 0-35，不包括盔甲槽）
        for (int slot = 0; slot < 36; slot++) {
            if (mc.player.getInventory().getStack(slot).isEmpty()) {
                emptySlots++;
            }
        }

        return emptySlots;
    }

    private void resetState() {
        info("重置物品整理状态机（当前状态：" + currentState + "）");

        // 根据引导设置决定初始状态
        if (enableGuide.get()) {
            currentState = ProcessingState.GUIDE;
            info("进入引导模式，请通过交互箱子来配置源箱子列表");
        } else {
            currentState = ProcessingState.IDLE;
        }

        chestsToProcess.clear();
        currentChestIndex = 0;
        categorizedItems.clear();
        categoriesToProcess.clear();
        currentCategoryIndex = 0;
        nextChestTime = 0;
        nextRoundTime = 0;
        currentOpenChest = null;

        // 关闭打开的容器
        if (mc.player != null && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
            mc.player.closeHandledScreen();
        }
    }

    private void setState(ProcessingState newState) {
        info("状态变更：" + currentState + " → " + newState);
        currentState = newState;
        stateStartTime = System.currentTimeMillis();
    }

    private void startItemSorting() {
        if (currentState != ProcessingState.IDLE) {
            error("尝试在非空闲状态下开始处理，当前状态：" + currentState + "，强制重置");
            resetState();
            return;
        }

        chestsToProcess.clear();
        currentChestIndex = 0;
        categorizedItems.clear();

        List<String> list = sourceChests.get();
        for (String string : list) {
            ChestLocation location = ChestLocation.fromJson(string);
            if (!chestsToProcess.contains(location)) {
                if (location != null)
                    chestsToProcess.add(location);
            }
        }

        if (chestsToProcess.isEmpty()) {
            info("未配置源箱子，跳过处理");
            setState(ProcessingState.PROCESSING_COMPLETE);
            return;
        }

        info("开始处理 " + chestsToProcess.size() + " 个源箱子");
        processNextChest();
    }

    @Override
    public void info(String message, Object... args) {
        if (lastLog.equals(message))
            return;

        super.info(message, args);
        lastLog = message;
    }

    // 分类预设
    enum Preset {
        /**
         * 激进分类：快速、基于名称或关键字进行粗暴分组
         */
        AGGRESSIVE("暴力分类"),

        /**
         * 智能分类：启用属性分析、附魔、稀有度等多维度判断
         */
        SMART("智能分类"),
        /**
         * 极简分类：仅按物品大类（方块、工具、食物等）划分
         */
        MINIMAL("极简分类"),

        /**
         * BALANCED - 平衡分类
         * 在准确度与性能之间取得平衡（推荐默认）
         */
        BALANCED("平衡分类"),
        /**
         * CUSTOM - 自定义分类
         * 使用玩家配置的规则，不使用内置逻辑
         */
        CUSTOM("自定义分类");

        private final String displayName;

        Preset(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

}
