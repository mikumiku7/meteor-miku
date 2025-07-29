package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.NopPathManager;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

public class AutoWalk extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    protected MinecraftClient mc;

    // 慢速行走的内部变量
    private int slowWalkTickCounter = 0;

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("模式")
        .description("行走模式。")
        .defaultValue(Mode.Smart)
        .onChanged(mode1 -> {
            if (isActive()) {
                if (mode1 == Mode.Simple) {
                    PathManagers.get().stop();
                } else {
                    createGoal();
                }

                unpress(true);
            }
        })
        .build()
    );

    private final Setting<Direction> direction = sgGeneral.add(new EnumSetting.Builder<Direction>()
        .name("简单模式方向")
        .description("简单模式下的行走方向。")
        .defaultValue(Direction.Forwards)
        .onChanged(direction1 -> {
            if (isActive()) unpress(false);
        })
        .visible(() -> mode.get() == Mode.Simple)
        .build()
    );

    private final Setting<Boolean> sprint = sgGeneral.add(new BoolSetting.Builder()
        .name("疾跑")
        .description("行走时疾跑。")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.Simple)
        .build()
    );

    private final Setting<Boolean> slowWalk = sgGeneral.add(new BoolSetting.Builder()
        .name("慢速行走")
        .description("启用慢速行走模式。")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.Simple)
        .build()
    );

    private final Setting<Integer> slowWalkSpeed = sgGeneral.add(new IntSetting.Builder()
        .name("慢速行走速度")
        .description("慢速行走速度（以游戏刻为单位，数值越高越慢）。20刻 = 1秒。")
        .defaultValue(5)
        .min(2)
        .max(40)
        .sliderMax(20)
        .visible(() -> mode.get() == Mode.Simple && slowWalk.get())
        .build()
    );

    public AutoWalk() {
        super("自动走路", "自动走路. 增加了一个慢速模式. 方便");
        mc = MinecraftClient.getInstance();
    }

    @Override
    public void onActivate() {
        slowWalkTickCounter = 0;
        if (mode.get() == Mode.Smart) createGoal();
    }

    @Override
    public void onDeactivate() {
        if (mode.get() == Mode.Simple) unpress(true);
        else PathManagers.get().stop();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Pre event) {
        if (mode.get() == Mode.Simple) {
            // 处理慢速行走时机
            if (slowWalk.get()) {
                slowWalkTickCounter++;

                // 每隔指定刻数才移动一次
                if (slowWalkTickCounter >= slowWalkSpeed.get()) {
                    slowWalkTickCounter = 0;
                    handleMovement();
                } else {
                    // 在其他刻数不移动，释放所有按键
                    unpress(false);
                }
            } else {
                // 正常速度移动
                handleMovement();
            }
        } else {
            if (PathManagers.get() instanceof NopPathManager) {
                info("智能模式需要 Baritone");
                toggle();
            }
        }
    }

    private void handleMovement() {
        // 处理移动方向
        switch (direction.get()) {
            case Forwards -> setPressed(mc.options.forwardKey, true);
            case Backwards -> setPressed(mc.options.backKey, true);
            case Left -> setPressed(mc.options.leftKey, true);
            case Right -> setPressed(mc.options.rightKey, true);
        }

        // 处理疾跑
        if (sprint.get()) {
            setPressed(mc.options.sprintKey, sprint.get());
        }
    }

    private void unpress(boolean sprintChange) {
        setPressed(mc.options.forwardKey, false);
        setPressed(mc.options.backKey, false);
        setPressed(mc.options.leftKey, false);
        setPressed(mc.options.rightKey, false);
        if (sprintChange) {
            setPressed(mc.options.sprintKey, false);
        }
    }

    private void setPressed(KeyBinding key, boolean pressed) {
        key.setPressed(pressed);
        Input.setKeyState(key, pressed);
    }

    private void createGoal() {
        PathManagers.get().moveInDirection(mc.player.getYaw());
    }

    public enum Mode {
        Simple("简单"),
        Smart("智能");

        private final String displayName;

        Mode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum Direction {
        Forwards("前进"),
        Backwards("后退"),
        Left("左"),
        Right("右");

        private final String displayName;

        Direction(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
