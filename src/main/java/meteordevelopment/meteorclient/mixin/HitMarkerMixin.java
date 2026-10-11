/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 *
 * 攻击实体时统一通知需要"命中反馈"的功能：
 * 命中标记、伤害飘字、攻击特效。
 * 这样多个功能共用一个注入点，避免重复 mixin 同一方法。
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.HitMarker;
import meteordevelopment.meteorclient.systems.modules.render.AttackEffects;
import meteordevelopment.meteorclient.systems.modules.render.DamageNumbers;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class HitMarkerMixin {

    @Inject(method = "attackEntity", at = @At("TAIL"))
    private void soar$onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        try {
            // 是否暴击：玩家正在下落 + 非疾跑（接近原版判定）
            boolean crit = player.fallDistance > 0.0F
                && !player.isOnGround()
                && !player.isClimbing()
                && !player.isTouchingWater()
                && !player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS)
                && !player.hasVehicle()
                && !player.isSprinting();

            HitMarker hm = Modules.get().get(HitMarker.class);
            if (hm != null && hm.isActive()) hm.onHit(false);

            AttackEffects ae = Modules.get().get(AttackEffects.class);
            if (ae != null && ae.isActive()) ae.onHit(target, crit);

            DamageNumbers dn = Modules.get().get(DamageNumbers.class);
            if (dn != null && dn.isActive()) {
                Vec3d pos = target.getPos();
                // 伤害数值无法在此精确取得，用攻击力估算
                float dmg = (float) player.getAttributeValue(
                    net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE);
                dn.addDamage(pos.x, pos.y + target.getHeight() + 0.4, pos.z, dmg, crit);
            }
        } catch (Throwable ignored) {
        }
    }
}
