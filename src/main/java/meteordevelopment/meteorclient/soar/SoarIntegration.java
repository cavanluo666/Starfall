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

import java.util.List;

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
        logPackState();
        if (Soar.getInstance().isStarted()) return;
        Soar.getInstance().start();
        MeteorClient.EVENT_BUS.subscribe(new SoarIntegration());
    }

    /**
     * 打印两个内置资源包的真实加载状态。
     *
     * <p>判据取 {@code ResourcePackManager} 的启用集合（即本次会话真正参与加载的包），
     * 而不是 {@code options.resourcePacks} —— 后者只是意图/显示状态，曾经因为名字写成
     * {@code namespace/path} 而与实际不符，出现"界面显示已启用、材质没生效"。
     */
    private static void logPackState() {
        try {
            var rpm = MinecraftClient.getInstance().getResourcePackManager();
            if (rpm == null) {
                MeteorClient.LOG.warn("[内置包] ResourcePackManager 为 null，跳过状态检查");
                return;
            }
            MeteorClient.LOG.info("[内置包] 实际加载状态: Xray={} CozyUI={}",
                    rpm.getEnabledIds().contains(XRAY_PACK_NAME),
                    rpm.getEnabledIds().contains(COZYUI_PACK_NAME));
        } catch (Throwable t) {
            MeteorClient.LOG.warn("[内置包] 状态检查失败: {}", t.toString());
        }
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

    /**
     * 内置资源包在 {@code ResourcePackManager} 里的真实 profile 名形式不确定
     * （Fabric 不同版本用过 {@code namespace:path} / {@code namespace/path}
     * / {@code builtin/...} 等多种拼法），因此这里生成全部候选名并逐个探测，
     * 而不是赌某一个写法。
     *
     * @param canonical 规范名，格式 {@code namespace:path}
     * @return 真实存在的 profile 名；都不存在时返回 null
     */
    private static String resolvePackName(net.minecraft.resource.ResourcePackManager rpm, String canonical) {
        for (String c : packNameCandidates(canonical)) {
            if (rpm.hasProfile(c)) return c;
        }
        return null;
    }

    private static List<String> packNameCandidates(String canonical) {
        int i = canonical.indexOf(':');
        String ns = i < 0 ? "" : canonical.substring(0, i);
        String path = i < 0 ? canonical : canonical.substring(i + 1);
        return List.of(
                ns + ":" + path,
                ns + "/" + path,
                "builtin/" + ns + "/" + path,
                "builtin/" + ns + ":" + path,
                path
        );
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

        // -Dsoar.autoxray=true 时自动启用透视模块，用于验证"模块 → 内置材质包"这条链路
        if (auTick == 80 && Boolean.getBoolean("soar.autoxray")) {
            try {
                var xray = meteordevelopment.meteorclient.systems.modules.Modules.get()
                        .get(meteordevelopment.meteorclient.systems.modules.render.Xray.class);
                if (xray != null && !xray.isActive()) {
                    xray.toggle();
                    MeteorClient.LOG.info("[SoarDiag] 已切换透视模块，isActive={} 材质包开关={}",
                            xray.isActive(), xray.isUsingResourcePack());
                }
            } catch (Throwable t) {
                MeteorClient.LOG.warn("[SoarDiag] 切换透视模块失败: {}", t.toString());
            }
        }

        // 较晚再打一次状态，确认模块启用的包没有被后续流程重置
        if (auTick == 200) {
            logPackState();
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

    /**
     * 资源包在 {@code ResourcePackManager} 里的 profile 名。
     *
     * <p>格式是 {@code namespace:path}（Fabric 的 {@code ModNioResourcePack} 用
     * {@code Identifier.toString()} 作为 packId），**不是** {@code namespace/path}。
     * 实测 {@code ResourcePackManager.getIds()} 里就是 {@code meteor-client:xray}。
     *
     * <p>曾经误写成 {@code meteor-client/xray}，导致 {@code hasProfile()} 恒为 false、
     * 包每次启动都被 "Removed resource pack ... because it doesn't seem to exist anymore"
     * 清掉，两个内置包从未真正加载。
     */
    private static final String XRAY_PACK_NAME = "meteor-client:xray";

    public static boolean isXrayPackEnabled() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null || mc.options.resourcePacks == null) return false;
        return mc.options.resourcePacks.contains(XRAY_PACK_NAME);
    }

    // ---- CozyUI+ 原版界面材质包 ----

    private static final String COZYUI_PACK_NAME = "meteor-client:cozyui";

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
     * <p><b>注意：profile 名的确切写法由 Fabric 决定，不能想当然。</b>本项目踩过的坑：
     * 曾把名字写成 {@code namespace/path}，实测 {@code getIds()} 里却是
     * {@code namespace:path}；改对之后仍不生效，说明还存在其它形式。
     * 因此现在统一走 {@link #resolvePackName} 逐个候选探测，两处（Manager 与 options）
     * 都使用探测到的**同一个真实名字**，避免只改一处造成状态不一致。
     * <p>两处都要写：{@code ResourcePackManager} 的启用集合决定本次会话实际加载哪些包；
     * {@code options.resourcePacks} 决定下次启动和资源包界面里的显示状态。
     * 只改前者会让界面显示不同步，只改后者会被上面的清理规则删掉。
     */
    private static void setPackEnabled(String packName, boolean enabled) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null) return;

        // 解析真实 profile 名；两处（Manager 与 options）必须用同一个名字，否则必然有一处失效
        String resolved = packName;
        try {
            var rpm = mc.getResourcePackManager();
            if (rpm != null) {
                String r = resolvePackName(rpm, packName);
                if (r != null) {
                    resolved = r;
                    if (enabled) rpm.enable(resolved);
                    else rpm.disable(resolved);
                    MeteorClient.LOG.info("[内置包] {} -> 真实 profile 名 '{}'，已{}",
                            packName, resolved, enabled ? "启用" : "停用");
                } else {
                    MeteorClient.LOG.warn("[内置包] ResourcePackManager 里找不到 '{}' 的任何候选名，无法切换", packName);
                }
            }
        } catch (Throwable t) {
            MeteorClient.LOG.warn("[内置包] 切换 {} 失败: {}", packName, t.toString());
        }

        // 同步写 options，供下次启动与资源包界面显示
        try {
            if (mc.options.resourcePacks != null) {
                // 先把历史遗留的错误写法清掉，避免它再次触发 "doesn't seem to exist anymore"
                for (String c : packNameCandidates(packName)) {
                    if (!c.equals(resolved)) mc.options.resourcePacks.remove(c);
                }
                if (enabled) {
                    if (!mc.options.resourcePacks.contains(resolved)) mc.options.resourcePacks.add(resolved);
                    mc.options.incompatibleResourcePacks.remove(resolved);
                } else {
                    mc.options.resourcePacks.remove(resolved);
                }
                mc.options.write();
                MeteorClient.LOG.info("[内置包] options.resourcePacks 现为 {}（resolved='{}'）",
                        mc.options.resourcePacks, resolved);
            }
        } catch (Throwable t) {
            MeteorClient.LOG.warn("[内置包] 写 options 失败: {}", t.toString());
        }

        if (mc.getResourceManager() != null) {
            mc.reloadResources();
        }
    }
}
