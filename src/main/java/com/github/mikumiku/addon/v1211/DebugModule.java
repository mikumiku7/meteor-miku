package com.github.mikumiku.addon.v1211;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.BlockPosListSetting;
import com.github.mikumiku.addon.util.ItemListMapSetting;
import com.github.mikumiku.addon.util.StringMapSetting;
import com.github.mikumiku.addon.util.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.MyPotion;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;

import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.*;

public class DebugModule extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDisplay = settings.createGroup("显示设置");
    private final SettingGroup sgTracking = settings.createGroup("追踪设置");
    private final SettingGroup sgPerformance = settings.createGroup("性能监控");
    private final SettingGroup sgSettingsDemo = settings.createGroup("设置展示");

    public final Setting<Boolean> inv = sgGeneral.add(new BoolSetting.Builder()
        .name("打印库存")
        .description("打印库存")
        .defaultValue(false)
        .build()
    );

    private final Setting<Map<String, String>> map = sgDisplay.add(new StringMapSetting.Builder()
        .name("Map")
        .description("Map")
        .build()
    );
    private final Setting<Map<String, List<Item>>> itemListMap = sgDisplay.add(new ItemListMapSetting.Builder()
        .name("ItemListMapSetting")
        .description("ItemListMapSetting")
        .build()
    );
    private final Setting<List<BlockPos>> blist = sgDisplay.add(new BlockPosListSetting.Builder()
        .name("BlockPosListSetting")
        .description("BlockPosListSetting")
        .build()
    );

    // === 设置展示组的各种设置类型 ===

    // 基础类型设置
    private final Setting<Boolean> boolDemo = sgSettingsDemo.add(new BoolSetting.Builder()
        .name("BoolSetting")
        .description("布尔值设置示例")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> intDemo = sgSettingsDemo.add(new IntSetting.Builder()
        .name("IntSetting")
        .description("整数设置示例")
        .defaultValue(42)
        .min(0)
        .max(100)
        .sliderMin(0)
        .sliderMax(100)
        .build()
    );

    private final Setting<Double> doubleDemo = sgSettingsDemo.add(new DoubleSetting.Builder()
        .name("DoubleSetting")
        .description("双精度浮点数设置示例")
        .defaultValue(3.14)
        .min(0.0)
        .max(10.0)
        .sliderMin(0.0)
        .sliderMax(10.0)
        .build()
    );

    private final Setting<String> stringDemo = sgSettingsDemo.add(new StringSetting.Builder()
        .name("StringSetting")
        .description("字符串设置示例")
        .defaultValue("Hello, World!")
        .build()
    );

    // Minecraft 相关设置
    private final Setting<Block> blockDemo = sgSettingsDemo.add(new BlockSetting.Builder()
        .name("BlockSetting")
        .description("方块设置示例")
        .defaultValue(Blocks.DIAMOND_BLOCK)
        .build()
    );

    private final Setting<Item> itemDemo = sgSettingsDemo.add(new ItemSetting.Builder()
        .name("ItemSetting")
        .description("物品设置示例")
        .defaultValue(Items.DIAMOND_SWORD)
        .build()
    );

    private final Setting<List<Block>> blockListDemo = sgSettingsDemo.add(new BlockListSetting.Builder()
        .name("BlockListSetting")
        .description("方块列表设置示例")
        .defaultValue(List.of(Blocks.DIAMOND_BLOCK, Blocks.EMERALD_BLOCK, Blocks.GOLD_BLOCK))
        .build()
    );

    private final Setting<List<Item>> itemListDemo = sgSettingsDemo.add(new ItemListSetting.Builder()
        .name("ItemListSetting")
        .description("物品列表设置示例")
        .defaultValue(List.of(Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_SHOVEL))
        .build()
    );

    private final Setting<BlockPos> blockPosDemo = sgSettingsDemo.add(new BlockPosSetting.Builder()
        .name("BlockPosSetting")
        .description("方块位置设置示例")
        .defaultValue(new BlockPos(0, 64, 0))
        .build()
    );

    private final Setting<Vector3d> vector3dDemo = sgSettingsDemo.add(new Vector3dSetting.Builder()
            .name("Vector3dSetting")
            .description("三维向量设置示例")
//        .defaultValue(new Vec3d(1.0, 2.0, 3.0))
            .build()
    );

    // 枚举设置
    private final Setting<TestEnum> enumDemo = sgSettingsDemo.add(new EnumSetting.Builder<TestEnum>()
        .name("EnumSetting")
        .description("枚举设置示例")
        .defaultValue(TestEnum.OPTION_A)
        .build()
    );

    // 颜色设置
    private final Setting<SettingColor> colorDemo = sgSettingsDemo.add(new ColorSetting.Builder()
        .name("ColorSetting")
        .description("颜色设置示例")
        .build()
    );

    private final Setting<List<SettingColor>> colorListDemo = sgSettingsDemo.add(new ColorListSetting.Builder()
            .name("ColorListSetting")
            .description("颜色列表设置示例")
//        .defaultValue(List.of(0xFF0000, 0x00FF00, 0x0000FF)) // 红、绿、蓝
            .build()
    );

    // 实体和效果设置
    private final Setting<Set<EntityType<?>>> entityTypeListDemo = sgSettingsDemo.add(new EntityTypeListSetting.Builder()
            .name("EntityTypeListSetting")
            .description("实体类型列表设置示例")
//        .defaultValue(List.of(EntityType.CREEPER, EntityType.ZOMBIE, EntityType.SKELETON))
            .build()
    );

    private final Setting<List<StatusEffect>> statusEffectListDemo = sgSettingsDemo.add(new StatusEffectListSetting.Builder()
        .name("StatusEffectListSetting")
        .description("状态效果列表设置示例")
        .build()
    );

    // 粒子和声音设置
    private final Setting<List<ParticleType<?>>> particleTypeListDemo = sgSettingsDemo.add(new ParticleTypeListSetting.Builder()
        .name("ParticleTypeListSetting")
        .description("粒子类型列表设置示例")
        .defaultValue(List.of(ParticleTypes.FLAME, ParticleTypes.SMOKE, ParticleTypes.EXPLOSION))
        .build()
    );

    private final Setting<List<SoundEvent>> soundEventListDemo = sgSettingsDemo.add(new SoundEventListSetting.Builder()
        .name("SoundEventListSetting")
        .description("声音事件列表设置示例")
        .defaultValue(List.of(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundEvents.BLOCK_ANVIL_USE))
        .build()
    );

    // 字符串列表设置
    private final Setting<List<String>> stringListDemo = sgSettingsDemo.add(new StringListSetting.Builder()
        .name("StringListSetting")
        .description("字符串列表设置示例")
        .defaultValue(List.of("示例1", "示例2", "示例3"))
        .build()
    );

    // 附魔设置
    private final Setting<Set<RegistryKey<Enchantment>>> enchantmentListDemo = sgSettingsDemo.add(new EnchantmentListSetting.Builder()
        .name("EnchantmentListSetting")
        .description("附魔列表设置示例")
        .build()
    );

    // 键位绑定设置
    private final Setting<Keybind> keybindDemo = sgSettingsDemo.add(new KeybindSetting.Builder()
        .name("KeybindSetting")
        .description("键位绑定设置示例")
        .build()
    );

    // 模块列表设置（如果存在）
    // private final Setting<List<Module>> moduleListDemo = sgSettingsDemo.add(new ModuleListSetting.Builder()
    //     .name("ModuleListSetting")
    //     .description("模块列表设置示例")
    //     .defaultValue(List.of())
    //     .build()
    // );

    // 药水设置
    private final Setting<MyPotion> potionDemo = sgSettingsDemo.add(new PotionSetting.Builder()
        .name("PotionSetting")
        .description("药水设置示例")
        .defaultValue(MyPotion.RegenerationLong)
        .build()
    );

    // 存储方块列表设置（如果存在）
    private final Setting<List<BlockEntityType<?>>> storageBlockListDemo = sgSettingsDemo.add(new StorageBlockListSetting.Builder()
            .name("StorageBlockListSetting")
            .description("存储方块列表设置示例")
//         .defaultValue(List.of(Blocks.CHEST, Blocks.BARREL, Blocks.SHULKER_BOX))
            .build()
    );

    // 屏幕处理器列表设置（如果存在）
    // private final Setting<List<ScreenHandlerType<?>>> screenHandlerListDemo = sgSettingsDemo.add(new ScreenHandlerListSetting.Builder()
    //     .name("ScreenHandlerListSetting")
    //     .description("屏幕处理器列表设置示例")
    //     .defaultValue(List.of(ScreenHandlerType.GENERIC_9X1, ScreenHandlerType.GENERIC_9X3))
    //     .build()
    // );

    // 状态效果增幅图设置（如果存在）
    // private final Setting<Map<StatusEffect, Integer>> statusEffectAmplifierMapDemo = sgSettingsDemo.add(new StatusEffectAmplifierMapSetting.Builder()
    //     .name("StatusEffectAmplifierMapSetting")
    //     .description("状态效果增幅图设置示例")
    //     .defaultValue(Map.of(StatusEffects.STRENGTH, 1, StatusEffects.SPEED, 2))
    //     .build()
    // );

    // 数据包列表设置
    private final Setting<Set<Class<? extends Packet<?>>>> packetListDemo = sgSettingsDemo.add(new PacketListSetting.Builder()
        .name("PacketListSetting")
        .description("数据包列表设置示例")
        .defaultValue(Set.of(PlayerMoveC2SPacket.class))
        .build()
    );

    // 条件可见性设置示例
    private final Setting<Boolean> conditionalToggle = sgSettingsDemo.add(new BoolSetting.Builder()
        .name("ConditionalToggle")
        .description("条件开关")
        .defaultValue(false)
        .build()
    );

    private final Setting<String> conditionalString = sgSettingsDemo.add(new StringSetting.Builder()
        .name("ConditionalString")
        .description("只有当ConditionalToggle为true时才可见")
        .defaultValue("条件可见的字符串")
        .visible(() -> conditionalToggle.get())
        .build()
    );

    // 设置组示例（创建子组）
    private final SettingGroup sgAdvancedSettings = sgSettingsDemo;
    private final SettingGroup sgExperimentalSettings = sgSettingsDemo;

    // 高级设置组中的设置
    private final Setting<Boolean> advancedMode = sgAdvancedSettings.add(new BoolSetting.Builder()
        .name("高级模式")
        .description("启用高级功能")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> precisionSetting = sgAdvancedSettings.add(new DoubleSetting.Builder()
        .name("精度设置")
        .description("高级精度控制")
        .defaultValue(0.001)
        .min(0.0001)
        .max(1.0)
        .visible(() -> advancedMode.get())
        .sliderMin(0.0001)
        .sliderMax(0.1)
        .build()
    );

    // 实验性设置组中的设置
    private final Setting<Boolean> experimentalFeatures = sgExperimentalSettings.add(new BoolSetting.Builder()
        .name("实验性功能")
        .description("启用实验性功能（可能有风险）")
        .defaultValue(false)
        .build()
    );

    private final Setting<String> experimentalConfig = sgExperimentalSettings.add(new StringSetting.Builder()
        .name("实验配置")
        .description("实验性功能的配置")
        .defaultValue("{}")
        .visible(() -> experimentalFeatures.get())
        .build()
    );

    // 测试枚举
    private enum TestEnum {
        OPTION_A("选项A"),
        OPTION_B("选项B"),
        OPTION_C("选项C");

        private final String displayName;

        TestEnum(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final Setting<Boolean> trackPackets = sgTracking.add(new BoolSetting.Builder()
        .name("追踪数据包")
        .description("追踪网络数据包")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> showPacketDetails = sgTracking.add(new BoolSetting.Builder()
        .name("显示数据包详情")
        .description("显示数据包的详细字段信息")
        .defaultValue(true)
        .visible(() -> trackPackets.get())
        .build()
    );


    private final Setting<Set<Class<? extends Packet<?>>>> packets = sgTracking.add(new PacketListSetting.Builder()
        .name("数据包类型")
        .description("选择要追踪的数据包类型")
        .defaultValue(new LinkedHashSet<>())
        .visible(() -> trackPackets.get())
        .build()
    );

    private final Setting<Integer> maxPacketLength = sgTracking.add(new IntSetting.Builder()
        .name("最大显示长度")
        .description("数据包信息的最大显示字符数")
        .defaultValue(5000)
        .min(100)
        .max(20000)
        .sliderMin(100)
        .sliderMax(10000)
        .visible(() -> trackPackets.get() && showPacketDetails.get())
        .build()
    );


    // 内部状态
    private int updateTimer = 0;
    private final DecimalFormat decimalFormat = new DecimalFormat("#.##");
    private final List<Entity> trackedEntities = new ArrayList<>();
    private int packetCount = 0;
    private int incomingCount = 0;
    private int outgoingCount = 0;
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public DebugModule() {
        super(CATEGORY, "Debug", "多功能调试模块");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        updateTimer = 0;
        trackedEntities.clear();
        packetCount = 0;
        incomingCount = 0;
        outgoingCount = 0;

        ChatUtils.sendMsg("§a[调试模块] 数据包追踪已激活");

        if (inv.get()) {
            MikuUtil.printInv();
            inv.set(false);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        updateTimer++;

        // 每100 tick（5秒）显示一次统计信息
        if (updateTimer % 100 == 0 && trackPackets.get() && packetCount > 0) {
            ChatUtils.sendMsg(String.format("§b[数据包统计] 总计: %d | 接收: %d | 发送: %d",
                packetCount, incomingCount, outgoingCount));
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {

    }

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
//        if (trackNewEntities.get()) {
//            Entity entity = event.entity;
//            String entityInfo = String.format("新实体: %s [%.1f, %.1f, %.1f]",
//                entity.getType().getTranslationKey(),
//                entity.getX(), entity.getY(), entity.getZ());
//            sendMsg(entityInfo);
//        }
    }

    @EventHandler
    private void onEntityRemoved(EntityRemovedEvent event) {

    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (!trackPackets.get()) return;

        Packet<?> packet = event.packet;
        String packetName = getReadablePacketName(packet);
        // 过滤功能，检查是否在追踪列表中
        if (!packets.get().contains(packet.getClass())) {
            return;
        }

        outgoingCount++;
        packetCount++;

        String direction = "§c发送->";
        String message = String.format("§e[数据包 %s] §f%s §7(#%d)", direction, packetName, packetCount);

        if (showPacketDetails.get()) {
            String details = getPacketDetails(packet);
            if (!details.isEmpty()) {
                String truncatedDetails = truncateString(details, maxPacketLength.get());
                message += "\n§7" + truncatedDetails.replace("\n", "\n§7");
            }
        }

        ChatUtils.sendMsgDebounce(message);
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!trackPackets.get()) return;

        Packet<?> packet = event.packet;
        String packetName = getReadablePacketName(packet);

        // 过滤一些常见但不重要的数据包
//        if (packetName.equals("EntityPositionS2CPacket") ||
//            packetName.equals("EntityS2CPacket") ||
//            packetName.equals("EntityVelocityUpdateS2CPacket")) {
//            return;
//        }

        //  过滤功能，检查是否在追踪列表中
        if (!packets.get().contains(packet.getClass())) {
            return;
        }

        incomingCount++;
        packetCount++;

        String direction = "§a接收<-";
        String message = String.format("§e[数据包 %s] §f%s §7(#%d)", direction, packetName, packetCount);

        if (showPacketDetails.get()) {
            String details = getPacketDetails(packet);
            if (!details.isEmpty()) {
                String truncatedDetails = truncateString(details, maxPacketLength.get());
                message += "\n§7" + truncatedDetails.replace("\n", "\n§7");
            }
        }

        ChatUtils.sendMsg(message);
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        // 可以用来监控聊天消息
//        if (trackPackets.get()) {
//            Text message = event.getMessage();
//            sendMsg("聊天: " + message.getString());
//        }
    }

    private void updateDebugInfo() {

    }

    /**
     * 获取数据包的详细信息
     */
    private String getPacketDetails(Packet<?> packet) {
        StringBuilder details = new StringBuilder();


        // 打印移动包
        if (packet instanceof PlayerMoveC2SPacket move) {
            double x = move.getX(mc.player.getX());
            double y = move.getY(mc.player.getY());
            double z = move.getZ(mc.player.getZ());
            float yaw = move.getYaw(mc.player.getYaw());
            float pitch = move.getPitch(mc.player.getPitch());
            boolean onGround = move.isOnGround();
            return (String.format("""
                    ===Move===
                    位置: (%.3f, %.3f, %.3f)
                    视角: yaw=%.1f pitch=%.1f
                    是否在地面: %s
                    """,
                x, y, z, yaw, pitch, onGround
            ));
        }

        // 打印方块交互包
        if (packet instanceof PlayerInteractBlockC2SPacket p) {
            Hand hand = p.getHand();
            BlockHitResult hit = p.getBlockHitResult();
            BlockPos pos = hit.getBlockPos();
            Direction face = hit.getSide();
            Vec3d hitPos = hit.getPos();

            return (String.format("""
                    ===InteractBlock===
                    手: %s
                    方块坐标: %s
                    面: %s
                    命中位置: (%.3f, %.3f, %.3f)
                    是否内部命中: %s
                    """,
                hand,
                pos.toShortString(),
                face,
                hitPos.x, hitPos.y, hitPos.z,
                hit.isInsideBlock()
            ));
        }
        // 玩家行为（破坏方块、开始破坏、停止使用物品等）
        if (packet instanceof PlayerActionC2SPacket p) {
            BlockPos pos = p.getPos();
            Direction dir = p.getDirection();
            PlayerActionC2SPacket.Action action = p.getAction();

            return String.format("""
                    ===PlayerAction===
                    动作: %s
                    目标方块: %s
                    朝向: %s
                    """,
                action,
                pos != null ? pos.toShortString() : "(无)",
                dir != null ? dir : "(无)"
            );
        }

        // 槽位状态变化（例如切换主手槽位、开关物品栏某状态）
        if (packet instanceof SlotChangedStateC2SPacket p) {
            int slot = p.slotId();
            boolean newState = p.newState();

            return String.format("""
                    ===SlotChangedState===
                    槽位: %d
                    状态: %s
                    """,
                slot,
                newState ? "已启用/选中" : "未启用"
            );
        }

        // 客户端命令（例如开始冲刺、停止潜行、开始跳跃充能）
        if (packet instanceof ClientCommandC2SPacket p) {
            ClientCommandC2SPacket.Mode mode = p.getMode();
            int mountJumpPower = p.getMountJumpHeight();
            return String.format("""
                    ===ClientCommand===
                    模式: %s
                    骑乘跳跃强度: %d
                    """,
                mode,
                mountJumpPower
            );
        }


        // 点击槽位
        if (packet instanceof ClickSlotC2SPacket click) {
            int slot = click.getSlot(); // 被点击的槽位
            int button = click.getButton(); // 鼠标键
            SlotActionType actionType = click.getActionType(); // 点击动作类型（PICKUP、QUICK_MOVE等）
            ItemStack stack = click.getStack(); // 被点击槽位上的物品
            Int2ObjectMap<ItemStack> modified = click.getModifiedStacks(); // 修改的槽位

            return String.format("""
                    ===ClickSlot===
                    窗口ID: %d
                    槽位: %d
                    按钮: %d
                    动作类型: %s
                    物品: %s x%d
                    修改项:
                      %s
                    """,
                slot,
                button,
                actionType,
                stack.getItem().getName().getString(),
                stack.getCount(),
                formatItemMap(modified)
            );
        }

        // 按钮点击（例如熔炉、工作台按钮）
        if (packet instanceof ButtonClickC2SPacket btn) {
            int syncId = btn.syncId();
            int buttonId = btn.buttonId();
            return String.format("""
                    ===ButtonClick===
                    窗口ID: %d
                    按钮ID: %d
                    """,
                syncId,
                buttonId
            );
        }


        // 关闭界面
        if (packet instanceof CloseHandledScreenC2SPacket close) {
            int syncId = close.getSyncId();
            return String.format("""
                    ===CloseHandledScreen===
                    窗口ID: %d
                    """,
                syncId
            );
        }

        // 挥手（动画）
        if (packet instanceof HandSwingC2SPacket swing) {
            Hand hand = swing.getHand();
            return String.format("""
                    ===HandSwing===
                    手: %s
                    """,
                hand
            );
        }


        try {
            Class<?> packetClass = packet.getClass();
            Field[] fields = packetClass.getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true);
                String fieldName = field.getName();

                // 跳过一些不重要的字段
                if (fieldName.equals("LOGGER") || fieldName.equals("codec") ||
                    fieldName.equals("field_37935") || fieldName.equals("field_37936")) {
                    continue;
                }

                try {
                    Object value = field.get(packet);
                    String valueStr = formatFieldValue(value, fieldName);

                    if (!valueStr.isEmpty()) {
                        details.append(String.format("  %s: %s\n", fieldName, valueStr));
                    }
                } catch (IllegalAccessException e) {
                    details.append(String.format("  %s: [访问失败]\n", fieldName));
                }
            }

            // 移除最后的换行符
            if (details.length() > 0) {
                details.setLength(details.length() - 1);
            }

        } catch (Exception e) {
            return "[解析失败: " + e.getMessage() + "]";
        }

        return details.toString();
    }

    /**
     * 格式化字段值，处理各种数据类型
     */
    private String formatFieldValue(Object value, String fieldName) {
        if (value == null) {
            return "null";
        }

        String className = value.getClass().getSimpleName();

        // 处理基础类型
        if (value instanceof String) {
            String str = (String) value;
            if (str.length() > 50) {
                return "§b\"" + str.substring(0, 47) + "...\"§7";
            }
            return "§b\"" + str + "\"§7";
        } else if (value instanceof Number) {
            return "§6" + value + "§7";
        } else if (value instanceof Boolean) {
            return "§a" + value + "§7";
        } else if (value instanceof Enum) {
            return "§d" + ((Enum<?>) value).name() + "§7";
        }

        // 处理位置相关类型
        if (className.contains("BlockPos")) {
            return "§e" + value + "§7";
        } else if (className.contains("Vec3d")) {
            return "§e" + value + "§7";
        } else if (className.contains("Vec3i")) {
            return "§e" + value + "§7";
        }

        // 处理实体ID
        if (className.contains("EntityId") || className.contains("int") &&
            (fieldName != null && (fieldName.contains("id") || fieldName.contains("Id")))) {
            return "§c" + value + "§7";
        }

        // 处理数组类型
        if (value.getClass().isArray()) {
            if (value instanceof byte[]) {
                int length = ((byte[]) value).length;
                return "§cbyte[" + length + "]§7";
            } else if (value instanceof int[]) {
                int length = ((int[]) value).length;
                return "§cint[" + length + "]§7";
            }
            return "§c" + className + "[]§7";
        }

        // 处理集合类型
        if (value instanceof Collection) {
            int size = ((Collection<?>) value).size();
            return "§d" + className + "(" + size + " items)§7";
        }

        // 处理NBT相关
        if (className.contains("Nbt") || className.contains("NBT")) {
            String nbtStr = value.toString();
            if (nbtStr.length() > 100) {
                return "§c" + className + "[...]§7";
            }
            return "§c" + nbtStr + "§7";
        }

        // 处理ItemStack
        if (className.contains("ItemStack")) {
            return "§eItemStack§7";
        }

        // 处理Text组件
        if (className.contains("Text")) {
            return "§b" + value.toString().replace("\n", "\\n") + "§7";
        }

        // 处理其他对象
        String result = value.toString();
        if (result.length() > 150) {
            return "§c" + className + "[数据过长]§7";
        }

        return "§f" + result + "§7";
    }

    /**
     * 截断字符串到指定长度
     */
    private String truncateString(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    private static String formatItemMap(Int2ObjectMap<ItemStack> map) {
        if (map == null || map.isEmpty()) return "无修改";

        StringBuilder sb = new StringBuilder();
        sb.append("修改槽位:\n");
        map.forEach((slot, stack) -> {
            String itemName = stack.isEmpty() ? "空" : stack.getItem().getName().getString();
            sb.append(String.format("  [%d] %s x%d\n", slot, itemName, stack.getCount()));
        });
        return sb.toString();
    }


    private static String getReadablePacketName(Packet<?> packet) {
        String deobfName = packet.getClass().getSimpleName(); // 混淆名，例如 "net.minecraft.class_2828$class_2830"

        return deobfName.substring(deobfName.lastIndexOf('.') + 1);
    }
}
