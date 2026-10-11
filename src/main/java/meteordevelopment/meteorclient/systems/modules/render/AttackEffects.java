/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;

/**
 * 攻击特效：命中时在目标位置生成额外的粒子特效。
 *
 * <p>可分别控制暴击 / 锋利 / 火焰 / 血 / 闪电，并各自调节倍率。
 * 粒子只在本机生成（客户端），不发送任何数据包。
 */
public class AttackEffects extends Module {

    private final SettingGroup sgParticles = settings.createGroup("粒子");
    private final SettingGroup sgMisc = settings.createGroup("其它");

    private final Setting<Boolean> crit = sgParticles.add(new BoolSetting.Builder()
        .name("暴击")
        .description("命中时生成暴击粒子。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> critMultiplier = sgParticles.add(new DoubleSetting.Builder()
        .name("暴击倍率")
        .description("暴击粒子数量倍率。")
        .defaultValue(1.0).min(0).sliderRange(0, 5)
        .visible(crit::get)
        .build()
    );

    private final Setting<Boolean> sharpness = sgParticles.add(new BoolSetting.Builder()
        .name("锋利")
        .description("生成锋利附魔粒子。")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> sharpnessMultiplier = sgParticles.add(new DoubleSetting.Builder()
        .name("锋利倍率")
        .description("锋利粒子数量倍率。")
        .defaultValue(1.0).min(0).sliderRange(0, 5)
        .visible(sharpness::get)
        .build()
    );

    private final Setting<Boolean> flame = sgParticles.add(new BoolSetting.Builder()
        .name("火焰")
        .description("生成火焰粒子。")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> flameMultiplier = sgParticles.add(new DoubleSetting.Builder()
        .name("火焰倍率")
        .description("火焰粒子数量倍率。")
        .defaultValue(1.0).min(0).sliderRange(0, 5)
        .visible(flame::get)
        .build()
    );

    private final Setting<Boolean> blood = sgParticles.add(new BoolSetting.Builder()
        .name("血液")
        .description("生成红色血粒子。")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> bloodColor = sgParticles.add(new ColorSetting.Builder()
        .name("血液颜色")
        .description("血粒子的颜色。")
        .defaultValue(new SettingColor(180, 0, 0))
        .visible(blood::get)
        .build()
    );

    private final Setting<Double> bloodMultiplier = sgParticles.add(new DoubleSetting.Builder()
        .name("血液倍率")
        .description("血粒子数量倍率。")
        .defaultValue(1.0).min(0).sliderRange(0, 5)
        .visible(blood::get)
        .build()
    );

    private final Setting<Boolean> lightning = sgParticles.add(new BoolSetting.Builder()
        .name("闪电")
        .description("生成闪电粒子。")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> lightningCount = sgParticles.add(new IntSetting.Builder()
        .name("闪电数量")
        .description("每次命中的闪电粒子数量。")
        .defaultValue(1).min(1).sliderRange(1, 10)
        .visible(lightning::get)
        .build()
    );

    private final Setting<Boolean> onlyCrit = sgMisc.add(new BoolSetting.Builder()
        .name("仅暴击时")
        .description("只有暴击命中才生成特效。")
        .defaultValue(false)
        .build()
    );

    public AttackEffects() {
        super(Categories.Render, "attack-effects", "命中目标时生成额外的粒子特效（仅本机可见）。");
    }

    /** 由 mixin 在攻击命中时调用。 */
    public void onHit(Entity target, boolean isCrit) {
        if (!isActive() || mc.world == null) return;
        if (onlyCrit.get() && !isCrit) return;
        if (!(target instanceof LivingEntity)) return;

        double x = target.getX();
        double y = target.getY() + target.getHeight() / 2.0;
        double z = target.getZ();

        spawnCount(ParticleTypes.CRIT, x, y, z, crit.get() ? critMultiplier.get() : 0);
        spawnCount(ParticleTypes.ENCHANTED_HIT, x, y, z, sharpness.get() ? sharpnessMultiplier.get() : 0);
        spawnCount(ParticleTypes.FLAME, x, y, z, flame.get() ? flameMultiplier.get() : 0);

        if (blood.get()) {
            SettingColor bc = bloodColor.get();
            var effect = new net.minecraft.particle.DustParticleEffect(
                new org.joml.Vector3f(bc.r / 255f, bc.g / 255f, bc.b / 255f), 1.2f);
            int n = (int) Math.round(8 * bloodMultiplier.get());
            for (int i = 0; i < n; i++) {
                mc.world.addParticle(effect,
                    x + rnd(), y + rnd(), z + rnd(),
                    rnd() * 0.2, Math.abs(rnd()) * 0.2, rnd() * 0.2);
            }
        }

        if (lightning.get()) {
            for (int i = 0; i < lightningCount.get(); i++) {
                mc.world.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    x + rnd(), y + rnd(), z + rnd(), 0, 0, 0);
            }
        }
    }

    private void spawnCount(net.minecraft.particle.SimpleParticleType type,
                            double x, double y, double z, double multiplier) {
        int n = (int) Math.round(8 * multiplier);
        for (int i = 0; i < n; i++) {
            mc.world.addParticle(type, x + rnd(), y + rnd(), z + rnd(), rnd() * 0.4, rnd() * 0.4, rnd() * 0.4);
        }
    }

    private double rnd() {
        return (Math.random() - 0.5) * 0.8;
    }
}
