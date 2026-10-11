package com.soarclient.gui.modmenu.pages;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.soarclient.gui.api.Layout;
import com.soarclient.gui.api.SoarGui;
import com.soarclient.gui.api.page.Page;
import com.soarclient.gui.api.page.impl.RightTransition;
import com.soarclient.gui.modmenu.component.SettingBar;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Icon;
import com.soarclient.utils.SearchUtils;
import com.soarclient.utils.language.I18n;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

/**
 * 展示某个 Meteor 模块的全部设置项（按 Meteor 的 SettingGroup 顺序）。
 */
public class SettingsImplPage extends Page {

    private final List<SettingBar> bars = new ArrayList<>();
    private final Class<? extends Page> prevPage;
    private final Module module;

    public SettingsImplPage(SoarGui parent, Class<? extends Page> prevPage, Module module) {
        super(parent, "text.mods", Icon.SETTINGS, new RightTransition(true));
        this.prevPage = prevPage;
        this.module = module;
    }

    public Module getModule() {
        return module;
    }

    @Override
    public void init() {
        super.init();
        bars.clear();

        for (SettingGroup group : module.settings) {
            for (Setting<?> s : group) {
                if (!s.isVisible()) continue;
                bars.add(new SettingBar(s, x + Layout.listPadX(), y + Layout.contentTop(), width - Layout.listPadX() * 2f));
            }
        }
        parent.setClosable(false);
    }

    private boolean matches(SettingBar b) {
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

        contentBottom = offsetY;
        // 视口高度统一由基类按内容起点推算，避免与滚动条轨道口径不一致
        updateScrollRange();
        Skia.restore();
    }

    @Override
    public void mousePressed(double mouseX, double mouseY, int button) {
        super.mousePressed(mouseX, mouseY, button);
        mouseY = mouseY - scrollHelper.getValue();
        for (SettingBar b : bars) if (matches(b)) b.mousePressed(mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        super.mouseReleased(mouseX, mouseY, button);
        mouseY = mouseY - scrollHelper.getValue();
        for (SettingBar b : bars) if (matches(b)) b.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        super.charTyped(chr, modifiers);
        for (SettingBar b : bars) if (matches(b)) b.charTyped(chr, modifiers);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        super.keyPressed(keyCode, scanCode, modifiers);
        for (SettingBar b : bars) if (matches(b)) b.keyPressed(keyCode, scanCode, modifiers);

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            parent.setClosable(true);
            parent.setCurrentPage(prevPage);
        }
    }

    @Override
    public void onClosed() {
        if (!parent.isClosable()) {
            parent.setClosable(true);
            com.soarclient.gui.api.page.SimplePage prev = parent.getPage(prevPage);
            if (prev != null) prev.onClosed();
        }
    }
}
