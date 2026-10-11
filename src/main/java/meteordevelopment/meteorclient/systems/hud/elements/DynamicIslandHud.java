/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.soar.Soar2D;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * 灵动岛：屏幕顶部的胶囊形状态栏。
 *
 * <p>把常用的零散信息（时间、延迟、帧率、玩家数、坐标、血量）收进一个圆角胶囊里，
 * 按开启顺序横向排布，中间用分隔点隔开。玻璃质感来自半透明底色 + 细描边。
 *
 * <p>可选独立位置，配合 HUD 编辑器拖动。
 */
public class DynamicIslandHud extends HudElement {

    public static final HudElementInfo<DynamicIslandHud> INFO = new HudElementInfo<>(
        Hud.GROUP, "dynamic-island", "屏幕顶部的胶囊形状态栏，集中显示时间、延迟、帧率等信息。",
        DynamicIslandHud::new);

    private final SettingGroup sgContent = settings.createGroup("内容");
    private final SettingGroup sgStyle = settings.createGroup("外观");

    private final Setting<Boolean> showTime = sgContent.add(new BoolSetting.Builder()
        .name("显示时间").description("显示本机时间。").defaultValue(true)
        .onChanged(v -> updateSize()).build());

    private final Setting<Boolean> use12Hour = sgContent.add(new BoolSetting.Builder()
        .name("12 小时制").description("时间用 12 小时制。").defaultValue(false)
        .visible(showTime::get).build());

    private final Setting<Boolean> showPing = sgContent.add(new BoolSetting.Builder()
        .name("显示延迟").description("显示与服务器的延迟。").defaultValue(true)
        .onChanged(v -> updateSize()).build());

    private final Setting<Boolean> showFps = sgContent.add(new BoolSetting.Builder()
        .name("显示帧率").description("显示当前帧率。").defaultValue(true)
        .onChanged(v -> updateSize()).build());

    private final Setting<Boolean> showPlayers = sgContent.add(new BoolSetting.Builder()
        .name("显示玩家数").description("显示在线玩家数量。").defaultValue(false)
        .onChanged(v -> updateSize()).build());

    private final Setting<Boolean> showCoords = sgContent.add(new BoolSetting.Builder()
        .name("显示坐标").description("显示当前坐标。").defaultValue(false)
        .onChanged(v -> updateSize()).build());

    private final Setting<Boolean> showHealth = sgContent.add(new BoolSetting.Builder()
        .name("显示血量").description("显示当前血量。").defaultValue(false)
        .onChanged(v -> updateSize()).build());

    private final Setting<Double> scale = sgStyle.add(new DoubleSetting.Builder()
        .name("缩放").description("整体缩放。").defaultValue(1.0).min(0.5).sliderRange(0.5, 3)
        .onChanged(v -> updateSize()).build());

    private final Setting<Double> padding = sgStyle.add(new DoubleSetting.Builder()
        .name("内边距").description("内容与胶囊边缘的距离。").defaultValue(10).min(2).sliderRange(2, 40)
        .onChanged(v -> updateSize()).build());

    private final Setting<Double> gap = sgStyle.add(new DoubleSetting.Builder()
        .name("间隔").description("各段之间的间距。").defaultValue(10).min(2).sliderRange(2, 60)
        .onChanged(v -> updateSize()).build());

    private final Setting<SettingColor> bgColor = sgStyle.add(new ColorSetting.Builder()
        .name("背景颜色").description("胶囊底色。").defaultValue(new SettingColor(16, 18, 24, 190)).build());

    private final Setting<SettingColor> outlineColor = sgStyle.add(new ColorSetting.Builder()
        .name("描边颜色").description("胶囊描边。").defaultValue(new SettingColor(255, 255, 255, 60)).build());

    private final Setting<SettingColor> textColor = sgStyle.add(new ColorSetting.Builder()
        .name("文字颜色").description("文字颜色。").defaultValue(new SettingColor(240, 240, 245)).build());

    private final Setting<SettingColor> accentColor = sgStyle.add(new ColorSetting.Builder()
        .name("强调颜色").description("数值部分的颜色。").defaultValue(new SettingColor(120, 190, 255)).build());

    public DynamicIslandHud() {
        super(INFO);
        updateSize();
    }

    /** 组装当前要显示的片段。 */
    private List<String> segments() {
        List<String> list = new ArrayList<>();

        if (showTime.get()) {
            DateTimeFormatter f = use12Hour.get()
                ? DateTimeFormatter.ofPattern("hh:mm")
                : DateTimeFormatter.ofPattern("HH:mm");
            list.add(LocalTime.now().format(f));
        }
        if (showPing.get()) list.add(pingText());
        if (showFps.get()) list.add(mc.getCurrentFps() + " fps");
        if (showPlayers.get()) {
            int n = mc.getNetworkHandler() == null ? 0 : mc.getNetworkHandler().getPlayerList().size();
            list.add(n + " 人");
        }
        if (showCoords.get() && mc.player != null) {
            list.add(String.format("%.0f, %.0f, %.0f",
                mc.player.getX(), mc.player.getY(), mc.player.getZ()));
        }
        if (showHealth.get() && mc.player != null) {
            list.add(String.format("%.1f ❤", mc.player.getHealth() + mc.player.getAbsorptionAmount()));
        }
        return list;
    }

    private String pingText() {
        if (mc.player == null || mc.getNetworkHandler() == null) return "-- ms";
        var e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return (e == null ? "--" : String.valueOf(e.getLatency())) + " ms";
    }

    /** 估算宽度：按片段数量与内容长度粗算，实际绘制时再对齐。 */
    private void updateSize() {
        int n = Math.max(1, countEnabled());
        double w = padding.get() * 2 + n * 46 + Math.max(0, n - 1) * gap.get();
        double h = 22 + padding.get() * 0.6;
        setSize(w * scale.get(), h * scale.get());
    }

    private int countEnabled() {
        int n = 0;
        if (showTime.get()) n++;
        if (showPing.get()) n++;
        if (showFps.get()) n++;
        if (showPlayers.get()) n++;
        if (showCoords.get()) n++;
        if (showHealth.get()) n++;
        return n;
    }

    @Override
    public void render(HudRenderer renderer) {
        List<String> segs = segments();
        if (segs.isEmpty()) return;

        double s = scale.get();
        double pad = padding.get();
        double g = gap.get();
        double h = 22;

        // 先用渲染器量出真实宽度
        double totalText = 0;
        for (int i = 0; i < segs.size(); i++) {
            totalText += renderer.textWidth(segs.get(i), true, s);
            if (i < segs.size() - 1) totalText += g;
        }

        double w = totalText + pad * 2;
        double boxH = h + pad * 0.6;

        // 让 HUD 的碰撞箱与实际尺寸一致，方便编辑器拖动
        setSize(w, boxH);

        double x = getX();
        double y = getY();

        // 玻璃胶囊：半透明底 + 细描边 + 圆角
        double radius = boxH / 2;
        Soar2D.roundedRect(x, y, w, boxH, radius, bgColor.get());
        // 用一圈略小的描边模拟玻璃边缘
        Soar2D.outline(x, y, w, boxH, 1, outlineColor.get());

        // 逐段绘制
        double cx = x + pad;
        double cy = y + (boxH - renderer.textHeight(true, s)) / 2;

        for (int i = 0; i < segs.size(); i++) {
            String t = segs.get(i);
            Color c = (i == 0 && showTime.get()) ? accentColor.get() : textColor.get();
            renderer.text(t, cx, cy, c, true, s);
            cx += renderer.textWidth(t, true, s);

            if (i < segs.size() - 1) {
                // 分隔点
                double dotR = 1.6 * s;
                double dotX = cx + g / 2 - dotR;
                double dotY = y + boxH / 2 - dotR;
                Soar2D.roundedRect(dotX, dotY, dotR * 2, dotR * 2, dotR,
                    Soar2D.alpha(textColor.get(), 0.45));
                cx += g;
            }
        }
    }
}
