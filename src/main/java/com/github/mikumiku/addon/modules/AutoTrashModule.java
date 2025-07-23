package com.github.mikumiku.addon.modules;


import com.github.mikumiku.addon.MikuMikuAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

import java.util.*;

public class AutoTrashModule extends Module {

    // 设置组
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgGroup1 = settings.createGroup("垃圾组 1");
    private final SettingGroup sgGroup2 = settings.createGroup("垃圾组 2");
    private final SettingGroup sgGroup3 = settings.createGroup("垃圾组 3");

    // 通用设置
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("丢弃延迟")
        .description("每次丢弃物品之间的延迟（游戏刻）")
        .defaultValue(5)
        .min(1)
        .max(60)
        .sliderMin(1)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> dropAll = sgGeneral.add(new BoolSetting.Builder()
        .name("全部丢弃")
        .description("是否丢弃整组物品，否则只丢弃一个")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> excludeHotbar = sgGeneral.add(new BoolSetting.Builder()
        .name("排除快捷栏")
        .description("不丢弃快捷栏中的物品")
        .defaultValue(false)
        .build()
    );

    // 垃圾组 1 设置
    private final Setting<Boolean> group1Enabled = sgGroup1.add(new BoolSetting.Builder()
        .name("启用垃圾组 1")
        .description("启用第一个垃圾组")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> group1Name = sgGroup1.add(new StringSetting.Builder()
        .name("组名")
        .description("垃圾组 1 的名称")
        .defaultValue("食物垃圾")
        .build()
    );

    private final Setting<List<Item>> group1Items = sgGroup1.add(new ItemListSetting.Builder()
        .name("物品列表")
        .description("垃圾组 1 中的物品")
        .defaultValue(Arrays.asList(
            Items.ROTTEN_FLESH,
            Items.SPIDER_EYE,
            Items.POISONOUS_POTATO,
            Items.PUFFERFISH
        ))
        .build()
    );

    // 垃圾组 2 设置
    private final Setting<Boolean> group2Enabled = sgGroup2.add(new BoolSetting.Builder()
        .name("启用垃圾组 2")
        .description("启用第二个垃圾组")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> group2Name = sgGroup2.add(new StringSetting.Builder()
        .name("组名")
        .description("垃圾组 2 的名称")
        .defaultValue("方块垃圾")
        .build()
    );

    private final Setting<List<Item>> group2Items = sgGroup2.add(new ItemListSetting.Builder()
            .name("物品列表")
            .description("垃圾组 2 中的物品")
            .defaultValue(Arrays.asList(
                Items.NETHERRACK,
                Items.BLACKSTONE,
//            Items.COBBLESTONE,
                Items.DIRT,
                Items.GRAVEL,
                Items.GRANITE,
                Items.DIORITE,
                Items.ANDESITE
            ))
            .build()
    );

    // 垃圾组 3 设置
    private final Setting<Boolean> group3Enabled = sgGroup3.add(new BoolSetting.Builder()
        .name("启用垃圾组 3")
        .description("启用第三个垃圾组")
        .defaultValue(false)
        .build()
    );

    private final Setting<String> group3Name = sgGroup3.add(new StringSetting.Builder()
        .name("组名")
        .description("垃圾组 3 的名称")
        .defaultValue("自定义垃圾")
        .build()
    );

    private final Setting<List<Item>> group3Items = sgGroup3.add(new ItemListSetting.Builder()
        .name("物品列表")
        .description("垃圾组 3 中的物品")
        .defaultValue(new ArrayList<>())
        .build()
    );

    // 内部变量
    private int tickTimer = 0;
    private Set<Item> allTrashItems = new HashSet<>();

    public AutoTrashModule() {
        super(MikuMikuAddon.CATEGORY, "自动扔垃圾", "自动丢弃背包中的垃圾物品");
    }

    @Override
    public void onActivate() {
        updateTrashItems();
        tickTimer = 0;
        info("自动扔垃圾已启用，共 %d 种垃圾物品", allTrashItems.size());

    }

    @Override
    public void onDeactivate() {
        allTrashItems.clear();
        info("自动扔垃圾已禁用");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // 更新垃圾物品列表
        updateTrashItems();

        // 检查延迟
        if (tickTimer > 0) {
            tickTimer--;
            return;
        }

        // 查找并丢弃垃圾物品
        boolean itemDropped = false;

        // 遍历库存（排除快捷栏如果设置了的话）
        int startSlot = excludeHotbar.get() ? 9 : 0;

        for (int i = startSlot; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) continue;

            Item item = mc.player.getInventory().getStack(i).getItem();

            if (allTrashItems.contains(item)) {
                dropItem(i);
                itemDropped = true;

                // 记录丢弃信息
                String itemName = item.getName().toString();
                String groupName = getGroupNameForItem(item);
//                info("丢弃垃圾物品: %s (来自: %s)", itemName, groupName);

                break; // 一次只丢一种物品
            }
        }

        // 设置延迟
        if (itemDropped) {
            tickTimer = delay.get();
        }
    }

    private void updateTrashItems() {
        allTrashItems.clear();

        if (group1Enabled.get()) {
            allTrashItems.addAll(group1Items.get());
        }

        if (group2Enabled.get()) {
            allTrashItems.addAll(group2Items.get());
        }

        if (group3Enabled.get()) {
            allTrashItems.addAll(group3Items.get());
        }
    }

    private void dropItem(int slot) {
        if (mc.interactionManager == null) return;

        // 转换为屏幕槽位
        int screenSlot = SlotUtils.indexToId(slot);

        if (dropAll.get()) {
            // 丢弃整组物品（Ctrl + Q）
            mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                screenSlot,
                1, // 右键
                SlotActionType.THROW,
                mc.player
            );
        } else {
            // 只丢弃一个物品（Q）
            mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                screenSlot,
                0, // 左键
                SlotActionType.THROW,
                mc.player
            );
        }
    }

    private String getGroupNameForItem(Item item) {
        if (group1Enabled.get() && group1Items.get().contains(item)) {
            return group1Name.get();
        }
        if (group2Enabled.get() && group2Items.get().contains(item)) {
            return group2Name.get();
        }
        if (group3Enabled.get() && group3Items.get().contains(item)) {
            return group3Name.get();
        }
        return "未知组";
    }

    // 获取当前垃圾物品统计信息
    public String getTrashStats() {
        if (mc.player == null) return "无数据";

        Map<String, Integer> groupCounts = new HashMap<>();

        // 统计各组垃圾物品数量
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) continue;

            Item item = mc.player.getInventory().getStack(i).getItem();
            if (allTrashItems.contains(item)) {
                String groupName = getGroupNameForItem(item);
                int count = mc.player.getInventory().getStack(i).getCount();
                groupCounts.put(groupName, groupCounts.getOrDefault(groupName, 0) + count);
            }
        }

        if (groupCounts.isEmpty()) return "库存中无垃圾物品";

        StringBuilder stats = new StringBuilder("垃圾统计: ");
        for (Map.Entry<String, Integer> entry : groupCounts.entrySet()) {
            stats.append(entry.getKey()).append("(").append(entry.getValue()).append(") ");
        }

        return stats.toString().trim();
    }

    // 手动清理所有垃圾的方法
    public void clearAllTrash() {
        if (mc.player == null) return;

        updateTrashItems();
        int totalDropped = 0;

        for (int i = (excludeHotbar.get() ? 9 : 0); i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) continue;

            Item item = mc.player.getInventory().getStack(i).getItem();
            if (allTrashItems.contains(item)) {
                int count = mc.player.getInventory().getStack(i).getCount();
                dropItem(i);
                totalDropped += count;
            }
        }

        if (totalDropped > 0) {
            info("手动清理完成，共丢弃 %d 个垃圾物品", totalDropped);
        } else {
            info("库存中没有需要清理的垃圾物品");
        }
    }
}
