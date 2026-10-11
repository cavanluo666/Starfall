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

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * 物品使用状态：显示吃东西 / 喝药 / 拉弓的进度条与剩余时间。
 */
public class ItemUseStatusHud extends HudElement {

    public static final HudElementInfo<ItemUseStatusHud> INFO = new HudElementInfo<>(
        Hud.GROUP, "item-use-status", "显示当前正在使用物品的进度与剩余时间。", ItemUseStatusHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> width = sgGeneral.add(new DoubleSetting.Builder()
        .name("宽度")
        .description("进度条宽度。")
        .defaultValue(120).min(40).sliderRange(40, 400)
        .onChanged(v -> updateSize())
        .build()
    );

    private final Setting<Double> height = sgGeneral.add(new DoubleSetting.Builder()
        .name("高度")
        .description("进度条高度。")
        .defaultValue(8).min(4).sliderRange(4, 30)
        .onChanged(v -> updateSize())
        .build()
    );

    private final Setting<Boolean> showName = sgGeneral.add(new BoolSetting.Builder()
        .name("显示物品名")
        .description("在进度条上方显示物品名称。")
        .defaultValue(true)
        .onChanged(v -> updateSize())
        .build()
    );

    private final Setting<SettingColor> barColor = sgGeneral.add(new ColorSetting.Builder()
        .name("进度颜色")
        .description("进度条填充颜色。")
        .defaultValue(new SettingColor(80, 180, 255))
        .build()
    );

    private final Setting<SettingColor> bgColor = sgGeneral.add(new ColorSetting.Builder()
        .name("背景颜色")
        .description("进度条底色。")
        .defaultValue(new SettingColor(0, 0, 0, 140))
        .build()
    );

    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder()
        .name("文字颜色")
        .description("文字颜色。")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    public ItemUseStatusHud() {
        super(INFO);
        updateSize();
    }

    private void updateSize() {
        double h = height.get() + (showName.get() ? 12 : 0);
        setSize(width.get(), h);
    }

    @Override
    public void render(HudRenderer renderer) {
        if (mc.player == null) return;

        // 只有正在使用物品且使用时间大于 0 才显示
        if (!mc.player.isUsingItem()) return;
        int useTicks = mc.player.getItemUseTime();
        if (useTicks <= 0) return;

        var stack = mc.player.getActiveItem();
        if (stack.isEmpty()) return;

        // 估算总时长：常见情况吃药/食物 32 tick，拉弓 20 tick
        int total = 32;
        if (stack.getMaxUseTime(mc.player) > 0) total = stack.getMaxUseTime(mc.player);

        double progress = Math.min(1.0, (double) useTicks / total);

        double x = getX();
        double y = getY();

        if (showName.get()) {
            String name = stack.getName().getString();
            renderer.text(name, x, y, textColor.get(), true, -1);
            y += 12;
        }

        double w = width.get();
        double h = height.get();

        renderer.quad(x, y, w, h, bgColor.get());
        renderer.quad(x, y, w * progress, h, barColor.get());

        // 剩余秒数
        double remain = Math.max(0, (total - useTicks) / 20.0);
        String t = String.format("%.1fs", remain);
        renderer.text(t, x + w + 4, y - 2, textColor.get(), true, -1);
    }
}
