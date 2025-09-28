//package com.github.mikumiku.addon.modules;
//
//import com.github.mikumiku.addon.BaseModule;
//import com.github.mikumiku.addon.util.BagUtil;
//import kassuk.addon.blackout.enums.RotationType;
//import kassuk.addon.blackout.enums.SwingState;
//import kassuk.addon.blackout.enums.SwingType;
//import kassuk.addon.blackout.managers.Managers;
//import kassuk.addon.blackout.utils.BOInvUtils;
//import kassuk.addon.blackout.utils.RotationUtils;
//import kassuk.addon.blackout.utils.SettingUtils;
//import meteordevelopment.meteorclient.events.render.Render3DEvent;
//import meteordevelopment.meteorclient.settings.*;
//import meteordevelopment.meteorclient.systems.friends.Friends;
//import meteordevelopment.meteorclient.utils.entity.DamageUtils;
//import meteordevelopment.meteorclient.utils.player.InvUtils;
//import meteordevelopment.meteorclient.utils.player.Rotations;
//import meteordevelopment.orbit.EventHandler;
//import net.minecraft.entity.player.PlayerEntity;
//import net.minecraft.item.AxeItem;
//import net.minecraft.item.ItemStack;
//import net.minecraft.item.SwordItem;
//import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
//import net.minecraft.util.Hand;
//
//import java.util.Objects;
//
///**
// * @author OLEPOSSU
// */
//
//public class KillAuraPlus extends BaseModule {
//    public KillAuraPlus() {
//        super("杀戮光环+", "超级强力的杀敌光环，专门为水晶PVP打造");
//    }
//
//    private final SettingGroup sgGeneral = settings.getDefaultGroup();
//
//    private final Setting<TargetMode> targetMode = sgGeneral.add(new EnumSetting.Builder<TargetMode>()
//        .name("目标选择")
//        .description("锁定哪种敌人")
//        .defaultValue(TargetMode.Health)
//        .build()
//    );
//    private final Setting<Integer> maxHp = sgGeneral.add(new IntSetting.Builder()
//        .name("最大血量")
//        .description("目标的血量必须低于这个值")
//        .defaultValue(36)
//        .min(0)
//        .sliderMax(36)
//        .build()
//    );
//    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
//        .name("攻击间隔")
//        .description("每次攻击的间隔时间")
//        .defaultValue(0.500)
//        .min(0)
//        .sliderMax(1)
//        .build()
//    );
//    private final Setting<RotationMode> rotationMode = sgGeneral.add(new EnumSetting.Builder<RotationMode>()
//        .name("转头模式")
//        .description("什么时候转头")
//        .defaultValue(RotationMode.OnHit)
//        .build()
//    );
//    private final Setting<SwitchMode> switchMode = sgGeneral.add(new EnumSetting.Builder<SwitchMode>()
//        .name("切换模式")
//        .description("建议关闭")
//        .defaultValue(SwitchMode.Disabled)
//        .build()
//    );
//    private final Setting<Boolean> onlyWeapon = sgGeneral.add(new BoolSetting.Builder()
//        .name("只用武器")
//        .description("只用武器攻击")
//        .defaultValue(true)
//        .build()
//    );
//    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
//        .name("抬手")
//        .description("攻击时显示抬手动画")
//        .defaultValue(true)
//        .build()
//    );
//
//    private double timer = 0;
//    private PlayerEntity target = null;
//
//    @EventHandler
//    private void onRender(Render3DEvent event) {
//        timer = Math.min(delay.get(), timer + event.frameTime);
//
//        updateTarget();
//
//        if (target == null) {
//            return;
//        }
//
//        boolean switched = false;
//        switch (switchMode.get()) {
//            case Disabled ->
//                switched = !onlyWeapon.get() || mc.player.getMainHandStack().getItem() instanceof SwordItem || mc.player.getMainHandStack().getItem() instanceof AxeItem;
//            case Normal -> {
//                int slot = bestSlot(false);
//                if (slot >= 0) {
//                    InvUtils.swap(slot, true);
//                    switched = true;
//                }
//            }
//            case InvSwitch, PickSwitch, Silent -> switched = true;
//        }
//
//        if (!switched) {
//            return;
//        }
//
//        boolean rotated = rotationMode.get() != RotationMode.Constant ||
//            !SettingUtils.shouldRotate(RotationType.Attacking) ||
//            Managers.ROTATION.start(target.getBoundingBox(), priority, RotationType.Attacking, Objects.hash(name + "attacking"));
//
//        if (!rotated || timer < delay.get()) {
//            return;
//        }
//
//        rotated = rotationMode.get() != RotationMode.OnHit || !SettingUtils.shouldRotate(RotationType.Attacking) || Managers.ROTATION.start(target.getBoundingBox(), priority, RotationType.Attacking, Objects.hash(name + "attacking"));
//
//        if (!rotated) {
//            return;
//        }
//
//
//        switch (switchMode.get()) {
//            case Silent -> {
//                switched = false;
//                int slot = bestSlot(false);
//                if (slot >= 0) {
//                    InvUtils.swap(slot, true);
//                    switched = true;
//                }
//            }
//            case PickSwitch -> {
//                switched = false;
//                int slot = bestSlot(true);
//                if (slot >= 0) {
//                    switched = true;
//                    BagUtil.switchToSlot(slot);
//                }
//            }
//            case InvSwitch -> {
//                switched = false;
//                int slot = bestSlot(true);
//                if (slot >= 0) {
//                    switched = true;
//                    BagUtil.switchToSlot(slot);
//                }
//            }
//        }
//
//        if (!switched) {
//            return;
//        }
//
//        attackTarget();
//
//        switch (switchMode.get()) {
//            case Silent -> InvUtils.swapBack();
//            case InvSwitch -> InvUtils.swapBack();
//            case PickSwitch -> InvUtils.swapBack();
//        }
//
////        if (rotationMode.get() == RotationMode.OnHit) {
////            Managers.ROTATION.end(Objects.hash(name + "attacking"));
////        }
//    }
//
//    private void attackTarget() {
//        timer = 0;
//
//        SettingUtils.swing(SwingState.Pre, SwingType.Attacking, Hand.MAIN_HAND);
//
//        sendSequencedPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
//
//        SettingUtils.swing(SwingState.Post, SwingType.Attacking, Hand.MAIN_HAND);
//        if (swing.get()) clientSwing(swingHand.get(), Hand.MAIN_HAND);
//    }
//
//    private int bestSlot(boolean inventory) {
//        int slot = -1;
//        double hDmg = -1;
//        double dmg;
//        for (int i = 0; i < (inventory ? mc.player.getInventory().size() + 1 : 9); i++) {
//            ItemStack stack = mc.player.getInventory().getStack(i);
//            if (onlyWeapon.get() && !(stack.getItem() instanceof SwordItem) && !(stack.getItem() instanceof AxeItem)) {
//                continue;
//            }
//
//            dmg = DamageUtils.getAttackDamage(mc.player, target, stack);
//            if (dmg > hDmg) {
//                slot = i;
//                hDmg = dmg;
//            }
//        }
//        return slot;
//    }
//
//    private void updateTarget() {
//        double value = 0;
//        target = null;
//
//        mc.world.getPlayers().forEach(player -> {
//            if (player.getHealth() <= 0 || player.isSpectator() || player.getHealth() + player.getAbsorptionAmount() > maxHp.get() || !SettingUtils.inAttackRange(player.getBoundingBox()) || player == mc.player || Friends.get().isFriend(player)) {
//                return;
//            }
//
//            double val = switch (targetMode.get()) {
//                case Health -> 10000 - player.getHealth() - player.getAbsorptionAmount();
//                case Angle -> 10000 - Math.abs(RotationUtils.yawAngle(mc.player.getYaw(), Rotations.getYaw(player)));
//                case Distance -> 10000 - mc.player.getPos().distanceTo(player.getPos());
//            };
//            if (val > value) {
//                target = player;
//            }
//        });
//    }
//
//    public enum TargetMode {
//        Health,
//        Angle,
//        Distance
//    }
//
//    public enum RotationMode {
//        OnHit,
//        Constant
//    }
//
//    public enum SwitchMode {
//        Disabled,
//        Normal,
//        Silent,
//        PickSwitch,
//        InvSwitch
//    }
//}
