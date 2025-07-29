package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;


public class LitematicaMover extends BaseModule {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgMovement = this.settings.createGroup("移动设置");

    // 设置项
    private final Setting<Direction> direction = sgMovement.add(new EnumSetting.Builder<Direction>()
        .name("移动方向")
        .description("投影移动的方向。")
        .defaultValue(Direction.NORTH)
        .build()
    );

    private final Setting<Integer> speed = sgMovement.add(new IntSetting.Builder()
        .name("移动速度")
        .description("移动速度（以游戏刻为单位，20刻 = 1秒）。")
        .defaultValue(20)
        .min(1)
        .max(200)
        .sliderMax(100)
        .build()
    );

    private final Setting<Boolean> autoDetect = sgGeneral.add(new BoolSetting.Builder()
        .name("自动检测")
        .description("自动检测并移动当前选中的投影。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> logMovement = sgGeneral.add(new BoolSetting.Builder()
        .name("记录移动")
        .description("在聊天栏中记录移动操作。")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> followPlayer = sgGeneral.add(new BoolSetting.Builder()
        .name("跟随玩家")
        .description("让投影跟随玩家的水平移动。")
        .defaultValue(false)
        .build()
    );

    // 内部变量
    private int tickCounter = 0;
    private SchematicPlacement currentPlacement = null;

    // 跟随玩家变量
    private BlockPos lastPlayerPos = null;
    private BlockPos relativeOffset = null;
    private boolean isFollowingInitialized = false;

    public LitematicaMover() {
        super("投影跟随", "自动移动当前选定的 Litematica 投影原理图. 需要安装Litematica.");
    }

    @Override
    public void onActivate() {
        tickCounter = 0;
        isFollowingInitialized = false;
        lastPlayerPos = null;
        relativeOffset = null;

        if (logMovement.get()) {
            info("投影移动器已启用");
        }

        // 激活时尝试获取当前投影
        getCurrentPlacement();

        // 如果启用了跟随玩家功能，则初始化
        if (followPlayer.get()) {
            initializeFollowPlayer();
        }
    }

    @Override
    public void onDeactivate() {
        if (logMovement.get()) {
            info("投影移动器已停用");
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (MinecraftClient.getInstance().player == null || MinecraftClient.getInstance().world == null) {
            return;
        }

        // 处理跟随玩家逻辑
        if (followPlayer.get()) {
            handleFollowPlayer();
        } else {
            // 普通移动逻辑
            tickCounter++;

            // 每隔指定刻数移动一次
            if (tickCounter >= speed.get()) {
                tickCounter = 0;
                moveSchematic();
            }
        }
    }

    private SchematicPlacement getCurrentPlacement() {
        if (!autoDetect.get()) {
            return currentPlacement;
        }

        try {
            SchematicPlacementManager placementManager = DataManager.getSchematicPlacementManager();
            if (placementManager != null) {
                currentPlacement = placementManager.getSelectedSchematicPlacement();
            }
        } catch (Exception e) {
            if (logMovement.get()) {
                error("获取当前投影位置失败: " + e.getMessage());
            }
        }

        return currentPlacement;
    }

    private void moveSchematic() {
        SchematicPlacement placement = getCurrentPlacement();

        if (placement == null) {
            if (logMovement.get()) {
                warning("未选择投影");
            }
            return;
        }

        try {
            // 获取当前原点位置
            BlockPos currentOrigin = placement.getOrigin();

            // 根据方向计算新位置
            BlockPos newOrigin = currentOrigin.offset(direction.get());

            // 设置新的原点位置
            placement.setOrigin(newOrigin, null);

            if (logMovement.get()) {
                info("投影已从 " + currentOrigin.toShortString() + " 移动到 " + newOrigin.toShortString());
            }

        } catch (Exception e) {
            if (logMovement.get()) {
                error("移动投影失败: " + e.getMessage());
            }
        }
    }

    // 不同移动方向的工具方法
    public void moveNorth() {
        direction.set(Direction.NORTH);
    }

    public void moveSouth() {
        direction.set(Direction.SOUTH);
    }

    public void moveEast() {
        direction.set(Direction.EAST);
    }

    public void moveWest() {
        direction.set(Direction.WEST);
    }

    public void moveUp() {
        direction.set(Direction.UP);
    }

    public void moveDown() {
        direction.set(Direction.DOWN);
    }

    // 外部访问的获取方法
    public Direction getCurrentDirection() {
        return direction.get();
    }

    public int getCurrentSpeed() {
        return speed.get();
    }

    public boolean isAutoDetectEnabled() {
        return autoDetect.get();
    }

    // 跟随玩家方法
    private void initializeFollowPlayer() {
        if (MinecraftClient.getInstance().player == null) {
            return;
        }

        SchematicPlacement placement = getCurrentPlacement();
        if (placement == null) {
            if (logMovement.get()) {
                warning("未找到投影，无法初始化跟随玩家功能");
            }
            return;
        }

        try {
            BlockPos playerPos = MinecraftClient.getInstance().player.getBlockPos();
            BlockPos schematicPos = placement.getOrigin();

            // 计算相对偏移量（仅X和Z轴，忽略Y轴）
            relativeOffset = new BlockPos(
                schematicPos.getX() - playerPos.getX(),
                0, // 不跟踪Y轴偏移
                schematicPos.getZ() - playerPos.getZ()
            );

            lastPlayerPos = new BlockPos(playerPos.getX(), 0, playerPos.getZ());
            isFollowingInitialized = true;

            if (logMovement.get()) {
                info("跟随玩家功能已初始化。相对偏移: " + relativeOffset.getX() + ", " + relativeOffset.getZ());
            }
        } catch (Exception e) {
            if (logMovement.get()) {
                error("初始化跟随玩家功能失败: " + e.getMessage());
            }
        }
    }

    private void handleFollowPlayer() {
        if (MinecraftClient.getInstance().player == null) {
            return;
        }

        // 如果尚未初始化则进行初始化
        if (!isFollowingInitialized) {
            initializeFollowPlayer();
            return;
        }

        try {
            BlockPos currentPlayerPos = MinecraftClient.getInstance().player.getBlockPos();
            BlockPos currentPlayerPosFlat = new BlockPos(currentPlayerPos.getX(), 0, currentPlayerPos.getZ());

            // 检查玩家是否水平移动
            if (lastPlayerPos == null || !lastPlayerPos.equals(currentPlayerPosFlat)) {
                moveSchematicToFollowPlayer(currentPlayerPosFlat);
                lastPlayerPos = currentPlayerPosFlat;
            }
        } catch (Exception e) {
            if (logMovement.get()) {
                error("处理跟随玩家功能失败: " + e.getMessage());
            }
        }
    }

    private void moveSchematicToFollowPlayer(BlockPos playerPos) {
        SchematicPlacement placement = getCurrentPlacement();
        if (placement == null || relativeOffset == null) {
            return;
        }

        try {
            BlockPos currentSchematicPos = placement.getOrigin();

            // 根据玩家位置和相对偏移量计算新的投影位置
            BlockPos newSchematicPos = new BlockPos(
                playerPos.getX() + relativeOffset.getX(),
                currentSchematicPos.getY(), // 保持原始Y位置
                playerPos.getZ() + relativeOffset.getZ()
            );

            // 只有位置真正改变时才移动
            if (!currentSchematicPos.equals(newSchematicPos)) {
                placement.setOrigin(newSchematicPos, null);

                if (logMovement.get()) {
                    info("跟随玩家: 投影已移动到 " + newSchematicPos.toShortString());
                }
            }
        } catch (Exception e) {
            if (logMovement.get()) {
                error("跟随玩家移动投影失败: " + e.getMessage());
            }
        }
    }

    // 重置跟随玩家功能的工具方法
    public void resetFollowPlayer() {
        isFollowingInitialized = false;
        lastPlayerPos = null;
        relativeOffset = null;
        if (logMovement.get()) {
            info("跟随玩家功能已重置");
        }
    }

    // 跟随玩家状态的获取方法
    public boolean isFollowPlayerEnabled() {
        return followPlayer.get();
    }

    public boolean isFollowPlayerInitialized() {
        return isFollowingInitialized;
    }

    public BlockPos getRelativeOffset() {
        return relativeOffset;
    }
}
