/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

/**
 * 鼠标逻辑优化：让点击/瞄准的判定更跟手。
 *
 * <p>包含两项独立改进：
 * <ul>
 *   <li><b>消除点击粘滞</b>：松开左键时不再等待"使用物品"动画结束就允许下一次点击；</li>
 *   <li><b>忽略失焦点击</b>：窗口失去焦点时不把点击算作攻击。</li>
 * </ul>
 * 这些都只影响本机输入处理，不改变发送给服务器的行为。
 */
public class BetterMouseLogic extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> noClickSticky = sgGeneral.add(new BoolSetting.Builder()
        .name("消除点击粘滞")
        .description("松开鼠标后立刻允许下一次点击。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreUnfocused = sgGeneral.add(new BoolSetting.Builder()
        .name("忽略失焦点击")
        .description("窗口未聚焦时不响应点击。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> instantAttack = sgGeneral.add(new BoolSetting.Builder()
        .name("即时攻击")
        .description("点击瞬间就触发攻击判定，而不是等下一 tick。")
        .defaultValue(false)
        .build()
    );

    public BetterMouseLogic() {
        super(Categories.Player, "better-mouse-logic", "优化鼠标点击与瞄准判定，让操作更跟手。");
    }

    public boolean noClickSticky() {
        return isActive() && noClickSticky.get();
    }

    public boolean ignoreUnfocused() {
        return isActive() && ignoreUnfocused.get();
    }

    public boolean instantAttack() {
        return isActive() && instantAttack.get();
    }
}
