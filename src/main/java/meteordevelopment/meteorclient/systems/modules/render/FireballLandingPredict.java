/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

/**
 * 火球落点预测：恶魂 / 烈焰人的火球飞行时，在预计落点画出标记框。
 *
 * <p>用简单的抛物线积分模拟轨迹，遇到实体方块即认为落点。
 */
public class FireballLandingPredict extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> maxTicks = sgGeneral.add(new IntSetting.Builder()
        .name("预测步数")
        .description("最多模拟多少 tick 的轨迹。")
        .defaultValue(120).min(10).sliderRange(10, 600)
        .build()
    );

    private final Setting<Double> size = sgGeneral.add(new DoubleSetting.Builder()
        .name("标记大小")
        .description("落点方框的半径。")
        .defaultValue(1.5).min(0.5).sliderRange(0.5, 8)
        .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("颜色")
        .description("落点框与轨迹颜色。")
        .defaultValue(new SettingColor(255, 120, 40))
        .build()
    );

    private final Setting<Boolean> showTrajectory = sgGeneral.add(new BoolSetting.Builder()
        .name("显示轨迹")
        .description("画出火球的预测飞行路径。")
        .defaultValue(true)
        .build()
    );

    /** 缓存：实体 -> 预测落点 */
    private final Map<Integer, Vec3d> landings = new HashMap<>();

    public FireballLandingPredict() {
        super(Categories.Render, "fireball-landing-predict", "预测火球的落点并画框标记。");
    }

    @Override
    public void onActivate() {
        landings.clear();
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.world == null || mc.player == null) return;

        landings.clear();

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof FireballEntity fb)) continue;

            Vec3d landing = simulate(fb);
            if (landing == null) continue;

            landings.put(fb.getId(), landing);

            double s = size.get();
            SettingColor col = color.get();

            event.renderer.box(
                landing.x - s, landing.y - 0.1, landing.z - s,
                landing.x + s, landing.y + 0.1, landing.z + s,
                col, col, ShapeMode.Lines, 0);

            if (showTrajectory.get()) {
                drawTrajectory(event, fb, landing);
            }
        }
    }

    /** 简单抛物线积分，返回落点；未落地返回 null。 */
    private Vec3d simulate(FireballEntity fb) {
        Vec3d pos = fb.getPos();
        Vec3d vel = fb.getVelocity();

        int steps = maxTicks.get();
        for (int i = 0; i < steps; i++) {
            Vec3d next = pos.add(vel);

            BlockPos bp = BlockPos.ofFloored(next);
            if (!mc.world.getBlockState(bp).isAir()) {
                return next;
            }

            pos = next;
            // 火球不受重力影响（原版火球直线飞行），但保留一点衰减更接近实际
            vel = vel.multiply(0.99);
        }
        return null;
    }

    private void drawTrajectory(Render3DEvent event, FireballEntity fb, Vec3d landing) {
        Vec3d pos = fb.getPos();
        Vec3d vel = fb.getVelocity();
        SettingColor c = color.get();

        Vec3d prev = pos;
        int steps = Math.min(maxTicks.get(), 200);
        for (int i = 0; i < steps; i++) {
            Vec3d next = prev.add(vel);
            if (next.squaredDistanceTo(landing) < 0.01) break;

            // 每隔几步画一小段，避免线太密
            if (i % 3 == 0) {
                event.renderer.line(prev.x, prev.y, prev.z, next.x, next.y, next.z, c);
            }
            prev = next;
            vel = vel.multiply(0.99);
        }
    }
}
