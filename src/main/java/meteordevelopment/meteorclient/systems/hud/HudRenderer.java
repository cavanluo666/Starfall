/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.CustomFontChangedEvent;
import meteordevelopment.meteorclient.renderer.*;
import meteordevelopment.meteorclient.renderer.text.CustomTextRenderer;
import meteordevelopment.meteorclient.renderer.text.Font;
import meteordevelopment.meteorclient.renderer.text.VanillaTextRenderer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HudRenderer {
    public static final HudRenderer INSTANCE = new HudRenderer();

    private static final double SCALE_TO_HEIGHT = 1.0 / 18.0;

    private final Hud hud = Hud.get();
    private final List<Runnable> postTasks = new ArrayList<>();

    private final Int2ObjectMap<FontHolder> fontsInUse = new Int2ObjectOpenHashMap<>();
    private final LoadingCache<Integer, FontHolder> fontCache = CacheBuilder.newBuilder()
        .maximumSize(4)
        .expireAfterAccess(Duration.ofMinutes(10))
        .removalListener(notification -> {
            if (notification.wasEvicted())
                ((FontHolder) notification.getValue()).destroy();
        })
        .build(CacheLoader.from(HudRenderer::loadFont));

    public DrawContext drawContext;
    public double delta;

    private HudRenderer() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    public void begin(DrawContext drawContext) {
        Renderer2D.COLOR.begin();

        this.drawContext = drawContext;
        this.delta = Utils.frameTime;

        if (!hud.hasCustomFont()) {
            VanillaTextRenderer.INSTANCE.scaleIndividually = true;
            VanillaTextRenderer.INSTANCE.begin();
        }
    }

    public void end() {
        Renderer2D.COLOR.render(new MatrixStack());

        if (hud.hasCustomFont()) {
            // Render fonts that were visited this frame and move to cache which weren't visited
            for (Iterator<FontHolder> it = fontsInUse.values().iterator(); it.hasNext(); ) {
                FontHolder fontHolder = it.next();

                if (fontHolder.visited) {
                    GL.bindTexture(fontHolder.font.texture.getGlId());
                    fontHolder.getMesh().render(null);
                }
                else {
                    it.remove();
                    fontCache.put(fontHolder.font.getHeight(), fontHolder);
                }

                fontHolder.visited = false;
            }
        }
        else {
            VanillaTextRenderer.INSTANCE.end();
            VanillaTextRenderer.INSTANCE.scaleIndividually = false;
        }

        for (Runnable task : postTasks) task.run();
        postTasks.clear();

        this.drawContext = null;
    }

    public void line(double x1, double y1, double x2, double y2, Color color) {
        Renderer2D.COLOR.line(x1, y1, x2, y2, color);
    }

    /**
     * 元素背景填充（自动套用全局圆角）。
     *
     * <p>圆角半径取自 {@code Hud.cornerRadius}（设置里可调，0 = 直角）。
     * 元素有 19+ 个、背景调用几十处，逐个改不现实 —— 在这里统一介入：
     * 元素只要用本方法画背景就自动获得圆角。
     */
    public void quad(double x, double y, double width, double height, Color color) {
        double r = hud.cornerRadius.get();
        // 细条（如 ActiveModulesHud 用 quad 拼的 1~2px 边框）不套圆角：
        // 半径超过短边一半时形状退化，且会把边框切成碎片。
        double minSide = Math.min(width, height);
        if (r > 0.5 && minSide >= r * 2.0 + 1.0) {
            roundedQuad(x, y, width, height, r, color);
        } else {
            rawQuad(x, y, width, height, color);
        }
    }

    /** 直通绘制，不做圆角；供 roundedQuad 内部使用，避免递归。 */
    private void rawQuad(double x, double y, double width, double height, Color color) {
        Renderer2D.COLOR.quad(x, y, width, height, color);
    }

    /**
     * 圆角矩形填充。
     *
     * <p>底层只有三角形原语（{@code Renderer2D} 无圆角支持），这里用
     * 「中间十字 + 四角扇形」剖分近似：中央整列 + 左右中段覆盖非圆角区，
     * 四个角按角度区间细分扇形。屏幕坐标系 y 轴向下，故角度 0=右、90=下、
     * 180=左、270=上。
     *
     * <p>半径自动收敛到不超过半宽/半高，避免形状自交；
     * 半径 &lt;= 0.5 或尺寸非法时退化为普通 quad，保持与旧行为一致。
     */
    public void roundedQuad(double x, double y, double width, double height, double radius, Color color) {
        if (width <= 0 || height <= 0) return;

        double r = Math.min(radius, Math.min(width, height) / 2.0);
        if (r <= 0.5) {
            rawQuad(x, y, width, height, color);
            return;
        }

        // 中间十字：中央整列（含上下边的中段）+ 左右两侧中段
        rawQuad(x + r, y, width - r * 2, height, color);
        rawQuad(x, y + r, r, height - r * 2, color);
        rawQuad(x + width - r, y + r, r, height - r * 2, color);

        // 四角扇形
        int seg = Math.max(3, Math.min(8, (int) Math.ceil(r)));
        cornerFan(x + r, y + r, r, 180, 270, seg, color);                    // 左上
        cornerFan(x + width - r, y + r, r, 270, 360, seg, color);            // 右上
        cornerFan(x + width - r, y + height - r, r, 0, 90, seg, color);      // 右下
        cornerFan(x + r, y + height - r, r, 90, 180, seg, color);            // 左下
    }

    /** 圆角扇形（以 cx,cy 为圆心，角度区间 [startDeg,endDeg] 内的三角扇）。 */
    private void cornerFan(double cx, double cy, double r, double startDeg, double endDeg,
                           int seg, Color color) {
        double a0 = Math.toRadians(startDeg);
        double a1 = Math.toRadians(endDeg);
        for (int i = 0; i < seg; i++) {
            double t0 = a0 + (a1 - a0) * i / seg;
            double t1 = a0 + (a1 - a0) * (i + 1) / seg;
            Renderer2D.COLOR.triangle(
                    cx, cy,
                    cx + Math.cos(t0) * r, cy + Math.sin(t0) * r,
                    cx + Math.cos(t1) * r, cy + Math.sin(t1) * r,
                    color);
        }
    }
    public void quad(double x, double y, double width, double height, Color cTopLeft, Color cTopRight, Color cBottomRight, Color cBottomLeft) {
        Renderer2D.COLOR.quad(x, y, width, height, cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void triangle(double x1, double y1, double x2, double y2, double x3, double y3, Color color) {
        Renderer2D.COLOR.triangle(x1, y1, x2, y2, x3, y3, color);
    }

    public void texture(Identifier id, double x, double y, double width, double height, Color color) {
        GL.bindTexture(id);

        Renderer2D.TEXTURE.begin();
        Renderer2D.TEXTURE.texQuad(x, y, width, height, color);
        Renderer2D.TEXTURE.render(null);
    }

    public double text(String text, double x, double y, Color color, boolean shadow, double scale) {
        if (scale == -1) scale = hud.getTextScale();

        if (!hud.hasCustomFont()) {
            VanillaTextRenderer.INSTANCE.scale = scale * 2;
            return VanillaTextRenderer.INSTANCE.render(text, x, y, color, shadow);
        }

        FontHolder fontHolder = getFontHolder(scale, true);

        Font font = fontHolder.font;
        Mesh mesh = fontHolder.getMesh();

        double width;

        if (shadow) {
            int preShadowA = CustomTextRenderer.SHADOW_COLOR.a;
            CustomTextRenderer.SHADOW_COLOR.a = (int) (color.a / 255.0 * preShadowA);

            width = font.render(mesh, text, x + 1, y + 1, CustomTextRenderer.SHADOW_COLOR, scale);
            font.render(mesh, text, x, y, color, scale);

            CustomTextRenderer.SHADOW_COLOR.a = preShadowA;
        }
        else {
            width = font.render(mesh, text, x, y, color, scale);
        }

        return width;
    }
    public double text(String text, double x, double y, Color color, boolean shadow) {
        return text(text, x, y, color, shadow, -1);
    }

    public double textWidth(String text, boolean shadow, double scale) {
        if (text.isEmpty()) return 0;

        if (hud.hasCustomFont()) {
            double width = getFont(scale).getWidth(text, text.length());
            return (width + (shadow ? 1 : 0)) * (scale == -1 ? hud.getTextScale() : scale) + (shadow ? 1 : 0);
        }

        VanillaTextRenderer.INSTANCE.scale = (scale == -1 ? hud.getTextScale() : scale) * 2;
        return VanillaTextRenderer.INSTANCE.getWidth(text, shadow);
    }
    public double textWidth(String text, boolean shadow) {
        return textWidth(text, shadow, -1);
    }
    public double textWidth(String text, double scale) {
        return textWidth(text, false, scale);
    }
    public double textWidth(String text) {
        return textWidth(text, false, -1);
    }

    public double textHeight(boolean shadow, double scale) {
        if (hud.hasCustomFont()) {
            double height = getFont(scale).getHeight() + 1;
            return (height + (shadow ? 1 : 0)) * (scale == -1 ? hud.getTextScale() : scale);
        }

        VanillaTextRenderer.INSTANCE.scale = (scale == -1 ? hud.getTextScale() : scale) * 2;
        return VanillaTextRenderer.INSTANCE.getHeight(shadow);
    }
    public double textHeight(boolean shadow) {
        return textHeight(shadow, -1);
    }
    public double textHeight() {
        return textHeight(false, -1);
    }

    public void post(Runnable task) {
        postTasks.add(task);
    }

    public void item(ItemStack itemStack, int x, int y, float scale, boolean overlay, String countOverlay) {
        RenderUtils.drawItem(drawContext, itemStack, x, y, scale, overlay, countOverlay);
    }

    public void item(ItemStack itemStack, int x, int y, float scale, boolean overlay) {
        RenderUtils.drawItem(drawContext, itemStack, x, y, scale, overlay);
    }

    private FontHolder getFontHolder(double scale, boolean render) {
        // Calculate font height
        if (scale == -1) scale = hud.getTextScale();
        int height = (int) Math.round(scale / SCALE_TO_HEIGHT);

        // Check fonts in use
        FontHolder fontHolder = fontsInUse.get(height);
        if (fontHolder != null) {
            if (render) fontHolder.visited = true;
            return fontHolder;
        }

        // Create font if not in cache otherwise remove from cache and add to fonts in use
        if (render) {
            fontHolder = fontCache.getIfPresent(height);
            if (fontHolder == null) fontHolder = loadFont(height);
            else fontCache.invalidate(height);

            fontsInUse.put(height, fontHolder);
            fontHolder.visited = true;

            return fontHolder;
        }

        // Otherwise get from cache
        return fontCache.getUnchecked(height);
    }

    private Font getFont(double scale) {
        return getFontHolder(scale, false).font;
    }

    @EventHandler
    private void onCustomFontChanged(CustomFontChangedEvent event) {
        // Need to destroy both fonts in use and in cache because they were not evicted from the cache automatically
        for (FontHolder fontHolder : fontsInUse.values()) fontHolder.destroy();
        for (FontHolder fontHolder : fontCache.asMap().values()) fontHolder.destroy();

        // Clear collections
        fontsInUse.clear();
        fontCache.invalidateAll();
    }

    private static FontHolder loadFont(int height) {
        byte[] data = Utils.readBytes(Fonts.RENDERER.fontFace.toStream());
        ByteBuffer buffer = BufferUtils.createByteBuffer(data.length).put(data).flip();

        return new FontHolder(new Font(buffer, height));
    }

    private static class FontHolder {
        public final Font font;
        public boolean visited;

        private Mesh mesh;

        public FontHolder(Font font) {
            this.font = font;
        }

        public Mesh getMesh() {
            if (mesh == null) mesh = new ShaderMesh(Shaders.TEXT, DrawMode.Triangles, Mesh.Attrib.Vec2, Mesh.Attrib.Vec2, Mesh.Attrib.Color);
            if (!mesh.isBuilding()) mesh.begin();
            return mesh;
        }

        public void destroy() {
            font.texture.clearGlId();
            if (mesh != null) mesh.destroy();
        }
    }
}
