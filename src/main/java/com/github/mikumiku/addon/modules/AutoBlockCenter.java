package com.github.mikumiku.addon.modules;


import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.Via;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;

public class AutoBlockCenter extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("速度")
        .description("移动到方块中心的速度")
        .defaultValue(0.2)
        .min(0.01)
        .max(1.0)
        .sliderMin(0.01)
        .sliderMax(1.0)
        .build()
    );

    private final Setting<Boolean> onlyGround = sgGeneral.add(new BoolSetting.Builder()
        .name("仅在地面")
        .description("只有在地面上时才自动居中")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> keepY = sgGeneral.add(new BoolSetting.Builder()
        .name("保持Y轴")
        .description("移动时保持Y轴位置不变")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> minSft = sgGeneral.add(new DoubleSetting.Builder()
        .name("最小偏移")
        .description("触发居中的最小偏移距离")
        .defaultValue(0.01)
        .min(0.001)
        .max(0.2)
        .sliderMin(0.001)
        .sliderMax(0.2)
        .build()
    );

    public AutoBlockCenter() {
        super(BaseModule.CATEGORY_MIKU_COMBAT, "拉方块中心", "自动将玩家移动到方块中心位置 (x.3-x.7, z.3-z.7)");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        // 如果设置了仅在地面且玩家不在地面，则不执行
        if (onlyGround.get() && !mc.player.isOnGround()) {
            toggle();
            return;
        }

        if (isPlayerCentered()) {
            toggle();
            return;
        }
        Vec3d pos = Via.getEntityPos(mc.player);
        double currentX = pos.x;
        double currentZ = pos.z;
        double currentY = pos.y;

        // 计算目标中心位置
        double targetX = getBlockCenter(currentX);
        double targetZ = getBlockCenter(currentZ);

        // 计算偏移距离
        double offsetX = targetX - currentX;
        double offsetZ = targetZ - currentZ;
        double totalOffset = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);

        // 如果偏移距离小于最小值，则不移动
        if (totalOffset < minSft.get()) {
            toggle();
            return;
        }

        // 计算移动向量
        double speed = Math.min(this.speed.get(), totalOffset);
        double moveX = (offsetX / totalOffset) * speed;
        double moveZ = (offsetZ / totalOffset) * speed;

        // 设置玩家位置
        if (keepY.get()) {
            mc.player.setPosition(currentX + moveX, currentY, currentZ + moveZ);
        } else {
            mc.player.setPosition(currentX + moveX, pos.y, currentZ + moveZ);
        }
    }

    /**
     * 获取方块中心坐标
     * 方块中心定义为 xxx.3 到 xxx.7 之间，理想中心为 xxx.5
     */
    private double getBlockCenter(double coord) {
        double decimal = coord - Math.floor(coord);

        // 如果已经在 0.3 到 0.7 之间，目标是 0.5
        if (decimal >= 0.3 && decimal <= 0.7) {
            return Math.floor(coord) + 0.5;
        }

        // 如果在 0.0 到 0.3 之间，移动到 0.5
        if (decimal < 0.3) {
            return Math.floor(coord) + 0.5;
        }

        // 如果在 0.7 到 1.0 之间，移动到 0.5
        return Math.floor(coord) + 0.5;
    }

    /**
     * 检查坐标是否在方块中心范围内
     */
    private boolean isInBlockCenter(double coord) {
        double decimal = coord - Math.floor(coord);
        return decimal >= 0.3 && decimal <= 0.7;
    }

    /**
     * 检查玩家是否已在方块中心
     */
    public boolean isPlayerCentered() {
        if (mc.player == null) return false;
        Vec3d pos = Via.getEntityPos(mc.player);
        return isInBlockCenter(pos.x) && isInBlockCenter(pos.z);
    }
}
