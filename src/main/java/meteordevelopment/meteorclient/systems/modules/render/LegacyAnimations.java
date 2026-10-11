/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

/**
 * 1.7 动画：还原老版本的挥剑 / 钓鱼竿 / 受击倾斜等观感。
 *
 * <p>各项可独立开关。取值方法供 mixin 在渲染阶段读取，
 * 未启用时一律返回原版行为，保证零副作用。
 */
public class LegacyAnimations extends Module {

    private final SettingGroup sgSwing = settings.createGroup("挥动");
    private final SettingGroup sgRod = settings.createGroup("钓鱼竿");
    private final SettingGroup sgEntity = settings.createGroup("实体");

    private final Setting<Boolean> swing = sgSwing.add(new BoolSetting.Builder()
        .name("老版挥剑")
        .description("挥动动画更贴近 1.7 的手感。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> swingSpeed = sgSwing.add(new DoubleSetting.Builder()
        .name("挥动速度")
        .description("挥动动画的速度倍率。")
        .defaultValue(1.0).min(0.5).sliderRange(0.5, 3)
        .visible(swing::get)
        .build()
    );

    private final Setting<Boolean> rod = sgRod.add(new BoolSetting.Builder()
        .name("老版钓鱼竿")
        .description("钓鱼竿的持握与投掷动画使用老版本姿势。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> headRotation = sgEntity.add(new BoolSetting.Builder()
        .name("头部跟随")
        .description("实体头部随身体旋转（老版没有独立头部朝向）。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> hurtTilt = sgEntity.add(new BoolSetting.Builder()
        .name("受击倾斜")
        .description("受击时角色向后倾斜。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> itemPickup = sgEntity.add(new BoolSetting.Builder()
        .name("拾取动画")
        .description("拾取物品时手部抬起。")
        .defaultValue(true)
        .build()
    );

    public LegacyAnimations() {
        super(Categories.Render, "legacy-animations", "还原 1.7 版本的挥剑、钓鱼竿与角色动画。");
    }

    public boolean legacySwing() {
        return isActive() && swing.get();
    }

    public double swingSpeed() {
        return swingSpeed.get();
    }

    public boolean legacyRod() {
        return isActive() && rod.get();
    }

    public boolean headFollowsBody() {
        return isActive() && headRotation.get();
    }

    public boolean hurtTilt() {
        return isActive() && hurtTilt.get();
    }

    public boolean pickupAnimation() {
        return isActive() && itemPickup.get();
    }
}
