package com.gai.app;

import android.content.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 记忆：持久保存最近 N 轮对话（玩家消息 + AI 回复），
 * 下次聊天时注入上下文，让 AI 记得玩家说过的话。
 */
public class Memory {

    private static final int MAX_TURNS = 24; // 最多记住 24 轮（约 48 条消息）

    /** 读取全部记忆消息（role + content） */
    public static List<Map<String, Object>> load(Context c) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        try {
            String raw = Prefs.memory(c);
            Object arr = Json.parse(raw);
            if (arr instanceof List) {
                for (Object o : (List<?>) arr) {
                    Map<String, Object> m = Json.asMap(o);
                    if (m != null) {
                        String role = Json.gs(m, "role");
                        String content = Json.gs(m, "content");
                        if (role != null && content != null) {
                            out.add(AiClient.msg(role, content));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 记忆损坏则清空
            Prefs.setMemory(c, "[]");
        }
        return out;
    }

    /** 追加一轮对话（用户说了什么 + AI 回什么），自动裁剪到上限并保存 */
    public static void remember(Context c, String userContent, String aiContent) {
        try {
            List<Map<String, Object>> all = new ArrayList<Map<String, Object>>();
            Object arr = Json.parse(Prefs.memory(c));
            if (arr instanceof List) {
                for (Object o : (List<?>) arr) {
                    Map<String, Object> m = Json.asMap(o);
                    if (m != null && Json.gs(m, "content") != null) all.add(m);
                }
            }
            if (userContent != null && !userContent.trim().isEmpty()) {
                Map<String, Object> u = new LinkedHashMap<String, Object>();
                u.put("role", "user");
                u.put("content", "玩家：" + userContent.trim());
                all.add(u);
            }
            if (aiContent != null && !aiContent.trim().isEmpty()) {
                Map<String, Object> a = new LinkedHashMap<String, Object>();
                a.put("role", "assistant");
                a.put("content", aiContent.trim());
                all.add(a);
            }
            while (all.size() > MAX_TURNS * 2) {
                all.remove(0);
            }
            Prefs.setMemory(c, Json.stringify(all));
        } catch (Exception e) {
            // 忽略记忆写入失败
        }
    }

    /** 清空记忆 */
    public static void clear(Context c) {
        Prefs.setMemory(c, "[]");
    }
}
