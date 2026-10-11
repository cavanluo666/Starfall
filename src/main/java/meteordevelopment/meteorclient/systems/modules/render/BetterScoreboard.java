/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

/**
 * 计分板美化：自定义侧边栏的背景、文字颜色与尺寸。
 *
 * <p>绘制由 mixin 在 InGameHud 阶段接管，本模块只负责配置与取值。
 */
public class BetterScoreboard extends Module {

    public enum BackgroundMode {
        无,
        纯色,
        渐变
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<BackgroundMode> backgroundMode = sgGeneral.add(new EnumSetting.Builder<BackgroundMode>()
        .name("背景模式")
        .description("侧边栏背景的绘制方式。")
        .defaultValue(BackgroundMode.纯色)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgGeneral.add(new ColorSetting.Builder()
        .name("背景颜色")
        .description("侧边栏背景颜色。")
        .defaultValue(new SettingColor(12, 14, 20, 170))
        .visible(() -> backgroundMode.get() != BackgroundMode.无)
        .build()
    );

    private final Setting<Double> backgroundOpacity = sgGeneral.add(new DoubleSetting.Builder()
        .name("背景不透明度")
        .description("在设定颜色基础上再乘的系数。")
        .defaultValue(1.0).min(0.05).sliderRange(0.05, 1)
        .visible(() -> backgroundMode.get() != BackgroundMode.无)
        .build()
    );

    private final Setting<Boolean> rounded = sgGeneral.add(new BoolSetting.Builder()
        .name("圆角")
        .description("背景使用圆角。")
        .defaultValue(true)
        .visible(() -> backgroundMode.get() != BackgroundMode.无)
        .build()
    );

    private final Setting<SettingColor> titleColor = sgGeneral.add(new ColorSetting.Builder()
        .name("标题颜色")
        .description("计分板标题的颜色。")
        .defaultValue(new SettingColor(120, 190, 255))
        .build()
    );

    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder()
        .name("文字颜色")
        .description("条目文字颜色。")
        .defaultValue(new SettingColor(235, 235, 240))
        .build()
    );

    private final Setting<Boolean> hideScores = sgGeneral.add(new BoolSetting.Builder()
        .name("隐藏分数")
        .description("隐藏右侧的分数数字。")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("缩放")
        .description("文字缩放。")
        .defaultValue(1.0).min(0.5).sliderRange(0.5, 2)
        .build()
    );

    public BetterScoreboard() {
        super(Categories.Render, "better-scoreboard", "美化计分板侧边栏：自定义背景、颜色与缩放。");
    }

    public BackgroundMode getBackgroundMode() {
        return backgroundMode.get();
    }

    public SettingColor getBackgroundColor() {
        SettingColor c = new SettingColor(backgroundColor.get());
        c.a = (int) (c.a * backgroundOpacity.get());
        return c;
    }

    public boolean isRounded() {
        return rounded.get();
    }

    public SettingColor getTitleColor() {
        return titleColor.get();
    }

    public SettingColor getTextColor() {
        return textColor.get();
    }

    public boolean isHideScores() {
        return hideScores.get();
    }

    public double getScale() {
        return scale.get();
    }
}
