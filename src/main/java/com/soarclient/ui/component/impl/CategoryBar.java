package com.soarclient.ui.component.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.soarclient.Soar;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.utils.ColorUtils;
import com.soarclient.utils.mouse.MouseUtils;

import io.github.humbleui.types.Rect;

/**
 * 分类筛选栏：一排可点击的标签，用于按分类过滤列表内容。
 *
 * <p>标签按可用宽度自动换行；选中项用主色胶囊高亮，并有平滑过渡。
 * 适合触摸板 —— 点击区域比文字本身宽裕。
 */
public class CategoryBar {

    /** 单个标签的内边距 */
    private static final float PAD_X = 12f;
    private static final float PAD_Y = 5f;
    private static final float GAP = 6f;
    /** 命中判定的额外放宽（便于触摸板点选） */
    private static final float HIT_EXTRA = 4f;

    private final List<Tab> tabs = new ArrayList<>();

    private String selected;
    private Consumer<String> onChange;

    private float x, y, width;
    /** 每次布局后得到的总高度 */
    private float height;

    public CategoryBar() {
    }

    /** 设置可选项。第一项通常是"全部"。 */
    public void setTabs(List<String> labels, String selected) {
        tabs.clear();
        for (String l : labels) {
            tabs.add(new Tab(l));
        }
        this.selected = selected;
    }

    /** 当前标签数量（诊断用）。 */
    public int getTabCount() {
        return tabs.size();
    }

    public void setSelected(String s) {
        this.selected = s;
    }

    public String getSelected() {
        return selected;
    }

    public void setOnChange(Consumer<String> c) {
        this.onChange = c;
    }

    public void setBounds(float x, float y, float width) {
        this.x = x;
        this.y = y;
        this.width = width;
        layout();
    }

    public float getHeight() {
        return height;
    }

    /** 计算每个标签的位置（自动换行）。 */
    private void layout() {
        float cx = x;
        float cy = y;
        float lineH = 0;

        for (Tab t : tabs) {
            float tw = textWidth(t.label) + PAD_X * 2;
            float th = textHeight() + PAD_Y * 2;

            if (cx > x && cx + tw > x + width) {
                cx = x;
                cy += lineH + GAP;
                lineH = 0;
            }

            t.x = cx;
            t.y = cy;
            t.w = tw;
            t.h = th;
            t.animation.setFirstTick(t.animation.getValue() == 0);

            cx += tw + GAP;
            lineH = Math.max(lineH, th);
        }

        height = (cy - y) + lineH;

        if (diagLeft > 0) {
            diagLeft--;
            StringBuilder sb = new StringBuilder();
            for (Tab t : tabs) {
                sb.append(t.label).append("(").append((int) t.x).append(",")
                  .append((int) t.y).append(",").append((int) t.w).append(") ");
            }
            meteordevelopment.meteorclient.MeteorClient.LOG.info(
                "[CatBar] x={} y={} w={} tabs={} height={} -> {}",
                x, y, width, tabs.size(), height, sb);
        }
    }

    private int diagLeft = 3;

    private static float textWidth(String s) {
        try {
            Rect b = Skia.getTextBounds(s, Fonts.getMedium(14));
            if (b != null && b.getWidth() > 0) return b.getWidth();
        } catch (Throwable ignored) {
        }
        // 兜底估算：中文字符按字号算，其它按一半
        float w = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            w += (ch > 0x2E80) ? 14f : 7f;
        }
        return w;
    }

    private static float textHeight() {
        return 14f;
    }

    /** 是否命中某个标签，返回其索引；未命中返回 -1。 */
    private int hit(double mx, double my) {
        for (int i = 0; i < tabs.size(); i++) {
            Tab t = tabs.get(i);
            if (MouseUtils.isInside(mx, my,
                    t.x - HIT_EXTRA, t.y - HIT_EXTRA,
                    t.w + HIT_EXTRA * 2, t.h + HIT_EXTRA * 2)) {
                return i;
            }
        }
        return -1;
    }

    public void mousePressed(double mx, double my, int button) {
        if (button != org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        int i = hit(mx, my);
        if (i >= 0) tabs.get(i).pressed = true;
    }

    public void mouseReleased(double mx, double my, int button) {
        if (button != org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        int i = hit(mx, my);
        for (int k = 0; k < tabs.size(); k++) tabs.get(k).pressed = false;

        if (i >= 0) {
            String label = tabs.get(i).label;
            if (!label.equals(selected)) {
                selected = label;
                if (onChange != null) onChange.accept(label);
            }
        }
    }

    public void draw(double mx, double my) {
        if (tabs.isEmpty()) return;

        ColorPalette palette = Soar.getInstance().getColorManager().getPalette();

        for (Tab t : tabs) {
            boolean active = t.label.equals(selected);
            boolean hover = hit(mx, my) == tabs.indexOf(t);

            t.animation.onTick(active ? 1f : 0f, 12);
            float a = t.animation.getValue();

            float radius = t.h / 2f;

            // 未选中的底色
            if (a < 0.99f) {
                Skia.drawRoundedRect(t.x, t.y, t.w, t.h, radius,
                        ColorUtils.applyAlpha(palette.getSurfaceContainerHigh(),
                                (hover ? 0.85f : 0.55f) * (1f - a)));
            }

            // 选中的主色胶囊
            if (a > 0.01f) {
                Skia.drawRoundedRect(t.x, t.y, t.w, t.h, radius,
                        ColorUtils.applyAlpha(palette.getPrimary(), a));
            }

            // 文字
            Color fg = active
                    ? palette.getOnPrimary()
                    : (hover ? palette.getOnSurface() : palette.getOnSurfaceVariant());

            Skia.drawFullCenteredText(t.label, t.x + t.w / 2f, t.y + t.h / 2f, fg, Fonts.getMedium(14));
        }
    }

    private static class Tab {
        final String label;
        float x, y, w, h;
        boolean pressed;
        final SimpleAnimation animation = new SimpleAnimation();

        Tab(String label) {
            this.label = label;
        }
    }
}
