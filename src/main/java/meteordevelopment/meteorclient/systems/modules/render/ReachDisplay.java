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
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * 攻击距离显示：在准星附近显示当前瞄准实体的距离。
 */
public class ReachDisplay extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> onlyWhenTargeting = sgGeneral.add(new BoolSetting.Builder()
        .name("仅瞄准时显示")
        .description("只有准星指向实体时才显示。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("缩放")
        .description("文字缩放。")
        .defaultValue(1.0).min(0.5).sliderRange(0.5, 3)
        .build()
    );

    private final Setting<Double> offsetY = sgGeneral.add(new DoubleSetting.Builder()
        .name("垂直偏移")
        .description("相对准星向下的偏移量。")
        .defaultValue(18).sliderRange(-100, 100)
        .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("颜色")
        .description("文字颜色。")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    public ReachDisplay() {
        super(Categories.Render, "reach-display", "在准星附近显示你与瞄准实体之间的距离。");
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mc.player == null || mc.world == null) return;

        Entity target = null;
        HitResult hit = mc.crosshairTarget;
        if (hit instanceof EntityHitResult ehr) {
            target = ehr.getEntity();
        }

        if (target == null) {
            if (onlyWhenTargeting.get()) return;
            return;
        }

        double dist = mc.player.distanceTo(target);

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        double cx = sw / 2.0;
        double cy = sh / 2.0 + offsetY.get();

        String text = String.format("%.2f", dist);
        Color c = color.get();

        Renderer2D.COLOR.begin();
        double w = Soar2D.textWidth(text, scale.get());
        // 半透明底，保证可读
        Soar2D.rect(cx - w / 2 - 2, cy - 4, w + 4, Soar2D.textHeight(scale.get()) + 4,
                new Color(0, 0, 0, 120));
        Renderer2D.COLOR.render(null);

        Soar2D.text(text, cx - w / 2, cy, c, true, scale.get());
    }
}
