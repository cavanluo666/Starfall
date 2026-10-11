/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

/**
 * 平滑快捷栏：滚轮切换物品栏时带过渡动画，避免瞬间跳跃。
 *
 * <p>只做视觉上的平滑，不改动实际选中的槽位逻辑，
 * 因此不会与服务器产生不同步。
 */
public class SmoothHotbar extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("速度")
        .description("过渡速度，越大越快。")
        .defaultValue(0.55).min(0.05).sliderRange(0.05, 2)
        .build()
    );

    private final Setting<Integer> rollover = sgGeneral.add(new IntSetting.Builder()
        .name("循环滚动")
        .description("从第 9 格继续滚到第 1 格的模式。0 为关闭。")
        .defaultValue(0).min(0).max(2).sliderRange(0, 2)
        .build()
    );

    public SmoothHotbar() {
        super(Categories.Render, "smooth-hotbar", "滚轮切换物品栏时带平滑过渡动画。");
    }

    public double getSpeed() {
        return speed.get();
    }

    public int getRollover() {
        return rollover.get();
    }
}
