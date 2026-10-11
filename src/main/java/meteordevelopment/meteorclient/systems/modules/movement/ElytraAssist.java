/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

/**
 * 鞘翅辅助：坠落时自动展开鞘翅，并在滑翔中自动补充烟花。
 */
public class ElytraAssist extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> autoDeploy = sgGeneral.add(new BoolSetting.Builder()
        .name("自动展开")
        .description("空中坠落时自动使用鞘翅。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> deployHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("触发高度")
        .description("距地面多少格内不展开，避免贴地乱飞。")
        .defaultValue(3.0).min(0).sliderRange(0, 30)
        .visible(autoDeploy::get)
        .build()
    );

    private final Setting<Boolean> autoFirework = sgGeneral.add(new BoolSetting.Builder()
        .name("自动放烟花")
        .description("滑翔且水平速度过低时自动使用烟花推进。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> minSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("最低速度")
        .description("水平速度低于该值时补充烟花。")
        .defaultValue(0.6).min(0).sliderRange(0, 3)
        .visible(autoFirework::get)
        .build()
    );

    private final Setting<Integer> fireworkDelay = sgGeneral.add(new IntSetting.Builder()
        .name("烟花间隔")
        .description("两次放烟花之间至少间隔多少 tick。")
        .defaultValue(10).min(1).sliderRange(1, 100)
        .visible(autoFirework::get)
        .build()
    );

    private int fireworkCooldown = 0;

    public ElytraAssist() {
        super(Categories.Movement, "elytra-assist", "坠落时自动展开鞘翅，滑翔中自动补充烟花。");
    }

    @Override
    public void onActivate() {
        fireworkCooldown = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        if (fireworkCooldown > 0) fireworkCooldown--;

        boolean hasElytra = mc.player.getInventory().armor.get(2).isOf(Items.ELYTRA);
        if (!hasElytra) return;

        // 自动展开
        if (autoDeploy.get() && !mc.player.isFallFlying() && !mc.player.isOnGround()
                && mc.player.getVelocity().y < -0.1) {

            // 粗略估计离地高度
            double groundY = mc.player.getY();
            for (int i = 1; i < 64; i++) {
                var bp = mc.player.getBlockPos().down(i);
                if (!mc.world.getBlockState(bp).isAir()) { groundY = bp.getY() + 1; break; }
            }
            double above = mc.player.getY() - groundY;

            if (above > deployHeight.get()) {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            }
        }

        // 自动烟花
        if (autoFirework.get() && mc.player.isFallFlying() && fireworkCooldown == 0) {
            double hSpeed = Math.sqrt(
                mc.player.getVelocity().x * mc.player.getVelocity().x
                + mc.player.getVelocity().z * mc.player.getVelocity().z);

            if (hSpeed < minSpeed.get() && isHoldingFirework()) {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                fireworkCooldown = fireworkDelay.get();
            }
        }
    }

    private boolean isHoldingFirework() {
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().isOf(Items.FIREWORK_ROCKET)
            || mc.player.getOffHandStack().isOf(Items.FIREWORK_ROCKET);
    }
}
