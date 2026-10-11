package com.soarclient.management.color.api;

import java.awt.Color;

import com.soarclient.libraries.material3.Material3;
import com.soarclient.libraries.material3.dynamiccolor.DynamicScheme;
import com.soarclient.libraries.material3.dynamiccolor.MaterialDynamicColors;
import com.soarclient.libraries.material3.hct.Hct;
import com.soarclient.libraries.material3.utils.ColorUtils;

public class ColorPalette {

    private final DynamicScheme scheme;
    private final MaterialDynamicColors colors;
    private final boolean dark;
    private final Hct hct;

    public ColorPalette(Hct hct, boolean dark) {
        this(hct, dark, 0);
    }

    public ColorPalette(Hct hct, boolean dark, float contrast) {
        this.hct = hct;
        this.dark = dark;
        this.scheme = Material3.getDynamicScheme(hct, dark, contrast);
        this.colors = new MaterialDynamicColors();
    }

    private Color c(com.soarclient.libraries.material3.dynamiccolor.DynamicColor c) {
        return argbToColor(c.getArgb(scheme));
    }

    public Color getPrimary() { return c(colors.primary()); }
    public Color getSecondary() { return c(colors.secondary()); }
    public Color getTertiary() { return c(colors.tertiary()); }
    public Color getPrimaryContainer() { return c(colors.primaryContainer()); }
    public Color getSecondaryContainer() { return c(colors.secondaryContainer()); }
    public Color getTertiaryContainer() { return c(colors.tertiaryContainer()); }
    public Color getOnPrimary() { return c(colors.onPrimary()); }
    public Color getOnSecondary() { return c(colors.onSecondary()); }
    public Color getOnTertiary() { return c(colors.onTertiary()); }
    public Color getOnPrimaryContainer() { return c(colors.onPrimaryContainer()); }
    public Color getOnSecondaryContainer() { return c(colors.onSecondaryContainer()); }
    public Color getOnTertiaryContainer() { return c(colors.onTertiaryContainer()); }
    public Color getError() { return c(colors.error()); }
    public Color getErrorContainer() { return c(colors.errorContainer()); }
    public Color getOnError() { return c(colors.onError()); }
    public Color getOnErrorContainer() { return c(colors.onErrorContainer()); }
    public Color getBackground() { return c(colors.background()); }
    public Color getOnBackground() { return c(colors.onBackground()); }
    public Color getSurface() { return c(colors.surface()); }
    public Color getOnSurface() { return c(colors.onSurface()); }
    public Color getSurfaceContainer() { return c(colors.surfaceContainer()); }
    public Color getSurfaceVariant() { return c(colors.surfaceVariant()); }
    public Color getOnSurfaceVariant() { return c(colors.onSurfaceVariant()); }
    public Color getSurfaceContainerHigh() { return c(colors.surfaceContainerHigh()); }
    public Color getSurfaceContainerHighest() { return c(colors.surfaceContainerHighest()); }
    public Color getSurfaceContainerLow() { return c(colors.surfaceContainerLow()); }
    public Color getSurfaceContainerLowest() { return c(colors.surfaceContainerLowest()); }
    public Color getSurfaceTint() { return c(colors.surfaceTint()); }
    public Color getOutline() { return c(colors.outline()); }
    public Color getOutlineVariant() { return c(colors.outlineVariant()); }

    private static Color argbToColor(int argb) {
        return new Color(ColorUtils.redFromArgb(argb), ColorUtils.greenFromArgb(argb), ColorUtils.blueFromArgb(argb));
    }

    public boolean isDarkMode() {
        return dark;
    }

    public Hct getHct() {
        return hct;
    }

    /** 供面板背景使用的半透明表面色 */
    public Color getSurfaceAlpha(int alpha) {
        Color s = getSurface();
        return new Color(s.getRed(), s.getGreen(), s.getBlue(), alpha);
    }
}
