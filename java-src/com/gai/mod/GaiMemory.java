package com.gai.mod;

import com.gai.mod.util.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * G-ai AI 永久记忆：<游戏目录>/config/gai-memory.json
 * 保存最近 MAX_TURNS 轮对话/任务上下文，重启后仍然保留。
 */
public final class GaiMemory {

    public static final int MAX_TURNS = 24;

    private static final List<Map<String, Object>> TURNS = new ArrayList<Map<String, Object>>();
    private static Path file;

    private GaiMemory() {}

    public static void init(Path gameDir) {
        Path configDir = gameDir.resolve("config");
        file = configDir.resolve("gai-memory.json");
        TURNS.clear();
        try {
            Files.createDirectories(configDir);
            if (Files.exists(file)) {
                String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                Object o = Json.parse(text);
                List<Object> list = Json.asList(o);
                if (list != null) {
                    for (Object it : list) {
                        Map<String, Object> m = Json.asMap(it);
                        if (m != null) TURNS.add(m);
                    }
                }
            }
        } catch (Exception e) {
            // 记忆文件损坏时清空重来
        }
        if (TURNS.size() > MAX_TURNS) {
            TURNS.subList(0, TURNS.size() - MAX_TURNS).clear();
        }
    }

    /** 追加一轮并落盘。role: system/user/assistant */
    public static synchronized void remember(String role, String content) {
        if (content == null || content.trim().isEmpty()) return;
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("role", role);
        m.put("content", content.length() > 400 ? content.substring(0, 400) : content);
        TURNS.add(m);
        while (TURNS.size() > MAX_TURNS) {
            TURNS.remove(0);
        }
        save();
    }

    public static synchronized List<Map<String, Object>> turns() {
        return new ArrayList<Map<String, Object>>(TURNS);
    }

    public static synchronized int size() {
        return TURNS.size();
    }

    public static synchronized void clear() {
        TURNS.clear();
        save();
    }

    private static synchronized void save() {
        if (file == null) return;
        try {
            Files.write(file, Json.stringify(TURNS).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // ignore
        }
    }
}
