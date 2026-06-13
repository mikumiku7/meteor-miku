package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.BagUtil;
import net.minecraft.item.Items;

public class OneKeyPearl extends BaseModule {
    public OneKeyPearl() {
        super(BaseModule.CATEGORY_MIKU_COMBAT, "一键珍珠", "一键珍珠");
    }

    @Override
    public void onActivate() {

        BagUtil.quickUse(Items.ENDER_PEARL);
        this.toggle();
    }
}
