/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.entity.LivingEntity;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * 目标信息：显示准星所指实体的名字、血量、护甲与距离。
 *
 * <p>与 Meteor 自带的 CombatHud 不同，这里聚焦"当前准星目标"，
 * 布局紧凑，适合 PVP 时快速扫一眼。
 */
public class TargetInfoHud extends HudElement {

    public static final HudElementInfo<TargetInfoHud> INFO = new HudElementInfo<>(
        Hud.GROUP, "target-info", "显示准星所指实体的血量、护甲与距离。", TargetInfoHud::new);

    public enum Style {
        紧凑,
        标准
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Style> style = sgGeneral.add(new EnumSetting.Builder<Style>()
        .name("样式")
        .description("布局样式。")
        .defaultValue(Style.标准)
        .onChanged(v -> updateSize())
        .build()
    );

    private final Setting<Double> width = sgGeneral.add(new DoubleSetting.Builder()
        .name("宽度")
        .description("整体宽度。")
        .defaultValue(140).min(80).sliderRange(80, 400)
        .onChanged(v -> updateSize())
        .build()
    );

    private final Setting<Boolean> showArmor = sgGeneral.add(new BoolSetting.Builder()
        .name("显示护甲")
        .description("显示目标护甲值。")
        .defaultValue(true)
        .visible(() -> style.get() == Style.标准)
        .build()
    );

    private final Setting<Boolean> showDistance = sgGeneral.add(new BoolSetting.Builder()
        .name("显示距离")
        .description("显示与目标的距离。")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> bgColor = sgGeneral.add(new ColorSetting.Builder()
        .name("背景颜色")
        .description("底衬颜色。")
        .defaultValue(new SettingColor(0, 0, 0, 140))
        .build()
    );

    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder()
        .name("文字颜色")
        .description("名字颜色。")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    private final Setting<SettingColor> healthColor = sgGeneral.add(new ColorSetting.Builder()
        .name("血量颜色")
        .description("血条颜色。")
        .defaultValue(new SettingColor(255, 60, 60))
        .build()
    );

    private final Setting<SettingColor> absorptionColor = sgGeneral.add(new ColorSetting.Builder()
        .name("吸收血量颜色")
        .description("金色吸收心的颜色。")
        .defaultValue(new SettingColor(255, 200, 40))
        .build()
    );

    public TargetInfoHud() {
        super(INFO);
        updateSize();
    }

    private void updateSize() {
        double h = style.get() == Style.紧凑 ? 22 : 38;
        setSize(width.get(), h);
    }

    @Override
    public void render(HudRenderer renderer) {
        if (mc.player == null || mc.world == null) return;

        // 优先用准星目标，否则用最近攻击过的
        LivingEntity target = null;
        if (mc.targetedEntity instanceof LivingEntity le && le != mc.player) {
            target = le;
        }
        if (target == null || !target.isAlive()) return;

        double x = getX();
        double y = getY();
        double w = getWidth();
        double h = getHeight();

        renderer.quad(x, y, w, h, bgColor.get());

        String name = target.getName().getString();
        // 限宽，避免名字太长撑破布局
        while (renderer.textWidth(name, true, -1) > w - 8 && name.length() > 3) {
            name = name.substring(0, name.length() - 2);
        }
        renderer.text(name, x + 4, y + 3, textColor.get(), true, -1);

        double dist = mc.player.distanceTo(target);
        if (showDistance.get()) {
            String ds = String.format("%.1fm", dist);
            double dw = renderer.textWidth(ds, true, -1);
            renderer.text(ds, x + w - dw - 4, y + 3, textColor.get(), true, -1);
        }

        // 血条
        float hp = target.getHealth();
        float maxHp = Math.max(1, target.getMaxHealth());
        float absorb = target.getAbsorptionAmount();

        double barY = y + 16;
        double barH = 6;
        double barW = w - 8;

        double hpRatio = Math.min(1.0, hp / maxHp);
        double absorbRatio = Math.min(1.0 - hpRatio, absorb / maxHp);

        renderer.quad(x + 4, barY, barW, barH, new Color(0, 0, 0, 160));
        renderer.quad(x + 4, barY, barW * hpRatio, barH, healthColor.get());
        if (absorbRatio > 0) {
            renderer.quad(x + 4 + barW * hpRatio, barY, barW * absorbRatio, barH, absorptionColor.get());
        }

        if (style.get() == Style.标准) {
            String hs = String.format("%.1f / %.1f", hp, maxHp);
            renderer.text(hs, x + 4, barY + barH + 2, textColor.get(), true, -1);

            if (showArmor.get()) {
                int armor = target.getArmor();
                String as = "护甲 " + armor;
                double aw = renderer.textWidth(as, true, -1);
                renderer.text(as, x + w - aw - 4, barY + barH + 2, textColor.get(), true, -1);
            }
        }
    }
}
