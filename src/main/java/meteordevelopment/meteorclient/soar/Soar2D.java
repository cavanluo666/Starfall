package meteordevelopment.meteorclient.soar;

import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;

/**
 * 新增功能共用的 2D 绘制入口。
 *
 * <p>Meteor 的 2D 绘制要求成对使用 {@code Renderer2D.COLOR.begin()} /
 * {@code render(null)}，且必须在同一批次内画完。这里把它封装成
 * {@link #run(Draw)}，避免每个模块各写一套导致批次错乱。
 *
 * <p>文字走 {@code TextRenderer.get()}，需要单独 begin/end。
 */
public final class Soar2D {

    private Soar2D() {
    }

    public interface Draw {
        void draw();
    }

    /** 在一个 2D 批次里执行绘制。 */
    public static void run(Draw draw) {
        Renderer2D.COLOR.begin();
        try {
            draw.draw();
        } finally {
            Renderer2D.COLOR.render(null);
        }
    }

    // ---------------- 基础图元 ----------------

    public static void rect(double x, double y, double w, double h, Color c) {
        Renderer2D.COLOR.quad(x, y, w, h, c);
    }

    public static void gradient(double x, double y, double w, double h,
                                Color tl, Color tr, Color br, Color bl) {
        Renderer2D.COLOR.quad(x, y, w, h, tl, tr, br, bl);
    }

    /** 圆角矩形：用 4 个矩形 + 4 个三角形近似，够用且不需要 shader。 */
    public static void roundedRect(double x, double y, double w, double h, double radius, Color c) {
        if (radius <= 0.01) {
            rect(x, y, w, h, c);
            return;
        }
        double r = Math.min(radius, Math.min(w, h) / 2.0);

        // 中间十字
        rect(x + r, y, w - 2 * r, h, c);
        rect(x, y + r, r, h - 2 * r, c);
        rect(x + w - r, y + r, r, h - 2 * r, c);

        // 四角：用扇形近似（这里用三角扇）
        corner(x + r, y + r, r, Math.PI, Math.PI * 1.5, c);              // 左上
        corner(x + w - r, y + r, r, Math.PI * 1.5, Math.PI * 2, c);      // 右上
        corner(x + w - r, y + h - r, r, 0, Math.PI * 0.5, c);            // 右下
        corner(x + r, y + h - r, r, Math.PI * 0.5, Math.PI, c);          // 左下
    }

    private static void corner(double cx, double cy, double r, double a0, double a1, Color c) {
        int seg = 6;
        for (int i = 0; i < seg; i++) {
            double t0 = a0 + (a1 - a0) * i / seg;
            double t1 = a0 + (a1 - a0) * (i + 1) / seg;
            Renderer2D.COLOR.triangle(
                cx, cy,
                cx + Math.cos(t0) * r, cy + Math.sin(t0) * r,
                cx + Math.cos(t1) * r, cy + Math.sin(t1) * r,
                c
            );
        }
    }

    /** 有粗细的线段。 */
    public static void line(double x1, double y1, double x2, double y2, double thickness, Color c) {
        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 0.0001) return;
        double nx = -dy / len * thickness / 2.0;
        double ny = dx / len * thickness / 2.0;

        // 两个三角形拼成矩形
        Renderer2D.COLOR.triangle(x1 + nx, y1 + ny, x1 - nx, y1 - ny, x2 + nx, y2 + ny, c);
        Renderer2D.COLOR.triangle(x2 + nx, y2 + ny, x1 - nx, y1 - ny, x2 - nx, y2 - ny, c);
    }

    /** 矩形描边。 */
    public static void outline(double x, double y, double w, double h, double t, Color c) {
        rect(x, y, w, t, c);
        rect(x, y + h - t, w, t, c);
        rect(x, y + t, t, h - 2 * t, c);
        rect(x + w - t, y + t, t, h - 2 * t, c);
    }

    // ---------------- 文字 ----------------

    /** 画一段文字，返回宽度。 */
    public static double text(String s, double x, double y, Color c, boolean shadow, double scale) {
        TextRenderer tr = TextRenderer.get();
        tr.begin(scale);
        double w = tr.getWidth(s, shadow);
        tr.render(s, x, y, c, shadow);
        tr.end();
        return w;
    }

    /** 以 (cx, cy) 为中心画文字。 */
    public static void textCentered(String s, double cx, double cy, Color c, boolean shadow, double scale) {
        TextRenderer tr = TextRenderer.get();
        tr.begin(scale);
        double w = tr.getWidth(s, shadow);
        double h = tr.getHeight(shadow);
        tr.render(s, cx - w / 2.0, cy - h / 2.0, c, shadow);
        tr.end();
    }

    /** 测量文字高度（会临时开一个批次）。 */
    public static double textHeight(double scale) {
        TextRenderer tr = TextRenderer.get();
        tr.begin(scale);
        double h = tr.getHeight(false);
        tr.end();
        return h;
    }

    /** 只测量宽度（会临时开一个批次）。 */
    public static double textWidth(String s, double scale) {
        TextRenderer tr = TextRenderer.get();
        tr.begin(scale);
        double w = tr.getWidth(s, false);
        tr.end();
        return w;
    }

    // ---------------- 颜色 ----------------

    public static Color alpha(Color c, double factor) {
        return new Color(c.r, c.g, c.b, (int) Math.max(0, Math.min(255, c.a * factor)));
    }
}
