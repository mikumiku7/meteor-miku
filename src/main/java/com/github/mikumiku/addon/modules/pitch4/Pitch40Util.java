//package com.github.mikumiku.addon.modules.pitch4;
//
//import com.stash.hunt.Addon;
//import meteordevelopment.meteorclient.events.world.TickEvent;
//import meteordevelopment.meteorclient.settings.*;
//import meteordevelopment.meteorclient.systems.modules.Module;
//import meteordevelopment.meteorclient.systems.modules.Modules;
//import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly;
//import meteordevelopment.meteorclient.utils.player.InvUtils;
//import meteordevelopment.orbit.EventHandler;
//import net.minecraft.util.Hand;
//import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFlightModes;
//
//import static com.stash.hunt.Utils.firework;
//
//
//public class Pitch40Util extends Module {
//
//    private final SettingGroup sgGeneral = settings.getDefaultGroup();
//
//    public final Setting<Double> boundGap = sgGeneral.add(new DoubleSetting.Builder()
//        .name("bound-gap")
//        .description("The gap between the upper and lower bounds. Used when reconnecting, or when at max height if Auto Adjust Bounds is enabled.")
//        .defaultValue(60)
//        .sliderRange(50, 100)
//        .build()
//    );
//
//    public Pitch40Util() {
//        super(Addon.CATEGORY, "Pitch40Util", "Makes sure pitch 40 stays on when reconnecting to 2b2t, and sets your bounds as you reach highest point each climb.");
//    }
//
//    Module elytraFly = Modules.get().get(ElytraFly.class);
//
//    int fireworkCooldown = 0;
//
//    boolean goingUp = true;
//
//    int elytraSwapSlot = -1;
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
//            if (fireworkCooldown > 0) {
//                fireworkCooldown--;
//            }
//
//            if (elytraSwapSlot != -1) {
//                InvUtils.swap(elytraSwapSlot, true);
//                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
//                InvUtils.swapBack();
//                elytraSwapSlot = -1;
//            }
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
//
//}
