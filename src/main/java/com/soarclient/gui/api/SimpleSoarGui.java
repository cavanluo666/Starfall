package com.soarclient.gui.api;

import com.soarclient.SoarConfig;
import com.soarclient.compat.McCompat;
import com.soarclient.skia.Skia;
import com.soarclient.skia.context.SkiaContext;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Skia 界面的 MC Screen 包装。
 *
 * <h2>坐标空间（此前按钮点不动的根因所在）</h2>
 * MC 在 {@code Mouse.onMouseButton} 里把光标坐标换算成
 * <b>GUI 缩放坐标</b>后才调用 {@code mouseClicked}：
 * <pre>clickX = rawX * Window.getScaledWidth() / Window.getWidth()</pre>
 * 而 {@code render(...)} 传入的 mouseX/mouseY 也是同一空间。
 * 因此本类把 Skia 画布也映射到「GUI 缩放坐标」：
 * <pre>canvasScale = framebufferWidth / scaledWidth</pre>
 * 于是<b>绘制、布局、鼠标三者共用同一个坐标系</b>，任意 GUI Scale / HiDPI 下命中都准确。
 *
 * <p>旧实现绘制时按 framebuffer/window 缩放、鼠标却直接比较，两者相差一个
 * scaleFactor，导致设置页里的开关/滑条点不动、缩放窗口后整体错位。
 */
public class SimpleSoarGui {

    protected MinecraftClient client = MinecraftClient.getInstance();

    public SimpleSoarGui(boolean mcScale) {
    }

    public void init() {
    }

    public void draw(double mouseX, double mouseY) {
    }

    public void mousePressed(double mouseX, double mouseY, int button) {
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
    }

    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
    }

    public void charTyped(char chr, int modifiers) {
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
    }

    /** 渲染面尺寸变化时由外部调用，用于重新布局。 */
    public void onResize() {
    }

    /**
     * Skia 覆盖层渲染前的钩子（在 renderBackground 之后、Skia 画布之前）。
     *
     * <p>HUD 编辑器用它在 MC 渲染栈里画元素预览 —— 这些元素由
     * {@code HudRenderer}（MC 管线）绘制，必须在 Skia 全屏遮罩之前完成，
     * 否则会被遮罩盖住（表现为「除顶栏外一片空白」）。
     */
    public void onRenderBeforeOverlay(net.minecraft.client.gui.DrawContext context,
                                      double mouseX, double mouseY, float delta) {
    }

    /**
     * 帧缓冲像素 -> GUI 缩放坐标 的精确缩放比。
     * 用 framebuffer/scaledWidth 而非 scaleFactor，可消除 scaledWidth 取整带来的偏差。
     */
    public static float canvasScale() {
        return McCompat.framebufferScale();
    }

    public Screen build() {
        return new Screen(Text.empty()) {

            private int lastFbW = -1, lastFbH = -1;

            @Override
            public void init() {
                SimpleSoarGui.this.init();
            }

            @Override
            public void render(DrawContext context, int mouseX, int mouseY, float delta) {

                // 推进动画时间。必须在任何 getValue() 之前调用，
                // 否则动画永远停在起始帧（入场 alpha=0 → 界面透明）。
                com.soarclient.animation.Delta.update();

                if (!McCompat.windowReady()) return;

                // 本类覆写了 render 且不调用 super.render()，
                // 因此必须显式触发 renderBackground —— 它负责画原版背景并完成官方模糊，
                // 再由 ScreenGlassMixin 在末尾叠加很薄的 Material You 遮罩。
                this.renderBackground(context, mouseX, mouseY, delta);

                // 子类钩子：在 Skia 覆盖层之前渲染（如 HUD 编辑器的元素预览，
                // 走 MC 渲染栈，必须在 Skia 画布绘制前完成，否则被遮罩盖住）。
                SimpleSoarGui.this.onRenderBeforeOverlay(context, mouseX, mouseY, delta);

                SkiaContext.ensureSurface(McCompat.framebufferWidth(), McCompat.framebufferHeight());

                // 帧缓冲尺寸变化 -> 重新布局（修缩放窗口错位）
                int fbW = McCompat.framebufferWidth(), fbH = McCompat.framebufferHeight();
                if (fbW != lastFbW || fbH != lastFbH) {
                    lastFbW = fbW;
                    lastFbH = fbH;
                    SimpleSoarGui.this.onResize();
                }

                // 背景模糊已由 ScreenGlassMixin 统一处理，这里不再重复做

                SkiaContext.draw(canvas -> {
                    Skia.save();
                    // 画布 -> GUI 缩放坐标，与 mouseX/mouseY、mouseClicked 完全一致
                    Skia.scale(canvasScale());
                    SimpleSoarGui.this.draw(mouseX, mouseY);
                    Skia.restore();
                });
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                SimpleSoarGui.this.mousePressed(mouseX, mouseY, button);
                return true;
            }

            @Override
            public boolean mouseReleased(double mouseX, double mouseY, int button) {
                SimpleSoarGui.this.mouseReleased(mouseX, mouseY, button);
                return true;
            }

            @Override
            public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
                SimpleSoarGui.this.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
                return true;
            }

            @Override
            public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
                SimpleSoarGui.this.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
                return true;
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                SimpleSoarGui.this.keyPressed(keyCode, scanCode, modifiers);
                return true;
            }

            @Override
            public boolean charTyped(char chr, int modifiers) {
                SimpleSoarGui.this.charTyped(chr, modifiers);
                return true;
            }

            @Override
            public boolean shouldPause() {
                return false;
            }
        };
    }
}
