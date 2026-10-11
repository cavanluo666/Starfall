/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

/**
 * 低血量警告：血量低于阈值时屏幕边缘泛红并闪烁。
 */
public class LowHealthWarning extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> threshold = sgGeneral.add(new DoubleSetting.Builder()
        .name("血量阈值")
        .description("低于该血量时开始警告（按红心数）。")
        .defaultValue(8).min(1).sliderRange(1, 20)
        .build()
    );

    private final Setting<Integer> pulseSpeed = sgGeneral.add(new IntSetting.Builder()
        .name("闪烁速度")
        .description("每秒闪烁次数。")
        .defaultValue(2).min(1).sliderRange(1, 10)
        .build()
    );

    private final Setting<Double> opacity = sgGeneral.add(new DoubleSetting.Builder()
        .name("不透明度")
        .description("边缘泛红的最大不透明度。")
        .defaultValue(0.45).min(0.05).sliderRange(0.05, 1)
        .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("颜色")
        .description("警告颜色。")
        .defaultValue(new SettingColor(255, 0, 0))
        .build()
    );

    public LowHealthWarning() {
        super(Categories.Render, "low-health-warning", "血量过低时在屏幕边缘显示红色警告。");
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mc.player == null) return;

        float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        double th = threshold.get();
        if (health > th) return;

        // 越接近死亡越明显
        double severity = 1.0 - (health / th);

        // 闪烁
        double t = System.currentTimeMillis() / 1000.0;
        double pulse = (Math.sin(t * Math.PI * 2 * pulseSpeed.get()) + 1) / 2.0;

        double a = opacity.get() * severity * (0.55 + 0.45 * pulse);

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        Color c = color.get();
        Color edge = new Color(c.r, c.g, c.b, (int) Math.max(0, Math.min(255, c.a * a)));

        // 四条边缘渐变带
        double band = Math.min(sw, sh) * 0.18;
        Color transparent = new Color(c.r, c.g, c.b, 0);

        Renderer2D.COLOR.begin();
        // 上
        Renderer2D.COLOR.quad(0, 0, sw, band, edge, edge, transparent, transparent);
        // 下
        Renderer2D.COLOR.quad(0, sh - band, sw, band, transparent, transparent, edge, edge);
        // 左
        Renderer2D.COLOR.quad(0, 0, band, sh, edge, transparent, transparent, edge);
        // 右
        Renderer2D.COLOR.quad(sw - band, 0, band, sh, transparent, edge, edge, transparent);
        Renderer2D.COLOR.render(null);
    }
}
