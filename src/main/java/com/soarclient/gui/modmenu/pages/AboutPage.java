package com.soarclient.gui.modmenu.pages;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.soarclient.Soar;
import com.soarclient.gui.api.Layout;
import com.soarclient.gui.api.SoarGui;
import com.soarclient.gui.api.page.Page;
import com.soarclient.gui.api.page.impl.RightLeftTransition;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;
import com.soarclient.ui.component.impl.Tooltip;
import com.soarclient.utils.ColorUtils;
import com.soarclient.utils.mouse.MouseUtils;

import io.github.humbleui.types.Rect;

/**
 * 关于页面：作者信息 + 本项目使用的开源项目清单。
 *
 * <p>开源清单是 GPLv3 的分发要求之一（需保留各组件的许可证声明），
 * 因此这里同时作为 NOTICE 的界面呈现。
 */
public class AboutPage extends Page {

    /** 一个开源项目条目 */
    private record Project(String name, String license, String note) {
    }

    /** 作者信息：点击可打开链接 */
    private record Link(String label, String value, String url) {
    }

    private static final List<Link> LINKS = List.of(
        new Link("GitHub", "cavanluo666", "https://github.com/cavanluo666"),
        new Link("Bilibili", "空间主页", "https://space.bilibili.com/3493095616284680"),
        new Link("个人网站", "246644.xyz", "https://246644.xyz")
    );

    /** 本项目实际使用的开源组件（与 libs / 内置 jar 一致） */
    private static final List<Project> PROJECTS = List.of(
        new Project("Meteor Client", "GPL-3.0", "客户端核心功能与框架"),
        new Project("SoarClient v8.0", "MIT", "Material You 界面、Skia 渲染层"),
        new Project("Baritone", "LGPL-3.0", "寻路与自动挖矿（已内置）"),
        new Project("Skija / Skia", "Apache-2.0", "界面 GPU 渲染"),
        new Project("Fabric Loader / API", "Apache-2.0", "模组加载与事件"),
        new Project("Orbit", "MIT", "事件总线"),
        new Project("Starscript", "MIT", "HUD 表达式解析"),
        new Project("Caffeine", "Apache-2.0", "缓存"),
        new Project("fastutil", "Apache-2.0", "高性能集合"),
        new Project("JOML", "MIT", "数学库"),
        new Project("Commons IO", "Apache-2.0", "IO 工具"),
        new Project("JNA", "Apache-2.0 / LGPL-2.1+", "输入法修复（调用 imm32）"),
        new Project("LWJGL NFD", "BSD-3-Clause", "原生文件选择框"),
        new Project("CozyUI+", "见包内 LICENSE", "原版界面与字体材质（内置）"),
        new Project("Xray Ultimate", "见包内 LICENSE", "矿石透视材质（内置）")
    );

    private final Tooltip tooltip = new Tooltip();
    private String hoverUrl = null;

    public AboutPage(SoarGui parent) {
        super(parent, "text.about", Icon.INFO, new RightLeftTransition(true));
    }

    @Override
    public void draw(double mouseX, double mouseY) {
        super.draw(mouseX, mouseY);

        ColorPalette palette = Soar.getInstance().getColorManager().getPalette();

        float padX = Layout.listPadX();
        float cx = x + width / 2f;
        float top = y + Layout.contentTop() + 8f;

        // 滚动坐标系
        double my = mouseY - scrollHelper.getValue();
        hoverUrl = null;

        Skia.save();
        Skia.translate(0, scrollHelper.getValue());

        // ---- 标题 ----
        Skia.drawFullCenteredText(Icon.ROCKET_LAUNCH, cx, top, palette.getPrimary(), Fonts.getIcon(64));
        Skia.drawCenteredText("星陨 Starfall", cx, top + 52, palette.getOnSurface(), Fonts.getMedium(30));
        Skia.drawCenteredText("Minecraft 实用客户端 · Material You 界面", cx, top + 84,
                palette.getOnSurfaceVariant(), Fonts.getRegular(15));
        Skia.drawCenteredText("Version " + Soar.getInstance().getVersion() + " · GPL-3.0", cx, top + 106,
                ColorUtils.applyAlpha(palette.getOnSurfaceVariant(), 0.8f), Fonts.getRegular(13));

        float y = top + 140;

        // ---- 作者 ----
        y = sectionTitle(palette, "作者", x + padX, y, width - padX * 2f);

        float cardH = LINKS.size() * 34f + 16f;
        Skia.drawRoundedRect(x + padX, y, width - padX * 2f, cardH, 16, palette.getSurface());

        float ly = y + 8f;
        for (Link l : LINKS) {
            float lx = x + padX + 14f;
            boolean hover = MouseUtils.isInside(my, my, lx, ly, width - padX * 2f - 28f, 30)
                    || MouseUtils.isInside(mouseX, my, lx, ly, width - padX * 2f - 28f, 30);

            Skia.drawText(l.label, lx, ly + 4f, palette.getOnSurfaceVariant(), Fonts.getRegular(14));
            Skia.drawText(l.value, lx + 80f, ly + 4f,
                    hover ? palette.getPrimary() : palette.getOnSurface(), Fonts.getMedium(15));

            // 右侧外部链接图标
            Skia.drawText(Icon.OPEN_IN_NEW, x + width - padX - 30f, ly + 5f,
                    ColorUtils.applyAlpha(palette.getOnSurfaceVariant(), 0.7f), Fonts.getIcon(16));

            if (MouseUtils.isInside(mouseX, my, lx - 6, ly, width - padX * 2f - 28f, 30)) {
                hoverUrl = l.url;
            }
            ly += 34f;
        }
        y += cardH + 18f;

        // ---- 开源项目 ----
        y = sectionTitle(palette, "使用的开源项目", x + padX, y, width - padX * 2f);

        float rowH = 30f;
        float tableH = PROJECTS.size() * rowH + 12f;
        Skia.drawRoundedRect(x + padX, y, width - padX * 2f, tableH, 16, palette.getSurface());

        float ty = y + 6f;
        for (int i = 0; i < PROJECTS.size(); i++) {
            Project pr = PROJECTS.get(i);

            // 斑马纹，便于阅读
            if (i % 2 == 1) {
                Skia.drawRoundedRect(x + padX + 6f, ty, width - padX * 2f - 12f, rowH, 8,
                        ColorUtils.applyAlpha(palette.getSurfaceContainerHigh(), 0.5f));
            }

            float tx = x + padX + 14f;
            Skia.drawText(pr.name, tx, ty + 7f, palette.getOnSurface(), Fonts.getMedium(14));

            // 许可证（右对齐）
            String lic = "[" + pr.license + "]";
            Rect b = Skia.getTextBounds(lic, Fonts.getRegular(12));
            float licW = b == null ? lic.length() * 6f : b.getWidth();
            Skia.drawText(lic, x + width - padX - 14f - licW, ty + 8f,
                    withAlpha(palette.getPrimary(), 0.9f), Fonts.getRegular(12));

            ty += rowH;
        }
        y += tableH + 18f;

        // ---- 说明 ----
        Skia.drawText("本项目以 GPL-3.0 开源，源码地址：", x + padX, y,
                palette.getOnSurfaceVariant(), Fonts.getRegular(13));
        Skia.drawText("https://github.com/cavanluo666/Starfall", x + padX, y + 18f,
                palette.getPrimary(), Fonts.getMedium(13));
        y += 44f;

        Skia.drawText("感谢 Meteor Client、SoarClient、Baritone 及上述所有开源项目的作者。",
                x + padX, y, ColorUtils.applyAlpha(palette.getOnSurfaceVariant(), 0.85f), Fonts.getRegular(12));

        contentBottom = y + 30f;
        scrollHelper.setMaxScroll(contentBottom - Layout.contentTop(), height - Layout.contentTop());

        Skia.restore();

        // ---- 悬停提示（显示完整链接）----
        if (hoverUrl != null) {
            tooltip.set("链接", hoverUrl, mouseX, mouseY);
        } else {
            tooltip.hide();
        }
        tooltip.draw(x, y, width, height,
                com.soarclient.compat.McCompat.scaledWidth(),
                com.soarclient.compat.McCompat.scaledHeight());
    }

    /** 画一个小节标题，返回下一行的 Y。 */
    private float sectionTitle(ColorPalette palette, String text, float sx, float sy, float w) {
        Skia.drawText(text, sx, sy, palette.getPrimary(), Fonts.getMedium(16));
        Skia.drawRect(sx, sy + 20f, 28f, 2f, palette.getPrimary());
        return sy + 34f;
    }

    private static Color withAlpha(Color c, float f) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(),
                Math.max(0, Math.min(255, (int) (c.getAlpha() * f))));
    }

    /** 供 GuiModMenu 判断是否需要打开链接用。 */
    public String getHoverUrl() {
        return hoverUrl;
    }
}
