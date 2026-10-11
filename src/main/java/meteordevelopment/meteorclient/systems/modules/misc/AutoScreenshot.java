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
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;

import java.io.File;

/**
 * 自动截图：检测到关键事件（胜利 / 击杀 / 自定义关键词）时自动保存截图，
 * 并在聊天栏提示文件位置。
 */
public class AutoScreenshot extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> triggers = sgGeneral.add(new StringSetting.Builder()
        .name("触发关键词")
        .description("用逗号分隔，聊天中出现任一关键词即截图。")
        .defaultValue("victory,winner,胜利,游戏结束")
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("延迟")
        .description("触发后等待多少 tick 再截图，让画面稳定。")
        .defaultValue(10).min(0).sliderRange(0, 100)
        .build()
    );

    private final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
        .name("冷却")
        .description("两次截图的最小间隔（秒）。")
        .defaultValue(5).min(1).sliderRange(1, 120)
        .build()
    );

    private final Setting<Boolean> notify = sgGeneral.add(new BoolSetting.Builder()
        .name("聊天提示")
        .description("截图后在聊天栏显示保存位置。")
        .defaultValue(true)
        .build()
    );

    private int pendingTicks = -1;
    private long lastShot = 0;

    public AutoScreenshot() {
        super(Categories.Misc, "auto-screenshot", "检测到胜利等关键事件时自动截图。");
    }

    @Override
    public void onActivate() {
        pendingTicks = -1;
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (System.currentTimeMillis() - lastShot < cooldown.get() * 1000L) return;
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
        lastShot = System.currentTimeMillis();
        takeScreenshot();
    }

    /** 手动触发一次截图（也可由其它模块调用）。 */
    public void takeScreenshot() {
        if (mc.getWindow() == null) return;
        try {
            ScreenshotRecorder.saveScreenshot(
                mc.runDirectory,
                mc.getFramebuffer(),
                msg -> {
                    if (notify.get() && mc.player != null) {
                        mc.player.sendMessage(Text.literal("§7[§b自动截图§7] §f" + msg.getString()), false);
                    }
                });
        } catch (Throwable t) {
            if (notify.get()) warning("截图失败：" + t.getMessage());
        }
    }
}
