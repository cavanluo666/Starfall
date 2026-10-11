/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 *
 * 给快捷栏当前选中的格子叠加自定义高亮。
 *
 * 注入 InGameHud.renderHotbar 的 TAIL：原版已经画完快捷栏，
 * 此时再叠一层高亮框，不需要理解原版布局细节，也不会破坏它。
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterItemSelector;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class BetterItemSelectorMixin {

    @Inject(method = "renderHotbar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At("TAIL"))
    private void soar$itemSelectorHighlight(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {

        BetterItemSelector mod = Modules.get().get(BetterItemSelector.class);
        if (mod == null || !mod.isActive()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (mc.options.hudHidden) return;
        if (mc.player.isSpectator()) return;

        int selected = mc.player.getInventory().selectedSlot;
        if (selected < 0 || selected > 8) return;

        int w = context.getScaledWindowWidth();
        int h = context.getScaledWindowHeight();

        // 原版快捷栏布局：9 格，每格 20px，整体宽 182，底部留 22px
        int barW = 182;
        int barH = 22;
        int left = w / 2 - barW / 2;
        int top = h - barH;

        int cellW = 20;
        int x = left + 3 + selected * cellW;
        int y = top + 1;
        int size = 20;

        SettingColor sc = mod.getColor();
        int color = (sc.a << 24) | (sc.r << 16) | (sc.g << 8) | sc.b;

        var style = mod.getStyle();
        int t = (int) Math.max(1, Math.round(mod.getThickness()));

        if (style == BetterItemSelector.HighlightStyle.填充
            || style == BetterItemSelector.HighlightStyle.两者) {
            context.fill(x, y, x + size, y + size, color);
        }

        if (style == BetterItemSelector.HighlightStyle.边框
            || style == BetterItemSelector.HighlightStyle.两者) {
            // 四条边
            context.fill(x, y, x + size, y + t, color);
            context.fill(x, y + size - t, x + size, y + size, color);
            context.fill(x, y + t, x + t, y + size - t, color);
            context.fill(x + size - t, y + t, x + size, y + size - t, color);
        }
    }
}
