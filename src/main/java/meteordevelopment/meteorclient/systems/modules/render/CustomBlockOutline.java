/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

/**
 * 自定义方块描边：修改准星所指方块的轮廓颜色、线宽与填充。
 *
 * <p>实际绘制由 mixin 在 WorldRenderer 阶段完成，本模块只提供配置与取值。
 */
public class CustomBlockOutline extends Module {

    private final SettingGroup sgOutline = settings.createGroup("轮廓");
    private final SettingGroup sgFill = settings.createGroup("填充");

    private final Setting<SettingColor> color = sgOutline.add(new ColorSetting.Builder()
        .name("颜色")
        .description("轮廓颜色。")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    private final Setting<Double> width = sgOutline.add(new DoubleSetting.Builder()
        .name("线宽")
        .description("轮廓线宽。")
        .defaultValue(1.5).min(0.5).sliderRange(0.5, 8)
        .build()
    );

    private final Setting<Boolean> fill = sgFill.add(new BoolSetting.Builder()
        .name("填充")
        .description("在轮廓内额外填充半透明面。")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> fillColor = sgFill.add(new ColorSetting.Builder()
        .name("填充颜色")
        .description("填充面的颜色。")
        .defaultValue(new SettingColor(255, 255, 255, 40))
        .visible(fill::get)
        .build()
    );

    public CustomBlockOutline() {
        super(Categories.Render, "custom-block-outline", "自定义准星所指方块的轮廓颜色、线宽与填充。");
    }

    public SettingColor getColor() {
        return color.get();
    }

    public double getWidth() {
        return width.get();
    }

    public boolean isFillEnabled() {
        return fill.get();
    }

    public SettingColor getFillColor() {
        return fillColor.get();
    }
}
