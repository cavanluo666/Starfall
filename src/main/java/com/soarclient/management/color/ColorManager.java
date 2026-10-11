package com.soarclient.management.color;

import com.soarclient.SoarConfig;
import com.soarclient.management.color.api.ColorPalette;

public class ColorManager {

    private ColorPalette palette;

    public ColorManager() {
        updatePalette();
    }

    public void onTick() {
        SoarConfig cfg = SoarConfig.get();
        if (palette == null || palette.isDarkMode() != cfg.isDarkMode() || palette.getHct() != cfg.getHct()) {
            updatePalette();
        }
    }

    private void updatePalette() {
        SoarConfig cfg = SoarConfig.get();
        palette = new ColorPalette(cfg.getHct(), cfg.isDarkMode());
    }

    public ColorPalette getPalette() {
        if (palette == null) updatePalette();
        return palette;
    }
}
