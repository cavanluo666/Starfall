package com.soarclient.ui.component.impl;

import java.awt.Color;

import com.soarclient.Soar;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.skia.Skia;
import com.soarclient.utils.ColorUtils;
import com.soarclient.utils.mouse.ScrollHelper;
import com.soarclient.utils.mouse.MouseUtils;

/**
 * 可拖动的滚动条。
 *
 * <p>触摸板 / 无滚轮设备上，仅靠滚轮无法浏览长列表，因此提供可抓取的滑块。
 * 宽度刻意做得较宽（默认 8），命中判定额外放宽到 20，方便抓取。
 */
public class ScrollBar {

    /** 轨道右边缘距面板右侧的间距 */
    public static final float MARGIN = 6f;
    /** 滑块宽度 */
    public static final float WIDTH = 8f;
    /** 命中判定宽度（比视觉更宽，便于抓取） */
    public static final float HIT_WIDTH = 22f;

    private final SimpleAnimation hoverAnimation = new SimpleAnimation();

    private float trackX, trackY, trackWidth, trackHeight;

    public void setBounds(float x, float y, float width, float height) {
        this.trackX = x;
        this.trackY = y;
        this.trackWidth = width;
        this.trackHeight = height;
    }

    public float getTrackX() { return trackX; }
    public float getTrackY() { return trackY; }
    public float getTrackWidth() { return trackWidth; }
    public float getTrackHeight() { return trackHeight; }

    public float getThumbLength(ScrollHelper h) {
        return trackHeight * h.getThumbRatio();
    }

    public float getThumbTop(ScrollHelper h) {
        float range = trackHeight - getThumbLength(h);
        return trackY + range * h.getProgress();
    }

    /** 鼠标是否落在滑块上。 */
    public boolean isOnThumb(ScrollHelper h, double mx, double my) {
        if (!h.isScrollable()) return false;
        float top = getThumbTop(h);
        float len = getThumbLength(h);
        return mx > trackX - (HIT_WIDTH - trackWidth) / 2f
                && mx < trackX + trackWidth + (HIT_WIDTH - trackWidth) / 2f
                && my > top && my < top + len;
    }

    /** 鼠标是否落在轨道上（用于点击跳转）。 */
    public boolean isOnTrack(double mx, double my) {
        return mx > trackX - (HIT_WIDTH - trackWidth) / 2f
                && mx < trackX + trackWidth + (HIT_WIDTH - trackWidth) / 2f
                && my > trackY && my < trackY + trackHeight;
    }

    public void draw(ScrollHelper h, double mouseX, double mouseY) {

        if (!h.isScrollable()) return;

        ColorPalette palette = Soar.getInstance().getColorManager().getPalette();

        boolean hovered = isOnThumb(h, mouseX, mouseY) || isOnTrack(mouseX, mouseY) || h.isDragging();
        hoverAnimation.onTick(hovered ? 1f : 0f, 12);
        float a = hoverAnimation.getValue();

        float radius = trackWidth / 2f;

        // 轨道：始终淡淡的显示，提示“这里可以滚”
        Skia.drawRoundedRect(trackX, trackY, trackWidth, trackHeight, radius,
                ColorUtils.applyAlpha(palette.getOnSurfaceVariant(), 0.12f + a * 0.08f));

        // 滑块
        float thumbTop = getThumbTop(h);
        float thumbLen = getThumbLength(h);
        Color thumbColor = h.isDragging()
                ? palette.getPrimary()
                : ColorUtils.applyAlpha(palette.getOnSurfaceVariant(), 0.45f + a * 0.35f);

        Skia.drawRoundedRect(trackX, thumbTop, trackWidth, thumbLen, radius, thumbColor);
    }
}
