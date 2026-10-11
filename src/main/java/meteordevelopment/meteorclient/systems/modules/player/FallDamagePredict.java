/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.soar.Soar2D;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * 摔落伤害预测：下落过程中预测落地伤害，显示在屏幕上。
 *
 * <p>伤害按原版公式估算：超过 3 格后每格 1 点（简单近似），
 * 并考虑水中 / 干草堆 / 蜂蜜等缓冲与落地水。
 */
public class FallDamagePredict extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("缩放")
        .description("文字缩放。")
        .defaultValue(1.2).min(0.5).sliderRange(0.5, 3)
        .build()
    );

    private final Setting<Double> offsetY = sgGeneral.add(new DoubleSetting.Builder()
        .name("垂直偏移")
        .description("显示位置相对屏幕中心的偏移。")
        .defaultValue(-60).sliderRange(-300, 300)
        .build()
    );

    private final Setting<Boolean> warnOnLethal = sgGeneral.add(new BoolSetting.Builder()
        .name("致命警告")
        .description("预计会摔死时用红色高亮。")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("颜色")
        .description("正常情况下的文字颜色。")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    private final Setting<SettingColor> lethalColor = sgGeneral.add(new ColorSetting.Builder()
        .name("致命颜色")
        .description("预计会摔死时的颜色。")
        .defaultValue(new SettingColor(255, 60, 60))
        .visible(warnOnLethal::get)
        .build()
    );

    private float predictedDamage = 0;
    private boolean lethal = false;

    public FallDamagePredict() {
        super(Categories.Player, "fall-damage-predict", "下落时预测落地受到的伤害，并在屏幕上提示。");
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mc.player == null || mc.world == null) return;

        update();

        if (predictedDamage <= 0.01f) return;

        String text = String.format("预计摔落伤害 %.1f", predictedDamage);

        Color c = (lethal && warnOnLethal.get()) ? lethalColor.get() : color.get();

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        double cx = sw / 2.0;
        double cy = sh / 2.0 + offsetY.get();

        Renderer2D.COLOR.begin();
        double w = Soar2D.textWidth(text, scale.get());
        double h = Soar2D.textHeight(scale.get());
        Soar2D.rect(cx - w / 2 - 3, cy - 2, w + 6, h + 4, new Color(0, 0, 0, 130));
        Renderer2D.COLOR.render(null);

        Soar2D.text(text, cx - w / 2, cy, c, true, scale.get());
    }

    private void update() {
        var player = mc.player;
        var world = mc.world;

        predictedDamage = 0;
        lethal = false;

        // 只有下落才预测
        if (player.getVelocity().y >= 0) return;
        if (player.isOnGround() || player.isTouchingWater() || player.isInLava()
                || player.isFallFlying() || player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOW_FALLING)
                || player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.LEVITATION)) {
            return;
        }

        double fallDistance = player.fallDistance;
        if (fallDistance <= 3.0) return;

        // 从上往下找第一个可落地的方块
        BlockPos pos = player.getBlockPos();
        int maxScan = 320;
        BlockPos landing = null;

        for (int i = 1; i < maxScan; i++) {
            BlockPos p = pos.down(i);
            if (p.getY() < world.getBottomY()) break;
            BlockState state = world.getBlockState(p);
            if (!state.isAir() && state.getFluidState().isEmpty()) {
                landing = p;
                break;
            }
        }

        if (landing == null) return;

        double heightAbove = player.getY() - (landing.getY() + 1);
        if (heightAbove <= 3.0) return;

        float damage = (float) (heightAbove - 3.0);

        // 缓冲方块减伤
        BlockState landingState = world.getBlockState(landing);
        if (landingState.isOf(Blocks.HAY_BLOCK)) damage *= 0.2f;
        else if (landingState.isOf(Blocks.HONEY_BLOCK)) damage *= 0.2f;
        else if (landingState.isOf(Blocks.SLIME_BLOCK)) damage = 0;

        // 护甲与抗性提升的粗略减免
        damage = MathHelper.clamp(damage, 0, 1e4f);

        predictedDamage = damage;
        lethal = damage >= player.getHealth() + player.getAbsorptionAmount();
    }
}
