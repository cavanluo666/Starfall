package com.soarclient.animation;

/**
 * 动画的时间源。
 *
 * <h2>为什么不用 EventBus</h2>
 * 原版 Soar 用一个 {@code GameLoopEvent} 来刷新 deltaTime。合并版里没有任何地方
 * 会 post 那个事件（它是 Soar 自己 mixin 驱动的一环，未移植），于是
 * {@link #getDeltaTime()} 恒为 0，导致：
 * <pre>
 * Animation.getValue(): timePassed += 0  →  永远停在 start
 * 入场动画 start=0  →  alpha 恒为 0  →  界面全透明看不见
 * </pre>
 * 表现就是「按 Right Shift 没画面，按一下 Esc 才突然显示」
 * （Esc 翻转动画方向后 start 变成 1）。
 *
 * <p>现在直接读 GLFW 真实时间，任何帧循环里都能正确推进动画。
 */
public final class Delta {

    private static double deltaTime;

    /** 上一帧时间戳（秒）。0 表示尚未初始化。 */
    private static double lastTime;
    /** 单帧最大步进，防止卡顿 / 窗口拖动后动画瞬间跳完。 */
    private static final double MAX_STEP_MS = 100.0;

    private Delta() {
    }

    /**
     * 推进时间。应在每帧渲染前调用一次。
     */
    public static void update() {
        double now = org.lwjgl.glfw.GLFW.glfwGetTime() * 1000.0;

        if (lastTime == 0.0) {
            lastTime = now;
            deltaTime = 0.0;
            return;
        }

        double dt = now - lastTime;
        lastTime = now;

        if (dt < 0) dt = 0;
        if (dt > MAX_STEP_MS) dt = MAX_STEP_MS;
        deltaTime = dt;
    }

    /** 距上一帧的毫秒数。 */
    public static double getDeltaTime() {
        return deltaTime;
    }
}
