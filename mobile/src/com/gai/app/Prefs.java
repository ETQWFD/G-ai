package com.gai.app;

import android.content.Context;
import android.content.SharedPreferences;

/** G-ai 配置存储 */
public class Prefs {
    private static final String NAME = "gai_prefs";

    /** 当前版本号（最终版 1.0.0，与 GitHub Release tag 一致） */
    public static final String VERSION = "1.0.0";

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static boolean agreed(Context c) { return sp(c).getBoolean("agreed", false); }
    public static void setAgreed(Context c) { sp(c).edit().putBoolean("agreed", true).apply(); }

    public static String url(Context c) { return sp(c).getString("url", ""); }
    public static void setUrl(Context c, String v) { sp(c).edit().putString("url", v).apply(); }

    public static String key(Context c) { return sp(c).getString("key", ""); }
    public static void setKey(Context c, String v) { sp(c).edit().putString("key", v).apply(); }

    public static String model(Context c) { return sp(c).getString("model", ""); }
    public static void setModel(Context c, String v) { sp(c).edit().putString("model", v).apply(); }

    public static int port(Context c) { return sp(c).getInt("port", 8080); }
    public static void setPort(Context c, int v) { sp(c).edit().putInt("port", v).apply(); }

    public static boolean connected(Context c) { return sp(c).getBoolean("connected", false); }
    public static void setConnected(Context c, boolean v) { sp(c).edit().putBoolean("connected", v).apply(); }

    /** ---- AI 记忆（持久保存对话历史，JSON 数组） ---- */
    public static String memory(Context c) { return sp(c).getString("memory", "[]"); }
    public static void setMemory(Context c, String json) { sp(c).edit().putString("memory", json).apply(); }

    /** ---- 角色设定（玩家意识）开关，默认开启 ---- */
    public static boolean aiAware(Context c) { return sp(c).getBoolean("ai_aware", true); }
    public static void setAiAware(Context c, boolean v) { sp(c).edit().putBoolean("ai_aware", v).apply(); }
}
