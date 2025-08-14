package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * @author OLEPOSSU miku
 */

public class AutoEz extends BaseModule {
    public AutoEz() {
        super("自动EZ", "敌人死亡后发送EZ嘲讽消息。");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgKill = settings.createGroup("击杀");
    private final SettingGroup sgPop = settings.createGroup("图腾");

    //--------------------General--------------------//
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("敌人范围")
        .description("只有敌人在此范围内死亡时才发送消息。")
        .defaultValue(25)
        .min(0)
        .sliderRange(0, 50)
        .build()
    );
    private final Setting<Integer> tickDelay = sgGeneral.add(new IntSetting.Builder()
        .name("延迟")
        .description("发送消息之间等待的tick数量。")
        .defaultValue(50)
        .min(0)
        .sliderRange(0, 100)
        .build()
    );

    //--------------------Kill--------------------//
    private final Setting<Boolean> kill = sgKill.add(new BoolSetting.Builder()
        .name("击杀消息")
        .description("敌人死亡时是否发送消息")
        .defaultValue(true)
        .build()
    );
    private final Setting<MessageMode> killMsgMode = sgKill.add(new EnumSetting.Builder<MessageMode>()
        .name("击杀消息模式")
        .description("发送什么类型的消息。")
        .defaultValue(MessageMode.Miku)
        .build()
    );
    private final Setting<List<String>> killMessages = sgKill.add(new StringListSetting.Builder()
        .name("击杀消息")
        .description("启用Miku消息模式时击杀敌人要发送的消息")
        .defaultValue(List.of("被Miku干掉了！", "Miku最强", "Miku牛逼"))
        .visible(() -> killMsgMode.get() == MessageMode.Miku)
        .build()
    );

    //--------------------Pop--------------------//
    private final Setting<Boolean> pop = sgPop.add(new BoolSetting.Builder()
        .name("图腾消息")
        .description("敌人爆图腾时是否发送消息")
        .defaultValue(true)
        .build()
    );
    private final Setting<List<String>> popMessages = sgPop.add(new StringListSetting.Builder()
        .name("图腾消息")
        .description("敌人爆图腾时要发送的消息")
        .defaultValue(List.of("我就喜欢看你用图腾 <NAME>", "真是悦耳的音乐 <NAME>", "噼里啪啦停不下来直到你倒下 <NAME>"))
        .build()
    );

    private final Random r = new Random();
    private int lastNum;
    private int lastPop;
    private boolean lastState;
    private String name = null;
    private final List<Message> messageQueue = new LinkedList<>();
    private int timer = 0;

    // credits to exhibition for these messages
    private final String[] more = new String[]{
        "哇，%s这都能死，你是真有点东西啊",
        "%s在这里寄了，笑死",
        "%s用安卓机也能寄，乐死我了",
        "%s，你妈绝对是反向慈母型的",
        "#胜利大逃杀#结束了，下次好运，%s！",
        "我奶奶打MC都比你强，%s",
        "%s，劝你买个烟雾吧，不然真没眼看",
        "哎呀，对不起啊%s（假装同情）",
        "%s，你的皮肤和你是个休闲废物哪个更烂？",
        "算了，%s，盲人玩家也该有机会，我支持你",
        "%s，你这波操作真是离谱",
        "%s，被我踩得服服帖帖感觉如何",
        "%s，你是真的喜欢死吗？",
        "要是只能选你和蔡徐坤，我选坤",
        "%s，你的智商和击杀数有共同点：都低得离谱",
        "%s，要不要听点PVP建议？",
        "哇，你在乐高游戏里寄了",
        "%s，没想到你居然能点下‘安装’按钮",
        "%s，我说中文你能别叽里呱啦吗？",
        "%s，拿好你的大写L",
        "%s被玩成了表情包",
        "%s是个新手皮！！！1！1！",
        "%s，在这破这里都能寄也是绝活",
        "%s喜欢二次元",
        "%s，菜狗，你连我都蹭不到几下",
        "%s，被我打到退服了",
        "%s，直接被爆干了",
        "%s连4格距离都打不过",
        "有人给这小孩递个纸，%s要哭了！",
        "%s你爹是光头",
        "%s，你家谱是不是仙人掌，全家带刺的",
        "%s，你菜得垃圾车都把你收走了",
        "%s，有的人是出生时被摔了，你是直接扔墙上了",
        "%s，赶紧回娘胎里回炉重造吧",
        "谢谢送的人头，%s！",
        "%s，你是装的还是菜的",
        "%s，你是真废",
        "%s，我妈打这游戏都比你强",
        "%s，哈哈哈哈急了？",
        "%s，加我好友，我们聊聊你到底有多废",
        "%s：“管理员！有人打我！我被打烂了！”",
        "%s，你就这点跳跳跳的水平还想打到我？",
        "%s，烟雾是个好东西你真的该试试！",
        "%s，我没开长臂，你只是点太慢了",
        "%s，希望你录像回放自己有多菜",
        "%s，这游戏左右键都要用的，别忘了",
        "%s，你的延迟和智商是一比一的，傻X",
        "%s，ALT+F4一下问题就没了",
        "%s，ALT+F4打开隐藏福利",
        "%s，反正你迟早回去玩堡垒之夜，不如现在",
        "%s，回堡垒之夜去吧，五岁逆天小学生",
        "%s，我要对你尸体跳个橙色正义舞",
        "%s，我是真·玩家，而你只是背景板",
        "%s，尝尝你自己的药，开挂废物",
        "%s，去淹死在你自己的眼泪里吧",
        "%s，你这么菜，我看你我都掉智商",
        "%s，从学校楼上带绳子下去吧",
        "%s，被我支配了，小猴子",
        "%s，加我好友来喷我吧，我就爱听",
        "%s，我日了你爹",
        "%s，来啊，气得退服，咱俩都乐",
        "%s，不，你没瞎，我真把你打爆了",
        "%s，轻轻松松满血打穿你",
        "%s，我都能隔着屏幕听到你惨叫",
        "%s，看这句话的实锤弯了",
        "%s，你最近拉屎了吗？我刚把你屎都打出来了",
        "%s，6格女人打手",
        "%s，女权爆破机",
        "%s，你染色体数比这游戏地图还大",
        "几百万年进化出来个%s",
        "人体70%是水，%s怎么100%是盐？",
        "%s，L",
        "%s，被我打烂了",
        "%s，你太胖了，家里失火你用微波炉打了119",
        "%s，我也是天天被踢，长点脑子吧",
        "LMAO %s用裂开客户端",
        "%s，基佬",
        "%s，求你闭嘴",
        "%s，我和你妈玩堡垒之夜双排",
        "%s装得很硬，但你爹打你更狠",
        "哈哈%s，建议退服自闭",
        "你这准星是怎么点下载按钮的？%s",
        "%s，你的瞄准就像癌症，但癌症至少能杀人",
        "%s，你的用处和轮椅脚踏板一样",
        "%s，瞄准是帕金森赞助的吗",
        "%s，我让你卸载，你可能连卸载按钮都点不中",
        "%s，我赌你网恋过",
        "%s，请别死行吗，爷谢谢你",
        "%s，ez",
        "%s，你是不是舔门把手长大的",
        "%s，建议别呼吸了，智障",
        "%s，舔门把手去吧",
        "想提升PVP水平吗%s？去跳海吧",
        "L %s",
        "%s，没有好用的客户端",
        "%s的客户端崩了",
        "%s，别开挂了，智障",
        "%s :potato:",
        "%s，去打袋鼠吧，澳洲高延迟怪",
        "%s，超级马里奥死亡音效",
        "大家快/friend add %s，喷他菜",
        "%s，学学法国1940投降吧",
        "嘿%s，想听个笑话吗？你赢不了的",
        "%s，被OOF了",
        "%s，你爸妈都是你没见过的人",
        "%s，来喷我吧，我爱听",
        "oof %s",
        "%s，FBI开门，你搜裂开烟雾被发现了",
        "%s，从窗户跳下去送个免费会员",
        "%s，你连一点机会都没有",
        "%s，继续努力啊",
        "%s，你是那种1v1也能拿第三的人",
        "%s，不是我说，你的命比我手机电还不值钱",
        "原来送人头是你的天赋，%s",
        "%s，霍金的手眼协调都比你好",
        "%s，你这就是避孕套发明的意义",
        "%s，你就是憨批代名词",
        "%s，哈哈，GG",
        "%s，烂客户端",
        "%s，gg ez，小菜鸡",
        "别忘了举报我，%s",
        "%s，你的智商就是个zz",
        "%s，你最近拉屎了吗？我刚把你屎都打出来了",
        "%s，下次别在我甜甜圈里放豆子",
        "%s，2+2=4，减1是你的IQ",
        "你该用烟雾了，%s！",
        "%s，一击必杀，LUL",
        "%s，你是避孕套广告代言人",
        "%s，我不懂为啥避孕套不叫你名字",
        "%s，我瞎了的爷爷瞄得都比你好",
        "%s，Exhibob都比你强",
        "%s，你真是EZ到爆",
        "%s，NMSL",
        "%s，你父母不要你，孤儿院也不要你",
        "%s，你比拿破仑还胖",
        "%s，考虑一下别活了",
        "%s，现实杀了人还说是意外的那种人",
        "%s，你现实里也是秒掉的货色",
        "%s，智商测验得了个F",
        "别忘了举报我，%s",
        "%s，你妈基",
        "%s，我刚往你脑门打了个喷嚏",
        "%s，你牙齿像星星——金黄而且分散",
        "%s，玫瑰是蓝的，星星是红的，你被开挂干死了",
        "%s，我不开挂，因为watchdog肯定会封我",
        "%s，少吸点油漆吧兄弟",
        "%s，你是被全服最强客户端干死的，现在还带无限疾跑绕过",
        "%s，你胖得肚脐比你先回家20分钟",
        "%s，你那玩意小得用麦圈都能套上"
    };

    private final String[] noway = new String[]{
        "是啊，这帮人说要封我，懂个屁",
        "只是想在终点刷个分",
        "没问你意见，你算哪根葱？",
        "手摸着刀，分分钟让你变意面",
        "这帮人想学我，其实只是嫉妒",
        "和兄弟们嗨着，在矿车里抽着烟",
        "不得不甩了她，她还懵着呢",
        "上来就找死，活该被秒",
        "你们这帮人都是废物",
        "你妹子跑我家来了，我可真会玩",
        "带着劳力士两色表拧断你脖子",
        "带着个大长腿的妹子晃悠",
    };


    @Override
    public void onActivate() {
        super.onActivate();
        lastState = false;
        lastNum = -1;
    }

    @Override
    public String getInfoString() {
        return killMsgMode.get().name();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(TickEvent.Pre event) {
        timer++;
        if (mc.player != null && mc.world != null) {
            if (anyDead(range.get()) && kill.get()) {
                if (!lastState) {
                    lastState = true;
                    sendKillMessage();
                }
            } else lastState = false;

            if (timer >= tickDelay.get() && !messageQueue.isEmpty()) {
                Message msg = messageQueue.get(0);
                ChatUtils.sendPlayerMsg(msg.message);
                timer = 0;

                if (msg.kill) messageQueue.clear();
                else messageQueue.remove(0);
            }
        }
    }

    @EventHandler
    private void onReceive(PacketEvent.Receive event) {
        if (event.packet instanceof EntityStatusS2CPacket packet) {
            // Pop
            if (packet.getStatus() == 35) {
                Entity entity = packet.getEntity(mc.world);
                if (pop.get() && mc.player != null && mc.world != null && entity instanceof PlayerEntity) {
                    if (entity != mc.player && !Friends.get().isFriend((PlayerEntity) entity) &&
                        mc.player.getPos().distanceTo(entity.getPos()) <= range.get()) {
                        sendPopMessage(entity.getName().getString());
                    }
                }
            }
        }
    }

    @SuppressWarnings("DataFlowIssue")
    private boolean anyDead(double range) {
        for (PlayerEntity pl : mc.world.getPlayers()) {
            if (pl != mc.player && !Friends.get().isFriend(pl) && pl.getPos().distanceTo(mc.player.getPos()) <= range
                && pl.getHealth() <= 0) {
                name = pl.getName().getString();
                return true;
            }
        }
        return false;
    }

    private void sendKillMessage() {
        switch (killMsgMode.get()) {
            case 智能 -> {
                int num = r.nextInt(0, more.length);
                if (num == lastNum) {
                    num = num < more.length - 1 ? num + 1 : 0;
                }
                lastNum = num;
                messageQueue.add(0, new Message(more[num].replace("%s", name == null ? "你" : name), true));
            }

            case Miku -> {
                if (!killMessages.get().isEmpty()) {
                    int num = r.nextInt(0, killMessages.get().size());
                    if (num == lastNum) num = num < killMessages.get().size() - 1 ? num + 1 : 0;

                    lastNum = num;
                    messageQueue.add(0, new Message(killMessages.get().get(num), true));
                }
            }
            case 简单 -> {
                int num = r.nextInt(0, noway.length);
                if (num == lastNum) {
                    num = num < noway.length - 1 ? num + 1 : 0;
                }
                lastNum = num;
                messageQueue.add(0, new Message(noway[num].replace("%s", name == null ? "你" : name), true));
            }
        }
    }

    private void sendPopMessage(String name) {
        if (!popMessages.get().isEmpty()) {
            int num = r.nextInt(0, popMessages.get().size() - 1);
            if (num == lastPop) {
                num = num < popMessages.get().size() - 1 ? num + 1 : 0;
            }
            lastPop = num;
            messageQueue.add(new Message(popMessages.get().get(num).replace("<NAME>", name), false));
        }
    }

    private record Message(String message, boolean kill) {
    }

    public enum MessageMode {
        Miku,
        智能,
        简单,
    }
}
