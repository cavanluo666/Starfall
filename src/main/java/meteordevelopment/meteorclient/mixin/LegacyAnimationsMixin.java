/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 *
 * 1.7 动画：还原老版本观感。
 *
 * 已实现两项（都是老版与现代版最明显的差异，且改动可控）：
 *   1. 头部跟随身体 —— 老版实体没有独立的头部朝向，头随身体转；
 *   2. 受击倾斜 —— 受击时角色向后仰。
 *
 * 通过注入 LivingEntityRenderer.setupTransforms，在模型变换阶段调整。
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.LegacyAnimations;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LegacyAnimationsMixin {

    @Inject(method = "setupTransforms", at = @At("TAIL"))
    private void soar$legacyTransforms(LivingEntity entity, MatrixStack matrices,
                                       float animationProgress, float bodyYaw,
                                       float tickDelta, float scale, CallbackInfo ci) {
        try {
            LegacyAnimations mod = Modules.get().get(LegacyAnimations.class);
            if (mod == null || !mod.isActive()) return;

            // 1) 头部跟随身体：老版没有独立头朝向，把头拧到身体朝向
            if (mod.headFollowsBody()) {
                // 让头部 yaw 与 bodyYaw 对齐（消掉头部相对身体的偏转）
                float head = entity.getHeadYaw();
                float body = entity.getBodyYaw();
                float diff = net.minecraft.util.math.MathHelper.wrapDegrees(head - body);
                // 反向旋转以抵消头部偏转 —— 表现为"头跟着身体"
                if (Math.abs(diff) > 0.1f) {
                    matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y
                        .rotationDegrees(-diff));
                }
            }

            // 2) 受击倾斜：受击时向后仰
            if (mod.hurtTilt() && entity.hurtTime > 0) {
                float hurt = entity.hurtTime - tickDelta;
                float max = Math.max(1, entity.maxHurtTime);
                float t = Math.max(0, hurt) / max;
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X
                    .rotationDegrees(-8.0f * t));
            }
        } catch (Throwable ignored) {
        }
    }
}
