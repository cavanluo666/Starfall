package com.soarclient.gui.modmenu.pages;

import com.soarclient.Soar;
import com.soarclient.gui.api.SoarGui;
import com.soarclient.gui.api.page.Page;
import com.soarclient.gui.api.page.impl.RightLeftTransition;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;

import meteordevelopment.meteorclient.systems.modules.Modules;

/** 首页：显示模块统计与快捷键说明。 */
public class HomePage extends Page {

    public HomePage(SoarGui parent) {
        super(parent, "text.home", Icon.HOME, new RightLeftTransition(true));
    }

    @Override
    public void draw(double mouseX, double mouseY) {
        super.draw(mouseX, mouseY);

        ColorPalette palette = Soar.getInstance().getColorManager().getPalette();

        int total = 0, active = 0;
        for (meteordevelopment.meteorclient.systems.modules.Module m : Modules.get().getList()) {
            total++;
            if (m.isActive()) active++;
        }

        float cx = x + width / 2f;
        float top = y + 120;

        Skia.drawFullCenteredText(Icon.ROCKET_LAUNCH, cx, top, palette.getPrimary(), Fonts.getIcon(96));
        Skia.drawCenteredText("Meteor + Soar", cx, top + 70, palette.getOnSurface(), Fonts.getMedium(34));
        Skia.drawCenteredText("Meteor 功能 · Material You 界面", cx, top + 108, palette.getOnSurfaceVariant(),
                Fonts.getRegular(16));

        Skia.drawRoundedRect(cx - 260, top + 150, 520, 96, 24, palette.getSurface());
        Skia.drawCenteredText("已启用模块  " + active + " / " + total, cx, top + 182, palette.getOnSurface(),
                Fonts.getMedium(20));
        Skia.drawCenteredText("按 Right Shift 开关此界面，按 Esc 关闭", cx, top + 214,
                palette.getOnSurfaceVariant(), Fonts.getRegular(14));

        Skia.drawCenteredText("提示：点击模块卡片切换开关，点击卡片上半部分进入设置", cx, top + 280,
                palette.getOnSurfaceVariant(), Fonts.getRegular(14));
    }
}
