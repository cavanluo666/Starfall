/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.text;

import meteordevelopment.meteorclient.renderer.*;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

public class CustomTextRenderer implements TextRenderer {
    public static final Color SHADOW_COLOR = new Color(60, 60, 60, 180);

    private final Mesh mesh = new ShaderMesh(Shaders.TEXT, DrawMode.Triangles, Mesh.Attrib.Vec2, Mesh.Attrib.Vec2, Mesh.Attrib.Color);

    public final FontFace fontFace;

    private final Font[] fonts;
    private Font font;

    private boolean building;
    private boolean scaleOnly;
    private double fontScale = 1;
    private double scale = 1;

    /**
     * 图集未覆盖的字形（本工程的打包范围不含 CJK，见 Font.packRange）。
     * 含这类字符的文本改走原版文字渲染器 —— 它经 unifont 支持中文。
     * 否则中文会全部回退成空格，表现为「按钮边框在、文字一片空白」。
     */
    private boolean needsFallback(String text) {
        if (text == null || text.isEmpty()) return false;

        Font f = building ? font : fonts[0];

        for (int i = 0; i < text.length(); i++) {
            if (!f.hasChar(text.charAt(i))) return true;
        }

        return false;
    }

    public CustomTextRenderer(FontFace fontFace) {
        this.fontFace = fontFace;

        byte[] bytes = Utils.readBytes(fontFace.toStream());
        ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length).put(bytes).flip();

        fonts = new Font[5];
        for (int i = 0; i < fonts.length; i++) {
            fonts[i] = new Font(buffer, (int) Math.round(27 * ((i * 0.5) + 1)));
        }
    }

    @Override
    public void setAlpha(double a) {
        mesh.alpha = a;
    }

    @Override
    public void begin(double scale, boolean scaleOnly, boolean big) {
        if (building) throw new RuntimeException("CustomTextRenderer.begin() called twice");

        if (!scaleOnly) mesh.begin();

        if (big) {
            this.font = fonts[fonts.length - 1];
        }
        else {
            double scaleA = Math.floor(scale * 10) / 10;

            int scaleI;
            if (scaleA >= 3) scaleI = 5;
            else if (scaleA >= 2.5) scaleI = 4;
            else if (scaleA >= 2) scaleI = 3;
            else if (scaleA >= 1.5) scaleI = 2;
            else scaleI = 1;

            font = fonts[scaleI - 1];
        }

        this.building = true;
        this.scaleOnly = scaleOnly;

        this.fontScale = font.getHeight() / 27.0;
        this.scale = 1 + (scale - fontScale) / fontScale;
    }

    @Override
    public double getWidth(String text, int length, boolean shadow) {
        if (text.isEmpty()) return 0;

        // 含图集外字形（如中文）时委托原版渲染器，其宽度算法保持一致
        if (needsFallback(text)) {
            return VanillaTextRenderer.INSTANCE.getWidth(text, length, shadow);
        }

        Font font = building ? this.font : fonts[0];
        return (font.getWidth(text, length) + (shadow ? 1 : 0)) * scale / 1.5;
    }

    @Override
    public double getHeight(boolean shadow) {
        Font font = building ? this.font : fonts[0];
        return (font.getHeight() + 1 + (shadow ? 1 : 0)) * scale / 1.5;
    }

    @Override
    public double render(String text, double x, double y, Color color, boolean shadow) {
        // 含图集外字形（如中文）→ 原版渲染器。这是「HUD 编辑器里中文变空白」的修复点
        if (needsFallback(text)) {
            VanillaTextRenderer vr = VanillaTextRenderer.INSTANCE;
            vr.scaleIndividually = false;
            boolean wasBuilding = vr.isBuilding();
            if (!wasBuilding) vr.begin(this.scale / 1.5);   // begin 内部会乘 2，对齐当前字号
            vr.render(text, x, y, color, shadow);
            if (!wasBuilding) vr.end();
            return vr.getWidth(text, shadow);
        }

        boolean wasBuilding = building;
        if (!wasBuilding) begin();

        double width;
        if (shadow) {
            int preShadowA = SHADOW_COLOR.a;
            SHADOW_COLOR.a = (int) (color.a / 255.0 * preShadowA);

            width = font.render(mesh, text, x + fontScale * scale / 1.5, y + fontScale * scale / 1.5, SHADOW_COLOR, scale / 1.5);
            font.render(mesh, text, x, y, color, scale / 1.5);

            SHADOW_COLOR.a = preShadowA;
        }
        else {
            width = font.render(mesh, text, x, y, color, scale / 1.5);
        }

        if (!wasBuilding) end();
        return width;
    }

    @Override
    public boolean isBuilding() {
        return building;
    }

    @Override
    public void end(MatrixStack matrices) {
        if (!building) throw new RuntimeException("CustomTextRenderer.end() called without calling begin()");

        if (!scaleOnly) {
            mesh.end();

            GL.bindTexture(font.texture.getGlId());
            mesh.render(matrices);
        }

        building = false;
        scale = 1;
    }

    public void destroy() {
        mesh.destroy();
    }
}
