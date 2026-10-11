/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 *
 * 附魔光效范围控制。
 *
 * 复用 Meteor 已有的技术：在 ItemRenderer.renderItem 调用 renderBakedItemModel 时，
 * 替换传入的 VertexConsumer —— 用不带 glint 的层替代，即可隐藏闪光。
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.CustomEnchantmentGlint;
import net.minecraft.block.StainedGlassPaneBlock;
import net.minecraft.block.TransparentBlock;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ItemRenderer.class)
public abstract class CustomEnchantmentGlintMixin {

    @ModifyArgs(
        method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/ItemRenderer;renderBakedItemModel(Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/item/ItemStack;IILnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;)V"
        )
    )
    private void soar$controlGlint(Args args, ItemStack stack, ModelTransformationMode renderMode,
                                   boolean leftHanded, MatrixStack matrices,
                                   VertexConsumerProvider vertexConsumers, int light, int overlay,
                                   BakedModel model) {
        try {
            CustomEnchantmentGlint mod = Modules.get().get(CustomEnchantmentGlint.class);
            if (mod == null || !mod.isActive()) return;

            // GUI 内关闭
            if (mod.isDisableInGui() && renderMode == ModelTransformationMode.GUI) {
                args.set(5, vertexConsumers.getBuffer(RenderLayers.getItemLayer(stack, true)));
                return;
            }

            // 完全隐藏：用不带 glint 的层
            if (mod.isHidden()) {
                boolean bl = renderMode == ModelTransformationMode.GUI
                    || renderMode.isFirstPerson()
                    || !(stack.getItem() instanceof BlockItem blockItem)
                    || !(blockItem.getBlock() instanceof TransparentBlock)
                       && !(blockItem.getBlock() instanceof StainedGlassPaneBlock);

                args.set(5, vertexConsumers.getBuffer(RenderLayers.getItemLayer(stack, bl)));
            }
        } catch (Throwable ignored) {
        }
    }
}
