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

import java.util.ArrayList;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import java.util.List;

/**
 * 按键显示：WASD + 鼠标左右键 + 空格，按下时高亮。
 */
public class KeystrokesHud extends HudElement {

    public static final HudElementInfo<KeystrokesHud> INFO = new HudElementInfo<>(
        Hud.GROUP, "keystrokes", "显示当前按下的移动键与鼠标键。", KeystrokesHud::new);

    public enum Style {
        简洁,
        带背景
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Style> style = sgGeneral.add(new EnumSetting.Builder<Style>()
        .name("样式")
        .description("按键格子的样式。")
        .defaultValue(Style.带背景)
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("缩放")
        .description("整体缩放。")
        .defaultValue(1.0).min(0.5).sliderRange(0.5, 3)
        .onChanged(v -> calculateSize())
        .build()
    );

    private final Setting<Double> keySize = sgGeneral.add(new DoubleSetting.Builder()
        .name("按键大小")
        .description("单个按键格子的边长。")
        .defaultValue(16).min(8).sliderRange(8, 48)
        .onChanged(v -> calculateSize())
        .build()
    );

    private final Setting<Double> spacing = sgGeneral.add(new DoubleSetting.Builder()
        .name("间距")
        .description("按键格子之间的间距。")
        .defaultValue(2).min(0).sliderRange(0, 12)
        .onChanged(v -> calculateSize())
        .build()
    );

    private final Setting<Boolean> showMouse = sgGeneral.add(new BoolSetting.Builder()
        .name("显示鼠标键")
        .description("显示左右键。")
        .defaultValue(true)
        .onChanged(v -> calculateSize())
        .build()
    );

    private final Setting<Boolean> showSpace = sgGeneral.add(new BoolSetting.Builder()
        .name("显示空格")
        .description("显示空格键。")
        .defaultValue(true)
        .onChanged(v -> calculateSize())
        .build()
    );

    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder()
        .name("文字颜色")
        .description("未按下时的文字颜色。")
        .defaultValue(new SettingColor(200, 200, 200))
        .build()
    );

    private final Setting<SettingColor> pressedColor = sgGeneral.add(new ColorSetting.Builder()
        .name("按下颜色")
        .description("按下时的背景颜色。")
        .defaultValue(new SettingColor(80, 160, 255))
        .build()
    );

    private final Setting<SettingColor> bgColor = sgGeneral.add(new ColorSetting.Builder()
        .name("背景颜色")
        .description("未按下时的背景颜色。")
        .defaultValue(new SettingColor(0, 0, 0, 120))
        .build()
    );

    public KeystrokesHud() {
        super(INFO);
        calculateSize();
    }

    /** 按键布局描述：{标签, 列, 行, 宽倍数}。 */
    private List<Key> keys() {
        List<Key> list = new ArrayList<>();
        list.add(new Key("W", 1, 0, 1));
        list.add(new Key("A", 0, 1, 1));
        list.add(new Key("S", 1, 1, 1));
        list.add(new Key("D", 2, 1, 1));
        int row = 2;
        if (showSpace.get()) {
            list.add(new Key("空格", 0, row, 3));
            row++;
        }
        if (showMouse.get()) {
            list.add(new Key("左键", 0, row, 2));
            list.add(new Key("右键", 2, row, 1));
        }
        return list;
    }

    private void calculateSize() {
        double s = keySize.get();
        double sp = spacing.get();
        int maxCol = 3;
        int rows = 2 + (showSpace.get() ? 1 : 0) + (showMouse.get() ? 1 : 0);
        setSize(maxCol * s + (maxCol - 1) * sp, Math.max(1, rows) * s + (rows - 1) * sp);
    }

    @Override
    public void render(HudRenderer renderer) {
        double s = keySize.get();
        double sp = spacing.get();

        List<Key> ks = keys();

        for (Key k : ks) {
            double x = getX() + k.col * (s + sp);
            double y = getY() + k.row * (s + sp);
            double w = k.span * s + (k.span - 1) * sp;

            boolean down = isDown(k.label);

            Color bg = down ? pressedColor.get() : bgColor.get();
            Color fg = down ? new Color(255, 255, 255) : textColor.get();

            renderer.quad(x, y, w, s, bg);

            String label = k.label;
            // 用 renderer 测量，保证与绘制一致
            double tw = renderer.textWidth(label, true, -1);
            double th = renderer.textHeight(true, -1);

            renderer.text(label, x + (w - tw) / 2, y + (s - th) / 2, fg, true, -1);
        }
    }

    private boolean isDown(String label) {
        if (mc == null || mc.getWindow() == null) return false;
        long h = mc.getWindow().getHandle();

        return switch (label) {
            case "W" -> org.lwjgl.glfw.GLFW.glfwGetKey(h, org.lwjgl.glfw.GLFW.GLFW_KEY_W) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            case "A" -> org.lwjgl.glfw.GLFW.glfwGetKey(h, org.lwjgl.glfw.GLFW.GLFW_KEY_A) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            case "S" -> org.lwjgl.glfw.GLFW.glfwGetKey(h, org.lwjgl.glfw.GLFW.GLFW_KEY_S) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            case "D" -> org.lwjgl.glfw.GLFW.glfwGetKey(h, org.lwjgl.glfw.GLFW.GLFW_KEY_D) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            case "空格" -> org.lwjgl.glfw.GLFW.glfwGetKey(h, org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            case "左键" -> org.lwjgl.glfw.GLFW.glfwGetMouseButton(h, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            case "右键" -> org.lwjgl.glfw.GLFW.glfwGetMouseButton(h, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            default -> false;
        };
    }

    private record Key(String label, int col, int row, int span) {
    }
}
