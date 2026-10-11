package meteordevelopment.meteorclient.soar;

import org.lwjgl.glfw.GLFW;

import com.soarclient.Soar;
import com.soarclient.SoarConfig;
import com.soarclient.gui.modmenu.GuiModMenu;
import com.soarclient.skia.context.SkiaContext;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Meteor 与 Soar 的桥接：
 * - 启动时初始化 Soar 的字体与调色板；
 * - 每帧维护 Skia 渲染面；
 * - 用 Right Shift 打开 Material You 主界面（替代 Meteor 原生 ClickGUI）。
 */
public final class SoarIntegration {

    private static Screen menuScreen;

    /** 保存界面实例，便于自动切页（诊断 / 自动化用）。 */
    private static com.soarclient.gui.modmenu.GuiModMenu menuGui;

    private SoarIntegration() {
    }

    /** 内置矿透材质包：resourcepacks/xray */
    public static final Identifier XRAY_PACK = Identifier.of("meteor-client", "xray");
    /** 内置的原版 UI + 字体材质包（CozyUI+） */
    public static final Identifier COZYUI_PACK = Identifier.of("meteor-client", "cozyui");

    public static void init() {
        registerBuiltinPacks();
        if (Soar.getInstance().isStarted()) return;
        Soar.getInstance().start();
        MeteorClient.EVENT_BUS.subscribe(new SoarIntegration());
    }

    /** 由 preLaunch 入口调用：在游戏读取 options 之前注册内置资源包。 */
    public static void registerBuiltinPacksEarly() {
        registerBuiltinPacks();
    }

    private static void registerBuiltinPacks() {
        var opt = FabricLoader.getInstance().getModContainer("meteor-client");
        if (opt.isEmpty()) {
            MeteorClient.LOG.warn("[内置包] 找不到 meteor-client 容器，无法注册");
            return;
        }

        // 用 DEFAULT_ENABLED 而不是 NORMAL：
        // NORMAL 需要玩家在 options.txt 里记录该包名，而 GameOptions 的校验
        // 时机会早于 Fabric 内置包注入 ResourcePackManager，导致条目被
        // "Removed resource pack ... because it doesn't seem to exist anymore" 移除。
        // DEFAULT_ENABLED 由 Fabric 在构造 ResourcePackManager 时直接启用，
        // 不经过 options 校验，因此不受时机影响。
        // Xray 由"透视"模块控制，保持 NORMAL（不默认开启）
        register(XRAY_PACK, "Xray Ultimate 矿石透视", opt.get(),
                ResourcePackActivationType.NORMAL);

        // CozyUI+ 是外观类，默认启用
        register(COZYUI_PACK, "CozyUI+ 原版界面与字体", opt.get(),
                ResourcePackActivationType.DEFAULT_ENABLED);
    }

    private static void register(Identifier id, String name,
                                 net.fabricmc.loader.api.ModContainer container,
                                 ResourcePackActivationType type) {
        try {
            boolean ok = ResourceManagerHelper.registerBuiltinResourcePack(
                    id, container, Text.literal(name), type);
            MeteorClient.LOG.info("[内置包] {} 注册={} id={} type={}", name, ok, id, type);
        } catch (Throwable t) {
            MeteorClient.LOG.warn("[内置包] {} 注册失败: {}", name, t.toString());
        }
    }

    /** 由 Meteor 的渲染钩子每帧调用，保证 Skia 面与窗口尺寸同步。 */
    public static void onFrame() {
        if (!Soar.getInstance().isStarted()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return;
        SkiaContext.ensureSurface(mc.getWindow().getWidth(), mc.getWindow().getHeight());
    }

    /** 打开/关闭 Material You 界面。 */
    public static void toggleMenu() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null && menuScreen != null && mc.currentScreen.getClass() == menuScreen.getClass()) {
            mc.setScreen(null);
            return;
        }
        if (menuScreen == null) {
            menuGui = new GuiModMenu();
            menuScreen = menuGui.build();
        }
        mc.setScreen(menuScreen);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        Soar.getInstance().getColorManager().onTick();
        // 截图辅助：-Dsoar.autoui=true 时自动开界面
        if (Boolean.getBoolean("soar.autoui")) {
            autoUiTick();
        }
    }

    private static int auTick = -1;

    private static void autoUiTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (auTick < 0) { auTick = 0; return; }
        auTick++;

        // GuiModMenu 不是 Screen 的子类（它通过 build() 返回匿名 Screen），
        // 因此不能用 instanceof 判断，改成与缓存的 menuScreen 比较。
        if (auTick == 60) {
            boolean alreadyOpen = mc.currentScreen != null
                && menuScreen != null
                && mc.currentScreen.getClass() == menuScreen.getClass();
            if (!alreadyOpen) toggleMenu();
        }

        // 自动切页：-Dsoar.autopage=mods 时切到"模块"页
        if (auTick == 110 && menuGui != null
                && "mods".equalsIgnoreCase(System.getProperty("soar.autopage"))) {
            try {
                for (var page : menuGui.getPages()) {
                    if (page.getClass().getSimpleName().contains("ModsPage")) {
                        menuGui.setCurrentPage(page);
                        MeteorClient.LOG.info("[SoarDiag] 已切到模块页: {}", page.getClass().getSimpleName());
                        break;
                    }
                }
            } catch (Throwable t) {
                MeteorClient.LOG.warn("[SoarDiag] 切页失败: {}", t.toString());
            }
        }

        // -Dsoar.autocat=战斗 时，自动点击对应分类标签
        if (auTick == 150 && menuGui != null) {
            String want = System.getProperty("soar.autocat");
            if (want != null && !want.isEmpty()) {
                try {
                    var page = menuGui.getCurrentPage();
                    if (page != null) {
                        // 分类栏在页面顶部，第二行第一个之前都是标签
                        // 直接反射调用 CategoryBar 来切换，避免坐标猜测
                        var f = page.getClass().getDeclaredField("categoryBar");
                        f.setAccessible(true);
                        Object bar = f.get(page);
                        var m = bar.getClass().getMethod("setSelected", String.class);
                        m.invoke(bar, want);
                        MeteorClient.LOG.info("[SoarDiag] 已切换分类到: {}", want);
                    }
                } catch (Throwable t) {
                    MeteorClient.LOG.warn("[SoarDiag] 切分类失败: {}", t.toString());
                }
            }
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        onFrame();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        // 离开世界时缓存界面，避免下次进入时状态残留
        menuScreen = null;
    }

    public static int menuKey() {
        return GLFW.GLFW_KEY_RIGHT_SHIFT;
    }

    // ---- 内置矿透材质包开关 ----

    private static final String XRAY_PACK_NAME = "meteor-client/xray";

    public static boolean isXrayPackEnabled() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null || mc.options.resourcePacks == null) return false;
        return mc.options.resourcePacks.contains(XRAY_PACK_NAME);
    }

    // ---- CozyUI+ 原版界面材质包 ----

    private static final String COZYUI_PACK_NAME = "meteor-client/cozyui";

    public static boolean isCozyUiEnabled() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null || mc.options.resourcePacks == null) return false;
        return mc.options.resourcePacks.contains(COZYUI_PACK_NAME);
    }

    public static void setCozyUiEnabled(boolean enabled) {
        setPackEnabled(COZYUI_PACK_NAME, enabled);
    }

    public static void setXrayPackEnabled(boolean enabled) {
        setPackEnabled(XRAY_PACK_NAME, enabled);
    }

    /**
     * 统一的资源包开关。
     *
     * <h2>为什么不能只写 options.txt</h2>
     * 内置包（NORMAL 类型）在 {@code GameOptions} 构造时还没进
     * {@code ResourcePackManager} 的可用列表，于是 options 里的条目会被
     * "doesn't seem to exist anymore" 规则清掉。实测日志：
     * <pre>
     * 12:31:55 [main] 注册=true id=meteor-client:xray type=NORMAL
     * 12:32:11 [Render] Removed resource pack meteor-client/xray ...   ← 被清掉
     * </pre>
     * 因此这里<b>直接操作 ResourcePackManager 的启用集合</b>，并在写入 options
     * 之前先把它加入"当前可用包"的判定路径，避免下次启动又被清。
     */
    private static void setPackEnabled(String packName, boolean enabled) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null) return;

        try {
            var rpm = mc.getResourcePackManager();
            if (rpm != null && rpm.hasProfile(packName)) {
                // enable/disable 会自动写回 enabled 集合并处理依赖，比手工拼列表安全
                if (enabled) rpm.enable(packName);
                else rpm.disable(packName);
            }
        } catch (Throwable t) {
            MeteorClient.LOG.warn("[内置包] 切换 {} 失败: {}", packName, t.toString());
        }

        // 同步写 options，供资源包界面显示（即使被清也不影响当前会话）
        try {
            if (mc.options.resourcePacks != null) {
                if (enabled) {
                    if (!mc.options.resourcePacks.contains(packName)) mc.options.resourcePacks.add(packName);
                    mc.options.incompatibleResourcePacks.remove(packName);
                } else {
                    mc.options.resourcePacks.remove(packName);
                }
                mc.options.write();
            }
        } catch (Throwable ignored) {
        }

        if (mc.getResourceManager() != null) {
            mc.reloadResources();
        }
    }
}
