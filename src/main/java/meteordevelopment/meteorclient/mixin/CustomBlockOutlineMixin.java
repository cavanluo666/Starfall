/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 *
 * 自定义方块描边。
 *
 * 原版在 WorldRenderer.drawBlockOutline 里画准星所指方块的轮廓。
 * 这里在 TAIL 再叠一层自定义颜色的框（略放大以产生"加粗"观感），
 * 不取消原版绘制 —— 即使 API 有版本差异也不会导致轮廓整个消失。
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.CustomBlockOutline;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class CustomBlockOutlineMixin {

    @Inject(method = "drawBlockOutline", at = @At("TAIL"))
    private void soar$customOutline(MatrixStack matrices, VertexConsumer vertexConsumer,
                                    net.minecraft.entity.Entity entity,
                                    double camX, double camY, double camZ,
                                    BlockPos pos, net.minecraft.block.BlockState state,
                                    CallbackInfo ci) {
        try {
            CustomBlockOutline mod = Modules.get().get(CustomBlockOutline.class);
            if (mod == null || !mod.isActive()) return;

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world == null) return;

            SettingColor c = mod.getColor();

            // 略微放大，制造"加粗"观感
            double e = Math.max(0.0, mod.getWidth() - 1.0) * 0.004;
            Box box = new Box(pos).expand(e);

            VertexConsumer vc = mc.getBufferBuilders().getEntityVertexConsumers()
                .getBuffer(RenderLayer.getLines());

            // 用 Box 重载，坐标已相对于相机
            WorldRenderer.drawBox(matrices, vc,
                box.minX - camX, box.minY - camY, box.minZ - camZ,
                box.maxX - camX, box.maxY - camY, box.maxZ - camZ,
                c.r / 255f, c.g / 255f, c.b / 255f, c.a / 255f);

            // 填充面（可选）
            if (mod.isFillEnabled()) {
                SettingColor fc = mod.getFillColor();
                WorldRenderer.renderFilledBox(matrices, vc,
                    box.minX - camX, box.minY - camY, box.minZ - camZ,
                    box.maxX - camX, box.maxY - camY, box.maxZ - camZ,
                    fc.r / 255f, fc.g / 255f, fc.b / 255f, fc.a / 255f);
            }
        } catch (Throwable ignored) {
        }
    }
}
