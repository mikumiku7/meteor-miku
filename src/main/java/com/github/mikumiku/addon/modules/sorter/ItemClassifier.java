package com.github.mikumiku.addon.modules.sorter;


import com.github.mikumiku.addon.util.MikuUtil;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 物品分类器 - 负责将物品分类到不同的类别中
 */
public class ItemClassifier {

    public Map<String, List<Item>> customCategories = new HashMap<>();
    public boolean enableSmartClassification = true;


    public ItemClassifier(Map<String, List<Item>> cate, boolean enableSmartClassification) {
        this.customCategories = cate;
        this.enableSmartClassification = enableSmartClassification;
    }

    /**
     * 获取物品的分类
     *
     * @param itemStack 物品堆栈
     * @return 分类名称
     */
    public String classifyItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return "unknown_item";
        }
        String itemName = MikuUtil.getItemName(itemStack);

        // 1. 首先检查用户自定义分类
        for (Map.Entry<String, List<Item>> entry : customCategories.entrySet()) {
            if (entry.getValue().contains(itemStack.getItem())) {
                return entry.getKey();
            }
        }

        // 2. 如果启用智能分类，基于物品属性进行分类
        if (enableSmartClassification) {
            String smartCategory = classifyByAttributes(itemName, itemStack);
            if (smartCategory != null) {
                return smartCategory;
            }
        }

        // 3. 缺省分类：物品名称本身就是分类
        return itemName;
    }

    /**
     * 基于物品属性进行智能分类
     */
    private String classifyByAttributes(String itemName, ItemStack stack) {

        // 潜影盒特殊处理
        if (isShulkerBox(itemName)) {
            String result = classifyShulkerBox(stack);
            return result != null ? result : "shulker_box";
        }

        // 附魔书特殊处理
        if ("enchanted_book".equals(itemName)) {
            return "enchanted_books";
        }

        // 超稀有物品（生存可获得）
        if (isUltraRare(itemName)) {
            return "ultra_rare";
        }

        // 音乐唱片
        if (itemName.startsWith("music_disc_")) {
            return "music_discs";
        }

        // 陶片
        if (itemName.endsWith("_pottery_sherd")) {
            return "pottery_sherds";
        }

        // 药水
        if (isPotion(itemName)) {
            return "potions";
        }

        // 垃圾物品
        if (isTrashItem(itemName)) {
            return "trash";
        }

        // 颜色变种物品检查
        String colorVariant = getColorVariantCategory(itemName);
        if (colorVariant != null) {
            return colorVariant;
        }

        // 缺省分类：如果没有找到任何分类，物品名称本身就是分类
        return itemName; // 物品自己单独作为一个分类
    }

    private boolean isUltraRare(String itemName) {
        return itemName.equals("elytra") || itemName.equals("totem_of_undying") ||
            itemName.equals("nether_star") || itemName.equals("beacon") || itemName.equals("conduit") ||
            itemName.equals("dragon_head"); // 龙蛋是创造模式物品，移除
    }

    /**
     * 获取颜色变种物品的分类
     */
    private String getColorVariantCategory(String itemName) {
        // 羊毛
        if (itemName.endsWith("_wool")) {
            return "wool";
        }

        // 混凝土
        if (itemName.endsWith("_concrete")) {
            return "concrete";
        }

        // 陶瓦
        if (itemName.endsWith("_terracotta") || itemName.equals("terracotta")) {
            return "terracotta";
        }

        // 玻璃
        if (itemName.endsWith("_glass") || itemName.equals("glass") || itemName.equals("tinted_glass")) {
            return "glass";
        }

        // 地毯
        if (itemName.endsWith("_carpet")) {
            return "carpet";
        }

        // 床
        if (itemName.endsWith("_bed")) {
            return "bed";
        }

        return null;
    }


    private boolean isPotion(String itemName) {
        return itemName.contains("potion") || itemName.equals("glass_bottle") ||
            itemName.equals("dragon_breath") || itemName.equals("experience_bottle") ||
            itemName.equals("ominous_bottle");
    }

    private boolean isTrashItem(String itemName) {
        return itemName.equals("rotten_flesh") || itemName.equals("spider_eye") ||
            itemName.equals("fermented_spider_eye") || itemName.equals("bone") ||
            itemName.equals("string") || itemName.equals("feather") ||
            itemName.equals("leather") || itemName.equals("rabbit_hide") || itemName.equals("slime_ball") ||
            itemName.equals("stick") || itemName.equals("phantom_membrane");
    }

    /**
     * 是否有附魔
     */
    public boolean isEnchanted(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.hasEnchantments();
    }

    /**
     * 是否潜影盒
     */
    private boolean isShulkerBox(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        return blockItem.getBlock() instanceof ShulkerBoxBlock;
    }


    /**
     * 检查物品是否为潜影盒
     */
    private boolean isShulkerBox(String itemName) {

        return itemName.contains(Items.SHULKER_BOX.getName().getString());
    }

    /**
     * 对潜影盒进行特殊分类处理
     * 通过分析盒内物品，找出数量最多的物品类型作为该潜影盒的分类
     */
    private String classifyShulkerBox(ItemStack shulkerBoxStack) {
        if (shulkerBoxStack == null || shulkerBoxStack.isEmpty()) {
            return null;
        }

        try {
            ContainerComponent container = shulkerBoxStack.get(DataComponentTypes.CONTAINER);
            if (container == null) {
                return null;
            }

            // 获取容器内的物品列表
            List<ItemStack> containerItems = container.stream().toList();

            if (containerItems == null || containerItems.isEmpty()) {
                return null;
            }

            // 统计每种物品类型的数量
            Map<String, Integer> itemTypeCounts = new HashMap<>();

            for (ItemStack item : containerItems) {
                if (item == null || item.isEmpty()) continue;

                String itemName = MikuUtil.getItemName(item);

                // 递归分类盒内物品（避免无限递归，潜影盒内的潜影盒直接按名称分类）
                String itemType = classifyByAttributes(itemName, item);

                int count = item.getCount();
                itemTypeCounts.put(itemType, itemTypeCounts.getOrDefault(itemType, 0) + count);
            }


            // 找出数量最多的物品类型
            return itemTypeCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        } catch (Exception e) {
            // 如果解析失败，返回null让其按默认逻辑处理
            return null;
        }
    }
}
