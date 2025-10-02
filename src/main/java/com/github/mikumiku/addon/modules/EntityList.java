package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.*;
import java.util.Map.Entry;

public class EntityList extends BaseModule {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    public final Setting<Set<EntityType<?>>> entitys = this.sgGeneral
        .add(
            new Builder().name("主世界实体").description("仅在主世界显示的实体")
                .defaultValue(new EntityType[]{EntityType.EXPERIENCE_ORB, EntityType.ZOMBIFIED_PIGLIN})
                .build()
        );
    public final Setting<Set<EntityType<?>>> netherEntitys = this.sgGeneral
        .add(
            new Builder().name("下界实体").description("仅在下界显示的实体")
                .defaultValue(
                    new EntityType[]{
                        EntityType.EXPERIENCE_ORB,
                        EntityType.COW,
                        EntityType.SHEEP,
                        EntityType.PIG,
                        EntityType.HORSE,
                        EntityType.ZOMBIE,
                        EntityType.CREEPER,
                        EntityType.BOGGED,
                        EntityType.HUSK,
                        EntityType.SLIME,
                        EntityType.VILLAGER,
                        EntityType.SPIDER,
                        EntityType.CAVE_SPIDER,
                        EntityType.DROWNED,
                        EntityType.ZOMBIE_VILLAGER
                    }
                )
                .build()
        );
    public final Setting<SettingColor> entitysColor = this.sgGeneral
        .add(
            new ColorSetting.Builder().name("实体颜色")
                .defaultValue(Color.MAGENTA)
                .build()
        );

    private final SettingGroup itemGroup = this.settings.createGroup("物品");
    public final Setting<List<Item>> items1 = this.itemGroup
        .add(
            new ItemListSetting.Builder().name("物品1")
                .defaultValue(
                    new Item[]{
                        Items.ELYTRA,
                        Items.WHITE_SHULKER_BOX,
                        Items.ORANGE_SHULKER_BOX,
                        Items.MAGENTA_SHULKER_BOX,
                        Items.LIGHT_BLUE_SHULKER_BOX,
                        Items.YELLOW_SHULKER_BOX,
                        Items.LIME_SHULKER_BOX,
                        Items.PINK_SHULKER_BOX,
                        Items.GRAY_SHULKER_BOX,
                        Items.LIGHT_GRAY_SHULKER_BOX,
                        Items.CYAN_SHULKER_BOX,
                        Items.PURPLE_SHULKER_BOX,
                        Items.BLUE_SHULKER_BOX,
                        Items.BROWN_SHULKER_BOX,
                        Items.GREEN_SHULKER_BOX,
                        Items.RED_SHULKER_BOX,
                        Items.BLACK_SHULKER_BOX,
                        Items.BUNDLE,
                        Items.ANCIENT_DEBRIS,
                        Items.NETHERITE_SCRAP,
                        Items.NETHERITE_INGOT,
                        Items.NETHERITE_BLOCK,
                        Items.NETHERITE_SWORD,
                        Items.NETHERITE_AXE,
                        Items.NETHERITE_HOE,
                        Items.NETHERITE_PICKAXE,
                        Items.NETHERITE_SHOVEL,
                        Items.NETHERITE_HELMET,
                        Items.NETHERITE_CHESTPLATE,
                        Items.NETHERITE_LEGGINGS,
                        Items.NETHERITE_BOOTS
                    }
                )
                .build()
        );
    public final Setting<SettingColor> items1Color = this.itemGroup
        .add(
            new ColorSetting.Builder().name("物品1颜色")
                .defaultValue(Color.RED)
                .build()
        );
    public final Setting<Boolean> item1Log = this.itemGroup
        .add(
            new BoolSetting.Builder()
                .name("物品1日志")
                .defaultValue(false)
                .build()
        );
    public final Setting<List<Item>> items2 = this.itemGroup
        .add(
            new ItemListSetting.Builder().name("物品2")
                .build()
        );
    public final Setting<SettingColor> items2Color = this.itemGroup
        .add(
            new ColorSetting.Builder().name("物品2颜色")
                .defaultValue(Color.CYAN)
                .build()
        );
    public final Setting<SettingColor> itemsColor = this.itemGroup
        .add(
            new ColorSetting.Builder().name("物品默认颜色")
                .defaultValue(Color.YELLOW)
                .build()
        );
    public final Setting<List<Item>> blackList = this.itemGroup
        .add(
            new ItemListSetting.Builder().name("黑名单")
                .build()
        );
    private final SettingGroup ui = this.settings.createGroup("界面");
    public final Setting<Integer> xOffset = this.ui
        .add(
            new IntSetting.Builder()
                .name("X偏移")
                .min(0)
                .sliderMax(2048)
                .defaultValue(20)
                .build()
        );
    public final Setting<Integer> yOffset = this.ui
        .add(
            new IntSetting.Builder()
                .name("Y偏移")
                .min(0)
                .sliderMax(2048)
                .defaultValue(500)
                .build()
        );
    public final Setting<Integer> lineHeight = this.ui
        .add(
            new IntSetting.Builder()
                .name("行高")
                .min(0)
                .sliderMax(100)
                .defaultValue(20)
                .build()
        );
    public final Setting<Double> scale = this.ui
        .add(
            new DoubleSetting.Builder().name("字体大小")
                .min(0.0)
                .sliderMax(6.0)
                .defaultValue(1.0)
                .build()
        );

    public EntityList() {
        super("实体列表", "显示实体列表. 来自某违反协议的lotos");
    }

    @Override
    public void onActivate() {
        super.onActivate();
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!Utils.isLoading()) {
            if (this.isActive()) {
                Set<Item> items1Set = new HashSet<>(this.items1.get());
                Set<Item> items2Set = new HashSet<>(this.items2.get());
                Set<Item> blackListSet = new HashSet<>(this.blackList.get());
                Map<Item, Integer> items1Map = new HashMap<>();
                Map<Item, Integer> items2Map = new HashMap<>();
                Map<Item, Integer> itemsMap = new HashMap<>();
                Map<EntityType<?>, Integer> entitysMap = new HashMap<>();
                RegistryKey<World> registryKey = this.mc.player.getWorld().getRegistryKey();

                for (Entity entity : this.mc.world.getEntities()) {
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
                    } else {
                        EntityType<?> entityType = entity.getType();
                        Set<EntityType<?>> entityTypes;
                        if (registryKey == ServerWorld.NETHER) {
                            entityTypes = this.netherEntitys.get();
                        } else if (registryKey == ServerWorld.OVERWORLD) {
                            entityTypes = this.entitys.get();
                        } else {
                            entityTypes = Collections.emptySet();
                        }

                        if (entityTypes.contains(entityType)) {
                            Integer qty = entitysMap.getOrDefault(entityType, 0);
                            entitysMap.put(entityType, qty + 1);

                        }
                    }
                }

                int y = this.yOffset.get();
                y = this.draw(items1Map, y, this.items1Color.get());
                y = this.draw(items2Map, y, this.items2Color.get());
                y = this.draw(itemsMap, y, this.itemsColor.get());
                y = this.draw2(entitysMap, y, this.entitysColor.get());
            }
        }
    }

    private int draw(Map<Item, Integer> grayMap, int y, Color color) {
        if (grayMap.isEmpty()) {
            return y;
        } else {
            TextRenderer textRenderer = TextRenderer.get();
            Integer x = this.xOffset.get();

            for (Entry<Item, Integer> entry : grayMap.entrySet()) {
                Item item = entry.getKey();
                Integer count = entry.getValue();
                String name = Names.get(item);
                String text = String.format("[%s] x %s", name, count);
                textRenderer.begin(this.scale.get());
                textRenderer.render(text, x.intValue(), y, color, true);
                textRenderer.end();
                y = (int) (y + this.lineHeight.get().intValue() * this.scale.get());
            }

            return y;
        }
    }

    private int draw2(Map<EntityType<?>, Integer> grayMap, int y, Color color) {
        TextRenderer textRenderer = TextRenderer.get();
        Integer x = this.xOffset.get();

        for (Entry<EntityType<?>, Integer> entry : grayMap.entrySet()) {
            EntityType<?> item = entry.getKey();
            Integer count = entry.getValue();
            String name = Names.get(item);
            String text = String.format("[%s] x %s", name, count);
            textRenderer.begin(this.scale.get());
            textRenderer.render(text, x.intValue(), y, color, true);
            textRenderer.end();
            y = (int) (y + this.lineHeight.get().intValue() * this.scale.get());
        }

        return y;
    }
}
