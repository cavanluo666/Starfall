package com.soarclient.gui.modmenu.pages;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.soarclient.Soar;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.gui.api.Layout;
import com.soarclient.gui.api.SoarGui;
import com.soarclient.gui.api.page.Page;
import com.soarclient.gui.api.page.impl.LeftRightTransition;
import com.soarclient.gui.api.page.impl.RightLeftTransition;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;
import com.soarclient.ui.component.api.PressAnimation;
import com.soarclient.ui.component.impl.CategoryBar;
import com.soarclient.ui.component.impl.Tooltip;
import com.soarclient.utils.ColorUtils;
import com.soarclient.utils.SearchUtils;
import com.soarclient.utils.language.I18n;
import com.soarclient.utils.mouse.MouseUtils;

import meteordevelopment.meteorclient.systems.modules.Module;

/**
 * 模块总览页：直接读取 Meteor 的模块注册表。
 */
public class ModsPage extends Page {

    private final List<Item> items = new ArrayList<>();

    /** 分类筛选栏 */
    private final CategoryBar categoryBar = new CategoryBar();

    /** 悬停提示 */
    private final Tooltip tooltip = new Tooltip();

    /** 临时诊断计数 */
    private int diagLeft = 3;

    /** 当前选中的分类名（Meteor Category.name），null / "全部" 表示不筛选 */
    private String selectedCategory = "全部";

    public ModsPage(SoarGui parent) {
        super(parent, "text.mods", Icon.INVENTORY_2, new RightLeftTransition(true));
    }

    @Override
    public void init() {
        super.init();
        items.clear();
        for (Module m : meteordevelopment.meteorclient.systems.modules.Modules.get().getList()) {
            if (m.addon != null && m.addon.name.equalsIgnoreCase("meteor")) {
                // 保留全部 Meteor 自带模块
            }
            Item i = new Item(m);
            if (m.isActive()) i.pressAnimation.setPressed();
            items.add(i);
        }
        for (Item i : items) {
            i.xAnimation.setFirstTick(true);
            i.yAnimation.setFirstTick(true);
        }

        // 分类标签：全部 + Meteor 的六个分类（中文显示）
        java.util.List<String> labels = new ArrayList<>();
        labels.add("全部");
        for (meteordevelopment.meteorclient.systems.modules.Category cat
                : meteordevelopment.meteorclient.systems.modules.Modules.loopCategories()) {
            labels.add(I18n.has(cat.name) ? I18n.get(cat.name) : cat.name);
        }

        categoryBar.setTabs(labels, selectedCategory);
        categoryBar.setOnChange(sel -> {
            selectedCategory = sel;
            scrollHelper.reset();
        });
    }

    /** 模块是否属于当前选中的分类。中英文名都接受。 */
    private boolean inCategory(Module m) {
        if (selectedCategory == null || "全部".equals(selectedCategory)) return true;
        if (m.category == null) return false;

        String name = m.category.name;
        String localized = I18n.has(name) ? I18n.get(name) : name;

        // 同时接受 "Render" 与 "渲染"，方便脚本/别名使用
        return selectedCategory.equals(localized) || selectedCategory.equalsIgnoreCase(name);
    }

    private boolean matches(Module m) {
        if (!inCategory(m)) return false;
        if (searchBar.getText().isEmpty()) return true;
        return SearchUtils.isSimilar(searchText(m), searchBar.getText());
    }

    private static String searchText(Module m) {
        String zh = I18n.has(m.title) ? I18n.get(m.title) : "";
        return (m.title + " " + m.name + " " + zh + " " + (m.description == null ? "" : m.description)).toLowerCase();
    }

    @Override
    public void draw(double mouseX, double mouseY) {
        // 保存原始鼠标坐标：下方会为滚动改写 mouseY，
        // 但悬停提示必须用屏幕坐标，否则会跟着滚动偏移（表现为"卡在下面"）。
        final double rawMouseX = mouseX;
        final double rawMouseY = mouseY;

        super.draw(mouseX, mouseY);

        ColorPalette palette = Soar.getInstance().getColorManager().getPalette();

        int index = 0;
        int perRow = Layout.cardsPerRow();
        float padX = Layout.contentPadX();
        float offsetX = padX;
        float offsetY = 0;

        if (diagLeft > 0) {
            diagLeft--;
            meteordevelopment.meteorclient.MeteorClient.LOG.info(
                "[ModsDiag] x={} y={} w={} h={} contentTop={} barH={} top={} tabs={} sel={}",
                x, y, width, height, Layout.contentTop(),
                categoryBar.getHeight(),
                contentStartY(),
                categoryBar.getTabCount(), categoryBar.getSelected());
        }

        // 分类栏固定在顶部，不随内容滚动
        float barX = x + padX;
        float barW = width - padX * 2f;
        categoryBar.setBounds(barX, categoryBarTop(), barW);
        // 把实测高度回传给基类，供内容起点 / 滚动条轨道 / 视口高度统一推导
        categoryBarHeight = categoryBar.getHeight();

        // 卡片区从内容起点开始（内容起点已包含 搜索栏 + 分类栏 + 间距）
        float contentY = contentStartY();

        // 本帧鼠标悬停的模块（用于显示说明）
        Module hovered = null;

        // 传给卡片的是"已去掉滚动量"的坐标（分类栏已单独处理）
        mouseY = mouseY - scrollHelper.getValue();

        Skia.save();
        Skia.translate(0, scrollHelper.getValue());

        for (Item i : items) {

            Module m = i.module;
            SimpleAnimation focusAnimation = i.focusAnimation;
            SimpleAnimation xAnimation = i.xAnimation;
            SimpleAnimation yAnimation = i.yAnimation;

            if (!matches(m)) continue;

            float itemX = x + offsetX;
            float itemY = contentY + offsetY;

            focusAnimation.onTick(MouseUtils.isInside(mouseX, mouseY, itemX, itemY,
                    Layout.CARD_W, Layout.CARD_H + Layout.CARD_BAR_H) ? i.pressed ? 0.12F : 0.08F : 0, 8);
            xAnimation.onTick(itemX, 14);
            yAnimation.onTick(itemY, 14);

            itemX = xAnimation.getValue();
            itemY = yAnimation.getValue();

            Skia.drawRoundedRectVarying(itemX, itemY, Layout.CARD_W, Layout.CARD_H, 26, 26, 0, 0, palette.getSurface());
            Skia.drawRoundedRectVarying(itemX, itemY + Layout.CARD_H, Layout.CARD_W, Layout.CARD_BAR_H, 0, 0, 26, 26,
                    palette.getSurfaceContainerLow());

            Skia.drawRoundedRectVarying(itemX, itemY + Layout.CARD_H, Layout.CARD_W, Layout.CARD_BAR_H, 0, 0, 26, 26,
                    ColorUtils.applyAlpha(palette.getSurfaceContainerLowest(), focusAnimation.getValue()));

            Skia.save();
            Skia.clip(itemX, itemY + Layout.CARD_H, Layout.CARD_W, Layout.CARD_BAR_H, 0, 0, 26, 26);
            i.pressAnimation.draw(itemX, itemY + Layout.CARD_H, (Layout.CARD_W - 20), Layout.CARD_BAR_H, palette.getPrimaryContainer(), 1);
            Skia.restore();

            String name = I18n.has(m.title) ? I18n.get(m.title) : m.title;
            Skia.drawFullCenteredText(Skia.getLimitText(name, Fonts.getRegular(16), Layout.CARD_W - 20),
                    itemX + Layout.CARD_W / 2f, itemY + Layout.CARD_H + Layout.CARD_BAR_H / 2f,
                    m.isActive() ? palette.getOnPrimaryContainer() : palette.getOnSurfaceVariant(),
                    Fonts.getRegular(16));
            Skia.drawFullCenteredText(moduleIcon(m), itemX + Layout.CARD_W / 2f, itemY + Layout.CARD_H / 2f,
                    m.isActive() ? palette.getPrimary() : palette.getOnSurfaceVariant(), Fonts.getIcon(68));

            // 记录鼠标悬停的模块，供最后绘制提示
            if (MouseUtils.isInside(mouseX, mouseY, itemX, itemY,
                    Layout.CARD_W, Layout.CARD_H + Layout.CARD_BAR_H)) {
                hovered = m;
            }

            index++;
            offsetX += Layout.CARD_GAP_X + Layout.CARD_W;
            if (index % perRow == 0) {
                offsetX = padX;
                offsetY += Layout.CARD_GAP_Y + Layout.CARD_H + Layout.CARD_BAR_H;
            }
        }

        // 内容总高 -> 滚动范围（视口由基类按内容起点推算）
        contentBottom = contentY + offsetY + Layout.CARD_H + Layout.CARD_BAR_H + 16f;
        updateScrollRange();

        Skia.restore();

        // ---- 顶部常驻区（搜索栏 + 分类栏）----
        // 卡片滚动时会从下方滑过，所以这里画一张不透明卡片把它们整体托住，
        // 既避免文字被卡片穿过造成重影，也让顶部区域有清晰的视觉分层。
        ColorPalette pal = Soar.getInstance().getColorManager().getPalette();

        float padX2 = Layout.contentPadX();
        // 顶栏底卡的几何与搜索栏/分类栏/内容起点共用同一套常量，避免各自硬编码后失配
        float topY = y + Layout.searchTop() - 8f;                       // 搜索栏上方留一点内边距
        float topH = Layout.topAreaHeight(categoryBarHeight) + 8f;      // 与 contentStartY 对齐

        // 不透明底卡（比面板底色略亮一档，形成"顶栏"层次）
        Skia.drawRoundedRectVarying(
                x + 4f, topY, width - 8f, topH,
                22f, 22f, 14f, 14f,
                pal.getSurfaceContainerHigh());

        // 底边细分隔线，进一步区分顶栏与内容区
        Skia.drawRect(x + 16f, topY + topH - 1f, width - 32f, 1f,
                com.soarclient.utils.ColorUtils.applyAlpha(pal.getOutline(), 0.35f));

        // 顶栏内容最后画，确保在卡片之上
        searchBar.draw(mouseX, mouseY);
        categoryBar.draw(mouseX, mouseY);

        // ---- 悬停提示（最后绘制，保证在最上层）----
        if (hovered != null) {
            String zhName = I18n.has(hovered.title) ? I18n.get(hovered.title) : hovered.title;
            String zhDesc = hovered.description == null ? "" 
                    : (I18n.has(hovered.description) ? I18n.get(hovered.description) : hovered.description);
            tooltip.set(zhName, zhDesc, rawMouseX, rawMouseY);
        } else {
            tooltip.hide();
        }

        tooltip.draw(x, y, width, height,
                com.soarclient.compat.McCompat.scaledWidth(),
                com.soarclient.compat.McCompat.scaledHeight());
    }

    private static String moduleIcon(Module m) {
        if (m.category == meteordevelopment.meteorclient.systems.modules.Categories.Combat) return Icon.SWORDS;
        if (m.category == meteordevelopment.meteorclient.systems.modules.Categories.Player) return Icon.PERSON;
        if (m.category == meteordevelopment.meteorclient.systems.modules.Categories.Movement) return Icon.DIRECTIONS_RUN;
        if (m.category == meteordevelopment.meteorclient.systems.modules.Categories.Render) return Icon.VISIBILITY;
        if (m.category == meteordevelopment.meteorclient.systems.modules.Categories.World) return Icon.PUBLIC;
        return Icon.CATEGORY;
    }

    @Override
    public void mousePressed(double mouseX, double mouseY, int button) {
        // 搜索栏是常驻控件且位于分类栏上方，先由父类处理
        super.mousePressed(mouseX, mouseY, button);

        // 落在搜索栏区域则不继续处理（搜索栏常驻在分类栏上方）
        if (searchBar != null
                && mouseX >= searchBar.getX() && mouseX <= searchBar.getX() + searchBar.getWidth()
                && mouseY >= searchBar.getY() && mouseY <= searchBar.getY() + searchBar.getHeight()) {
            return;
        }

        // 分类栏
        categoryBar.mousePressed(mouseX, mouseY, button);
        if (isInsideCategoryBar(mouseY)) {
            return;
        }

        for (Item i : items) {
            if (!matches(i.module)) continue;
            float itemX = i.xAnimation.getValue();
            float itemY = i.yAnimation.getValue();
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && MouseUtils.isInside(mouseX, mouseY, itemX, itemY + Layout.CARD_H, Layout.CARD_W, Layout.CARD_BAR_H)) {
                i.pressed = true;
            }
        }
    }

    /** 鼠标是否落在分类栏矩形内（与绘制位置共用同一来源，避免命中区域错位）。 */
    private boolean isInsideCategoryBar(double mouseY) {
        return categoryBarHeight > 0f
                && mouseY >= categoryBarTop()
                && mouseY <= categoryBarTop() + categoryBarHeight;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {

        // 必须先转发给父类 —— 搜索栏是常驻控件，由 Page 处理
        super.mouseReleased(mouseX, mouseY, button);

        // 分类栏
        categoryBar.mouseReleased(mouseX, mouseY, button);
        if (isInsideCategoryBar(mouseY)) {
            return;
        }

        mouseY = mouseY - scrollHelper.getValue();

        for (Item i : items) {

            Module m = i.module;
            if (!matches(m)) continue;

            float itemX = i.xAnimation.getValue();
            float itemY = i.yAnimation.getValue();

            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if (MouseUtils.isInside(mouseX, mouseY, itemX, itemY + Layout.CARD_H, Layout.CARD_W, Layout.CARD_BAR_H)) {
                    m.toggle();
                    if (m.isActive()) i.pressAnimation.onPressed(mouseX, mouseY, itemX, itemY + Layout.CARD_H);
                    else i.pressAnimation.onReleased(mouseX, mouseY, itemX, itemY + Layout.CARD_H);
                }

                if (MouseUtils.isInside(mouseX, mouseY, itemX, itemY, Layout.CARD_W, Layout.CARD_H)) {
                    boolean any = false;
                    for (meteordevelopment.meteorclient.settings.SettingGroup g : m.settings) {
                        if (g.iterator().hasNext()) { any = true; break; }
                    }
                    if (any) {
                        parent.setCurrentPage(new SettingsImplPage(parent, this.getClass(), m));
                        this.setTransition(new LeftRightTransition(true));
                    }
                }
            }

            i.pressed = false;
        }
    }

    @Override
    public void onClosed() {
        this.setTransition(new RightLeftTransition(true));
    }

    private static class Item {

        private final Module module;
        private final SimpleAnimation focusAnimation = new SimpleAnimation();
        private final SimpleAnimation xAnimation = new SimpleAnimation();
        private final SimpleAnimation yAnimation = new SimpleAnimation();
        private final PressAnimation pressAnimation = new PressAnimation();
        private boolean pressed;

        private Item(Module module) {
            this.module = module;
        }
    }
}
