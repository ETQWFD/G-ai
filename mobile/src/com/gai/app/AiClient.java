package com.gai.app;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** OpenAI 兼容 AI 接口客户端：支持任意自定义 AI / 中转 / 聚合 API */
public class AiClient {

    /** 归一化接口地址：去掉末尾 / 与 /chat/completions */
    public static String normalizeBase(String raw) {
        String s = raw == null ? "" : raw.trim();
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        if (s.endsWith("/chat/completions")) {
            s = s.substring(0, s.length() - "/chat/completions".length());
        }
        return s;
    }

    /** 列出该 Key 下全部可用模型（GET {base}/models） */
    public static List<String> listModels(String rawBase, String key) throws Exception {
        String base = normalizeBase(rawBase);
        HttpURLConnection c = (HttpURLConnection) new URL(base + "/models").openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setRequestProperty("Authorization", "Bearer " + key);
        c.setRequestProperty("Accept", "application/json");
        int code = c.getResponseCode();
        String body = readAll(code >= 400 ? c.getErrorStream() : c.getInputStream());
        c.disconnect();
        if (code >= 400) throw new Exception("接口返回 " + code + ": " + cut(body, 200));

        List<String> out = new ArrayList<String>();
        Object root = Json.parse(body);
        Map<String, Object> m = Json.asMap(root);
        if (m != null) {
            Object data = m.get("data");
            if (data instanceof List) {
                for (Object it : (List<?>) data) {
                    Map<String, Object> im = Json.asMap(it);
                    if (im != null && im.get("id") != null) out.add(String.valueOf(im.get("id")));
                }
            }
            if (out.isEmpty() && m.get("models") instanceof List) {
                for (Object it : (List<?>) m.get("models")) {
                    Map<String, Object> im = Json.asMap(it);
                    if (im != null) {
                        Object id = im.get("name");
                        if (id == null) id = im.get("model");
                        if (id == null) id = im.get("id");
                        if (id != null) out.add(String.valueOf(id));
                    }
                }
            }
        } else if (root instanceof List) {
            for (Object it : (List<?>) root) {
                if (it != null) out.add(String.valueOf(it));
            }
        }
        return out;
    }

    /** 对话补全，返回模型文本回复 */
    public static String chat(String rawBase, String key, String model,
                              List<Map<String, Object>> messages, double temperature) throws Exception {
        String base = normalizeBase(rawBase);
        Map<String, Object> req = new LinkedHashMap<String, Object>();
        req.put("model", model);
        req.put("messages", messages);
        req.put("stream", Boolean.FALSE);
        req.put("temperature", temperature);

        HttpURLConnection c = (HttpURLConnection) new URL(base + "/chat/completions").openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(20000);
        c.setReadTimeout(120000);
        c.setRequestProperty("Authorization", "Bearer " + key);
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        c.setDoOutput(true);
        OutputStream os = c.getOutputStream();
        os.write(Json.stringify(req).getBytes("UTF-8"));
        os.flush();
        os.close();

        int code = c.getResponseCode();
        String body = readAll(code >= 400 ? c.getErrorStream() : c.getInputStream());
        c.disconnect();
        if (code >= 400) throw new Exception("接口返回 " + code + ": " + cut(body, 300));

        Map<String, Object> root = Json.obj(body);
        if (root == null) return "";
        List<Object> choices = Json.gl(root, "choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> first = Json.asMap(choices.get(0));
            Map<String, Object> msg = Json.asMap(Json.get(first, "message"));
            Object content = msg == null ? null : msg.get("content");
            if (content != null) return String.valueOf(content);
        }
        return "";
    }

    public static Map<String, Object> msg(String role, String content) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) sb.append(line).append('\n');
        r.close();
        return sb.toString();
    }

    private static String cut(String s, int n) {
        return s == null ? "" : (s.length() > n ? s.substring(0, n) : s);
    }
}
