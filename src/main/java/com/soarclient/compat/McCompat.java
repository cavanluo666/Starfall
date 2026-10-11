package com.soarclient.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

/**
 * <b>MC 版本适配层。</b>
 *
 * <p>合并版把 Soar 的界面栈移植到 Meteor 所支持的各个 MC 版本。不同版本之间
 * 最容易变化的渲染 / 窗口 / 资源 API 全部收敛到这里，移植到新版本时
 * <b>只需改动本文件</b>，不必去翻 30 多个散布的调用点。
 *
 * <h2>移植清单（按版本改动本文件的这些点）</h2>
 * <ul>
 *   <li>{@link #framebufferScale()} —— 帧缓冲像素 / GUI 缩放坐标 的比例来源</li>
 *   <li>{@link #scaledWidth()} / {@link #scaledHeight()} —— 布局空间尺寸</li>
 *   <li>{@link #framebufferWidth()} / {@link #framebufferHeight()} —— Skia 渲染面尺寸</li>
 *   <li>{@link #mouseX()} / {@link #mouseY()} —— 鼠标在 GUI 缩放坐标下的位置</li>
 * </ul>
 *
 * <p>已核对：1.21.1 / 1.21.x 上这些方法名一致；1.20.x 的 {@code DrawContext} 与
 * {@code Screen#render} 签名不同，属 mixin 层要处理的部分（见
 * {@code meteordevelopment.meteorclient.mixin.widget}）。
 */
public final class McCompat {

    private McCompat() {
    }

    public static MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }

    public static Window window() {
        return mc().getWindow();
    }

    public static boolean windowReady() {
        Window w = window();
        return w != null && w.getScaledWidth() > 0;
    }

    // ---------------- 尺寸 ----------------

    /** Skia 渲染面尺寸：始终对齐帧缓冲，否则会拉伸 / 错位。 */
    public static int framebufferWidth() {
        Window w = window();
        return w == null ? 0 : w.getFramebufferWidth();
    }

    public static int framebufferHeight() {
        Window w = window();
        return w == null ? 0 : w.getFramebufferHeight();
    }

    /** 布局空间宽度（GUI 缩放坐标），与 mouseX 同空间。 */
    public static int scaledWidth() {
        Window w = window();
        return w == null ? 0 : w.getScaledWidth();
    }

    public static int scaledHeight() {
        Window w = window();
        return w == null ? 0 : w.getScaledHeight();
    }

    /**
     * 帧缓冲像素 -> GUI 缩放坐标 的精确比例。
     *
     * <p>用 framebufferWidth / scaledWidth 而不是 scaleFactor：
     * scaledWidth 计算时有向上取整，直接用 scaleFactor 会带来 1px 级别的偏差，
     * 在滑条 / 开关的命中判定上会累积成"点不准"。
     */
    public static float framebufferScale() {
        Window w = window();
        if (w == null || w.getScaledWidth() <= 0) return 1f;
        return (float) w.getFramebufferWidth() / (float) w.getScaledWidth();
    }

    // ---------------- 鼠标 ----------------

    /**
     * 鼠标位置。
     *
     * <p>注意：{@code Mouse.getX()} 返回的是<b>原始窗口像素</b>，不是 GUI 缩放坐标。
     * 而 {@code Screen#mouseClicked} 与 {@code render} 传入的坐标是<b>缩放后的</b>。
     * 本方法统一换算到缩放坐标，避免两套坐标混用导致点击错位。
     */
    public static double mouseX() {
        Window w = window();
        if (w == null || w.getWidth() <= 0) return 0;
        return mc().mouse.getX() * w.getScaledWidth() / (double) w.getWidth();
    }

    public static double mouseY() {
        Window w = window();
        if (w == null || w.getHeight() <= 0) return 0;
        return mc().mouse.getY() * w.getScaledHeight() / (double) w.getHeight();
    }

    public static double windowScaleX() {
        Window w = window();
        if (w == null || w.getWidth() <= 0) return 1;
        return (double) w.getScaledWidth() / w.getWidth();
    }

    public static double windowScaleY() {
        Window w = window();
        if (w == null || w.getHeight() <= 0) return 1;
        return (double) w.getScaledHeight() / w.getHeight();
    }
}
