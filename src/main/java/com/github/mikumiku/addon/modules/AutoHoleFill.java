package com.github.mikumiku.addon.modules;


import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.BagUtil;
import com.github.mikumiku.addon.util.BaritoneUtil;
import com.github.mikumiku.addon.util.Rotation;
import com.github.mikumiku.addon.util.RotationManager;
import com.github.mikumiku.addon.util.Via;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.stream.Collectors;

public class AutoHoleFill extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlace = settings.createGroup("放置设置");
    private final SettingGroup sgRange = settings.createGroup("范围设置");
    private final SettingGroup sgMisc = settings.createGroup("其他设置");

    // 通用设置
    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("放置延迟")
        .description("每次放置方块之间的延迟时间")
        .defaultValue(50)
        .min(0)
        .max(500)
        .sliderMax(500)
        .build()
    );

    private final Setting<Integer> blocksPer = sgGeneral.add(new IntSetting.Builder()
        .name("每次放置数量")
        .description("每次tick放置的方块数量")
        .defaultValue(1)
        .min(1)
        .max(8)
        .sliderMax(8)
        .build()
    );

    // 放置设置
    private final Setting<Boolean> rotate = sgPlace.add(new BoolSetting.Builder()
        .name("旋转")
        .description("放置时旋转到目标位置")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> packetPlace = sgPlace.add(new BoolSetting.Builder()
        .name("数据包放置")
        .description("使用数据包进行放置")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> breakCrystal = sgPlace.add(new BoolSetting.Builder()
        .name("破坏水晶")
        .description("放置前破坏水晶")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> eatPause = sgPlace.add(new BoolSetting.Builder()
        .name("进食暂停")
        .description("进食时暂停破坏水晶")
        .defaultValue(true)
        .visible(breakCrystal::get)
        .build()
    );

    private final Setting<Boolean> detectMining = sgPlace.add(new BoolSetting.Builder()
        .name("检测挖掘")
        .description("检测正在挖掘的方块")
        .defaultValue(false)
        .build()
    );

    // 范围设置
    private final Setting<Double> placeRange = sgRange.add(new DoubleSetting.Builder()
        .name("放置范围")
        .description("放置方块的最大范围")
        .defaultValue(5)
        .min(0)
        .max(8)
        .sliderMax(8)
        .build()
    );

    private final Setting<Double> enemyRange = sgRange.add(new DoubleSetting.Builder()
        .name("敌人范围")
        .description("检测敌人的范围")
        .defaultValue(6)
        .min(0)
        .max(8)
        .sliderMax(8)
        .build()
    );

    private final Setting<Double> holeRange = sgRange.add(new DoubleSetting.Builder()
        .name("洞穴范围")
        .description("在敌人周围搜索洞穴的范围")
        .defaultValue(2)
        .min(0)
        .max(8)
        .sliderMax(8)
        .build()
    );

    private final Setting<Double> selfRange = sgRange.add(new DoubleSetting.Builder()
        .name("自身范围")
        .description("距离玩家的最小安全距离")
        .defaultValue(2)
        .min(0)
        .max(8)
        .sliderMax(8)
        .build()
    );

    // 其他设置
    private final Setting<Integer> predictTicks = sgMisc.add(new IntSetting.Builder()
        .name("预测刻数")
        .description("预测敌人位置的tick数")
        .defaultValue(1)
        .min(1)
        .max(8)
        .sliderMax(8)
        .build()
    );

    private final Setting<Boolean> usingPause = sgMisc.add(new BoolSetting.Builder()
        .name("使用物品暂停")
        .description("使用物品时暂停填坑")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> inAirPause = sgMisc.add(new BoolSetting.Builder()
        .name("空中暂停")
        .description("在空中时暂停填坑")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> inventory = sgMisc.add(new BoolSetting.Builder()
        .name("背包切换")
        .description("使用背包物品进行切换")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> web = sgMisc.add(new BoolSetting.Builder()
        .name("蜘蛛网优先")
        .description("优先使用蜘蛛网而不是黑曜石")
        .defaultValue(true)
        .build()
    );

    private long lastPlaceTime = 0;
    private int progress = 0;

    public AutoHoleFill() {
        super(BaseModule.CATEGORY_MIKU_COMBAT, "自动填坑", "自动填充敌人周围的基岩坑");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (System.currentTimeMillis() - lastPlaceTime < placeDelay.get()) return;
        progress = 0;

        int block = getBlock();
        if (block == -1) return;

        if (usingPause.get() && mc.player.isUsingItem()) return;
        if (inAirPause.get() && !mc.player.isOnGround()) return;

        List<PlayerEntity> enemies = getEnemies();

        enemies.stream()
            .flatMap(enemy -> getSphere(enemy).stream())
            .filter(this::isValidHole)
            .distinct()
            .forEach(this::tryPlaceBlock);
    }

    private void tryPlaceBlock(BlockPos pos) {
        if (pos == null) return;
        if (progress >= blocksPer.get()) return;

        int block = getBlock();
        if (block == -1) return;

        if (!canPlace(pos)) return;

        if (breakCrystal.get()) {
            attackCrystal(pos);
        } else if (hasEntity(pos)) return;


        placeBlock(pos);

        progress++;
        lastPlaceTime = System.currentTimeMillis();
    }

    private int getBlock() {
        if (web.get()) {
            int webSlot = BagUtil.findItemInventorySlot(Blocks.COBWEB.asItem());
            if (webSlot != -1) return webSlot;
        }

        return BagUtil.findItemInventorySlot(Blocks.OBSIDIAN.asItem());
    }

    private List<PlayerEntity> getEnemies() {
        return mc.world.getPlayers().stream()
            .filter(player -> player != mc.player)
            .filter(player -> !player.isDead())
            .filter(player -> player.distanceTo(mc.player) <= enemyRange.get())
            .collect(Collectors.toList());
    }

    private List<BlockPos> getSphere(PlayerEntity player) {
        Vec3d predictedPos = predictPosition(player);
        return getSphereBlocks(predictedPos, holeRange.get());
    }

    public List<BlockPos> getSphereBlocks(Vec3d center, double radius) {
        List<BlockPos> sphere = new java.util.ArrayList<>();
        BlockPos centerPos = BlockPos.ofFloored(center);
        int radiusInt = (int) Math.ceil(radius);

        for (int x = -radiusInt; x <= radiusInt; x++) {
            for (int y = -radiusInt; y <= radiusInt; y++) {
                for (int z = -radiusInt; z <= radiusInt; z++) {
                    BlockPos pos = centerPos.add(x, y, z);
                    if (center.distanceTo(pos.toCenterPos()) <= radius) {
                        sphere.add(pos);
                    }
                }
            }
        }

        return sphere;
    }

    private Vec3d predictPosition(PlayerEntity player) {
        Vec3d velocity = new Vec3d(
            player.getX() - player.lastRenderX,
            player.getY() - player.lastRenderY,
            player.getZ() - player.lastRenderZ
        );
        return Via.getEntityPos(player).add(velocity.multiply(predictTicks.get()));
    }

    private boolean isValidHole(BlockPos pos) {
        if (pos.toCenterPos().distanceTo(Via.getEntityPos(mc.player)) <= selfRange.get()) return false;
        return isHole(pos);
    }

    private boolean isHole(BlockPos pos) {
        if (!mc.world.getBlockState(pos).isAir()) return false;
        return mc.world.getBlockState(pos.down()).isSolidBlock(mc.world, pos.down());
    }

    private boolean canPlace(BlockPos pos) {
        return Via.getEntityPos(mc.player).distanceTo(pos.toCenterPos()) <= placeRange.get() &&
            mc.world.getBlockState(pos).isReplaceable();
    }

    private boolean hasEntity(BlockPos pos) {
        return !mc.world.getOtherEntities(null,
            new net.minecraft.util.math.Box(pos)).isEmpty();
    }

    private void attackCrystal(BlockPos pos) {

        for (Entity entity : mc.world.getEntities()) {
            if (entity == null) continue;
            if (!entity.getBlockPos().equals(pos)) continue;
            if (entity instanceof EndCrystalEntity) {
                RotationManager.getInstance().register(new Rotation((float) Rotations.getYaw(entity), (float) Rotations.getPitch(entity)));
                mc.interactionManager.attackEntity(mc.player, entity);
                mc.player.swingHand(Hand.MAIN_HAND);
            }

            // 找到第一个符合条件的实体后执行操作并退出循环

            break; // 模拟 findFirst() 的行为：只处理第一个匹配项
        }


    }

    private void placeBlock(BlockPos pos) {

        int slot = BagUtil.findItemInventorySlot(Blocks.OBSIDIAN.asItem());
        if (slot == -1) {
            return;
        }
        BagUtil.doSwap(slot);
        BaritoneUtil.placeBlock(pos);
        BagUtil.doSwap(slot);
    }
}
