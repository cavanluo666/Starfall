package com.soarclient.gui.api.page;

import com.soarclient.gui.api.Layout;
import com.soarclient.gui.api.SoarGui;
import com.soarclient.skia.Skia;
import com.soarclient.ui.component.impl.ScrollBar;
import com.soarclient.ui.component.impl.text.SearchBar;
import com.soarclient.utils.mouse.ScrollHelper;

/**
 * 带滚动与搜索的页面基类。
 *
 * <p>滚动条支持拖动（触摸板友好）；滚动条位置贴在页面右缘。
 */
public class Page extends SimplePage {

    protected ScrollHelper scrollHelper = new ScrollHelper();
    protected SearchBar searchBar;

    /** 可拖动的滚动条 */
    protected final ScrollBar scrollBar = new ScrollBar();

    /** 内容底部（用于计算滚动范围），由子类在 draw 里设置 */
    protected float contentBottom = 0f;

    /** 分类栏实测高度（面板坐标系），由带分类栏的子类在 draw 里写入 */
    protected float categoryBarHeight = 0f;

    /** 搜索栏在屏幕上的顶部 Y（供子类对齐顶栏底卡）。 */
    protected float searchBarTop() {
        return y + Layout.searchTop();
    }

    /** 分类栏在屏幕上的顶部 Y。 */
    protected float categoryBarTop() {
        return y + Layout.categoryTop();
    }

    /**
     * 内容区在屏幕上的起始 Y。
     *
     * <p>由搜索栏 + 分类栏实测高度推导，**滚动条轨道与卡片区都必须用它**，
     * 否则分类栏换行变高时就会盖住滚动条 / 卡片穿进分类栏。
     */
    protected float contentStartY() {
        return y + Layout.contentTop(categoryBarHeight);
    }

    public Page(SoarGui parent, String title, String icon, GuiTransition transition) {
        super(parent, title, icon, transition);
    }

    @Override
    public void init() {

        String text = "";

        if (searchBar != null) {
            text = searchBar.getText();
        }

        float sw = Layout.searchWidth();
        searchBar = new SearchBar(x + width - sw - Layout.listPadX(), searchBarTop(), sw, text, () -> {
            scrollHelper.reset();
        });
    }

    @Override
    public void draw(double mouseX, double mouseY) {

        scrollHelper.onUpdate();

        // 滚动条轨道：与内容区上下对齐，绝不能压到顶部常驻区（搜索栏 / 分类栏）
        float trackTop = contentStartY();
        float trackBottom = y + height - Layout.MARGIN_Y;
        float trackH = trackBottom - trackTop;
        if (trackH < 40f) trackH = Math.max(20f, height - 60f);
        scrollBar.setBounds(x + width - ScrollBar.WIDTH - ScrollBar.MARGIN, trackTop,
                ScrollBar.WIDTH, trackH);

        // 搜索栏常驻：画在未滚动层，不受内容滚动影响
        searchBar.draw(mouseX, mouseY);

        // 滚动条同样画在未滚动层
        if (searchBar.getText().isEmpty()) {
            scrollBar.draw(scrollHelper, mouseX, mouseY);
        }
    }

    /**
     * 由子类在完成布局后调用，更新滚动范围。
     *
     * <p>视口高度取「内容区」高度（面板底 - 内容起点），而不是整个面板高度 ——
     * 否则滚动范围会按整块面板算，滑块比例与拖动映射都会偏。
     */
    protected void updateScrollRange() {
        float viewport = (y + height - Layout.MARGIN_Y) - contentStartY();
        scrollHelper.setMaxScroll(contentBottom - contentStartY(), viewport);
    }

    @Override
    public void mousePressed(double mouseX, double mouseY, int button) {

        // 先处理滚动条拖动（优先，避免被内容抢走）
        if (scrollHelper.isScrollable()
                && (scrollBar.isOnThumb(scrollHelper, mouseX, mouseY) || scrollBar.isOnTrack(mouseX, mouseY))
                && button == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) {

            if (scrollBar.isOnThumb(scrollHelper, mouseX, mouseY)) {
                scrollHelper.beginDrag((float) mouseY, scrollBar.getThumbTop(scrollHelper),
                        scrollBar.getTrackY(), scrollBar.getTrackHeight(), scrollBar.getThumbLength(scrollHelper));
            } else {
                // 点轨道空白：跳到该位置
                scrollHelper.jumpTo((float) mouseY, scrollBar.getTrackY(),
                        scrollBar.getTrackHeight(), scrollBar.getThumbLength(scrollHelper));
            }
            return;
        }

        // 搜索栏常驻，用屏幕原始坐标（不减滚动量）
        searchBar.mousePressed(mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (scrollHelper.isDragging()) {
            scrollHelper.endDrag();
            return;
        }
        // 搜索栏常驻，用屏幕原始坐标
        searchBar.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (scrollHelper.isDragging()) {
            scrollHelper.updateDrag((float) mouseY);
        }
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        searchBar.charTyped(chr, modifiers);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        searchBar.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollHelper.onScroll(verticalAmount);
    }
}
