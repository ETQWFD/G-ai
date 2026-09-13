package com.gai.mod;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * G-ai 模组配置：<游戏目录>/config/gai.properties
 * 支持自定义 AI（url/key/model）、WebSocket 端口等。
 */
public final class GaiConfig {

    private static final Properties PROPS = new Properties();
    private static Path file;

    private GaiConfig() {}

    public static void init(Path gameDir) {
        Path configDir = gameDir.resolve("config");
        file = configDir.resolve("gai.properties");
        try {
            Files.createDirectories(configDir);
            if (Files.exists(file)) {
                try (InputStream in = Files.newInputStream(file)) {
                    PROPS.load(in);
                }
            }
        } catch (IOException e) {
            // 配置不可读写时使用默认值
        }
        if (!PROPS.containsKey("url")) PROPS.setProperty("url", "https://api.deepseek.com/v1/chat/completions");
        if (!PROPS.containsKey("key")) PROPS.setProperty("key", "");
        if (!PROPS.containsKey("model")) PROPS.setProperty("model", "deepseek-chat");
        if (!PROPS.containsKey("port")) PROPS.setProperty("port", "8080");
        save();
    }

    private static void save() {
        if (file == null) return;
        try (OutputStream out = Files.newOutputStream(file)) {
            PROPS.store(out, "G-ai mod config");
        } catch (IOException e) {
            // ignore
        }
    }

    public static String url() { return PROPS.getProperty("url", "").trim(); }
    public static String key() { return PROPS.getProperty("key", "").trim(); }
    public static String model() { return PROPS.getProperty("model", "deepseek-chat").trim(); }
    public static int port() {
        try { return Integer.parseInt(PROPS.getProperty("port", "8080").trim()); }
        catch (Exception e) { return 8080; }
    }

    public static String set(String key, String value) {
        if (value == null) value = "";
        PROPS.setProperty(key, value.trim());
        save();
        return value.trim();
    }

    public static String get(String key) {
        return PROPS.getProperty(key, "");
    }
}
