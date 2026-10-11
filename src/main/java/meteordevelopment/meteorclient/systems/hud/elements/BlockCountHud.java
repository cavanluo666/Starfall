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

import java.util.HashMap;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * 方块数量显示：统计背包里指定方块的数量。
 *
 * <p>适合挖矿场景 —— 一眼看到手里/背包里目标方块攒了多少。
 * 默认统计常见的矿物与建筑材料，可自定义列表。
 */
public class BlockCountHud extends HudElement {

    public static final HudElementInfo<BlockCountHud> INFO = new HudElementInfo<>(
        Hud.GROUP, "block-count", "统计背包内指定方块的数量。", BlockCountHud::new);

    public enum Layout {
        列表,
        单行
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Layout> layout = sgGeneral.add(new EnumSetting.Builder<Layout>()
        .name("布局").description("多行列表或单行横排。").defaultValue(Layout.列表)
        .onChanged(v -> updateSize()).build());

    private final Setting<String> blocks = sgGeneral.add(new StringSetting.Builder()
        .name("方块列表")
        .description("用逗号分隔的物品 id，留空则统计全部方块。")
        .defaultValue("diamond,netherite_scrap,ancient_debris,emerald,gold_ingot,iron_ingot")
        .onChanged(v -> updateSize()).build());

    private final Setting<Boolean> showTotal = sgGeneral.add(new BoolSetting.Builder()
        .name("显示总数").description("额外显示所有统计方块的总数。").defaultValue(true)
        .onChanged(v -> updateSize()).build());

    private final Setting<Boolean> background = sgGeneral.add(new BoolSetting.Builder()
        .name("背景").description("绘制半透明底衬。").defaultValue(true).build());

    private final Setting<SettingColor> nameColor = sgGeneral.add(new ColorSetting.Builder()
        .name("名称颜色").description("方块名称颜色。").defaultValue(new SettingColor(200, 200, 200)).build());

    private final Setting<SettingColor> countColor = sgGeneral.add(new ColorSetting.Builder()
        .name("数量颜色").description("数量颜色。").defaultValue(new SettingColor(120, 220, 140)).build());

    private final Setting<SettingColor> bgColor = sgGeneral.add(new ColorSetting.Builder()
        .name("背景颜色").description("底衬颜色。").defaultValue(new SettingColor(0, 0, 0, 120))
        .visible(background::get).build());

    public BlockCountHud() {
        super(INFO);
        updateSize();
    }

    private void updateSize() {
        if (layout.get() == Layout.单行) {
            setSize(240, 16);
        } else {
            setSize(150, Math.max(1, rows()) * 13);
        }
    }

    private int rows() {
        return Math.max(1, collect().size() + (showTotal.get() ? 1 : 0));
    }

    /** 统计结果：显示名 -> 数量。 */
    private Map<String, Integer> collect() {
        Map<String, Integer> result = new java.util.LinkedHashMap<>();
        if (mc.player == null) return result;

        String filter = blocks.get().trim().toLowerCase();
        boolean all = filter.isEmpty();

        Map<String, Integer> counts = new HashMap<>();
        var inv = mc.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            var st = inv.getStack(i);
            if (st.isEmpty()) continue;
            String id = net.minecraft.registry.Registries.ITEM.getId(st.getItem()).getPath();

            if (!all) {
                boolean match = false;
                for (String want : filter.split(",")) {
                    String w = want.trim();
                    if (!w.isEmpty() && id.equals(w)) { match = true; break; }
                }
                if (!match) continue;
            } else {
                // 全部模式只统计方块类物品
                if (!(st.getItem() instanceof net.minecraft.item.BlockItem)) continue;
            }

            counts.merge(id, st.getCount(), Integer::sum);
        }

        // 保持用户提供的顺序
        if (!all) {
            for (String want : filter.split(",")) {
                String w = want.trim();
                if (w.isEmpty()) continue;
                int n = counts.getOrDefault(w, 0);
                if (n > 0) result.put(w, n);
            }
        } else {
            counts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(12)
                .forEach(e -> result.put(e.getKey(), e.getValue()));
        }
        return result;
    }

    @Override
    public void render(HudRenderer renderer) {
        Map<String, Integer> data = collect();
        if (data.isEmpty()) return;

        double x = getX();
        double y = getY();

        if (background.get()) {
            renderer.quad(x, y, getWidth(), getHeight(), bgColor.get());
        }

        if (layout.get() == Layout.单行) {
            double cx = x + 3;
            double cy = y + 2;
            boolean first = true;
            for (var e : data.entrySet()) {
                if (!first) {
                    renderer.text("|", cx, cy, Soar2D.alpha(nameColor.get(), 0.4), true, -1);
                    cx += renderer.textWidth("|", true, -1) + 4;
                }
                first = false;
                String s = pretty(e.getKey()) + " " + e.getValue();
                renderer.text(s, cx, cy, countColor.get(), true, -1);
                cx += renderer.textWidth(s, true, -1) + 6;
            }
            return;
        }

        double ly = y + 1;
        for (var e : data.entrySet()) {
            String name = pretty(e.getKey());
            renderer.text(name, x + 3, ly, nameColor.get(), true, -1);
            String c = String.valueOf(e.getValue());
            double cw = renderer.textWidth(c, true, -1);
            renderer.text(c, x + getWidth() - cw - 3, ly, countColor.get(), true, -1);
            ly += 13;
        }

        if (showTotal.get()) {
            int total = data.values().stream().mapToInt(Integer::intValue).sum();
            renderer.text("合计", x + 3, ly, nameColor.get(), true, -1);
            String c = String.valueOf(total);
            double cw = renderer.textWidth(c, true, -1);
            renderer.text(c, x + getWidth() - cw - 3, ly, countColor.get(), true, -1);
        }
    }

    /** 把 item id 转成易读的中文/英文名。 */
    private String pretty(String id) {
        var item = net.minecraft.registry.Registries.ITEM.get(
            net.minecraft.util.Identifier.of("minecraft", id));
        if (item != null && item != net.minecraft.item.Items.AIR) {
            return item.getName().getString();
        }
        return id.replace('_', ' ');
    }
}
