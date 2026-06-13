//
//package com.github.mikumiku.addon.modules.pitch4;
//
//import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
//import meteordevelopment.meteorclient.events.world.TickEvent;
//import meteordevelopment.meteorclient.mixininterface.IVec3d;
//import meteordevelopment.meteorclient.settings.BoolSetting;
//import meteordevelopment.meteorclient.settings.DoubleSetting;
//import meteordevelopment.meteorclient.settings.Setting;
//import meteordevelopment.meteorclient.settings.SettingGroup;
//import meteordevelopment.meteorclient.systems.modules.Categories;
//import meteordevelopment.meteorclient.systems.modules.Module;
//import meteordevelopment.meteorclient.systems.modules.Modules;
//import meteordevelopment.orbit.EventHandler;
//import net.minecraft.client.MinecraftClient;
//import net.minecraft.entity.EquipmentSlot;
//import net.minecraft.item.ElytraItem;
//import net.minecraft.util.math.Vec3d;
//
//public class ElytraFly extends Module {
//    private final SettingGroup sgGeneral = settings.getDefaultGroup();
//
//    // General
//
//    public final Setting<Double> pitch40lowerBounds = sgGeneral.add(new DoubleSetting.Builder()
//        .name("pitch40-lower-bounds")
//        .description("The bottom height boundary for pitch40.")
//        .defaultValue(80)
//        .min(-128)
//        .sliderMax(360)
//        .build()
//    );
//
//    public final Setting<Double> pitch40upperBounds = sgGeneral.add(new DoubleSetting.Builder()
//        .name("pitch40-upper-bounds")
//        .description("The upper height boundary for pitch40.")
//        .defaultValue(120)
//        .min(-128)
//        .sliderMax(360)
//        .build()
//    );
//
//    public final Setting<Double> pitch40rotationSpeed = sgGeneral.add(new DoubleSetting.Builder()
//        .name("pitch40-rotate-speed")
//        .description("The speed for pitch rotation (degrees per tick)")
//        .defaultValue(4)
//        .min(1)
//        .sliderMax(6)
//        .build()
//    );
//
//    public final Setting<Boolean> dontGoIntoUnloadedChunks = sgGeneral.add(new BoolSetting.Builder()
//        .name("no-unloaded-chunks")
//        .description("Stops you from going into unloaded chunks.")
//        .defaultValue(true)
//        .build()
//    );
//
//
//    public final Setting<Double> boundGap = sgGeneral.add(new DoubleSetting.Builder()
//        .name("bound-gap")
//        .description("The gap between the upper and lower bounds. Used when reconnecting, or when at max height if Auto Adjust Bounds is enabled.")
//        .defaultValue(60)
//        .sliderRange(50, 100)
//        .build()
//    );
//
//    private boolean pitchingDown = true;
//    private int pitch;
//    protected final ElytraFly elytraFly;
//    protected final MinecraftClient mc;
//
//    protected boolean lastForwardPressed;
//    protected double velX, velY, velZ;
//
//    private ElytraFly currentMode;
//
//    int fireworkCooldown = 0;
//
//    boolean goingUp = true;
//
//    int elytraSwapSlot = -1;
//
//    public ElytraFly() {
//        super(Categories.Movement, "elytra-fly", "Gives you more control over your elytra.");
//        this.elytraFly = Modules.get().get(ElytraFly.class);
//        this.mc = MinecraftClient.getInstance();
//        currentMode = this;
//    }
//
//    @Override
//    public void onActivate() {
//        if (mc.player.getY() < pitch40upperBounds.get()) {
//            error("Player must be above upper bounds!");
//            toggle();
//        }
//
//        pitch = 40;
//    }
//
//
//    @EventHandler
//    private void onPlayerMove(PlayerMoveEvent event) {
//        if (!(mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof ElytraItem)) return;
//
//        if (mc.player.isFallFlying()) {
//            currentMode.velX = 0;
//            currentMode.velY = event.movement.y;
//            currentMode.velZ = 0;
//
//
//            currentMode.handleHorizontalSpeed(event);
//
//            int chunkX = (int) ((mc.player.getX() + currentMode.velX) / 16);
//            int chunkZ = (int) ((mc.player.getZ() + currentMode.velZ) / 16);
//            if (dontGoIntoUnloadedChunks.get()) {
//                if (mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) {
//                    ((IVec3d) event.movement).set(currentMode.velX, currentMode.velY, currentMode.velZ);
//                } else {
//                    ((IVec3d) event.movement).set(0, currentMode.velY, 0);
//                }
//            } else {
//                ((IVec3d) event.movement).set(currentMode.velX, currentMode.velY, currentMode.velZ);
//            }
//
//        } else {
//            if (currentMode.lastForwardPressed) {
//                mc.options.forwardKey.setPressed(false);
//                currentMode.lastForwardPressed = false;
//            }
//        }
//    }
//
//    @EventHandler
//    private void onTick(TickEvent.Post event) {
//
//        if (pitchingDown && mc.player.getY() <= elytraFly.pitch40lowerBounds.get()) {
//            pitchingDown = false;
//        } else if (!pitchingDown && mc.player.getY() >= elytraFly.pitch40upperBounds.get()) {
//            pitchingDown = true;
//        }
//
//        // Pitch upwards
//        if (!pitchingDown && mc.player.getPitch() > -40) {
//            pitch -= elytraFly.pitch40rotationSpeed.get();
//
//            if (pitch < -40) pitch = -40;
//            // Pitch downwards
//        } else if (pitchingDown && mc.player.getPitch() < 40) {
//            pitch += elytraFly.pitch40rotationSpeed.get();
//
//            if (pitch > 40) pitch = 40;
//        }
//
//        mc.player.setPitch(pitch);
//    }
//
//
//    public void handleHorizontalSpeed(PlayerMoveEvent event) {
//        velX = event.movement.x;
//        velZ = event.movement.z;
//    }
//
//    private void resetBounds() {
//        Setting<Double> upperBounds = (Setting<Double>) elytraFly.settings.get("pitch40-upper-bounds");
//        upperBounds.set(mc.player.getY() - 5);
//        Setting<Double> lowerBounds = (Setting<Double>) elytraFly.settings.get("pitch40-lower-bounds");
//        lowerBounds.set(mc.player.getY() - 5 - boundGap.get());
//    }
//
//    @EventHandler
//    private void onTick(TickEvent.Pre event) {
//        if (elytraFly.isActive()) {
//
//            // this means the player fell below the lower bound, so we reset the bounds. this will only really happen if not using fireworks
//            if (mc.player.getY() <= (double) elytraFly.settings.get("pitch40-lower-bounds").get() - 10) {
//                resetBounds();
//                return;
//            }
//
//            // -40 pitch is facing upwards
//            if (mc.player.getPitch() == -40) {
////                info("Y less than upper bounds: " + (mc.player.getY() < (double)elytraFlyModule.settings.get("pitch40-upper-bounds").get()));
//                goingUp = true;
//            }
//            // waits until your at the highest point, when y velocity is 0, then sets min and max bounds based on your position
//            else if (goingUp && mc.player.getVelocity().y <= 0) {
//                goingUp = false;
//                resetBounds();
//            }
//        } else {
//            // waits for you to not be in queue, then turns elytrafly back on
//            if (!mc.player.getAbilities().allowFlying) {
//                elytraFly.toggle();
//                // always reset when rejoining
//                resetBounds();
//            }
//        }
//
//    }
//
//}
