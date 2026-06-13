package com.github.mikumiku.addon.modules;


import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.BagUtil;
import com.github.mikumiku.addon.util.Rotation;
import com.github.mikumiku.addon.util.RotationManager;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShapes;

public class Phase extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPearl = settings.createGroup("末影珍珠");
    private final SettingGroup sgClip = settings.createGroup("穿墙设置");

    // General
    private final Setting<PhaseMode> mode = sgGeneral.add(new EnumSetting.Builder<PhaseMode>()
        .name("模式")
        .description("穿墙模式选择")
        .defaultValue(PhaseMode.Normal)
        .build()
    );

    // Pearl settings
    private final Setting<Integer> pitch = sgPearl.add(new IntSetting.Builder()
        .name("俯仰角")
        .description("投掷末影珍珠的俯仰角度")
        .defaultValue(85)
        .min(70)
        .max(90)
        .visible(() -> mode.get() == PhaseMode.Pearl)
        .build()
    );

    private final Setting<Boolean> attack = sgPearl.add(new BoolSetting.Builder()
        .name("攻击实体")
        .description("攻击珍珠路径上的实体")
        .defaultValue(false)
        .visible(() -> mode.get() == PhaseMode.Pearl)
        .build()
    );

    private final Setting<Boolean> selfFill = sgPearl.add(new BoolSetting.Builder()
        .name("自动填充")
        .description("自动在穿墙位置填充方块")
        .defaultValue(false)
        .visible(() -> mode.get() == PhaseMode.Pearl)
        .build()
    );

    // Clip settings
    private final Setting<Double> blocks = sgClip.add(new DoubleSetting.Builder()
        .name("穿墙距离")
        .description("穿墙的方块距离")
        .defaultValue(0.003)
        .min(0.001)
        .max(10.0)
        .sliderMax(1.0)
        .visible(() -> mode.get() != PhaseMode.Pearl && mode.get() != PhaseMode.Clip)
        .build()
    );

    private final Setting<Double> distance = sgClip.add(new DoubleSetting.Builder()
        .name("偏移距离")
        .description("穿墙时的偏移距离")
        .defaultValue(0.2)
        .min(0.0)
        .max(10.0)
        .sliderMax(1.0)
        .visible(() -> mode.get() != PhaseMode.Pearl && mode.get() != PhaseMode.Clip)
        .build()
    );

    private final Setting<Boolean> autoClip = sgClip.add(new BoolSetting.Builder()
        .name("自动穿墙")
        .description("自动执行穿墙操作")
        .defaultValue(true)
        .visible(() -> mode.get() != PhaseMode.Pearl && mode.get() != PhaseMode.Clip)
        .build()
    );

    public Phase() {
        super(BaseModule.CATEGORY_MIKU_COMBAT, "珍珠穿墙", "允许玩家穿过固体方块");
    }

    @Override
    public void onActivate() {
        if (mc.player == null) return;

        if (mode.get() == PhaseMode.Pearl) {
            throwPearl();
            toggle();
        } else if (autoClip.get() && mode.get() != PhaseMode.Clip) {
            performAutoClip();
        }
    }

    @EventHandler
    private void onTick(PlayerMoveEvent event) {
        if (mode.get() == PhaseMode.Clip && mc.player.isOnGround() && !mc.player.hasVehicle()) {
            Vec3d center = mc.player.getBlockPos().toCenterPos();
            boolean flagX = (center.x - mc.player.getX()) > 0;
            boolean flagZ = (center.z - mc.player.getZ()) > 0;
            double x = center.x + 0.2 * (flagX ? -1 : 1);
            double z = center.z + 0.2 * (flagZ ? -1 : 1);
            mc.player.setPosition(x, mc.player.getY(), z);
            toggle();
        }
    }

    @EventHandler
    private void onCollisionShape(CollisionShapeEvent event) {
        if (mc.player == null) return;

        switch (mode.get()) {
            case Normal -> {
                if (event.shape != VoxelShapes.empty() &&
                    event.shape.getBoundingBox().maxY > mc.player.getBoundingBox().minY &&
                    mc.player.isSneaking()) {
                    event.shape = VoxelShapes.empty();
                }
            }
            case Sand -> {
                event.shape = VoxelShapes.empty();
                mc.player.noClip = true;
            }
            case Climb -> {
                if (mc.player.horizontalCollision) {
                    event.shape = VoxelShapes.empty();
                }
            }
        }
    }

    private void throwPearl() {
        int slot = BagUtil.findItemInventorySlot(Items.ENDER_PEARL);
        if (slot == -1) {
            error("未找到末影珍珠或珍珠冷却中！");
            return;
        }

        float prevYaw = mc.player.getYaw();
        float prevPitch = mc.player.getPitch();
        Vec3d target = new Vec3d(Math.floor(mc.player.getX()) + 0.5, 0.0, Math.floor(mc.player.getZ()) + 0.5);

        double deltaX = target.x - mc.player.getX();
        double deltaZ = target.z - mc.player.getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0f) + 180.0f;

        if (attack.get()) {
            attackEntities(yaw);
            destroyScaffolding();
        }

        if (selfFill.get()) {
            placeBlockUnderFeet(yaw);
        }

        boolean registered = RotationManager.getInstance().register(new Rotation(yaw, pitch.get()));
        if (registered) {

        }
        // Throw pearl
        Rotations.rotate(yaw, pitch.get(), () -> {
            BagUtil.doSwap(slot);
            mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, yaw, pitch.get()));

            mc.player.swingHand(Hand.MAIN_HAND);
            BagUtil.doSwap(slot);
        });

        mc.player.setYaw(prevYaw);
        mc.player.setPitch(prevPitch);
    }

    private void attackEntities(float yaw) {
        BlockHitResult hit = (BlockHitResult) mc.player.raycast(3.0, 0, false);
        Box box = new Box(hit.getBlockPos()).expand(0.2);

        for (var entity : mc.world.getOtherEntities(null, box)) {
            if (entity instanceof ItemFrameEntity) {
                mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
                mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
        }
    }

    private void destroyScaffolding() {
        if (mc.world.getBlockState(mc.player.getBlockPos()).getBlock() instanceof ScaffoldingBlock) {
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                mc.player.getBlockPos(),
                Direction.UP
            ));
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                mc.player.getBlockPos(),
                Direction.UP
            ));
        }
    }

    private void placeBlockUnderFeet(float yaw) {
        float normalizedYaw = yaw % 360.0f;
        if (normalizedYaw < 0.0f) normalizedYaw += 360.0f;

        BlockPos pos = mc.player.getBlockPos();
        if (normalizedYaw >= 22.5 && normalizedYaw < 67.5) pos = pos.south().west();
        else if (normalizedYaw >= 67.5 && normalizedYaw < 112.5) pos = pos.west();
        else if (normalizedYaw >= 112.5 && normalizedYaw < 157.5) pos = pos.north().west();
        else if (normalizedYaw >= 157.5 && normalizedYaw < 202.5) pos = pos.north();
        else if (normalizedYaw >= 202.5 && normalizedYaw < 247.5) pos = pos.north().east();
        else if (normalizedYaw >= 247.5 && normalizedYaw < 292.5) pos = pos.east();
        else if (normalizedYaw >= 292.5 && normalizedYaw < 337.5) pos = pos.south().east();
        else pos = pos.south();

        FindItemResult block = InvUtils.findInHotbar(itemStack ->
            itemStack.getItem() == Items.OBSIDIAN ||
                itemStack.getItem() == Items.END_STONE ||
                itemStack.getItem() == Items.NETHERITE_BLOCK ||
                itemStack.getItem() == Items.DIAMOND_BLOCK ||
                itemStack.getItem() == Items.IRON_BLOCK ||
                itemStack.getItem() == Items.GOLD_BLOCK ||
                itemStack.getItem() == Items.COAL_BLOCK ||
                itemStack.getItem() == Items.REDSTONE_BLOCK
        );

        if (block.found() && !mc.world.getBlockState(pos.down()).isReplaceable()) {
            BlockUtils.place(pos, block, true, 0);
        }
    }

    private void performAutoClip() {
        double cos = Math.cos(Math.toRadians(mc.player.getYaw() + 90.0f));
        double sin = Math.sin(Math.toRadians(mc.player.getYaw() + 90.0f));
        mc.player.setPosition(
            mc.player.getX() + blocks.get() * cos,
            mc.player.getY(),
            mc.player.getZ() + blocks.get() * sin
        );
    }

    private boolean isPhasing() {
        Box bb = mc.player.getBoundingBox();
        for (int x = MathHelper.floor(bb.minX); x < MathHelper.floor(bb.maxX) + 1; x++) {
            for (int y = MathHelper.floor(bb.minY); y < MathHelper.floor(bb.maxY) + 1; y++) {
                for (int z = MathHelper.floor(bb.minZ); z < MathHelper.floor(bb.maxZ) + 1; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (mc.world.getBlockState(pos).blocksMovement()) {
                        if (bb.intersects(new Box(x, y, z, x + 1.0, y + 1.0, z + 1.0))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public String getInfoString() {
        return mode.get().name();
    }

    public enum PhaseMode {
        Normal,
        Sand,
        Climb,
        Pearl,
        Clip
    }
}
