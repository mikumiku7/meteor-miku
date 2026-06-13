package com.github.mikumiku.addon.modules;


import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.*;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectIntImmutablePair;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.VillagerProfession;

import java.util.*;

public class VillagerTrader extends BaseModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRestock = settings.createGroup("补给设置");
    private final SettingGroup sgTrading = settings.createGroup("交易设置");
    private final SettingGroup sgStorage = settings.createGroup("存储设置");
    private final SettingGroup sgBooks = settings.createGroup("附魔书设置");

    // 通用设置

    private final Setting<Integer> waitTimeout = sgGeneral.add(new IntSetting.Builder()
        .name("交互超时")
        .description("等待交互完成的超时时间(tick)")
        .defaultValue(100)
        .min(20)
        .max(600)
        .sliderMax(600)
        .build()
    );

    private final Setting<Integer> pathingDistance = sgGeneral.add(new IntSetting.Builder()
        .name("手长")
        .description("操作时与目标的距离，你手有多长？")
        .defaultValue(4)
        .min(1)
        .max(10)
        .sliderMax(10)
        .build()
    );

    // 补给设置
    private final Setting<BlockPos> restockChest = sgRestock.add(new BlockPosSetting.Builder()
        .name("补给箱位置")
        .description("存放绿宝石的箱子位置")
        .defaultValue(new BlockPos(0, 0, 0))
        .build()
    );

    private final Setting<Integer> restockThreshold = sgRestock.add(new IntSetting.Builder()
        .name("补给阈值")
        .description("绿宝石数量低于此值时触发补给")
        .defaultValue(10)
        .min(1)
        .max(2304)
        .sliderMax(2304)
        .build()
    );

    private final Setting<Integer> restockStacks = sgRestock.add(new IntSetting.Builder()
        .name("每次补给组数")
        .description("每次补给的组数")
        .defaultValue(3)
        .min(1)
        .max(36)
        .sliderMax(36)
        .build()
    );

    // 交易设置
    private final Setting<List<VillagerProfession>> professions = sgTrading.add(new VillagerProfessionListSetting.Builder()
        .name("村民职业")
        .description("要交易的村民职业")
        .build()
    );

    private final Setting<List<Item>> buyItems = sgTrading.add(new ItemListSetting.Builder()
        .name("购买物品")
        .description("要购买的物品列表")
        .defaultValue(Arrays.asList(Items.EXPERIENCE_BOTTLE))
        .build()
    );

    private final Setting<Integer> maxSpendPerTrade = sgTrading.add(new IntSetting.Builder()
        .name("每次交易最大花费")
        .description("单次交易最多花费的绿宝石数")
        .defaultValue(1)
        .min(1)
        .max(64)
        .sliderMax(64)
        .build()
    );

    private final Setting<Map<String, String>> itemMaxSpend = sgTrading.add(new StringMapSetting.Builder()
        .name("物品特定花费")
        .description("特定物品的最大花费(物品英文id:价格)")
        .defaultValue(Maps.newHashMap(Map.of("experience_bottle", "1")))
        .build()
    );

    private final Setting<Integer> storeThreshold = sgTrading.add(new IntSetting.Builder()
        .name("存储阈值")
        .description("购买物品槽位超过此值时存储")
        .defaultValue(20)
        .min(1)
        .max(36)
        .sliderMax(36)
        .build()
    );

    private final Setting<Integer> restockWaitTime = sgTrading.add(new IntSetting.Builder()
        .name("补货等待时间")
        .description("等待村民补货的时间(秒)")
        .defaultValue(60)
        .min(10)
        .max(600)
        .sliderMax(600)
        .build()
    );

    // 存储设置
    private final Setting<BlockPos> storeChest = sgStorage.add(new BlockPosSetting.Builder()
        .name("存储箱位置")
        .description("存放购买物品的箱子位置")
        .defaultValue(new BlockPos(0, 0, 0))
        .build()
    );

    // 书籍设置
    private final Setting<Boolean> buyEnchantedBooks = sgBooks.add(new BoolSetting.Builder()
        .name("购买附魔书")
        .description("是否购买附魔书")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyMaxLevel = sgBooks.add(new BoolSetting.Builder()
        .name("仅最高等级")
        .description("只购买最高等级的附魔书")
        .defaultValue(true)
        .visible(buyEnchantedBooks::get)
        .build()
    );

    private final Setting<Boolean> onlyDesiredEnchants = sgBooks.add(new BoolSetting.Builder()
        .name("仅指定附魔")
        .description("只购买指定的附魔书")
        .defaultValue(true)
        .visible(buyEnchantedBooks::get)
        .build()
    );

    private final Setting<Set<RegistryKey<Enchantment>>> desiredEnchants = sgBooks.add(new EnchantmentListSetting.Builder()
        .name("期望附魔")
        .description("期望的附魔")
        .defaultValue(new LinkedHashSet<>(Arrays.asList(Enchantments.MENDING, Enchantments.UNBREAKING)))
        .visible(() -> buyEnchantedBooks.get() && onlyDesiredEnchants.get())
        .build()
    );

    private final Setting<BlockPos> bookRestockChest = sgBooks.add(new BlockPosSetting.Builder()
        .name("书本补给箱")
        .description("存放普通书的箱子位置")
        .defaultValue(new BlockPos(0, 0, 0))
        .visible(buyEnchantedBooks::get)
        .build()
    );

    private final Setting<Integer> bookRestockThreshold = sgBooks.add(new IntSetting.Builder()
        .name("书本补给阈值")
        .description("书本数量低于此值时补给")
        .defaultValue(24)
        .min(1)
        .max(64)
        .sliderMax(64)
        .visible(buyEnchantedBooks::get)
        .build()
    );

    // 状态变量
    private State state = State.RESTOCK_GO_TO_CHEST;
    private final Set<Integer> interactedVillagers = new HashSet<>();
    private int waitTimer = 0;
    private int restockWaitTimer = 0;
    private boolean pathingComplete = false;
    private int containerOpenTimer = 0;

    public VillagerTrader() {
        super(CATEGORY_MIKU_BUILD, "极速自动村民交易", "自动与村民交易");
    }

    @Override
    public void onActivate() {
        state = State.RESTOCK_GO_TO_CHEST;
        interactedVillagers.clear();
        waitTimer = 0;
        restockWaitTimer = 0;
        pathingComplete = false;
        containerOpenTimer = 0;
    }

    @Override
    public void onDeactivate() {
        state = State.RESTOCK_GO_TO_CHEST;
        interactedVillagers.clear();
        if (mc.player != null && mc.player.currentScreenHandler != null) {
            mc.player.closeHandledScreen();
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        toggle();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        switch (state) {
            case RESTOCK_GO_TO_CHEST -> handleRestockGoToChest();
            case RESTOCK_PATHING_TO_CHEST -> handleRestockPathing();
            case RESTOCK_WITHDRAWING_FROM_CHEST -> handleRestockWithdrawing();
            case RESTOCK_CRAFT_EMERALD_BLOCKS -> handleCraftEmeraldBlocks();
            case RESTOCK_AWAIT_CRAFT_EMERALD_BLOCKS -> handleAwaitCraft();
            case TRADING_INTERACT_WITH_VILLAGER -> handleInteractWithVillager();
            case TRADING_AWAIT_INTERACT_WITH_VILLAGER -> handleAwaitInteract();
            case TRADING_TRY_START_PURCHASE -> handleStartPurchase();
            case TRADING_AWAIT_PURCHASE -> handleAwaitPurchase();
            case STORE_GO_TO_CHEST -> handleStoreGoToChest();
            case STORE_DEPOSIT -> handleStoreDeposit();
            case STORE_AWAIT_DEPOSIT -> handleAwaitStoreDeposit();
            case WAITING_FOR_VILLAGER_TRADE_RESTOCK -> handleWaitingForRestock();
            case BOOK_RESTOCK_GO_TO_CHEST -> handleBookRestockGoToChest();
            case BOOK_RESTOCK_PATHING_TO_CHEST -> handleBookRestockPathing();
            case BOOK_RESTOCK_WITHDRAWING_FROM_CHEST -> handleBookRestockWithdrawing();
        }
    }

    // ==================== 补给相关 ====================

    private void handleRestockGoToChest() {
        int emeraldCount = countItem(Items.EMERALD);
        int emeraldBlockCount = countItem(Items.EMERALD_BLOCK);

        if (emeraldCount + (emeraldBlockCount * 9) < restockThreshold.get()) {
            ChatUtils.info("绿宝石不足，前往补给箱");
            pathingComplete = false;
//            MikuUtil.pathTo(new BlockPos(0, 0, 0), pathingDistance.get());
            MikuUtil.pathTo(restockChest.get(), pathingDistance.get());

            setState(State.RESTOCK_PATHING_TO_CHEST);
        } else if (emeraldBlockCount > 0) {
            setState(State.RESTOCK_CRAFT_EMERALD_BLOCKS);
        } else {
            setState(State.TRADING_INTERACT_WITH_VILLAGER);
        }
    }

    private void handleRestockPathing() {
        if (!pathingComplete) {
            double distance = Via.getEntityPos(mc.player).distanceTo(Vec3d.ofCenter(restockChest.get()));
            if (distance <= pathingDistance.get() + 1) {
                pathingComplete = true;
                containerOpenTimer = 0;
                // 右键打开箱子
                interactWithBlock(restockChest.get());
            }
        } else {
            containerOpenTimer++;
            if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
                waitTimer = 0;
                setState(State.RESTOCK_WITHDRAWING_FROM_CHEST);
            } else if (containerOpenTimer > waitTimeout.get()) {
                ChatUtils.warning("打开补给箱超时，重试");
                setState(State.RESTOCK_GO_TO_CHEST);
            }
        }
    }

    private void handleRestockWithdrawing() {
        waitTimer++;

        // 从箱子中取出绿宝石和绿宝石块
        if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
            withdrawItems(Items.EMERALD, restockStacks.get());
            int stacks = 1;
            if (restockStacks.get() > 9) {
                stacks = restockStacks.get() / 9;
            }

            withdrawItems(Items.EMERALD_BLOCK, stacks);
            mc.player.closeHandledScreen();
        }

        if (waitTimer > 20) {
            int emeraldCount = countItem(Items.EMERALD);
            int emeraldBlockCount = countItem(Items.EMERALD_BLOCK);

            if (emeraldCount + (emeraldBlockCount * 9) < restockThreshold.get()) {
                ChatUtils.warning("补给后绿宝石仍不足 " + restockThreshold.get() + "，继续交易");
            }

            if (emeraldBlockCount > 0) {
                setState(State.RESTOCK_CRAFT_EMERALD_BLOCKS);
            } else {
                setState(State.TRADING_INTERACT_WITH_VILLAGER);
            }
        }
    }

    private void handleCraftEmeraldBlocks() {
        int emeraldBlockCount = countItem(Items.EMERALD_BLOCK);
        if (emeraldBlockCount == 0) {
            setState(State.TRADING_INTERACT_WITH_VILLAGER);
            return;
        }

        int emptySlots = countEmptySlots();
        if (emptySlots < 5) {
            ChatUtils.warning("背包空间不足，无法分解绿宝石块");
            setState(State.TRADING_INTERACT_WITH_VILLAGER);
            return;
        }

        // 打开合成台并分解绿宝石块
        int emeraldBlockSlot = findItemSlot(Items.EMERALD_BLOCK);
        if (emeraldBlockSlot != -1) {

            clickCraft(emeraldBlockSlot);

            setState(State.RESTOCK_AWAIT_CRAFT_EMERALD_BLOCKS);
        } else {
            setState(State.TRADING_INTERACT_WITH_VILLAGER);
        }
    }

    private void handleAwaitCraft() {
        waitTimer++;
        if (waitTimer > 10) {
            waitTimer = 0;
            int emeraldBlockCount = countItem(Items.EMERALD_BLOCK);
            if (emeraldBlockCount > 0) {
                setState(State.RESTOCK_CRAFT_EMERALD_BLOCKS);
            } else {
                setState(State.TRADING_INTERACT_WITH_VILLAGER);
            }
        }
    }

    // ==================== 交易相关 ====================

    private void handleInteractWithVillager() {
        int buyItemCount = countBuyItemSlotUsages();
        if (buyItemCount > storeThreshold.get()) {
            setState(State.STORE_GO_TO_CHEST);
            return;
        }

        VillagerEntity nextVillager = findNextVillager();
        if (nextVillager == null) {
            if (interactedVillagers.isEmpty()) {
                ChatUtils.warning("未找到可交易的村民，返回补给");
                setState(State.RESTOCK_GO_TO_CHEST);
            } else {
                if (countBuyItems() > 0) {
                    setState(State.STORE_GO_TO_CHEST);
                } else {
                    setState(State.WAITING_FOR_VILLAGER_TRADE_RESTOCK);
                    restockWaitTimer = 0;
                    ChatUtils.info("等待村民补货 " + restockWaitTime.get() + " 秒");
                }
            }
            return;
        }

        interactedVillagers.add(nextVillager.getId());
        interactWithEntity(nextVillager);
        containerOpenTimer = 0;
        setState(State.TRADING_AWAIT_INTERACT_WITH_VILLAGER);
    }

    private void handleAwaitInteract() {
        containerOpenTimer++;

        if (mc.player.currentScreenHandler instanceof MerchantScreenHandler) {
            waitTimer = 0;
            setState(State.TRADING_TRY_START_PURCHASE);
        } else if (containerOpenTimer > waitTimeout.get()) {
            ChatUtils.warning("与村民交互超时，寻找下一个");
            setState(State.TRADING_INTERACT_WITH_VILLAGER);
        }
    }

    private void handleStartPurchase() {
        if (!(mc.player.currentScreenHandler instanceof MerchantScreenHandler merchantHandler)) {
            setState(State.TRADING_INTERACT_WITH_VILLAGER);
            return;
        }

        int emptySlots = countEmptySlots();
        int requiredEmptySlots = 2;

        if (emptySlots <= requiredEmptySlots) {
            ChatUtils.info("背包空间不足 (" + emptySlots + " 空槽)，前往存储");
            mc.player.closeHandledScreen();
            setState(State.STORE_GO_TO_CHEST);
            return;
        }

        int availableSlots = emptySlots - requiredEmptySlots;
        List<TradeOffer> validTrades = findValidTrades(merchantHandler);

        if (validTrades.isEmpty()) {
            ChatUtils.info("该村民没有可购买的交易");
            mc.player.closeHandledScreen();
            setState(State.TRADING_INTERACT_WITH_VILLAGER);
            return;
        }

        // 检查是否需要补充书本
        if (shouldRestockBooks()) {
            mc.player.closeHandledScreen();
            setState(State.BOOK_RESTOCK_GO_TO_CHEST);
            return;
        }

        // 执行交易
        boolean traded = false;
        for (TradeOffer trade : validTrades) {
            int slotsNeeded = estimateSlotsNeeded(trade);
            if (slotsNeeded > availableSlots) {
                continue;
            }

            // 执行交易
            if (executeTrade(merchantHandler, trade)) {
                traded = true;
                availableSlots -= slotsNeeded;
                ChatUtils.info("购买: " + trade.getSellItem().getName().getString());
            }

            if (availableSlots <= 0) break;
        }

        mc.player.closeHandledScreen();
        setState(State.TRADING_AWAIT_PURCHASE);
    }

    private void handleAwaitPurchase() {
        waitTimer++;
        if (waitTimer > 20) {
            waitTimer = 0;
            if (countBuyItemSlotUsages() > storeThreshold.get()) {
                setState(State.STORE_GO_TO_CHEST);
            } else if (countItem(Items.EMERALD) < restockThreshold.get()) {
                setState(State.RESTOCK_GO_TO_CHEST);
            } else {
                setState(State.TRADING_INTERACT_WITH_VILLAGER);
            }
        }
    }

    // ==================== 存储相关 ====================

    private void handleStoreGoToChest() {
        ChatUtils.info("前往存储箱");
        pathingComplete = false;
        MikuUtil.pathTo(storeChest.get(), pathingDistance.get());
        setState(State.STORE_DEPOSIT);
    }

    private void handleStoreDeposit() {
        if (!pathingComplete) {
            double distance = Via.getEntityPos(mc.player).distanceTo(Vec3d.ofCenter(storeChest.get()));
            if (distance <= pathingDistance.get() + 1) {
                pathingComplete = true;
                containerOpenTimer = 0;
                interactWithBlock(storeChest.get());
            }
        } else {
            containerOpenTimer++;
            if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
                waitTimer = 0;
                // 存储购买的物品
                depositBuyItems();
                mc.player.closeHandledScreen();
                setState(State.STORE_AWAIT_DEPOSIT);
            } else if (containerOpenTimer > waitTimeout.get()) {
                ChatUtils.warning("打开存储箱超时，重试");
                setState(State.STORE_GO_TO_CHEST);
            }
        }
    }

    private void handleAwaitStoreDeposit() {
        waitTimer++;
        if (waitTimer > 20) {
            waitTimer = 0;
            int remaining = countBuyItems();
            if (remaining > 0) {
                ChatUtils.warning("无法完全存储物品（剩余 " + remaining + "），继续交易");
            }
            setState(State.RESTOCK_GO_TO_CHEST);
        }
    }

    // ==================== 等待补货 ====================

    private void handleWaitingForRestock() {
        restockWaitTimer++;
        if (restockWaitTimer >= restockWaitTime.get() * 20) {
            interactedVillagers.clear();
            ChatUtils.info("等待完成，重新开始交易");
            setState(State.RESTOCK_GO_TO_CHEST);
        }
    }

    // ==================== 书本补给 ====================

    private void handleBookRestockGoToChest() {
        ChatUtils.info("前往书本补给箱");
        pathingComplete = false;
        MikuUtil.pathTo(bookRestockChest.get(), pathingDistance.get());
        setState(State.BOOK_RESTOCK_PATHING_TO_CHEST);
    }

    private void handleBookRestockPathing() {
        if (!pathingComplete) {
            double distance = Via.getEntityPos(mc.player).distanceTo(Vec3d.ofCenter(bookRestockChest.get()));
            if (distance <= pathingDistance.get() + 1) {
                pathingComplete = true;
                containerOpenTimer = 0;
                interactWithBlock(bookRestockChest.get());
            }
        } else {
            containerOpenTimer++;
            if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
                waitTimer = 0;
                setState(State.BOOK_RESTOCK_WITHDRAWING_FROM_CHEST);
            } else if (containerOpenTimer > waitTimeout.get()) {
                ChatUtils.warning("打开书本箱超时，重试");
                setState(State.BOOK_RESTOCK_GO_TO_CHEST);
            }
        }
    }

    private void handleBookRestockWithdrawing() {
        waitTimer++;

        if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
            withdrawItems(Items.BOOK, bookRestockThreshold.get() / 64);
            mc.player.closeHandledScreen();
        }

        if (waitTimer > 20) {
            waitTimer = 0;
            setState(State.TRADING_INTERACT_WITH_VILLAGER);
        }
    }

    // ==================== 工具方法 ====================

    private void setState(State newState) {
        this.state = newState;
    }

    private VillagerEntity findNextVillager() {
        if (mc.world == null) return null;

        VillagerEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof VillagerEntity villager)) continue;
            if (interactedVillagers.contains(villager.getId())) continue;

            VillagerProfession profession = getVillagerProfession(villager);
            if (!professions.get().contains(profession)) continue;

            double dist = mc.player.squaredDistanceTo(villager);
            if (dist < closestDist) {
                closestDist = dist;
                closest = villager;
            }
        }

        return closest;
    }

    public static VillagerProfession getVillagerProfession(VillagerEntity villager) {
        return Via.getVillagerProfession(villager);
    }

    private List<TradeOffer> findValidTrades(MerchantScreenHandler handler) {
        List<TradeOffer> validTrades = new ArrayList<>();

        for (TradeOffer offer : handler.getRecipes()) {
            if (offer.isDisabled()) continue;
            if (!isValidTrade(offer)) continue;

            validTrades.add(offer);
        }

        return validTrades;
    }

    private boolean isValidTrade(TradeOffer offer) {
        ItemStack output = offer.getSellItem();
        ItemStack firstInput = offer.getFirstBuyItem().itemStack();

        // 只购买需要绿宝石的交易
        if (firstInput.getItem() != Items.EMERALD) return false;

        // 检查是否是目标物品
        HashSet<Item> targetItems = new HashSet<>(buyItems.get());
        if (buyEnchantedBooks.get() && output.getItem() == Items.ENCHANTED_BOOK) {
            return matchesEnchantmentRequirements(output);
        }

        if (!targetItems.contains(output.getItem())) return false;

        // 检查价格
        int cost = firstInput.getCount();
        String outputName = Registries.ITEM.getId(output.getItem()).getPath();
        String string = itemMaxSpend.get().get(outputName);

        int maxSpend = string2Int(maxSpendPerTrade.get(), string);

        return cost <= maxSpend;
    }

    private Integer string2Int(Integer defaultValue, String string) {
        try {
            return string == null ? defaultValue : Integer.parseInt(string);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public Map<RegistryEntry<Enchantment>, Integer> getEnchants(ItemStack stack) {
        LinkedHashMap<RegistryEntry<Enchantment>, Integer> map = new LinkedHashMap<>();
        List<Pair<RegistryEntry<Enchantment>, Integer>> ret = new ArrayList<>();
        for (var e : EnchantmentHelper.getEnchantments(stack).getEnchantmentEntries()) {
            RegistryEntry<Enchantment> key = e.getKey();
            int value = e.getIntValue();
            map.put(key, value);
            ret.add(ObjectIntImmutablePair.of(key, value));
        }
        return map;
    }

    private boolean matchesEnchantmentRequirements(ItemStack book) {
        Map<RegistryEntry<Enchantment>, Integer> enchants = getEnchants(book);

        if (enchants.size() != 1) return false;

        for (Map.Entry<RegistryEntry<Enchantment>, Integer> entry : enchants.entrySet()) {
            RegistryEntry<Enchantment> enchant = entry.getKey();
            int level = entry.getValue();
            int ml = enchant.value().getMaxLevel();


            // 检查最高等级要求
            if (onlyMaxLevel.get()) {
                if (level < enchant.value().getMaxLevel()) return false;
            }

            // 检查指定附魔要求
            if (onlyDesiredEnchants.get() && !desiredEnchants.get().isEmpty()) {
                if (!desiredEnchants.get().contains(enchant)) return false;
            }
        }

        return true;
    }

    private boolean executeTrade(MerchantScreenHandler handler, TradeOffer offer) {
        int recipeIndex = handler.getRecipes().indexOf(offer);
        if (recipeIndex == -1) return false;

        // 选择交易
        handler.setRecipeIndex(recipeIndex);
        handler.switchTo(recipeIndex);

        // Shift+左键快速交易
        int maxTrades = offer.getMaxUses() - offer.getUses();
        for (int i = 0; i < maxTrades; i++) {
            clickSlot(2, 0, SlotActionType.QUICK_MOVE);
        }

        return true;
    }

    private int estimateSlotsNeeded(TradeOffer offer) {
        ItemStack output = offer.getSellItem();
        int maxUses = offer.getMaxUses() - offer.getUses();
        int totalItems = maxUses * output.getCount();
        int maxStackSize = output.getMaxCount();

        return (totalItems + maxStackSize - 1) / maxStackSize;
    }

    private void interactWithBlock(BlockPos pos) {
        BaritoneUtil.clickBlock(pos, Direction.UP, true, Hand.MAIN_HAND, BaritoneUtil.SwingSide.All);
    }

    private void interactWithEntity(Entity entity) {
        if (mc.interactionManager == null) return;
        mc.interactionManager.interactEntity(
            mc.player,
            entity,
            Hand.MAIN_HAND
        );
    }

    private void clickSlot(int slot, int button, SlotActionType actionType) {
        if (mc.interactionManager == null) return;
        mc.interactionManager.clickSlot(
            mc.player.currentScreenHandler.syncId,
            slot,
            button,
            actionType,
            mc.player
        );
    }

    private void clickCraft(int slot) {
        if (mc.interactionManager == null) return;
        ScreenHandler currentScreenHandler = mc.player.currentScreenHandler;
        if (mc.player.currentScreenHandler instanceof PlayerScreenHandler) {
            // 2️⃣ 拿起整组
            mc.interactionManager.clickSlot(currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(currentScreenHandler.syncId, 1, 0, SlotActionType.PICKUP, mc.player);

            mc.interactionManager.clickSlot(currentScreenHandler.syncId, 0, 0, SlotActionType.QUICK_MOVE, mc.player);

            mc.player.closeHandledScreen();
        } else {
            mc.setScreen(new InventoryScreen(mc.player));
        }
    }

    private void withdrawItems(Item item, int stacks) {
        if (mc.player.currentScreenHandler == mc.player.playerScreenHandler) return;

        int withdrawn = 0;
        for (int i = 0; i < mc.player.currentScreenHandler.slots.size(); i++) {
            if (withdrawn >= stacks) break;

            ItemStack stack = mc.player.currentScreenHandler.getSlot(i).getStack();
            if (stack.getItem() == item) {
                clickSlot(i, 0, SlotActionType.QUICK_MOVE);
                withdrawn++;
            }
        }
    }

    private void depositBuyItems() {
        if (mc.player.currentScreenHandler == mc.player.playerScreenHandler) return;

        Set<Item> targetItems = new HashSet<>(buyItems.get());
        if (buyEnchantedBooks.get()) {
            targetItems.add(Items.ENCHANTED_BOOK);
        }

        // 遍历玩家背包，将目标物品存入箱子
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            if (targetItems.contains(stack.getItem())) {
                clickSlot(i, 0, SlotActionType.QUICK_MOVE);
            }
        }
    }

    private int countItem(Item item) {
        int count = 0;
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private int countEmptySlots() {
        int count = 0;
        for (int i = 9; i < 45; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private int countBuyItems() {
        int count = 0;
        Set<Item> targetItems = new HashSet<>(buyItems.get());
        if (buyEnchantedBooks.get()) {
            targetItems.add(Items.ENCHANTED_BOOK);
        }

        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            if (targetItems.contains(stack.getItem())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private int countBuyItemSlotUsages() {
        int count = 0;
        HashSet<Item> targetItems = new HashSet<>(buyItems.get());
        if (buyEnchantedBooks.get()) {
            targetItems.add(Items.ENCHANTED_BOOK);
        }

        for (int i = 0; i < 45; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            if (targetItems.contains(stack.getItem())) {
                count++;
            }
        }
        return count;
    }

    private int findItemSlot(Item item) {
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private boolean shouldRestockBooks() {
        if (!buyEnchantedBooks.get()) return false;

        int bookCount = countItem(Items.BOOK);
        return bookCount < bookRestockThreshold.get();
    }

    // ==================== 状态枚举 ====================

    private enum State {
        RESTOCK_GO_TO_CHEST,
        RESTOCK_PATHING_TO_CHEST,
        RESTOCK_WITHDRAWING_FROM_CHEST,
        RESTOCK_CRAFT_EMERALD_BLOCKS,
        RESTOCK_AWAIT_CRAFT_EMERALD_BLOCKS,
        TRADING_INTERACT_WITH_VILLAGER,
        TRADING_AWAIT_INTERACT_WITH_VILLAGER,
        TRADING_TRY_START_PURCHASE,
        TRADING_AWAIT_PURCHASE,
        STORE_GO_TO_CHEST,
        STORE_DEPOSIT,
        STORE_AWAIT_DEPOSIT,
        WAITING_FOR_VILLAGER_TRADE_RESTOCK,
        BOOK_RESTOCK_GO_TO_CHEST,
        BOOK_RESTOCK_PATHING_TO_CHEST,
        BOOK_RESTOCK_WITHDRAWING_FROM_CHEST
    }
}
