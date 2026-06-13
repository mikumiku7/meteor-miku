package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.MikuUtil;
import com.github.mikumiku.addon.util.Rotation;
import com.github.mikumiku.addon.util.RotationManager;
import com.github.mikumiku.addon.util.Via;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class KillAuraMiku extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargeting = settings.createGroup("目标选择");
    private final SettingGroup sgTiming = settings.createGroup("时机控制");

    // 通用设置

    private final Setting<Weapon> weapon = sgGeneral.add(new EnumSetting.Builder<Weapon>()
        .name("武器类型")
        .description("仅在手持指定武器时攻击实体")
        .defaultValue(Weapon.All)
        .build()
    );

    private final Setting<RotationMode> rotation = sgGeneral.add(new EnumSetting.Builder<RotationMode>()
        .name("视角旋转")
        .description("决定何时将视角转向目标")
        .defaultValue(RotationMode.OnHit)
        .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("自动切换")
        .description("攻击目标时自动切换到选定的武器")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> packetAttack = sgGeneral.add(new BoolSetting.Builder()
        .name("发包攻击")
        .description("更好的模式")
        .defaultValue(false)
        .build()
    );


    private final Setting<Boolean> stopSprint = sgGeneral.add(new BoolSetting.Builder()
        .name("停止疾跑")
        .description("攻击前停止疾跑以保持原版行为")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> stopShield = sgGeneral.add(new BoolSetting.Builder()
        .name("停止格挡")
        .description("攻击前自动处理盾牌格挡")
        .defaultValue(false)
        .build()
    );


    private final Setting<Boolean> onlyOnClick = sgGeneral.add(new BoolSetting.Builder()
        .name("仅在点击时")
        .description("仅在按住鼠标左键时攻击")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> onlyOnLook = sgGeneral.add(new BoolSetting.Builder()
        .name("仅在注视时")
        .description("仅在注视实体时攻击")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> pauseOnCombat = sgGeneral.add(new BoolSetting.Builder()
        .name("暂停Baritone")
        .description("在攻击实体时暂时冻结 Baritone 自动寻路")
        .defaultValue(true)
        .build()
    );

    // 目标选择
    private final Setting<Set<EntityType<?>>> entities = sgTargeting.add(new EntityTypeListSetting.Builder()
        .name("实体类型")
        .description("要攻击的实体类型")
        .onlyAttackable()
        .defaultValue(EntityType.PLAYER,
            EntityType.BLAZE,              // 烈焰人
            EntityType.HUSK,               // 沼骸
            EntityType.WIND_CHARGE,        // 旋风人（如果是模组可自定义）
            EntityType.CAVE_SPIDER,        // 洞穴蜘蛛
            EntityType.CREEPER,            // 苦力怕
            EntityType.DROWNED,            // 溺尸
            EntityType.ELDER_GUARDIAN,     // 远古守卫者
            EntityType.ENDER_DRAGON,       // 末影龙
            EntityType.ENDERMAN,           // 末影人
            EntityType.ENDERMITE,          // 末影螨
            EntityType.EVOKER,             // 唤魔者
            EntityType.GHAST,              // 恶魂
            EntityType.GIANT,              // 巨人
            EntityType.GUARDIAN,           // 守卫者
            EntityType.HOGLIN,             // 疣猪兽
            EntityType.HUSK,               // 尸壳
            EntityType.ILLUSIONER,         // 幻术师
            EntityType.MAGMA_CUBE,         // 岩浆怪
            EntityType.PHANTOM,            // 幻翼
            EntityType.PIGLIN,             // 猪灵
            EntityType.PIGLIN_BRUTE,       // 猪灵蛮兵
            EntityType.PILLAGER,           // 掠夺者
            EntityType.RAVAGER,            // 劫掠兽
            EntityType.SHULKER,            // 潜影贝
            EntityType.SILVERFISH,         // 蠹虫
            EntityType.SKELETON,           // 骷髅
            EntityType.SLIME,              // 史莱姆
            EntityType.SPIDER,             // 蜘蛛
            EntityType.STRAY,              // 流浪者
            EntityType.VEX,                // 恼鬼
            EntityType.VINDICATOR,         // 卫道士
            EntityType.WARDEN,             // 监察者
            EntityType.WITCH,              // 女巫
            EntityType.WITHER,             // 凋灵
            EntityType.WITHER_SKELETON,    // 凋灵骷髅（可调整）
            EntityType.ZOMBIE,             // 僵尸
            EntityType.ZOMBIFIED_PIGLIN,   // 僵尸猪灵
            EntityType.ZOGLIN,             // 僵尸疣猪兽
            EntityType.FIREBALL,           // 火球
            EntityType.SHULKER_BULLET)
        .build()
    );

    private final Setting<SortPriority> priority = sgTargeting.add(new EnumSetting.Builder<SortPriority>()
        .name("优先级")
        .description("范围内目标的筛选方式")
        .defaultValue(SortPriority.ClosestAngle)
        .build()
    );

    private final Setting<Integer> maxTargets = sgTargeting.add(new IntSetting.Builder()
        .name("最大目标数")
        .description("同时锁定的实体数量")
        .defaultValue(1)
        .min(1)
        .sliderRange(1, 5)
        .visible(() -> !onlyOnLook.get())
        .build()
    );

    private final Setting<Double> range = sgTargeting.add(new DoubleSetting.Builder()
        .name("攻击范围")
        .description("可攻击实体的最大距离")
        .defaultValue(3.1)
        .min(3)
        .sliderMax(7)
        .build()
    );

    private final Setting<Double> wallsRange = sgTargeting.add(new DoubleSetting.Builder()
        .name("穿墙范围")
        .description("可穿墙攻击实体的最大距离")
        .defaultValue(4.5)
        .min(2)
        .sliderMax(7)
        .build()
    );

    private final Setting<EntityAge> mobAgeFilter = sgTargeting.add(new EnumSetting.Builder<EntityAge>()
        .name("生物年龄过滤")
        .description("决定要攻击的生物年龄（幼年、成年或全部）")
        .defaultValue(EntityAge.Both)
        .build()
    );

    private final Setting<Boolean> ignoreNamed = sgTargeting.add(new BoolSetting.Builder()
        .name("忽略命名生物")
        .description("是否攻击拥有自定义名称的生物")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignorePassive = sgTargeting.add(new BoolSetting.Builder()
        .name("忽略被动生物")
        .description("仅在被动型生物主动攻击你时才进行反击.如猪人、小黑、狼")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreTamed = sgTargeting.add(new BoolSetting.Builder()
        .name("忽略驯服生物")
        .description("避免攻击你驯服的生物")
        .defaultValue(true)
        .build()
    );

    // 时机控制
    private final Setting<Boolean> pauseOnLag = sgTiming.add(new BoolSetting.Builder()
        .name("卡顿时暂停")
        .description("服务器卡顿时暂停攻击")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseOnUse = sgTiming.add(new BoolSetting.Builder()
        .name("使用物品时暂停")
        .description("使用物品时不进行攻击")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseOnCA = sgTiming.add(new BoolSetting.Builder()
        .name("水晶光环时暂停")
        .description("水晶光环放置时不进行攻击")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> tpsSync = sgTiming.add(new BoolSetting.Builder()
        .name("TPS同步")
        .description("尝试将攻击延迟与服务器 TPS 同步")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> customDelay = sgTiming.add(new BoolSetting.Builder()
        .name("自定义延迟")
        .description("使用自定义延迟而非原版冷却时间")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> hitDelay = sgTiming.add(new IntSetting.Builder()
        .name("攻击延迟")
        .description("攻击实体的速度（以刻为单位）")
        .defaultValue(13)
        .min(0)
        .sliderMax(60)
        .visible(customDelay::get)
        .build()
    );

    private final Setting<Integer> switchDelay = sgTiming.add(new IntSetting.Builder()
        .name("切换延迟")
        .description("切换快捷栏后等待多少刻才能攻击实体")
        .defaultValue(0)
        .min(0)
        .sliderMax(10)
        .build()
    );

    private final List<Entity> targets = new ArrayList<>();
    private int switchTimer, hitTimer;
    private boolean wasPathing = false;
    public boolean attacking;

    public KillAuraMiku() {
        super(BaseModule.CATEGORY_MIKU_COMBAT, "Miku杀戮光环", "超级强力的杀敌光环,自动攻击你周围指定的实体,不卡脚");
    }

    @Override
    public void onDeactivate() {
        targets.clear();
        attacking = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!mc.player.isAlive() || PlayerUtils.getGameMode() == GameMode.SPECTATOR) return;
        if (pauseOnUse.get() && (mc.interactionManager.isBreakingBlock() || mc.player.isUsingItem())) return;
        if (onlyOnClick.get() && !mc.options.attackKey.isPressed()) return;
        if (TickRate.INSTANCE.getTimeSinceLastTick() >= 1f && pauseOnLag.get()) return;
        if (pauseOnCA.get() && Modules.get().get(CrystalAura.class).isActive() && Modules.get().get(CrystalAura.class).kaTimer > 0)
            return;

        if (onlyOnLook.get()) {
            Entity targeted = mc.targetedEntity;

            if (targeted == null) return;
            if (!entityCheck(targeted)) return;

            targets.clear();
            targets.add(mc.targetedEntity);
        } else {
            targets.clear();
            TargetUtils.getList(targets, this::entityCheck, priority.get(), maxTargets.get());
        }

        if (targets.isEmpty()) {
            attacking = false;
            if (wasPathing) {
                PathManagers.get().resume();
                wasPathing = false;
            }
            return;
        }

        Entity primary = targets.getFirst();

        if (autoSwitch.get()) {
            Predicate<ItemStack> predicate = switch (weapon.get()) {
                case Axe -> stack -> stack.getItem() instanceof AxeItem;
                case Sword -> stack -> MikuUtil.isSwordItem(stack.getItem());
                case Mace -> stack -> stack.getItem() instanceof MaceItem;
                case Trident -> stack -> stack.getItem() instanceof TridentItem;
                case All ->
                    stack -> stack.getItem() instanceof AxeItem || MikuUtil.isSwordItem(stack.getItem()) || stack.getItem() instanceof MaceItem || stack.getItem() instanceof TridentItem;
                default -> o -> true;
            };
            FindItemResult weaponResult = InvUtils.findInHotbar(predicate);

            InvUtils.swap(weaponResult.slot(), false);
        }

        if (!itemInHand()) return;

        attacking = true;
        if (rotation.get() == RotationMode.Always)
            Rotations.rotate(Rotations.getYaw(primary), Rotations.getPitch(primary, Target.Body));
        if (pauseOnCombat.get() && PathManagers.get().isPathing() && !wasPathing) {
            PathManagers.get().pause();
            wasPathing = true;
        }

        if (delayCheck()) targets.forEach(this::attack);
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
            switchTimer = switchDelay.get();
        }
    }

    private boolean shouldShieldBreak() {


        return false;
    }

    private boolean entityCheck(Entity entity) {
        if (entity.equals(mc.player)) return false;
        if ((entity instanceof LivingEntity livingEntity && livingEntity.isDead()) || !entity.isAlive()) return false;

        Box hitbox = entity.getBoundingBox();
        if (!PlayerUtils.isWithin(
            MathHelper.clamp(mc.player.getX(), hitbox.minX, hitbox.maxX),
            MathHelper.clamp(mc.player.getY(), hitbox.minY, hitbox.maxY),
            MathHelper.clamp(mc.player.getZ(), hitbox.minZ, hitbox.maxZ),
            range.get()
        )) return false;

        if (!entities.get().contains(entity.getType())) return false;
        if (ignoreNamed.get() && entity.hasCustomName()) return false;
        if (!PlayerUtils.canSeeEntity(entity) && !PlayerUtils.isWithin(entity, wallsRange.get())) return false;
        if (ignoreTamed.get()) {
//            if (entity instanceof Tameable tameable
//                && tameable.getOwnerUuid() != null
//                && tameable.getOwnerUuid().equals(mc.player.getUuid())
//            ) {
//
//                return false;
//            }
        }
        if (ignorePassive.get()) {
            if (entity instanceof EndermanEntity enderman && !enderman.isAngry()) return false;
            if (entity instanceof ZombifiedPiglinEntity piglin && !piglin.isAttacking()) return false;
            if (entity instanceof WolfEntity wolf && !wolf.isAttacking()) return false;
        }
        if (entity instanceof PlayerEntity player) {
            if (player.isCreative()) return false;
            if (!Friends.get().shouldAttack(player)) return false;

        }
        if (entity instanceof AnimalEntity animal) {
            return switch (mobAgeFilter.get()) {
                case Baby -> animal.isBaby();
                case Adult -> !animal.isBaby();
                case Both -> true;
            };
        }
        return true;
    }

    private boolean delayCheck() {
        if (switchTimer > 0) {
            switchTimer--;
            return false;
        }

        float delay = (customDelay.get()) ? hitDelay.get() : 0.5f;
        if (tpsSync.get()) delay /= (TickRate.INSTANCE.getTickRate() / 20);

        if (customDelay.get()) {
            if (hitTimer < delay) {
                hitTimer++;
                return false;
            } else {
                return true;
            }
        } else {
            return mc.player.getAttackCooldownProgress(delay) >= 1;
        }
    }

    private void attack(Entity target) {
//         Rotations.getYaw(target), Rotations.getPitch(target, Target.Body) ;


        if (stopSprint.get()) {
            if (mc.player.isSneaking()) {
//                PlayerInputC2SPacket
                Via.sendReleaseShift();
            }
            if (mc.player.isSprinting()) {
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            }
        }

        Vec3d feetPos = Via.getEntityPos(target);

        Vec3d torsoPos = feetPos.add(0.0, target.getHeight() / 2.0f, 0.0);
        Vec3d eyesPos = target.getEyePos();
        Vec3d hitVec = Stream.of(feetPos, torsoPos, eyesPos)
            .min(Comparator.comparing(pos -> mc.player.getEyePos().squaredDistanceTo(pos)))
            .orElse(eyesPos);

        Rotation rotation = new Rotation(hitVec).setPriority(10);
        Rotation rotation1 = new Rotation((float) Rotations.getYaw(target), (float) Rotations.getPitch(target, Target.Body));
        RotationManager.getInstance().register(rotation);

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);

        if (packetAttack.get()) {

            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
//            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            mc.player.swingHand(Hand.MAIN_HAND);

        } else {

            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);

        }

        hitTimer = 0;

        if (this.rotation.get() == RotationMode.OnHit) {
            RotationManager.getInstance().sync();
        }
    }

    private boolean itemInHand() {
        Item item = mc.player.getMainHandStack().getItem();
        if (shouldShieldBreak()) return item instanceof AxeItem;

        return switch (weapon.get()) {
            case Axe -> item instanceof AxeItem;
            case Sword -> MikuUtil.isSwordItem(item);
            case Mace -> item instanceof MaceItem;
            case Trident -> item instanceof TridentItem;
            case All ->
                item instanceof AxeItem || MikuUtil.isSwordItem(item) || item instanceof MaceItem || item instanceof TridentItem;
            default -> true;
        };
    }

    public Entity getTarget() {
        if (!targets.isEmpty()) return targets.getFirst();
        return null;
    }

    @Override
    public String getInfoString() {
        if (!targets.isEmpty()) return EntityUtils.getName(getTarget());
        return null;
    }

    public enum Weapon {
        Sword("剑"),
        Axe("斧"),
        Mace("锤"),
        Trident("三叉戟"),
        All("全部武器"),
        Any("任意物品");

        private final String displayName;

        Weapon(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum RotationMode {
        Always("始终旋转"),
        OnHit("攻击时旋转"),
        None("不旋转");

        private final String displayName;

        RotationMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }


    public enum EntityAge {
        Baby("幼年"),
        Adult("成年"),
        Both("全部");

        private final String displayName;

        EntityAge(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

}
