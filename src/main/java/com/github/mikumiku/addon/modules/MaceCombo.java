package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.BagUtil;
import com.github.mikumiku.addon.util.MikuUtil;
import com.github.mikumiku.addon.util.Via;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class MaceCombo extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPower = settings.createGroup("威力增强");

    public enum WeaponType {
        SWORD("剑"),
        AXE("斧头"),
        HAND("空手"),
        ANY("任意武器");

        private final String name;

        WeaponType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // 通用设置
    private final Setting<WeaponType> weaponType = sgGeneral.add(new EnumSetting.Builder<WeaponType>()
        .name("武器类型")
        .description("触发切换的武器类型")
        .defaultValue(WeaponType.SWORD)
        .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("自动切换")
        .description("使用指定武器攻击时自动切换到锤子")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> breachOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("仅破甲锤")
        .description("只切换到有破甲附魔的锤子")
        .defaultValue(true)
        .visible(autoSwitch::get)
        .build()
    );

    private final Setting<Integer> switchDelay = sgGeneral.add(new IntSetting.Builder()
        .name("切回延迟")
        .description("攻击后多少tick切回剑")
        .defaultValue(1)
        .min(0)
        .max(5)
        .sliderMax(5)
        .visible(autoSwitch::get)
        .build()
    );

    // 威力增强设置
    private final Setting<Boolean> macePower = sgPower.add(new BoolSetting.Builder()
        .name("威力增强")
        .description("使用锤子时增强伤害")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> elytraOnly = sgPower.add(new BoolSetting.Builder()
        .name("仅鞘翅时")
        .description("只在鞘翅飞行时增强威力")
        .defaultValue(true)
        .visible(macePower::get)
        .build()
    );


    private final Setting<Boolean> maxPower = sgPower.add(new BoolSetting.Builder()
        .name("最大威力")
        .description("自动寻找最大可用高度")
        .defaultValue(false)
        .visible(macePower::get)
        .build()
    );

    private final Setting<Integer> fallHeight = sgPower.add(new IntSetting.Builder()
        .name("下落高度")
        .description("模拟的下落高度")
        .defaultValue(22)
        .min(1)
        .max(50)
        .sliderMax(50)
        .visible(() -> macePower.get() && !maxPower.get())
        .build()
    );


    private final Setting<Boolean> checkTarget = sgPower.add(new BoolSetting.Builder()
        .name("检查目标")
        .description("不对创造模式、无敌、格挡的玩家使用")
        .defaultValue(true)
        .visible(macePower::get)
        .build()
    );

    private boolean wasGliding = false;
    private int originalSlot = -1;
    private int switchBackTicks = 0;

    public MaceCombo() {
        super(BaseModule.CATEGORY_MIKU_COMBAT, "切锤增伤", "武器锤连击. 使用指定武器攻击时自动切锤增伤");
    }

    @Override
    public void onDeactivate() {
        if (originalSlot != -1 && mc.player != null) {
            BagUtil.swap(originalSlot, false);
            originalSlot = -1;
        }
        switchBackTicks = 0;
        wasGliding = false;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onAttack(AttackEntityEvent event) {
        if (mc.player == null || mc.world == null) return;

        Entity target = event.entity;
        if (target == null || !target.isAlive() || !(target instanceof LivingEntity)) return;

        wasGliding = Via.isFallFlying(mc);

        // 自动切换逻辑
        if (autoSwitch.get() && isWeaponTypeMatched()) {
            FindItemResult mace = findMace();
            if (mace.found()) {
                originalSlot = Via.getSelectedSlot();
                BagUtil.swap(mace.slot(), false);
                switchBackTicks = switchDelay.get();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onAttackAfter(AttackEntityEvent event) {
        if (mc.player == null || mc.world == null) return;

        Entity target = event.entity;
        if (!(target instanceof LivingEntity living)) return;

        // 威力增强逻辑
        if (macePower.get() && mc.player.getMainHandStack().getItem() instanceof MaceItem) {
            if (!elytraOnly.get() || Via.isFallFlying(mc)) {
                if (!shouldSkipTarget(living)) {
                    applyMacePower();
                }
            }
        }

        // 恢复鞘翅飞行状态
        if (wasGliding && !Via.isFallFlying(mc)) {
            mc.player.networkHandler.sendPacket(
                new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING)
            );
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        // 处理切回剑
        if (switchBackTicks > 0) {
            switchBackTicks--;
            if (switchBackTicks == 0 && originalSlot != -1) {
                BagUtil.swap(originalSlot, false);
                originalSlot = -1;
            }
        }

        // 持续保持鞘翅状态
        if (wasGliding && !Via.isFallFlying(mc)) {
            mc.player.networkHandler.sendPacket(
                new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING)
            );
            wasGliding = false;
        }
    }

    private boolean isWeaponTypeMatched() {
        Item item = mc.player.getMainHandStack().getItem();
        switch (weaponType.get()) {
            case SWORD:
                return MikuUtil.isArmor(item);
            case AXE:
                return item instanceof AxeItem;
            case HAND:
                return mc.player.getMainHandStack().isEmpty();
            case ANY:
                return true; // 任意武器都触发切换
            default:
                return false;
        }
    }

    private FindItemResult findMace() {
        if (breachOnly.get()) {
            return BagUtil.findInHotbar(stack -> isMaceWithBreach(stack));
        } else {
            return BagUtil.findInHotbar(stack -> stack.getItem() instanceof MaceItem);
        }
    }

    private boolean isMaceWithBreach(ItemStack stack) {
        if (!(stack.getItem() instanceof MaceItem)) return false;

        ItemEnchantmentsComponent enchants = stack.getOrDefault(
            DataComponentTypes.ENCHANTMENTS,
            ItemEnchantmentsComponent.DEFAULT
        );

        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchants.getEnchantmentEntries()) {
            RegistryEntry<?> enchant = entry.getKey();
            if (enchant.getKey().isPresent()) {
                Identifier id = enchant.getKey().get().getValue();
                if (id.getPath().equals("breach")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean shouldSkipTarget(LivingEntity target) {
        if (!checkTarget.get()) return false;

        if (target instanceof PlayerEntity player) {
            return player.isCreative() || player.isBlocking();
        }
        return target.isInvulnerable();
    }

    private void applyMacePower() {
        try {
            Vec3d originalPos = Via.getEntityPos(mc.player);
            int height = getOptimalHeight();

            if (height <= 0) return;

            BlockPos checkPos1 = mc.player.getBlockPos().add(0, height, 0);
            BlockPos checkPos2 = checkPos1.up();

            if (!isSafeBlock(checkPos1) || !isSafeBlock(checkPos2)) return;


            applyPower(originalPos, height);
        } catch (Exception ignored) {
        }
    }


    private void applyPower(Vec3d originalPos, int height) {
        int packets = Math.min((int) Math.ceil(height / 10.0), 20);

        for (int i = 0; i < Math.max(4, packets - 1); i++) {
            mc.player.networkHandler.sendPacket(
                Via.getOnGroundOnly(false)
            );
        }

        double targetY = mc.player.getY() + Math.min(height, fallHeight.get());
        mc.player.networkHandler.sendPacket(Via.getPositionAndOnGround(
                mc.player.getX(), targetY, mc.player.getZ(),
                false
            )
        );

        mc.player.networkHandler.sendPacket(
            Via.getPositionAndOnGround(
                originalPos.x, originalPos.y, originalPos.z,
                false
            )
        );
    }

    private int getOptimalHeight() {
        if (!maxPower.get()) {
            return fallHeight.get();
        }

        BlockPos playerPos = mc.player.getBlockPos();
        int maxSearch = playerPos.getY() + 170;

        for (int y = maxSearch; y > playerPos.getY(); y--) {
            BlockPos check1 = new BlockPos(playerPos.getX(), y, playerPos.getZ());
            BlockPos check2 = check1.up();
            if (isSafeBlock(check1) && isSafeBlock(check2)) {
                return y - playerPos.getY();
            }
        }
        return 0;
    }

    private boolean isSafeBlock(BlockPos pos) {
        return mc.world.getBlockState(pos).isReplaceable() &&
            mc.world.getFluidState(pos).isEmpty() &&
            !mc.world.getBlockState(pos).isOf(Blocks.POWDER_SNOW);
    }
}
