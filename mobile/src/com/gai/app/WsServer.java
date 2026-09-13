package com.gai.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 极简 WebSocket 服务端（RFC 6455），用于我的世界基岩版 /connect 连接。
 * 游戏端在聊天框输入 /connect 本机IP:端口 后，即可收发命令与事件。
 */
public class WsServer {

    public interface Listener {
        void onStatus(String status);
        void onConnected();
        void onDisconnected();
        void onCommandResponse(String commandLine, int statusCode, String statusMessage);
        void onPlayerMessage(String sender, String message);
        void onError(String err);
    }

    private final int port;
    private final Listener listener;
    private ServerSocket server;
    private Thread thread;
    private volatile boolean running;
    private volatile Socket gameSocket;
    private volatile OutputStream out;

    public WsServer(int port, Listener l) {
        this.port = port;
        this.listener = l;
    }

    public void start() throws IOException {
        if (running) return;
        running = true;
        thread = new Thread(new Runnable() {
            public void run() { acceptLoop(); }
        }, "gai-ws");
        thread.setDaemon(true);
        thread.start();
    }

    private void acceptLoop() {
        try {
            server = new ServerSocket();
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress(port));
            listener.onStatus("连接服务已启动（端口 " + port + "），等待游戏连接…\n在游戏聊天框输入 /connect 本机IP:" + port);
            while (running) {
                Socket s = server.accept();
                handleClient(s);
            }
        } catch (IOException e) {
            if (running) listener.onError("连接服务异常: " + e.getMessage());
        }
    }

    private void handleClient(final Socket s) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    InputStream in = s.getInputStream();
                    OutputStream os = s.getOutputStream();
                    StringBuilder hs = new StringBuilder();
                    int b;
                    while ((b = in.read()) != -1) {
                        hs.append((char) b);
                        if (hs.toString().endsWith("\r\n\r\n")) break;
                        if (hs.length() > 8192) throw new IOException("handshake too long");
                    }
                    String head = hs.toString();
                    String key = null;
                    for (String line : head.split("\r\n")) {
                        if (line.toLowerCase().startsWith("sec-websocket-key:")) {
                            key = line.substring(line.indexOf(':') + 1).trim();
                        }
                    }
                    if (key == null) throw new IOException("no sec-websocket-key");
                    String accept;
                    try {
                        accept = acceptKey(key);
                    } catch (Exception ex) {
                        throw new IOException("handshake failed");
                    }
                    os.write(("HTTP/1.1 101 Switching Protocols\r\n" +
                            "Upgrade: websocket\r\n" +
                            "Connection: Upgrade\r\n" +
                            "Sec-WebSocket-Accept: " + accept + "\r\n\r\n").getBytes("UTF-8"));
                    os.flush();
                    gameSocket = s;
                    out = os;
                    listener.onConnected();
                    frameLoop(in, os);
                } catch (IOException e) {
                    if (running) listener.onDisconnected();
                } finally {
                    closeGame();
                }
            }
        }, "gai-ws-client").start();
    }

    private void frameLoop(InputStream in, OutputStream os) throws IOException {
        while (running) {
            int b0 = in.read();
            if (b0 < 0) break;
            int b1 = in.read();
            if (b1 < 0) break;
            int opcode = b0 & 0x0F;
            boolean masked = (b1 & 0x80) != 0;
            long len = b1 & 0x7F;
            if (len == 126) {
                len = ((long) in.read() << 8) | in.read();
            } else if (len == 127) {
                long l = 0;
                for (int i = 0; i < 8; i++) l = (l << 8) | in.read();
                len = l;
            }
            if (len > 4 * 1024 * 1024) break;
            byte[] mask = new byte[4];
            if (masked) {
                int n = in.read(mask);
                if (n < 4) break;
            }
            byte[] payload = new byte[(int) len];
            int off = 0;
            while (off < len) {
                int n = in.read(payload, off, (int) len - off);
                if (n < 0) break;
                off += n;
            }
            if (masked) {
                for (int i = 0; i < payload.length; i++) payload[i] ^= mask[i % 4];
            }
            if (opcode == 0x8) { // close
                sendClose(os);
                break;
            } else if (opcode == 0x9) { // ping
                sendFrame(os, 0xA, payload);
            } else if (opcode == 0x1 || opcode == 0x2) {
                handleMessage(new String(payload, "UTF-8"));
            }
        }
    }

    private void handleMessage(String text) {
        try {
            Map<String, Object> root = Json.obj(text);
            if (root == null) return;
            Map<String, Object> header = Json.asMap(Json.get(root, "header"));
            String purpose = header == null ? null : Json.gs(header, "messagePurpose");
            if ("commandResponse".equals(purpose)) {
                Map<String, Object> body = Json.asMap(Json.get(root, "body"));
                if (body != null) {
                    String cmd = Json.gs(body, "commandLine");
                    int code = 0;
                    Object sc = body.get("statusCode");
                    if (sc instanceof Number) code = ((Number) sc).intValue();
                    String msg = Json.gs(body, "statusMessage");
                    listener.onCommandResponse(cmd == null ? "" : cmd, code, msg == null ? "" : msg);
                }
            } else if ("keepAlive".equals(purpose)) {
                String reqId = header == null ? null : Json.gs(header, "requestId");
                Map<String, Object> resp = new LinkedHashMap<String, Object>();
                Map<String, Object> h = new LinkedHashMap<String, Object>();
                h.put("requestId", reqId == null ? UUID.randomUUID().toString() : reqId);
                h.put("messagePurpose", "keepAlive");
                h.put("version", 1L);
                resp.put("header", h);
                Map<String, Object> b = new LinkedHashMap<String, Object>();
                b.put("alive", Boolean.TRUE);
                resp.put("body", b);
                sendText(Json.stringify(resp));
            } else if ("event".equals(purpose)) {
                // 游戏事件：玩家聊天等
                Map<String, Object> body = Json.asMap(Json.get(root, "body"));
                if (body != null) {
                    String eventName = Json.gs(body, "eventName");
                    if ("PlayerMessage".equals(eventName)) {
                        String sender = Json.gs(body, "sender");
                        String msg = Json.gs(body, "message");
                        if (msg != null && !msg.trim().isEmpty()) {
                            listener.onPlayerMessage(sender == null ? "玩家" : sender, msg);
                        }
                    }
                }
            }
            // 其他事件（event / error）暂不处理
        } catch (Exception e) {
            // 忽略解析错误
        }
    }

    /** 向游戏发送一条命令（自动补 /） */
    public void sendCommand(String cmd) {
        if (out == null) return;
        String c = cmd.startsWith("/") ? cmd : "/" + cmd;
        Map<String, Object> req = new LinkedHashMap<String, Object>();
        Map<String, Object> h = new LinkedHashMap<String, Object>();
        h.put("version", 1L);
        h.put("requestId", UUID.randomUUID().toString());
        h.put("messageType", "commandRequest");
        h.put("messagePurpose", "commandRequest");
        req.put("header", h);
        Map<String, Object> b = new LinkedHashMap<String, Object>();
        b.put("version", 1L);
        b.put("commandLine", c);
        Map<String, Object> origin = new LinkedHashMap<String, Object>();
        origin.put("type", "player");
        b.put("origin", origin);
        req.put("body", b);
        sendText(Json.stringify(req));
    }

    public void sendText(String text) {
        try {
            if (out != null) sendFrame(out, 0x1, text.getBytes("UTF-8"));
        } catch (IOException e) {
            // ignore
        }
    }

    private static void sendFrame(OutputStream os, int opcode, byte[] payload) throws IOException {
        synchronized (os) {
            os.write(0x80 | opcode);
            int len = payload.length;
            if (len < 126) {
                os.write(len);
            } else if (len < 65536) {
                os.write(126);
                os.write((len >> 8) & 0xFF);
                os.write(len & 0xFF);
            } else {
                os.write(127);
                long l = len;
                for (int i = 7; i >= 0; i--) os.write((int) ((l >> (8 * i)) & 0xFF));
            }
            os.write(payload);
            os.flush();
        }
    }

    private static void sendClose(OutputStream os) {
        try {
            sendFrame(os, 0x8, new byte[0]);
        } catch (IOException e) {
            // ignore
        }
    }

    public boolean isRunning() { return running; }

    public boolean isConnected() {
        return gameSocket != null && !gameSocket.isClosed();
    }

    public void stop() {
        running = false;
        closeGame();
        try {
            if (server != null) server.close();
        } catch (IOException e) {
            // ignore
        }
        server = null;
    }

    private void closeGame() {
        try {
            if (gameSocket != null) gameSocket.close();
        } catch (IOException e) {
            // ignore
        }
        gameSocket = null;
        out = null;
    }

    private static String acceptKey(String key) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(digest);
    }
}
