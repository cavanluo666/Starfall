package com.soarclient.ui.component.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.soarclient.Soar;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.utils.ColorUtils;

import io.github.humbleui.types.Rect;

/**
 * 悬停提示：鼠标停在控件上时弹出说明气泡。
 *
 * <p>统一在界面的最上层绘制（最后调用 {@link #draw}），保证不被卡片遮挡。
 * 带淡入动画与自动翻转（靠近屏幕边缘时移到另一侧），避免超出屏幕。
 */
public class Tooltip {

    /** 气泡内边距 */
    private static final float PAD_X = 10f;
    private static final float PAD_Y = 7f;
    /** 鼠标与气泡的间距 */
    private static final float OFFSET = 12f;
    /** 最大宽度，超出则换行 */
    private static final float MAX_WIDTH = 220f;
    /** 行高 */
    private static final float LINE_H = 15f;

    private final SimpleAnimation fade = new SimpleAnimation();

    private String title;
    private String body;
    private double mouseX, mouseY;
    private boolean visible;

    /** 设置当前要显示的提示。每帧调用；不显示时传 null。 */
    public void set(String title, String body, double mouseX, double mouseY) {
        this.title = title;
        this.body = body;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.visible = title != null || body != null;
    }

    public void hide() {
        this.visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    /** 在最上层绘制。应在所有其它内容之后调用。 */
    public void draw(float panelX, float panelY, float panelW, float panelH,
                     float screenW, float screenH) {

        fade.onTick(visible ? 1f : 0f, 18);
        float a = fade.getValue();
        if (a < 0.01f) return;

        ColorPalette palette = Soar.getInstance().getColorManager().getPalette();

        // ---- 文本换行 ----
        List<String> lines = new ArrayList<>();
        if (title != null && !title.isEmpty()) {
            lines.add(title);
        }
        if (body != null && !body.isEmpty()) {
            lines.addAll(wrap(body, MAX_WIDTH - PAD_X * 2));
        }
        if (lines.isEmpty()) return;

        // ---- 计算尺寸 ----
        float textW = 0;
        for (String l : lines) {
            textW = Math.max(textW, textWidth(l, lines.indexOf(l) == 0 && title != null));
        }
        float w = textW + PAD_X * 2;
        float h = lines.size() * LINE_H + PAD_Y * 2;

        // ---- 定位（自动翻转，避免出屏）----
        float bx = (float) mouseX + OFFSET;
        float by = (float) mouseY + OFFSET;

        if (bx + w > screenW - 4) bx = (float) mouseX - w - OFFSET;
        if (by + h > screenH - 4) by = (float) mouseY - h - OFFSET;

        // 限制在屏幕内
        bx = Math.max(4, Math.min(bx, screenW - w - 4));
        by = Math.max(4, Math.min(by, screenH - h - 4));

        // ---- 背景 + 描边 ----
        float radius = 10f;
        Color bg = ColorUtils.applyAlpha(palette.getSurfaceContainerHighest(), a * 0.97f);
        Skia.drawRoundedRect(bx, by, w, h, radius, bg);
        Skia.drawOutline(bx, by, w, h, radius, 1f,
                ColorUtils.applyAlpha(palette.getOutline(), a * 0.5f));

        // ---- 文字 ----
        float ty = by + PAD_Y;
        for (int i = 0; i < lines.size(); i++) {
            boolean isTitle = title != null && !title.isEmpty() && i == 0;
            Color c = isTitle
                    ? ColorUtils.applyAlpha(palette.getPrimary(), a)
                    : ColorUtils.applyAlpha(palette.getOnSurfaceVariant(), a * 0.95f);
            Skia.drawText(lines.get(i), bx + PAD_X, ty,
                    c, isTitle ? Fonts.getMedium(15) : Fonts.getRegular(13));
            ty += LINE_H;
        }
    }

    // ---------------- 辅助 ----------------

    private static float textWidth(String s, boolean bold) {
        Rect r = Skia.getTextBounds(s, bold ? Fonts.getMedium(15) : Fonts.getRegular(13));
        return r == null ? s.length() * 7f : r.getWidth();
    }

    /** 按宽度把中文/英文混排文本切成多行。 */
    private static List<String> wrap(String text, float maxWidth) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;

        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\n') {
                out.add(cur.toString());
                cur.setLength(0);
                continue;
            }

            cur.append(ch);

            if (textWidth(cur.toString(), false) > maxWidth && cur.length() > 1) {
                // 回退一个字符，避免把行尾的字挤掉
                cur.setLength(cur.length() - 1);
                out.add(cur.toString());
                cur.setLength(0);
                cur.append(ch);
            }
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }
}
