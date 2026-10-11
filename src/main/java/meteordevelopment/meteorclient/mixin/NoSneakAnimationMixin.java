/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 *
 * 去掉潜行时的视角下沉。
 *
 * <h2>原理</h2>
 * MC 1.21.1 的第一人称视角高度来自
 * <pre>Entity.getEyeHeight(pose) -> getDimensions(pose).eyeHeight()</pre>
 * 潜行时姿态是 {@code EntityPose.CROUCHING}，其尺寸比站立矮，
 * 于是视角随之下沉。
 *
 * <p>这里注入 {@code Entity.getDimensions(EntityPose)}：当本地玩家处于潜行姿态且
 * 模块启用时，改用 {@code STANDING} 的尺寸，从而眼高保持站立值 —— 视角不再下沉，
 * 但碰撞箱仍由服务端/原版逻辑决定（本注入只影响渲染用的眼高计算）。
 *
 * <p>只处理本地玩家，避免影响其它实体。
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoSneakAnimation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class NoSneakAnimationMixin {

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void soar$noSneakEyeHeight(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        try {
            Entity self = (Entity) (Object) this;
            if (self != MinecraftClient.getInstance().player) return;
            if (pose != EntityPose.CROUCHING) return;

            NoSneakAnimation mod = Modules.get().get(NoSneakAnimation.class);
            if (mod == null || !mod.isActive()) return;
            if (mod.isViewOnly()) {
                // 用站立尺寸计算眼高，视觉上不下沉
                cir.setReturnValue(self.getDimensions(EntityPose.STANDING));
            }
        } catch (Throwable ignored) {
        }
    }
}
