/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * 自动 GG：对局结束时在聊天栏自动发送 gg。
 *
 * <p>触发方式：监听聊天中出现的关键词（如 "Winner"、"比赛结束"），
 * 延迟若干 tick 后发送，避免与其它自动消息撞车。
 */
public class AutoGG extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> text = sgGeneral.add(new StringSetting.Builder()
        .name("发送内容")
        .description("要发送的文本。")
        .defaultValue("gg")
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("延迟")
        .description("检测到结束语后等待多少 tick 再发送。")
        .defaultValue(20).min(0).sliderRange(0, 100)
        .build()
    );

    private final Setting<String> triggers = sgGeneral.add(new StringSetting.Builder()
        .name("触发关键词")
        .description("用逗号分隔的触发词（匹配聊天内容）。")
        .defaultValue("gg,winner,victory,游戏结束,胜利")
        .build()
    );

    private final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
        .name("冷却")
        .description("两次发送之间的最小间隔（秒）。")
        .defaultValue(10).min(1).sliderRange(1, 120)
        .build()
    );

    private int pendingTicks = -1;
    private long lastSend = 0;

    public AutoGG() {
        super(Categories.Misc, "auto-gg", "对局结束时自动在聊天栏发送 gg。");
    }

    @Override
    public void onActivate() {
        pendingTicks = -1;
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (mc.player == null) return;
        if (System.currentTimeMillis() - lastSend < cooldown.get() * 1000L) return;
        if (pendingTicks >= 0) return;

        String msg = event.getMessage().getString().toLowerCase();
        for (String t : triggers.get().split(",")) {
            String k = t.trim().toLowerCase();
            if (!k.isEmpty() && msg.contains(k)) {
                pendingTicks = delay.get();
                return;
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (pendingTicks < 0) return;
        if (pendingTicks > 0) {
            pendingTicks--;
            return;
        }

        pendingTicks = -1;
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        String t = text.get().trim();
        if (t.isEmpty()) return;

        if (t.startsWith("/")) {
            mc.getNetworkHandler().sendChatCommand(t.substring(1));
        } else {
            mc.getNetworkHandler().sendChatMessage(t);
        }
        lastSend = System.currentTimeMillis();
    }
}
