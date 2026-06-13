package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.MikuUtil;
import com.github.mikumiku.addon.util.Via;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.*;
import java.util.Map.Entry;

public class EntityList extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Set<EntityType<?>>> allEntitys = sgGeneral.add(
        new Builder()
            .name("通用实体")
            .description("所有世界显示的实体")
            .defaultValue(new EntityType[]{EntityType.EXPERIENCE_ORB, EntityType.ENDER_PEARL, EntityType.ZOMBIFIED_PIGLIN})
            .build()
    );

    public final Setting<Set<EntityType<?>>> entitys = sgGeneral.add(
        new Builder()
            .name("主世界实体")
            .description("仅在主世界显示的实体")
            .defaultValue(new EntityType[]{EntityType.EXPERIENCE_ORB, EntityType.ENDER_PEARL, EntityType.ZOMBIFIED_PIGLIN})
            .build()
    );

    public final Setting<Set<EntityType<?>>> netherEntitys = sgGeneral.add(
        new Builder()
            .name("下界实体")
            .description("仅在下界显示的实体")
            .defaultValue(new EntityType[]{
                EntityType.EXPERIENCE_ORB, EntityType.COW, EntityType.SHEEP, EntityType.PIG,
                EntityType.HORSE, EntityType.ZOMBIE, EntityType.CREEPER, EntityType.BOGGED,
                EntityType.HUSK, EntityType.SLIME, EntityType.VILLAGER, EntityType.SPIDER,
                EntityType.CAVE_SPIDER, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER
            })
            .build()
    );

    public final Setting<SettingColor> entitysColor = sgGeneral.add(
        new ColorSetting.Builder()
            .name("实体颜色")
            .defaultValue(new SettingColor(138, 180, 248, 255)) // #8AB4F8 - 现代蓝色
            .build()
    );

    public final Setting<SettingColor> playerColor = sgGeneral.add(
        new ColorSetting.Builder()
            .name("玩家颜色")
            .defaultValue(new SettingColor(129, 201, 149, 255)) // #81C995 - 现代绿色
            .build()
    );

    private final SettingGroup itemGroup = settings.createGroup("物品");

    public final Setting<List<Item>> items1 = itemGroup.add(
        new ItemListSetting.Builder()
            .name("物品1")
            .defaultValue(new Item[]{
                Items.ELYTRA, Items.WHITE_SHULKER_BOX, Items.ORANGE_SHULKER_BOX,
                Items.MAGENTA_SHULKER_BOX, Items.LIGHT_BLUE_SHULKER_BOX, Items.YELLOW_SHULKER_BOX,
                Items.LIME_SHULKER_BOX, Items.PINK_SHULKER_BOX, Items.GRAY_SHULKER_BOX,
                Items.LIGHT_GRAY_SHULKER_BOX, Items.CYAN_SHULKER_BOX, Items.PURPLE_SHULKER_BOX,
                Items.BLUE_SHULKER_BOX, Items.BROWN_SHULKER_BOX, Items.GREEN_SHULKER_BOX,
                Items.RED_SHULKER_BOX, Items.BLACK_SHULKER_BOX, Items.BUNDLE,
                Items.ANCIENT_DEBRIS, Items.NETHERITE_SCRAP, Items.NETHERITE_INGOT,
                Items.NETHERITE_BLOCK, Items.NETHERITE_SWORD, Items.NETHERITE_AXE,
                Items.NETHERITE_HOE, Items.NETHERITE_PICKAXE, Items.NETHERITE_SHOVEL,
                Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS,
                Items.NETHERITE_BOOTS
            })
            .build()
    );

    public final Setting<SettingColor> items1Color = itemGroup.add(
        new ColorSetting.Builder()
            .name("物品1颜色")
            .defaultValue(new SettingColor(255, 107, 107, 255))
            .build()
    );

    public final Setting<List<Item>> items2 = itemGroup.add(
        new ItemListSetting.Builder()
            .name("物品2")
            .build()
    );

    public final Setting<SettingColor> items2Color = itemGroup.add(
        new ColorSetting.Builder()
            .name("物品2颜色")
            .defaultValue(new SettingColor(78, 205, 196, 255))
            .build()
    );

    //物品1: #FF6B6B - 柔和珊瑚红（替代刺眼的纯红）
    //物品2: #4ECDC4 - 现代青绿色（替代过亮的青色）
    //默认: #FFC14D - 温暖琥珀黄（替代纯黄色）
    //实体: #8AB4F8 - 现代蓝色（替代洋红色）
    //玩家: #81C995 - 现代绿色
    public final Setting<SettingColor> itemsColor = itemGroup.add(
        new ColorSetting.Builder()
            .name("物品默认颜色")
            .defaultValue(new SettingColor(255, 193, 77, 255))
            .build()
    );

    public final Setting<List<Item>> blackList = itemGroup.add(
        new ItemListSetting.Builder()
            .name("黑名单")
            .build()
    );

    private final SettingGroup ui = settings.createGroup("界面");

    public final Setting<Integer> xOffset = ui.add(
        new IntSetting.Builder()
            .name("X偏移")
            .min(0)
            .sliderMax(2048)
            .defaultValue(20)
            .build()
    );

    public final Setting<Integer> yOffset = ui.add(
        new IntSetting.Builder()
            .name("Y偏移")
            .min(0)
            .sliderMax(2048)
            .defaultValue(500)
            .build()
    );

    public final Setting<Integer> lineHeight = ui.add(
        new IntSetting.Builder()
            .name("行高")
            .min(0)
            .sliderMax(100)
            .defaultValue(20)
            .build()
    );

    public final Setting<Double> scale = ui.add(
        new DoubleSetting.Builder()
            .name("字体大小")
            .min(0.0)
            .sliderMax(6.0)
            .defaultValue(1.0)
            .build()
    );

    public EntityList() {
        super("实体列表", "显示实体、玩家、凋落物列表.");
    }

    @Override
    public void onActivate() {
        super.onActivate();
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!Utils.isLoading() && isActive()) {
            Set<Item> items1Set = new HashSet<>(items1.get());
            Set<Item> items2Set = new HashSet<>(items2.get());
            Set<Item> blackListSet = new HashSet<>(blackList.get());
            Map<Item, Integer> items1Map = new HashMap<>();
            Map<Item, Integer> items2Map = new HashMap<>();
            Map<Item, Integer> itemsMap = new HashMap<>();
            Map<EntityType<?>, Integer> entitysMap = new HashMap<>();
            Map<String, Double> playersMap = new HashMap<>();
            Map<String, String> playerArmorMap = new HashMap<>();
            RegistryKey<World> registryKey = Via.getEntityWorld(mc.player).getRegistryKey();

            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof ItemEntity itemEntity) {
                    ItemStack stack = itemEntity.getStack();
                    Item item = stack.getItem();

                    if (!blackListSet.contains(item)) {
                        Map<Item, Integer> map;
                        if (items1Set.contains(item)) {
                            map = items1Map;
                        } else if (items2Set.contains(item)) {
                            map = items2Map;
                        } else {
                            map = itemsMap;
                        }

                        int count = map.containsKey(item) ? stack.getCount() + map.get(item) : stack.getCount();
                        map.put(item, count);
                    }
                } else if (entity instanceof PlayerEntity player && entity != mc.player) {
                    double distance = mc.player.distanceTo(player);
                    String playerName = Via.getGameProfileName(player);
                    String armorSetName = getArmorSetName(player);
                    playersMap.put(playerName, distance);
                    playerArmorMap.put(playerName, armorSetName);
                } else {
                    EntityType<?> entityType = entity.getType();
                    Set<EntityType<?>> entityTypes;
                    Set<EntityType<?>> allEntitysSet = allEntitys.get();

                    // 首先检查是否在通用实体列表中
                    if (allEntitysSet.contains(entityType)) {
                        int qty = entitysMap.getOrDefault(entityType, 0);
                        entitysMap.put(entityType, qty + 1);
                        continue;
                    }

                    // 如果不在通用列表中，再根据维度检查
                    if (registryKey == ServerWorld.NETHER) {
                        entityTypes = netherEntitys.get();
                    } else if (registryKey == ServerWorld.OVERWORLD) {
                        entityTypes = entitys.get();
                    } else {
                        entityTypes = Collections.emptySet();
                    }

                    if (entityTypes.contains(entityType)) {
                        int qty = entitysMap.getOrDefault(entityType, 0);
                        entitysMap.put(entityType, qty + 1);
                    }
                }
            }

            int y = yOffset.get();
            y = drawPlayer(playersMap, playerArmorMap, y, playerColor.get());
            y = draw(items1Map, y, items1Color.get());
            y = draw(items2Map, y, items2Color.get());
            y = draw(itemsMap, y, itemsColor.get());
            y = drawEntity(entitysMap, y, entitysColor.get());
        }
    }

    private int draw(Map<Item, Integer> grayMap, int y, Color color) {
        if (grayMap.isEmpty()) {
            return y;
        }
        TextRenderer textRenderer = TextRenderer.get();
        int x = xOffset.get();

        for (Entry<Item, Integer> entry : grayMap.entrySet()) {
            String text = String.format("[%s] x %s", Names.get(entry.getKey()), entry.getValue());
            textRenderer.begin(scale.get());
            textRenderer.render(text, x, y, color, true);
            textRenderer.end();
            y += (int) (lineHeight.get() * scale.get());
        }

        return y;
    }

    private int drawEntity(Map<EntityType<?>, Integer> grayMap, int y, Color color) {
        TextRenderer textRenderer = TextRenderer.get();
        int x = xOffset.get();

        for (Entry<EntityType<?>, Integer> entry : grayMap.entrySet()) {
            String text = String.format("[%s] x %s", Names.get(entry.getKey()), entry.getValue());
            textRenderer.begin(scale.get());
            textRenderer.render(text, x, y, color, true);
            textRenderer.end();
            y += (int) (lineHeight.get() * scale.get());
        }

        return y;
    }

    private int drawPlayer(Map<String, Double> playersMap, Map<String, String> playerArmorMap, int y, Color color) {
        TextRenderer textRenderer = TextRenderer.get();
        int x = xOffset.get();

        for (Entry<String, Double> entry : playersMap.entrySet()) {

            String name = entry.getKey();
            String armor = playerArmorMap.get(name);
            String text = String.format("%s [%s][%.1fm]", name, armor, entry.getValue());
            textRenderer.begin(scale.get());
            textRenderer.render(text, x, y, color, true);
            textRenderer.end();
            y += (int) (lineHeight.get() * scale.get());
        }

        return y;
    }


//    private ItemStack getItem(PlayerEntity entity, int index) {
//        return switch (index) {
//            case 0 -> entity.getMainHandStack();
//            case 1 -> entity.getInventory().armor.get(3); // 头盔
//            case 2 -> entity.getInventory().armor.get(2); // 胸甲
//            case 3 -> entity.getInventory().armor.get(1); // 护腿
//            case 4 -> entity.getInventory().armor.get(0); // 靴子
//            case 5 -> entity.getOffHandStack();
//            default -> ItemStack.EMPTY;
//        };
//    }

    private String getArmorSetName(PlayerEntity player) {
        // 获取四个护甲槽
        ItemStack head = player.getEquippedStack(EquipmentSlot.HEAD);
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack legs = player.getEquippedStack(EquipmentSlot.LEGS);
        ItemStack feet = player.getEquippedStack(EquipmentSlot.FEET);

        // 全为空
        if (head.isEmpty() && chest.isEmpty() && legs.isEmpty() && feet.isEmpty()) {
            return "裸吊";
        }

        // 记录每种材质的数量
        Map<String, Integer> typeCount = new HashMap<>();
        ItemStack[] items = {head, chest, legs, feet};

        for (ItemStack item : items) {
            if (MikuUtil.isArmor(item.getItem())) {
                String matId = Registries.ITEM.getId(item.getItem()).toString();

                // 转中文标签
                String type;
                if (matId.contains("netherite")) type = "合金";
                else if (matId.contains("diamond")) type = "钻石";
                else if (matId.contains("iron")) type = "铁";
                else if (matId.contains("gold")) type = "金";
                else if (matId.contains("chain")) type = "锁链";
                else if (matId.contains("leather")) type = "皮革";
                else if (matId.contains("turtle")) type = "海龟";
                else if (matId.contains("armadillo")) type = "犰狳";
                else type = matId; // 支持模组护甲

                typeCount.merge(type, 1, Integer::sum);
            }
        }

        // 没有任何护甲
        if (typeCount.isEmpty()) return "裸吊";

        // 找出现最多的材质
        String mainType = null;
        int maxCount = 0;
        for (Map.Entry<String, Integer> e : typeCount.entrySet()) {
            if (e.getValue() > maxCount) {
                mainType = e.getKey();
                maxCount = e.getValue();
            }
        }

        // 判断是否为混合套
        if (typeCount.size() == 1) {
            return mainType + "套";
        } else {
            return mainType + "套" + "(混)";
        }
    }

//    public static Text getArmorSetDisplayName(ItemStack armor) {
//        if (!(armor.getItem() instanceof ArmorItem armorItem)) return Text.literal("Unknown");
//
//        RegistryEntry<ArmorMaterial> material = armorItem.getMaterial();
//        ArmorMaterial material = armorItem.getComponents().get(DataComponentTypes.ma);
//
//        String name;
//        if (material.matches(ArmorMaterials.LEATHER)) name = "Leather";
//        else if (material.matches(ArmorMaterials.CHAIN)) name = "Chainmail";
//        else if (material.matches(ArmorMaterials.IRON)) name = "Iron";
//        else if (material.matches(ArmorMaterials.GOLD)) name = "Gold";
//        else if (material.matches(ArmorMaterials.DIAMOND)) name = "Diamond";
//        else if (material.matches(ArmorMaterials.TURTLE)) name = "Turtle";
//        else if (material.matches(ArmorMaterials.NETHERITE)) name = "Netherite";
//        else if (material.matches(ArmorMaterials.ARMADILLO)) name = "Armadillo";
//        else name = material.getIdAsString();
//
//        return Text.literal(name);
//    }
//
//    public static String getArmorSetName(ItemStack armor) {
//        if (!(armor.getItem() instanceof ArmorItem armorItem)) return "Unknown";
//
//        RegistryEntry<ArmorMaterial> material = armorItem.getMaterial();
//
//        if (material.matches(ArmorMaterials.LEATHER)) {
//            return "Leather";
//        } else if (material.matches(ArmorMaterials.CHAIN)) {
//            return "Chainmail";
//        } else if (material.matches(ArmorMaterials.IRON)) {
//            return "Iron";
//        } else if (material.matches(ArmorMaterials.GOLD)) {
//            return "Gold";
//        } else if (material.matches(ArmorMaterials.DIAMOND)) {
//            return "Diamond";
//        }   else if (material.matches(ArmorMaterials.NETHERITE)) {
//            return "Netherite";
//        }   else {
//            // 输出原始注册ID，方便调试自定义材质
//            return material.getIdAsString();
//        }
//    }

}
