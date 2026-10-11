/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 */

package meteordevelopment.meteorclient.systems.modules.combat;

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
import net.minecraft.sound.SoundEvents;

/**
 * 命中标记：击中被你攻击的实体时，在准星处画一个短暂淡出的十字，可附带命中音效。
 */
public class HitMarker extends Module {

    public enum SoundType {
        清脆,
        柔和
    }

    public enum TriggerMode {
        全部,
        仅近战,
        仅远程
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSound = settings.createGroup("音效");
    private final SettingGroup sgRender = settings.createGroup("外观");

    private final Setting<Boolean> enableSound = sgSound.add(new BoolSetting.Builder()
        .name("启用音效")
        .description("命中时播放提示音。")
        .defaultValue(true)
        .build()
    );

    private final Setting<SoundType> soundType = sgSound.add(new EnumSetting.Builder<SoundType>()
        .name("音效类型")
        .description("播放哪种命中音。")
        .defaultValue(SoundType.清脆)
        .visible(enableSound::get)
        .build()
    );

    private final Setting<TriggerMode> triggerMode = sgSound.add(new EnumSetting.Builder<TriggerMode>()
        .name("触发条件")
        .description("音效在哪些命中情况下播放。")
        .defaultValue(TriggerMode.全部)
        .visible(enableSound::get)
        .build()
    );

    private final Setting<Double> volume = sgSound.add(new DoubleSetting.Builder()
        .name("音量")
        .description("音效音量。")
        .defaultValue(1.0)
        .min(0).sliderRange(0, 2)
        .visible(enableSound::get)
        .build()
    );

    private final Setting<SettingColor> color = sgRender.add(new ColorSetting.Builder()
        .name("颜色")
        .description("命中标记颜色。")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    private final Setting<Double> duration = sgRender.add(new DoubleSetting.Builder()
        .name("持续时间")
        .description("淡出所需时间（毫秒）。")
        .defaultValue(250).min(50).sliderRange(50, 1000)
        .build()
    );

    private final Setting<Double> size = sgRender.add(new DoubleSetting.Builder()
        .name("大小")
        .description("十字臂长。")
        .defaultValue(5).min(2).sliderRange(2, 20)
        .build()
    );

    private final Setting<Double> gap = sgRender.add(new DoubleSetting.Builder()
        .name("间隙")
        .description("中心留空半径。")
        .defaultValue(3).min(0).sliderRange(0, 12)
        .build()
    );

    private final Setting<Double> thickness = sgRender.add(new DoubleSetting.Builder()
        .name("粗细")
        .description("线条粗细。")
        .defaultValue(2).min(0.5).sliderRange(0.5, 6)
        .build()
    );

    private long lastHitTime = 0;
    private Color hitColor = null;

    public HitMarker() {
        super(Categories.Combat, "hit-marker", "击中被你攻击过的实体时，在准星处显示短暂的十字标记，可附带命中音效。");
    }

    @Override
    public void onActivate() {
        lastHitTime = 0;
    }

    /** 每次造成伤害时调用（由 mixin 触发）。 */
    public void onHit(boolean isRanged) {
        if (!isActive()) return;

        lastHitTime = System.currentTimeMillis();
        hitColor = color.get();

        if (!enableSound.get() || mc.player == null) return;

        boolean play = switch (triggerMode.get()) {
            case 全部 -> true;
            case 仅近战 -> !isRanged;
            case 仅远程 -> isRanged;
        };
        if (!play) return;

        float v = volume.get().floatValue();
        if (soundType.get() == SoundType.清脆) {
            mc.player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, v, 1.0F);
        } else {
            mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, v, 0.5F);
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (lastHitTime == 0) return;

        long elapsed = System.currentTimeMillis() - lastHitTime;
        double dur = duration.get();
        if (elapsed >= dur) {
            lastHitTime = 0;
            return;
        }

        double fade = 1.0 - (double) elapsed / dur;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        double cx = sw / 2.0;
        double cy = sh / 2.0;

        Color base = hitColor == null ? color.get() : hitColor;
        Color c = Soar2D.alpha(base, fade);

        double arm = size.get();
        double g = gap.get();
        double t = thickness.get();

        Renderer2D.COLOR.begin();
        Soar2D.line(cx - g, cy - g, cx - g - arm, cy - g - arm, t, c);
        Soar2D.line(cx + g, cy - g, cx + g + arm, cy - g - arm, t, c);
        Soar2D.line(cx + g, cy + g, cx + g + arm, cy + g + arm, t, c);
        Soar2D.line(cx - g, cy + g, cx - g - arm, cy + g + arm, t, c);
        Renderer2D.COLOR.render(null);
    }
}
