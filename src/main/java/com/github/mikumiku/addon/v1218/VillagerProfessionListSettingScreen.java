
package com.github.mikumiku.addon.v1218;

import com.github.mikumiku.addon.util.VillagerProfessionListSetting;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.settings.base.CollectionListSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.registry.Registries;
import net.minecraft.village.VillagerProfession;

import java.util.function.Predicate;

public class VillagerProfessionListSettingScreen extends CollectionListSettingScreen<VillagerProfession> {
    public VillagerProfessionListSettingScreen(GuiTheme theme, VillagerProfessionListSetting setting) {
        super(theme, "选择", setting, setting.get(), Registries.VILLAGER_PROFESSION);
    }

    @Override
    protected boolean includeValue(VillagerProfession value) {
        Predicate<VillagerProfession> filter = ((VillagerProfessionListSetting) setting).filter;
        if (filter != null && !filter.test(value)) return false;


        VillagerProfession profession = Registries.VILLAGER_PROFESSION.get(VillagerProfession.NONE);
        return value != profession;
    }

    @Override
    protected WWidget getValueWidget(VillagerProfession profession) {

        String key = "entity.minecraft.villager." + profession.id();
        String translated = I18n.translate(key);
        return theme.label(translated);
    }

    @Override
    protected String[] getValueNames(VillagerProfession profession) {

        return new String[]{profession.id().getString(), profession.id().toString()};
    }

}
