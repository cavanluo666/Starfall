package com.soarclient.gui.modmenu;

import java.util.ArrayList;
import java.util.List;

import com.soarclient.gui.api.Layout;
import com.soarclient.gui.api.SoarGui;
import com.soarclient.gui.api.page.SimplePage;
import com.soarclient.gui.modmenu.component.NavigationRail;
import com.soarclient.gui.modmenu.pages.AboutPage;
import com.soarclient.gui.modmenu.pages.HomePage;
import com.soarclient.gui.modmenu.pages.ModsPage;
import com.soarclient.gui.modmenu.pages.SettingsPage;

/**
 * Material You 风格的主界面窗口。
 *
 * <p>尺寸与列数全部由 {@link Layout} 按当前屏幕 / GUI Scale 推算，
 * 不再写死 938x580。
 */
public class GuiModMenu extends SoarGui {

    private NavigationRail navigationRail;

    public GuiModMenu() {
        super(false);
    }

    @Override
    public void init() {
        layout();
        super.init();
    }

    /** 重建导航栏（尺寸随窗口变化）。 */
    private void layout() {
        components.clear();
        navigationRail = new NavigationRail(this, getX(), getY(), Layout.navWidth(), getHeight());
        components.add(navigationRail);
    }

    @Override
    public void onResize() {
        // 只重新布局，不重播入场动画
        layout();
        if (getCurrentPage() != null) {
            setPageSize(getCurrentPage());
            getCurrentPage().init();
        }
    }

    @Override
    public void setPageSize(SimplePage p) {
        p.setX(getX() + navigationRail.getWidth());
        p.setY(getY());
        p.setWidth(getWidth() - navigationRail.getWidth());
        p.setHeight(getHeight());
    }

    @Override
    public List<SimplePage> createPages() {
        List<SimplePage> pages = new ArrayList<>();
        pages.add(new HomePage(this));
        pages.add(new ModsPage(this));
        pages.add(new SettingsPage(this));
        pages.add(new AboutPage(this));
        return pages;
    }

    @Override
    public float getWidth() {
        return Layout.panelWidth();
    }

    @Override
    public float getHeight() {
        return Layout.panelHeight();
    }

    @Override
    public float getX() {
        return Layout.panelX();
    }

    @Override
    public float getY() {
        return Layout.panelY();
    }
}
