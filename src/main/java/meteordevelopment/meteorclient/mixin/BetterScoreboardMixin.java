/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 *
 * 接管原版计分板侧边栏绘制，用自定义背景 / 颜色 / 缩放替代。
 * 注入在 HEAD 且 cancellable，未启用时直接放行原版逻辑。
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterScoreboard;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Mixin(InGameHud.class)
public abstract class BetterScoreboardMixin {

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At("HEAD"), cancellable = true)
    private void soar$renderScoreboard(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter, CallbackInfo ci) {

        BetterScoreboard mod = Modules.get().get(BetterScoreboard.class);
        if (mod == null || !mod.isActive()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        Scoreboard sb = mc.world.getScoreboard();
        ScoreboardObjective obj = sb.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (obj == null) return;

        // 收集条目并排序（分数降序）
        Collection<ScoreboardEntry> raw = sb.getScoreboardEntries(obj);
        List<ScoreboardEntry> entries = new ArrayList<>(raw);
        entries.removeIf(ScoreboardEntry::hidden);
        entries.sort(Comparator.comparingInt(ScoreboardEntry::value).reversed());

        double scale = mod.getScale();
        TextRenderer tr = mc.textRenderer;

        // 组装每行的显示文本
        List<String> lines = new ArrayList<>();
        // 标题
        String title = obj.getDisplayName().getString();
        lines.add(title);

        for (ScoreboardEntry e : entries) {
            Text name = Team.decorateName(sb.getScoreHolderTeam(e.owner()), e.name());
            String s = name.getString();
            if (!mod.isHideScores()) {
                s = s + " " + e.value();
            }
            lines.add(s);
        }

        // 计算最大宽度
        int maxW = 0;
        for (String s : lines) {
            maxW = Math.max(maxW, tr.getWidth(s));
        }

        int pad = 3;
        int lineH = tr.fontHeight + 1;
        int boxW = maxW + pad * 2;
        int boxH = lines.size() * lineH + pad * 2;

        // 右上角定位（与原版一致：右侧留一点边距，Y = 居中偏上）
        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();
        int x = screenW - boxW - 3;
        int y = screenH / 2 - boxH / 2;

        // 背景
        if (mod.getBackgroundMode() != BetterScoreboard.BackgroundMode.无) {
            SettingColor bg = mod.getBackgroundColor();
            int color = (bg.a << 24) | (bg.r << 16) | (bg.g << 8) | bg.b;
            context.fill(x, y, x + boxW, y + boxH, color);
        }

        // 标题与条目
        SettingColor tc = mod.getTitleColor();
        SettingColor nc = mod.getTextColor();
        int titleColor = (255 << 24) | (tc.r << 16) | (tc.g << 8) | tc.b;
        int textColor = (255 << 24) | (nc.r << 16) | (nc.g << 8) | nc.b;

        int ly = y + pad;
        for (int i = 0; i < lines.size(); i++) {
            int color = i == 0 ? titleColor : textColor;
            context.drawTextWithShadow(tr, lines.get(i), x + pad, ly, color);
            ly += lineH;
        }

        ci.cancel();
    }
}
