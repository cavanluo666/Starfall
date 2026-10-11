package com.soarclient.gui.modmenu.pages;

import java.util.List;
import java.util.function.Consumer;

import com.soarclient.Soar;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.ui.component.Component;
import com.soarclient.ui.component.handler.impl.ComboButtonHandler;
import com.soarclient.ui.component.handler.impl.SliderHandler;
import com.soarclient.ui.component.handler.impl.SwitchHandler;
import com.soarclient.ui.component.impl.ComboButton;
import com.soarclient.ui.component.impl.Slider;
import com.soarclient.ui.component.impl.Switch;

/** 外观设置行：同一行样式，右侧控件可为开关 / 滑条 / 下拉。 */
public class AppearanceBar extends Component {

    private final SimpleAnimation yAnimation = new SimpleAnimation();
    /** 命中判定用：本行“当前实际绘制”的 Y（动画后的值）。 */
    private float hitY;
    private final String title;
    private final String description;
    private final String icon;
    private final Component control;

    /** 开关型 */
    public AppearanceBar(String title, String description, String icon, float x, float y, float width,
                         boolean initial, Consumer<Boolean> onChange) {
        super(x, y);
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.width = width;
        this.height = com.soarclient.gui.api.Layout.rowHeight();

        Switch sw = new Switch(x, y, initial);
        sw.setHandler(new SwitchHandler() {
            @Override public void onEnabled() { onChange.accept(true); }
            @Override public void onDisabled() { onChange.accept(false); }
        });
        this.control = sw;
    }

    /** 滑条型（整数） */
    public AppearanceBar(String title, String description, String icon, float x, float y, float width,
                         float initial, float min, float max, float step, Consumer<Float> onChange) {
        super(x, y);
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.width = width;
        this.height = com.soarclient.gui.api.Layout.rowHeight();

        Slider sl = new Slider(x, y, 200, initial, min, max, step);
        sl.setHandler(new SliderHandler() {
            @Override public void onValueChanged(float value) { onChange.accept(value); }
        });
        this.control = sl;
    }

    /** 下拉型 */
    public AppearanceBar(String title, String description, String icon, float x, float y, float width,
                         List<String> options, String initial, Consumer<String> onChange) {
        super(x, y);
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.width = width;
        this.height = com.soarclient.gui.api.Layout.rowHeight();

        ComboButton cb = new ComboButton(x, y, options, initial);
        cb.setHandler(new ComboButtonHandler() {
            @Override public void onChanged(String option) { onChange.accept(option); }
        });
        this.control = cb;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }

    @Override
    public void draw(double mouseX, double mouseY) {

        ColorPalette palette = Soar.getInstance().getColorManager().getPalette();

        yAnimation.onTick(y, 14);
        float itemY = yAnimation.getValue();
        this.hitY = itemY;

        if (control != null) {
            control.setX(x + width - control.getWidth() - 22);
            control.setY(itemY + (height - control.getHeight()) / 2f);
        }

        Skia.drawRoundedRect(x, itemY, width, height, 18, palette.getSurface());
        Skia.drawFullCenteredText(icon, x + 30, itemY + (height / 2f), palette.getOnSurface(), Fonts.getIcon(32));
        Skia.drawText(title, x + 52, itemY + 20, palette.getOnSurface(), Fonts.getRegular(17));
        Skia.drawText(Skia.getLimitText(description, Fonts.getRegular(14), width - 260), x + 52, itemY + 37,
                palette.getOnSurfaceVariant(), Fonts.getRegular(14));

        if (control != null) control.draw(mouseX, mouseY);
    }

    @Override
    public void mousePressed(double mouseX, double mouseY, int button) {
        if (control != null) control.mousePressed(mouseX, mouseY, button);
    }
    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {

        if (control == null) return;

        // 先走控件自身的命中判定
        boolean insideRow = mouseX > x && mouseX < x + width
                && mouseY > hitY && mouseY < hitY + height;

        // 容错：整行都可点。开关本身只有 52x32，在缩放的 GUI 坐标里很小，
        // 只认控件会导致“点半天没反应”。整行命中时直接驱动控件。
        if (insideRow && button == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            control.setX(x + width - control.getWidth() - 22);
            control.setY(hitY + (height - control.getHeight()) / 2f);
        }

        control.mouseReleased(mouseX, mouseY, button);
    }

    @Override public void charTyped(char chr, int modifiers) { if (control != null) control.charTyped(chr, modifiers); }
    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) { if (control != null) control.keyPressed(keyCode, scanCode, modifiers); }
}
