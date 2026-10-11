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

/**
 * 服务器自动登录：检测到登录提示时自动发送密码。
 *
 * <p>规则格式：{@code 关键词:密码}，多条用 {@code ;} 分隔。
 * 例如 {@code /login:MyPass123;/register:MyPass123}。
 * 密码保存在本地配置，不会上传。
 */
public class ServerAutoLogin extends Module {

    public enum Mode {
        /** 匹配规则里的关键词后发送对应命令 */
        按规则,
        /** 固定发送 /login <密码> */
        固定登录
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("模式")
        .description("使用规则匹配还是固定命令。")
        .defaultValue(Mode.按规则)
        .build()
    );

    private final Setting<String> password = sgGeneral.add(new StringSetting.Builder()
        .name("密码")
        .description("登录密码。")
        .defaultValue("")
        .visible(() -> mode.get() == Mode.固定登录)
        .build()
    );

    private final Setting<String> rules = sgGeneral.add(new StringSetting.Builder()
        .name("规则")
        .description("格式 关键词:命令，用分号分隔。")
        .defaultValue("/login:;请输入密码:/login ")
        .visible(() -> mode.get() == Mode.按规则)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("延迟")
        .description("检测到提示后等待多少 tick 再发送。")
        .defaultValue(10).min(0).sliderRange(0, 100)
        .build()
    );

    private final Setting<Boolean> once = sgGeneral.add(new BoolSetting.Builder()
        .name("只登录一次")
        .description("同一服务器只自动登录一次，避免重复发送。")
        .defaultValue(true)
        .build()
    );

    private String pending = null;
    private int waitTicks = 0;
    private boolean sentThisSession = false;

    public ServerAutoLogin() {
        super(Categories.Misc, "server-auto-login", "检测到登录提示时自动发送登录命令。");
    }

    @Override
    public void onActivate() {
        pending = null;
        sentThisSession = false;
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (mc.player == null) return;
        if (once.get() && sentThisSession) return;

        String msg = event.getMessage().getString();

        if (mode.get() == Mode.固定登录) {
            // 常见的登录提示词
            if (msg.contains("/login") || msg.contains("请输入密码") || msg.contains("please login")) {
                waitTicks = delay.get();
                pending = "/login " + password.get();
            }
            return;
        }

        for (String rule : rules.get().split(";")) {
            int idx = rule.indexOf(':');
            if (idx <= 0) continue;
            String key = rule.substring(0, idx).trim();
            String cmd = rule.substring(idx + 1).trim();
            if (key.isEmpty()) continue;

            if (msg.toLowerCase().contains(key.toLowerCase())) {
                waitTicks = delay.get();
                pending = cmd;
                return;
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (pending == null) return;
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        String cmd = pending;
        pending = null;

        if (mc.getNetworkHandler() == null || cmd == null || cmd.isBlank()) return;

        if (cmd.startsWith("/")) {
            mc.getNetworkHandler().sendChatCommand(cmd.substring(1));
        } else {
            mc.getNetworkHandler().sendChatMessage(cmd);
        }
        sentThisSession = true;
    }
}
