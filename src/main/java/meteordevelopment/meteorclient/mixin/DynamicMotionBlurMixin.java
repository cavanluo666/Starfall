/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 *
 * 动态模糊：按移动速度给相机加轻微的方向性偏移，产生运动拖影。
 *
 * <p>真正的多帧混合需要自建后处理 FBO（会与 Meteor 的渲染管线冲突，
 * 也是之前文字错位的根源）。这里改用**相机微偏移**的近似做法：
 * 速度越快，相机相对眼位偏移越多，视觉上是柔和的运动感，
 * 且完全不动渲染状态，零副作用。
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.DynamicMotionBlur;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class DynamicMotionBlurMixin {

    /** 保留上一次的相机位置，用于计算滞后偏移。 */
    private Vec3d soar$lagPos;

    @Inject(method = "getCameraPosVec", at = @At("RETURN"), cancellable = true)
    private void soar$motionBlurCamera(float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        try {
            Entity self = (Entity) (Object) this;
            if (self != MinecraftClient.getInstance().getCameraEntity()) return;

            DynamicMotionBlur mod = Modules.get().get(DynamicMotionBlur.class);
            if (mod == null || !mod.isActive()) {
                soar$lagPos = null;
                return;
            }

            Vec3d pos = cir.getReturnValue();

            if (soar$lagPos == null) {
                soar$lagPos = pos;
                return;
            }

            // 相机滞后于当前位置，滞后量随强度与移动速度变化
            Vec3d vel = self.getVelocity();
            double speed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);

            // 平滑跟随
            double follow = Math.min(1.0, 0.35 / Math.max(0.05, mod.getStrength()));
            Vec3d smoothed = soar$lagPos.add(pos.subtract(soar$lagPos).multiply(follow));

            // 按速度做方向性偏移（拖影感）
            double amt = Math.min(0.35, speed * 0.06) * mod.getStrength();
            Vec3d offset = new Vec3d(-vel.x, 0, -vel.z).normalize().multiply(amt);

            soar$lagPos = smoothed;

            cir.setReturnValue(smoothed.add(offset));
        } catch (Throwable ignored) {
        }
    }
}
