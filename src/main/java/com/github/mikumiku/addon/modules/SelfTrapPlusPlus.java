package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.*;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelfTrapPlusPlus extends BaseModule {
    public SelfTrapPlusPlus() {
        super("包裹自己", "使用Miku用方块困住自己。");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlacing = settings.createGroup("放置");
    private final SettingGroup sgToggle = settings.createGroup("开关");
    private final SettingGroup sgRender = settings.createGroup("渲染");

    //--------------------常规--------------------//
    private final Setting<Boolean> pauseEat = sgGeneral.add(new BoolSetting.Builder()
        .name("进食时暂停")
        .description("吃东西时暂停放置方块。")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> onlyConfirmed = sgGeneral.add(new BoolSetting.Builder()
        .name("仅确认方块")
        .description("只在服务器确认存在的方块上放置。")
        .defaultValue(true)
        .build()
    );
    private final Setting<SwitchMode> switchMode = sgGeneral.add(new EnumSetting.Builder<SwitchMode>()
        .name("切换模式")
        .description("切换物品的方法。静默模式最可靠。")
        .defaultValue(SwitchMode.静默)
        .build()
    );

    private final Setting<TrapMode> trapMode = sgGeneral.add(new EnumSetting.Builder<TrapMode>()
        .name("陷阱模式")
        .description("在哪些位置放置方块。")
        .defaultValue(TrapMode.全部)
        .build()
    );

    //--------------------放置--------------------//
    private final Setting<List<Block>> blocks = sgPlacing.add(new BlockListSetting.Builder()
        .name("方块")
        .description("要使用的方块。")
        .defaultValue(Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.NETHERITE_BLOCK)
        .build()
    );
    private final Setting<Double> placeDelay = sgPlacing.add(new DoubleSetting.Builder()
        .name("放置延迟")
        .description("每次放置之间的延迟。")
        .defaultValue(0.125)
        .range(0, 10)
        .sliderRange(0, 10)
        .build()
    );
    private final Setting<Integer> places = sgPlacing.add(new IntSetting.Builder()
        .name("每次放置数")
        .description("每次放置的方块数量。")
        .defaultValue(1)
        .range(1, 10)
        .sliderRange(1, 10)
        .build()
    );
    private final Setting<Double> delay = sgPlacing.add(new DoubleSetting.Builder()
        .name("位置延迟")
        .description("在每个位置放置之间的延迟。")
        .defaultValue(0.3)
        .range(0, 10)
        .sliderRange(0, 10)
        .build()
    );

    //--------------------开关--------------------//
    private final Setting<Boolean> toggleMove = sgToggle.add(new BoolSetting.Builder()
        .name("移动时关闭")
        .description("水平移动时关闭模块。")
        .defaultValue(true)
        .build()
    );
    private final Setting<ToggleYMode> toggleY = sgToggle.add(new EnumSetting.Builder<ToggleYMode>()
        .name("垂直移动时关闭")
        .description("垂直移动时关闭模块。")
        .defaultValue(ToggleYMode.全部)
        .build()
    );
    private final Setting<Boolean> toggleSneak = sgToggle.add(new BoolSetting.Builder()
        .name("潜行时关闭")
        .description("潜行时关闭模块。")
        .defaultValue(false)
        .build()
    );

    //--------------------渲染--------------------//
    private final Setting<Boolean> placeSwing = sgRender.add(new BoolSetting.Builder()
        .name("挥手动画")
        .description("放置方块时显示挥手动画。")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("形状模式")
        .description("渲染方块的哪些部分。")
        .defaultValue(ShapeMode.Both)
        .build()
    );
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("线条颜色")
        .description("方框轮廓的颜色")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .build()
    );
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("侧面颜色")
        .description("方框侧面的颜色。")
        .defaultValue(new SettingColor(255, 0, 0, 50))
        .build()
    );
    private final Setting<SettingColor> supportLineColor = sgRender.add(new ColorSetting.Builder()
        .name("支撑线条颜色")
        .description("支撑方块的轮廓颜色")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .build()
    );
    private final Setting<SettingColor> supportSideColor = sgRender.add(new ColorSetting.Builder()
        .name("支撑侧面颜色")
        .description("支撑方块的侧面颜色。")
        .defaultValue(new SettingColor(255, 0, 0, 50))
        .build()
    );

    // Custom timer system
    private final Map<BlockPos, Timer> placementTimers = new HashMap<>();
    private final Map<BlockPos, Timer> placedTimers = new HashMap<>();
    private final Timer placeTimer = new Timer();
    private int placesLeft = 0;
    private BlockPos startPos = BlockPos.ORIGIN;
    private boolean lastSneak = false;
    private final List<Render> render = new ArrayList<>();

    public static boolean placing = false;

    @Override
    public void onActivate() {
        super.onActivate();
        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }
        startPos = mc.player.getBlockPos();
        placesLeft = places.get();
        placeTimer.reset();
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        placesLeft = places.get();
        placeTimer.reset();
        placementTimers.clear();
        placedTimers.clear();
        RotationManager.getInstance().sync();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onRender(Render3DEvent event) {
        // Update timers
        updateTimers();

        placing = false;

        // Update place timer
        if (placeTimer.passedMs(placeDelay.get() * 1000)) {
            placesLeft = places.get();
            placeTimer.reset();
        }

        if (mc.player != null && mc.world != null) {
            // Movement checks
            if (checkMovement()) {
                return;
            }

            // Get blocks to place
            List<BlockPos> blocksList = getBlocks(getSize(mc.player.getBlockPos().up()),
                mc.player.getBoundingBox().intersects(Box.from(new BlockBox(mc.player.getBlockPos().up(2)))));

            render.clear();
            List<BlockPos> placements = getValid(blocksList);

            // Render blocks
            render.forEach(item -> event.renderer.box(Box.from(new BlockBox(item.pos)),
                item.support ? supportSideColor.get() : sideColor.get(),
                item.support ? supportLineColor.get() : lineColor.get(),
                shapeMode.get(), 0));

            // Check if we can place
            if (canPlaceBlocks(placements)) {
                placeBlocks(placements);
            }
        }
    }

    private boolean checkMovement() {
        // Horizontal movement check
        if (toggleMove.get() && (mc.player.getBlockPos().getX() != startPos.getX() ||
            mc.player.getBlockPos().getZ() != startPos.getZ())) {
            sendDisableMsg("移动了");
            toggle();
            return true;
        }

        // Vertical movement check
        switch (toggleY.get()) {
            case 全部 -> {
                if (mc.player.getBlockPos().getY() != startPos.getY()) {
                    sendDisableMsg("垂直移动了");
                    toggle();
                    return true;
                }
            }
            case 向上 -> {
                if (mc.player.getBlockPos().getY() > startPos.getY()) {
                    sendDisableMsg("向上移动了");
                    toggle();
                    return true;
                }
            }
            case 向下 -> {
                if (mc.player.getBlockPos().getY() < startPos.getY()) {
                    sendDisableMsg("向下移动了");
                    toggle();
                    return true;
                }
            }
        }

        // Sneak check
        if (toggleSneak.get()) {
            boolean isClicked = mc.options.sneakKey.isPressed();
            if (isClicked && !lastSneak) {
                sendDisableMsg("潜行了");
                toggle();
                return true;
            }
            lastSneak = isClicked;
        }

        return false;
    }

    private void sendDisableMsg(String reason) {
//        sendMsg("Disabled: " + reason, hashCode());
        info("已关闭：" + reason);
    }

    private void updateTimers() {
        // Clean up expired timers
        placementTimers.entrySet().removeIf(entry -> entry.getValue().passedMs(delay.get() * 1000));
        placedTimers.entrySet().removeIf(entry -> entry.getValue().passedMs(1000)); // 1 second for confirmed placements
    }

    private boolean canPlaceBlocks(List<BlockPos> placements) {
        if (pauseEat.get() && mc.player.isUsingItem()) {
            return false;
        }

        if (placesLeft <= 0 || placements.isEmpty()) {
            return false;
        }

        // Check if we have blocks available
        Hand hand = getValidHand();
        if (hand != null) {
            return true;
        }

        // Check inventory based on switch mode
        return switch (switchMode.get()) {
            case 静默, SwitchMode.普通 -> findInHotbar().found();
            case 拾取静默, SwitchMode.物品栏切换 -> findInInventory().found();
            default -> false;
        };
    }

    private void placeBlocks(List<BlockPos> placements) {
        List<BlockPos> toPlace = new ArrayList<>();
        for (BlockPos placement : placements) {
            if (toPlace.size() < placesLeft && canPlace(placement)) {
                toPlace.add(placement);
            }
        }

        if (toPlace.isEmpty()) {
            return;
        }

        // Get block count
        int blockCount = getBlockCount();
        if (blockCount <= 0) {
            return;
        }

        placing = true;

        // Place blocks
        for (int i = 0; i < Math.min(blockCount, toPlace.size()); i++) {
            BlockPos pos = toPlace.get(i);
            if (placeBlock(pos)) {
                placesLeft--;
                placeTimer.reset();

                // Add to timers
                placementTimers.put(pos, new Timer());
                if (onlyConfirmed.get()) {
                    placedTimers.put(pos, new Timer());
                }
            }
        }

        // Clean up rotation
        RotationManager.getInstance().sync();
    }

    private boolean placeBlock(BlockPos pos) {
        Direction direction = BaritoneUtil.getInteractDirection(pos, true);
        if (direction == null) {
            return false;
        }

        BlockPos neighbor = pos.offset(direction.getOpposite());
        Vec3d hitPos = neighbor.toCenterPos();

        // Handle rotation
        Rotation rotation = new Rotation(hitPos);
        rotation.setPriority(100); // High priority for block placement

        if (!RotationManager.getInstance().register(rotation)) {
            return false;
        }

        // Get hand
        Hand hand = getValidHand();
        if (hand == null) {
            // Switch to block
            switch (switchMode.get()) {
                case 静默, SwitchMode.普通 -> {
                    FindItemResult hotbar = findInHotbar();
                    if (hotbar.found()) {
                        BagUtil.doSwap(hotbar.slot());
                        hand = Hand.MAIN_HAND;
                    }
                }
                case SwitchMode.物品栏切换, SwitchMode.拾取静默 -> {
                    FindItemResult inventory = findInInventory();
                    if (inventory.found()) {
                        // Use BagUtil for inventory switching
                        if (switchMode.get() == SwitchMode.拾取静默) {
                            BagUtil.inventorySwapAtTruth(inventory.slot(), 45);
                        } else {
                            BagUtil.doSwap(inventory.slot());
                        }
                        hand = Hand.MAIN_HAND;
                    }
                }
            }
        }

        if (hand == null) {
            RotationManager.getInstance().sync();
            return false;
        }

        // Place the block
        boolean success = BaritoneUtil.placeBlock(neighbor, direction, placeSwing.get(), true);

        // Swing hand if needed
        if (placeSwing.get()) {
            mc.player.swingHand(hand);
        }

        // Switch back if we changed slots
        if (getValidHand() == null) {
            switch (switchMode.get()) {
                case 静默 -> BagUtil.doSwapOnTruth(mc.player.getInventory().selectedSlot);
                case 拾取静默 -> BagUtil.inventorySwapAtTruth(45, mc.player.getInventory().selectedSlot);
                case SwitchMode.物品栏切换 -> BagUtil.doSwapOnTruth(mc.player.getInventory().selectedSlot);
            }
        }

        return success;
    }

    private Hand getValidHand() {
        ItemStack mainHand = mc.player.getMainHandStack();
        ItemStack offHand = mc.player.getOffHandStack();

        if (isValid(mainHand)) return Hand.MAIN_HAND;
        if (isValid(offHand)) return Hand.OFF_HAND;
        return null;
    }

    private boolean isValid(ItemStack item) {
        return item.getItem() instanceof BlockItem && blocks.get().contains(((BlockItem) item.getItem()).getBlock());
    }

    private boolean canPlace(BlockPos pos) {
        if (!BaritoneUtil.canPlace(pos, true)) {
            return false;
        }

        // Check if position is in range
        double distance = mc.player.getEyePos().distanceTo(pos.toCenterPos());
        return distance <= 6.0; // Default reach distance
    }

    private FindItemResult findInHotbar() {
        return InvUtils.findInHotbar(item -> item.getItem() instanceof BlockItem &&
            blocks.get().contains(((BlockItem) item.getItem()).getBlock()));
    }

    private FindItemResult findInInventory() {
        return InvUtils.find(item -> item.getItem() instanceof BlockItem &&
            blocks.get().contains(((BlockItem) item.getItem()).getBlock()));
    }

    private int getBlockCount() {
        Hand hand = getValidHand();
        if (hand != null) {
            ItemStack stack = hand == Hand.MAIN_HAND ? mc.player.getMainHandStack() : mc.player.getOffHandStack();
            return stack.getCount();
        }

        return switch (switchMode.get()) {
            case 静默, SwitchMode.普通 -> findInHotbar().count();
            case 拾取静默, SwitchMode.物品栏切换 -> findInInventory().count();
            default -> 0;
        };
    }

    private List<BlockPos> getValid(List<BlockPos> blocks) {
        List<BlockPos> list = new ArrayList<>();

        if (blocks.isEmpty()) {
            return list;
        }

        blocks.forEach(block -> {
            if (!mc.world.getBlockState(block).isAir()) {
                return;
            }

            if (BaritoneUtil.canPlace(block, true) && canPlace(block)) {
                render.add(new Render(block, false));
                if (!EntityUtils.intersectsWithEntity(Box.from(new BlockBox(block)),
                    entity -> !entity.isSpectator() && !(entity instanceof ItemEntity)) &&
                    !placementTimers.containsKey(block)) {
                    list.add(block);
                }
                return;
            }

            // 1 block support
            Direction support1 = getSupport(block);
            if (support1 != null) {
                render.add(new Render(block, false));
                render.add(new Render(block.offset(support1), true));

                if (!EntityUtils.intersectsWithEntity(Box.from(new BlockBox(block.offset(support1))),
                    entity -> !entity.isSpectator() && !(entity instanceof ItemEntity)) &&
                    !placementTimers.containsKey(block.offset(support1))) {
                    list.add(block.offset(support1));
                }
                return;
            }

            // 2 block support
            for (Direction dir : Direction.values()) {
                if (!mc.world.getBlockState(block.offset(dir)).isAir() || !canPlace(block.offset(dir))) {
                    continue;
                }

                Direction support2 = getSupport(block.offset(dir));
                if (support2 != null) {
                    render.add(new Render(block, false));
                    render.add(new Render(block.offset(dir), true));
                    render.add(new Render(block.offset(dir).offset(support2), true));

                    if (!EntityUtils.intersectsWithEntity(Box.from(new BlockBox(block.offset(dir).offset(support2))),
                        entity -> !entity.isSpectator() && !(entity instanceof ItemEntity)) &&
                        !placementTimers.containsKey(block.offset(dir).offset(support2))) {
                        list.add(block.offset(dir).offset(support2));
                    }
                    return;
                }
            }
        });
        return list;
    }

    private Direction getSupport(BlockPos position) {
        Direction bestDir = null;
        double bestDist = Double.MAX_VALUE;
        int bestValue = -1;

        for (Direction dir : Direction.values()) {
            BlockPos checkPos = position.offset(dir);

            if (!BaritoneUtil.canPlace(checkPos, true) || !canPlace(checkPos)) {
                continue;
            }

            // Check for entities
            boolean hasItem = EntityUtils.intersectsWithEntity(Box.from(new BlockBox(checkPos)),
                entity -> !entity.isSpectator() && entity.getType() == EntityType.ITEM);
            boolean hasCrystal = EntityUtils.intersectsWithEntity(Box.from(new BlockBox(checkPos)),
                entity -> !entity.isSpectator() && entity.getType() == EntityType.END_CRYSTAL);

            double dist = mc.player.getEyePos().distanceTo(checkPos.toCenterPos());
            int value = hasCrystal ? 1 : (hasItem ? 2 : 0);

            if (dist < bestDist || value > bestValue) {
                bestDir = dir;
                bestDist = dist;
                bestValue = value;
            }
        }

        return bestDir;
    }

    private List<BlockPos> getBlocks(int[] size, boolean higher) {
        List<BlockPos> list = new ArrayList<>();
        BlockPos pos = mc.player.getBlockPos().up(higher ? 2 : 1);

        if (mc.player != null && mc.world != null) {
            for (int x = size[0] - 1; x <= size[1] + 1; x++) {
                for (int z = size[2] - 1; z <= size[3] + 1; z++) {
                    boolean isX = x == size[0] - 1 || x == size[1] + 1;
                    boolean isZ = z == size[2] - 1 || z == size[3] + 1;

                    // Check if position is already occupied
                    boolean ignore = isX && !isZ ? (!mc.world.getBlockState(pos.add(closerToZero(x), 0, z)).isAir() || placedTimers.containsKey(pos.add(closerToZero(x), 0, z))) :
                        !isX && isZ && (!mc.world.getBlockState(pos.add(x, 0, closerToZero(z))).isAir() || placedTimers.containsKey(pos.add(x, 0, closerToZero(z))));

                    BlockPos bPos = null;
                    if (eye() && isX != isZ && !ignore) {
                        bPos = new BlockPos(x, pos.getY(), z).add(pos.getX(), 0, pos.getZ());
                    } else if (top() && !isX && !isZ && mc.world.getBlockState(pos.add(x, 0, z)).isAir() && !placedTimers.containsKey(pos.add(x, 0, z))) {
                        bPos = new BlockPos(x, pos.getY(), z).add(pos.getX(), 1, pos.getZ());
                    }

                    if (bPos != null) {
                        list.add(bPos);
                    }
                }
            }
        }
        return list;
    }

    private int closerToZero(int value) {
        return value > 0 ? -1 : (value < 0 ? 1 : 0);
    }

    private boolean top() {
        return trapMode.get() == TrapMode.全部 || trapMode.get() == TrapMode.顶部;
    }

    private boolean eye() {
        return trapMode.get() == TrapMode.全部 || trapMode.get() == TrapMode.眼部;
    }

    private int[] getSize(BlockPos pos) {
        int minX = 0;
        int maxX = 0;
        int minZ = 0;
        int maxZ = 0;

        if (mc.player != null && mc.world != null) {
            Box box = mc.player.getBoundingBox();
            if (box.intersects(Box.from(new BlockBox(pos.north())))) minZ--;
            if (box.intersects(Box.from(new BlockBox(pos.south())))) maxZ++;
            if (box.intersects(Box.from(new BlockBox(pos.west())))) minX--;
            if (box.intersects(Box.from(new BlockBox(pos.east())))) maxX++;
        }
        return new int[]{minX, maxX, minZ, maxZ};
    }

    public enum SwitchMode {
        禁用,
        普通,
        静默,
        拾取静默,
        物品栏切换
    }

    public enum TrapMode {
        顶部,
        眼部,
        全部
    }

    public enum ToggleYMode {
        禁用,
        向上,
        向下,
        全部
    }

    private record Render(BlockPos pos, boolean support) {
    }
}
