package com.soarclient.utils.language;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class I18n {

    private static final Map<String, String> translateMap = new HashMap<>();
    private static Language currentLanguage = Language.CHINESE;

    private I18n() {
    }

    public static void setLanguage(Language language) {
        currentLanguage = language;
        load(language);
    }

    private static void load(Language language) {
        translateMap.clear();
        // 先加载英文兜底，再用目标语言覆盖，避免缺项显示成 key
        readInto("assets/soar/languages/en.lang");
        if (language != Language.ENGLISH) {
            readInto(String.format("assets/soar/languages/%s.lang", language.getId()));
        }
    }

    private static void readInto(String resourcePath) {
        try (InputStream in = I18n.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) return;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                reader.lines()
                        .filter(line -> !line.isBlank() && !line.startsWith("#"))
                        .map(line -> line.split("=", 2))
                        .filter(parts -> parts.length == 2)
                        .forEach(parts -> translateMap.put(parts[0].trim(), parts[1].trim()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 查表；缺失时回退到 Meteor 的英文字面量汉化表，最后原样返回。 */
    public static String get(String key) {
        if (key == null) return "";
        String v = translateMap.get(key);
        if (v != null) return v;
        return meteordevelopment.meteorclient.soar.SoarI18n.t(key);
    }

    public static boolean has(String key) {
        return translateMap.containsKey(key) || meteordevelopment.meteorclient.soar.SoarI18n.has(key);
    }

    public static Language getCurrentLanguage() {
        return currentLanguage;
    }
}
