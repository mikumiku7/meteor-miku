package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.mixin.WorldRendererAccessor;
import com.github.mikumiku.addon.util.BagUtil;
import com.github.mikumiku.addon.util.BaritoneUtil;
import com.github.mikumiku.addon.util.Rotation;
import com.github.mikumiku.addon.util.RotationManager;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import com.github.mikumiku.addon.util.Via;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.BlockBreakingInfo;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class Surround extends BaseModule {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgToggles = settings.createGroup("切换设置");
    private final SettingGroup sgRender = settings.createGroup("渲染设置");

    // 通用
    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("方块类型")
        .description("用于包裹自身的方块类型。")
        .defaultValue(Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.ENDER_CHEST, Blocks.NETHERITE_BLOCK)
        .filter(this::blockFilter)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("放置延迟")
        .description("每次放置方块之间的延迟（以刻为单位）。")
        .min(0)
        .defaultValue(0)
        .build()
    );

    private final Setting<Surround.Center> center = sgGeneral.add(new EnumSetting.Builder<Surround.Center>()
        .name("自动居中")
        .description("启用后会将玩家传送至当前方块中心位置。")
        .defaultValue(Center.Incomplete)
        .build()
    );

    // 是否启用移动检测
    private final Setting<Boolean> pauseOnMovement = sgGeneral.add(new BoolSetting.Builder()
        .name("移动时暂停")
        .description("当玩家移动时暂停")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> doubleHeight = sgGeneral.add(new BoolSetting.Builder()
        .name("双层高度")
        .description("在原始包裹方块上方再放置一层黑曜石，防止他人直接破防。")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("仅在地面生效")
        .description("仅当玩家站在方块上时才工作。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> toggleModules = sgGeneral.add(new BoolSetting.Builder()
        .name("启用时关闭其他模块")
        .description("在启动包裹模块时自动关闭指定的其他模块。")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> toggleBack = sgGeneral.add(new BoolSetting.Builder()
        .name("关闭后重新启用")
        .description("在包裹模块关闭时，重新开启之前关闭的模块。")
        .defaultValue(false)
        .visible(toggleModules::get)
        .build()
    );

    private final Setting<List<Module>> modules = sgGeneral.add(new ModuleListSetting.Builder()
        .name("受影响模块")
        .description("指定在启用包裹模块时应关闭的模块。")
        .visible(toggleModules::get)
        .build()
    );

    private final Setting<Boolean> protect = sgGeneral.add(new BoolSetting.Builder()
        .name("防护破坏")
        .description("尝试打掉周围的水晶以防止包裹方块被破坏。")
        .defaultValue(true)
        .build()
    );

    // 切换条件
    private final Setting<Boolean> toggleOnYChange = sgToggles.add(new BoolSetting.Builder()
        .name("高度变化时关闭")
        .description("当玩家高度（Y坐标）变化时自动禁用（如跳跃、上台阶等）。")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> toggleOnComplete = sgToggles.add(new BoolSetting.Builder()
        .name("完成放置后关闭")
        .description("当所有方块放置完成后自动关闭模块。")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> toggleOnDeath = sgToggles.add(new BoolSetting.Builder()
        .name("死亡时关闭")
        .description("当玩家死亡时自动关闭模块。")
        .defaultValue(true)
        .build()
    );

    // 渲染设置
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder()
        .name("渲染挥手动作")
        .description("放置方块时渲染玩家手部挥动动画。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("启用渲染")
        .description("在即将放置黑曜石的位置渲染方块轮廓。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderBelow = sgRender.add(new BoolSetting.Builder()
        .name("渲染脚下方块")
        .description("是否渲染脚下的方块。")
        .defaultValue(false)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("渲染模式")
        .description("控制方块形状的渲染方式。")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> safeSideColor = sgRender.add(new ColorSetting.Builder()
        .name("安全方块面颜色")
        .description("安全方块的表面颜色。")
        .defaultValue(new SettingColor(13, 255, 0, 0))
        .visible(() -> render.get() && shapeMode.get() != ShapeMode.Lines)
        .build()
    );

    private final Setting<SettingColor> safeLineColor = sgRender.add(new ColorSetting.Builder()
        .name("安全方块线颜色")
        .description("安全方块的线条颜色。")
        .defaultValue(new SettingColor(13, 255, 0, 0))
        .visible(() -> render.get() && shapeMode.get() != ShapeMode.Sides)
        .build()
    );

    private final Setting<SettingColor> normalSideColor = sgRender.add(new ColorSetting.Builder()
        .name("普通方块面颜色")
        .description("普通方块的表面颜色。")
        .defaultValue(new SettingColor(0, 255, 238, 12))
        .visible(() -> render.get() && shapeMode.get() != ShapeMode.Lines)
        .build()
    );

    private final Setting<SettingColor> normalLineColor = sgRender.add(new ColorSetting.Builder()
        .name("普通方块线颜色")
        .description("普通方块的线条颜色。")
        .defaultValue(new SettingColor(0, 255, 238, 100))
        .visible(() -> render.get() && shapeMode.get() != ShapeMode.Sides)
        .build()
    );

    private final Setting<SettingColor> unsafeSideColor = sgRender.add(new ColorSetting.Builder()
        .name("危险方块面颜色")
        .description("危险方块的表面颜色。")
        .defaultValue(new SettingColor(204, 0, 0, 12))
        .visible(() -> render.get() && shapeMode.get() != ShapeMode.Lines)
        .build()
    );

    private final Setting<SettingColor> unsafeLineColor = sgRender.add(new ColorSetting.Builder()
        .name("危险方块线颜色")
        .description("危险方块的线条颜色。")
        .defaultValue(new SettingColor(204, 0, 0, 100))
        .visible(() -> render.get() && shapeMode.get() != ShapeMode.Sides)
        .build()
    );

    private final Mutable placePos = new Mutable();
    private final Mutable renderPos = new Mutable();
    private final Mutable testPos = new Mutable();
    public ArrayList<Module> toActivate = new ArrayList<>();
    private int ticks;

    public Surround() {
        super(BaseModule.CATEGORY_MIKU_COMBAT, "围脚", "使用方块快速包裹自己的玉足，减少水晶爆炸造成的伤害。");
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (this.render.get()) {
            if (this.renderBelow.get()) {
                this.draw(event, null, -1, 0);
            }

            for (CardinalDirection direction : CardinalDirection.values()) {
                this.draw(event, direction, 0, this.doubleHeight.get() ? 2 : 0);
            }

            if (this.doubleHeight.get()) {
                for (CardinalDirection direction : CardinalDirection.values()) {
                    this.draw(event, direction, 1, 4);
                }
            }
        }
    }

    private void draw(Render3DEvent event, CardinalDirection direction, int y, int exclude) {
        this.renderPos.set(this.offsetPosFromPlayer(direction, y));
        Color sideColor = this.getSideColor(this.renderPos);
        Color lineColor = this.getLineColor(this.renderPos);
        event.renderer.box(this.renderPos, sideColor, lineColor, this.shapeMode.get(), exclude);
    }

    @Override
    public void onActivate() {
        if (this.center.get() == Center.OnActivate) {
            PlayerUtils.centerPlayer();
        }

        this.ticks = 0;
        if (this.toggleModules.get() && !this.modules.get().isEmpty() && mc.world != null && mc.player != null) {
            for (Module module : this.modules.get()) {
                if (module.isActive()) {
                    module.toggle();
                    this.toActivate.add(module);
                }
            }
        }
    }

    @Override
    public void onDeactivate() {
        if (this.toggleBack.get() && !this.toActivate.isEmpty() && mc.world != null && mc.player != null) {
            for (Module module : this.toActivate) {
                if (!module.isActive()) {
                    module.toggle();
                }
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (this.ticks > 0) {
            this.ticks--;
        } else {
            this.ticks = this.delay.get();
            if (pauseOnMovement.get() && isPlayerMoving()) {
                return;
            }

            if (this.toggleOnYChange.get() && mc.player.lastRenderY != mc.player.getY()) {
                this.toggle();
            } else if (!this.onlyOnGround.get() || mc.player.isOnGround()) {
                if (this.getInvBlock() != -1) {
                    if (this.center.get() == Center.Always) {
                        PlayerUtils.centerPlayer();
                    }

                    int safe = 0;

                    for (CardinalDirection direction : CardinalDirection.values()) {
                        if (this.place(direction, 0)) {
                            break;
                        }

                        safe++;
                    }

                    if (this.doubleHeight.get() && safe == 4) {
                        for (CardinalDirection direction : CardinalDirection.values()) {
                            if (this.place(direction, 1)) {
                                break;
                            }

                            safe++;
                        }
                    }

                    boolean complete = safe == (this.doubleHeight.get() ? 8 : 4);
                    if (complete && this.toggleOnComplete.get()) {
                        this.toggle();
                    } else {
                        if (!complete && this.center.get() == Center.Incomplete) {
                            PlayerUtils.centerPlayer();
                        }
                    }
                }
            }
        }
    }

    private boolean place(CardinalDirection direction, int y) {
        this.placePos.set(this.offsetPosFromPlayer(direction, y));
        int slot = getInvBlock();
        boolean placed = false;

        if (slot != -1) {
            BagUtil.doSwap(slot);
            placed = BaritoneUtil.placeBlock(this.placePos);
            BagUtil.doSwap(slot);
        }

        boolean beingMined = false;
        ObjectIterator isThreat = ((WorldRendererAccessor) mc.worldRenderer).getBlockBreakingInfos().values().iterator();

        while (isThreat.hasNext()) {
            BlockBreakingInfo value = (BlockBreakingInfo) isThreat.next();
            if (value.getPos().equals(this.placePos)) {
                beingMined = true;
                break;
            }
        }

        boolean isThreatx = mc.world.getBlockState(this.placePos).isReplaceable() || beingMined;
        if (this.protect.get() && !placed && isThreatx) {
            Box box = new Box(
                this.placePos.getX() - 1,
                this.placePos.getY() - 1,
                this.placePos.getZ() - 1,
                this.placePos.getX() + 1,
                this.placePos.getY() + 1,
                this.placePos.getZ() + 1
            );
            Predicate<Entity> entityPredicate = entity -> entity instanceof EndCrystalEntity
                && DamageUtils.crystalDamage(mc.player, Via.getEntityPos(entity)) < PlayerUtils.getTotalHealth();

            for (Entity crystal : mc.world.getOtherEntities(null, box, entityPredicate)) {

                RotationManager.getInstance().register(new Rotation((float) Rotations.getYaw(crystal), (float) Rotations.getPitch(crystal)));
                mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));

                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
        }

        return placed;
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof DeathMessageS2CPacket packet) {
            Entity entity = mc.world.getEntityById(packet.playerId());
            if (entity == mc.player && this.toggleOnDeath.get()) {
                this.toggle();
                info("你死了，再接再厉.");
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
            Input.isPressed(mc.options.jumpKey);

        return keyPressed;
    }

    private Mutable offsetPosFromPlayer(CardinalDirection direction, int y) {
        return this.offsetPos(mc.player.getBlockPos(), direction, y);
    }

    private Mutable offsetPos(BlockPos origin, CardinalDirection direction, int y) {
        return direction == null
            ? this.testPos.set(origin.getX(), origin.getY() + y, origin.getZ())
            : this.testPos
            .set(
                origin.getX() + direction.toDirection().getOffsetX(),
                origin.getY() + y,
                origin.getZ() + direction.toDirection().getOffsetZ()
            );
    }

    private BlockType getBlockType(BlockPos pos) {
        BlockState blockState = mc.world.getBlockState(pos);
        if (blockState.getBlock().getHardness() < 0.0F) {
            return BlockType.Safe;
        } else {
            return blockState.getBlock().getBlastResistance() >= 600 ? BlockType.Normal : BlockType.Unsafe;
        }
    }

    private Color getSideColor(BlockPos pos) {
        return switch (this.getBlockType(pos)) {
            case Safe -> this.safeSideColor.get();
            case Normal -> this.normalSideColor.get();
            case Unsafe -> this.unsafeSideColor.get();
        };
    }

    private Color getLineColor(BlockPos pos) {
        return switch (this.getBlockType(pos)) {
            case Safe -> this.safeLineColor.get();
            case Normal -> this.normalLineColor.get();
            case Unsafe -> this.unsafeLineColor.get();
        };
    }

    private int getInvBlock() {
        return BagUtil.findItemInventorySlot(itemStack -> this.blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
    }

    private boolean blockFilter(Block block) {
        return block == Blocks.OBSIDIAN
            || block == Blocks.CRYING_OBSIDIAN
            || block == Blocks.NETHERITE_BLOCK
            || block == Blocks.ENDER_CHEST
            || block == Blocks.RESPAWN_ANCHOR;
    }

    public enum BlockType {
        Safe,
        Normal,
        Unsafe
    }

    public enum Center {
        Never,
        OnActivate,
        Incomplete,
        Always
    }
}
