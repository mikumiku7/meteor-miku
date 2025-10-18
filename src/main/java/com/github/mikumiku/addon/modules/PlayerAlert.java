package com.github.mikumiku.addon.modules;


import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.dynamic.DV;
import com.github.mikumiku.addon.util.PlayerUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
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
public class PlayerAlert extends BaseModule {


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

    // 用于跟踪视距内的玩家
    private final Set<String> playersInRenderDistance = new HashSet<>();

    // 用于跟踪靠近检测距离内的玩家
    private final Set<String> playersInCloseRange = new HashSet<>();

    // 模块设置
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSound = settings.createGroup("声音设置");
    private final SettingGroup sgChat = settings.createGroup("聊天设置");

    // 通用设置
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("靠近检测距离")
        .description("检测玩家的靠近")
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

    private final Setting<Boolean> onlyHostile = sgGeneral.add(new BoolSetting.Builder()
        .name("仅非队友玩家")
        .description("只提醒非队友玩家")
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
        .max(10.0d)
        .sliderMin(0.1d)
        .sliderMax(10.0d)
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
        super("玩家提醒", "当有玩家进入附近时播放声音并提醒");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        knownPlayers.clear();
        playersInRenderDistance.clear();
        playersInCloseRange.clear();
        tickCounter = 0;
        info("玩家提醒模块已启动，靠近检测距离: %.1f 格", range.get());
    }

    @Override
    public void onDeactivate() {
        knownPlayers.clear();
        playersInRenderDistance.clear();
        playersInCloseRange.clear();
        info("玩家提醒模块已关闭");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {

        if (mc == null || mc.player == null || mc.world == null) return;

        // 控制检测频率
        tickCounter++;
        if (tickCounter < checkInterval.get()) {
            return;
        }

        tickCounter = 0;

        checkForPlayers();
    }

    /**
     * 检测附近的玩家
     */
    private void checkForPlayers() {
        Set<String> currentRenderDistancePlayers = new HashSet<>();
        Set<String> currentCloseRangePlayers = new HashSet<>();

//        for (Entity entity : mc.world.getEntities()) {
//            if (entity instanceof PlayerEntity player && !player.getUuid().equals(mc.player.getUuid())) {
//
//
//                String playerName = player.getGameProfile().getName();
//                double distance = mc.player.distanceTo(player);
//                boolean within = PlayerUtils.isWithin(entity, 8);
//
//                boolean friend = Friends.get().isFriend(player);
//
//
//            }
//        }

        // 遍历世界中的所有玩家 (视距内的玩家)
        for (PlayerEntity player : mc.world.getPlayers()) {
            // 跳过自己
            if (player.getUuid().equals(mc.player.getUuid())) {
                continue;
            }

            String playerName = DV.of(PlayerUtil.class).getGameProfileName(player);
            double distance = mc.player.distanceTo(player);

            // 检查是否为敌对玩家
            if (onlyHostile.get() && isFriendly(player)) continue;

            // 所有视距内的玩家
            currentRenderDistancePlayers.add(playerName);

            // 检查是否进入视距 (新玩家)
            if (!playersInRenderDistance.contains(playerName)) {
                alertPlayerEnterRenderDistance(player, distance);
            }

            // 检查是否在靠近检测距离内
            if (distance <= range.get()) {
                currentCloseRangePlayers.add(playerName);

                // 检查是否进入靠近检测距离 (新玩家进入靠近范围)
                if (!playersInCloseRange.contains(playerName)) {
                    alertPlayerEnterCloseRange(player, distance);
                }
            }
        }

        // 检查离开视距的玩家
        for (String playerName : playersInRenderDistance) {
            if (!currentRenderDistancePlayers.contains(playerName)) {
                alertPlayerLeaveRenderDistance(playerName);
            }
        }

        // 更新玩家列表
        playersInRenderDistance.clear();
        playersInRenderDistance.addAll(currentRenderDistancePlayers);

        playersInCloseRange.clear();
        playersInCloseRange.addAll(currentCloseRangePlayers);

        // 保持向后兼容性
        knownPlayers.clear();
        knownPlayers.addAll(currentCloseRangePlayers);
    }

    /**
     * 玩家进入视距提醒
     */
    private void alertPlayerEnterRenderDistance(PlayerEntity player, double distance) {
//        String playerName = DV.of(PlayerUtil.class).getGameProfileName(player);

        // 播放声音
        if (playSound.get()) {
            playAlertSound();
        }

        // 聊天框提醒
        if (chatAlert.get()) {
            sendChatAlert(player, distance, "进入视距");
        }
    }

    /**
     * 玩家离开视距提醒
     */
    private void alertPlayerLeaveRenderDistance(String playerName) {
        // 播放声音
        if (playSound.get()) {
            playAlertSound();
        }

        // 聊天框提醒
        if (chatAlert.get()) {
            sendChatAlertLeave(playerName, "离开视距");
        }
    }

    /**
     * 玩家进入靠近检测距离提醒
     */
    private void alertPlayerEnterCloseRange(PlayerEntity player, double distance) {
//        String playerName = player.getGameProfile().getName();

        // 播放声音
        if (playSound.get()) {
            playAlertSound();
        }

        // 聊天框提醒
        if (chatAlert.get()) {
            sendChatAlert(player, distance, "进入靠近范围");
        }
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
     * 发送聊天提醒 (进入)
     */
    private void sendChatAlert(PlayerEntity player, double distance, String alertType) {
        String playerName = DV.of(PlayerUtil.class).getGameProfileName(player);
        StringBuilder message = new StringBuilder();

        message.append("§c[玩家提醒] §f").append(alertType).append(": §e").append(playerName);

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
     * 发送聊天提醒 (离开)
     */
    private void sendChatAlertLeave(String playerName, String alertType) {
        StringBuilder message = new StringBuilder();
        message.append("§c[玩家提醒] §f").append(alertType).append(": §e").append(playerName);

        // 发送本地聊天消息
        ChatUtils.info(message.toString());
    }

    /**
     * 判断玩家是否为友好玩家
     */
    private boolean isFriendly(PlayerEntity player) {

        boolean friend = Friends.get().isFriend(player);

        return friend;
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
        playersInRenderDistance.clear();
        playersInCloseRange.clear();
        info("已刷新玩家列表");
    }
}
