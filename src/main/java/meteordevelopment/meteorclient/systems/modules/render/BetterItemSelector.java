/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

/**
 * 物品栏选择器美化：给快捷栏当前选中的格子加上自定义高亮框。
 *
 * <p>只做视觉叠加，不改变选中逻辑。
 */
public class BetterItemSelector extends Module {

    public enum HighlightStyle {
        边框,
        填充,
        两者
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<HighlightStyle> style = sgGeneral.add(new EnumSetting.Builder<HighlightStyle>()
        .name("样式")
        .description("高亮方式。")
        .defaultValue(HighlightStyle.边框)
        .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("颜色")
        .description("高亮颜色。")
        .defaultValue(new SettingColor(255, 255, 255, 120))
        .build()
    );

    private final Setting<Double> thickness = sgGeneral.add(new DoubleSetting.Builder()
        .name("边框粗细")
        .description("边框线宽。")
        .defaultValue(2.0).min(1).sliderRange(1, 6)
        .visible(() -> style.get() != HighlightStyle.填充)
        .build()
    );

    private final Setting<Boolean> animate = sgGeneral.add(new BoolSetting.Builder()
        .name("过渡动画")
        .description("切换槽位时高亮框平滑移动。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> animSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("动画速度")
        .description("过渡速度。")
        .defaultValue(0.35).min(0.05).sliderRange(0.05, 1)
        .visible(animate::get)
        .build()
    );

    public BetterItemSelector() {
        super(Categories.Render, "better-item-selector", "给快捷栏选中的格子添加自定义高亮框。");
    }

    public HighlightStyle getStyle() {
        return style.get();
    }

    public SettingColor getColor() {
        return color.get();
    }

    public double getThickness() {
        return thickness.get();
    }

    public boolean isAnimate() {
        return animate.get();
    }

    public double getAnimSpeed() {
        return animSpeed.get();
    }
}
