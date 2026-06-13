package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;

public class DeathAutoCommand extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> command = sgGeneral.add(new StringSetting.Builder()
        .name("指令内容")
        .description("死亡后自动发送的指令（不需要输入/）")
        .defaultValue("home")
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("延迟")
        .description("死亡后延迟多少tick再发送指令")
        .defaultValue(20)
        .min(0)
        .sliderMax(100)
        .build()
    );

    private final Setting<Boolean> afterRespawn = sgGeneral.add(new BoolSetting.Builder()
        .name("重生后")
        .description("重生后再发")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> chatMode = sgGeneral.add(new BoolSetting.Builder()
        .name("聊天模式")
        .description("以聊天消息方式发送而不是命令")
        .defaultValue(false)
        .build()
    );

    private final Setting<String> prefix = sgGeneral.add(new StringSetting.Builder()
        .name("命令前缀")
        .description("命令前缀（通常是 / 或其他）")
        .defaultValue("/")
        .visible(() -> !chatMode.get())
        .build()
    );

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
        .name("调试模式")
        .description("显示调试信息")
        .defaultValue(false)
        .build()
    );

    private int tickCounter = 0;
    private boolean shouldSend = false;

    public DeathAutoCommand() {
        super("死亡自动指令", "死亡后自动发送自定义聊天指令");
    }

    @Override
    public void onActivate() {
        tickCounter = 0;
        shouldSend = false;
    }


    @EventHandler(priority = EventPriority.HIGH)
    private void onOpenScreenEvent(OpenScreenEvent event) {
        if (!(event.screen instanceof DeathScreen)) return;
        if (afterRespawn.get()) return;

        info("检测到死亡");
        triggerCommand();

    }


    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null) return;
        if (!afterRespawn.get()) return;
        // 方法1: 监听 CombatDeathS2CPacket (玩家死亡数据包)
        if (event.packet instanceof PlayerRespawnS2CPacket packet) {
            if (debug.get()) {
                info("检测到重生");
            }
            triggerCommand();
        }
    }


    private void triggerCommand() {
        shouldSend = true;
        tickCounter = 0;
        if (debug.get()) {
            info("死亡触发，将在 " + delay.get() + " tick 后发送指令");
        }
    }

    @Override
    public void onDeactivate() {
        shouldSend = false;
        tickCounter = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!shouldSend) return;
        if (mc.player == null) return;

        tickCounter++;

        // 达到延迟时间后发送指令
        if (tickCounter >= delay.get()) {
            sendCommand();
            shouldSend = false;
            tickCounter = 0;
        }
    }

    private void sendCommand() {
        String cmd = command.get().trim();
        if (cmd.isEmpty()) {
            warning("指令内容为空，未发送");
            return;
        }

        try {
            if (chatMode.get()) {
                // 聊天模式：直接发送消息
                mc.player.networkHandler.sendChatMessage(cmd);
                info("已发送聊天消息: " + cmd);
            } else {
                // 命令模式：发送命令
                String fullCommand = prefix.get() + cmd;
                // 使用 sendChatCommand 会自动处理命令
                mc.player.networkHandler.sendChatCommand(cmd);
                info("已发送指令: " + fullCommand);
            }
        } catch (Exception e) {
            error("发送指令失败: " + e.getMessage());
        }
    }
}
