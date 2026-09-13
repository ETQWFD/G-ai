package com.gai.app;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 轻量 JSON 解析与序列化（无第三方依赖） */
public class Json {

    public static Object parse(String s) {
        return new P(s).value();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> obj(String s) {
        Object o = parse(s);
        return o instanceof Map ? (Map<String, Object>) o : null;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : null;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asList(Object o) {
        return o instanceof List ? (List<Object>) o : null;
    }

    public static Object get(Map<String, Object> m, String k) {
        return m == null ? null : m.get(k);
    }

    /** 取字符串字段 */
    public static String gs(Object o, String k) {
        Object v = get(asMap(o), k);
        return v == null ? null : String.valueOf(v);
    }

    /** 取列表字段 */
    public static List<Object> gl(Object o, String k) {
        Object v = get(asMap(o), k);
        return asList(v);
    }

    public static String stringify(Object o) {
        StringBuilder sb = new StringBuilder();
        write(o, sb);
        return sb.toString();
    }

    private static void write(Object o, StringBuilder sb) {
        if (o == null) {
            sb.append("null");
        } else if (o instanceof String) {
            writeStr((String) o, sb);
        } else if (o instanceof Boolean || o instanceof Number) {
            sb.append(String.valueOf(o));
        } else if (o instanceof Map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                if (!first) sb.append(',');
                first = false;
                writeStr(String.valueOf(e.getKey()), sb);
                sb.append(':');
                write(e.getValue(), sb);
            }
            sb.append('}');
        } else if (o instanceof List) {
            sb.append('[');
            boolean first = true;
            for (Object it : (List<?>) o) {
                if (!first) sb.append(',');
                first = false;
                write(it, sb);
            }
            sb.append(']');
        } else {
            writeStr(String.valueOf(o), sb);
        }
    }

    private static void writeStr(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    /** 递归下降解析器 */
    static class P {
        final String s;
        int i;

        P(String s) { this.s = s; }

        void ws() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        Object value() {
            ws();
            if (i >= s.length()) return null;
            char c = s.charAt(i);
            if (c == '{') return obj();
            if (c == '[') return arr();
            if (c == '"') return str();
            if (c == 't') { i += 4; return Boolean.TRUE; }
            if (c == 'f') { i += 5; return Boolean.FALSE; }
            if (c == 'n') { i += 4; return null; }
            return num();
        }

        Map<String, Object> obj() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            i++; ws();
            if (i < s.length() && s.charAt(i) == '}') { i++; return m; }
            while (true) {
                ws();
                String k = str();
                ws();
                if (i < s.length() && s.charAt(i) == ':') i++;
                Object v = value();
                m.put(k, v);
                ws();
                if (i >= s.length()) break;
                char c = s.charAt(i);
                if (c == ',') { i++; continue; }
                if (c == '}') { i++; break; }
            }
            return m;
        }

        List<Object> arr() {
            List<Object> l = new ArrayList<Object>();
            i++; ws();
            if (i < s.length() && s.charAt(i) == ']') { i++; return l; }
            while (true) {
                Object v = value();
                l.add(v);
                ws();
                if (i >= s.length()) break;
                char c = s.charAt(i);
                if (c == ',') { i++; continue; }
                if (c == ']') { i++; break; }
            }
            return l;
        }

        String str() {
            i++;
            StringBuilder sb = new StringBuilder();
            while (i < s.length()) {
                char c = s.charAt(i++);
                if (c == '"') break;
                if (c == '\\') {
                    if (i >= s.length()) break;
                    char e = s.charAt(i++);
                    switch (e) {
                        case 'n': sb.append('\n'); break;
                        case 't': sb.append('\t'); break;
                        case 'r': sb.append('\r'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'u':
                            if (i + 4 <= s.length()) {
                                sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                                i += 4;
                            }
                            break;
                        default: sb.append(e);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        Object num() {
            int st = i;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (Character.isDigit(c) || c == '-' || c == '.' || c == 'e' || c == 'E' || c == '+') i++;
                else break;
            }
            String t = s.substring(st, i);
            if (t.indexOf('.') >= 0 || t.indexOf('e') >= 0 || t.indexOf('E') >= 0) {
                try { return Double.parseDouble(t); } catch (NumberFormatException e) { return 0d; }
            }
            try { return Long.parseLong(t); } catch (NumberFormatException e) { return 0L; }
        }
    }
}
