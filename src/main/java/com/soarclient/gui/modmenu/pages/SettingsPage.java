package com.soarclient.gui.modmenu.pages;

import java.util.ArrayList;
import java.util.List;

import com.soarclient.SoarConfig;
import com.soarclient.gui.api.Layout;
import com.soarclient.gui.api.SoarGui;
import com.soarclient.gui.api.page.Page;
import com.soarclient.gui.api.page.impl.RightLeftTransition;
import com.soarclient.gui.modmenu.component.SettingBar;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Icon;
import com.soarclient.utils.SearchUtils;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.config.Config;

/**
 * 客户端设置页：Meteor 的全局配置（Config）加上 Material3 外观开关。
 * 外观开关通过自定义的 SettingBar 子类渲染。
 */
public class SettingsPage extends Page {

    private final List<SettingBar> bars = new ArrayList<>();
    private final List<AppearanceBar> appearance = new ArrayList<>();

    public SettingsPage(SoarGui parent) {
        super(parent, "text.settings", Icon.SETTINGS, new RightLeftTransition(true));
    }

    @Override
    public void init() {
        super.init();
        bars.clear();
        appearance.clear();

        Config cfg = Config.get();
        if (cfg != null) {
            for (SettingGroup g : cfg.settings) {
                for (Setting<?> s : g) {
                    if (!s.isVisible()) continue;
                    bars.add(new SettingBar(s, x + Layout.listPadX(), y + Layout.contentTop(), width - Layout.listPadX() * 2f));
                }
            }
        }

        // ---- 外观 ----
        appearance.add(new AppearanceBar("外观 · 主题", "深色 / 浅色（Material You）",
                Icon.BRIGHTNESS_6, x + Layout.listPadX(), y + Layout.contentTop(), width - Layout.listPadX() * 2f,
                java.util.List.of("深色", "浅色"),
                SoarConfig.get().isDarkMode() ? "深色" : "浅色",
                v -> {
                    SoarConfig.get().setDarkMode("深色".equals(v));
                    // 让调色板立即刷新
                    com.soarclient.Soar.getInstance().getColorManager().onTick();
                }));

        appearance.add(new AppearanceBar("外观 · 主题色相", "调整 Material You 种子色的色相（0-360）",
                Icon.PALETTE, x + Layout.listPadX(), y + Layout.contentTop(), width - Layout.listPadX() * 2f,
                (float) SoarConfig.get().getHct().getHue(), 0f, 360f, 1f,
                v -> {
                    com.soarclient.libraries.material3.hct.Hct old = SoarConfig.get().getHct();
                    SoarConfig.get().setHct(com.soarclient.libraries.material3.hct.Hct.from(
                            v, Math.max(old.getChroma(), 20), old.getTone()));
                }));

        appearance.add(new AppearanceBar("外观 · 背景模糊", "开启面板与界面的毛玻璃模糊",
                Icon.LENS_BLUR, x + Layout.listPadX(), y + Layout.contentTop(), width - Layout.listPadX() * 2f, SoarConfig.get().isBlur(),
                v -> SoarConfig.get().setBlur(v)));

        appearance.add(new AppearanceBar("外观 · 模糊强度", "模糊半径，越大越朦胧（1-20）",
                Icon.BLUR_LINEAR, x + Layout.listPadX(), y + Layout.contentTop(), width - Layout.listPadX() * 2f,
                (float) SoarConfig.get().getBlurIntensity(), 1f, 20f, 1f,
                v -> SoarConfig.get().setBlurIntensity(Math.round(v))));

        appearance.add(new AppearanceBar("外观 · 快速模糊", "限制模糊刷新率以降低性能开销",
                Icon.SPEED, x + Layout.listPadX(), y + Layout.contentTop(), width - Layout.listPadX() * 2f, SoarConfig.get().isFastBlur(),
                v -> SoarConfig.get().setFastBlur(v)));

        appearance.add(new AppearanceBar("外观 · 原版界面美化材质", "启用内置的 CozyUI+ 材质包，替换原版界面与字体",
                Icon.BRUSH, x + Layout.listPadX(), y + Layout.contentTop(), width - Layout.listPadX() * 2f,
                meteordevelopment.meteorclient.soar.SoarIntegration.isCozyUiEnabled(),
                v -> meteordevelopment.meteorclient.soar.SoarIntegration.setCozyUiEnabled(v)));

        appearance.add(new AppearanceBar("外观 · 矿石透视材质", "启用内置的 Xray Ultimate 矿石透视材质包",
                Icon.VISIBILITY, x + Layout.listPadX(), y + Layout.contentTop(), width - Layout.listPadX() * 2f,
                meteordevelopment.meteorclient.soar.SoarIntegration.isXrayPackEnabled(),
                v -> meteordevelopment.meteorclient.soar.SoarIntegration.setXrayPackEnabled(v)));
    }

    private boolean matches(SettingBar b) {
        if (searchBar.getText().isEmpty()) return true;
        return SearchUtils.isSimilar(b.getTitle() + " " + b.getDescription(), searchBar.getText());
    }

    private boolean matchesA(AppearanceBar b) {
        if (searchBar.getText().isEmpty()) return true;
        return SearchUtils.isSimilar(b.getTitle() + " " + b.getDescription(), searchBar.getText());
    }

    @Override
    public void draw(double mouseX, double mouseY) {
        super.draw(mouseX, mouseY);

        float offsetY = Layout.contentTop();
        mouseY = mouseY - scrollHelper.getValue();

        Skia.save();
        Skia.translate(0, scrollHelper.getValue());

        for (SettingBar b : bars) {
            if (!matches(b)) continue;
            b.setY(y + offsetY);
            b.draw(mouseX, mouseY);
            offsetY += b.getHeight() + Layout.rowGap();
        }
        for (AppearanceBar b : appearance) {
            if (!matchesA(b)) continue;
            b.setY(y + offsetY);
            b.draw(mouseX, mouseY);
            offsetY += b.getHeight() + Layout.rowGap();
        }

        contentBottom = offsetY;
        // 视口高度统一由基类按内容起点推算，避免与滚动条轨道口径不一致
        updateScrollRange();
        Skia.restore();
    }

    @Override
    public void mousePressed(double mouseX, double mouseY, int button) {
        super.mousePressed(mouseX, mouseY, button);
        float my = (float) (mouseY - scrollHelper.getValue());
        for (SettingBar b : bars) if (matches(b)) b.mousePressed(mouseX, my, button);
        for (AppearanceBar b : appearance) if (matchesA(b)) b.mousePressed(mouseX, my, button);
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        super.mouseReleased(mouseX, mouseY, button);
        float my = (float) (mouseY - scrollHelper.getValue());
        for (SettingBar b : bars) if (matches(b)) b.mouseReleased(mouseX, my, button);
        for (AppearanceBar b : appearance) if (matchesA(b)) b.mouseReleased(mouseX, my, button);
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        super.charTyped(chr, modifiers);
        for (SettingBar b : bars) if (matches(b)) b.charTyped(chr, modifiers);
        for (AppearanceBar b : appearance) if (matchesA(b)) b.charTyped(chr, modifiers);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        super.keyPressed(keyCode, scanCode, modifiers);
        for (SettingBar b : bars) if (matches(b)) b.keyPressed(keyCode, scanCode, modifiers);
        for (AppearanceBar b : appearance) if (matchesA(b)) b.keyPressed(keyCode, scanCode, modifiers);
    }
}
