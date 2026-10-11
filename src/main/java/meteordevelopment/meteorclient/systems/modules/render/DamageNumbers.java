/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.soar.Soar2D;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 伤害飘字：命中时在目标上方浮出伤害数字并上飘淡出。
 */
public class DamageNumbers extends Module {

    private static final Vector3d POS = new Vector3d();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("缩放")
        .description("数字大小。")
        .defaultValue(1.4).min(0.5).sliderRange(0.5, 4)
        .build()
    );

    private final Setting<Double> lifetime = sgGeneral.add(new DoubleSetting.Builder()
        .name("持续时间")
        .description("数字存在秒数。")
        .defaultValue(1.2).min(0.2).sliderRange(0.2, 5)
        .build()
    );

    private final Setting<Double> riseSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("上飘速度")
        .description("每秒上飘的格数。")
        .defaultValue(0.6).min(0).sliderRange(0, 3)
        .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("颜色")
        .description("数字颜色。")
        .defaultValue(new SettingColor(255, 80, 80))
        .build()
    );

    private final Setting<Boolean> critColor = sgGeneral.add(new BoolSetting.Builder()
        .name("暴击变色")
        .description("暴击时使用不同颜色。")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> critColorValue = sgGeneral.add(new ColorSetting.Builder()
        .name("暴击颜色")
        .description("暴击时的数字颜色。")
        .defaultValue(new SettingColor(255, 200, 60))
        .visible(critColor::get)
        .build()
    );

    private final List<Entry> entries = new ArrayList<>();

    public DamageNumbers() {
        super(Categories.Render, "damage-numbers", "命中时在目标上方浮出伤害数字。");
    }

    @Override
    public void onActivate() {
        entries.clear();
    }

    /** 由 mixin 在造成伤害时调用。 */
    public void addDamage(double x, double y, double z, float amount, boolean crit) {
        if (!isActive()) return;
        if (entries.size() > 64) entries.remove(0);
        entries.add(new Entry(x, y, z, amount, crit, System.currentTimeMillis()));
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (entries.isEmpty()) return;

        long now = System.currentTimeMillis();
        double life = lifetime.get() * 1000.0;

        Color base = color.get();
        Color crit = critColorValue.get();

        Renderer2D.COLOR.begin();
        for (Iterator<Entry> it = entries.iterator(); it.hasNext(); ) {
            Entry e = it.next();
            double age = now - e.time;
            if (age >= life) {
                it.remove();
                continue;
            }

            double t = age / life;
            double alpha = 1.0 - t;

            POS.set(e.x, e.y + riseSpeed.get() * (age / 1000.0), e.z);

            if (!NametagUtils.to2D(POS, scale.get())) continue;

            NametagUtils.begin(POS);

            String text = String.format("%.1f", e.amount);
            Color c = e.crit && critColor.get() ? crit : base;
            Color faded = new Color(c.r, c.g, c.b, (int) (c.a * alpha));

            double w = Soar2D.textWidth(text, 1.0);
            // 在 3D 投影坐标下绘制
            Soar2D.text(text, -w / 2, 0, faded, true, 1.0);

            NametagUtils.end();
        }
        Renderer2D.COLOR.render(null);
    }

    private record Entry(double x, double y, double z, float amount, boolean crit, long time) {
    }
}
