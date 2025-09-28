package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Random;

/**
 * @author KassuK
 */

public class AutoSM extends BaseModule {

    public AutoSM() {
        super("自动涩涩", "自动对最近的人说色色的话");
    }


    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // 模式选择：攻方台词 / 受方台词 / 混合随机
    private final Setting<MoanMode> moanmode = sgGeneral.add(new EnumSetting.Builder<MoanMode>()
        .name("消息模式")
        .description("选择发送哪种类型的台词。模式选择：攻方台词 / 受方台词 / 混合随机")
        .defaultValue(MoanMode.S) // 默认受方
        .build()
    );

    // 忽略好友：不开车给好友
    private final Setting<Boolean> iFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("忽略好友")
        .description("不会对好友说这些话。")
        .defaultValue(true)
        .build()
    );

    // 延迟：两次呻吟的间隔（tick）
    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
        .name("延迟")
        .description("两次呻吟的间隔 tick。")
        .defaultValue(100) // 大约 25 秒
        .min(1)
        .sliderRange(1, 1000)
        .build()
    );


    public enum MoanMode {
        M,
        S,
    }

    private int lastNum;
    private double timer = 0;
    private static final String[] Submissive = new String[]{
        "爹再狠一点干我",
        "再深点！爹再深一点！",
        "好爽！爹的太大了！",
        "我爱你的大屌 %s！",
        "爹在我射出来前别停！",
        "你为了我硬到不行",
        "想把我菊花撑大吗 %s？",
        "我爱你爹",
        "操到我菊花开花",
        "%s 超爱我的菊花",
        "我用紧致的菊花让 %s 爽到不行",
        "爹的大屌又粗又多汁！",
        "求你用尽全力操我",
        "我是 %s 的专属娘受精壶！",
        "求你把滚烫的汁射进我最深处爹！",
        "我爱 %s 在我体内的感觉！",
        "%s 一看到我屁股就硬到不行！",
        "%s 特别爱狠狠干我的菊花！",
        "你就不能把最后一句说出来吗",
    };

    private static final String[] Dominant = new String[]{
        // 我这是闲的没事干吗
        "乖乖听话，让爹爽",
        "老子最爱干你屁眼 %s！",
        "把你菊花献给爹！",
        "最爱你被我操到流前水的样子 %s",
        "像个乖狗一样给爹舔到底",
        "过来骑上爹的家伙 %s",
        "最爱你含着我看着我的样子 %s",
        "%s 被我干的时候可爱到爆",
        "%s 的菊花紧到爆！",
        "%s 挨操的时候真像个乖狗",
        "最爱你骑在我上面扭屁股",
        "%s 挨干的时候娇喘得贼奶",
        "%s 是天下第一精壶！",
        "%s 永远饥渴着等爹的家伙",
        "每次看到 %s 我都硬得像石头",
        "你就不能把最后一句说出来吗",
    };

    private final Random r = new Random();


    @EventHandler
    private void onRender(Render3DEvent event) {
        timer = Math.min(delay.get(), timer + event.frameTime);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        timer++;
        if (mc.player != null && mc.world != null && timer >= delay.get()) {
            MOAN();
            timer = 0;
        }
    }

    private void MOAN() {
        PlayerEntity target = getClosest();
        if (target == null) {
            return;
        }

        String name = target.getName().getString();
        switch (moanmode.get()) {
            case S -> {
                int num = r.nextInt(0, Submissive.length - 1);
                if (num == lastNum) {
                    num = num < Submissive.length - 1 ? num + 1 : 0;
                }
                lastNum = num;
                ChatUtils.sendPlayerMsg(Submissive[num].replace("%s", name));
            }
            case M -> {
                int num = r.nextInt(0, Dominant.length - 1);
                if (num == lastNum) {
                    num = num < Dominant.length - 1 ? num + 1 : 0;
                }
                lastNum = num;
                ChatUtils.sendPlayerMsg(Dominant[num].replace("%s", name));
            }
        }
    }

    private PlayerEntity getClosest() {
        assert mc.player != null && mc.world != null;
        PlayerEntity closest = null;
        float distance = -1;
        if (!mc.world.getPlayers().isEmpty()) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player != mc.player && (!iFriends.get() || !Friends.get().isFriend(player))) {
                    if (closest == null || mc.player.getPos().distanceTo(player.getPos()) < distance) {
                        closest = player;
                        distance = (float) mc.player.getPos().distanceTo(player.getPos());
                    }
                }
            }
        }
        return closest;
    }
}
