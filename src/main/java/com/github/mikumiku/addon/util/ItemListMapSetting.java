package com.github.mikumiku.addon.util;

import com.github.mikumiku.addon.util.Via;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.screens.settings.ItemListSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ItemListMapSetting extends Setting<Map<String, List<Item>>> {
    public final Predicate<Item> filter;
    private final boolean bypassFilterWhenSavingAndLoading;

    public ItemListMapSetting(String name, String description, Map<String, List<Item>> defaultValue,
                              Consumer<Map<String, List<Item>>> onChanged,
                              Consumer<Setting<Map<String, List<Item>>>> onModuleActivated,
                              IVisible visible, Predicate<Item> filter, boolean bypassFilterWhenSavingAndLoading) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
        this.filter = filter;
        this.bypassFilterWhenSavingAndLoading = bypassFilterWhenSavingAndLoading;
    }

    @Override
    protected Map<String, List<Item>> parseImpl(String str) {
        // 格式: key1:item1,item2;key2:item3,item4
        String[] entries = str.split(";");
        Map<String, List<Item>> map = new LinkedHashMap<>();

        try {
            for (String entry : entries) {
                String[] parts = entry.split(":", 2);
                if (parts.length != 2) continue;

                String key = parts[0].trim();
                String[] itemNames = parts[1].split(",");
                List<Item> items = new ArrayList<>();

                for (String itemName : itemNames) {
                    Item item = (Item) parseId(Registries.ITEM, itemName.trim());
                    if (item != null && (filter == null || filter.test(item))) {
                        items.add(item);
                    }
                }

                if (!items.isEmpty()) {
                    map.put(key, items);
                }
            }
        } catch (Exception ignored) {
        }

        return map;
    }

    @Override
    protected boolean isValueValid(Map<String, List<Item>> value) {
        return true;
    }

    @Override
    protected void resetImpl() {
        value = new LinkedHashMap<>();
        for (Map.Entry<String, List<Item>> entry : defaultValue.entrySet()) {
            value.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registries.ITEM.getIds();
    }

    @Override
    protected NbtCompound save(NbtCompound tag) {
        NbtCompound mapTag = new NbtCompound();

        for (Map.Entry<String, List<Item>> entry : get().entrySet()) {
            NbtList itemList = new NbtList();
            for (Item item : entry.getValue()) {
                if (bypassFilterWhenSavingAndLoading || filter == null || filter.test(item)) {
                    itemList.add(NbtString.of(Registries.ITEM.getId(item).toString()));
                }
            }
            if (!itemList.isEmpty()) {
                mapTag.put(entry.getKey(), itemList);
            }
        }

        tag.put("value", mapTag);
        return tag;
    }


    @Override
    protected Map<String, List<Item>> load(NbtCompound tag) {
        get().clear();

        NbtCompound mapTag = Via.getNbtCompound(tag, "value");
        for (String key : mapTag.getKeys()) {
            List<Item> items = new ArrayList<>();
            NbtList itemList = mapTag.getList(key, NbtElement.STRING_TYPE);

            for (NbtElement tagI : itemList) {
                Item item = (Item) Registries.ITEM.get(Identifier.of(tagI.asString()));
                if (bypassFilterWhenSavingAndLoading || filter == null || filter.test(item)) {
                    items.add(item);
                }
            }

            if (!items.isEmpty()) {
                get().put(key, items);
            }
        }

        return get();
    }

    public static void fillTable(GuiTheme theme, WTable table, ItemListMapSetting setting) {
        table.clear();

        Map<String, List<Item>> map = setting.get();

        for (String key : map.keySet()) {
            AtomicReference<String> keyRef = new AtomicReference<>(key);

            // Key textbox
            WTextBox keyBox = table.add(theme.textBox(keyRef.get())).minWidth(100).expandX().widget();
            keyBox.actionOnUnfocused = () -> {
                String newKey = keyBox.get();
                if (map.containsKey(newKey) && !newKey.equals(keyRef.get())) {
                    keyBox.set(keyRef.get());
                    return;
                }
                List<Item> items = map.remove(keyRef.get());
                keyRef.set(newKey);
                map.put(newKey, items);
            };

            // Items display (simplified - you may want to add item selector GUI)
            List<Item> items = map.get(keyRef.get());
            // "Select" button
            WButton selectBtn = table.add(theme.button("Select")).minWidth(100).expandX().widget();

            // Item count label
            String labelText = "(" + items.size() + " items)";
            table.add(theme.label(labelText)).expandX().widget();
            selectBtn.action = () -> {
                List<Item> itemsList = map.get(keyRef.get());

                ItemListSetting tempSetting = new ItemListSetting(
                    "items",
                    "Items for key '" + keyRef.get() + "'",
                    itemsList,
                    newValue -> {
                        map.put(keyRef.get(), newValue);
                        fillTable(theme, table, setting);
                    },
                    null,
                    null,
                    setting.filter,
                    true
                );

                ItemListSettingScreen screen = new ItemListSettingScreen(theme, tempSetting);
                screen.onClosed(() -> map.put(keyRef.get(), tempSetting.get()));

                MinecraftClient.getInstance().setScreen(screen);
            };


            // Delete entry button
            WMinus delete = table.add(theme.minus()).widget();
            delete.action = () -> {
                map.remove(keyRef.get());
                fillTable(theme, table, setting);
            };

            table.row();
        }

        if (!map.isEmpty()) {
            table.add(theme.horizontalSeparator()).expandX();
            table.row();
        }

        // Reset button
        WButton reset = table.add(theme.button(GuiRenderer.RESET)).widget();
        reset.action = () -> {
            setting.reset();
            fillTable(theme, table, setting);
        };

        // Add new entry button
        WPlus add = table.add(theme.plus()).widget();
        add.action = () -> {
            map.put("", new ArrayList<>());
            fillTable(theme, table, setting);
        };

        table.row();
    }

    public static class Builder extends SettingBuilder<Builder, Map<String, List<Item>>, ItemListMapSetting> {
        private Predicate<Item> filter;
        private boolean bypassFilterWhenSavingAndLoading;

        public Builder() {
            super(new LinkedHashMap<>(0));
        }

        public Builder defaultValue(Map<String, List<Item>> map) {
            this.defaultValue = new LinkedHashMap<>();
            for (Map.Entry<String, List<Item>> entry : map.entrySet()) {
                this.defaultValue.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            return this;
        }

        public Builder filter(Predicate<Item> filter) {
            this.filter = filter;
            return this;
        }

        public Builder bypassFilterWhenSavingAndLoading() {
            this.bypassFilterWhenSavingAndLoading = true;
            return this;
        }

        @Override
        public ItemListMapSetting build() {
            return new ItemListMapSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, filter, bypassFilterWhenSavingAndLoading);
        }
    }
}
