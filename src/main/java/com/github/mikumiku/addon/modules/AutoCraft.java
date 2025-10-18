package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.item.Item;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Arrays;
import java.util.List;

public class AutoCraft extends BaseModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
        .name("物品")
        .description("想要自动合成的物品列表")
        .defaultValue(Arrays.asList())
        .build()
    );

    private final Setting<Boolean> antiDesync = sgGeneral.add(new BoolSetting.Builder()
        .name("防不同步")
        .description("尝试防止物品栏不同步")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> craftAll = sgGeneral.add(new BoolSetting.Builder()
        .name("全部合成")
        .description("每次合成最大可能数量（Shift点击）")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> drop = sgGeneral.add(new BoolSetting.Builder()
        .name("丢弃物品")
        .description("自动丢弃合成物品（背包空间不足时有用）")
        .defaultValue(true)
        .build()
    );

    public AutoCraft() {
        super("喷射合成", "自动合成物品");
    }

    @Override
    public void onActivate() {
        super.onActivate();

    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.interactionManager == null) return;
        if (items.get().isEmpty()) return;

        if (!(mc.player.currentScreenHandler instanceof CraftingScreenHandler)) return;


        if (antiDesync.get())
            mc.player.getInventory().updateItems();

        CraftingScreenHandler currentScreenHandler = (CraftingScreenHandler) mc.player.currentScreenHandler;
        List<Item> itemList = items.get();
        List<RecipeResultCollection> recipeResultCollectionList = mc.player.getRecipeBook().getOrderedResults();
//        for (RecipeResultCollection recipeResultCollection : recipeResultCollectionList) {
//            for (RecipeEntry<?> recipe : recipeResultCollection.getRecipes(true)) {
//                if (!itemList.contains(recipe.value().getResult(mc.world.getRegistryManager()).getItem())) {
//                    continue;
//                }
//
//                mc.interactionManager.clickRecipe(currentScreenHandler.syncId, recipe, craftAll.get());
//                mc.interactionManager.clickSlot(currentScreenHandler.syncId, 0, 1,
//                    drop.get() ? SlotActionType.THROW : SlotActionType.QUICK_MOVE, mc.player);
//            }
//        }
    }
}
