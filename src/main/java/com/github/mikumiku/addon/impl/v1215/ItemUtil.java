package com.github.mikumiku.addon.impl.v1215;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.item.*;
import net.minecraft.registry.tag.BlockTags;

import java.util.Objects;

public class ItemUtil implements com.github.mikumiku.addon.util.ItemUtil {
    @Override
    public boolean isArmorItem(Item item) {
        return item.getComponents().contains(DataComponentTypes.EQUIPPABLE);
    }

    @Override
    public boolean isPickaxeItem(Item item) {
        ToolComponent toolComponent = item.getComponents().get(DataComponentTypes.TOOL);
        return Objects.nonNull(toolComponent) && toolComponent.rules().stream()
            .anyMatch(e -> e.blocks().stream().anyMatch(b -> b.isIn(BlockTags.PICKAXE_MINEABLE)));

    }

    @Override
    public boolean isToolOrWeapon(Item item) {
        return item.getComponents().contains(DataComponentTypes.TOOL) || item.getComponents().contains(DataComponentTypes.WEAPON);
    }
}
