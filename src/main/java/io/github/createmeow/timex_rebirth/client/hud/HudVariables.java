package io.github.createmeow.timex_rebirth.client.hud;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * FancyMenu 用户变量读取器（兼容 {@code user_variables.db} 文本格式）。
 *
 * <p>布局文件中以 {@code [loading_requirement:fancymenu_visibility_requirement_is_variable_value]...}
 * 引用的变量（如 {@code play:开}、{@code xyz:开}）由本类解析。变量值被缓存，
 * 游戏会话内通过 {@link #set(String, String)} 可动态修改（切换 HUD 元素显隐）。
 */
public final class HudVariables {

    private static final Map<String, String> values = new HashMap<>();
    private static boolean loaded = false;

    private HudVariables() {
    }

    /** 读取 FancyMenu 的 user_variables.db；不存在则使用内置默认值。 */
    public static void load() {
        if (loaded) return;
        loaded = true;
        // 内置默认
        values.put("play", "开");
        values.put("xyz", "开");
        values.put("op", "");
        values.put("devmode", "0");
        values.put("version", "2.54");

        Path p = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config/fancymenu/user_variables.db");
        if (!Files.isRegularFile(p)) {
            return;
        }
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(Files.newInputStream(p), StandardCharsets.UTF_8))) {
            String line;
            String name = null;
            String value = null;
            while ((line = r.readLine()) != null) {
                String t = line.trim();
                if (t.startsWith("name")) {
                    name = parseValue(t);
                } else if (t.startsWith("value")) {
                    value = parseValue(t);
                } else if (t.equals("}") && name != null) {
                    if (value != null) {
                        values.put(name, value);
                    }
                    name = null;
                    value = null;
                }
            }
        } catch (IOException e) {
            TimeX.LOGGER.warn("[Hud] 读取用户变量失败: {}", e.toString());
        }
    }

    private static String parseValue(String line) {
        int idx = line.indexOf('=');
        if (idx < 0) return "";
        return line.substring(idx + 1).trim();
    }

    public static String get(String name) {
        load();
        return values.getOrDefault(name, "");
    }

    public static boolean is(String name, String expected) {
        return expected.equals(get(name));
    }

    public static void set(String name, String value) {
        load();
        values.put(name, value);
    }
}
