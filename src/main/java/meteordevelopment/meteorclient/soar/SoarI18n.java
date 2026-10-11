package meteordevelopment.meteorclient.soar;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 汉化查表。
 * Meteor 1.21.1 的模块名/描述/设置项都是英文硬编码字面量（不是语言键），
 * 因此这里以「英文字面量 -> 中文」映射表的方式做本地化，
 * 表由官方 translations 分支的中英对照生成。
 */
public final class SoarI18n {

    private static final Map<String, String> MAP = new HashMap<>();
    private static boolean loaded;

    private SoarI18n() {
    }

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try (InputStream in = SoarI18n.class.getClassLoader()
                .getResourceAsStream("assets/meteor-client/lang/zh_cn.json")) {
            if (in == null) return;
            Type type = new TypeToken<Map<String, String>>() { }.getType();
            Map<String, String> m = new Gson().fromJson(
                    new InputStreamReader(in, StandardCharsets.UTF_8), type);
            if (m != null) MAP.putAll(m);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 有翻译返回中文，否则原样返回英文。 */
    public static String t(String english) {
        if (english == null || english.isEmpty()) return english;
        ensureLoaded();
        String v = MAP.get(english);
        return v != null ? v : english;
    }

    public static boolean has(String english) {
        if (english == null) return false;
        ensureLoaded();
        return MAP.containsKey(english);
    }

    public static int size() {
        ensureLoaded();
        return MAP.size();
    }
}
