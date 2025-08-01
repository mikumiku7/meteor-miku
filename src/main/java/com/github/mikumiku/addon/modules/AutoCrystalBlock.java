package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.BagUtil;
import com.github.mikumiku.addon.util.BaritoneUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * 自动挡水晶模块
 * <p>
 * 当检测到附近6格内有END_CRYSTAL时，自动在对应方向的玩家脚边放置方块进行抵挡
 * 优先放置黑曜石，如果没有黑曜石则放置石头、木头等常见方块
 */
public class AutoCrystalBlock extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBlocks = settings.createGroup("方块设置");

    // 检测范围设置
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("检测范围")
        .description("检测水晶的范围（格）")
        .defaultValue(6)
        .min(1)
        .max(10)
        .sliderMax(10)
        .build()
    );

    // 放置延迟设置
    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("放置延迟")
        .description("放置方块的延迟（tick）")
        .defaultValue(0)
        .min(0)
        .max(20)
        .sliderMax(20)
        .build()
    );

    // 是否只在脚边放置
    private final Setting<Boolean> onlyAtFeet = sgGeneral.add(new BoolSetting.Builder()
        .name("仅脚边放置")
        .description("只在玩家脚边放置方块，而不是在水晶和玩家之间")
        .defaultValue(true)
        .build()
    );

    // 方块优先级设置
    private final Setting<Boolean> useObsidian = sgBlocks.add(new BoolSetting.Builder()
        .name("使用黑曜石")
        .description("优先使用黑曜石")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> useStone = sgBlocks.add(new BoolSetting.Builder()
        .name("使用石头")
        .description("使用石头类方块")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> useWood = sgBlocks.add(new BoolSetting.Builder()
        .name("使用木头")
        .description("使用木头类方块")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> useCobblestone = sgBlocks.add(new BoolSetting.Builder()
        .name("使用圆石")
        .description("使用圆石")
        .defaultValue(true)
        .build()
    );

    // 内部变量
    private int placeTimer = 0;
    private final Set<BlockPos> placedPositions = new HashSet<>();
    private long lastErrorTime = 0;
    private static final long ERROR_COOLDOWN = 5000; // 5秒错误冷却

    // 方块优先级列表（按优先级排序）
    private final List<Block> priorityBlocks = Arrays.asList(
        Blocks.OBSIDIAN,           // 黑曜石 - 最高优先级
        Blocks.CRYING_OBSIDIAN,    // 哭泣的黑曜石
        Blocks.STONE,              // 石头
        Blocks.COBBLESTONE,        // 圆石
        Blocks.STONE_BRICKS,       // 石砖
        Blocks.DEEPSLATE,          // 深板岩
        Blocks.COBBLED_DEEPSLATE,  // 深板岩圆石
        Blocks.OAK_PLANKS,         // 橡木木板
        Blocks.SPRUCE_PLANKS,      // 云杉木板
        Blocks.BIRCH_PLANKS,       // 白桦木板
        Blocks.JUNGLE_PLANKS,      // 丛林木板
        Blocks.ACACIA_PLANKS,      // 金合欢木板
        Blocks.DARK_OAK_PLANKS,    // 深色橡木木板
        Blocks.MANGROVE_PLANKS,    // 红树木板
        Blocks.CHERRY_PLANKS,      // 樱花木板
        Blocks.BAMBOO_PLANKS,      // 竹木板
        Blocks.DIRT,               // 泥土
        Blocks.NETHERRACK          // 下界岩
    );

    public AutoCrystalBlock() {
        super("自动挡水晶", "检测到附近末地水晶时自动放置方块抵挡");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        // 处理放置延迟
        if (placeTimer > 0) {
            placeTimer--;
            return;
        }

        // 检测附近的END_CRYSTAL
        List<EndCrystalEntity> nearbyCrystals = findNearbyCrystals();
        if (nearbyCrystals.isEmpty()) {
            // 如果没有水晶了，清空已放置位置记录
            placedPositions.clear();
            return;
        }

        // 为每个水晶尝试放置防护方块
        for (EndCrystalEntity crystal : nearbyCrystals) {
            if (tryPlaceBlockForCrystal(crystal)) {
                placeTimer = placeDelay.get();
                break; // 一次只放置一个方块
            }
        }
    }

    /**
     * 查找附近的END_CRYSTAL实体
     */
    private List<EndCrystalEntity> findNearbyCrystals() {
        List<EndCrystalEntity> crystals = new ArrayList<>();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity crystal) {
                if (PlayerUtils.isWithin(entity, range.get())) {
                    crystals.add(crystal);
                }
            }
        }

        return crystals;
    }

    /**
     * 尝试为指定水晶放置防护方块
     */
    private boolean tryPlaceBlockForCrystal(EndCrystalEntity crystal) {
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos crystalPos = crystal.getBlockPos();

        // 计算需要放置方块的位置
        List<BlockPos> candidatePositions = calculateBlockPositions(playerPos, crystalPos);

        for (BlockPos pos : candidatePositions) {
            // 检查位置是否已经有方块或已经放置过
            if (!mc.world.getBlockState(pos).isAir() || placedPositions.contains(pos)) {
                continue;
            }

            // 查找合适的方块
            FindItemResult blockResult = findBestBlock();
            if (!blockResult.found()) {
                showNoBlockError();
                return false;
            }

            // 尝试放置方块
            if (placeBlockAt(pos, blockResult)) {
                placedPositions.add(pos);
                info("在 " + pos.toShortString() + " 放置防护方块抵挡水晶");
                return true;
            }
        }

        return false;
    }

    /**
     * 计算需要放置方块的候选位置
     */
    private List<BlockPos> calculateBlockPositions(BlockPos playerPos, BlockPos crystalPos) {
        List<BlockPos> positions = new ArrayList<>();

        if (onlyAtFeet.get()) {
            // 只在玩家脚边放置
            Vec3d playerVec = playerPos.toCenterPos();
            Vec3d crystalVec = crystalPos.toCenterPos();
            Vec3d direction = crystalVec.subtract(playerVec).normalize();

            // 在玩家周围的8个方向尝试放置
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue; // 跳过玩家脚下

                    BlockPos candidatePos = playerPos.add(x, 0, z);
                    positions.add(candidatePos);
                }
            }
        } else {
            // 在玩家和水晶之间放置
            Vec3d playerVec = playerPos.toCenterPos();
            Vec3d crystalVec = crystalPos.toCenterPos();
            Vec3d direction = crystalVec.subtract(playerVec).normalize();

            // 在玩家前方1-2格的位置放置
            for (int distance = 1; distance <= 2; distance++) {
                Vec3d targetVec = playerVec.add(direction.multiply(distance));
                BlockPos targetPos = BlockPos.ofFloored(targetVec);
                positions.add(targetPos);
                positions.add(targetPos.up()); // 也尝试上方一格
            }
        }

        return positions;
    }

    /**
     * 查找背包中最好的方块
     */
    private FindItemResult findBestBlock() {
        for (Block block : priorityBlocks) {
            // 检查设置是否允许使用该类型方块
            if (!isBlockTypeAllowed(block)) continue;

            FindItemResult result = InvUtils.find(itemStack -> {
                if (itemStack.getItem() instanceof BlockItem blockItem) {
                    return blockItem.getBlock() == block;
                }
                return false;
            });

            if (result.found()) {
                return result;
            }
        }

        return new FindItemResult(0, 0);
    }

    /**
     * 检查方块类型是否被允许使用
     */
    private boolean isBlockTypeAllowed(Block block) {
        if (block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN) {
            return useObsidian.get();
        }
        if (block == Blocks.STONE || block == Blocks.STONE_BRICKS ||
            block == Blocks.DEEPSLATE || block == Blocks.COBBLED_DEEPSLATE) {
            return useStone.get();
        }
        if (block == Blocks.COBBLESTONE) {
            return useCobblestone.get();
        }
        if (block.getDefaultState().toString().contains("planks")) {
            return useWood.get();
        }

        return true; // 其他方块默认允许
    }

    /**
     * 在指定位置放置方块
     */
    private boolean placeBlockAt(BlockPos pos, FindItemResult blockResult) {
        // 切换到对应物品
        BagUtil.doSwap(blockResult.slot());

        // 使用BlockUtilGrim放置方块
        boolean success = BaritoneUtil.placeBlock(pos, true, true, true);

        BagUtil.doSwap(blockResult.slot());


        return success;
    }

    /**
     * 显示没有方块的错误信息（带冷却）
     */
    private void showNoBlockError() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastErrorTime >= ERROR_COOLDOWN) {
            error("背包中没有可用的防护方块！");
            lastErrorTime = currentTime;
        }
    }

    @Override
    public void onActivate() {
        if (mc == null) {
            mc = MinecraftClient.getInstance();
        }
    }

    @Override
    public void onDeactivate() {
        placedPositions.clear();
        placeTimer = 0;
    }
}
