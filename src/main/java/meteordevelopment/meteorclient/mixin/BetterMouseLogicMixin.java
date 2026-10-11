/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 *
 * 鼠标逻辑优化：窗口未聚焦时不触发攻击 / 使用物品。
 *
 * <h2>回调类型必须与方法签名严格对应</h2>
 * MC 1.21.1 中：
 * <pre>
 *   private boolean doAttack();     -> CallbackInfoReturnable<Boolean>
 *   private void    doItemUse();    -> CallbackInfo
 * </pre>
 * 两者用混会在 mixin 应用阶段抛 InvalidInjectionException。
 * 该异常现在被隔离在 required=false 的配置里，只会警告不会崩溃，
 * 但功能会静默失效 —— 因此这里必须写对。
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.BetterMouseLogic;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class BetterMouseLogicMixin {

    /** 窗口未聚焦时不处理攻击。doAttack 返回 boolean。 */
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void soar$ignoreUnfocusedAttack(CallbackInfoReturnable<Boolean> cir) {
        if (soar$shouldBlock()) {
            cir.setReturnValue(false);
        }
    }

    /** 同理拦截物品使用。doItemUse 返回 void。 */
    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void soar$ignoreUnfocusedUse(CallbackInfo ci) {
        if (soar$shouldBlock()) {
            ci.cancel();
        }
    }

    private static boolean soar$shouldBlock() {
        try {
            BetterMouseLogic mod = Modules.get().get(BetterMouseLogic.class);
            if (mod == null || !mod.isActive()) return false;

            MinecraftClient mc = MinecraftClient.getInstance();
            return mod.ignoreUnfocused() && !mc.isWindowFocused();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
