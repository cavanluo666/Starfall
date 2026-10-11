package com.soarclient;

import com.soarclient.management.color.ColorManager;
import com.soarclient.skia.font.Fonts;
import com.soarclient.utils.language.I18n;
import com.soarclient.utils.language.Language;

/**
 * 合并版核心单例。原版管理音乐/浏览器/WebSocket/用户等子系统，
 * 这些在合并版中由 Meteor 提供或直接裁剪，这里只保留 Material3 UI 所需的骨架。
 */
public class Soar {

    private static final Soar instance = new Soar();

    private final String name = "Meteor+Soar";
    private final String version = "1.0.0";

    private long launchTime;
    private ColorManager colorManager;
    private boolean started;

    public void start() {
        if (started) return;
        started = true;
        launchTime = System.currentTimeMillis();
        Fonts.loadAll();
        I18n.setLanguage(Language.CHINESE);
        colorManager = new ColorManager();
    }

    public static Soar getInstance() {
        return instance;
    }

    public boolean isStarted() {
        return started;
    }

    public String getName() { return name; }
    public String getVersion() { return version; }
    public long getLaunchTime() { return launchTime; }

    public ColorManager getColorManager() {
        if (colorManager == null) colorManager = new ColorManager();
        return colorManager;
    }
}
