/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

/**
 * 无潜行动画：去掉潜行时视角下沉与模型下蹲的过渡，PVP 里手感更跟手。
 */
public class NoSneakAnimation extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> viewOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("仅视角")
        .description("只去掉第一人称的视角下沉，保留模型下蹲。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> dropScale = sgGeneral.add(new DoubleSetting.Builder()
        .name("下沉比例")
        .description("保留多少下沉量。0 为完全不下沉。")
        .defaultValue(0.0).min(0).sliderRange(0, 1)
        .visible(() -> !viewOnly.get())
        .build()
    );

    public NoSneakAnimation() {
        super(Categories.Render, "no-sneak-animation", "去掉潜行时视角下沉的过渡，让瞄准更跟手。");
    }

    /** 是否只改视角（不动模型下蹲）。 */
    public boolean isViewOnly() {
        return viewOnly.get();
    }

    /** 供 mixin 调用：返回视角下沉量的缩放系数。 */
    public double getViewDropScale() {
        if (!isActive()) return 1.0;
        return viewOnly.get() ? 0.0 : dropScale.get();
    }

    /** 供 mixin 调用：模型下蹲是否禁用。 */
    public boolean disableSneakPose() {
        return isActive() && !viewOnly.get() && dropScale.get() <= 0.001;
    }
}
