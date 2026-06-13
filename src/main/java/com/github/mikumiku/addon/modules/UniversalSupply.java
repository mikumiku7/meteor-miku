package com.github.mikumiku.addon.modules;


import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.utils.BetterBlockPos;
import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.BaritoneUtil;
import com.github.mikumiku.addon.util.ChatUtils;
import com.github.mikumiku.addon.util.MikuUtil;
import com.github.mikumiku.addon.util.Via;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFlightModes;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.github.mikumiku.addon.modules.ShulkerBoxItemFetcher.containsTargetItem;

/**
 * 通用物资补给
 * 1.末地pitch40飞行中，检测到背包所有鞘翅耐久度不足，吃紫颂果降落，从末影箱拿出鞘翅盒或烟花盒，更换鞘翅补充烟花，重新利用烟花起飞，到达指定高度后，重新对着原方向继续pitch40。
 * 2.地狱男中音对着指定坐标赶路时，检测到背包中鞘翅or烟花数量不足，降落至一块非岩浆非玄武岩三角洲区域，降落补物资（需防止恶魂炸掉物资潜影盒），补好物资后重新起飞，启动男中音继续对着指定坐标赶路。
 * 3.主世界pitch40或者spiral（jefff mod）中，检测到背包所有鞘翅耐久度不足，吃紫颂果降落，利用烟花向天空飞至200格位置，airplace搭建平台（防止主世界是晚上的时候因为怪物导致死亡），放置末影箱，从末影箱拿出鞘翅盒或烟花盒，更换鞘翅，重新利用烟花起飞，继续spiral或者原方向pitch40
 * 4.补充图腾 XP
 */
public class UniversalSupply extends BaseModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgEnd = settings.createGroup("末地设置");
    private final SettingGroup sgNether = settings.createGroup("地狱设置");
    private final SettingGroup sgOverworld = settings.createGroup("主世界设置");

    // 通用设置
    private final Setting<Integer> elytraDurabilityThreshold = sgGeneral.add(new IntSetting.Builder()
        .name("鞘翅耐久阈值")
        .description("低于此耐久度时触发补给")
        .defaultValue(10)
        .min(1)
        .max(432)
        .sliderMax(432)
        .build()
    );

    private final Setting<Integer> fireworkThreshold = sgGeneral.add(new IntSetting.Builder()
        .name("烟花数量阈值")
        .description("低于此数量时触发补给")
        .defaultValue(20)
        .min(1)
        .max(128)
        .sliderMax(128)
        .build()
    );

    private final Setting<Integer> totemThreshold = sgGeneral.add(new IntSetting.Builder()
        .name("图腾补充数量")
        .description("低于此数量时补给到数量")
        .defaultValue(4)
        .min(1)
        .max(30)
        .sliderMax(30)
        .build()
    );

    private final Setting<Integer> xpThreshold = sgGeneral.add(new IntSetting.Builder()
        .name("xp补充数量(组)")
        .description("低于此数量时补给到数量")
        .defaultValue(2)
        .min(0)
        .max(30)
        .sliderMax(30)
        .build()
    );
    //鞘翅补充数量
    private final Setting<Integer> elytraThreshold = sgGeneral.add(new IntSetting.Builder()
        .name("鞘翅补充数量")
        .description("低于此数量时补给到数量")
        .defaultValue(2)
        .min(0)
        .max(30)
        .sliderMax(30)
        .build()
    );
    //紫菘果补充数量
    private final Setting<Integer> chorusThreshold = sgGeneral.add(new IntSetting.Builder()
        .name("紫菘果补充组数")
        .description("低于此数量时补给到数量")
        .defaultValue(1)
        .min(0)
        .max(30)
        .sliderMax(30)
        .build()
    );
    private final Setting<Integer> takingOffPitch = sgGeneral.add(new IntSetting.Builder()
        .name("起飞角度")
        .description("建议70")
        .defaultValue(70)
        .min(0)
        .sliderRange(0, 90)
        .build()
    );


    private final Setting<LandingMode> landingMode = sgGeneral.add(new EnumSetting.Builder<LandingMode>()
        .name("降落方法")
        .description(" 。")
        .defaultValue(LandingMode.Chorus)
        .onChanged(UniversalSupply::onLandingChange)
        .build()
    );


    /**
     * 空气放置功能
     */
    private final Setting<Boolean> airPlace = sgGeneral.add(
        new BoolSetting.Builder()
            .name("空气放置")
            .description("允许在空气中放置方块，无视放置限制。并且方向可能不精准。需要服务器支持, 已知支持: org, cc 不支持: 3c")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> endTargetHeight = sgGeneral.add(new IntSetting.Builder()
        .name("目标高度")
        .description("补给后重新飞行的目标高度")
        .defaultValue(300)
        .min(10)
        .sliderMax(5000)
        .build()
    );

    // 地狱设置
    Setting<Boolean> protect = sgNether.add(new BoolSetting.Builder()
        .name("防恶魂火球")
        .description("防恶魂火球， 自动围墙")
        .defaultValue(false)
        .build()
    );

    Setting<Boolean> ta = sgNether.add(new BoolSetting.Builder()
        .name("坐标不用管会自动更新")
        .description("防恶魂火球， 自动围墙")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> netherTargetX = sgNether.add(new IntSetting.Builder()
        .name("地狱目标X坐标")
        .description("男中音目标X坐标")
        .defaultValue(0)
        .build()
    );

    private final Setting<Integer> netherTargetZ = sgNether.add(new IntSetting.Builder()
        .name("地狱目标Z坐标")
        .description("男中音目标Z坐标")
        .defaultValue(0)
        .build()
    );

    private final Setting<Integer> netherSafeRadius = sgNether.add(new IntSetting.Builder()
        .name("地狱安全半径")
        .description("检测安全降落区域的半径")
        .defaultValue(10)
        .min(5)
        .max(20)
        .sliderMax(20)
        .build()
    );

    // 主世界设置
    private final Setting<Integer> overworldPlatformHeight = sgOverworld.add(new IntSetting.Builder()
        .name("主世界平台高度")
        .description("补给平台搭建高度")
        .defaultValue(200)
        .min(150)
        .max(300)
        .sliderMax(300)
        .build()
    );

    private final Setting<Integer> overworldPlatformSize = sgOverworld.add(new IntSetting.Builder()
        .name("平台大小")
        .description("补给平台的边长")
        .defaultValue(5)
        .min(3)
        .max(10)
        .sliderMax(10)
        .build()
    );

    // 状态变量
    private SupplyState state = SupplyState.IDLE;
    private Vec3d originalDirection;
    private double originalYaw;
    private double originalPitch;
    private BlockPos platformCenter;
    private int retakeoffTicks = 0;
    public static long lastFireworkTime = 0;

    private boolean resumeBaritone = false; // 是否放置了新的末影箱
    BlockPos currentDestination = null;

    boolean eatting = false;

    public UniversalSupply() {
        super("赶路助手", "通用全自动赶路+物资补给系统. 自动处理意外落地.");
    }

    private static void onLandingChange(LandingMode v) {
        if (v != LandingMode.Chorus) {
            ChatUtils.info("当前只支持紫菘果降落");
        }

    }

    @Override
    public void onActivate() {
        super.onActivate();
        if (!MikuUtil.isClassExists("com.stash.hunt.modules.Pitch40Util")) {
            error("未找到jeff mod，请检查是否正确安装");
//            toggle();
            return;
        }

        checkPitchActive();
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        state = SupplyState.IDLE;

    }

    private void checkPitchActive() {

//        Pitch40Util pitch40Util = Modules.get().get(Pitch40Util.class);

    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        switch (state) {
            case IDLE:
                if (Via.isFallFlying(mc)) {
                    checkSupplyNeeded();
                    return;
                }
                break;
            case DESCENDING:
                handleDescending();
                break;
            case LANDING:
                handleLanding();
                break;
            case USE_XP:
                handleResupplying();
                break;
            case AWAIT_XP:
                awaitXP();
                break;
            case PLACE_ENDERCHEST:
                placeEnderChest();
                break;
            case OPEN_ENDERCHEST:
                openEnderChest();
                break;
            case AWAIT_OPEN_ENDERCHEST:
                handleAwaitOpenEnderchest();
                break;
            case CLOSING_CONTAINER:
                handleClosingContainer();
                break;
            case FETCH_ITEMS:
                handleFetchItems();
                break;
            case SAVING_BOX:
                handleSavingBox();
                break;
            case PLACE_SAVE_ENDERCHEST:
                placeSaveEnderChest();
                break;
            case OPEN_SAVE_ENDERCHEST:
                openSaveEnderChest();
                break;
            case AWAIT_OPEN_SAVE_ENDERCHEST:
                handleAwaitOpenSaveEnderchest();
                break;
            case CLOSE_SAVE_ENDERCHEST:
                handleCloseSaveEnderchest();
                break;
            case BREAK_SAVE_ENDERCHEST:
                breakSaveEnderChest();
                break;
            case TAKING_OFF:
                handleTakingOff();
                break;
            case RESUMING:
                handleResuming();
                break;
        }
    }

    private void handleAwaitOpenEnderchest() {

        // Check if container screen is open
        if (mc.currentScreen == null) {
            info("等待容器界面打开... (界面为空)");

            return;
        }

        if (!(mc.currentScreen instanceof HandledScreen)) {
            info("等待容器界面打开... (当前界面: " + mc.currentScreen.getClass().getSimpleName() + ")");
            return;
        }


        HandledScreen<?> containerScreen = (HandledScreen<?>) mc.currentScreen;
        var screenHandler = containerScreen.getScreenHandler();

        info("容器界面已打开: " + containerScreen.getClass().getSimpleName());

        // Count empty slots in player inventory (excluding hotbar slot 0 which we want to keep)
        int emptySlots = ShulkerBoxItemFetcher.countEmptyInventorySlots();
        if (emptySlots <= 1) {
            info("库存空间不足，至少保留一格空位");
            state = (SupplyState.CLOSING_CONTAINER);
            return;
        }

        // 计算需要补充的物品
        Map<Item, Integer> neededItems = calculateNeededItems();
        if (neededItems.isEmpty()) {
            info("所有物品数量充足，无需补充");
            state = SupplyState.CLOSING_CONTAINER;
            return;
        }

        // 显示需要补充的物品
        StringBuilder neededInfo = new StringBuilder("需要补充的物品: ");
        for (Map.Entry<Item, Integer> entry : neededItems.entrySet()) {
            neededInfo.append(entry.getKey().getName().getString()).append(" x").append(entry.getValue()).append(", ");
        }
        info(neededInfo.toString());

        boolean foundItems = false;
        int containerSlots = screenHandler.slots.size() - 36; // Container slots only (excluding player inventory)

        info("容器槽位数: " + containerSlots + ", 总槽位数: " + screenHandler.slots.size());

        // 遍历容器中的潜影盒
        for (int i = 0; i < containerSlots; i++) {
            ItemStack stack = screenHandler.getSlot(i).getStack();
            if (stack.isEmpty()) {
                continue;
            }
            info("槽位 " + i + ": " + stack.getItem().getName().getString() + " x" + stack.getCount());

            // 检查是否为潜影盒
            if (stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                if (block instanceof ShulkerBoxBlock) {
                    // 检查潜影盒中是否包含需要补充的物品
                    boolean containsNeededItem = false;
                    for (Item neededItem : neededItems.keySet()) {
                        if (containsTargetItem(stack, neededItem)) {
                            containsNeededItem = true;
                            break;
                        }
                    }

                    if (containsNeededItem) {
                        info("发现包含目标物品的潜影盒，开始提取");
                        // 点击提取潜影盒
                        mc.interactionManager.clickSlot(
                            screenHandler.syncId,
                            i,
                            0,
                            SlotActionType.QUICK_MOVE,
                            mc.player
                        );
                        foundItems = true;
                        info("提取了 " + stack.getCount() + " 个 " + stack.getName().getString());

                        // 检查库存空间
                        emptySlots = ShulkerBoxItemFetcher.countEmptyInventorySlots();
                        if (emptySlots <= 1) {
                            info("库存空间不足，停止提取");
                            break;
                        }

                        // 重新计算还需要补充的物品
                        neededItems = calculateNeededItems();
                        if (neededItems.isEmpty()) {
                            info("物品补充完成");
                            break;
                        }
                    }
                }
            }
        }

        // 如果没有找到需要的物品，关闭容器
        if (!foundItems) {
            info("末影箱中没有找到需要的目标物品");
        } else if (neededItems.isEmpty()) {
            // 所有物品补充完成
            info("所有物品补充完成，关闭容器");
        }

        if (mc.currentScreen != null) {
            mc.player.closeScreen();
        }

        state = SupplyState.CLOSING_CONTAINER;
    }

    private void awaitXP() {


        AutoXP xp = Modules.get().get(AutoXP.class);
        if (!xp.isActive()) {
            info("维修完成，下一步");
            state = SupplyState.PLACE_ENDERCHEST;
        }
    }

    private void checkSupplyNeeded() {
        if (needsElytraRepair() || needsFireworks()) {
            if (!Via.isFallFlying(mc)) return;

            saveCurrentState();
            info("检测到需要补给，开始降落流程");
            state = SupplyState.DESCENDING;
        }
    }

    private boolean needsElytraRepair() {
        int elytraCount = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            var stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.ELYTRA) {
                elytraCount++;
                int durability = stack.getMaxDamage() - stack.getDamage();
                if (durability > elytraDurabilityThreshold.get()) {
                    return false;
                }
            }
        }
        if (elytraCount == 0) {
            return true;
        }
        return true;
    }

    private boolean needsFireworks() {
        int fireworkCount = MikuUtil.countItem(Items.FIREWORK_ROCKET);
        return fireworkCount < fireworkThreshold.get();
    }

    private void saveCurrentState() {
        originalYaw = mc.player.getYaw();
        originalPitch = mc.player.getPitch();
        originalDirection = mc.player.getRotationVec(1.0f);

        if (BaritoneAPI.getProvider().getPrimaryBaritone().getElytraProcess().isActive()) {
            resumeBaritone = true;
            currentDestination = BaritoneAPI.getProvider().getPrimaryBaritone().getElytraProcess().currentDestination();
        } else {
            resumeBaritone = false;
        }

    }

    private void handleDescending() {

        if (mc.player.isOnGround()) {
            info("已降落，准备补给");
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();

            state = SupplyState.LANDING;
            return;
        }

        // 吃紫颂果降落
        AutoChorusFruit autoChorusFruit = Modules.get().get(AutoChorusFruit.class);
        if (!autoChorusFruit.isActive()) {
            autoChorusFruit.toggle();
        }

//        if (getCurrentDimension() == Dimension.End ||
//            getCurrentDimension() == Dimension.Overworld) {
//            useChorusFruit();
//
//        } else if (getCurrentDimension() == Dimension.Nether) {
//            updateTagetPos();
//
//            findSafeLandingSpot();
//        }

    }

    private void handleLanding() {
        //拉方块中心
        AutoBlockCenter autoBlockCenter = Modules.get().get(AutoBlockCenter.class);

        if (autoBlockCenter.isPlayerCentered()) {
            state = SupplyState.USE_XP;
        } else {
            if (!autoBlockCenter.isActive()) {
                autoBlockCenter.toggle();
            }
        }


//        if (getCurrentDimension() == Dimension.Overworld) {
//            // 主世界需要先飞到200格高度搭建平台
//            useFirework();
//            if (mc.player.getY() >= overworldPlatformHeight.get()) {
//                buildPlatform();
//                state = SupplyState.RESUPPLYING;
//            }
//        } else if (getCurrentDimension() == Dimension.Nether) {
//            // 地狱需要防止恶魂炸掉潜影盒
//            protectShulkerBox();
//            state = SupplyState.RESUPPLYING;
//        } else {
//            // 末地直接补给
//            state = SupplyState.RESUPPLYING;
//        }
    }

    private void handleResupplying() {

        AutoXP xp = Modules.get().get(AutoXP.class);
        if (!xp.isActive()) {
            xp.toggle();
        }
        info("丢XP");

        state = SupplyState.AWAIT_XP;

        // 1. 放置末影箱
//        placeEnderChest();
//
//        // 2. 打开末影箱，取出鞘翅盒/烟花盒
//        openEnderChest();
//
//        // 3. 更换鞘翅
//        if (needsElytraRepair()) {
//            replaceElytra();
//        }
//
//        // 4. 补充烟花
//        if (needsFireworks()) {
//            refillFireworks();
//        }
//
//        // 5. 收回末影箱
//        breakEnderChest();
//
//        state = SupplyState.TAKING_OFF;
//        info("补给完成，准备起飞");
    }

    private void handleTakingOff() {


        if (mc.player.isOnGround()) {

            Rotations.rotate(
                mc.player.getYaw(),
                takingOffPitch.get() * -1,
                50,
                () -> {

                    mc.player.jump(); // 模拟跳跃
                    new Thread(() -> {
                        try {
                            Thread.sleep(200); // 延迟 150 毫秒，大概 2 tick
                        } catch (InterruptedException ignored) {
                        }
                        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                        useFirework();

                        lastFireworkTime = System.currentTimeMillis();
                    }).start();

                }
            );

            return;
        }

        int targetHeight = switch (getCurrentDimension()) {
            case Dimension.End -> endTargetHeight.get();
            case Dimension.Overworld -> endTargetHeight.get();
            case Dimension.Nether -> (int) mc.player.getY();
        };

        if (mc.player.getY() >= targetHeight) {
            state = SupplyState.RESUMING;
            retakeoffTicks = 0;
            info("到达目标高度，恢复原飞行状态");
        }
    }

    public void handleResuming() {
        // 恢复原来的飞行方向和角度
        mc.player.setYaw((float) originalYaw);

        if (getCurrentDimension() == Dimension.End) {
//            mc.player.setPitch((float) endPitch.get());
        } else if (getCurrentDimension() == Dimension.Overworld) {
            mc.player.setPitch((float) originalPitch);
        } else if (getCurrentDimension() == Dimension.Nether) {
            // 重新启动男中音朝向目标坐标
            alignToTarget(netherTargetX.get(), netherTargetZ.get());
            BaritoneAPI.getProvider().getPrimaryBaritone().getElytraProcess().pathTo(currentDestination);
        }

        if (resumeBaritone) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getElytraProcess().pathTo(currentDestination);
        }

        retakeoffTicks++;
        if (retakeoffTicks > 20) { // 1秒后恢复IDLE状态
            state = SupplyState.IDLE;
            info("已恢复飞行状态");
        }
    }

    // 辅助方法
    private Dimension getCurrentDimension() {

        return PlayerUtils.getDimension();
    }

    private void useChorusFruit() {
        // 实现吃紫颂果逻辑
        findAndUseItem(Items.CHORUS_FRUIT);
    }

    private void useFirework() {
        // 实现使用烟花逻辑
        findAndUseItem(Items.FIREWORK_ROCKET);
    }

    private void updateTagetPos() {
        // 实现使用烟花逻辑
        if (BaritoneAPI.getProvider().getPrimaryBaritone().getElytraProcess().isActive()) {
            Goal goal = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getGoal();
            if (goal instanceof GoalXZ goalXZ) {
                netherTargetX.set(goalXZ.getX());
                netherTargetZ.set(goalXZ.getZ());
            }
            if (goal instanceof GoalBlock goalBlock) {
                netherTargetX.set(goalBlock.x);
                netherTargetZ.set(goalBlock.z);
            }
            if (goal instanceof GoalNear goalBlock) {
                netherTargetX.set(goalBlock.getGoalPos().getX());
                netherTargetZ.set(goalBlock.getGoalPos().getZ());
            }
        }

    }

    private void findAndUseItem(Item item) {
        // 查找并使用物品的实现
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                Via.setSelectedSlot(i);
                mc.interactionManager.interactItem(mc.player, mc.player.getActiveHand());
                return;
            }
        }
    }

    private void findSafeLandingSpot() {
        // 地狱寻找安全降落点（非岩浆、非玄武岩三角洲）
        // 实现安全区域检测逻辑

        useChorusFruit();

    }

    private void buildPlatform() {
        // 在主世界搭建补给平台
        platformCenter = mc.player.getBlockPos();
        int size = overworldPlatformSize.get();
        // 实现平台搭建逻辑
    }

    private void protectShulkerBox() {
        // 地狱防止恶魂炸掉潜影盒
        // 可以搭建简单保护结构
        if (!protect.get()) {
            return;
        }

        Vec3d playerEyePos = mc.player.getCameraPosVec(1.0f);
        double detectRange = 32.0; // 恶魂最大攻击距离（大约30格）
        boolean danger = false;
        Entity targetGhast = null;

        // 遍历附近的恶魂
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof GhastEntity ghast) {
                if (ghast.squaredDistanceTo(mc.player) > detectRange * detectRange) continue;


                // 恶魂的眼睛高度
                Vec3d ghastEyePos = ghast.getCameraPosVec(1.0f);

                // 射线检测：恶魂 -> 玩家
                BlockHitResult hit = mc.world.raycast(new RaycastContext(
                    ghastEyePos,
                    playerEyePos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    ghast
                ));

                // 如果射线命中结果是玩家（或者没有阻挡）
                if (hit.getType() == HitResult.Type.MISS) {
                    danger = true;
                    targetGhast = ghast;
                    break;
                }
            }
        }
        // 2️⃣ 若检测到危险恶魂
        if (targetGhast != null) {
            Vec3d ghastPos = Via.getEntityPos(targetGhast);
            Vec3d playerPos = Via.getEntityPos(mc.player);

            // 计算玩家与恶魂的中点方向
            Vec3d direction = ghastPos.subtract(playerPos).normalize();
            Vec3d wallCenter = playerPos.add(direction.multiply(3.0)); // 距离玩家 3 格处建墙

            BlockPos wallBase = BlockPos.ofFloored(wallCenter);
            List<BlockPos> wallBlocks = new ArrayList<>();

            // 3️⃣ 建造一面竖直的 3×3 墙（中间对准视线方向）
            Direction side = getHorizontalDirectionFromVector(direction);
            for (int y = 0; y < 3; y++) {
                for (int w = -1; w <= 1; w++) {
                    BlockPos offset;
                    if (side == Direction.NORTH || side == Direction.SOUTH) {
                        offset = wallBase.add(w, y, 0);
                    } else {
                        offset = wallBase.add(0, y, w);
                    }
                    wallBlocks.add(offset);
                }
            }
            ChatUtils.sendMsg(Text.literal("⚠ 检测到恶魂正在注视你，已自动搭建保护结构。"));

            // 4️⃣ 放置方块
            for (BlockPos pos : wallBlocks) {
                if (mc.world.isAir(pos)) {
                    BaritoneUtil.airPlaceBlock(pos);
                }
            }
        }
    }

    private Direction getHorizontalDirectionFromVector(Vec3d dir) {
        double absX = Math.abs(dir.x);
        double absZ = Math.abs(dir.z);
        if (absX > absZ) {
            return dir.x > 0 ? Direction.EAST : Direction.WEST;
        } else {
            return dir.z > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    private void placeEnderChest() {
        // 放置末影箱

        BlockPos pos = MikuUtil.findSuitablePlacePosition();
        if (pos != null) {
            BaritoneUtil.placeItem(pos, Items.ENDER_CHEST);
            ChatUtils.sendMsg(Text.literal("已放置末影箱"));
            state = SupplyState.OPEN_ENDERCHEST;
        }
    }

    private boolean openEnderChest() {
        BlockPos targetChest = getBlockPos(Blocks.ENDER_CHEST);
        if (targetChest == null) {
            ChatUtils.sendMsg(Text.literal("附近没有末影箱（4格内）, 等待"));
//            state = SupplyState.PLACE_ENDERCHEST;
            return false;
        }

        // 如果找到末影箱，就模拟右键打开
        if (targetChest != null) {
            BaritoneUtil.clickBlock(targetChest, Direction.UP, true, Hand.MAIN_HAND, BaritoneUtil.SwingSide.All);
            state = SupplyState.OPEN_ENDERCHEST;
            return true;

        }

        return false;
    }


    private void replaceElytra() {
        // 更换鞘翅
    }

    private void refillFireworks() {
        // 补充烟花
        ShulkerBoxItemFetcher fetcher = Modules.get().get(ShulkerBoxItemFetcher.class);
        if (!fetcher.isActive()) {
            fetcher.targetItem.set(Items.FIREWORK_ROCKET);
            fetcher.toggle();
        }
    }


    private boolean isNight() {
        long timeOfDay = mc.world.getTimeOfDay() % 24000;
        return timeOfDay >= 13000 && timeOfDay <= 23000;
    }

    private boolean isPitch40Flying() {
        ElytraFly pitch40 = Modules.get().get(ElytraFly.class);
        if (pitch40 == null) return false;
        if (!pitch40.isActive()) return false;
        if (pitch40.flightMode.get() == ElytraFlightModes.Pitch40) {
            return true;
        }
        return false;
    }

    private boolean isSafeLocation(BlockPos pos) {
        // 检查脚下和周围方块
        Block below = mc.world.getBlockState(pos.down()).getBlock();

        // 下界特殊检查
        if (MikuUtil.isInNether()) {
            if (below == Blocks.LAVA || below == Blocks.MAGMA_BLOCK) return false;

            // 检查是否在玄武岩三角洲
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Block block = mc.world.getBlockState(pos.add(x, -1, z)).getBlock();
                    if (block == Blocks.BASALT || block == Blocks.BLACKSTONE) {
                        return false; // 避免玄武岩三角洲
                    }
                }
            }
        }

        return below != Blocks.AIR && below != Blocks.LAVA;
    }

    private boolean breakEnderChest() {
        // 破坏并收回末影箱
        BlockPos targetChest = getBlockPos(Blocks.ENDER_CHEST);
        if (targetChest == null) return false;
        if (targetChest != null) {
            BetterBlockPos betterBlockPos = BetterBlockPos.from(targetChest);
            BaritoneAPI.getProvider().getPrimaryBaritone().getSelectionManager().addSelection(betterBlockPos, betterBlockPos);
            BaritoneAPI.getProvider().getPrimaryBaritone().getBuilderProcess().clearArea(betterBlockPos, betterBlockPos);

            return true;
        }

        return false;
    }

    private @Nullable BlockPos getBlockPos(Block block) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return null;

        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos targetChest = null;
        double closestDistance = 4.0; // 搜索范围（方块距离）

        // 遍历周围 4 格的方块
        for (int x = -4; x <= 4; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (mc.world.getBlockState(pos).getBlock() == block) {
                        double distance = mc.player.squaredDistanceTo(
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5
                        );
                        if (distance <= closestDistance * closestDistance) {
                            targetChest = pos;
                            closestDistance = Math.sqrt(distance);
                        }
                    }
                }
            }
        }
        return targetChest;
    }

    private void alignToTarget(double targetX, double targetZ) {
        // 对准目标坐标（男中音）
        double deltaX = targetX - mc.player.getX();
        double deltaZ = targetZ - mc.player.getZ();
        double yaw = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90;
        mc.player.setYaw((float) yaw);
    }

    /**
     * 统计玩家库存中特定物品的数量（包括潜影盒内的物品），并返回需要补充的数量
     *
     * @return Map<Item, Integer> 需要补充的物品及其数量
     */
    private Map<Item, Integer> calculateNeededItems() {
        Map<Item, Integer> currentCount = new LinkedHashMap<>();
        Map<Item, Integer> neededItems = new LinkedHashMap<>();

        // 统计当前库存中的物品数量（包括潜影盒内的物品）
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                Item item = stack.getItem();

                // 对于鞘翅，只计算耐久度高于阈值的
                if (item == Items.ELYTRA) {
                    int durability = stack.getMaxDamage() - stack.getDamage();
                    if (durability > elytraDurabilityThreshold.get()) {
                        currentCount.put(item, currentCount.getOrDefault(item, 0) + stack.getCount());
                    }
                } else {
                    currentCount.put(item, currentCount.getOrDefault(item, 0) + stack.getCount());
                }

                // 如果是潜影盒，还要统计其中的物品
                if (item instanceof BlockItem blockItem) {
                    Block block = blockItem.getBlock();
                    if (block instanceof ShulkerBoxBlock) {
                        countShulkerBoxContents(stack, currentCount);
                    }
                }
            }
        }

        // 计算烟花需要补充的数量
        int currentFireworks = currentCount.getOrDefault(Items.FIREWORK_ROCKET, 0);
        if (currentFireworks < fireworkThreshold.get()) {
            neededItems.put(Items.FIREWORK_ROCKET, fireworkThreshold.get() - currentFireworks);
        }

        // 计算图腾需要补充的数量
        int currentTotems = currentCount.getOrDefault(Items.TOTEM_OF_UNDYING, 0);
        if (currentTotems < totemThreshold.get()) {
            neededItems.put(Items.TOTEM_OF_UNDYING, totemThreshold.get() - currentTotems);
        }

        // 计算XP需要补充的数量（按组计算，每组64个）
        int currentXP = currentCount.getOrDefault(Items.EXPERIENCE_BOTTLE, 0);
        int neededXPGroups = xpThreshold.get();
        int currentXPGroups = divideBy64Ceiling(currentXP);
        if (currentXPGroups < neededXPGroups) {
            neededItems.put(Items.EXPERIENCE_BOTTLE, neededXPGroups - currentXPGroups);
        }

        // 计算鞘翅需要补充的数量
        int currentElytras = currentCount.getOrDefault(Items.ELYTRA, 0);
        if (currentElytras < elytraThreshold.get()) {
            neededItems.put(Items.ELYTRA, elytraThreshold.get() - currentElytras);
        }

        // 计算紫颂果需要补充的数量（按组计算，每组64个）
        int currentChorus = currentCount.getOrDefault(Items.CHORUS_FRUIT, 0);
        int neededChorusGroups = chorusThreshold.get();
        int currentChorusGroups = divideBy64Ceiling(currentChorus);
        if (currentChorusGroups < neededChorusGroups) {
            neededItems.put(Items.CHORUS_FRUIT, neededChorusGroups - currentChorusGroups);
        }


        return neededItems;
    }

    /**
     * 统计潜影盒内的物品数量（使用 1.21+ Data Components API）
     *
     * @param shulkerBoxStack 潜影盒物品栈
     * @param currentCount    当前物品计数Map
     */
    private void countShulkerBoxContents(ItemStack shulkerBoxStack, Map<Item, Integer> currentCount) {
        // 使用 1.21+ 的 Data Components API 获取潜影盒内容
        ContainerComponent container = shulkerBoxStack.get(DataComponentTypes.CONTAINER);
        if (container == null) return;

        // 遍历潜影盒中的所有物品
        for (ItemStack stack : container.stream().toList()) {
            if (stack.isEmpty()) continue;

            Item item = stack.getItem();

            // 对于鞘翅，只计算耐久度高于阈值的
            if (item == Items.ELYTRA) {
                int durability = stack.getMaxDamage() - stack.getDamage();
                if (durability > elytraDurabilityThreshold.get()) {
                    currentCount.put(item, currentCount.getOrDefault(item, 0) + stack.getCount());
                }
            } else {
                // 其他物品直接计数
                currentCount.put(item, currentCount.getOrDefault(item, 0) + stack.getCount());
            }
        }
    }

    /**
     * 处理从潜影盒提取物品的逻辑
     */
    private void handleFetchItems() {
        // 计算主背包中缺失的物品（不包括潜影盒中的）
        Map<Item, Integer> neededItems = calculateNeededItemsFromInventoryOnly();

        if (neededItems.isEmpty()) {
            info("主背包物品已充足，准备回收盒子");
            state = SupplyState.SAVING_BOX;
            return;
        }

        // 显示需要补充的物品
        StringBuilder neededInfo = new StringBuilder("需要从潜影盒提取: ");
        for (Map.Entry<Item, Integer> entry : neededItems.entrySet()) {
            neededInfo.append(entry.getKey().getName().getString()).append(" x").append(entry.getValue()).append(", ");
        }
        info(neededInfo.toString());

        // 获取 ShulkerBoxItemFetcher 模块
        ShulkerBoxItemFetcher fetcher = Modules.get().get(ShulkerBoxItemFetcher.class);
        if (fetcher == null) {
            info("找不到 ShulkerBoxItemFetcher 模块");
            state = SupplyState.SAVING_BOX;
            return;
        }

        // 检查当前是否正在提取物品
        if (fetcher.isActive()) {
            // 正在提取中，等待完成
            return;
        }

        // 开始提取第一个需要的物品
        Item firstNeededItem = neededItems.keySet().iterator().next();
        int neededCount = neededItems.get(firstNeededItem);

        // 设置目标物品并启动提取
        fetcher.targetItem.set(firstNeededItem);
        fetcher.extractAmount.set(neededCount);
        fetcher.toggle();

        info("开始提取 " + firstNeededItem.getName().getString() + " x" + " (" + neededCount + " 组)");
    }

    /**
     * 只统计主背包中的物品数量（不包括潜影盒中的），并返回需要补充的数量
     *
     * @return Map<Item, Integer> 需要补充的物品及其数量
     */
    private Map<Item, Integer> calculateNeededItemsFromInventoryOnly() {
        Map<Item, Integer> currentCount = new HashMap<>();
        Map<Item, Integer> neededItems = new LinkedHashMap<>();

        // 只统计主背包中的物品数量（不包括潜影盒）
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                Item item = stack.getItem();

                // 跳过潜影盒本身
                if (item instanceof BlockItem blockItem) {
                    Block block = blockItem.getBlock();
                    if (block instanceof ShulkerBoxBlock) {
                        continue;
                    }
                }

                // 对于鞘翅，只计算耐久度高于阈值的
                if (item == Items.ELYTRA) {
                    int durability = stack.getMaxDamage() - stack.getDamage();
                    if (durability > elytraDurabilityThreshold.get()) {
                        currentCount.put(item, currentCount.getOrDefault(item, 0) + stack.getCount());
                    }
                } else {
                    currentCount.put(item, currentCount.getOrDefault(item, 0) + stack.getCount());
                }
            }
        }


        // 计算图腾需要补充的数量
        int currentTotems = currentCount.getOrDefault(Items.TOTEM_OF_UNDYING, 0);
        if (currentTotems < totemThreshold.get()) {
            neededItems.put(Items.TOTEM_OF_UNDYING, totemThreshold.get() - currentTotems);
        }

        // 计算XP需要补充的数量（按组计算，每组64个）
        int currentXP = currentCount.getOrDefault(Items.EXPERIENCE_BOTTLE, 0);
        int neededXPGroups = xpThreshold.get();
        int currentXPGroups = divideBy64Ceiling(currentXP);
        if (currentXPGroups < neededXPGroups) {
            neededItems.put(Items.EXPERIENCE_BOTTLE, neededXPGroups - currentXPGroups);
        }

        // 计算鞘翅需要补充的数量
        int currentElytras = currentCount.getOrDefault(Items.ELYTRA, 0);
        if (currentElytras < elytraThreshold.get()) {
            neededItems.put(Items.ELYTRA, elytraThreshold.get() - currentElytras);
        }

        // 计算紫颂果需要补充的数量（按组计算，每组64个）
        int currentChorus = currentCount.getOrDefault(Items.CHORUS_FRUIT, 0);
        int neededChorusGroups = chorusThreshold.get();
        int currentChorusGroups = divideBy64Ceiling(currentChorus);
        if (currentChorusGroups < neededChorusGroups) {
            neededItems.put(Items.CHORUS_FRUIT, neededChorusGroups - currentChorusGroups);
        }

        // 计算烟花需要补充的数量
        int currentFireworks = currentCount.getOrDefault(Items.FIREWORK_ROCKET, 0);
        if (currentFireworks < fireworkThreshold.get()) {
            neededItems.put(Items.FIREWORK_ROCKET, 40);
        }
        return neededItems;
    }

    /**
     * 处理关闭容器的逻辑
     */
    private void handleClosingContainer() {
        // 关闭容器界面
        if (mc.currentScreen != null) {
            mc.player.closeScreen();
            info("关闭容器界面");
        } else {
            state = SupplyState.FETCH_ITEMS;
            return;
        }

        // 破坏并收回末影箱
//        if (breakEnderChest()) {
//            info("已收回末影箱");
//        }
//
//        // 进入起飞状态
//        state = SupplyState.TAKING_OFF;
//        info("补给完成，准备起飞");
    }

    enum SupplyState {
        IDLE,           // 空闲状态
        DESCENDING,     // 降落中
        LANDING,        // 着陆处理
        USE_XP,    // 补给中
        AWAIT_XP,    // 补给中
        PLACE_ENDERCHEST,    // 补给中
        OPEN_ENDERCHEST,    // 补给中
        AWAIT_OPEN_ENDERCHEST,    // 补给中
        CLOSING_CONTAINER,  // 补给中
        FETCH_ITEMS,  // 补给中
        SAVING_BOX,  // 保存潜影盒
        PLACE_SAVE_ENDERCHEST,    // 放置用于保存的末影箱
        OPEN_SAVE_ENDERCHEST,      // 打开用于保存的末影箱
        AWAIT_OPEN_SAVE_ENDERCHEST, // 等待打开用于保存的末影箱
        CLOSE_SAVE_ENDERCHEST,     // 关闭用于保存的末影箱
        BREAK_SAVE_ENDERCHEST,     // 破坏用于保存的末影箱
        TAKING_OFF,     // 起飞中,
        RESUMING        // 恢复飞行
    }

    /**
     * 处理保存潜影盒到末影箱的逻辑
     */
    private void handleSavingBox() {
        info("开始保存潜影盒到末影箱");
        state = SupplyState.PLACE_SAVE_ENDERCHEST;
    }

    /**
     * 放置用于保存的末影箱
     */
    private void placeSaveEnderChest() {
        // 首先检查周围是否已有末影箱
        BlockPos existingChest = getBlockPos(Blocks.ENDER_CHEST);
        if (existingChest != null) {
            info("发现现有末影箱，无需放置新的");
            state = SupplyState.OPEN_SAVE_ENDERCHEST;
            return;
        }

        // 没有现有末影箱，放置新的
        BlockPos pos = MikuUtil.findSuitablePlacePosition();
        if (pos != null) {
            BaritoneUtil.placeItem(pos, Items.ENDER_CHEST);
            info("已放置保存用末影箱");
            state = SupplyState.OPEN_SAVE_ENDERCHEST;
        } else {
            info("找不到合适的位置放置末影箱，直接起飞");
            state = SupplyState.TAKING_OFF;
        }
    }

    /**
     * 打开用于保存的末影箱
     */
    private void openSaveEnderChest() {
        // 再次检查周围是否有末影箱
        BlockPos existingChest = getBlockPos(Blocks.ENDER_CHEST);
        if (existingChest == null) {
            info("周围没有末影箱，返回放置状态");
            state = SupplyState.PLACE_SAVE_ENDERCHEST;
            return;
        }

        // 如果找到末影箱，尝试打开
        if (openEnderChest()) {
            state = SupplyState.AWAIT_OPEN_SAVE_ENDERCHEST;
        } else {
            info("无法打开末影箱，等待重试");
        }
    }

    /**
     * 处理等待打开保存用末影箱
     */
    private void handleAwaitOpenSaveEnderchest() {
        // Check if container screen is open
        if (mc.currentScreen == null) {
            info("等待保存用容器界面打开...");
            return;
        }

        if (!(mc.currentScreen instanceof HandledScreen)) {
            info("等待保存用容器界面打开...");
            return;
        }

        HandledScreen<?> containerScreen = (HandledScreen<?>) mc.currentScreen;
        var screenHandler = containerScreen.getScreenHandler();

        info("保存用容器界面已打开");

        boolean hasShulkerBoxes = false;
        int containerSlots = screenHandler.slots.size() - 36; // Container slots only

        // 首先检查容器中是否已有潜影盒
        for (int i = 0; i < containerSlots; i++) {
            ItemStack stack = screenHandler.getSlot(i).getStack();
            if (!stack.isEmpty()) {
                Item item = stack.getItem();
                if (item instanceof BlockItem blockItem) {
                    Block block = blockItem.getBlock();
                    if (block instanceof ShulkerBoxBlock) {
                        hasShulkerBoxes = true;
                        break;
                    }
                }
            }
        }

        // 将背包中的潜影盒放入末影箱
        boolean movedAnyBox = false;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                Item item = stack.getItem();
                if (item instanceof BlockItem blockItem) {
                    Block block = blockItem.getBlock();
                    if (block instanceof ShulkerBoxBlock) {
                        // 找到空的容器槽位
                        for (int j = 0; j < containerSlots; j++) {
                            ItemStack containerStack = screenHandler.getSlot(j).getStack();
                            if (containerStack.isEmpty()) {
                                // 将潜影盒移入容器
                                mc.interactionManager.clickSlot(
                                    screenHandler.syncId,
                                    i + 36, // 玩家背包槽位需要加36
                                    0,
                                    SlotActionType.QUICK_MOVE,
                                    mc.player
                                );
                                movedAnyBox = true;
                                info("已将潜影盒移入末影箱");
                                break;
                            }
                        }
                        if (movedAnyBox) break;
                    }
                }
            }
        }

        if (movedAnyBox) {
            // 移动了潜影盒，关闭容器
            state = SupplyState.CLOSE_SAVE_ENDERCHEST;
        } else if (hasShulkerBoxes) {
            // 没有移动潜影盒但容器中有潜影盒，直接关闭
            info("末影箱中已有潜影盒，无需移动");
            state = SupplyState.CLOSE_SAVE_ENDERCHEST;
        } else {
            // 没有潜影盒可移动，直接关闭
            info("背包中没有潜影盒需要保存");
            state = SupplyState.CLOSE_SAVE_ENDERCHEST;
        }
    }

    /**
     * 关闭保存用末影箱
     */
    private void handleCloseSaveEnderchest() {
        if (mc.currentScreen != null) {
            mc.player.closeScreen();
            info("已关闭保存用末影箱");
        }
        state = SupplyState.BREAK_SAVE_ENDERCHEST;
    }

    /**
     * 破坏保存用末影箱
     */
    private void breakSaveEnderChest() {
        // 只有当我们确实放置了新的末影箱时才破坏它
        if (breakEnderChest()) {
            info("已回收保存用末影箱");
        } else {
            info("未能回收保存用末影箱");
        }

        BlockPos existingChest = getBlockPos(Blocks.ENDER_CHEST);
        if (existingChest == null) {
            info("潜影盒保存完成，准备起飞");
            state = SupplyState.TAKING_OFF;
            return;
        }
    }

    public static int divideBy64Ceiling(int n) {
        if (n <= 0) {
            return 0;
        }
        return (n + 63) / 64;
    }
}
