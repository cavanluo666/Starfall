/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

/**
 * 主手辅助：使用盾牌 / 消耗品后，自动把主手切回武器。
 */
public class MainHandAssist extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> meleeWeapon = sgGeneral.add(new BoolSetting.Builder()
        .name("切回近战武器")
        .description("用完副手物品后切回主手的武器。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> shield = sgGeneral.add(new BoolSetting.Builder()
        .name("自动举盾")
        .description("切到盾牌时保持举起。")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> quickUse = sgGeneral.add(new BoolSetting.Builder()
        .name("快捷使用")
        .description("允许快速使用副手物品。")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> switchBack = sgGeneral.add(new BoolSetting.Builder()
        .name("自动切回")
        .description("副手动作结束后自动切回原来的槽位。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> switchDelay = sgGeneral.add(new IntSetting.Builder()
        .name("切换延迟")
        .description("切换回原槽位前的等待 tick 数。")
        .defaultValue(2).min(0).sliderRange(0, 20)
        .visible(switchBack::get)
        .build()
    );

    private int savedSlot = -1;
    private int waitTicks = 0;

    public MainHandAssist() {
        super(Categories.Player, "main-hand-assist", "使用盾牌或消耗品后，自动把主手切回武器，减少手动切换。");
    }

    @Override
    public void onActivate() {
        savedSlot = -1;
        waitTicks = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        boolean usingOffhand = mc.player.isUsingItem() && mc.player.getActiveHand() == Hand.OFF_HAND;
        boolean holdingShield = mc.player.getOffHandStack().isOf(Items.SHIELD)
                || mc.player.getMainHandStack().isOf(Items.SHIELD);

        if (usingOffhand && savedSlot < 0) {
            savedSlot = mc.player.getInventory().selectedSlot;
        }

        if (usingOffhand) {
            waitTicks = switchDelay.get();
            return;
        }

        if (savedSlot >= 0) {
            if (waitTicks > 0) {
                waitTicks--;
                return;
            }
            if (switchBack.get() && savedSlot != mc.player.getInventory().selectedSlot) {
                mc.player.getInventory().selectedSlot = savedSlot;
            }
            savedSlot = -1;
        }

        // 自动举盾
        if (shield.get() && holdingShield && !mc.player.isUsingItem()) {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
        }
    }

    /** 当前主手是否武器（供其它功能判断，如快捷使用）。 */
    public boolean hasMeleeWeapon() {
        if (mc.player == null) return false;
        ItemStack s = mc.player.getMainHandStack();
        return !s.isEmpty();
    }
}
