/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 *
 * 平滑快捷栏：滚轮切换槽位时做过渡。
 *
 * 做法：不改原版槽位逻辑（避免与服务器不同步），
 * 而是在 ClientPlayerEntity.tick 之后把槽位"追平"到目标值 ——
 * 每次滚轮先把目标槽位记下，随后每 tick 朝目标移动一格，
 * 视觉上就是逐格滚动而不是瞬移。
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.SmoothHotbar;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class SmoothHotbarMixin {

    /** 上一次观察到的槽位，用于识别"跳变"。 */
    private static int soar$lastSlot = -1;
    /** 平滑过渡的剩余步数。 */
    private static int soar$pending = 0;

    @Inject(method = "tick", at = @At("TAIL"))
    private void soar$smoothHotbar(CallbackInfo ci) {
        try {
            SmoothHotbar mod = Modules.get().get(SmoothHotbar.class);
            if (mod == null || !mod.isActive()) {
                soar$lastSlot = -1;
                soar$pending = 0;
                return;
            }

            MinecraftClient mc = MinecraftClient.getInstance();
            ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
            if (mc.player != self) return;

            int cur = self.getInventory().selectedSlot;

            // 首次记录
            if (soar$lastSlot < 0) {
                soar$lastSlot = cur;
                soar$pending = 0;
                return;
            }

            // 槽位发生跳变（滚轮跨越了多格）
            if (cur != soar$lastSlot) {
                int delta = cur - soar$lastSlot;
                // 处理环绕：9 格
                if (delta > 4) delta -= 9;
                if (delta < -4) delta += 9;

                int steps = Math.abs(delta);
                if (steps > 1 && mod.getSpeed() > 0) {
                    // 回退到起点，逐 tick 前进
                    self.getInventory().selectedSlot = soar$lastSlot;
                    soar$pending = delta;
                } else {
                    soar$pending = 0;
                }
                soar$lastSlot = cur;
                return;
            }

            // 平滑推进
            if (soar$pending != 0) {
                int step = Integer.signum(soar$pending);
                int next = self.getInventory().selectedSlot + step;
                if (next > 8) next = 0;
                if (next < 0) next = 8;
                self.getInventory().selectedSlot = next;
                soar$pending -= step;
                soar$lastSlot = next;
            }
        } catch (Throwable ignored) {
        }
    }
}
