package com.soarclient.gui.modmenu.component;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.soarclient.Soar;
import com.soarclient.animation.Animation;
import com.soarclient.animation.Duration;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.animation.cubicbezier.impl.EaseStandard;
import com.soarclient.animation.other.DummyAnimation;
import com.soarclient.gui.api.SoarGui;
import com.soarclient.gui.api.page.SimplePage;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;
import com.soarclient.ui.component.Component;
import com.soarclient.utils.ColorUtils;
import com.soarclient.utils.mouse.MouseUtils;

import io.github.humbleui.skija.Font;
import io.github.humbleui.types.Rect;

/** 左侧导航栏：首页 / 模块 / 设置。 */
public class NavigationRail extends Component {

    private final List<Navigation> navigations = new ArrayList<>();
    private Navigation currentNavigation;
    private final SoarGui parent;

    /** 底部的"HUD 编辑器"按钮（不属于页面导航） */
    private final SimpleAnimation hudBtnFocus = new SimpleAnimation();
    private boolean hudBtnPressed;

    public NavigationRail(SoarGui parent, float x, float y, float width, float height) {
        super(x, y);
        this.parent = parent;
        this.width = width;
        this.height = height;

        for (SimplePage p : parent.getPages()) {
            Navigation n = new Navigation(p);
            if (p.getTitle().equals(parent.getCurrentPage().getTitle())) {
                currentNavigation = n;
                n.animation = new EaseStandard(Duration.MEDIUM_3, 0, 1);
            }
            navigations.add(n);
        }
        if (currentNavigation == null && !navigations.isEmpty()) currentNavigation = navigations.get(0);
    }

    @Override
    public void draw(double mouseX, double mouseY) {

        ColorPalette palette = Soar.getInstance().getColorManager().getPalette();

        Skia.drawRoundedRectVarying(x, y, width, height, 35, 0, 0, 35, palette.getSurface());

        float offsetY = navStartY();

        for (Navigation n : navigations) {

            SimplePage p = n.page;
            String icon = p.getIcon();
            boolean selected = currentNavigation != null && currentNavigation.equals(n);
            Font font = selected ? Fonts.getIconFill(24) : Fonts.getIcon(24);
            Rect bounds = Skia.getTextBounds(icon, font);
            float iconWidth = bounds.getWidth();
            float iconHeight = bounds.getHeight();

            Color c0 = selected ? palette.getOnSecondaryContainer() : palette.getOnSurfaceVariant();

            Animation animation = n.animation;
            float selWidth = 56;
            float selHeight = 32;
            boolean focus = MouseUtils.isInside(mouseX, mouseY, x + (width / 2) - (selWidth / 2), y + offsetY,
                    selWidth, selHeight) || n.pressed;

            n.focusAnimation.onTick(focus ? n.pressed ? 0.12F : 0.08F : 0, 8);

            Skia.drawRoundedRect(x + (width / 2) - (selWidth / 2), y + offsetY, selWidth, selHeight, 16,
                    ColorUtils.applyAlpha(palette.getOnSurfaceVariant(), n.focusAnimation.getValue()));

            if (animation.getEnd() != 0 || !animation.isFinished()) {
                Skia.drawRoundedRect(
                        x + (width / 2) - (selWidth / 2) + (selWidth - selWidth * animation.getValue()) / 2,
                        y + offsetY, selWidth * animation.getValue(), selHeight, 16,
                        ColorUtils.applyAlpha(palette.getSecondaryContainer(), animation.getValue()));
            }

            Skia.drawText(icon, x + (width / 2) - (iconWidth / 2), y + (offsetY + (selHeight / 2)) - (iconHeight / 2),
                    c0, font);

            offsetY += navStep();
        }

        // ---- 底部：HUD 编辑器按钮 ----
        drawHudButton(mouseX, mouseY);
    }

    /** HUD 编辑器按钮的位置（导航栏底部居中）。 */
    private float hudBtnX() {
        float sw = 56;
        return x + (width / 2) - (sw / 2);
    }

    private float hudBtnY() {
        return y + height - 52;
    }

    /** 打开 Meteor 的 HUD 编辑器。 */
    private void openHudEditor() {
        try {
            var mc = net.minecraft.client.MinecraftClient.getInstance();
            var theme = meteordevelopment.meteorclient.gui.GuiThemes.get();
            mc.setScreen(new meteordevelopment.meteorclient.systems.hud.screens
                    .HudEditorScreen(theme));
        } catch (Throwable t) {
            meteordevelopment.meteorclient.MeteorClient.LOG.warn("打开 HUD 编辑器失败: {}", t.toString());
        }
    }

    private void drawHudButton(double mouseX, double mouseY) {

        ColorPalette palette = Soar.getInstance().getColorManager().getPalette();

        float sw = 56;
        float sh = 32;
        float bx = hudBtnX();
        float by = hudBtnY();

        boolean focus = MouseUtils.isInside(mouseX, mouseY, bx, by, sw, sh) || hudBtnPressed;
        hudBtnFocus.onTick(focus ? hudBtnPressed ? 0.14F : 0.09F : 0, 8);

        Skia.drawRoundedRect(bx, by, sw, sh, 16,
                ColorUtils.applyAlpha(palette.getOnSurfaceVariant(), hudBtnFocus.getValue()));

        String icon = Icon.EDIT;
        Font font = Fonts.getIcon(24);
        Rect bounds = Skia.getTextBounds(icon, font);
        Color col = focus ? palette.getOnSurface() : palette.getOnSurfaceVariant();

        Skia.drawText(icon, bx + (sw / 2) - (bounds.getWidth() / 2),
                by + (sh / 2) - (bounds.getHeight() / 2), col, font);
    }
    @Override
    public void mousePressed(double mouseX, double mouseY, int button) {

        // HUD 编辑器按钮优先
        if (MouseUtils.isInside(mouseX, mouseY, hudBtnX(), hudBtnY(), 56, 32)
                && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            hudBtnPressed = true;
            return;
        }

        float offsetY = navStartY();
        float selWidth = 56;
        float selHeight = 32;

        for (Navigation n : navigations) {
            if (MouseUtils.isInside(mouseX, mouseY, x + (width / 2) - (selWidth / 2), y + offsetY, selWidth,
                    selHeight) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !n.equals(currentNavigation)) {
                n.pressed = true;
            }
            offsetY += navStep();
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {

        // HUD 编辑器按钮
        if (hudBtnPressed) {
            hudBtnPressed = false;
            if (MouseUtils.isInside(mouseX, mouseY, hudBtnX(), hudBtnY(), 56, 32)
                    && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                openHudEditor();
            }
            return;
        }

        float offsetY = navStartY();
        float selWidth = 56;
        float selHeight = 32;

        for (Navigation n : navigations) {

            if (MouseUtils.isInside(mouseX, mouseY, x + (width / 2) - (selWidth / 2), y + offsetY, selWidth,
                    selHeight) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !n.equals(currentNavigation)) {

                if (currentNavigation != null) currentNavigation.animation = new EaseStandard(Duration.MEDIUM_3, 1, 0);
                currentNavigation = n;
                n.animation = new EaseStandard(Duration.MEDIUM_3, 0, 1);
                parent.setCurrentPage(n.page);
            }

            n.pressed = false;
            offsetY += navStep();
        }
    }

    /** 导航项起始 Y：矮屏压缩。 */
    private float navStartY() {
        return height < 300 ? 16f : 34f;
    }

    /** 导航项间距：矮屏压缩，保证所有页面入口都能显示。 */
    private float navStep() {
        int n = Math.max(1, navigations.size());
        // 预留底部空间，把导航项均分在可用高度里，但不小于 46
        float avail = height - navStartY() - 20f;
        float step = avail / n;
        return Math.max(46f, Math.min(68f, step));
    }

    private static class Navigation {

        private final SimpleAnimation focusAnimation = new SimpleAnimation();
        private Animation animation;
        private final SimplePage page;
        private boolean pressed;

        private Navigation(SimplePage page) {
            this.page = page;
            this.animation = new DummyAnimation();
        }
    }
}
