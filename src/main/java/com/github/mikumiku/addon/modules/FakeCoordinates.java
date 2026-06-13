package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.mixinface.MagicMix;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;

import java.security.SecureRandom;
import java.util.Random;

public class FakeCoordinates extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgOffset = settings.createGroup("偏移设置");
    private final SettingGroup sgProtection = settings.createGroup("保护设置");
    private final SettingGroup sgAdvanced = settings.createGroup("高级选项");


    private final Setting<String> fakeName = sgGeneral.add(new StringSetting.Builder()
        .name("自定义模块名")
        .description("防止穿帮")
        .defaultValue("假坐标")
        .onChanged(s -> setFakeName(s))
        .build()
    );

    // 通用设置
    private final Setting<ObfuscationMode> mode = sgGeneral.add(new EnumSetting.Builder<ObfuscationMode>()
        .name("混淆模式")
        .description("选择坐标混淆的方式")
        .defaultValue(ObfuscationMode.RANDOM_OFFSET)
        .build()
    );


    private final Setting<Boolean> liveMode = sgGeneral.add(new BoolSetting.Builder()
        .name("直播模式")
        .description("启用时尽量防止穿帮 不输出任何信息")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> minimap = sgGeneral.add(new BoolSetting.Builder()
        .name("支持小地图")
        .description("支持小地图")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> minihud = sgGeneral.add(new BoolSetting.Builder()
        .name("支持minihud")
        .description("支持minihud")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> worldmap = sgGeneral.add(new BoolSetting.Builder()
        .name("支持worldmap")
        .description("支持")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> validateSetup = sgGeneral.add(new BoolSetting.Builder()
        .name("验证配置")
        .description("启用时验证混淆设置是否正确")
        .defaultValue(true)
        .build()
    );

    // 偏移设置
    private final Setting<Integer> randomMinDistanceFromSelf = sgOffset.add(new IntSetting.Builder()
        .name("随机最小自身距离")
        .description("随机偏移时与真实坐标的最小距离（区块）")
        .defaultValue(100000)
        .min(0)
        .sliderMax(1000000)
        .visible(() -> mode.get() == ObfuscationMode.RANDOM_OFFSET)
        .build()
    );

    private final Setting<Integer> randomMinDistanceFromSpawn = sgOffset.add(new IntSetting.Builder()
        .name("随机最小出生点距离")
        .description("随机偏移时与出生点的最小距离（区块）")
        .defaultValue(100000)
        .min(0)
        .sliderMax(1000000)
        .visible(() -> mode.get() == ObfuscationMode.RANDOM_OFFSET)
        .build()
    );

    private final Setting<Integer> randomMaxDistanceFromSpawn = sgOffset.add(new IntSetting.Builder()
        .name("随机最大出生点距离")
        .description("随机偏移时与出生点的最大距离（区块）")
        .defaultValue(29000000)
        .min(0)
        .sliderMax(29999999)
        .visible(() -> mode.get() == ObfuscationMode.RANDOM_OFFSET)
        .build()
    );

    private final Setting<Integer> constantOffsetX = sgOffset.add(new IntSetting.Builder()
        .name("固定偏移X")
        .description("X轴的固定偏移量（区块）")
        .defaultValue(0)
        .sliderMin(-29999999)
        .sliderMax(29999999)
        .visible(() -> mode.get() == ObfuscationMode.CONSTANT_OFFSET)
        .build()
    );

    private final Setting<Integer> constantOffsetZ = sgOffset.add(new IntSetting.Builder()
        .name("固定偏移Z")
        .description("Z轴的固定偏移量（区块）")
        .defaultValue(0)
        .sliderMin(-29999999)
        .sliderMax(29999999)
        .visible(() -> mode.get() == ObfuscationMode.CONSTANT_OFFSET)
        .build()
    );

    private final Setting<Boolean> constantOffsetNetherTranslate = sgOffset.add(new BoolSetting.Builder()
        .name("下界坐标转换")
        .description("在下界自动调整偏移比例")
        .defaultValue(true)
        .visible(() -> mode.get() == ObfuscationMode.CONSTANT_OFFSET)
        .build()
    );

    private final Setting<Integer> constantOffsetMinSpawnDistance = sgOffset.add(new IntSetting.Builder()
        .name("最小出生点距离")
        .description("真实坐标距离出生点的最小距离，否则断开连接（区块）")
        .defaultValue(100000)
        .min(0)
        .sliderMax(1000000)
        .visible(() -> mode.get() == ObfuscationMode.CONSTANT_OFFSET)
        .build()
    );

    private final Setting<Integer> atLocationX = sgOffset.add(new IntSetting.Builder()
        .name("指定位置X")
        .description("将玩家显示在此X坐标（区块）")
        .defaultValue(0)
        .sliderMin(-29999999)
        .sliderMax(29999999)
        .visible(() -> mode.get() == ObfuscationMode.AT_LOCATION)
        .build()
    );

    private final Setting<Integer> atLocationZ = sgOffset.add(new IntSetting.Builder()
        .name("指定位置Z")
        .description("将玩家显示在此Z坐标（区块）")
        .defaultValue(0)
        .sliderMin(-29999999)
        .sliderMax(29999999)
        .visible(() -> mode.get() == ObfuscationMode.AT_LOCATION)
        .build()
    );

    private final Setting<Integer> teleportOffsetRegenerateDistanceMin = sgOffset.add(new IntSetting.Builder()
        .name("传送重新生成距离")
        .description("传送超过此距离时重新生成偏移坐标（区块）")
        .defaultValue(64)
        .min(0)
        .sliderMax(1000)
        .build()
    );

    // 保护设置
    private final Setting<Boolean> disconnectWhileEyeOfEnderPresent = sgProtection.add(new BoolSetting.Builder()
        .name("末影之眼时断连")
        .description("存在末影之眼时自动断开连接以防止坐标泄露")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> disconnectWhileNearOffsetBlocks = sgProtection.add(new BoolSetting.Builder()
        .name("可能泄露坐标时断连")
        .description("接近可能泄露坐标的偏移方块时断开连接")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> delayPlayerLoginsAfterTpMs = sgProtection.add(new IntSetting.Builder()
        .name("传送后登录延迟")
        .description("传送后延迟多久才允许玩家登录（毫秒）")
        .defaultValue(1000)
        .min(0)
        .sliderMax(10000)
        .build()
    );

    // 直播模式保护设置
    private final Setting<Boolean> liveModeAntiF3 = sgProtection.add(new BoolSetting.Builder()
        .name("直播模式防F3")
        .description("直播模式下禁用F3调试信息")
        .defaultValue(true)
        .visible(liveMode::get)
        .build()
    );

    private final Setting<Boolean> liveModeAntiCoords = sgProtection.add(new BoolSetting.Builder()
        .name("直播模式防坐标泄露")
        .description("直播模式下自动防止可能的坐标泄露行为")
        .defaultValue(true)
        .visible(liveMode::get)
        .build()
    );

    // 高级选项
    private final Setting<Boolean> obfuscateBedrock = sgAdvanced.add(new BoolSetting.Builder()
        .name("混淆基岩")
        .description("混淆基岩层数据")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> obfuscateChunkHeightmap = sgAdvanced.add(new BoolSetting.Builder()
        .name("混淆区块高度图")
        .description("混淆区块的高度图数据")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> obfuscateChunkLighting = sgAdvanced.add(new BoolSetting.Builder()
        .name("混淆区块光照")
        .description("混淆区块的光照数据")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> obfuscateBiomes = sgAdvanced.add(new BoolSetting.Builder()
        .name("混淆生物群系")
        .description("将所有生物群系替换为指定类型")
        .defaultValue(false)
        .build()
    );

    private final Setting<String> obfuscateBiomesKey = sgAdvanced.add(new StringSetting.Builder()
        .name("生物群系类型")
        .description("用于替换的生物群系类型")
        .defaultValue("plains")
        .visible(obfuscateBiomes::get)
        .build()
    );

    // 静态变量用于存储偏移后的坐标
    public static double x = 0;
    public static double z = 0;
    // 原点坐标（模块激活时的位置）
    public static double originx = 0;
    public static double originz = 0;

    // 私有变量
    private final Random random = new SecureRandom();
    private int offsetChunkX = 0;
    private int offsetChunkZ = 0;
    private double lastPosX = 0;
    private double lastPosZ = 0;
    private boolean needsOffsetRegeneration = false;

    // 直播模式相关变量
    private long lastF3PressTime = 0;
    private boolean f3Blocked = false;

    public FakeCoordinates() {
        super(BaseModule.CATEGORY_MIKU_PRO, "假坐标", "通过偏移和混淆数据包来隐藏你的真实坐标位置, 都是动态的");
    }

    public enum ObfuscationMode {
        RANDOM_OFFSET("随机偏移"),
        CONSTANT_OFFSET("固定偏移"),
        AT_LOCATION("指定位置");

        private final String title;

        ObfuscationMode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    @Override
    public void onActivate() {
        super.onActivate();

        // 获取玩家当前位置作为原点
        if (mc.player != null) {
            originx = mc.player.getX();
            originz = mc.player.getZ();
            lastPosX = originx;
            lastPosZ = originz;

            // 生成初始偏移
            generateOffset(originx, originz);

            // 只在非直播模式下输出信息
            if (!liveMode.get()) {
                info("假坐标已启用 - 原点: (%.1f, %.1f), 偏移: (%d, %d)",
                    originx, originz, offsetChunkX, offsetChunkZ);
            }
        } else {
            originx = 0;
            originz = 0;
            lastPosX = 0;
            lastPosZ = 0;
        }
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();

        // 重置所有值
        MagicMix.x = 0;
        MagicMix.z = 0;
        originx = 0;
        originz = 0;
        offsetChunkX = 0;
        offsetChunkZ = 0;
        lastPosX = 0;
        lastPosZ = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        double currentX = mc.player.getX();
        double currentZ = mc.player.getZ();

        // 直播模式保护
        if (liveMode.get()) {
            handleLiveModeProtections();
        }

        // 检查是否需要重新生成偏移（传送检测）
        double moveDistance = Math.sqrt(
            Math.pow(currentX - lastPosX, 2) +
                Math.pow(currentZ - lastPosZ, 2)
        );

        if (moveDistance > teleportOffsetRegenerateDistanceMin.get() * 16) {
            // 只在非直播模式下输出信息
            if (!liveMode.get()) {
                info("检测到长距离移动 (%.1f 方块), 重新生成偏移", moveDistance);
            }
            generateOffset(currentX, currentZ);
        }

        // 更新最后位置
        lastPosX = currentX;
        lastPosZ = currentZ;

        // 计算并更新偏移后的坐标
        calculateObfuscatedCoordinates(currentX, currentZ);
    }

    /**
     * 生成坐标偏移
     */
    private void generateOffset(double playerX, double playerZ) {
        switch (mode.get()) {
            case RANDOM_OFFSET -> generateRandomOffset(playerX, playerZ);
            case CONSTANT_OFFSET -> generateConstantOffset(playerX, playerZ);
            case AT_LOCATION -> generateLocationOffset(playerX, playerZ);
        }
    }

    /**
     * 随机偏移模式
     */
    private void generateRandomOffset(double playerX, double playerZ) {
        int tries = 0;
        while (tries < 100) {
            tries++;

            int randomX = generateRandomPos();
            int randomZ = generateRandomPos();

            // 检查距离要求
            double distanceFromSelf = Math.sqrt(
                Math.pow(randomX - playerX, 2) +
                    Math.pow(randomZ - playerZ, 2)
            );

            if (distanceFromSelf < randomMinDistanceFromSelf.get() * 16) {
                continue; // 距离太近，重试
            }

            // 计算区块偏移
            int playerChunkX = (int) Math.floor(playerX / 16);
            int playerChunkZ = (int) Math.floor(playerZ / 16);
            offsetChunkX = (randomX / 16) - playerChunkX;
            offsetChunkZ = (randomZ / 16) - playerChunkZ;

            return;
        }

        // 只在非直播模式下输出警告信息
        if (!liveMode.get()) {
            warning("随机偏移生成失败，使用默认偏移");
        }
        offsetChunkX = 100000;
        offsetChunkZ = 100000;
    }

    /**
     * 生成随机位置
     */
    private int generateRandomPos() {
        int min = randomMinDistanceFromSpawn.get();
        int max = randomMaxDistanceFromSpawn.get();
        int distance = random.nextInt(min, max + 1);
        return distance * (random.nextBoolean() ? 1 : -1);
    }

    /**
     * 固定偏移模式
     */
    private void generateConstantOffset(double playerX, double playerZ) {
        // 检查是否距离出生点太近
        double distanceFromSpawn = Math.sqrt(playerX * playerX + playerZ * playerZ);
        if (distanceFromSpawn < constantOffsetMinSpawnDistance.get() * 16) {
            // 只在非直播模式下输出警告信息
            if (!liveMode.get()) {
                warning("距离出生点太近 (%.1f < %d), 无法使用固定偏移模式",
                    distanceFromSpawn / 16, constantOffsetMinSpawnDistance.get());
            }
            offsetChunkX = random.nextInt(100000, 999999);
            offsetChunkZ = random.nextInt(100000, 999999);
            return;
        }

        // 下界坐标转换
        if (constantOffsetNetherTranslate.get() && isInNether()) {
            offsetChunkX = (constantOffsetX.get() / 16) / 8;
            offsetChunkZ = (constantOffsetZ.get() / 16) / 8;
        } else {
            offsetChunkX = constantOffsetX.get() / 16;
            offsetChunkZ = constantOffsetZ.get() / 16;
        }
    }

    /**
     * 指定位置模式
     */
    private void generateLocationOffset(double playerX, double playerZ) {
        int playerChunkX = (int) Math.floor(playerX / 16);
        int playerChunkZ = (int) Math.floor(playerZ / 16);

        int targetChunkX = atLocationX.get() / 16;
        int targetChunkZ = atLocationZ.get() / 16;

        offsetChunkX = targetChunkX - playerChunkX;
        offsetChunkZ = targetChunkZ - playerChunkZ;
    }

    /**
     * 计算混淆后的坐标
     */
    private void calculateObfuscatedCoordinates(double playerX, double playerZ) {
        // 应用区块偏移到实际坐标
        MagicMix.x = playerX + (offsetChunkX * 16);
        MagicMix.z = playerZ + (offsetChunkZ * 16);
    }

    /**
     * 检查是否在下界
     */
    private boolean isInNether() {
        if (mc.world == null) return false;
        return mc.world.getRegistryKey().getValue().getPath().equals("the_nether");
    }

    /**
     * 处理直播模式保护措施
     */
    private void handleLiveModeProtections() {
        // 防F3调试信息
        if (liveModeAntiF3.get()) {
            // 检测F3按键按下
//            if (mc.options.deb.isPressed()) {
//                long currentTime = System.currentTimeMillis();
//                if (currentTime - lastF3PressTime > 1000) { // 1秒冷却
//                    lastF3PressTime = currentTime;
//                    f3Blocked = true;
//
//                    // 模拟F3键释放以防止调试界面显示
//                    mc.options.debugKey.setPressed(false);
//
//                    // 发送警告提示（仅在非直播模式时显示）
//                    // 直播模式下静默处理
//                }
//            }
        }

        // 防坐标泄露检测
        if (liveModeAntiCoords.get()) {
            // 检测可能泄露坐标的行为
            detectPotentialCoordinateLeaks();
        }
    }

    /**
     * 检测可能的坐标泄露行为
     */
    private void detectPotentialCoordinateLeaks() {
        // 这里可以添加更多检测逻辑
        // 例如：检测玩家查看特定方块、使用特定物品等

        // 简单示例：检测玩家是否在看地图或相关物品
        if (mc.player != null && mc.player.getMainHandStack() != null) {
            // 可以扩展检测更多可能泄露坐标的物品或行为
        }
    }

    /**
     * 检查是否在直播模式
     */
    public boolean isInLiveMode() {
        return liveMode.get();
    }

    private static void setFakeName(String s) {

    }

    /**
     * 获取当前偏移信息（用于调试）
     */
    public String getOffsetInfo() {
        // 直播模式下返回模糊信息
        if (liveMode.get()) {
            return "直播模式：坐标信息已隐藏";
        }

        return String.format("模式: %s | 偏移: (%d, %d) 区块 | 假坐标: (%.1f, %.1f)",
            mode.get().toString(),
            offsetChunkX, offsetChunkZ,
            MagicMix.x, MagicMix.z);
    }
}
