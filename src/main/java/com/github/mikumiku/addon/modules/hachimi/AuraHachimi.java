//package com.github.mikumiku.addon.modules.hachimi;
//
//import meteordevelopment.meteorclient.events.render.Render3DEvent;
//import meteordevelopment.meteorclient.events.world.TickEvent;
//import meteordevelopment.meteorclient.renderer.ShapeMode;
//import meteordevelopment.meteorclient.settings.*;
//import meteordevelopment.meteorclient.systems.friends.Friends;
//import meteordevelopment.meteorclient.systems.modules.Categories;
//import meteordevelopment.meteorclient.systems.modules.Module;
//import meteordevelopment.meteorclient.utils.entity.EntityUtils;
//import meteordevelopment.meteorclient.utils.player.InvUtils;
//import meteordevelopment.meteorclient.utils.player.Rotations;
//import meteordevelopment.meteorclient.utils.render.color.SettingColor;
//import meteordevelopment.orbit.EventHandler;
//import net.minecraft.entity.Entity;
//import net.minecraft.entity.LivingEntity;
//import net.minecraft.entity.decoration.EndCrystalEntity;
//import net.minecraft.entity.player.PlayerEntity;
//import net.minecraft.entity.projectile.ArrowEntity;
//import net.minecraft.item.*;
//import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
//import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
//import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
//import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
//import net.minecraft.util.Hand;
//import net.minecraft.util.hit.BlockHitResult;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.util.math.Box;
//import net.minecraft.util.math.Vec3d;
//import net.minecraft.world.RaycastContext;
//
//import java.util.stream.Stream;
//
//public class AuraHachimi extends Module {
//    private final SettingGroup sgGeneral = settings.getDefaultGroup();
//    private final SettingGroup sgTarget = settings.createGroup("目标选择");
//    private final SettingGroup sgAttack = settings.createGroup("攻击设置");
//    private final SettingGroup sgRotation = settings.createGroup("旋转设置");
//    private final SettingGroup sgRender = settings.createGroup("渲染设置");
//
//    // 常规设置
//    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
//        .name("挥手动画")
//        .description("攻击后播放挥手动画")
//        .defaultValue(true)
//        .build()
//    );
//
//    private final Setting<TargetMode> targetMode = sgGeneral.add(new EnumSetting.Builder<TargetMode>()
//        .name("目标模式")
//        .description("选择目标的模式")
//        .defaultValue(TargetMode.SWITCH)
//        .build()
//    );
//
//    private final Setting<Priority> priority = sgGeneral.add(new EnumSetting.Builder<Priority>()
//        .name("优先级")
//        .description("搜索目标时的优先级")
//        .defaultValue(Priority.HEALTH)
//        .build()
//    );
//
//    // 目标选择
//    private final Setting<Double> searchRange = sgTarget.add(new DoubleSetting.Builder()
//        .name("搜索范围")
//        .description("搜索目标的范围")
//        .defaultValue(5.0)
//        .min(1.0)
//        .sliderMax(10.0)
//        .build()
//    );
//
//    private final Setting<Double> attackRange = sgTarget.add(new DoubleSetting.Builder()
//        .name("攻击范围")
//        .description("攻击实体的范围")
//        .defaultValue(4.5)
//        .min(1.0)
//        .sliderMax(6.0)
//        .build()
//    );
//
//    private final Setting<Double> wallRange = sgTarget.add(new DoubleSetting.Builder()
//        .name("穿墙范围")
//        .description("穿墙攻击实体的范围")
//        .defaultValue(4.5)
//        .min(1.0)
//        .sliderMax(6.0)
//        .build()
//    );
//
//    private final Setting<Boolean> vanillaRange = sgTarget.add(new BoolSetting.Builder()
//        .name("原版范围")
//        .description("仅在原版范围内攻击")
//        .defaultValue(false)
//        .build()
//    );
//
//    private final Setting<Double> fov = sgTarget.add(new DoubleSetting.Builder()
//        .name("视野角度")
//        .description("攻击实体的视野角度")
//        .defaultValue(180.0)
//        .min(1.0)
//        .sliderMax(180.0)
//        .build()
//    );
//
//    private final Setting<Boolean> players = sgTarget.add(new BoolSetting.Builder()
//        .name("玩家")
//        .description("攻击玩家")
//        .defaultValue(true)
//        .build()
//    );
//
//    private final Setting<Boolean> monsters = sgTarget.add(new BoolSetting.Builder()
//        .name("怪物")
//        .description("攻击怪物")
//        .defaultValue(false)
//        .build()
//    );
//
//    private final Setting<Boolean> neutrals = sgTarget.add(new BoolSetting.Builder()
//        .name("中立生物")
//        .description("攻击中立生物")
//        .defaultValue(false)
//        .build()
//    );
//
//    private final Setting<Boolean> animals = sgTarget.add(new BoolSetting.Builder()
//        .name("动物")
//        .description("攻击动物")
//        .defaultValue(false)
//        .build()
//    );
//
//    private final Setting<Boolean> invisibles = sgTarget.add(new BoolSetting.Builder()
//        .name("隐身实体")
//        .description("攻击隐身实体")
//        .defaultValue(true)
//        .build()
//    );
//
//    private final Setting<Boolean> armorCheck = sgTarget.add(new BoolSetting.Builder()
//        .name("护甲检测")
//        .description("攻击前检测目标是否有护甲")
//        .defaultValue(false)
//        .build()
//    );
//
//    private final Setting<Integer> ticksExisted = sgTarget.add(new IntSetting.Builder()
//        .name("存在时长")
//        .description("实体存在的最小刻数才会被攻击")
//        .defaultValue(0)
//        .min(0)
//        .sliderMax(200)
//        .build()
//    );
//
//    // 攻击设置
//    private final Setting<Boolean> attackDelay = sgAttack.add(new BoolSetting.Builder()
//        .name("攻击延迟")
//        .description("根据 Minecraft 攻击延迟来延迟攻击以获得最大伤害")
//        .defaultValue(true)
//        .build()
//    );
//
//    private final Setting<Double> attackSpeed = sgAttack.add(new DoubleSetting.Builder()
//        .name("攻击速度")
//        .description("攻击延迟（仅当攻击延迟关闭时生效）")
//        .defaultValue(20.0)
//        .min(1.0)
//        .sliderMax(20.0)
//        .visible(() -> !attackDelay.get())
//        .build()
//    );
//
//    private final Setting<Double> randomSpeed = sgAttack.add(new DoubleSetting.Builder()
//        .name("随机速度")
//        .description("攻击的随机延迟（仅当攻击延迟关闭时生效）")
//        .defaultValue(0.0)
//        .min(0.0)
//        .sliderMax(10.0)
//        .visible(() -> !attackDelay.get())
//        .build()
//    );
//
//    private final Setting<Double> swapPenalty = sgAttack.add(new DoubleSetting.Builder()
//        .name("切换惩罚")
//        .description("切换物品后攻击的延迟，防止反作弊标记")
//        .defaultValue(0.0)
//        .min(0.0)
//        .sliderMax(10.0)
//        .build()
//    );
//
//    private final Setting<Swap> autoSwap = sgAttack.add(new EnumSetting.Builder<Swap>()
//        .name("自动切换")
//        .description("攻击前自动切换到武器")
//        .defaultValue(Swap.OFF)
//        .build()
//    );
//
//    private final Setting<Boolean> swordCheck = sgAttack.add(new BoolSetting.Builder()
//        .name("武器检测")
//        .description("攻击前检测手中是否有武器")
//        .defaultValue(true)
//        .build()
//    );
//
//    private final Setting<Boolean> stopSprint = sgAttack.add(new BoolSetting.Builder()
//        .name("停止疾跑")
//        .description("攻击前停止疾跑以保持原版行为")
//        .defaultValue(false)
//        .build()
//    );
//
//    private final Setting<Boolean> stopShield = sgAttack.add(new BoolSetting.Builder()
//        .name("停止格挡")
//        .description("攻击前自动处理盾牌格挡")
//        .defaultValue(false)
//        .build()
//    );
//
//    private final Setting<Boolean> maceBreach = sgAttack.add(new BoolSetting.Builder()
//        .name("重锤破甲")
//        .description("利用原版漏洞将破甲附魔应用到剑上")
//        .defaultValue(false)
//        .visible(() -> autoSwap.get() != Swap.SILENT)
//        .build()
//    );
//
//    // 旋转设置
//    private final Setting<HitVector> hitVector = sgRotation.add(new EnumSetting.Builder<HitVector>()
//        .name("瞄准位置")
//        .description("攻击实体时瞄准的位置")
//        .defaultValue(HitVector.FEET)
//        .build()
//    );
//
//    private final Setting<Boolean> rotate = sgRotation.add(new BoolSetting.Builder()
//        .name("旋转")
//        .description("攻击前旋转视角")
//        .defaultValue(false)
//        .build()
//    );
//
//    private final Setting<Boolean> silentRotate = sgRotation.add(new BoolSetting.Builder()
//        .name("静默旋转")
//        .description("静默旋转到服务器")
//        .defaultValue(false)
//        .visible(rotate::get)
//        .build()
//    );
//
//    private final Setting<Boolean> yawStep = sgRotation.add(new BoolSetting.Builder()
//        .name("偏航限制")
//        .description("在多个刻中旋转偏航以防止反作弊标记")
//        .defaultValue(false)
//        .visible(rotate::get)
//        .build()
//    );
//
//    private final Setting<Integer> yawStepLimit = sgRotation.add(new IntSetting.Builder()
//        .name("偏航限制值")
//        .description("单个刻中最大偏航旋转角度")
//        .defaultValue(180)
//        .min(1)
//        .sliderMax(180)
//        .visible(() -> rotate.get() && yawStep.get())
//        .build()
//    );
//
//    // 渲染设置
//    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
//        .name("渲染")
//        .description("在目标上渲染指示器")
//        .defaultValue(true)
//        .build()
//    );
//
//    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
//        .name("形状模式")
//        .description("形状的渲染方式")
//        .defaultValue(ShapeMode.Both)
//        .build()
//    );
//
//    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
//        .name("侧面颜色")
//        .description("目标框的侧面颜色")
//        .defaultValue(new SettingColor(255, 0, 0, 75))
//        .build()
//    );
//
//    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
//        .name("线框颜色")
//        .description("目标框的线框颜色")
//        .defaultValue(new SettingColor(255, 0, 0, 255))
//        .build()
//    );
//
//    private Entity entityTarget;
//    private long randomDelay = -1;
//    private long lastAttackTime;
//    private long lastSwapTime;
//    private boolean shielding;
//    private boolean sneaking;
//    private boolean sprinting;
//    private boolean rotated;
//    private float[] silentRotations;
//
//    public AuraHachimi() {
//        super(Categories.Combat, "光环", "自动攻击附近的实体");
//    }
//
//    @Override
//    public void onDeactivate() {
//        entityTarget = null;
//        silentRotations = null;
//    }
//
//    @EventHandler
//    private void onTick(TickEvent.Pre event) {
//        if (mc.player == null || mc.world == null) return;
//        if (mc.player.isSpectator()) return;
//
//        Vec3d eyePos = mc.player.getEyePos();
//
//        // 选择目标
//        entityTarget = switch (targetMode.get()) {
//            case SWITCH -> getAttackTarget(eyePos);
//            case SINGLE -> {
//                if (entityTarget == null || !entityTarget.isAlive() || !isInAttackRange(eyePos, entityTarget)) {
//                    yield getAttackTarget(eyePos);
//                }
//                yield entityTarget;
//            }
//        };
//
//        if (entityTarget == null) {
//            silentRotations = null;
//            return;
//        }
//
//        // 检查切换延迟
//        if (System.currentTimeMillis() - lastSwapTime < swapPenalty.get() * 50) {
//            silentRotations = null;
//            return;
//        }
//
//        // 自动切换武器
//        int slot = getSwordSlot();
//        boolean silentSwapped = false;
//
//        if (!(mc.player.getMainHandStack().getItem() instanceof SwordItem) && slot != -1) {
//            switch (autoSwap.get()) {
//                case NORMAL -> InvUtils.swap(slot, false);
//                case SILENT -> {
//                    InvUtils.swap(slot, true);
//                    silentSwapped = true;
//                }
//            }
//        }
//
//        if (!isHoldingSword() && autoSwap.get() != Swap.SILENT) {
//            return;
//        }
//
//        // 处理旋转
//        if (rotate.get()) {
//            float[] rotation = Rotations.getYaw(entityTarget);
//
//            if (!silentRotate.get() && yawStep.get()) {
//                // 实现偏航限制逻辑
//                rotated = true; // 简化处理
//            } else {
//                rotated = true;
//            }
//
//            if (silentRotate.get()) {
//                silentRotations = rotation;
//            } else {
//                Rotations.rotate(rotation[0], rotation[1]);
//            }
//        }
//
//        if (!rotated && rotate.get() || !isInAttackRange(eyePos, entityTarget)) {
//            return;
//        }
//
//        // 攻击逻辑
//        if (attackDelay.get()) {
//            float cooldown = mc.player.getAttackCooldownProgress(0.5f);
//            if (cooldown >= 1.0f && attackTarget(entityTarget)) {
//                lastAttackTime = System.currentTimeMillis();
//            }
//        } else {
//            if (randomDelay < 0) {
//                randomDelay = (long) (Math.random() * (randomSpeed.get() * 10.0f + 1.0f));
//            }
//            float delay = (float) (attackSpeed.get() * 50.0f) + randomDelay;
//
//            long currentTime = System.currentTimeMillis() - lastAttackTime;
//            if (currentTime >= 1000.0f - delay && attackTarget(entityTarget)) {
//                randomDelay = -1;
//                lastAttackTime = System.currentTimeMillis();
//            }
//        }
//
//        if (silentSwapped) {
//            InvUtils.swapBack();
//        }
//    }
//
//    @EventHandler
//    private void onRender(Render3DEvent event) {
//        if (entityTarget != null && render.get() && (isHoldingSword() || autoSwap.get() == Swap.SILENT)) {
//            Box box = entityTarget.getBoundingBox();
//            event.renderer.box(box, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
//        }
//    }
//
//    private boolean attackTarget(Entity entity) {
//        preAttackTarget();
//
//        if (silentRotate.get() && silentRotations != null) {
//            Rotations.rotate(silentRotations[0], silentRotations[1]);
//        }
//
//        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
//
//        if (swing.get()) {
//            mc.player.swingHand(Hand.MAIN_HAND);
//        } else {
//            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
//        }
//
//        postAttackTarget(entity);
//        return true;
//    }
//
//    private void preAttackTarget() {
//        shielding = false;
//        if (stopShield.get()) {
//            if (mc.player.getOffHandStack().getItem() == Items.SHIELD && mc.player.isBlocking()) {
//                shielding = true;
//                // 发送停止格挡数据包
//            }
//        }
//
//        sneaking = false;
//        sprinting = false;
//        if (stopSprint.get()) {
//            if (mc.player.isSneaking()) {
//                sneaking = true;
//                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.WalkMode.RELEASE_SHIFT_KEY));
//            }
//            if (mc.player.isSprinting()) {
//                sprinting = true;
//                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.WalkMode.STOP_SPRINTING));
//            }
//        }
//    }
//
//    private void postAttackTarget(Entity entity) {
//        if (shielding) {
//            mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.OFF_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
//        }
//        if (sneaking) {
//            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.WalkMode.PRESS_SHIFT_KEY));
//        }
//        if (sprinting) {
//            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.WalkMode.START_SPRINTING));
//        }
//    }
//
//    private Entity getAttackTarget(Vec3d pos) {
//        double min = Double.MAX_VALUE;
//        Entity attackTarget = null;
//
//        for (Entity entity : mc.world.getEntities()) {
//            if (!isValidTarget(entity)) continue;
//
//            double dist = pos.distanceTo(entity.getPos());
//            if (dist <= searchRange.get()) {
//                if (entity.age < ticksExisted.get()) continue;
//
//                double value = switch (priority.get()) {
//                    case DISTANCE -> dist;
//                    case HEALTH -> entity instanceof LivingEntity e ? e.getHealth() + e.getAbsorptionAmount() : Double.MAX_VALUE;
//                    case ARMOR -> entity instanceof LivingEntity e ? getArmorDurability(e) : Double.MAX_VALUE;
//                };
//
//                if (value < min) {
//                    min = value;
//                    attackTarget = entity;
//                }
//            }
//        }
//        return attackTarget;
//    }
//
//    private boolean isValidTarget(Entity entity) {
//        if (entity == null || entity == mc.player || !entity.isAlive()) return false;
//        if (entity instanceof EndCrystalEntity || entity instanceof ArrowEntity) return false;
//        if (Friends.get().isFriend((PlayerEntity) entity)) return false;
//        if (!invisibles.get() && entity.isInvisible()) return false;
//
//        if (armorCheck.get() && entity instanceof LivingEntity living) {
//            if (!living.getArmorItems().iterator().hasNext()) return false;
//        }
//
//        return isEnemy(entity);
//    }
//
//    private boolean isEnemy(Entity e) {
//        if (e instanceof PlayerEntity && players.get()) return true;
//        if (EntityUtils.isMonster(e) && monsters.get()) return true;
//        if (EntityUtils.isNeutral(e) && neutrals.get()) return true;
//        if (EntityUtils.isPassive(e) && animals.get()) return true;
//        return false;
//    }
//
//    private float getArmorDurability(LivingEntity e) {
//        float damage = 0.0f;
//        float max = 0.0f;
//
//        for (ItemStack armor : e.getArmorItems()) {
//            if (!armor.isEmpty()) {
//                damage += armor.getDamage();
//                max += armor.getMaxDamage();
//            }
//        }
//        return max == 0 ? 0 : 100.0f - (damage / max * 100.0f);
//    }
//
//    private boolean isInAttackRange(Vec3d pos, Entity entity) {
//        Vec3d entityPos = getAttackRotateVec(entity);
//        double dist = pos.distanceTo(entityPos);
//
//        if (vanillaRange.get() && dist > 3.0) return false;
//        if (dist > attackRange.get()) return false;
//
//        BlockHitResult result = mc.world.raycast(new RaycastContext(
//            pos, entityPos,
//            RaycastContext.ShapeType.COLLIDER,
//            RaycastContext.FluidHandling.NONE, mc.player
//        ));
//
//        if (result != null && !result.getBlockPos().equals(BlockPos.ofFloored(entityPos)) && dist > wallRange.get()) {
//            return false;
//        }
//
//        if (fov.get() != 180.0) {
//            double angle = Math.abs(Rotations.getYaw(entity)[0] - mc.player.getYaw());
//            if (angle > 180) angle = 360 - angle;
//            return angle <= fov.get();
//        }
//
//        return true;
//    }
//
//    private Vec3d getAttackRotateVec(Entity entity) {
//        Vec3d feetPos = entity.getPos();
//        return switch (hitVector.get()) {
//            case FEET -> feetPos;
//            case TORSO -> feetPos.add(0.0, entity.getHeight() / 2.0, 0.0);
//            case EYES -> entity.getEyePos();
//            case AUTO -> entity.getEyePos(); // 简化处理
//        };
//
//    }
//
//    private int getSwordSlot() {
//        float maxDamage = 0.0f;
//        int slot = -1;
//
//        for (int i = 0; i < 9; i++) {
//            ItemStack stack = mc.player.getInventory().getStack(i);
//            Item item = stack.getItem();
//
//            if (item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem || item instanceof MaceItem) {
//                float damage = 0;
//                if (item instanceof SwordItem sword) damage = sword.getAttackDamage();
//                else if (item instanceof AxeItem axe) damage = axe.getAttackDamage();
//                else if (item instanceof TridentItem) damage = 9.0f;
//                else if (item instanceof MaceItem) damage = 5.0f;
//
//                if (damage > maxDamage) {
//                    maxDamage = damage;
//                    slot = i;
//                }
//            }
//        }
//        return slot;
//    }
//
//    private boolean isHoldingSword() {
//        if (!swordCheck.get()) return true;
//        Item item = mc.player.getMainHandStack().getItem();
//        return item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem || item instanceof MaceItem;
//    }
//
//    public Entity getEntityTarget() {
//        return entityTarget;
//    }
//
//    public enum TargetMode {
//        SWITCH("切换"),
//        SINGLE("单一");
//
//        private final String displayName;
//
//        TargetMode(String displayName) {
//            this.displayName = displayName;
//        }
//
//        @Override
//        public String toString() {
//            return displayName;
//        }
//    }
//
//    public enum Swap {
//        NORMAL("普通"),
//        SILENT("静默"),
//        OFF("关闭");
//
//        private final String displayName;
//
//        Swap(String displayName) {
//            this.displayName = displayName;
//        }
//
//        @Override
//        public String toString() {
//            return displayName;
//        }
//    }
//
//    public enum HitVector {
//        EYES("眼睛"),
//        TORSO("躯干"),
//        FEET("脚部"),
//        AUTO("自动");
//
//        private final String displayName;
//
//        HitVector(String displayName) {
//            this.displayName = displayName;
//        }
//
//        @Override
//        public String toString() {
//            return displayName;
//        }
//    }
//
//    public enum Priority {
//        HEALTH("生命值"),
//        DISTANCE("距离"),
//        ARMOR("护甲");
//
//        private final String displayName;
//
//        Priority(String displayName) {
//            this.displayName = displayName;
//        }
//
//        @Override
//        public String toString() {
//            return displayName;
//        }
//    }
//}
