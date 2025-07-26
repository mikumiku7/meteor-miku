package com.github.mikumiku.addon.modules;


import com.github.mikumiku.addon.MikuMikuAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.HashSet;
import java.util.Set;


/**
 * 玩家提醒模块
 * 当有玩家进入设定范围时播放声音并发送聊天提醒
 */
public class PlayerAlert extends MikuModule {


    /**
     * 声音选择枚举
     */
    enum SoundChoice {
        BELL(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value()),
        BELL_USE(SoundEvents.BLOCK_BELL_USE),
        DING(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value()),
        WARNING(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value()),
        ALERT(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP),
        DANGER(SoundEvents.ENTITY_ENDERMAN_TELEPORT);

        public final SoundEvent soundEvent;

        SoundChoice(SoundEvent soundEvent) {
            this.soundEvent = soundEvent;
        }
    }

    // 用于跟踪已知玩家，避免重复提醒
    private final Set<String> knownPlayers = new HashSet<>();

    // 模块设置
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSound = settings.createGroup("声音设置");
    private final SettingGroup sgChat = settings.createGroup("聊天设置");

    // 通用设置
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("检测距离")
        .description("检测玩家的最大距离")
        .defaultValue(50.0)
        .min(5.0)
        .max(200.0)
        .sliderMin(5.0)
        .sliderMax(100.0)
        .build()
    );

    private final Setting<Integer> checkInterval = sgGeneral.add(new IntSetting.Builder()
        .name("检测间隔")
        .description("检测玩家的间隔时间(tick)")
        .defaultValue(20)
        .min(1)
        .max(100)
        .sliderMin(1)
        .sliderMax(60)
        .build()
    );

    private final Setting<Boolean> ignoreInvisible = sgGeneral.add(new BoolSetting.Builder()
        .name("跳过隐身玩家")
        .description("是否忽略隐身状态的玩家")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> onlyHostile = sgGeneral.add(new BoolSetting.Builder()
        .name("仅敌对玩家")
        .description("只提醒非队友玩家(需要其他模组支持)")
        .defaultValue(false)
        .build()
    );

    // 声音设置
    private final Setting<Boolean> playSound = sgSound.add(new BoolSetting.Builder()
        .name("播放声音")
        .description("检测到玩家时播放声音")
        .defaultValue(true)
        .build()
    );

    private final Setting<SoundChoice> soundType = sgSound.add(new EnumSetting.Builder<SoundChoice>()
        .name("声音类型")
        .description("播放的声音类型")
        .defaultValue(SoundChoice.BELL)
        .build()
    );

    private final Setting<Double> soundVolume = sgSound.add(new DoubleSetting.Builder()
        .name("音量")
        .description("声音播放音量")
        .defaultValue(1.0d)
        .min(0.1d)
        .max(2.0d)
        .sliderMin(0.1d)
        .sliderMax(2.0d)
        .build()
    );

    private final Setting<Double> soundPitch = sgSound.add(new DoubleSetting.Builder()
        .name("音调")
        .description("声音播放音调")
        .defaultValue(1.0d)
        .min(0.5d)
        .max(2.0d)
        .sliderMin(0.5d)
        .sliderMax(2.0d)
        .build()
    );


    // 聊天设置
    private final Setting<Boolean> chatAlert = sgChat.add(new BoolSetting.Builder()
        .name("聊天提醒")
        .description("在聊天框显示玩家提醒")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showDistance = sgChat.add(new BoolSetting.Builder()
        .name("显示距离")
        .description("在提醒中显示玩家距离")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showCoordinates = sgChat.add(new BoolSetting.Builder()
        .name("显示坐标")
        .description("在提醒中显示玩家坐标")
        .defaultValue(false)
        .build()
    );

    // 计数器用于控制检测频率
    private int tickCounter = 0;

    public PlayerAlert() {
        super(MikuMikuAddon.CATEGORY, "玩家提醒", "当有玩家进入附近时播放声音并提醒");
    }

    @Override
    public void onActivate() {
        knownPlayers.clear();
        tickCounter = 0;
        info("玩家提醒模块已启动，检测距离: %.1f 格", range.get());
    }

    @Override
    public void onDeactivate() {
        knownPlayers.clear();
        info("玩家提醒模块已关闭");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // 控制检测频率
        tickCounter++;
        if (tickCounter < checkInterval.get()) return;
        tickCounter = 0;

        checkForPlayers();
    }

    /**
     * 检测附近的玩家
     */
    private void checkForPlayers() {
        Set<String> currentPlayers = new HashSet<>();

        // 遍历世界中的所有玩家
        for (PlayerEntity player : mc.world.getPlayers()) {
            // 跳过自己
            if (player == mc.player) continue;

            String playerName = player.getGameProfile().getName();
            double distance = mc.player.distanceTo(player);

            // 检查距离
            if (distance > range.get()) continue;

            // 检查是否隐身
            if (ignoreInvisible.get() && player.isInvisible()) continue;

            // 检查是否为敌对玩家(这里只是示例，实际需要根据具体需求实现)
            if (onlyHostile.get() && isFriendly(player)) continue;

            currentPlayers.add(playerName);

            // 如果是新发现的玩家，触发提醒
            if (!knownPlayers.contains(playerName)) {
                alertPlayer(player, distance);
            }
        }

        // 更新已知玩家列表
        knownPlayers.clear();
        knownPlayers.addAll(currentPlayers);
    }

    /**
     * 触发玩家提醒
     */
    private void alertPlayer(PlayerEntity player, double distance) {
        String playerName = player.getGameProfile().getName();

        // 播放声音
        if (playSound.get()) {
            playAlertSound();
        }

        // 聊天框提醒
        if (chatAlert.get()) {
            sendChatAlert(player, distance);
        }

        // 控制台日志
        info("检测到玩家: %s (距离: %.1f)", playerName, distance);
    }

    /**
     * 播放提醒声音
     */
    private void playAlertSound() {
        if (mc.getSoundManager() != null) {
            mc.getSoundManager().play(PositionedSoundInstance.master(
                soundType.get().soundEvent,
                soundPitch.get().floatValue(),
                soundVolume.get().floatValue()
            ));
        }
    }

    /**
     * 发送聊天提醒
     */
    private void sendChatAlert(PlayerEntity player, double distance) {
        String playerName = player.getGameProfile().getName();
        StringBuilder message = new StringBuilder();

        message.append("§c[玩家提醒] §f检测到玩家: §e").append(playerName);

        if (showDistance.get()) {
            message.append(" §7(距离: §a").append(String.format("%.1f", distance)).append("§7)");
        }

        if (showCoordinates.get()) {
            message.append(" §7[坐标: §b")
                .append(String.format("%.0f, %.0f, %.0f",
                    player.getX(), player.getY(), player.getZ()))
                .append("§7]");
        }

        // 发送本地聊天消息
        ChatUtils.info(message.toString());
    }

    /**
     * 判断玩家是否为友好玩家
     * 这里只是示例实现，实际应用中需要根据具体需求实现
     */
    private boolean isFriendly(PlayerEntity player) {
        // 示例：可以基于队伍、公会、朋友列表等判断
        // 目前简单地将所有玩家都视为敌对玩家（除非在同一个队伍中）
        // 如果玩家在同一队伍中，则认为是友好的
        if (mc.player != null && mc.player.isTeammate(player)) {
            return true;
        }
        return false;
    }

    /**
     * 获取格式化的距离字符串
     */
    private String getDistanceString(double distance) {
        if (distance < 10) {
            return String.format("%.1f", distance);
        } else {
            return String.format("%.0f", distance);
        }
    }

    /**
     * 手动刷新已知玩家列表
     */
    public void refreshPlayerList() {
        knownPlayers.clear();
        info("已刷新玩家列表");
    }
}
