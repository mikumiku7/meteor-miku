package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.BaritoneUtil;
import com.github.mikumiku.addon.util.ChatUtils;
import com.github.mikumiku.addon.util.Via;
import com.github.mikumiku.addon.util.timer.SyncedTickTimer;
import com.github.mikumiku.addon.util.timer.Timers;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
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
 * @since 1.0.0
 */
public class HandsomeSpin extends BaseModule {

    private final Identifier TEXTURE = Identifier.of("meteor-miku", "textures/miku1.png");
    private final Identifier TEXTURE2 = Identifier.of("meteor-miku", "textures/miku2.png");


    // 设置组
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // 旋转角度设置
    private final Setting<Double> rotationAngle = sgGeneral.add(new DoubleSetting.Builder()
        .name("旋转角度")
        .description("每tick旋转的角度（度）")
        .defaultValue(35.0)
        .min(-360.0)
        .max(360.0)
        .sliderMin(-180.0)
        .sliderMax(180.0)
        .build());

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("延迟")
        .description("动作之间的延迟时间（单位：tick）")
        .defaultValue(0)
        .sliderRange(0, 20)
        .build()
    );

    // 是否启用移动检测
    private final Setting<Boolean> pauseOnMovement = sgGeneral.add(new BoolSetting.Builder()
        .name("移动时暂停")
        .description("当玩家移动时暂停旋转")
        .defaultValue(true)
        .build());

    // 是否仅发包模式
    private final Setting<Boolean> packetOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("仅发包")
        .description("只向服务器发送旋转包，客户端玩家朝向不变")
        .defaultValue(false)
        .build());

    // 是否启用炫酷特效
    private final Setting<Boolean> handSwing = sgGeneral.add(new BoolSetting.Builder()
        .name("挥手")
        .description("启用高速挥手")
        .defaultValue(true)
        .build());

    // 是否启用高速蹲起
    private final Setting<Boolean> fastSneak = sgGeneral.add(new BoolSetting.Builder()
        .name("高速蹲起")
        .description("启用高速蹲起动作，增加视觉效果")
        .defaultValue(false)
        .build());

    // 是否显示状态信息
    private final Setting<Boolean> showStatus = sgGeneral.add(new BoolSetting.Builder()
        .name("显示状态")
        .description("在聊天中显示旋转状态信息")
        .defaultValue(true)
        .build());

    // 内部状态变量
    private float currentYaw = 0.0f;
    private BlockPos lastPlayerPos = null;
    private boolean isSpinning = false;
    private int tickCounter = 0;
    private boolean isSneaking = false;
    private SyncedTickTimer tickTimer = Timers.tickTimer();

    public HandsomeSpin() {
        super("靓仔转圈", "惊呆所有人的炫酷旋转效果！");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        // 初始化状态
        currentYaw = mc.player.getYaw();
        lastPlayerPos = mc.player.getBlockPos();
        isSpinning = true;
        isSneaking = mc.player.isSneaking();
        tickCounter = 0;

    }

    @Override
    public void onDeactivate() {
        if (showStatus.get()) {
            ChatUtils.sendMsg("🎭 靓仔转圈结束，感谢观赏！");
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
                    ChatUtils.sendMsg("⏸️ 检测到移动，暂停旋转");
                }
            }
            // 更新最后位置
            lastPlayerPos = mc.player.getBlockPos();
            return;
        } else {
            if (!isSpinning) {
                isSpinning = true;
                if (showStatus.get()) {
                    ChatUtils.sendMsg("▶️ 恢复旋转，继续惊呆众人！");
                }
            }
        }

        // 检查是否应该执行动作（基于delay设置）
        if (tickTimer.tick(delay.get())) {
            // 执行旋转
            performSpin();

            // 执行挥手动画
            if (handSwing.get()) {
                performHandSwing();
            }

            // 执行蹲起动作
            if (fastSneak.get()) {
                performFastSneak();
            }

            tickCounter++;

        }
    }

    /**
     * 检测玩家是否在移动
     *
     * @return true 如果玩家正在移动
     */
    public boolean isPlayerMoving() {
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
            Via.get(currentYaw,
                mc.player.getPitch(),
                mc.player.isOnGround())

        );

    }

    /**
     * 执行挥手动画
     */
    private void performHandSwing() {
        if (mc.player == null) return;

        // 根据旋转角度确定挥手方向
        float angleToAdd = rotationAngle.get().floatValue();
        Hand handToSwing;

        // 根据旋转方向选择挥手的手
        if (angleToAdd > 0) {
            // 正向旋转（顺时针）使用主手挥手
            handToSwing = Hand.MAIN_HAND;
        } else {
            // 反向旋转（逆时针）使用副手挥手
            handToSwing = Hand.OFF_HAND;
        }

        // 执行挥手动画
        // 发送挥手包到服务器确保其他玩家也能看到
        BaritoneUtil.swingHand(handToSwing, BaritoneUtil.SwingSide.All);
    }

    /**
     * 执行高速蹲起动作
     */
    private void performFastSneak() {
        if (mc.player == null) return;

        // 每个tick切换蹲起状态
        isSneaking = !isSneaking;

        // 设置玩家蹲起状态
        mc.player.setSneaking(isSneaking);

    }


    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!Utils.isLoading() && isActive()) {
            DrawContext drawContext = event.drawContext;
//            Via.drawTexture(drawContext, TEXTURE2, 0, 0, 100, 100);
        }
    }
}
