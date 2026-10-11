package com.soarclient.gui.hud;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;

import com.soarclient.Soar;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.compat.McCompat;
import com.soarclient.gui.api.Layout;
import com.soarclient.gui.api.SimpleSoarGui;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.utils.ColorUtils;
import com.soarclient.utils.mouse.MouseUtils;

import io.github.humbleui.types.Rect;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.utils.other.Snapper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

/**
 * Material You 风格的 HUD 编辑器（替代 Meteor 原生 HudEditorScreen）。
 *
 * <p>原生编辑器基于 Meteor 自研 GUI，其自定义字体图集只打包 ASCII/拉丁/希腊/西里尔，
 * 不含 CJK —— 中文一律回退成空格，表现为「按钮边框在、文字一片空白」。
 * 本编辑器改用与主界面同款的 Skia 渲染（支持中文），交互完整保留：
 * 拖拽、点选、框选、吸附、显示/隐藏、删除、右键添加。
 *
 * <p>坐标系：HUD 元素坐标是<b>帧缓冲像素</b>；Skia 画布是「GUI 缩放坐标」，
 * 两者相差 canvasScale() 倍。鼠标进入 HUD 坐标系前先乘，画 HUD 矩形时再除回。
 */
public class SoarHudEditor extends SimpleSoarGui {

    private static final float PAD = 8f;
    private static final float RADIUS = 16f;
    private static final float BAR_H = 40f;

    private final Hud hud = Hud.get();
    private final Snapper snapper = new Snapper(new EditorContainer());

    private final SimpleAnimation fadeIn = new SimpleAnimation();

    private boolean dragging, selecting, pressed, moved;
    private int clickX, clickY, lastX, lastY;
    private final List<HudElement> selection = new ArrayList<>();
    private int addX, addY;

    private float doneX, doneY, doneW, doneH = -1;
    @SuppressWarnings("unchecked")
    private final Consumer<Void>[] actionHandlers = new Consumer[3];
    private final float[][] actionBounds = new float[3][4];
    private boolean bottomBarVisible;

    private float panelX, panelY, panelW, panelH;

    public SoarHudEditor() {
        super(true);
    }

    @Override
    public void init() {
        panelX = Layout.MARGIN_X;
        panelY = Layout.MARGIN_Y;
        panelW = McCompat.scaledWidth() - Layout.MARGIN_X * 2;
        panelH = McCompat.scaledHeight() - Layout.MARGIN_Y * 2;
        fadeIn.onTick(1, 10);
    }

    @Override
    public void onResize() {
        init();
    }

    /**
     * 在 Skia 遮罩之前把 HUD 元素画出来（MC 渲染栈）。
     * 与原版 HudEditorScreen.renderElements 相同：编辑器打开时全部元素都画，
     * 隐藏的元素也要画（否则看不见自己在编辑什么）。
     */
    @Override
    public void onRenderBeforeOverlay(net.minecraft.client.gui.DrawContext context,
                                      double mouseX, double mouseY, float delta) {
        meteordevelopment.meteorclient.systems.hud.screens.HudEditorScreen.renderElements(context);
    }

    private static float scale() {
        return SimpleSoarGui.canvasScale();
    }

    // ---------------- 渲染 ----------------

    @Override
    public void draw(double mouseX, double mouseY) {
        ColorPalette pal = Soar.getInstance().getColorManager().getPalette();
        float sw = McCompat.scaledWidth();
        float sh = McCompat.scaledHeight();

        Skia.save();
        Skia.setAlpha((int) (fadeIn.getValue() * 255));

        Skia.drawRoundedRect(0, 0, sw, sh, 0, ColorUtils.applyAlpha(pal.getSurface(), 50));

        Skia.drawRoundedRect(panelX, panelY, panelW, BAR_H, RADIUS, pal.getSurfaceContainerHigh());
        Skia.drawText("HUD 编辑器", panelX + 16f, panelY + 11f, pal.getOnSurface(), Fonts.getMedium(17));

        String hint;
        if (dragging) hint = "拖拽中 · 靠近其它元素自动吸附";
        else if (selecting) hint = "框选中";
        else hint = "拖动移动 · 框选多选 · 单击切换显示 · 右键更多";
        Skia.drawText(hint, panelX + 118f, panelY + 13f, pal.getOnSurfaceVariant(), Fonts.getRegular(13));

        String done = "完成";
        Rect db = Skia.getTextBounds(done, Fonts.getMedium(15));
        doneW = db.getWidth() + 28f;
        doneX = panelX + panelW - doneW - 8f;
        doneY = panelY + 4f;
        doneH = BAR_H - 8f;
        boolean hoverDone = MouseUtils.isInside(mouseX, mouseY, doneX, doneY, doneW, doneH);
        Skia.drawRoundedRect(doneX, doneY, doneW, doneH, 12,
                hoverDone ? pal.getPrimary() : ColorUtils.applyAlpha(pal.getPrimary(), 160));
        Skia.drawFullCenteredText(done, doneX + doneW / 2, doneY + doneH / 2, pal.getOnPrimary(), Fonts.getMedium(15));

        if (selecting) {
            int x1 = Math.min(clickX, lastX);
            int y1 = Math.min(clickY, lastY);
            int x2 = Math.max(clickX, lastX);
            int y2 = Math.max(clickY, lastY);
            drawHudRect(x1, y1, x2 - x1, y2 - y1,
                    ColorUtils.applyAlpha(pal.getPrimary(), 26),
                    ColorUtils.applyAlpha(pal.getPrimary(), 150));
        }

        if (!dragging && !selecting) {
            HudElement hov = hovered(mouseX, mouseY);
            if (hov != null && !selection.contains(hov)) {
                drawHudRect(hov.getX() - (int) PAD, hov.getY() - (int) PAD,
                        hov.getWidth() + (int) PAD * 2, hov.getHeight() + (int) PAD * 2,
                        ColorUtils.applyAlpha(pal.getPrimary(), 22),
                        ColorUtils.applyAlpha(pal.getPrimary(), 120));
            }
        }

        for (HudElement e : selection) {
            boolean on = e.isActive();
            Color col = on ? pal.getPrimary() : pal.getError();
            drawHudRect(e.getX() - (int) PAD, e.getY() - (int) PAD,
                    e.getWidth() + (int) PAD * 2, e.getHeight() + (int) PAD * 2,
                    ColorUtils.applyAlpha(col, on ? 20 : 36),
                    ColorUtils.applyAlpha(col, on ? 170 : 220));
        }

        bottomBarVisible = !selection.isEmpty();
        if (bottomBarVisible) {
            float barY = panelY + panelH - 46f;
            Skia.drawRoundedRect(panelX, barY, panelW, 38f, RADIUS, pal.getSurfaceContainerHigh());

            int activeCount = 0;
            for (HudElement e : selection) if (e.isActive()) activeCount++;

            Skia.drawText("已选中 " + selection.size() + " 个 · " + activeCount + " 个显示中",
                    panelX + 16f, barY + 12f, pal.getOnSurface(), Fonts.getMedium(14));

            float bx = panelX + panelW - 12f;
            final int ac = activeCount;
            actionHandlers[0] = v -> {
                for (HudElement e : selection) e.remove();
                selection.clear();
            };
            actionHandlers[1] = v -> {
                for (HudElement e : new ArrayList<>(selection)) {
                    if (ac > 0 ? e.isActive() : !e.isActive()) e.toggle();
                }
            };
            actionHandlers[2] = v -> {
                if (selection.size() == 1) openSettings(selection.get(0));
            };
            bx = actionButton(bx, barY, "删除", pal.getError(), 0, mouseX, mouseY);
            bx = actionButton(bx, barY, ac > 0 ? "全部隐藏" : "全部显示", pal.getOnSurface(), 1, mouseX, mouseY);
            actionButton(bx, barY, "设置", pal.getOnSurface(), 2, mouseX, mouseY);
        }

        // setAlpha 内部是 saveLayer：必须 restore 两次（先弹 saveLayer 再弹 save()），
        // 少弹一次会让 saveLayer 残留在 canvas 栈上逐帧累积，导致整帧白屏。
        Skia.restore();
        Skia.restore();
    }

    private void drawHudRect(int hx, int hy, int hw, int hh, Color fill, Color outline) {
        float s = scale();
        Skia.drawRoundedRect(hx / s, hy / s, hw / s, hh / s, 8f, fill);
        Skia.drawOutline(hx / s, hy / s, hw / s, hh / s, 8f, 1.5f, outline);
    }

    private float actionButton(float rightX, float barY, String label, Color color, int idx, double mx, double my) {
        Rect b = Skia.getTextBounds(label, Fonts.getMedium(14));
        float w = b.getWidth() + 24f;
        float x = rightX - w;
        float y = barY + 5f;
        float h = 28f;

        boolean hover = MouseUtils.isInside(mx, my, x, y, w, h);
        Skia.drawRoundedRect(x, y, w, h, 10,
                hover ? ColorUtils.applyAlpha(color, 40) : ColorUtils.applyAlpha(color, 18));
        Skia.drawFullCenteredText(label, x + w / 2, y + h / 2, color, Fonts.getMedium(14));

        actionBounds[idx][0] = x;
        actionBounds[idx][1] = y;
        actionBounds[idx][2] = w;
        actionBounds[idx][3] = h;
        return x - 8f;
    }

    private HudElement hovered(double mouseX, double mouseY) {
        float s = scale();
        int hx = (int) (mouseX * s);
        int hy = (int) (mouseY * s);

        for (HudElement e : hud) {
            if (hx >= e.getX() && hx <= e.getX() + e.getWidth()
                    && hy >= e.getY() && hy <= e.getY() + e.getHeight()) return e;
        }
        return null;
    }

    // ---------------- 交互 ----------------

    @Override
    public void mousePressed(double mouseX, double mouseY, int button) {
        float s = scale();
        int hx = (int) (mouseX * s);
        int hy = (int) (mouseY * s);

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (MouseUtils.isInside(mouseX, mouseY, doneX, doneY, doneW, doneH)) {
                close();
                return;
            }

            if (bottomBarVisible) {
                for (int i = 0; i < 3; i++) {
                    if (MouseUtils.isInside(mouseX, mouseY,
                            actionBounds[i][0], actionBounds[i][1], actionBounds[i][2], actionBounds[i][3])) {
                        Consumer<Void> h = actionHandlers[i];
                        if (h != null) h.accept(null);
                        return;
                    }
                }
            }

            pressed = true;
            moved = false;
            clickX = lastX = hx;
            clickY = lastY = hy;

            HudElement hov = hovered(mouseX, mouseY);
            dragging = hov != null;
            if (dragging) {
                if (!selection.contains(hov)) {
                    selection.clear();
                    selection.add(hov);
                }
            } else {
                selection.clear();
            }
            selecting = !dragging;
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            addX = hx;
            addY = hy;
            HudElement hov = hovered(mouseX, mouseY);
            if (hov != null) openSettings(hov);
            else openAddMenu();
        }
    }

    @Override
    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!pressed) return;
        float s = scale();
        int hx = (int) (mouseX * s);
        int hy = (int) (mouseY * s);

        if (Math.abs(hx - clickX) > 2 || Math.abs(hy - clickY) > 2) moved = true;

        if (dragging && !selection.isEmpty() && (hx != lastX || hy != lastY)) {
            SelectionBox box = new SelectionBox();
            snapper.move(box, hx - lastX, hy - lastY);
        }

        lastX = hx;
        lastY = hy;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;

        if (pressed && !moved) {
            HudElement hov = hovered(mouseX, mouseY);
            if (hov != null) {
                hov.toggle();
                selection.remove(hov);
            } else {
                selection.clear();
            }
        } else if (selecting && moved) {
            fillSelection();
        }

        pressed = false;
        dragging = false;
        selecting = false;
    }

    private void fillSelection() {
        int x1 = Math.min(clickX, lastX);
        int y1 = Math.min(clickY, lastY);
        int x2 = Math.max(clickX, lastX);
        int y2 = Math.max(clickY, lastY);

        for (HudElement e : hud) {
            if ((e.getX() <= x2 && e.getX() + e.getWidth() >= x1)
                    && (e.getY() <= y2 && e.getY() + e.getHeight() >= y1)) {
                if (!selection.contains(e)) selection.add(e);
            }
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            for (HudElement e : selection) e.remove();
            selection.clear();
        }
    }

    // ---------------- 子界面 ----------------

    private void openSettings(HudElement e) {
        MinecraftClient.getInstance().setScreen(new SoarHudElementScreen(e, this::reopen).build());
    }

    private void openAddMenu() {
        MinecraftClient.getInstance().setScreen(new SoarHudAddScreen(addX, addY, this::reopen).build());
    }

    /** 从子界面回来时重建 Screen（SimpleSoarGui 的 Screen 包装是一次性的）。 */
    private void reopen() {
        MinecraftClient.getInstance().setScreen(build());
    }

    private void close() {
        meteordevelopment.meteorclient.systems.Systems.save();
        MinecraftClient.getInstance().setScreen(null);
    }

    // ---------------- 吸附 ----------------

    private class EditorContainer implements Snapper.Container {
        @Override
        public Iterable<Snapper.Element> getElements() {
            return () -> new java.util.Iterator<>() {
                private final java.util.Iterator<HudElement> it = hud.iterator();

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public Snapper.Element next() {
                    return it.next();
                }
            };
        }

        @Override
        public boolean shouldNotSnapTo(Snapper.Element element) {
            return selection.contains(element);
        }

        @Override
        public int getSnappingRange() {
            return hud.snappingRange.get();
        }
    }

    /** 把当前选择包装成一个可拖动整体（与原版编辑器一致）。 */
    private class SelectionBox implements Snapper.Element {
        private int x, y;
        private final int width, height;

        SelectionBox() {
            int x1 = Integer.MAX_VALUE, y1 = Integer.MAX_VALUE;
            int x2 = Integer.MIN_VALUE, y2 = Integer.MIN_VALUE;
            for (HudElement e : selection) {
                x1 = Math.min(x1, e.getX());
                y1 = Math.min(y1, e.getY());
                x2 = Math.max(x2, e.getX() + e.getWidth());
                y2 = Math.max(y2, e.getY() + e.getHeight());
            }
            this.x = x1;
            this.y = y1;
            this.width = x2 - x1;
            this.height = y2 - y1;
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public void setPos(int x, int y) {
            move(x - this.x, y - this.y);
        }

        @Override
        public void move(int dx, int dy) {
            int border = hud.border.get();
            int maxX = McCompat.framebufferWidth() - width - border;
            int maxY = McCompat.framebufferHeight() - height - border;
            int nx = MathHelper.clamp(x + dx, border, Math.max(border, maxX));
            int ny = MathHelper.clamp(y + dy, border, Math.max(border, maxY));
            int ax = nx - x;
            int ay = ny - y;
            x = nx;
            y = ny;
            if (ax != 0 || ay != 0) {
                for (HudElement e : selection) e.move(ax, ay);
            }
        }
    }
}
