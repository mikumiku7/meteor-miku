package com.github.mikumiku.addon.hud;

import com.github.mikumiku.addon.MikuMikuAddon;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class HudMiku extends HudElement {
    /**
     * The {@code name} parameter should be in kebab-case.
     */
    public static final HudElementInfo<HudMiku> INFO = new HudElementInfo<>(MikuMikuAddon.HUD_GROUP, "miku", "HUD element miku.", HudMiku::new);

    public HudMiku() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        setSize(renderer.textWidth("Miku element", true), renderer.textHeight(true));

        // Render background
        renderer.quad(x, y, getWidth(), getHeight(), Color.LIGHT_GRAY);

        // Render text
        renderer.text("Miku element", x, y, Color.WHITE, true);
    }
}
