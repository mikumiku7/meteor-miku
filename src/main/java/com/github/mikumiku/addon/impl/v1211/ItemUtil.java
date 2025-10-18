package com.github.mikumiku.addon.impl.v1211;

import net.minecraft.item.*;

public class ItemUtil implements com.github.mikumiku.addon.util.ItemUtil {
    @Override
    public boolean isArmorItem(Item item) {
        return item instanceof ArmorItem;
    }

    @Override
    public boolean isPickaxeItem(Item item) {
        return item instanceof PickaxeItem;
    }

    @Override
    public boolean isToolOrWeapon(Item item) {
        return item instanceof PickaxeItem || item instanceof AxeItem || item instanceof ShovelItem || item instanceof SwordItem;
    }
}
