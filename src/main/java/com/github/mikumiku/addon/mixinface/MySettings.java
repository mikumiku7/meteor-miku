package com.github.mikumiku.addon.mixinface;

import com.github.mikumiku.addon.util.ItemListMapSetting;
import com.github.mikumiku.addon.util.StringMapSetting;
import com.github.mikumiku.addon.util.VillagerProfessionListSetting;
import com.github.mikumiku.addon.util.VillagerProfessionListSettingScreen;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorLabel;
import meteordevelopment.meteorclient.gui.utils.SettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.Setting;

import java.util.Collection;
import java.util.Map;

public class MySettings {
    private final Map<Class<?>, SettingsWidgetFactory.Factory> factories;

    private final GuiTheme theme;

    public MySettings(Map<Class<?>, SettingsWidgetFactory.Factory> factories, GuiTheme theme) {
        this.factories = factories;
        this.theme = theme;
    }

    public void addSettings() {
        factories.put(StringMapSetting.class, (table, setting) -> stringMapW(table, (StringMapSetting) setting));
        factories.put(ItemListMapSetting.class, (table, setting) -> itemListMapW(table, (ItemListMapSetting) setting));
        factories.put(VillagerProfessionListSetting.class, (table, setting) -> proListW(table, (VillagerProfessionListSetting) setting));
    }

    public void stringMapW(WTable table, StringMapSetting setting) {
        WTable wtable = table.add(theme.table()).expandX().widget();
        StringMapSetting.fillTable(theme, wtable, setting);
    }

    public void itemListMapW(WTable table, ItemListMapSetting setting) {
        WTable wtable = table.add(theme.table()).expandX().widget();
        ItemListMapSetting.fillTable(theme, wtable, setting);
    }

    public void proListW(WTable table, VillagerProfessionListSetting setting) {
        selectW(table, setting, () -> MeteorClient.mc.setScreen(new VillagerProfessionListSettingScreen(theme, setting)));
    }

    public void selectW(WContainer c, Setting<?> setting, Runnable action) {
        boolean addCount = WSelectedCountLabel.getSize(setting) != -1;
        WContainer c2 = c;
        if (addCount) {
            c2 = c.add(this.theme.horizontalList()).expandCellX().widget();
            ((WHorizontalList) c2).spacing *= 2.0F;
        }

        WButton button = c2.add(this.theme.button("选择")).expandCellX().widget();
        button.action = action;
        if (addCount) {
            c2.add((new WSelectedCountLabel(setting)).color(this.theme.textSecondaryColor()));
        }

        reset(c, setting, null);
    }

    private void reset(WContainer c, Setting<?> setting, Runnable action) {
        WButton reset = c.add(this.theme.button(GuiRenderer.RESET)).widget();
        reset.action = () -> {
            setting.reset();
            if (action != null) {
                action.run();
            }

        };
    }


    public static class WSelectedCountLabel extends WMeteorLabel {
        private final Setting<?> setting;
        private int lastSize = -1;

        public WSelectedCountLabel(Setting<?> setting) {
            super("", false);
            this.setting = setting;
        }

        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            int size = getSize(this.setting);
            if (size != this.lastSize) {
                this.set("(" + size + " selected)");
                this.lastSize = size;
            }

            super.onRender(renderer, mouseX, mouseY, delta);
        }

        public static int getSize(Setting<?> setting) {
            Object var2 = setting.get();
            if (var2 instanceof Collection<?> collection) {
                return collection.size();
            } else {
                var2 = setting.get();
                if (var2 instanceof Map<?, ?> map) {
                    return map.size();
                } else {
                    return -1;
                }
            }
        }
    }
}
