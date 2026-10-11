/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

/**
 * 附魔光效控制。
 *
 * <h2>实现范围说明</h2>
 * 本模块只控制<b>是否渲染</b>附魔闪光，以及<b>哪些物品</b>显示闪光。
 *
 * <p>真正改变闪光颜色与流动速度需要替换 {@code RenderLayer.getGlint()} 并自带
 * 一套 shader —— 那会与 Meteor / 其它渲染优化模组争夺渲染管线，
 * 是本项目此前遇到"文字错位"的同类风险源。因此这里刻意不做，
 * 而是复用原版光效，只提供开关与范围控制。
 *
 * <p>若需要彩色光效，建议配合资源包实现，那样零兼容风险。
 */
public class CustomEnchantmentGlint extends Module {

    public enum GlintMode {
        /** 完全隐藏所有附魔闪光 */
        隐藏,
        /** 只在附魔物品上显示（原版行为） */
        仅附魔物品,
        /** 所有手持物品都显示闪光 */
        所有物品
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<GlintMode> mode = sgGeneral.add(new EnumSetting.Builder<GlintMode>()
        .name("光效模式")
        .description("控制附魔闪光的显示方式。")
        .defaultValue(GlintMode.仅附魔物品)
        .build()
    );

    private final Setting<Boolean> disableInGui = sgGeneral.add(new BoolSetting.Builder()
        .name("界面内关闭")
        .description("在物品栏 / 背包界面里不显示闪光，减少视觉干扰。")
        .defaultValue(false)
        .visible(() -> mode.get() != GlintMode.隐藏)
        .build()
    );

    private final Setting<Boolean> disableOnOthers = sgGeneral.add(new BoolSetting.Builder()
        .name("其它实体关闭")
        .description("不在其它玩家 / 实体手持物上显示闪光。")
        .defaultValue(false)
        .visible(() -> mode.get() != GlintMode.隐藏)
        .build()
    );

    public CustomEnchantmentGlint() {
        super(Categories.Render, "custom-enchantment-glint", "控制附魔闪光的显示范围（隐藏 / 仅附魔 / 所有物品）。");
    }

    public GlintMode getMode() {
        return mode.get();
    }

    /** 是否完全隐藏闪光。 */
    public boolean isHidden() {
        return isActive() && mode.get() == GlintMode.隐藏;
    }

    /** 是否让所有物品都显示闪光。 */
    public boolean forceAllGlint() {
        return isActive() && mode.get() == GlintMode.所有物品;
    }

    public boolean isDisableInGui() {
        return isActive() && disableInGui.get();
    }

    public boolean isDisableOnOthers() {
        return isActive() && disableOnOthers.get();
    }
}
