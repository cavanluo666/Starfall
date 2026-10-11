/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

/**
 * 自动存入箱子：打开箱子后把背包里的物品自动搬进去。
 *
 * <p>可选"只存资源"，避免把武器装备也丢进去。操作有节流，
 * 每个 tick 只移动一件，防止被服务器判定为异常。
 */
public class AutoChestDeposit extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> resourcesOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("只存资源")
        .description("只存方块、矿物等资源，保留装备与工具。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> depositDelay = sgGeneral.add(new IntSetting.Builder()
        .name("存入间隔")
        .description("每移动一件物品后等待多少 tick。")
        .defaultValue(2).min(1).sliderRange(1, 20)
        .build()
    );

    private final Setting<Boolean> closeWhenDone = sgGeneral.add(new BoolSetting.Builder()
        .name("完成后关闭")
        .description("全部存完后自动关闭箱子界面。")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> closeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("关闭延迟")
        .description("存完后等待多少 tick 再关闭。")
        .defaultValue(4).min(0).sliderRange(0, 40)
        .visible(closeWhenDone::get)
        .build()
    );

    private int cooldown = 0;
    private boolean done = false;

    public AutoChestDeposit() {
        super(Categories.Misc, "auto-chest-deposit", "打开箱子后自动把背包物品存入。");
    }

    @Override
    public void onActivate() {
        cooldown = 0;
        done = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) {
            // 离开箱子界面后重置
            done = false;
            return;
        }

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        if (done) {
            if (closeWhenDone.get() && closeDelay.get() > 0) {
                closeDelayTick();
            } else if (closeWhenDone.get()) {
                mc.player.closeHandledScreen();
            }
            return;
        }

        int containerSlots = handler.getInventory().size();
        int totalSlots = handler.slots.size();
        int playerStart = totalSlots - 36;

        // 箱子里的空位
        int target = -1;
        for (int i = 0; i < containerSlots; i++) {
            if (handler.getSlot(i).getStack().isEmpty()) { target = i; break; }
        }
        if (target < 0) { done = true; return; }

        // 找一个可存入的背包槽位（hotbar 36..44，主背包 9..35）
        int source = -1;
        for (int i = playerStart; i < totalSlots; i++) {
            ItemStack s = handler.getSlot(i).getStack();
            if (s.isEmpty()) continue;
            if (resourcesOnly.get() && !isResource(s)) continue;
            source = i;
            break;
        }

        if (source < 0) { done = true; return; }

        // quickMove：Shift 点击会把整叠移到对侧
        mc.interactionManager.clickSlot(handler.syncId, source, 0, SlotActionType.QUICK_MOVE, mc.player);
        cooldown = depositDelay.get();
    }

    private int closeDelayLeft = -1;

    private void closeDelayTick() {
        if (closeDelayLeft < 0) closeDelayLeft = closeDelay.get();
        if (closeDelayLeft-- <= 0 && mc.player != null) {
            mc.player.closeHandledScreen();
            closeDelayLeft = -1;
        }
    }

    /** 粗略判断是否为"资源"类物品。 */
    private boolean isResource(ItemStack s) {
        String id = net.minecraft.registry.Registries.ITEM.getId(s.getItem()).getPath();
        // 排除明显是装备/工具的
        if (id.endsWith("_sword") || id.endsWith("_pickaxe") || id.endsWith("_axe")
            || id.endsWith("_shovel") || id.endsWith("_hoe") || id.endsWith("_helmet")
            || id.endsWith("_chestplate") || id.endsWith("_leggings") || id.endsWith("_boots")
            || id.equals("shield") || id.equals("elytra") || id.equals("bow")
            || id.equals("crossbow") || id.equals("trident") || id.equals("fishing_rod")) {
            return false;
        }
        return true;
    }
}
