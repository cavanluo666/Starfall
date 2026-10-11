package com.soarclient.gui.hud;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.soarclient.Soar;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.compat.McCompat;
import com.soarclient.gui.api.Layout;
import com.soarclient.gui.api.SimpleSoarGui;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;
import com.soarclient.utils.ColorUtils;
import com.soarclient.utils.mouse.MouseUtils;

import io.github.humbleui.types.Rect;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;import meteordevelopment.meteorclient.systems.hud.XAnchor;import meteordevelopment.meteorclient.systems.hud.YAnchor;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.utils.other.Snapper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

/**
 * 单个 HUD 元素的设置界面（Material You 风格）。
 *
 * <p>提供原版编辑器的全部元素操作：显示/隐藏、删除、锚点切换、
 * 以及逐项列出元素自带设置（只读展示 + 常用开关）。
 * 中文渲染走 Skia，无字体缺字问题。
 */
public class SoarHudElementScreen extends SimpleSoarGui {

    private static final float RADIUS = 20f;
    private static final float ROW_H = 44f;

    private final HudElement element;
    private final Runnable onClose;
    private final SimpleAnimation fadeIn = new SimpleAnimation();

    private float panelX, panelY, panelW, panelH;
    private float listBottom;

    /** 可点击行（动态：元素设置项数量不定，固定槽位会越界） */
    private final List<float[]> hotRows = new ArrayList<>();
    private final List<Consumer<Void>> hotHandlers = new ArrayList<>();

    public SoarHudElementScreen(HudElement element, Runnable onClose) {
        super(true);
        this.element = element;
        this.onClose = onClose;
    }

    @Override
    public void init() {
        float sw = McCompat.scaledWidth();
        float sh = McCompat.scaledHeight();
        panelW = Math.min(360f, sw - 40f);
        panelH = Math.min(420f, sh - 60f);
        panelX = (sw - panelW) / 2f;
        panelY = (sh - panelH) / 2f;
        fadeIn.onTick(1, 8);
    }

    @Override
    public void onResize() {
        init();
    }

    @Override
    public void draw(double mouseX, double mouseY) {
        ColorPalette pal = Soar.getInstance().getColorManager().getPalette();
        float sw = McCompat.scaledWidth();
        float sh = McCompat.scaledHeight();

        Skia.save();
        Skia.setAlpha((int) (fadeIn.getValue() * 255));
        Skia.drawRoundedRect(0, 0, sw, sh, 0, ColorUtils.applyAlpha(pal.getSurface(), 120));

        // 卡片
        Skia.drawRoundedRect(panelX, panelY, panelW, panelH, RADIUS, pal.getSurfaceContainer());

        float pad = 18f;
        float y = panelY + 16f;

        // 标题
        String title = com.soarclient.utils.language.I18n.has(element.info.title)
                ? com.soarclient.utils.language.I18n.get(element.info.title) : element.info.title;
        Skia.drawText(title, panelX + pad, y, pal.getOnSurface(), Fonts.getMedium(19));
        y += 26f;

        String desc = element.info.description == null ? "" : element.info.description;
        if (!desc.isEmpty()) {
            Skia.drawText(desc, panelX + pad, y, pal.getOnSurfaceVariant(), Fonts.getRegular(13));
        }
        y += 30f;

        hotRows.clear();
        hotHandlers.clear();

        // 显示开关行
        y = row(pal, y, element.isActive() ? "显示中 · 点击隐藏" : "已隐藏 · 点击显示",
                element.isActive() ? Icon.VISIBILITY : Icon.VISIBILITY_OFF,
                element.isActive() ? pal.getPrimary() : pal.getOnSurfaceVariant(),
                v -> element.toggle());

        // 锚点行
        String anchor = element.box.xAnchor.name() + " / " + element.box.yAnchor.name();
        y = row(pal, y, "锚点 · " + anchor + "（点击改为居中）", Icon.PLACE, pal.getOnSurface(), v -> {
            element.box.xAnchor = XAnchor.Center;
            element.box.yAnchor = YAnchor.Center;
            element.box.updateAnchors();
        });

        // 删除行
        y = row(pal, y, "删除此元素", Icon.DELETE, pal.getError(), v -> {
            element.remove();
            back();
        });

        // 元素自带设置（标题与描述走汉化表）
        var sg = element.settings;
        int groups = 0;
        for (var g : sg) {
            groups++;
            for (meteordevelopment.meteorclient.settings.Setting<?> s : g) {
                if (s == null) continue;
                String v = String.valueOf(s.get());
                y = row(pal, y, s.title + " = " + v, Icon.TUNE, pal.getOnSurfaceVariant(), null);
                if (y > panelY + panelH - 60f) break;
            }
            if (y > panelY + panelH - 60f) break;
        }

        listBottom = y;

        // 返回按钮
        String back = "返回";
        Rect bb = Skia.getTextBounds(back, Fonts.getMedium(15));
        float bw = bb.getWidth() + 28f;
        float bx = panelX + panelW - bw - 14f;
        float by = panelY + panelH - 38f;
        boolean hover = MouseUtils.isInside(mouseX, mouseY, bx, by, bw, 28f);
        Skia.drawRoundedRect(bx, by, bw, 28f, 10,
                hover ? pal.getPrimary() : ColorUtils.applyAlpha(pal.getPrimary(), 150));
        Skia.drawFullCenteredText(back, bx + bw / 2, by + 14f, pal.getOnPrimary(), Fonts.getMedium(15));

        hotRows.add(new float[] { bx, by, bw, 28f });
        hotHandlers.add(v -> back());

        // setAlpha 内部是 saveLayer：必须 restore 两次（先弹 saveLayer 再弹 save()），
        // 少弹一次会让 saveLayer 残留在 canvas 栈上逐帧累积，导致整帧白屏。
        Skia.restore();
        Skia.restore();
    }

    /** 画一行可点击条目，返回下一行 y。 */
    private float row(ColorPalette pal, float y, String text, String icon, Color color, Consumer<Void> onClick) {
        float pad = 14f;
        float x = panelX + pad;
        float w = panelW - pad * 2;

        if (hotHandlers.size() >= 64) return y + ROW_H;

        boolean hover = MouseUtils.isInside(lastMouseX, lastMouseY, x, y, w, ROW_H);
        if (hover) {
            Skia.drawRoundedRect(x, y, w, ROW_H, 12, ColorUtils.applyAlpha(pal.getOnSurfaceVariant(), 18));
        }

        Skia.drawText(icon, x + 10f, y + 12f, color, Fonts.getIcon(20));
        Skia.drawText(text, x + 44f, y + 14f, onClick == null ? pal.getOnSurfaceVariant() : pal.getOnSurface(),
                Fonts.getRegular(15));

        hotRows.add(new float[] { x, y, w, ROW_H });
        hotHandlers.add(onClick);

        return y + ROW_H + 4f;
    }

    private double lastMouseX, lastMouseY;

    @Override
    public void mousePressed(double mouseX, double mouseY, int button) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        if (button != 0) return;

        for (int i = 0; i < hotRows.size(); i++) {
            float[] r = hotRows.get(i);
            if (MouseUtils.isInside(mouseX, mouseY, r[0], r[1], r[2], r[3])) {
                Consumer<Void> h = hotHandlers.get(i);
                if (h != null) h.accept(null);
                return;
            }
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) back();
    }

    private void back() {
        MinecraftClient.getInstance().setScreen(null);
        if (onClose != null) onClose.run();
    }
}
