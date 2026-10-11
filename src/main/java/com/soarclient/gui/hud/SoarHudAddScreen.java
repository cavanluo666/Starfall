package com.soarclient.gui.hud;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.soarclient.Soar;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.compat.McCompat;
import com.soarclient.gui.api.SimpleSoarGui;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;
import com.soarclient.utils.ColorUtils;
import com.soarclient.utils.language.I18n;
import com.soarclient.utils.mouse.MouseUtils;

import io.github.humbleui.types.Rect;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import net.minecraft.client.MinecraftClient;

/**
 * 添加 HUD 元素的选择列表（Material You 风格）。
 *
 * <p>按分组展示全部可添加的元素（含预设），标题与描述走汉化表；
 * 点击即添加到右键时的位置（帧缓冲像素坐标）。
 */
public class SoarHudAddScreen extends SimpleSoarGui {

    private static final float RADIUS = 20f;
    private static final float ROW_H = 40f;

    private final int addX, addY;
    private final Runnable onClose;
    private final SimpleAnimation fadeIn = new SimpleAnimation();

    private record Item(String title, String desc, Object payload) {}

    private final List<Row> rows = new ArrayList<>();
    private String filter = "";

    private static final class Row {
        final Item item;
        final float x, y, w, h;

        Row(Item item, float x, float y, float w, float h) {
            this.item = item;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    private float panelX, panelY, panelW, panelH;

    public SoarHudAddScreen(int addX, int addY, Runnable onClose) {
        super(true);
        this.addX = addX;
        this.addY = addY;
        this.onClose = onClose;
    }

    @Override
    public void init() {
        float sw = McCompat.scaledWidth();
        float sh = McCompat.scaledHeight();
        panelW = Math.min(400f, sw - 40f);
        panelH = Math.min(440f, sh - 60f);
        panelX = (sw - panelW) / 2f;
        panelY = (sh - panelH) / 2f;
        fadeIn.onTick(1, 8);
    }

    @Override
    public void onResize() {
        init();
    }

    private String zh(String s) {
        return I18n.has(s) ? I18n.get(s) : s;
    }

    private List<Map.Entry<HudGroup, List<Item>>> collect() {
        Map<HudGroup, List<Item>> grouped = new HashMap<>();

        for (HudElementInfo<?> info : Hud.get().infos.values()) {
            String t = zh(info.title);
            String d = info.description == null ? "" : zh(info.description);

            if (info.hasPresets()) {
                for (HudElementInfo<?>.Preset p : info.presets) {
                    String pt = t + " · " + zh(p.title);
                    if (match(pt)) grouped.computeIfAbsent(info.group, g -> new ArrayList<>())
                            .add(new Item(pt, d, p));
                }
            } else if (match(t)) {
                grouped.computeIfAbsent(info.group, g -> new ArrayList<>()).add(new Item(t, d, info));
            }
        }

        List<Map.Entry<HudGroup, List<Item>>> out = new ArrayList<>(grouped.entrySet());
        out.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getKey().title(), b.getKey().title()));
        return out;
    }

    private boolean match(String title) {
        return filter.isEmpty() || title.toLowerCase().contains(filter.toLowerCase());
    }

    @Override
    public void draw(double mouseX, double mouseY) {
        ColorPalette pal = Soar.getInstance().getColorManager().getPalette();
        float sw = McCompat.scaledWidth();
        float sh = McCompat.scaledHeight();

        Skia.save();
        Skia.setAlpha((int) (fadeIn.getValue() * 255));
        Skia.drawRoundedRect(0, 0, sw, sh, 0, ColorUtils.applyAlpha(pal.getSurface(), 120));

        Skia.drawRoundedRect(panelX, panelY, panelW, panelH, RADIUS, pal.getSurfaceContainer());

        float pad = 18f;
        float y = panelY + 16f;
        Skia.drawText("添加 HUD 元素", panelX + pad, y, pal.getOnSurface(), Fonts.getMedium(19));
        y += 30f;

        rows.clear();

        float listH = panelY + panelH - 52f - y;
        float scrollY = 0f; // 简化：不做滚动，元素多时通过过滤搜索

        for (Map.Entry<HudGroup, List<Item>> e : collect()) {
            if (y > panelY + panelH - 46f) break;

            Skia.drawText(zh(e.getKey().title()), panelX + pad, y, pal.getPrimary(), Fonts.getMedium(13));
            y += 20f;

            for (Item it : e.getValue()) {
                if (y + ROW_H > panelY + panelH - 40f) break;

                boolean hover = MouseUtils.isInside(mouseX, mouseY, panelX + pad, y, panelW - pad * 2, ROW_H);
                if (hover) {
                    Skia.drawRoundedRect(panelX + pad, y, panelW - pad * 2, ROW_H, 12,
                            ColorUtils.applyAlpha(pal.getOnSurfaceVariant(), 20));
                }

                Skia.drawText(it.title(), panelX + pad + 10f, y + 8f, pal.getOnSurface(), Fonts.getMedium(15));
                if (!it.desc().isEmpty()) {
                    String d = it.desc();
                    Rect db = Skia.getTextBounds(d, Fonts.getRegular(12));
                    if (db.getWidth() > panelW - pad * 2 - 40f) {
                        while (d.length() > 3 && Skia.getTextBounds(d + "…", Fonts.getRegular(12)).getWidth() > panelW - pad * 2 - 40f) {
                            d = d.substring(0, d.length() - 1);
                        }
                        d = d + "…";
                    }
                    Skia.drawText(d, panelX + pad + 10f, y + 24f, pal.getOnSurfaceVariant(), Fonts.getRegular(12));
                }

                rows.add(new Row(it, panelX + pad, y, panelW - pad * 2, ROW_H));
                y += ROW_H + 2f;
            }

            y += 6f;
        }

        // 关闭按钮
        String close = "关闭";
        Rect cb = Skia.getTextBounds(close, Fonts.getMedium(15));
        float cw = cb.getWidth() + 28f;
        float cx = panelX + panelW - cw - 14f;
        float cy = panelY + panelH - 40f;
        boolean hoverClose = MouseUtils.isInside(mouseX, mouseY, cx, cy, cw, 28f);
        Skia.drawRoundedRect(cx, cy, cw, 28f, 10,
                hoverClose ? pal.getPrimary() : ColorUtils.applyAlpha(pal.getPrimary(), 150));
        Skia.drawFullCenteredText(close, cx + cw / 2, cy + 14f, pal.getOnPrimary(), Fonts.getMedium(15));
        closeButton[0] = cx;
        closeButton[1] = cy;
        closeButton[2] = cw;
        closeButton[3] = 28f;

        // setAlpha 内部是 saveLayer：必须 restore 两次（先弹 saveLayer 再弹 save()），
        // 少弹一次会让 saveLayer 残留在 canvas 栈上逐帧累积，导致整帧白屏。
        Skia.restore();
        Skia.restore();
    }

    private final float[] closeButton = new float[4];

    @Override
    public void mousePressed(double mouseX, double mouseY, int button) {
        if (button != 0) return;

        if (MouseUtils.isInside(mouseX, mouseY, closeButton[0], closeButton[1], closeButton[2], closeButton[3])) {
            back();
            return;
        }

        for (Row r : rows) {
            if (MouseUtils.isInside(mouseX, mouseY, r.x, r.y, r.w, r.h)) {
                Object p = r.item.payload();
                if (p instanceof HudElementInfo<?>.Preset preset) {
                    Hud.get().add(preset, addX, addY);
                } else if (p instanceof HudElementInfo<?> info) {
                    Hud.get().add(info, addX, addY);
                }
                meteordevelopment.meteorclient.systems.Systems.save();
                back();
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
