package com.github.mikumiku.addon.modules;


import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.*;
import com.github.mikumiku.addon.util.timer.SyncedTickTimer;
import com.github.mikumiku.addon.util.timer.Timers;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class AutoCrystal extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlace = settings.createGroup("放置");
    private final SettingGroup sgBreak = settings.createGroup("破坏");
    private final SettingGroup sgDamage = settings.createGroup("伤害");
    private final SettingGroup base = settings.createGroup("底座");
    private final SettingGroup sgRender = settings.createGroup("渲染");


    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("目标")
        .description("要攻击的目标")
        .onlyAttackable()
        .defaultValue(EntityType.PLAYER, EntityType.WARDEN, EntityType.WITHER)
        .build()
    );

    // General
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("目标范围")
        .description("搜索目标的范围")
        .defaultValue(10)
        .min(0)
        .sliderMax(20)
        .build()
    );
    private final Setting<Boolean> ignoreNakeds = sgGeneral.add(new BoolSetting.Builder()
        .name("忽略裸吊")
        .description("忽略没有装备的玩家。")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> whileMining = sgGeneral.add(new BoolSetting.Builder()
        .name("挖掘时攻击")
        .description("允许在挖掘方块时进行攻击")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> pauseOnEat = sgGeneral.add(new BoolSetting.Builder()
        .name("进食暂停")
        .description("进食时暂停")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseOnDrink = sgGeneral.add(new BoolSetting.Builder()
        .name("喝药暂停")
        .description("喝药时暂停")
        .defaultValue(true)
        .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("目标优先级")
        .description("如何选择目标")
        .defaultValue(SortPriority.LowestHealth)
        .build()
    );
    // 添加智能目标选择设置
    private final Setting<Boolean> smartTargeting = sgGeneral.add(new BoolSetting.Builder()
        .name("智能目标选择")
        .description("根据威胁程度智能选择目标")
        .defaultValue(true)
        .build()
    );

    // Place
    private final Setting<Boolean> place = sgPlace.add(new BoolSetting.Builder()
        .name("放置")
        .description("是否放置水晶")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> placeSpeed = sgPlace.add(new DoubleSetting.Builder()
        .name("放置速度")
        .description("每秒放置水晶的次数")
        .defaultValue(18)
        .min(0)
        .sliderMax(20)
        .visible(place::get)
        .build()
    );

    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder()
        .name("放置范围")
        .description("放置水晶的范围")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .visible(place::get)
        .build()
    );

    private final Setting<Double> placeWallRange = sgPlace.add(new DoubleSetting.Builder()
        .name("穿墙放置范围")
        .description("穿墙放置水晶的范围")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .visible(place::get)
        .build()
    );

    // 添加自适应速度设置
    private final Setting<Boolean> adaptiveSpeed = sgPlace.add(new BoolSetting.Builder()
        .name("自适应速度")
        .description("根据服务器延迟自动调整放置速度")
        .defaultValue(true)
        .visible(place::get)
        .build()
    );


    private final Setting<Boolean> strictDirection = sgPlace.add(new BoolSetting.Builder()
        .name("严格方向")
        .description("只放置可见方向的水晶")
        .defaultValue(false)
        .visible(place::get)
        .build()
    );

    private final Setting<SupportMode> support = sgPlace.add(new EnumSetting.Builder<SupportMode>()
        .name("放置底座")
        .description("当没有合适位置时，放置底座方块。")
        .defaultValue(SupportMode.Disabled)
        .build()
    );
    private final Setting<Integer> supportDelay = sgPlace.add(new IntSetting.Builder()
        .name("底座延迟")
        .description("放置底座方块后的延迟刻数。")
        .defaultValue(1)
        .min(0)
        .visible(() -> support.get() != SupportMode.Disabled)
        .build()
    );

    // Break
    private final Setting<Double> breakSpeed = sgBreak.add(new DoubleSetting.Builder()
        .name("破坏速度")
        .description("每秒破坏水晶的次数")
        .defaultValue(13)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder()
        .name("破坏范围")
        .description("破坏水晶的范围")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Double> breakWallRange = sgBreak.add(new DoubleSetting.Builder()
        .name("穿墙破坏范围")
        .description("穿墙破坏水晶的范围")
        .defaultValue(4)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Boolean> antiWeakness = sgBreak.add(new BoolSetting.Builder()
        .name("反虚弱")
        .description("当有虚弱效果时自动切换到可以破坏水晶的工具。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> removeInhibit = sgBreak.add(new BoolSetting.Builder()
        .name("移除抑制")
        .description("防止多次攻击同一水晶")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> removeInhibitDelay = sgBreak.add(new IntSetting.Builder()
        .name("抑制延迟")
        .description("抑制延迟(tick)")
        .defaultValue(5)
        .min(0)
        .sliderMax(20)
        .visible(removeInhibit::get)
        .build()
    );

    // Damage
    private final Setting<Double> minDamage = sgDamage.add(new DoubleSetting.Builder()
        .name("最小伤害")
        .description("攻击目标所需的最低伤害")
        .defaultValue(4)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Double> maxSelfDamage = sgDamage.add(new DoubleSetting.Builder()
        .name("最大自伤")
        .description("允许的最大自身伤害")
        .defaultValue(19)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> antiSuicide = sgDamage.add(new BoolSetting.Builder()
        .name("防自杀")
        .description("防止自杀")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> armorBreaker = sgDamage.add(new BoolSetting.Builder()
        .name("破甲")
        .description("优先攻击低耐久护甲的敌人")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> armorScale = sgDamage.add(new DoubleSetting.Builder()
        .name("护甲耐久阈值")
        .description("护甲耐久百分比")
        .defaultValue(10)
        .min(0)
        .sliderMax(100)
        .visible(armorBreaker::get)
        .build()
    );

    // Render
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("渲染")
        .description("渲染放置位置")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("形状模式")
        .description("渲染的形状")
        .defaultValue(ShapeMode.Both)
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("侧面颜色")
        .description("渲染的侧面颜色")
        .defaultValue(new SettingColor(255, 0, 255, 40))
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("线条颜色")
        .description("渲染的线条颜色")
        .defaultValue(new SettingColor(255, 0, 255, 255))
        .visible(render::get)
        .build()
    );

    private final Setting<Boolean> renderDamage = sgRender.add(new BoolSetting.Builder()
        .name("渲染伤害")
        .description("渲染伤害值")
        .defaultValue(true)
        .visible(render::get)
        .build()
    );

    private final Setting<Boolean> debug = sgRender.add(new BoolSetting.Builder()
        .name("debug")
        .description("debug")
        .defaultValue(false)
        .visible(render::get)
        .build()
    );

    // Variables
    private BlockPos renderPos;
    private double renderedDamage;
    private final SyncedTickTimer placeTimer = Timers.tickTimer();
    private final SyncedTickTimer breakTimer = Timers.tickTimer();
    private final Map<Integer, Long> attackedCrystals = new ConcurrentHashMap<>();
    private final Map<BlockPos, Long> placedCrystals = new ConcurrentHashMap<>();
    private LivingEntity target;
    private final List<LivingEntity> targets = new ArrayList<>();

    public AutoCrystal() {
        super(BaseModule.CATEGORY_MIKU_COMBAT, "Miku水晶", "自动放置和破坏末影水晶");
    }

    @Override
    public void onActivate() {
        renderPos = null;
        renderedDamage = 0;
        attackedCrystals.clear();
        placedCrystals.clear();
        placeTimer.reset();
        breakTimer.reset();
        target = null;
        targets.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {

//        TargetUtils.getPlayerTarget(targetRange.get(), priority.get());

        findTargets();

        if (targets.isEmpty()) {
            return;
        }
        // Find target
        target = getNearestTarget();

        // Check pause conditions
        if (shouldPause()) return;

        // Break crystals
        if (breakTimer.tick(20 - breakSpeed.get().intValue())) {
            EndCrystalEntity crystal = findBestCrystal();
            if (crystal != null) {
                breakCrystal(crystal);
                breakTimer.reset();
            }
        }

        // Place crystals
        if (place.get() && placeTimer.tick(20 - placeSpeed.get().intValue())) {
            BlockPos pos = findBestPlacePos();
            if (pos != null) {
                placeCrystal(pos);
            }
            placeTimer.reset();
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || renderPos == null) return;

        event.renderer.box(renderPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);

        if (renderDamage.get() && renderedDamage > 0) {
            String text = String.format("%.1f", this.renderedDamage);
//            event.renderer.text(text, , lineColor.get(), true);

            MatrixStack matrices = event.matrices;
            // 示例位置：BlockPos(100, 64, 100) 的正中
            Vec3d pos = new Vec3d(renderPos.getX() + 0.5, renderPos.getY() + 0.5, renderPos.getZ() + 0.5); // 注意用 center 坐标
//            renderTextInWorld(matrices, pos, "Hello Meteor 3D", 0.02f, lineColor.get().getVec3f(), true, true);
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (renderDamage.get() && renderedDamage > 0) {

            Vector3d vec3 = new Vector3d();
            vec3.set(renderPos.getX() + 0.5, renderPos.getY() + 0.5, renderPos.getZ() + 0.5);
            if (NametagUtils.to2D(vec3, 1.25)) {
                NametagUtils.begin(vec3);
                TextRenderer.get().begin(1, false, true);

                String text = String.format("%.1f", renderedDamage);
                double w = TextRenderer.get().getWidth(text) / 2;
                TextRenderer.get().render(text, -w, 0, lineColor.get(), true);

                TextRenderer.get().end();
                NametagUtils.end();
            }
        }

    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof EntitySpawnS2CPacket packet) {
            if (packet.getEntityType() == EntityType.END_CRYSTAL) {
                BlockPos pos = BlockPos.ofFloored(packet.getX(), packet.getY() - 1, packet.getZ());
                placedCrystals.remove(pos);
            }
        }

        if (event.packet instanceof EntitiesDestroyS2CPacket packet) {
            for (int id : packet.getEntityIds()) {
                attackedCrystals.remove(id);
            }
        }

        if (event.packet instanceof PlaySoundS2CPacket packet) {
            if (packet.getSound().value() == SoundEvents.ENTITY_GENERIC_EXPLODE.value()
                && packet.getCategory() == SoundCategory.BLOCKS) {
                Vec3d pos = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
                mc.world.getEntities().forEach(e -> {
                    if (e instanceof EndCrystalEntity && Via.getEntityPos(e).distanceTo(pos) < 12) {
                        attackedCrystals.remove(e.getId());
                    }
                });
            }
        }
    }

    private boolean shouldPause() {
        if (pauseOnEat.get() && mc.player.isUsingItem() && mc.player.getActiveItem().getItem() != Items.END_CRYSTAL)
            return true;
        if (pauseOnDrink.get() && mc.player.isUsingItem() && mc.player.getActiveItem().getItem() instanceof PotionItem)
            return true;
        if (!whileMining.get() && mc.interactionManager.isBreakingBlock()) return true;
        return false;
    }

    private void findTargets() {
        targets.clear();

        // Living Entities
        for (Entity entity : mc.world.getEntities()) {
            // Ignore non-living
            if (!(entity instanceof LivingEntity livingEntity)) {
                continue;
            }

            // Player
            if (livingEntity instanceof PlayerEntity player) {
                if (player.getAbilities().creativeMode || livingEntity == mc.player) continue;
                if (!player.isAlive() || !Friends.get().shouldAttack(player)) continue;

                if (ignoreNakeds.get()) {
                    ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
                    ItemStack leggings = player.getEquippedStack(EquipmentSlot.LEGS);
                    ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);
                    ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);

                    if (player.getOffHandStack().isEmpty()
                        && player.getMainHandStack().isEmpty()
                        && boots.isEmpty()
                        && leggings.isEmpty()
                        && chestplate.isEmpty()
                        && helmet.isEmpty()
                    ) continue;
                }
            }

            // Animals, water animals, monsters, bats, misc
            if (!(entities.get().contains(livingEntity.getType()))) continue;

            // Close enough to damage
            if (livingEntity.squaredDistanceTo(mc.player) > targetRange.get() * targetRange.get()) continue;

            targets.add(livingEntity);
        }
    }

    private LivingEntity getNearestTarget() {
        LivingEntity nearestTarget = null;
        double nearestDistance = Double.MAX_VALUE;

        for (LivingEntity target : targets) {
            double distance = PlayerUtils.squaredDistanceTo(target);

            if (distance < nearestDistance) {
                nearestTarget = target;
                nearestDistance = distance;
            }
        }

        return nearestTarget;
    }

    private EndCrystalEntity findBestCrystal() {
        EndCrystalEntity best = null;
        double bestDamage = 0;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity crystal)) continue;
            if (!crystal.isAlive()) continue;

            // Range check
            if (mc.player.getEyePos().distanceTo(Via.getEntityPos(crystal)) > breakRange.get()) continue;

            // Wall check
            if (!canSee(Via.getEntityPos(crystal)) && mc.player.getEyePos().distanceTo(Via.getEntityPos(crystal)) > breakWallRange.get())
                continue;

            // Already attacked check
            if (removeInhibit.get() && attackedCrystals.containsKey(crystal.getId())) {
                long time = attackedCrystals.get(crystal.getId());
                if (System.currentTimeMillis() - time < removeInhibitDelay.get() * 50L) continue;
            }

            // Calculate damage
            double targetDamage = DamageUtils.crystalDamage(target, Via.getEntityPos(crystal));
            double selfDamage = DamageUtils.crystalDamage(mc.player, Via.getEntityPos(crystal));

            if (targetDamage < minDamage.get()) continue;
            if (selfDamage > maxSelfDamage.get()) continue;
            if (antiSuicide.get() && selfDamage >= EntityUtils.getTotalHealth(mc.player)) continue;

            if (targetDamage > bestDamage) {
                best = crystal;
                bestDamage = targetDamage;
            }
        }

        return best;
    }

    private BlockPos findBestPlacePos() {
        BlockPos best = null;
        double bestDamage = 0;

        for (BlockPos pos : WorldUtils.getSphere(placeRange.get())) {
            if (!canPlaceCrystal(pos)) continue;

            // Range check
            if (mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos)) > placeRange.get()) continue;

            // Wall check
            if (!canSee(Vec3d.ofCenter(pos)) && mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos)) > placeWallRange.get())
                continue;

            // Calculate damage
            Vec3d crystalPos = Vec3d.of(pos).add(0.5, 1, 0.5);
            double targetDamage = DamageUtils.crystalDamage(target, crystalPos);
            double selfDamage = DamageUtils.crystalDamage(mc.player, crystalPos);

            if (targetDamage < minDamage.get()) continue;
            if (selfDamage > maxSelfDamage.get()) continue;
            if (antiSuicide.get() && selfDamage >= EntityUtils.getTotalHealth(mc.player)) continue;

            if (armorBreaker.get()) {
                targetDamage += getArmorDamageBonus(target);
            }

            if (targetDamage > bestDamage) {
                best = pos;
                bestDamage = targetDamage;
                renderPos = pos;
                this.renderedDamage = targetDamage;
            }
        }
        return best;
    }

    private double getArmorDamageBonus(LivingEntity player) {
        double bonus = 0;
        for (ItemStack armor : new ItemStack[] {
            player.getEquippedStack(EquipmentSlot.HEAD),
            player.getEquippedStack(EquipmentSlot.CHEST),
            player.getEquippedStack(EquipmentSlot.LEGS),
            player.getEquippedStack(EquipmentSlot.FEET)
        }) {
            if (armor.isEmpty()) continue;
            double durability = (armor.getMaxDamage() - armor.getDamage()) / (double) armor.getMaxDamage() * 100;
            if (durability < armorScale.get()) {
                bonus += 2;
            }
        }
        return bonus;
    }

    private void breakCrystal(EndCrystalEntity crystal) {
        // Anti-weakness
        int slot = findWeaponSlot();
        if (antiWeakness.get() && mc.player.hasStatusEffect(StatusEffects.WEAKNESS)) {
            if (slot != -1) {
                BagUtil.doSwap(slot);
            }
        }
        Vec3d hitPos = Vec3d.ofCenter(crystal.getBlockPos());

        Rotation rotation = new Rotation(((float) Rotations.getYaw(Via.getEntityPos(crystal))),
            (float) Rotations.getPitch(Via.getEntityPos(crystal)));
        boolean registered = RotationManager.getInstance().register(rotation);

        if (registered) {
            attackCrystal(crystal);
        }

        // Restore
        if (antiWeakness.get() && mc.player.hasStatusEffect(StatusEffects.WEAKNESS)) {
            if (slot != -1) {
                BagUtil.doSwap(slot);
            }
        }
    }

    private void attackCrystal(EndCrystalEntity crystal) {
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
        mc.player.swingHand(Hand.MAIN_HAND);
        attackedCrystals.put(crystal.getId(), System.currentTimeMillis());
    }

    private void placeCrystal(BlockPos pos) {
        // Switch to crystal
        int slot = BagUtil.findItemInventorySlot(Items.END_CRYSTAL);
        if (slot == -1) return;

        BagUtil.doSwap(slot);

        // Rotate
        Vec3d vec = Vec3d.ofCenter(pos);

        boolean registered = RotationManager.getInstance().register(new Rotation(vec));
        if (registered) {
            placeBlock(pos);
        }

        // Restore
        BagUtil.doSwap(slot);

    }

    private void placeBlock(BlockPos pos) {
        Direction direction = getPlaceDirection(pos);
        BlockHitResult result = new BlockHitResult(Vec3d.ofCenter(pos), direction, pos, false);

        sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, id));
        mc.player.swingHand(Hand.MAIN_HAND);
        placedCrystals.put(pos, System.currentTimeMillis());
    }

    private Direction getPlaceDirection(BlockPos blockPos) {
        int x = blockPos.getX();
        int y = blockPos.getY();
        int z = blockPos.getZ();
        if (strictDirection.get()) {
            if (mc.player.getY() >= blockPos.getY()) {
                return Direction.UP;
            }
            BlockHitResult result = mc.world.raycast(new RaycastContext(
                mc.player.getEyePos(), new Vec3d(x + 0.5, y + 0.5, z + 0.5),
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE, mc.player));
            if (result != null && result.getType() == HitResult.Type.BLOCK) {
                return result.getSide();
            }
        } else {
            if (mc.world.isInBuildLimit(blockPos)) {
                return Direction.DOWN;
            }
            BlockHitResult result = mc.world.raycast(new RaycastContext(
                mc.player.getEyePos(), new Vec3d(x + 0.5, y + 0.5, z + 0.5),
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE, mc.player));
            if (result != null && result.getType() == HitResult.Type.BLOCK) {
                return result.getSide();
            }
        }
        return Direction.UP;
    }


    private boolean canPlaceCrystal(BlockPos pos) {
        // Check block
        if (!mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN)
            && !mc.world.getBlockState(pos).isOf(Blocks.BEDROCK)) {
            return false;
        }

        // Check space
        BlockPos pos1 = pos.up();

        if (!mc.world.getBlockState(pos1).isAir() && !mc.world.getBlockState(pos1).isOf(Blocks.FIRE)) {
            return false;
        }

        // Check entities
        double d = pos1.getX();
        double e = pos1.getY();
        double f = pos1.getZ();
        Box bb = new Box(0.0, 0.0, 0.0, 1.0, 2.0, 1.0);

        Box box = new Box(d, e, f, d + bb.maxX, e + bb.maxY, f + bb.maxZ);

        return noEntitiesBlockingCrystal(box);
    }


    private boolean noEntitiesBlockingCrystal(Box box) {
        List<Entity> entities = new CopyOnWriteArrayList<>(mc.world.getOtherEntities(null, box));
        if (entities.isEmpty()) {
            return true;
        }

        for (Entity entity : entities) {
            if (entity == null
                || !entity.isAlive()
                || entity instanceof ArmorStandEntity
                || entity instanceof ExperienceOrbEntity
                || entity instanceof ItemEntity && entity.age <= 10) {
                entities.remove(entity);
            } else if (entity instanceof EndCrystalEntity entity1
                && entity1.getBoundingBox().intersects(box)) {

                return false;
            } else {
                entities.remove(entity);
            }
        }
        return entities.isEmpty();
    }

    private boolean canSee(Vec3d pos) {
        return mc.world.raycast(new RaycastContext(
            mc.player.getEyePos(),
            pos,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            mc.player
        )).getType() == HitResult.Type.MISS;
    }

    private int findWeaponSlot() {

        return BagUtil.findItemInventorySlot(stack -> MikuUtil.isSwordItem(stack.getItem())
            || stack.getItem() instanceof MaceItem
            || stack.getItem() instanceof AxeItem);
    }

    private void debug(String message) {
        if (debug.get()) {
            info(message);
        }
    }

    // Enums
    public enum AutoSwitch {
        Normal,
        Silent,
        None
    }

    public enum Support1_12 {
        Full,
        Semi,
        None
    }

    public static enum SupportMode {
        Disabled,
        Accurate,
        Fast;

        private SupportMode() {
        }
    }
}
