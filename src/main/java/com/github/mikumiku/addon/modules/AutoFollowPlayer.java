package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.BagUtil;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.List;

public class AutoFollowPlayer extends BaseModule {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgRender = this.settings.createGroup("渲染设置");

    private final List<Entity> targets = new ArrayList<>();

    // ──────────────────────────────── 通用设置 ────────────────────────────────

    private final Setting<SortPriority> priority = this.sgGeneral.add(
        new EnumSetting.Builder<SortPriority>()
            .name("目标优先级")
            .description("当检测到多个敌人时，按何种顺序优先锁定目标")
            .defaultValue(SortPriority.LowestDistance)
            .build()
    );

    private final Setting<Double> range = this.sgGeneral.add(
        new DoubleSetting.Builder()
            .name("检测范围")
            .description("可锁定敌人的最大距离范围")
            .defaultValue(50.0)
            .range(0.0, 192.0)
            .build()
    );

    private final Setting<Boolean> onlyAir = this.sgGeneral.add(
        new BoolSetting.Builder()
            .name("仅限空中目标")
            .description("只对处于空中的目标进行检测与锁定")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> preventGround = this.sgGeneral.add(
        new BoolSetting.Builder()
            .name("防止落地")
            .description("启用后，角色在执行期间不会触地")
            .defaultValue(true)
            .build()
    );

// ──────────────────────────────── 渲染设置 ────────────────────────────────

    private final Setting<Boolean> render = this.sgRender.add(
        new BoolSetting.Builder()
            .name("启用渲染")
            .description("是否在屏幕上渲染锁定目标的标识")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = this.sgRender.add(
        new EnumSetting.Builder<ShapeMode>()
            .name("渲染模式")
            .description("决定目标标识的显示方式（线框 / 填充 / 双重）")
            .defaultValue(ShapeMode.Both)
            .visible(this.render::get)
            .build()
    );

    private final Setting<SettingColor> sideColor = this.sgRender.add(
        new ColorSetting.Builder()
            .name("填充颜色")
            .description("目标区域的填充部分颜色")
            .defaultValue(new SettingColor(160, 0, 225, 35))
            .visible(() -> this.shapeMode.get().sides())
            .build()
    );

    private final Setting<SettingColor> lineColor = this.sgRender.add(
        new ColorSetting.Builder()
            .name("轮廓颜色")
            .description("目标轮廓线的颜色")
            .defaultValue(new SettingColor(255, 255, 255, 50))
            .visible(() -> this.render.get() && this.shapeMode.get().lines())
            .build()
    );


    public AutoFollowPlayer() {
        super(BaseModule.CATEGORY_MIKU_COMBAT, "鞘翅追人", "鞘翅追人");
    }

    @Override
    public void onActivate() {
        this.targets.clear();
        TargetUtils.getList(
            this.targets,
            entity -> {
                if (entity instanceof PlayerEntity player) {
                    if (Friends.get().isFriend(player)) {
                        return false;
                    } else if (entity == this.mc.player) {
                        return false;
                    } else {
                        Box hitbox = entity.getBoundingBox();
                        return PlayerUtils.isWithin(
                            MathHelper.clamp(this.mc.player.getX(), hitbox.minX, hitbox.maxX),
                            MathHelper.clamp(this.mc.player.getY(), hitbox.minY, hitbox.maxY),
                            MathHelper.clamp(this.mc.player.getZ(), hitbox.minZ, hitbox.maxZ),
                            this.range.get()
                        );
                    }
                } else {
                    return false;
                }
            },
            this.priority.get(),
            1
        );
        BagUtil.quickUse(Items.FIREWORK_ROCKET);
    }

    @Override
    public void onDeactivate() {
        this.targets.clear();
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (this.mc.player.isAlive() && PlayerUtils.getGameMode() != GameMode.SPECTATOR) {
            // 如果没有目标，直接返回，不执行任何逻辑
            if (this.targets.isEmpty()) {
                return;
            }

            if (!this.onlyAir.get() || !this.mc.player.isOnGround()) {
                Entity primary = this.targets.getFirst();
                if (!this.preventGround.get() || !primary.isOnGround()) {
                    MeteorClient.mc.player.setYaw((float) Rotations.getYaw(primary));
                }

                MeteorClient.mc
                    .player
                    .setPitch(primary.isOnGround() && this.preventGround.get() ? -90.0F : (float) Rotations.getPitch(primary, Target.Body));
            }

            try {
                Entity lastAttackedEntity = this.targets.getFirst();
                if (this.targets.getFirst() != null) {
                    double x = MathHelper.lerp(event.tickDelta, lastAttackedEntity.lastRenderX, lastAttackedEntity.getX())
                        - lastAttackedEntity.getX();
                    double y = MathHelper.lerp(event.tickDelta, lastAttackedEntity.lastRenderY, lastAttackedEntity.getY())
                        - lastAttackedEntity.getY();
                    double z = MathHelper.lerp(event.tickDelta, lastAttackedEntity.lastRenderZ, lastAttackedEntity.getZ())
                        - lastAttackedEntity.getZ();
                    Box box = lastAttackedEntity.getBoundingBox();
                    event.renderer
                        .box(
                            x + box.minX,
                            y + box.minY,
                            z + box.minZ,
                            x + box.maxX,
                            y + box.maxY,
                            z + box.maxZ,
                            this.sideColor.get(),
                            this.lineColor.get(),
                            this.shapeMode.get(),
                            0
                        );
                }
            } catch (Exception var10) {
            }
        }
    }

    @Override
    public String getInfoString() {
        return !this.targets.isEmpty() ? EntityUtils.getName(this.targets.getFirst()) : null;
    }
}
