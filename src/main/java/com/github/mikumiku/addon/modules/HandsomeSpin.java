package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.VUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

/**
 * 靓仔转圈模块 - 惊呆所有人
 *
 * <p>让玩家以固定角度持续旋转，营造炫酷的视觉效果。
 * 当玩家移动时会自动暂停旋转，避免影响正常游戏操作。</p>
 *
 * <p>特色功能：</p>
 * <ul>
 *   <li>每tick精确旋转指定角度</li>
 *   <li>智能检测玩家移动状态</li>
 *   <li>可自定义旋转速度和方向</li>
 *   <li>平滑的视角过渡效果</li>
 *   <li>仅发包模式：隐形旋转，客户端视角不变</li>
 *   <li>炫酷特效和里程碑成就系统</li>
 * </ul>
 *
 * @author GGB Helper
 * @since 1.0.0
 */
public class HandsomeSpin extends BaseModule {

    // 设置组
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // 旋转角度设置
    private final Setting<Double> rotationAngle = sgGeneral.add(new DoubleSetting.Builder()
        .name("旋转角度")
        .description("每tick旋转的角度（度）")
        .defaultValue(45.0)
        .min(-360.0)
        .max(360.0)
        .sliderMin(-180.0)
        .sliderMax(180.0)
        .build());

    // 是否启用移动检测
    private final Setting<Boolean> pauseOnMovement = sgGeneral.add(new BoolSetting.Builder()
        .name("移动时暂停")
        .description("当玩家移动时暂停旋转")
        .defaultValue(true)
        .build());

    // 是否显示状态信息
    private final Setting<Boolean> showStatus = sgGeneral.add(new BoolSetting.Builder()
        .name("显示状态")
        .description("在聊天中显示旋转状态信息")
        .defaultValue(true)
        .build());

    // 是否启用炫酷特效
    private final Setting<Boolean> hand = sgGeneral.add(new BoolSetting.Builder()
        .name("挥手")
        .description("启用挥手")
        .defaultValue(true)
        .build());
    
    // 是否启用炫酷特效
    private final Setting<Boolean> coolEffects = sgGeneral.add(new BoolSetting.Builder()
        .name("炫酷特效")
        .description("启用额外的炫酷提示信息")
        .defaultValue(true)
        .build());

    // 是否仅发包模式
    private final Setting<Boolean> packetOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("仅发包")
        .description("只向服务器发送旋转包，客户端玩家朝向不变")
        .defaultValue(false)
        .build());

    // 内部状态变量
    private float currentYaw = 0.0f;
    private BlockPos lastPlayerPos = null;
    private boolean isSpinning = false;
    private int tickCounter = 0;

    public HandsomeSpin() {
        super("靓仔转圈", "惊呆所有人的炫酷旋转效果！");
    }

    @Override
    public void onActivate() {
        mc = MinecraftClient.getInstance();

        if (mc.player == null) {
            error("玩家不存在，无法启动靓仔转圈！");
            toggle();
            return;
        }

        // 初始化状态
        currentYaw = mc.player.getYaw();
        lastPlayerPos = mc.player.getBlockPos();
        isSpinning = true;
        tickCounter = 0;

        if (showStatus.get()) {
            if (coolEffects.get()) {
                info("🌟✨ 靓仔转圈模式启动！准备惊呆所有人！ ✨🌟");
                info("🔥 你就是全场最靓的仔！ 🔥");
                info("💫 旋转角度: " + rotationAngle.get() + "°/tick 💫");
                info("⚡ 移动暂停: " + (pauseOnMovement.get() ? "启用" : "禁用") + " ⚡");
                info("📡 仅发包模式: " + (packetOnly.get() ? "启用（隐形旋转）" : "禁用（可见旋转）") + " 📡");
            } else {
                info("🌟 靓仔转圈模式启动！准备惊呆所有人！ 🌟");
                info("旋转角度: " + rotationAngle.get() + "°/tick");
                info("移动暂停: " + (pauseOnMovement.get() ? "启用" : "禁用"));
                info("仅发包模式: " + (packetOnly.get() ? "启用" : "禁用"));
            }
        }
    }

    @Override
    public void onDeactivate() {
        if (showStatus.get()) {
            info("🎭 靓仔转圈结束，感谢观赏！");
            info("总共旋转了 " + tickCounter + " 个tick");
        }

        // 重置状态
        isSpinning = false;
        tickCounter = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // 检查是否需要暂停（玩家移动检测）
        boolean shouldPause = false;
        if (pauseOnMovement.get()) {
            shouldPause = isPlayerMoving();
        }

        if (shouldPause) {
            if (isSpinning) {
                isSpinning = false;
                if (showStatus.get()) {
                    info("⏸️ 检测到移动，暂停旋转");
                }
            }
            // 更新最后位置
            lastPlayerPos = mc.player.getBlockPos();
            return;
        } else {
            if (!isSpinning) {
                isSpinning = true;
                if (showStatus.get()) {
                    info("▶️ 恢复旋转，继续惊呆众人！");
                }
            }
        }

        // 执行旋转
        performSpin();
        tickCounter++;

        // 检查里程碑
        checkMilestones();

        // 每100tick显示一次状态（避免刷屏）
        if (showStatus.get() && tickCounter % 100 == 0) {
            if (coolEffects.get()) {
                String[] coolMessages = {
                    "🌪️ 旋风靓仔！已转 " + tickCounter + " tick！ 🌪️",
                    "⭐ 闪耀登场！转了 " + tickCounter + " 圈！ ⭐",
                    "🎭 全场焦点！" + tickCounter + " tick的精彩表演！ 🎭",
                    "🚀 超级旋转！" + tickCounter + " tick惊呆众人！ 🚀",
                    "💎 钻石级旋转！" + tickCounter + " tick完美演出！ 💎"
                };
                String message = coolMessages[tickCounter / 100 % coolMessages.length];
                info(message + " 朝向: " + String.format("%.1f°", currentYaw));
            } else {
                info("🔄 已旋转 " + tickCounter + " tick，当前朝向: " + String.format("%.1f°", currentYaw));
            }
        }
    }

    /**
     * 检测玩家是否在移动
     *
     * @return true 如果玩家正在移动
     */
    private boolean isPlayerMoving() {
        // 检查按键输入
        boolean keyPressed = Input.isPressed(mc.options.forwardKey) ||
            Input.isPressed(mc.options.backKey) ||
            Input.isPressed(mc.options.leftKey) ||
            Input.isPressed(mc.options.rightKey) ||
            Input.isPressed(mc.options.jumpKey) ||
            Input.isPressed(mc.options.sneakKey);

        // 检查位置变化
        BlockPos currentPos = mc.player.getBlockPos();
        boolean positionChanged = lastPlayerPos != null && !currentPos.equals(lastPlayerPos);

        return keyPressed || positionChanged;
    }

    /**
     * 执行旋转操作
     */
    private void performSpin() {
        // 计算新的偏航角
        float angleToAdd = rotationAngle.get().floatValue();
        currentYaw += angleToAdd;

        // 保持角度在 -180 到 180 范围内
        while (currentYaw > 180.0f) {
            currentYaw -= 360.0f;
        }
        while (currentYaw < -180.0f) {
            currentYaw += 360.0f;
        }

        if (!packetOnly.get()) {

            // 正常模式：设置客户端玩家朝向（你自己也会看到旋转效果）
            mc.player.setYaw(currentYaw);


            // 仅发包模式：只向服务器发送旋转包，客户端朝向不变
            // 其他玩家会看到你在旋转，但你自己的视角保持不变

        }

        // 同时发送到服务器确保其他玩家也能看到
        mc.player.networkHandler.sendPacket(
            VUtil.get(currentYaw,
                mc.player.getPitch(),
                mc.player.isOnGround())

        );

    }

    /**
     * 检查里程碑并显示特殊消息
     */
    private void checkMilestones() {
        if (!showStatus.get() || !coolEffects.get()) return;

        // 特殊里程碑
        String modeText = packetOnly.get() ? "（隐形模式）" : "（可见模式）";
        if (tickCounter == 360) {
            info("🎉 恭喜！完成一整圈旋转（360°）！你就是旋转之王！" + modeText + " 🎉");
        } else if (tickCounter == 720) {
            info("🏆 双圈达成！720°的完美表演！观众为你疯狂！" + modeText + " 🏆");
        } else if (tickCounter == 1000) {
            info("💯 千tick里程碑！你的旋转技巧已经炉火纯青！" + modeText + " 💯");
        } else if (tickCounter == 1800) {
            info("🌟 五圈成就解锁！1800°的华丽演出！" + modeText + " 🌟");
        } else if (tickCounter % 1000 == 0 && tickCounter > 1000) {
            info("🚀 " + (tickCounter / 1000) + "K里程碑达成！你就是永动机靓仔！" + modeText + " 🚀");
        }
    }

    /**
     * 获取当前旋转状态信息
     *
     * @return 状态信息字符串
     */
    public String getStatusInfo() {
        if (!isActive()) {
            return "靓仔转圈: 未启用";
        }

        String status = isSpinning ? "旋转中" : "暂停中";
        String mode = packetOnly.get() ? "隐形" : "可见";
        return String.format("靓仔转圈: %s (%s) | 角度: %.1f° | Tick: %d",
            status, mode, currentYaw, tickCounter);
    }
}
