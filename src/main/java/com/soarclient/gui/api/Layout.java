package com.soarclient.gui.api;

import com.soarclient.compat.McCompat;

/**
 * 响应式布局参数。
 *
 * <h2>为什么需要它</h2>
 * 原实现把面板写死成 938x580，再把内容硬塞进去。小窗口 / 高 GUI Scale 下
 * 面板会超出屏幕，或者被压缩到一屏只能看两三个条目。
 *
 * <p>改为：<b>面板铺满可用空间</b>（留边距），再由可用宽度决定<b>每行放几个</b>、
 * 由可用高度决定<b>可视区域多大</b>，超出部分交给滚动条。
 * 这样屏幕越大一屏看到的内容越多，而不是把内容整体放大。
 */
public final class Layout {

    private Layout() {
    }

    /** 面板相对屏幕保留的边距。 */
    public static final float MARGIN_X = 16f;
    public static final float MARGIN_Y = 14f;

    /** 导航栏宽度（随屏幕宽度浮动）。 */
    public static float navWidth() {
        float w = McCompat.scaledWidth();
        if (w < 420) return 56f;
        if (w < 640) return 72f;
        return 88f;
    }

    /** 面板宽度：尽量铺满，但有上限避免超宽屏上过于松散。 */
    public static float panelWidth() {
        float avail = McCompat.scaledWidth() - MARGIN_X * 2f;
        // 超宽屏上限 1100，避免面板拉得过长
        return Math.max(280f, Math.min(avail, 1100f));
    }

    /** 面板高度：铺满可用高度。 */
    public static float panelHeight() {
        float avail = McCompat.scaledHeight() - MARGIN_Y * 2f;
        return Math.max(180f, Math.min(avail, 720f));
    }

    public static float panelX() {
        return (McCompat.scaledWidth() - panelWidth()) / 2f;
    }

    public static float panelY() {
        return (McCompat.scaledHeight() - panelHeight()) / 2f;
    }

    /** 内容区（去掉导航栏）。 */
    public static float contentWidth() {
        return panelWidth() - navWidth();
    }

    // ---------------- 网格（模块卡片） ----------------

    /** 单个卡片的宽度（固定，避免太宽读起来累）。 */
    public static final float CARD_W = 210f;
    public static final float CARD_H = 104f;
    /** 卡片下方那条“开关条”的高度 */
    public static final float CARD_BAR_H = 30f;
    public static final float CARD_GAP_X = 16f;
    public static final float CARD_GAP_Y = 14f;

    /** 内容左右内边距。 */
    public static float contentPadX() {
        float cw = contentWidth();
        return cw < 420 ? 14f : 24f;
    }

    /** 每行可放几张卡片：由可用宽度算出来，而不是写死 3。 */
    public static int cardsPerRow() {
        float avail = contentWidth() - contentPadX() * 2f;
        int n = (int) Math.floor((avail + CARD_GAP_X) / (CARD_W + CARD_GAP_X));
        return Math.max(1, n);
    }

    // ---------------- 列表（设置项） ----------------

    /** 设置行高度：屏幕矮时压缩一点。 */
    public static float rowHeight() {
        float h = McCompat.scaledHeight();
        if (h < 300) return 52f;
        if (h < 420) return 60f;
        return 68f;
    }

    public static float rowGap() {
        float h = McCompat.scaledHeight();
        return h < 420 ? 10f : 16f;
    }

    /** 列表内容左右内边距。 */
    public static float listPadX() {
        float cw = contentWidth();
        return cw < 420 ? 14f : 26f;
    }

    /** 搜索栏宽度：随内容宽度缩放，窄屏不要占满。 */
    public static float searchWidth() {
        float cw = contentWidth();
        if (cw < 380) return Math.max(80f, cw - 100f);
        return Math.min(240f, cw * 0.45f);
    }

    // ---------------- 顶部常驻区（搜索栏 + 分类栏） ----------------
    //
    // 这三个常量必须是「单一来源」：内容起始位置、滚动条轨道起点、顶部底卡高度
    // 都由它们推导。曾经三处各写各的（searchBar 用 contentTop-52、滚动条轨道写死
    // y+92、ModsPage 顶栏卡写死 56+分类高度+14），结果是：
    //   · 分类栏有 7 个标签，中等宽度下换行成 2 行 = 54px 高，占 80..134
    //   · 滚动条轨道却从 92 开始 -> 与分类栏第二行重叠 42px，拖动条被盖住
    //   · contentTop=88 又压进分类栏第二行内部，卡片向上穿过分类栏
    // 现在统一：搜索栏 -> 分类栏 -> 内容区，依次排列且互不重叠。

    /** 顶部常驻区内边距（相对面板上边缘）。 */
    public static final float TOP_PAD = 10f;

    /**
     * 搜索栏高度。
     *
     * <p>与 {@code SearchBar} 内部高度共用这一个值：这台机器实测面板只有 212px 高，
     * 分类栏换行后还要 54px，顶栏每多 1px 都是从卡片区里抢的。原先的 42px
     * 让内容区只剩 88px（不足一张卡片高）。
     */
    public static final float SEARCH_H = 34f;

    /** 搜索栏与分类栏之间的间距。 */
    public static final float TOP_GAP = 4f;

    /** 分类栏与内容区之间的间距。 */
    public static final float CONTENT_GAP = 8f;

    /** 搜索栏在面板内的 Y 偏移。 */
    public static float searchTop() {
        return TOP_PAD;
    }

    /** 分类栏在面板内的 Y 偏移。 */
    public static float categoryTop() {
        return TOP_PAD + SEARCH_H + TOP_GAP;
    }

    /**
     * 内容起始 Y（面板内偏移，已含搜索栏 + 分类栏 + 间距）。
     *
     * @param categoryHeight 分类栏实测高度（会随标签换行变化），由 CategoryBar.getHeight() 提供
     */
    public static float contentTop(float categoryHeight) {
        return categoryTop() + Math.max(0f, categoryHeight) + CONTENT_GAP;
    }

    /** 无分类栏的页面（设置 / 关于）使用的内容起始 Y。 */
    public static float contentTop() {
        return contentTop(0f);
    }

    /** 顶部常驻区总高（搜索栏 + 间距 + 分类栏），供顶部底卡使用。 */
    public static float topAreaHeight(float categoryHeight) {
        return SEARCH_H + TOP_GAP + Math.max(0f, categoryHeight) + CONTENT_GAP;
    }
}
