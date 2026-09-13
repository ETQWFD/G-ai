package com.gai.app;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** 结构导入：把 .mcstructure 文件写入我的世界各世界目录的 structures 文件夹 */
public class StructureImporter {

    public static String importStructure(Context ctx, Uri uri) {
        String name = "gai_structure_" + System.currentTimeMillis() + ".mcstructure";
        try {
            InputStream in = ctx.getContentResolver().openInputStream(uri);
            if (in == null) return "导入失败：无法读取所选文件";
            String n0 = queryName(ctx, uri);
            if (n0 != null && !n0.trim().isEmpty()) {
                name = n0.trim();
                if (!name.toLowerCase().endsWith(".mcstructure")) name = name + ".mcstructure";
            }

            byte[] data = readAll(in);
            in.close();
            if (data.length == 0) return "导入失败：文件为空";

            String base = name.substring(0, name.length() - ".mcstructure".length());

            List<File> dirs = findStructureDirs();
            int copied = 0;
            for (File d : dirs) {
                if (d.exists() || d.mkdirs()) {
                    File f = new File(d, name);
                    FileOutputStream fos = new FileOutputStream(f);
                    fos.write(data);
                    fos.close();
                    copied++;
                }
            }

            // 兜底：同时保存一份到下载目录
            File fallback = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "G-ai/structures");
            if (fallback.exists() || fallback.mkdirs()) {
                File f = new File(fallback, name);
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(data);
                fos.close();
            }

            if (copied > 0) {
                return "导入成功！已写入 " + copied + " 个世界的 structures 目录（" + name + "）。\n游戏中输入 /structure load " + base + " 即可加载。";
            }
            return "已保存到 下载/G-ai/structures/" + name + "。\n未找到可写入的世界目录。请确认：1) 已授予「所有文件访问」权限；2) 已安装游戏并创建过世界；3) 若仍失败，请用文件管理器把文件复制到 游戏目录/minecraftWorlds/你的世界/structures/ 下。";
        } catch (SecurityException se) {
            return "导入失败：系统限制访问游戏目录（Android 11+）。\n已尝试保存到 下载/G-ai/structures/" + name + "，请用文件管理器手动复制到世界目录的 structures 文件夹。";
        } catch (Exception e) {
            return "导入失败：" + e.getMessage();
        }
    }

    private static String queryName(Context ctx, Uri uri) {
        try {
            android.database.Cursor c = ctx.getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    String n = c.getString(idx);
                    c.close();
                    return n;
                }
                c.close();
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private static List<File> findStructureDirs() {
        List<File> out = new ArrayList<File>();
        File ext = Environment.getExternalStorageDirectory();
        // 国际版（com.mojang.minecraftpe）新旧路径
        addWorldDirs(out, new File(ext,
                "Android/data/com.mojang.minecraftpe/files/games/com.mojang/minecraftWorlds"));
        addWorldDirs(out, new File(ext, "games/com.mojang/minecraftWorlds"));
        // 网易版常见路径
        addWorldDirs(out, new File(ext,
                "Android/data/com.netease.x19/files/games/com.netease.x19/minecraftWorlds"));
        addWorldDirs(out, new File(ext,
                "Android/data/com.netease.mc/files/games/com.netease.mc/minecraftWorlds"));
        addWorldDirs(out, new File(ext, "games/com.netease/minecraftWorlds"));
        return out;
    }

    private static void addWorldDirs(List<File> out, File worldsRoot) {
        File[] worlds = worldsRoot.listFiles();
        if (worlds == null) return;
        for (File w : worlds) {
            if (w.isDirectory()) {
                File s = new File(w, "structures");
                if (!out.contains(s)) out.add(s);
            }
        }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toByteArray();
    }
}
