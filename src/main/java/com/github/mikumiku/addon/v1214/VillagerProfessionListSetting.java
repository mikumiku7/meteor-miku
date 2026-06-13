package com.github.mikumiku.addon.v1214;

import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.village.VillagerProfession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class VillagerProfessionListSetting extends Setting<List<VillagerProfession>> {
    public final Predicate<VillagerProfession> filter;
    private final boolean bypassFilterWhenSavingAndLoading;

    public VillagerProfessionListSetting(String name, String description, List<VillagerProfession> defaultValue,
                                         Consumer<List<VillagerProfession>> onChanged,
                                         Consumer<Setting<List<VillagerProfession>>> onModuleActivated,
                                         IVisible visible,
                                         Predicate<VillagerProfession> filter,
                                         boolean bypassFilterWhenSavingAndLoading) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.filter = filter;
        this.bypassFilterWhenSavingAndLoading = bypassFilterWhenSavingAndLoading;
    }

    @Override
    protected List<VillagerProfession> parseImpl(String str) {
        String[] values = str.split(",");
        List<VillagerProfession> professions = new ArrayList<>(values.length);

        try {
            for (String value : values) {
                VillagerProfession profession = parseId(Registries.VILLAGER_PROFESSION, value);
                if (profession != null && (filter == null || filter.test(profession))) {
                    professions.add(profession);
                }
            }
        } catch (Exception ignored) {}

        return professions;
    }

    @Override
    public void resetImpl() {
        value = new ArrayList<>(defaultValue);
    }

    @Override
    protected boolean isValueValid(List<VillagerProfession> value) {
        return true;
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registries.VILLAGER_PROFESSION.getIds();
    }

    @Override
    public NbtCompound save(NbtCompound tag) {
        NbtList valueTag = new NbtList();
        for (VillagerProfession profession : get()) {
            if (bypassFilterWhenSavingAndLoading || (filter == null || filter.test(profession))) {
                valueTag.add(NbtString.of(Registries.VILLAGER_PROFESSION.getId(profession).toString()));
            }
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<VillagerProfession> load(NbtCompound tag) {
        get().clear();

        NbtList valueTag = tag.getList("value", 8);
        for (NbtElement tagI : valueTag) {
            VillagerProfession profession = Registries.VILLAGER_PROFESSION.get(Identifier.of(tagI.asString()));

            if (bypassFilterWhenSavingAndLoading || (filter == null || filter.test(profession))) {
                get().add(profession);
            }
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, List<VillagerProfession>, VillagerProfessionListSetting> {
        private Predicate<VillagerProfession> filter;
        private boolean bypassFilterWhenSavingAndLoading;

        public Builder() {
            super(new ArrayList<>(0));
        }

        public Builder defaultValue(VillagerProfession... defaults) {
            return defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
        }

        public Builder filter(Predicate<VillagerProfession> filter) {
            this.filter = filter;
            return this;
        }

        public Builder bypassFilterWhenSavingAndLoading() {
            this.bypassFilterWhenSavingAndLoading = true;
            return this;
        }

        @Override
        public VillagerProfessionListSetting build() {
            return new VillagerProfessionListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, filter, bypassFilterWhenSavingAndLoading);
        }
    }
}
