//package com.github.mikumiku.addon.modules;
//
//import com.github.mikumiku.addon.BaseModule;
//import com.github.mikumiku.addon.util.Via;
//import meteordevelopment.meteorclient.events.game.SendMessageEvent;
//import meteordevelopment.meteorclient.events.world.TickEvent;
//import meteordevelopment.meteorclient.settings.BoolSetting;
//import meteordevelopment.meteorclient.settings.IntSetting;
//import meteordevelopment.meteorclient.settings.Setting;
//import meteordevelopment.meteorclient.settings.SettingGroup;
//import meteordevelopment.meteorclient.utils.player.PlayerUtils;
//import meteordevelopment.orbit.EventHandler;
//import net.minecraft.entity.EquipmentSlot;
//import net.minecraft.entity.effect.StatusEffects;
//import net.minecraft.item.ItemStack;
//import net.minecraft.item.Items;
//import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
//import net.minecraft.screen.slot.SlotActionType;
//
///**
// * 无限耐久鞘翅模块
// * 通过在飞行期间自动切换鞘翅来防止耐久度消耗
// * <p>
// * 参考实现: ElytraExtra.java
// * 原始来源: https://github.com/etianl/Trouser-Streak/blob/1.21.4/src/main/java/pwn/noobs/trouserstreak/modules/InfiniteElytra.java
// */
//public class ElytraUnbreak2 extends BaseModule {
//    private final SettingGroup sgGeneral = settings.getDefaultGroup();
//
//    private final Setting<Integer> period = sgGeneral.add(new IntSetting.Builder()
//        .name("切换周期")
//        .description("鞘翅装备/卸下的切换周期（tick数）。")
//        .defaultValue(16)
//        .min(1)
//        .sliderRange(1, 100)
//        .build()
//    );
//
//    private final Setting<Boolean> doubleElytra = sgGeneral.add(new BoolSetting.Builder()
//        .name("双鞘翅无缝切换")
//        .description("准备2个鞘翅，来回切换。")
//        .defaultValue(true)
//        .build()
//    );
//
//    private final Setting<Boolean> autoRocket = sgGeneral.add(new BoolSetting.Builder()
//        .name("自动发射烟花")
//        .description("飞行时自动使用烟花火箭维持飞行。")
//        .defaultValue(false)
//        .build()
//    );
//
//    private final Setting<Integer> rocketDelay = sgGeneral.add(new IntSetting.Builder()
//        .name("烟花发射间隔")
//        .description("两次烟花发射之间的最小间隔（tick数）。")
//        .defaultValue(40)
//        .min(10)
//        .sliderRange(10, 100)
//        .build()
//    );
//
//    private final Setting<Boolean> antiKick = sgGeneral.add(new BoolSetting.Builder()
//        .name("防踢飞")
//        .description("在无法继续滑翔时发送跳跃包防止被踢。")
//        .defaultValue(true)
//        .build()
//    );
//
//    // 状态变量
//    private int tickCounter = 0;
//    private int globalTickCounter = 0;
//    private boolean wasFallFlying = false;
//    private int lastRocketTick = 0;
//    private boolean nextTickShouldStartFly = false;
//
//    public ElytraUnbreak2() {
//        super("无限耐久鞘翅", "通过自动切换鞘翅来防止耐久度消耗，并可选自动使用烟花火箭。");
//    }
//
//    @Override
//    public void onActivate() {
//        super.onActivate();
//        resetState();
//    }
//
//    @Override
//    public void onDeactivate() {
//        super.onDeactivate();
//        resetState();
//    }
//
//    private void resetState() {
//        tickCounter = 0;
//        globalTickCounter = 0;
//        wasFallFlying = false;
//        lastRocketTick = 0;
//        nextTickShouldStartFly = false;
//    }
//
//    @EventHandler
//    private void onTick(TickEvent.Pre event) {
//        if (mc.player == null || mc.world == null) return;
//
//        globalTickCounter++;
//        boolean isFlying = Via.isFallFlying(mc);
//
//        // 检测是否刚开始滑翔
//        if (isFlying && !wasFallFlying) {
//            tickCounter = 0;
//        }
//        wasFallFlying = isFlying;
//
//        if (!isFlying) {
//            // 不在滑翔时，检查是否需要自动装备鞘翅开始滑翔
//            if (nextTickShouldStartFly) {
//                nextTickShouldStartFly = false;
//                if (canContinueGliding() && !mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
//                    equipElytra();
//                    sendStartFallFlying();
//                }
//            }
//            return;
//        }
//
//        // 正在滑翔中
//        tickCounter++;
//
//        // 检查是否需要切换鞘翅
//        if (shouldSwitchElytra()) {
//            performElytraSwitch();
//        }
//
//        // 自动发射烟花
//        if (autoRocket.get() && canFireRocket()) {
//            launchRocket();
//        }
//    }
//
//    /**
//     * 检查是否应该切换鞘翅状态
//     */
//    private boolean shouldSwitchElytra() {
//        return tickCounter >= period.get();
//    }
//
////    /**
////     * 执行鞘翅切换操作
////     * 核心逻辑：卸下鞘翅 -> 下一tick重新装备 -> 发送开始滑翔包
////     */
////    private void performElytraSwitch() {
////        if (!canContinueGliding()) {
////            if (antiKick.get()) {
////                // 无法继续滑翔时，发送跳跃防止被踢
////                mc.player.jump();
////            }
////            return;
////        }
////
////        ItemStack chestStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);
////
////        // 当前装备的是鞘翅，需要卸下
////        if (chestStack.isOf(Items.ELYTRA)) {
////            unequipElytra();
////            // 标记下一tick需要重新装备并开始滑翔
////            nextTickShouldStartFly = true;
////        }
////
////        tickCounter = 0;
////    }
//
//    /**
//     * 执行鞘翅切换操作
//     * 核心逻辑：卸下鞘翅 -> 立即重新装备 -> 发送开始滑翔包（全部在同一tick完成）
//     */
//    private void performElytraSwitch() {
//        if (!canContinueGliding()) {
//            if (antiKick.get()) {
//                // 无法继续滑翔时，发送跳跃防止被踢
//                mc.player.jump();
//            }
//            return;
//        }
//
//        ItemStack chestStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);
//
//        // 当前装备的是鞘翅，需要切换到备用鞘翅
//        if (chestStack.isOf(Items.ELYTRA)) {
//            // 查找背包中的另一个鞘翅
//            int backupElytraSlot = findElytra();
//
//            if (backupElytraSlot != -1 && doubleElytra.get()) {
//                // 将备用鞘翅直接交换到胸甲槽（原子操作，立即生效）
//                swapToChestSlot(backupElytraSlot);
//
//                // 立即发送开始滑翔包以维持飞行状态
//                sendStartFallFlying();
//            } else {
//                // 没有备用鞘翅，使用原来的卸下-重装策略
//                unequipElytra();
//                nextTickShouldStartFly = true;
//            }
//        }
//
//        tickCounter = 0;
//    }
//
//
//    /**
//     * 装备鞘翅到胸甲槽
//     */
//    private void equipElytra() {
//        // 查找可用的鞘翅
//        int elytraSlot = findElytra();
//        if (elytraSlot == -1) return;
//        // 使用原子交换操作，立即生效
//        swapToChestSlot(elytraSlot);
//
//        // 如果鞘翅不在快捷栏，先移到快捷栏
////        if (elytraSlot >= 9) {
////            // 找一个空的快捷栏位置或非鞘翅位置
////            int targetSlot = findSwapSlot(elytraSlot);
////            if (targetSlot != -1) {
////                InvUtils.move().from(elytraSlot).to(targetSlot);
////                // 装备到胸甲槽
////                InvUtils.move().from(targetSlot).toArmor(2);
////            }
////        } else {
////            // 直接装备到胸甲槽
////            InvUtils.move().from(elytraSlot).toArmor(2);
////        }
//    }
//
//
//    public void switchSlotToArmor(int idx) {
//        int armorSlot = 6;   // 容器中的胸甲槽位索引
//        int targetSlot = idx; // 要交换的物品槽位（已经是容器坐标）// InvTasks.getScreenSlotByInventoryIndex(idx);
//        int syncId = mc.player.playerScreenHandler.syncId;
//        if (targetSlot >= 36 && targetSlot <= 45) {
//            // use number operation
//            // 情况1：物品在快捷栏或副手（容器坐标 36-45）
//            int target = (targetSlot < 45) ? targetSlot - 36 : 40;
//            mc.interactionManager.clickSlot(syncId, armorSlot, target, SlotActionType.SWAP, mc.player);
//        } else {
//            // fuck, do not kick me.
//
//            // 第1步：将目标物品与快捷栏位置40交换
//            mc.interactionManager.clickSlot(syncId, targetSlot, 40, SlotActionType.SWAP, mc.player);
//            // swap hotbar to armor, armor to hotbar
//            mc.interactionManager.clickSlot(syncId, armorSlot, 40, SlotActionType.SWAP, mc.player);
//            // swap the rest
//            mc.interactionManager.clickSlot(syncId, targetSlot, 40, SlotActionType.SWAP, mc.player);
//        }
//    }
//
//    /**
//     * 将指定槽位的物品直接交换到胸甲槽（使用底层API，立即生效）
//     *
//     * @param sourceSlot 源槽位（0-35，对应玩家背包）
//     */
//    private void swapToChestSlot(int sourceSlot) {
//        if (sourceSlot >= 46 - 9) {
//            return;
//        }
//        int i = sourceSlot;
//        // 使用 SlotActionType.SWAP 进行原子交换操作
//        // 胸甲槽在容器中的索引是 6（玩家容器的标准布局）
//        // 背包槽位需要加上 36 的偏移量来映射到容器槽位
//        mc.interactionManager.clickSlot(
//            mc.player.currentScreenHandler.syncId,
//            i < 9 ? i + 36 : i,  // 背包槽位转换为容器槽位
//            6,                 // 胸甲槽在容器中的索引
//            SlotActionType.SWAP,
//            mc.player
//        );
//    }
//
//
////    /**
////     * 卸下鞘翅到背包
////     */
////    private void unequipElytra() {
////        // 查找空的背包位置
////        int emptySlot = findEmptySlot();
////        if (emptySlot != -1) {
////            InvUtils.move().fromArmor(2).to(emptySlot);
////        }
////    }
//
//    /**
//     * 卸下鞘翅到背包
//     */
//    private void unequipElytra() {
//        // 查找空的背包位置
//        int emptySlot = findEmptySlot();
//        if (emptySlot != -1) {
//            if (emptySlot >= 46 - 9) {
//                return;
//            }
//
//            // 使用原子交换操作，立即生效
//            mc.interactionManager.clickSlot(
//                mc.player.currentScreenHandler.syncId,
//                emptySlot < 9 ? emptySlot + 36 : emptySlot,  // 目标背包槽位
//                6,               // 胸甲槽
//                SlotActionType.SWAP,
//                mc.player
//            );
//        }
//    }
//
//
//    /**
//     * 在背包中查找可用的鞘翅
//     */
//    private int findElytra() {
//
//        // 先搜索快捷栏（优先级高，交换更快）
//        for (int i = 0; i < 9; i++) {
//            ItemStack stack = mc.player.getInventory().getStack(i);
//            if (stack.isOf(Items.ELYTRA)) {
//                return i;
//            }
//        }
//
//        // 再搜索背包其他位置
//        for (int i = 9; i < 36; i++) {
//            ItemStack stack = mc.player.getInventory().getStack(i);
//            if (stack.isOf(Items.ELYTRA)) {
//                return i;
//            }
//        }
//        return -1;
//    }
//
//
//    /**
//     * 查找空的背包位置
//     */
//    private int findEmptySlot() {
//        for (int i = 0; i < 36; i++) {
//            if (mc.player.getInventory().getStack(i).isEmpty()) {
//                return i;
//            }
//        }
//        return -1;
//    }
//
//    /**
//     * 查找可以交换的快捷栏位置
//     */
//    private int findSwapSlot(int excludeSlot) {
//        // 优先找空位
//        for (int i = 0; i < 9; i++) {
//            if (mc.player.getInventory().getStack(i).isEmpty() && i != excludeSlot) {
//                return i;
//            }
//        }
//        // 找非鞘翅的位置
//        for (int i = 0; i < 9; i++) {
//            if (!mc.player.getInventory().getStack(i).isOf(Items.ELYTRA) && i != excludeSlot) {
//                return i;
//            }
//        }
//        return -1;
//    }
//
//    /**
//     * 发送开始滑翔数据包
//     */
//    private void sendStartFallFlying() {
//        if (mc.getNetworkHandler() != null) {
//            mc.getNetworkHandler().sendPacket(
//                new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING)
//            );
//        }
//    }
//
//    /**
//     * 检查是否可以继续滑翔
//     */
//    private boolean canContinueGliding() {
//        if (mc.player == null) return false;
//
//        // 基本条件检查
//        if (mc.player.isOnGround() || mc.player.getAbilities().flying || mc.player.hasVehicle()) {
//            return false;
//        }
//
//        // 液体检查
//        if (mc.player.isTouchingWater() || mc.player.isInLava()) {
//            return false;
//        }
//
//        // 效果检查
//        if (mc.player.hasStatusEffect(StatusEffects.LEVITATION)) {
//            return false;
//        }
//
//        return true;
//    }
//
//    /**
//     * 检查是否可以发射烟花
//     */
//    private boolean canFireRocket() {
//        if (!Via.isFallFlying(mc)) return false;
//        if (globalTickCounter - lastRocketTick < rocketDelay.get()) return false;
//
//        return findRocket() != null;
//    }
//
//    /**
//     * 查找可用的烟花火箭
//     * 搜索顺序：主手 -> 副手 -> 快捷栏
//     */
//    private ItemStack findRocket() {
//        // 检查主手
//        ItemStack mainHand = mc.player.getMainHandStack();
//        if (mainHand.isOf(Items.FIREWORK_ROCKET)) {
//            return mainHand;
//        }
//
//        // 检查副手
//        ItemStack offHand = mc.player.getOffHandStack();
//        if (offHand.isOf(Items.FIREWORK_ROCKET)) {
//            return offHand;
//        }
//
//        // 检查快捷栏
//        for (int i = 0; i < 9; i++) {
//            ItemStack stack = mc.player.getInventory().getStack(i);
//            if (stack.isOf(Items.FIREWORK_ROCKET)) {
//                return stack;
//            }
//        }
//
//        return null;
//    }
//
//    /**
//     * 发射烟花火箭
//     */
//    private void launchRocket() {
//        ItemStack rocket = findRocket();
//        if (rocket == null) return;
//
//        int currentSlot = mc.player.getInventory().selectedSlot;
//
//        // 如果烟花在主手，直接使用
//        if (rocket == mc.player.getMainHandStack()) {
//            mc.interactionManager.interactItem(mc.player, net.minecraft.util.Hand.MAIN_HAND);
//        }
//        // 如果烟花在副手，直接使用
//        else if (rocket == mc.player.getOffHandStack()) {
//            mc.interactionManager.interactItem(mc.player, net.minecraft.util.Hand.OFF_HAND);
//        }
//        // 如果烟花在快捷栏其他位置，切换过去使用
//        else {
//            for (int i = 0; i < 9; i++) {
//                if (mc.player.getInventory().getStack(i) == rocket) {
//                    Via.setSelectedSlot(i);
//                    mc.interactionManager.interactItem(mc.player, net.minecraft.util.Hand.MAIN_HAND);
//                    Via.setSelectedSlot(currentSlot);
//                    break;
//                }
//            }
//        }
//
//        lastRocketTick = globalTickCounter;
//    }
//}
