/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import java.time.LocalTime;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import java.time.format.DateTimeFormatter;

/**
 * 服务器信息：延迟 / TPS / 本地时钟。
 * 三行内容可各自开关，行距自适应。
 */
public class ServerInfoHud extends HudElement {

    public static final HudElementInfo<ServerInfoHud> INFO = new HudElementInfo<>(
        Hud.GROUP, "server-info", "显示延迟、服务器 TPS 与本机时间。", ServerInfoHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> showPing = sgGeneral.add(new BoolSetting.Builder()
        .name("显示延迟")
        .description("显示与服务器的延迟。")
        .defaultValue(true)
        .onChanged(v -> updateSize())
        .build()
    );

    private final Setting<Boolean> showTps = sgGeneral.add(new BoolSetting.Builder()
        .name("显示 TPS")
        .description("估算服务器每秒刻数。")
        .defaultValue(true)
        .onChanged(v -> updateSize())
        .build()
    );

    private final Setting<Boolean> showClock = sgGeneral.add(new BoolSetting.Builder()
        .name("显示时钟")
        .description("显示本机当前时间。")
        .defaultValue(true)
        .onChanged(v -> updateSize())
        .build()
    );

    private final Setting<Boolean> use12Hour = sgGeneral.add(new BoolSetting.Builder()
        .name("12 小时制")
        .description("时钟使用 12 小时制。")
        .defaultValue(false)
        .visible(showClock::get)
        .build()
    );

    private final Setting<Boolean> background = sgGeneral.add(new BoolSetting.Builder()
        .name("背景")
        .description("绘制半透明底衬。")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> labelColor = sgGeneral.add(new ColorSetting.Builder()
        .name("标签颜色")
        .description("标签文字颜色。")
        .defaultValue(new SettingColor(160, 160, 160))
        .build()
    );

    private final Setting<SettingColor> valueColor = sgGeneral.add(new ColorSetting.Builder()
        .name("数值颜色")
        .description("数值文字颜色。")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    private final Setting<SettingColor> bgColor = sgGeneral.add(new ColorSetting.Builder()
        .name("背景颜色")
        .description("底衬颜色。")
        .defaultValue(new SettingColor(0, 0, 0, 100))
        .visible(background::get)
        .build()
    );

    /** TPS 估算：统计每秒实际 tick 数 */
    private long lastTickTime = 0;
    private int ticksThisSecond = 0;
    private double tps = 20.0;

    public ServerInfoHud() {
        super(INFO);
        updateSize();
    }

    private void updateSize() {
        int lines = lineCount();
        setSize(110, Math.max(1, lines) * (textHeight() + 2));
    }

    private int lineCount() {
        int n = 0;
        if (showPing.get()) n++;
        if (showTps.get()) n++;
        if (showClock.get()) n++;
        return n;
    }

    private double textHeight() {
        return 10;
    }

    @Override
    public void tick(HudRenderer renderer) {
        // 每秒统计一次实际 tick 数，作为 TPS 估算
        long now = System.currentTimeMillis();
        ticksThisSecond++;
        if (lastTickTime == 0) lastTickTime = now;
        if (now - lastTickTime >= 1000) {
            tps = ticksThisSecond * 1000.0 / (now - lastTickTime);
            tps = Math.min(20.0, tps);
            ticksThisSecond = 0;
            lastTickTime = now;
        }
    }

    @Override
    public void render(HudRenderer renderer) {
        int lines = lineCount();
        if (lines == 0) return;

        double lineH = renderer.textHeight(true, -1) + 2;
        double boxH = lines * lineH;
        double boxW = getWidth();

        if (background.get()) {
            renderer.quad(getX(), getY(), boxW, boxH, bgColor.get());
        }

        double y = getY() + 1;

        if (showPing.get()) {
            int ping = 0;
            if (mc.player != null && mc.getNetworkHandler() != null) {
                var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (entry != null) ping = entry.getLatency();
            }
            drawLine(renderer, "延迟", ping + " ms", y, lineH);
            y += lineH;
        }

        if (showTps.get()) {
            drawLine(renderer, "TPS", String.format("%.1f", tps), y, lineH);
            y += lineH;
        }

        if (showClock.get()) {
            DateTimeFormatter f = use12Hour.get()
                ? DateTimeFormatter.ofPattern("hh:mm:ss a")
                : DateTimeFormatter.ofPattern("HH:mm:ss");
            drawLine(renderer, "时钟", LocalTime.now().format(f), y, lineH);
        }
    }

    private void drawLine(HudRenderer renderer, String label, String value, double y, double lineH) {
        double x = getX() + 2;
        renderer.text(label, x, y, labelColor.get(), true, -1);
        double lw = renderer.textWidth(label, true, -1);
        renderer.text(value, x + lw + 6, y, valueColor.get(), true, -1);
    }
}
