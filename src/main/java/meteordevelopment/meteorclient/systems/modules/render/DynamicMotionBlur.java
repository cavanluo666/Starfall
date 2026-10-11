/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

/**
 * 动态模糊：按移动速度给画面叠加拖影。
 *
 * <p>实现思路：不引入额外的后处理 shader（那会污染 Meteor 的渲染管线），
 * 而是通过降低视场变化时的“帧间插值”来实现柔和的运动观感 ——
 * 即调整相机在 tick 之间的插值强度，并可选降低 FOV 抖动。
 *
 * <p>强度越高，运动时的拖影越明显。
 */
public class DynamicMotionBlur extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> strength = sgGeneral.add(new DoubleSetting.Builder()
        .name("强度")
        .description("运动模糊强度。")
        .defaultValue(1.0).min(0.1).sliderRange(0.1, 3)
        .build()
    );

    private final Setting<Boolean> refreshRateScaling = sgGeneral.add(new BoolSetting.Builder()
        .name("按刷新率缩放")
        .description("高刷新率下自动减弱，避免模糊过度。")
        .defaultValue(true)
        .build()
    );

    public DynamicMotionBlur() {
        super(Categories.Render, "dynamic-motion-blur", "运动时产生柔和的拖影效果。");
    }

    public double getStrength() {
        double s = strength.get();
        if (refreshRateScaling.get()) {
            // 以 60fps 为基准，高刷时减弱
            double fps = Math.max(30, mc.getCurrentFps());
            s *= Math.sqrt(60.0 / fps);
        }
        return s;
    }
}
