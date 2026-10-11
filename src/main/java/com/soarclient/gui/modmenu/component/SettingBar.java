package com.soarclient.gui.modmenu.component;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.soarclient.Soar;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;
import com.soarclient.ui.component.Component;
import com.soarclient.ui.component.handler.impl.ComboButtonHandler;
import com.soarclient.ui.component.handler.impl.KeybindHandler;
import com.soarclient.ui.component.handler.impl.SliderHandler;
import com.soarclient.ui.component.handler.impl.SwitchHandler;
import com.soarclient.ui.component.handler.impl.TextHandler;
import com.soarclient.ui.component.impl.ComboButton;
import com.soarclient.ui.component.impl.Slider;
import com.soarclient.ui.component.impl.Switch;
import com.soarclient.ui.component.impl.text.TextField;
import com.soarclient.utils.language.I18n;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.StringSetting;

/**
 * 把一个 Meteor Setting 渲染成一行 Material3 设置项。
 * 名称/描述优先查中文表，查不到退回 Meteor 自带的 title/description。
 */
public class SettingBar extends Component {

    private final SimpleAnimation yAnimation = new SimpleAnimation();

    private final String title;
    private final String description;
    private final String icon;

    private Component component;
    private final Setting<?> setting;

    public SettingBar(Setting<?> setting, float x, float y, float width) {
        super(x, y);
        this.setting = setting;
        this.title = localize(setting.title);
        this.description = localize(setting.description);
        this.icon = iconFor(setting);
        this.width = width;
        this.height = com.soarclient.gui.api.Layout.rowHeight();

        buildControl(setting);
    }

    private static String localize(String s) {
        if (s == null) return "";
        return I18n.has(s) ? I18n.get(s) : s;
    }

    private static String iconFor(Setting<?> s) {
        if (s instanceof BoolSetting) return Icon.TOGGLE_ON;
        if (s instanceof IntSetting || s instanceof DoubleSetting) return Icon.TUNE;
        if (s instanceof EnumSetting) return Icon.LIST;
        if (s instanceof KeybindSetting) return Icon.KEYBOARD;
        if (s instanceof StringSetting) return Icon.TEXT_FIELDS;
        if (s.getClass().getSimpleName().toLowerCase().contains("color")) return Icon.PALETTE;
        return Icon.SETTINGS;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void buildControl(Setting<?> s) {

        if (s instanceof BoolSetting) {
            BoolSetting b = (BoolSetting) s;
            Switch sw = new Switch(0, 0, b.get());
            sw.setHandler(new SwitchHandler() {
                @Override public void onEnabled() { b.set(true); }
                @Override public void onDisabled() { b.set(false); }
            });
            component = sw;
            return;
        }

        if (s instanceof IntSetting) {
            IntSetting n = (IntSetting) s;
            float min = n.min, max = n.max;
            if (max - min <= 0) max = min + 1;
            Slider sl = new Slider(0, 0, 200, n.get(), min, max, 1);
            sl.setHandler(new SliderHandler() {
                @Override public void onValueChanged(float value) { n.set(Math.round(value)); }
            });
            component = sl;
            return;
        }

        if (s instanceof DoubleSetting) {
            DoubleSetting n = (DoubleSetting) s;
            float min = (float) n.min, max = (float) n.max;
            if (!Float.isFinite(min) || !Float.isFinite(max) || max - min <= 0) {
                min = 0; max = Math.max(1, (float) n.getDefaultValue().doubleValue() * 2);
            }
            float step = (float) Math.pow(10, -n.decimalPlaces);
            Slider sl = new Slider(0, 0, 200, n.get() == null ? min : (float) n.get().doubleValue(), min, max, step);
            sl.setHandler(new SliderHandler() {
                @Override public void onValueChanged(float value) { n.set((double) value); }
            });
            component = sl;
            return;
        }

        if (s instanceof EnumSetting) {
            EnumSetting en = (EnumSetting) s;
            List<String> options = new ArrayList<>();
            Object sample = en.get() != null ? en.get() : en.getDefaultValue();
            if (sample != null) for (Object o : ((Enum<?>) sample).getDeclaringClass().getEnumConstants()) options.add(((Enum<?>) o).name());
            String current = en.get() == null ? "" : ((Enum<?>) en.get()).name();
            ComboButton cb = new ComboButton(0, 0, options, current);
            cb.setHandler(new ComboButtonHandler() {
                @Override public void onChanged(String option) {
                    for (Object o : options) {
                        if (o.equals(option)) { en.set((Enum) Enum.valueOf(((Enum<?>) sample).getDeclaringClass(), option)); break; }
                    }
                }
            });
            component = cb;
            return;
        }

        if (s instanceof KeybindSetting) {
            KeybindSetting k = (KeybindSetting) s;
            meteordevelopment.meteorclient.utils.misc.Keybind kb =
                    k.get() == null ? meteordevelopment.meteorclient.utils.misc.Keybind.none() : k.get();
            com.soarclient.ui.component.impl.Keybind bind = new com.soarclient.ui.component.impl.Keybind(
                    0, 0, net.minecraft.client.util.InputUtil.fromKeyCode(kb.getValue(), 0));
            bind.setHandler(new KeybindHandler() {
                @Override public void onBinded(net.minecraft.client.util.InputUtil.Key key) {
                    k.set(meteordevelopment.meteorclient.utils.misc.Keybind.fromKey(key.getCode()));
                }
            });
            component = bind;
            return;
        }

        if (s instanceof StringSetting) {
            StringSetting st = (StringSetting) s;
            TextField tf = new TextField(0, 0, 150, st.get() == null ? "" : st.get());
            tf.setHandler(new TextHandler() {
                @Override public void onTyped(String value) { st.set(value); }
            });
            component = tf;
        }
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
    public Setting<?> getSetting() { return setting; }

    @Override
    public void draw(double mouseX, double mouseY) {

        ColorPalette palette = Soar.getInstance().getColorManager().getPalette();

        yAnimation.onTick(y, 14);
        float itemY = yAnimation.getValue();

        if (component != null) {
            component.setX(x + width - component.getWidth() - 22);
            component.setY(itemY + (height - component.getHeight()) / 2);
        }

        Skia.drawRoundedRect(x, itemY, width, height, 18, palette.getSurface());
        Skia.drawFullCenteredText(icon, x + 30, itemY + (height / 2), palette.getOnSurface(), Fonts.getIcon(32));
        Skia.drawText(title, x + 52, itemY + 20, palette.getOnSurface(), Fonts.getRegular(17));
        Skia.drawText(Skia.getLimitText(description, Fonts.getRegular(14), width - 260), x + 52, itemY + 37,
                palette.getOnSurfaceVariant(), Fonts.getRegular(14));

        if (component != null) component.draw(mouseX, mouseY);
    }

    @Override public void mousePressed(double mouseX, double mouseY, int button) { if (component != null) component.mousePressed(mouseX, mouseY, button); }
    @Override public void mouseReleased(double mouseX, double mouseY, int button) { if (component != null) component.mouseReleased(mouseX, mouseY, button); }
    @Override public void charTyped(char chr, int modifiers) { if (component != null) component.charTyped(chr, modifiers); }
    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) { if (component != null) component.keyPressed(keyCode, scanCode, modifiers); }
}
