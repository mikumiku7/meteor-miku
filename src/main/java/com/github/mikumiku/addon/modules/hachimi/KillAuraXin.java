//package com.github.mikumiku.addon.modules.hachimi;
//
//import com.github.mikumiku.addon.BaseModule;
//import com.github.mikumiku.addon.util.BaritoneUtil;
//import meteordevelopment.meteorclient.events.render.Render3DEvent;
//import meteordevelopment.meteorclient.events.world.TickEvent;
//import meteordevelopment.meteorclient.renderer.ShapeMode;
//import meteordevelopment.meteorclient.settings.*;
//import meteordevelopment.meteorclient.utils.entity.EntityUtils;
//import meteordevelopment.meteorclient.utils.entity.SortPriority;
//import meteordevelopment.meteorclient.utils.entity.TargetUtils;
//import meteordevelopment.meteorclient.utils.player.Rotations;
//import meteordevelopment.meteorclient.utils.render.color.SettingColor;
//import meteordevelopment.orbit.EventHandler;
//import net.minecraft.entity.Entity;
//import net.minecraft.entity.LivingEntity;
//import net.minecraft.entity.passive.PassiveEntity;
//import net.minecraft.entity.player.PlayerEntity;
//import net.minecraft.item.SwordItem;
//import net.minecraft.util.Hand;
//import net.minecraft.util.math.Box;
//
//public class KillAuraXin extends BaseModule {
//    public KillAuraXin() {
//        super("Kill Aura", "Automatically attacks entities that meet specified requirements.");
//    }
//
//    private final SettingGroup sgGeneral = settings.getDefaultGroup();
//    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
//    private final SettingGroup sgRotation = settings.createGroup("Rotation");
//    private final SettingGroup sgRender = settings.createGroup("Render");
//
//    // General Settings
//    public final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
//        .name("range")
//        .description("Attack range in blocks.")
//        .defaultValue(6.0)
//        .min(0.0)
//        .max(12.0)
//        .build());
//
//    public final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
//        .name("speed")
//        .description("Attack speed multiplier.")
//        .defaultValue(1.0)
//        .min(0.1)
//        .max(20.0)
//        .build());
//
//    public final Setting<AttackMode> attackMode = sgGeneral.add(new EnumSetting.Builder<AttackMode>()
//        .name("attack-mode")
//        .description("How to attack the target.")
//        .defaultValue(AttackMode.Mainhand)
//        .build());
//
//    public final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
//        .name("auto-switch")
//        .description("Automatically switch to sword.")
//        .defaultValue(false)
//        .build());
//
//    public final Setting<Integer> ticksExisted = sgGeneral.add(new IntSetting.Builder()
//        .name("ticks-existed")
//        .description("Minimum ticks an entity must exist for.")
//        .defaultValue(50)
//        .min(0)
//        .max(240)
//        .build());
//
//    // Targeting Settings
//    public final Setting<Boolean> players = sgTargeting.add(new BoolSetting.Builder()
//        .name("players")
//        .description("Attack players.")
//        .defaultValue(true)
//        .build());
//
//    public final Setting<Boolean> animals = sgTargeting.add(new BoolSetting.Builder()
//        .name("animals")
//        .description("Attack animals.")
//        .defaultValue(false)
//        .build());
//
//    public final Setting<Boolean> hostiles = sgTargeting.add(new BoolSetting.Builder()
//        .name("hostiles")
//        .description("Attack hostile mobs.")
//        .defaultValue(false)
//        .build());
//
//    public final Setting<Boolean> passives = sgTargeting.add(new BoolSetting.Builder()
//        .name("passives")
//        .description("Attack passive mobs.")
//        .defaultValue(false)
//        .build());
//
//    public final Setting<Boolean> invisibles = sgTargeting.add(new BoolSetting.Builder()
//        .name("invisibles")
//        .description("Attack invisible entities.")
//        .defaultValue(false)
//        .build());
//
//    // Rotation Settings
//    public final Setting<RotationMode> rotationMode = sgRotation.add(new EnumSetting.Builder<RotationMode>()
//        .name("rotation")
//        .description("How to rotate to target.")
//        .defaultValue(RotationMode.None)
//        .build());
//
//    public final Setting<Boolean> raytrace = sgRotation.add(new BoolSetting.Builder()
//        .name("raytrace")
//        .description("Only attack entities you can see.")
//        .defaultValue(false)
//        .build());
//
//    // Render Settings
//    public final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
//        .name("shape-mode")
//        .description("Rendering mode.")
//        .defaultValue(ShapeMode.Both)
//        .build());
//
//
//    public final Setting<SettingColor> fillColor = sgRender.add(
//        new ColorSetting.Builder()
//            .name("fill-color")
//            .defaultValue(new SettingColor(255, 107, 107, 50))
//            .build()
//    );
//
//    public final Setting<SettingColor> outlineColor = sgRender.add(
//        new ColorSetting.Builder()
//            .name("物品2颜色")
//            .defaultValue(new SettingColor(78, 205, 196, 50))
//            .build()
//    );
//
//    private LivingEntity target;
//    private int attackCooldown;
//
//    @EventHandler
//    public void onUpdate(TickEvent.Post e) {
//        // Find target
//        target = TargetUtils.get(entity -> {
//            if (!isValidEntity(entity)) return false;
//            if (entity.age < ticksExisted.get()) return false;
//            if (raytrace.get() && !BaritoneUtil.canSeeBlockFace(entity)) return false;
//            return mc.player.distanceTo(entity) <= range.get();
//        }, SortPriority.LowestDistance);
//
//        if (target == null) {
//            attackCooldown = 0;
//            return;
//        }
//
//        // Auto switch
//        if (autoSwitch.get()) {
//            int swordSlot = findSword();
//            if (swordSlot != -1) {
//                Via.getSelectedSlot()= swordSlot;
//            }
//        }
//
//        // Rotation
//        if (rotationMode.get() != RotationMode.None) {
//            Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target));
//        }
//
//        // Attack
//        if (attackCooldown <= 0) {
//            attack(target);
//            attackCooldown = (int) (20 / speed.get());
//        } else {
//            attackCooldown--;
//        }
//    }
//
//    @EventHandler
//    private void onRender3D(Render3DEvent event) {
//        if (target == null) return;
//        if (shapeMode.get() == ShapeMode.None) return;
//
//        Box box = target.getBoundingBox();
//        event.renderer.box(box, fillColor.get(), outlineColor.get(), shapeMode.get(), 0);
//    }
//
//    private void attack(LivingEntity target) {
//        // Rotate to target
//        if (rotationMode.get() == RotationMode.Packet) {
//            Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target));
//        }
//
//        // Attack
//        mc.interactionManager.attackEntity(mc.player, target);
//
//        // Swing
//        switch (attackMode.get()) {
//            case Mainhand -> mc.player.swingHand(Hand.MAIN_HAND);
//            case Offhand -> mc.player.swingHand(Hand.OFF_HAND);
//            case Both -> {
//                mc.player.swingHand(Hand.MAIN_HAND);
//                mc.player.swingHand(Hand.OFF_HAND);
//            }
//        }
//    }
//
//    private boolean isValidEntity(Entity entity) {
//        if (!(entity instanceof LivingEntity livingEntity)) return false;
//        if (livingEntity == mc.player) return false;
//        if (!livingEntity.isAlive()) return false;
//
//        if (players.get() && entity instanceof PlayerEntity) return true;
//        if (hostiles.get() && EntityUtils.isHostile(livingEntity)) return true;
//        if (animals.get() && EntityUtils.isAnimal(livingEntity)) return true;
//        if (passives.get() && entity instanceof PassiveEntity) return true;
//        if (invisibles.get() && livingEntity.isInvisible()) return true;
//
//        return false;
//    }
//
//    private int findSword() {
//        for (int i = 0; i < 9; i++) {
//            if (mc.player.getInventory().getStack(i).getItem() instanceof SwordItem) {
//                return i;
//            }
//        }
//        return -1;
//    }
//
//    enum AttackMode {
//        Mainhand,
//        Offhand,
//        Both
//    }
//
//    enum RotationMode {
//        None,
//        Normal,
//        Packet
//    }
//}
