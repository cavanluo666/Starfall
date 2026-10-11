package com.soarclient;

import java.io.File;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.soarclient.libraries.material3.hct.Hct;
import com.soarclient.utils.file.FileLocation;

import meteordevelopment.meteorclient.MeteorClient;

/**
 * 合并版的 UI 设置中枢（外观相关）。
 * 原来分散在 ModMenuSettings / SystemSettings / HUDModSettings 中，
 * 这里统一管理并持久化到 {@code .minecraft/meteor-client/soar-ui.json}。
 */
public final class SoarConfig {

    private static final SoarConfig INSTANCE = new SoarConfig();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Material3 种子色（HCT），默认蓝紫 */
    private Hct hct = Hct.from(220, 26, 6);
    /** 深色模式开关；false 即浅色主题 */
    private boolean darkMode = true;
    private boolean blur = true;
    private int blurIntensity = 5;
    private boolean fastBlur = true;
    /** 界面上那层遮罩的不透明度（0-255）。越低毛玻璃越通透 */
    private int scrimAlpha = 70;
    /** HUD 编辑器视觉风格：false=Material You 实底，true=液态玻璃（半透明+高光） */
    private boolean hudEditorGlass = true;

    private boolean loaded;

    private SoarConfig() {
    }

    public static SoarConfig get() {
        return INSTANCE;
    }

    public Hct getHct() {
        if (!loaded) load();
        return hct;
    }

    public void setHct(Hct hct) {
        this.hct = hct;
    }

    public boolean isDarkMode() {
        if (!loaded) load();
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        save();
    }

    /** 便捷：当前是否为浅色主题。 */
    public boolean isLightMode() {
        return !isDarkMode();
    }

    public void setLightMode(boolean light) {
        setDarkMode(!light);
    }

    public boolean isBlur() {
        return blur;
    }

    public void setBlur(boolean blur) {
        this.blur = blur;
        save();
    }

    public int getBlurIntensity() {
        return blurIntensity;
    }

    public void setBlurIntensity(int v) {
        this.blurIntensity = Math.max(1, Math.min(20, v));
        save();
    }

    public boolean isFastBlur() {
        return fastBlur;
    }

    public void setFastBlur(boolean fastBlur) {
        this.fastBlur = fastBlur;
        save();
    }

    public boolean isHudEditorGlass() {
        return hudEditorGlass;
    }

    public void setHudEditorGlass(boolean v) {
        this.hudEditorGlass = v;
        save();
    }

    public int getScrimAlpha() {
        return scrimAlpha;
    }

    public void setScrimAlpha(int v) {
        this.scrimAlpha = Math.max(0, Math.min(255, v));
        save();
    }


    // ---------------- 持久化 ----------------

    private static File file() {
        File dir = MeteorClient.FOLDER;
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "soar-ui.json");
    }

    public synchronized void load() {
        loaded = true;
        File f = file();
        if (!f.exists()) return;
        try {
            String txt = java.nio.file.Files.readString(f.toPath());
            JsonObject o = JsonParser.parseString(txt).getAsJsonObject();
            if (o.has("hue") && o.has("chroma") && o.has("tone")) {
                hct = Hct.from(o.get("hue").getAsDouble(), o.get("chroma").getAsDouble(),
                        o.get("tone").getAsDouble());
            }
            if (o.has("darkMode")) darkMode = o.get("darkMode").getAsBoolean();
            if (o.has("blur")) blur = o.get("blur").getAsBoolean();
            if (o.has("blurIntensity")) blurIntensity = o.get("blurIntensity").getAsInt();
            if (o.has("fastBlur")) fastBlur = o.get("fastBlur").getAsBoolean();
            if (o.has("scrimAlpha")) scrimAlpha = o.get("scrimAlpha").getAsInt();
            if (o.has("hudEditorGlass")) hudEditorGlass = o.get("hudEditorGlass").getAsBoolean();
        } catch (Exception e) {
            MeteorClient.LOG.warn("读取 soar-ui.json 失败: {}", e.toString());
        }
    }

    public synchronized void save() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("hue", hct.getHue());
            o.addProperty("chroma", hct.getChroma());
            o.addProperty("tone", hct.getTone());
            o.addProperty("darkMode", darkMode);
            o.addProperty("blur", blur);
            o.addProperty("blurIntensity", blurIntensity);
            o.addProperty("fastBlur", fastBlur);
            o.addProperty("scrimAlpha", scrimAlpha);
        o.addProperty("hudEditorGlass", hudEditorGlass);
            java.nio.file.Files.writeString(file().toPath(), GSON.toJson(o));
        } catch (Exception e) {
            MeteorClient.LOG.warn("写入 soar-ui.json 失败: {}", e.toString());
        }
    }
}
