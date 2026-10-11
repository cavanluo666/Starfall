package meteordevelopment.meteorclient.soar;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/**
 * 内置资源包的<b>预启动注册</b>入口。
 *
 * <h2>为什么需要它</h2>
 * Minecraft 构造 {@code GameOptions} 时会读 {@code options.txt}。
 * 若此时内置资源包尚未注册，其中记录的 {@code meteor-client/xray}
 * 等条目会被判定为"已不存在"而<b>静默移除</b>。
 *
 * <p>实测（注册晚于读取约 10 秒）：
 * <pre>
 * 11:52:48 [Render thread/WARN]: Removed resource pack meteor-client/xray ...
 * 11:52:58 [Render thread/INFO]: [内置包] Xray Ultimate 矿石透视 注册=true
 * </pre>
 *
 * <p>{@code preLaunch} 跑在游戏主流程之前，是唯一足够早的时机。
 * 注册本身只往 Fabric 的资源包表里塞条目，不触碰 Minecraft 类。
 */
public class SoarPreLaunch implements PreLaunchEntrypoint {

    @Override
    public void onPreLaunch() {
        // 注册是幂等的：这里注册失败也不影响，client 阶段还会再尝试一次
        try {
            SoarIntegration.registerBuiltinPacksEarly();
        } catch (Throwable ignored) {
            // preLaunch 阶段部分类可能尚未可用，静默跳过
        }
    }
}
