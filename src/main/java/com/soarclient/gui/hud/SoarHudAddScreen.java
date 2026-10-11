package com.soarclient.gui.hud;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.soarclient.Soar;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.compat.McCompat;
import com.soarclient.gui.api.SimpleSoarGui;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.ui.component.impl.ScrollBar;
import com.soarclient.utils.ColorUtils;
import com.soarclient.utils.language.I18n;
import com.soarclient.utils.mouse.MouseUtils;
import com.soarclient.utils.mouse.ScrollHelper;

import io.github.humbleui.types.Rect;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/**
 * 添加 HUD 元素的选择列表（Material You 风格）。
 *
 * <p>按分组展示全部可添加的元素（含预设），标题与描述走汉化表；
 * 点击即添加到右键时的位置（帧缓冲像素坐标）。
 *
 * <p>元素总数 19+，而游戏内面板高度只有 200px 左右（GUI 缩放坐标），
 * 不滚动时绝大多数条目不可见 —— 这就是「没法添加元素」的原因。
 * 现支持滚轮 + 可拖动滚动条（与主界面同款交互）。
 */
public class SoarHudAddScreen extends SimpleSoarGui {

    private static final float RADIUS = 20f;
    private static final float ROW_H = 40f;
    private static final float GROUP_H = 22f;

    private final int addX, addY;
    private final Runnable onClose;
    private final SimpleAnimation fadeIn = new SimpleAnimation();

    private record Item(String title, String desc, Object payload) {}

    /** 可点击条目（屏幕坐标，已含滚动偏移） */
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

    private final List<Row> rows = new ArrayList<>();
    private float contentBottom;

    private final ScrollHelper scroll = new ScrollHelper();
    private final ScrollBar scrollBar = new ScrollBar();

    private float panelX, panelY, panelW, panelH;
    private float listTop, listBottom;
    private float closeX, closeY, closeW, closeH = -1;

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
        panelW = Math.min(400f, sw - 24f);
        panelH = Math.min(520f, sh - 24f);
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
                    grouped.computeIfAbsent(info.group, g -> new ArrayList<>())
                            .add(new Item(t + " · " + zh(p.title), d, p));
                }
            } else {
                grouped.computeIfAbsent(info.group, g -> new ArrayList<>()).add(new Item(t, d, info));
            }
        }

        List<Map.Entry<HudGroup, List<Item>>> out = new ArrayList<>(grouped.entrySet());
        out.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getKey().title(), b.getKey().title()));
        return out;
    }

    @Override
    public void draw(double mouseX, double mouseY) {
        ColorPalette pal = Soar.getInstance().getColorManager().getPalette();
        float sw = McCompat.scaledWidth();
        float sh = McCompat.scaledHeight();

        scroll.onUpdate();

        Skia.save();
        Skia.setAlpha((int) (fadeIn.getValue() * 255));
        Skia.drawRoundedRect(0, 0, sw, sh, 0, ColorUtils.applyAlpha(pal.getSurface(), 120));

        Skia.drawRoundedRect(panelX, panelY, panelW, panelH, RADIUS, pal.getSurfaceContainer());

        float pad = 16f;

        // ---- 标题（固定区）----
        float y = panelY + 14f;
        Skia.drawText("添加 HUD 元素", panelX + pad, y, pal.getOnSurface(), Fonts.getMedium(18));
        y += 26f;

        listTop = y;
        listBottom = panelY + panelH - 46f;

        // ---- 列表（滚动区）----
        rows.clear();
        float scrollOffset = scroll.getValue();
        float cy = listTop + scrollOffset;

        Skia.save();
        // 裁剪到列表区，滚动时条目不会画出面板
        Skia.clip(panelX, listTop, panelW, listBottom - listTop, 0);

        for (Map.Entry<HudGroup, List<Item>> e : collect()) {
            // 分组标题
            if (cy + GROUP_H > listTop && cy < listBottom) {
                Skia.drawText(zh(e.getKey().title()), panelX + pad, cy, pal.getPrimary(), Fonts.getMedium(13));
            }
            cy += GROUP_H;

            for (Item it : e.getValue()) {
                if (cy + ROW_H > listTop && cy < listBottom) {
                    float rx = panelX + pad;
                    float rw = panelW - pad * 2 - ScrollBar.WIDTH - 10f;

                    boolean hover = MouseUtils.isInside(mouseX, mouseY, rx, cy, rw, ROW_H);
                    if (hover) {
                        Skia.drawRoundedRect(rx, cy, rw, ROW_H, 12,
                                ColorUtils.applyAlpha(pal.getOnSurfaceVariant(), 20));
                    }

                    Skia.drawText(it.title(), rx + 10f, cy + 8f, pal.getOnSurface(), Fonts.getMedium(15));
                    if (!it.desc().isEmpty()) {
                        String d = it.desc();
                        Rect db = Skia.getTextBounds(d, Fonts.getRegular(12));
                        if (db.getWidth() > rw - 20f) {
                            while (d.length() > 3 && Skia.getTextBounds(d + "…", Fonts.getRegular(12)).getWidth() > rw - 20f) {
                                d = d.substring(0, d.length() - 1);
                            }
                            d = d + "…";
                        }
                        Skia.drawText(d, rx + 10f, cy + 24f, pal.getOnSurfaceVariant(), Fonts.getRegular(12));
                    }

                    rows.add(new Row(it, rx, cy, rw, ROW_H));
                }
                cy += ROW_H + 2f;
            }

            cy += 6f;
        }
        contentBottom = cy - scrollOffset;

        Skia.restore();

        // 滚动范围（内容总高 - 可视高）
        float viewport = listBottom - listTop;
        scroll.setMaxScroll(contentBottom - viewport, viewport);

        // ---- 滚动条（可拖动）----
        scrollBar.setBounds(panelX + panelW - ScrollBar.WIDTH - ScrollBar.MARGIN,
                listTop, ScrollBar.WIDTH, viewport);
        if (scroll.isScrollable()) {
            scrollBar.draw(scroll, mouseX, mouseY);
        }

        // ---- 关闭按钮（固定区）----
        String close = "关闭";
        Rect cb = Skia.getTextBounds(close, Fonts.getMedium(15));
        closeW = cb.getWidth() + 28f;
        closeX = panelX + panelW - closeW - 12f;
        closeY = panelY + panelH - 36f;
        closeH = 28f;
        boolean hoverClose = MouseUtils.isInside(mouseX, mouseY, closeX, closeY, closeW, closeH);
        Skia.drawRoundedRect(closeX, closeY, closeW, closeH, 10,
                hoverClose ? pal.getPrimary() : ColorUtils.applyAlpha(pal.getPrimary(), 150));
        Skia.drawFullCenteredText(close, closeX + closeW / 2, closeY + 14f, pal.getOnPrimary(), Fonts.getMedium(15));

        // setAlpha 内部是 saveLayer：必须 restore 两次（先弹 saveLayer 再弹 save()），
        // 少弹一次会让 saveLayer 残留在 canvas 栈上逐帧累积，导致整帧白屏。
        Skia.restore();
        Skia.restore();
    }

    @Override
    public void mousePressed(double mouseX, double mouseY, int button) {
        // 滚动条优先（与 Page 基类相同的处理顺序）
        if (scroll.isScrollable()
                && (scrollBar.isOnThumb(scroll, mouseX, mouseY) || scrollBar.isOnTrack(mouseX, mouseY))
                && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (scrollBar.isOnThumb(scroll, mouseX, mouseY)) {
                scroll.beginDrag((float) mouseY, scrollBar.getThumbTop(scroll),
                        scrollBar.getTrackY(), scrollBar.getTrackHeight(), scrollBar.getThumbLength(scroll));
            } else {
                scroll.jumpTo((float) mouseY, scrollBar.getTrackY(),
                        scrollBar.getTrackHeight(), scrollBar.getThumbLength(scroll));
            }
            return;
        }

        if (button != 0) return;

        if (MouseUtils.isInside(mouseX, mouseY, closeX, closeY, closeW, closeH)) {
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
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (scroll.isDragging()) scroll.endDrag();
    }

    @Override
    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (scroll.isDragging()) scroll.updateDrag((float) mouseY);
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll.onScroll(verticalAmount);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) back();
    }

    private void back() {
        MinecraftClient.getInstance().setScreen(null);
        if (onClose != null) onClose.run();
    }
}
