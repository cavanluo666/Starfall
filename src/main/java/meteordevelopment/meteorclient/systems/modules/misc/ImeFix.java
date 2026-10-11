/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ChatScreen;


/**
 * 输入法修复：在游戏内自动关闭输入法，避免按键被输入法截走。
 *
 * <p>用 GLFW 的原生窗口句柄调 Windows 的 IMM32 接口
 * （{@code ImmAssociateContext}）把输入法上下文与窗口解绑。
 * 这样中文环境下 WASD 不会被输入法吞掉，打开聊天时也不会自动弹出候选框。
 *
 * <p>只在 Windows 生效；其它平台静默跳过。
 */
public class ImeFix extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> keepInChat = sgGeneral.add(new BoolSetting.Builder()
        .name("聊天时保留")
        .description("打开聊天框时恢复输入法，方便输入中文。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
        .name("调试输出")
        .description("在聊天栏输出输入法状态的切换信息。")
        .defaultValue(false)
        .build()
    );

    private static com.sun.jna.Pointer savedContextPtr = null;
    private static boolean contextCleared = false;

    private boolean lastInChat = false;

    public ImeFix() {
        super(Categories.Misc, "ime-fix", "游戏内自动关闭输入法，解决中文输入法抢占按键的问题（仅 Windows）。");
        initNative();
    }

    /** 用 JNA 直接绑定 imm32.dll，避免依赖 jna-platform 的类清单。 */
    private interface Imm32 extends com.sun.jna.Library {
        com.sun.jna.Pointer ImmGetContext(com.sun.jna.Pointer hwnd);
        boolean ImmReleaseContext(com.sun.jna.Pointer hwnd, com.sun.jna.Pointer ctx);
        com.sun.jna.Pointer ImmAssociateContext(com.sun.jna.Pointer hwnd, com.sun.jna.Pointer ctx);
    }

    private static Imm32 imm32;
    private static boolean nativeReady = false;

    private static void initNative() {
        if (nativeReady) return;
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("win")) return;
        try {
            imm32 = com.sun.jna.Native.load("imm32", Imm32.class);
            nativeReady = true;
        } catch (Throwable t) {
            nativeReady = false;
        }
    }

    @Override
    public void onActivate() {
        lastInChat = false;
    }

    @Override
    public void onDeactivate() {
        restoreIme();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.getWindow() == null) return;

        boolean inChat = mc.currentScreen instanceof ChatScreen;

        if (inChat != lastInChat) {
            lastInChat = inChat;
            if (inChat && keepInChat.get()) {
                restoreIme();
            } else {
                disableIme();
            }
        }
    }

    /**
     * 通过 GLFW 的窗口句柄禁用输入法。
     *
     * <p>用 GLFW 自带的 {@code glfwSetInputMode} 无法控制输入法，
     * 因此这里用反射调用 JNA（若存在）操作 IMM32。
     * 依赖不存在时退化为不操作，并在开启调试时给出提示。
     */
    private void disableIme() {
        initNative();
        if (!nativeReady || mc.getWindow() == null) return;

        try {
            com.sun.jna.Pointer hwnd = new com.sun.jna.Pointer(mc.getWindow().getHandle());
            com.sun.jna.Pointer ctx = imm32.ImmGetContext(hwnd);
            if (ctx != null) {
                savedContextPtr = ctx;
                imm32.ImmAssociateContext(hwnd, null);
                imm32.ImmReleaseContext(hwnd, ctx);
                contextCleared = true;
                if (debug.get()) info("已关闭输入法");
            }
        } catch (Throwable t) {
            if (debug.get()) warning("关闭输入法失败：" + t.getClass().getSimpleName());
        }
    }

    private void restoreIme() {
        if (!contextCleared || !nativeReady || mc.getWindow() == null) return;
        try {
            com.sun.jna.Pointer hwnd = new com.sun.jna.Pointer(mc.getWindow().getHandle());
            imm32.ImmAssociateContext(hwnd, savedContextPtr);
            contextCleared = false;
            if (debug.get()) info("已恢复输入法");
        } catch (Throwable ignored) {
        }
    }
}
