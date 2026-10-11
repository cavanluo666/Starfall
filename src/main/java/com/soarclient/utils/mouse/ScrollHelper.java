package com.soarclient.utils.mouse;

import com.soarclient.animation.SimpleAnimation;

/**
 * 滚动状态。
 *
 * <p>除了滚轮，还支持<b>拖动滚动条</b>（触摸板 / 无滚轮设备必需）。
 * 坐标系都是「GUI 缩放坐标」，正值表示内容向下移动。
 */
public class ScrollHelper {

    private final SimpleAnimation animation = new SimpleAnimation();

    /** 目标滚动值（负值 = 内容上移）。 */
    private float scroll;
    private float maxScroll = Float.MAX_VALUE;
    private final float minScroll = 0;

    /** 拖动状态 */
    private boolean dragging;
    /** 拖动起点：鼠标 Y 与当时的滚动值 */
    private float dragStartMouseY;
    private float dragStartScroll;

    /** 滚轮 */
    public void onScroll(double amount) {
        scroll += (float) amount * 60;
        clamp();
    }

    public void onUpdate() {
        animation.onTick(scroll, 18);
        clamp();
    }

    private void clamp() {
        scroll = Math.max(Math.min(minScroll, scroll), -maxScroll);
    }

    // ---------------- 拖动滚动条 ----------------

    /** 是否正在拖动。 */
    public boolean isDragging() {
        return dragging;
    }

    /** 内容高度 / 视口高度，用于计算滑块长度。 */
    public float getContentHeight() {
        return maxScroll + getViewportHeight();
    }

    private float viewportHeight = 1f;

    public float getViewportHeight() {
        return viewportHeight;
    }

    /**
     * 开始拖动。
     *
     * @param mouseY     鼠标 Y
     * @param thumbTop   当前滑块顶部位置
     * @param trackTop   轨道顶部
     * @param trackLen   轨道长度
     * @param thumbLen   滑块长度
     */
    public void beginDrag(float mouseY, float thumbTop, float trackTop, float trackLen, float thumbLen) {
        dragging = true;
        dragStartMouseY = mouseY;
        dragStartScroll = scroll;
        this.dragTrackTop = trackTop;
        this.dragTrackLen = trackLen;
        this.dragThumbLen = thumbLen;
    }

    private float dragTrackTop, dragTrackLen, dragThumbLen;

    /** 拖动中：把鼠标位移换算成内容位移。 */
    public void updateDrag(float mouseY) {
        if (!dragging) return;
        float trackRange = dragTrackLen - dragThumbLen;
        if (trackRange <= 0.5f) return;

        // 鼠标移动 -> 滑块移动 -> 内容按 maxScroll/trackRange 的比例滚动
        float deltaMouse = mouseY - dragStartMouseY;
        float ratio = maxScroll / trackRange;
        scroll = dragStartScroll - deltaMouse * ratio;
        clamp();
        // 拖动时动画立即跟上，避免拖了却感觉没动
        animation.setFirstTick(false);
    }

    public void endDrag() {
        dragging = false;
    }

    /** 点击轨道空白处：直接跳到该位置。 */
    public void jumpTo(float mouseY, float trackTop, float trackLen, float thumbLen) {
        float trackRange = trackLen - thumbLen;
        if (trackRange <= 0.5f) return;
        float t = (mouseY - trackTop - thumbLen / 2f) / trackRange;
        t = Math.max(0f, Math.min(1f, t));
        scroll = -t * maxScroll;
        clamp();
    }

    /** 滑块在轨道中的比例位置（0..1）。 */
    public float getProgress() {
        if (maxScroll <= 0.5f) return 0f;
        float p = -scroll / maxScroll;
        return Math.max(0f, Math.min(1f, p));
    }

    /** 滑块长度占轨道的比例（0..1）。 */
    public float getThumbRatio() {
        float content = getContentHeight();
        if (content <= 0.5f) return 1f;
        float r = viewportHeight / content;
        // 最小可抓取长度
        return Math.max(0.12f, Math.min(1f, r));
    }

    public boolean isScrollable() {
        return maxScroll > 0.5f;
    }

    // ---------------- 尺寸 ----------------

    public void setMaxScroll(float itemHeight, float itemSpace, int itemSize, int row, float height) {
        if (row <= 0) row = 1;
        int rows = (int) Math.ceil((double) itemSize / row);
        float totalHeight = rows * (itemHeight + itemSpace);
        setMaxScroll(totalHeight, height);
    }

    public void setMaxScroll(float totalHeight, float height) {
        this.viewportHeight = Math.max(1f, height);
        this.maxScroll = Math.max(0, totalHeight - height);
        clamp();
    }

    public float getValue() {
        return animation.getValue();
    }

    public void reset() {
        this.scroll = 0;
    }
}
