//package com.github.mikumiku.addon.modules;
//
//
//import com.github.mikumiku.addon.BaseModule;
//import com.github.mikumiku.addon.util.Via;
//import meteordevelopment.meteorclient.events.world.TickEvent;
//import meteordevelopment.meteorclient.settings.*;
//import meteordevelopment.meteorclient.utils.player.FindItemResult;
//import meteordevelopment.meteorclient.utils.player.InvUtils;
//import meteordevelopment.meteorclient.utils.player.PlayerUtils;
//import meteordevelopment.meteorclient.utils.player.Rotations;
//import meteordevelopment.meteorclient.utils.world.BlockUtils;
//import meteordevelopment.orbit.EventHandler;
//import net.minecraft.block.Block;
//import net.minecraft.block.Blocks;
//import net.minecraft.item.ItemStack;
//import net.minecraft.item.Items;
//import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
//import net.minecraft.screen.slot.SlotActionType;
//import net.minecraft.util.Hand;
//import net.minecraft.util.hit.BlockHitResult;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.util.math.Direction;
//import net.minecraft.util.math.Vec3d;
//
//public class UniversalSupplySystem extends BaseModule {
//    private final SettingGroup sgGeneral = settings.getDefaultGroup();
//    private final SettingGroup sgElytra = settings.createGroup("鞘翅设置");
//    private final SettingGroup sgFirework = settings.createGroup("烟花设置");
//    private final SettingGroup sgSafety = settings.createGroup("安全设置");
//
//    // 通用设置
//    private final Setting<Boolean> autoSupply = sgGeneral.add(new BoolSetting.Builder()
//        .name("自动补给")
//        .description("启用自动物资补给系统")
//        .defaultValue(true)
//        .build()
//    );
//
//    private final Setting<SupplyMode> supplyMode = sgGeneral.add(new EnumSetting.Builder<SupplyMode>()
//        .name("补给模式")
//        .description("根据所在维度自动选择补给策略")
//        .defaultValue(SupplyMode.AUTO)
//        .build()
//    );
//
//    // 鞘翅设置
//    private final Setting<Integer> elytraDurabilityThreshold = sgElytra.add(new IntSetting.Builder()
//        .name("鞘翅耐久阈值")
//        .description("当鞘翅耐久低于此值时触发补给")
//        .defaultValue(50)
//        .min(1)
//        .max(431)
//        .sliderMax(431)
//        .build()
//    );
//
//    private final Setting<Boolean> useEnderChest = sgElytra.add(new BoolSetting.Builder()
//        .name("使用末影箱")
//        .description("从末影箱中获取鞘翅")
//        .defaultValue(true)
//        .build()
//    );
//
//    private final Setting<Boolean> useShulkerBox = sgElytra.add(new BoolSetting.Builder()
//        .name("使用潜影盒")
//        .description("从潜影盒中获取鞘翅")
//        .defaultValue(true)
//        .build()
//    );
//
//    // 烟花设置
//    private final Setting<Integer> fireworkThreshold = sgFirework.add(new IntSetting.Builder()
//        .name("烟花数量阈值")
//        .description("当烟花数量低于此值时触发补给")
//        .defaultValue(32)
//        .min(1)
//        .max(64)
//        .sliderMax(64)
//        .build()
//    );
//
//    private final Setting<Integer> fireworkRefillAmount = sgFirework.add(new IntSetting.Builder()
//        .name("烟花补充数量")
//        .description("每次补给时补充的烟花数量")
//        .defaultValue(64)
//        .min(1)
//        .max(64)
//        .sliderMax(64)
//        .build()
//    );
//
//    // 安全设置
//    private final Setting<Boolean> eatChorusFruit = sgSafety.add(new BoolSetting.Builder()
//        .name("使用紫颂果")
//        .description("在末地和主世界降落时吃紫颂果")
//        .defaultValue(true)
//        .build()
//    );
//
//    private final Setting<Integer> safeLandingHeight = sgSafety.add(new IntSetting.Builder()
//        .name("安全降落高度")
//        .description("开始降落程序的最低高度")
//        .defaultValue(200)
//        .min(50)
//        .max(320)
//        .sliderMax(320)
//        .build()
//    );
//
//    private final Setting<Boolean> buildAirPlatform = sgSafety.add(new BoolSetting.Builder()
//        .name("搭建空中平台")
//        .description("在主世界高空搭建临时平台")
//        .defaultValue(true)
//        .build()
//    );
//
//    private final Setting<Boolean> antiGhast = sgSafety.add(new BoolSetting.Builder()
//        .name("恶魂防护")
//        .description("在下界补给时防止恶魂破坏")
//        .defaultValue(true)
//        .build()
//    );
//
//    // 内部状态
//    private SupplyState state = SupplyState.IDLE;
//    private Vec3d originalDirection = null;
//    private BlockPos platformPos = null;
//    private int tickCounter = 0;
//    private boolean isSupplying = false;
//
//    public enum SupplyMode {
//        AUTO("自动"),
//        END("末地模式"),
//        NETHER("下界模式"),
//        OVERWORLD("主世界模式");
//
//        private final String title;
//
//        SupplyMode(String title) {
//            this.title = title;
//        }
//
//        @Override
//        public String toString() {
//            return title;
//        }
//    }
//
//    public enum SupplyState {
//        IDLE,
//        DETECTING,
//        DESCENDING,
//        EATING_CHORUS,
//        FINDING_SAFE_SPOT,
//        BUILDING_PLATFORM,
//        PLACING_CONTAINER,
//        OPENING_CONTAINER,
//        REPLACING_ELYTRA,
//        REFILLING_FIREWORKS,
//        TAKING_OFF,
//        RESUMING_FLIGHT
//    }
//
//    public UniversalSupplySystem() {
//        super("赶路助手2", "通用物资补给系统");
//    }
//
//    @Override
//    public void onActivate() {
//        state = SupplyState.IDLE;
//        isSupplying = false;
//        tickCounter = 0;
//        originalDirection = null;
//        platformPos = null;
//    }
//
//    @EventHandler
//    private void onTick(TickEvent.Pre event) {
//        if (mc.player == null || mc.world == null) return;
//        if (!autoSupply.get()) return;
//
//        tickCounter++;
//
//        // 每20 tick检测一次
//        if (tickCounter % 20 == 0 && state == SupplyState.IDLE) {
//            if (needsSupply()) {
//                startSupplySequence();
//            }
//        }
//
//        // 执行当前状态的逻辑
//        processCurrentState();
//    }
//
//    private boolean needsSupply() {
//        // 检查是否正在飞行
//        if (!Via.isFallFlying(mc)) return false;
//
//        // 检查鞘翅耐久
//        if (needsElytraReplacement()) return true;
//
//        // 检查烟花数量
//        if (needsFireworkRefill()) return true;
//
//        return false;
//    }
//
//    private boolean needsElytraReplacement() {
//        FindItemResult elytra = InvUtils.find(Items.ELYTRA);
//        if (!elytra.found()) return false;
//
//        ItemStack stack = mc.player.getInventory().getStack(elytra.slot());
//        int durability = stack.getMaxDamage() - stack.getDamage();
//        return durability < elytraDurabilityThreshold.get();
//    }
//
//    private boolean needsFireworkRefill() {
//        int fireworkCount = InvUtils.find(Items.FIREWORK_ROCKET).count();
//        return fireworkCount < fireworkThreshold.get();
//    }
//
//    private void startSupplySequence() {
//        isSupplying = true;
//        originalDirection = mc.player.getRotationVec(1.0f);
//
//        info("开始补给序列 - 维度: " + PlayerUtils.getDimension());
//
//        switch (getCurrentSupplyMode()) {
//            case END:
//                startEndSupply();
//                break;
//            case NETHER:
//                startNetherSupply();
//                break;
//            case OVERWORLD:
//                startOverworldSupply();
//                break;
//            default:
//                break;
//        }
//    }
//
//    private SupplyMode getCurrentSupplyMode() {
//        if (supplyMode.get() != SupplyMode.AUTO) {
//            return supplyMode.get();
//        }
//
//        // 自动检测维度
//        String dimension = mc.world.getRegistryKey().getValue().getPath();
//        if (dimension.contains("the_end")) return SupplyMode.END;
//        if (dimension.contains("the_nether")) return SupplyMode.NETHER;
//        return SupplyMode.OVERWORLD;
//    }
//
//    private void startEndSupply() {
//        state = SupplyState.EATING_CHORUS;
//        info("末地补给: 准备使用紫颂果降落");
//    }
//
//    private void startNetherSupply() {
//        state = SupplyState.FINDING_SAFE_SPOT;
//        info("下界补给: 寻找安全降落点");
//    }
//
//    private void startOverworldSupply() {
//        state = SupplyState.EATING_CHORUS;
//        info("主世界补给: 准备降落并搭建平台");
//    }
//
//    private void processCurrentState() {
//        switch (state) {
//            case EATING_CHORUS:
//                handleEatingChorus();
//                break;
//            case FINDING_SAFE_SPOT:
//                handleFindingSafeSpot();
//                break;
//            case BUILDING_PLATFORM:
//                handleBuildingPlatform();
//                break;
//            case PLACING_CONTAINER:
//                handlePlacingContainer();
//                break;
//            case OPENING_CONTAINER:
//                handleOpeningContainer();
//                break;
//            case REPLACING_ELYTRA:
//                handleReplacingElytra();
//                break;
//            case REFILLING_FIREWORKS:
//                handleRefillingFireworks();
//                break;
//            case TAKING_OFF:
//                handleTakingOff();
//                break;
//            case RESUMING_FLIGHT:
//                handleResumingFlight();
//                break;
//            default:
//                break;
//        }
//    }
//
//    private void handleEatingChorus() {
//        if (!eatChorusFruit.get()) {
//            state = SupplyState.FINDING_SAFE_SPOT;
//            return;
//        }
//
//        FindItemResult chorus = InvUtils.find(Items.CHORUS_FRUIT);
//        if (chorus.found()) {
//            InvUtils.swap(chorus.slot(), true);
//            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
//            info("使用紫颂果降落");
//
//            // 等待传送完成
//            tickCounter = 0;
//            state = SupplyState.FINDING_SAFE_SPOT;
//        } else {
//            warning("未找到紫颂果，直接降落");
//            state = SupplyState.FINDING_SAFE_SPOT;
//        }
//    }
//
//    private void handleFindingSafeSpot() {
//        if (mc.player.isOnGround()) {
//            BlockPos pos = mc.player.getBlockPos();
//
//            // 检查周围是否安全
//            if (isSafeLocation(pos)) {
//                info("找到安全位置: " + pos);
//
//                if (getCurrentSupplyMode() == SupplyMode.OVERWORLD && mc.player.getY() < safeLandingHeight.get()) {
//                    state = SupplyState.BUILDING_PLATFORM;
//                } else {
//                    state = SupplyState.PLACING_CONTAINER;
//                }
//            } else {
//                // 尝试移动到安全位置
//                findAndMoveToSafeSpot();
//            }
//        }
//    }
//
//    private boolean isSafeLocation(BlockPos pos) {
//        // 检查脚下和周围方块
//        Block below = mc.world.getBlockState(pos.down()).getBlock();
//
//        // 下界特殊检查
//        if (getCurrentSupplyMode() == SupplyMode.NETHER) {
//            if (below == Blocks.LAVA || below == Blocks.MAGMA_BLOCK) return false;
//
//            // 检查是否在玄武岩三角洲
//            for (int x = -2; x <= 2; x++) {
//                for (int z = -2; z <= 2; z++) {
//                    Block block = mc.world.getBlockState(pos.add(x, -1, z)).getBlock();
//                    if (block == Blocks.BASALT || block == Blocks.BLACKSTONE) {
//                        return false; // 避免玄武岩三角洲
//                    }
//                }
//            }
//        }
//
//        return below != Blocks.AIR && below != Blocks.LAVA;
//    }
//
//    private void findAndMoveToSafeSpot() {
//        // 简化版：在附近寻找安全点
//        BlockPos playerPos = mc.player.getBlockPos();
//
//        for (int radius = 1; radius <= 5; radius++) {
//            for (int x = -radius; x <= radius; x++) {
//                for (int z = -radius; z <= radius; z++) {
//                    BlockPos checkPos = playerPos.add(x, 0, z);
//                    if (isSafeLocation(checkPos)) {
//                        // 移动到该位置
//                        Vec3d target = new Vec3d(checkPos.getX() + 0.5, checkPos.getY(), checkPos.getZ() + 0.5);
//                        mc.player.setVelocity(target.subtract(mc.player.getPos()).normalize().multiply(0.1));
//                        return;
//                    }
//                }
//            }
//        }
//    }
//
//    private void handleBuildingPlatform() {
//        if (platformPos == null) {
//            platformPos = mc.player.getBlockPos().up(200 - (int) mc.player.getY());
//        }
//
//        // 使用烟花垂直升至Y=200
//        if (mc.player.getY() < safeLandingHeight.get()) {
//            useFireworkBoost();
//            return;
//        }
//
//        // 到达高度后搭建平台
//        if (buildAirPlatform.get()) {
//            buildPlatform(platformPos);
//            info("空中平台搭建完成");
//        }
//
//        state = SupplyState.PLACING_CONTAINER;
//    }
//
//    private void buildPlatform(BlockPos center) {
//        FindItemResult blocks = InvUtils.findInHotbar(itemStack ->
//            itemStack.getItem() instanceof net.minecraft.item.BlockItem
//        );
//
//        if (!blocks.found()) {
//            warning("背包中没有方块，无法搭建平台");
//            return;
//        }
//
//        // 搭建3x3平台
//        for (int x = -1; x <= 1; x++) {
//            for (int z = -1; z <= 1; z++) {
//                BlockPos pos = center.add(x, 0, z);
//                if (mc.world.getBlockState(pos).isAir()) {
//                    BlockUtils.place(pos, blocks, false, 0, true);
//                }
//            }
//        }
//    }
//
//    private void handlePlacingContainer() {
//        BlockPos pos = mc.player.getBlockPos();
//
//        if (useEnderChest.get()) {
//            FindItemResult enderChest = InvUtils.findInHotbar(Items.ENDER_CHEST);
//            if (enderChest.found()) {
//                BlockUtils.place(pos.up(), enderChest, false, 0, true);
//                info("放置末影箱");
//                state = SupplyState.OPENING_CONTAINER;
//                return;
//            }
//        }
//
//        if (useShulkerBox.get()) {
//            FindItemResult shulkerBox = InvUtils.findInHotbar(itemStack ->
//                itemStack.getItem().toString().contains("shulker_box")
//            );
//            if (shulkerBox.found()) {
//                BlockUtils.place(pos.up(), shulkerBox, false, 0, true);
//                info("放置潜影盒");
//                state = SupplyState.OPENING_CONTAINER;
//                return;
//            }
//        }
//
//        warning("未找到容器，跳过补给");
//        state = SupplyState.TAKING_OFF;
//    }
//
//    private void handleOpeningContainer() {
//        // 打开容器
//        BlockPos containerPos = mc.player.getBlockPos().up();
//        BlockHitResult hitResult = new BlockHitResult(
//            Vec3d.ofCenter(containerPos),
//            Direction.UP,
//            containerPos,
//            false
//        );
//
//        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
//
//        state = SupplyState.REPLACING_ELYTRA;
//    }
//
//    private void handleReplacingElytra() {
//        if (needsElytraReplacement()) {
//            // 从容器中取出新鞘翅
//            replaceElytraFromContainer();
//            info("更换鞘翅完成");
//        }
//
//        state = SupplyState.REFILLING_FIREWORKS;
//    }
//
//    private void replaceElytraFromContainer() {
//        // 简化实现：查找并移动鞘翅
//        // 实际需要根据容器GUI进行操作
//        if (mc.player.currentScreenHandler != null) {
//            for (int i = 0; i < mc.player.currentScreenHandler.slots.size(); i++) {
//                if (mc.player.currentScreenHandler.getSlot(i).getStack().getItem() == Items.ELYTRA) {
//                    int durability = mc.player.currentScreenHandler.getSlot(i).getStack().getMaxDamage()
//                        - mc.player.currentScreenHandler.getSlot(i).getStack().getDamage();
//
//                    if (durability > elytraDurabilityThreshold.get()) {
//                        // 将鞘翅移到胸甲槽
//                        mc.interactionManager.clickSlot(
//                            mc.player.currentScreenHandler.syncId,
//                            i,
//                            0,
//                            SlotActionType.QUICK_MOVE,
//                            mc.player
//                        );
//                        break;
//                    }
//                }
//            }
//        }
//    }
//
//    private void handleRefillingFireworks() {
//        if (needsFireworkRefill()) {
//            refillFireworksFromContainer();
//            info("补充烟花完成");
//        }
//
//        // 关闭容器
//        mc.player.closeHandledScreen();
//        state = SupplyState.TAKING_OFF;
//    }
//
//    private void refillFireworksFromContainer() {
//        if (mc.player.currentScreenHandler != null) {
//            int collected = 0;
//            for (int i = 0; i < mc.player.currentScreenHandler.slots.size(); i++) {
//                if (mc.player.currentScreenHandler.getSlot(i).getStack().getItem() == Items.FIREWORK_ROCKET) {
//                    mc.interactionManager.clickSlot(
//                        mc.player.currentScreenHandler.syncId,
//                        i,
//                        0,
//                        SlotActionType.QUICK_MOVE,
//                        mc.player
//                    );
//                    collected += mc.player.currentScreenHandler.getSlot(i).getStack().getCount();
//
//                    if (collected >= fireworkRefillAmount.get()) break;
//                }
//            }
//        }
//    }
//
//    private void handleTakingOff() {
//
//        mc.player.jump();
//
//        // 开始飞行
//        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(
//            mc.player,
//            ClientCommandC2SPacket.Mode.START_FALL_FLYING
//        ));
//        // 使用烟花起飞
//        useFireworkBoost();
//
//        info("重新起飞");
//        state = SupplyState.RESUMING_FLIGHT;
//    }
//
//    private void handleResumingFlight() {
//        // 恢复原飞行方向
//        if (originalDirection != null) {
//            Rotations.rotate(
//                Rotations.getYaw(originalDirection),
//                Rotations.getPitch(originalDirection)
//            );
//        }
//
//        info("补给完成，恢复飞行");
//
//        // 重置状态
//        state = SupplyState.IDLE;
//        isSupplying = false;
//        platformPos = null;
//        originalDirection = null;
//    }
//
//    private void useFireworkBoost() {
//        FindItemResult firework = InvUtils.find(Items.FIREWORK_ROCKET);
//        if (firework.found()) {
//            InvUtils.swap(firework.slot(), true);
//            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
//        }
//    }
//}
