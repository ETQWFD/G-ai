package com.gai.mod;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.level.GameType;

import java.lang.reflect.Field;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tab 列表假玩家：让 AI（离线账号 et2416444244@outlook.com）像真人玩家一样
 * 出现在按 Tab 打开的玩家列表里。
 *
 * MC 26.2 中 ClientboundPlayerInfoUpdatePacket 的公开构造器需要真实 ServerPlayer，
 * 但 Entry 是公开 record（9 参）。因此：用真实玩家构造包 → 反射把 entries 替换成
 * 假 AI 条目 → 发送给目标玩家。整个流程全程 try-catch，任何一步失败都静默降级，
 * 绝不影响模组其他功能（AI 名字/聊天/建造照常）。
 */
public final class FakeTabPlayer {

    /** 固定 UUID（AI 的"账号"），保证每次进入都显示为同一个玩家。 */
    public static final UUID FAKE_UUID = UUID.fromString("7c2f0f36-1a3b-4c5d-8e6f-9a0b1c2d3e4f");
    public static final String FAKE_NAME = "et2416444244@outlook.com";

    private static volatile KeyPair CACHED_KEY;

    private FakeTabPlayer() {}

    /** 向指定玩家（viewer）的 Tab 列表加入 AI 假玩家条目。失败自动忽略。 */
    public static void addToTab(ServerPlayer viewer) {
        if (viewer == null) return;
        try {
            GameProfile profile = new GameProfile(FAKE_UUID, FAKE_NAME);
            Instant expires = Instant.now().plus(Duration.ofDays(365));
            byte[] sig = new byte[0];
            ProfilePublicKey.Data pkd = new ProfilePublicKey.Data(expires, cachedKey().getPublic(), sig);
            RemoteChatSession.Data session = new RemoteChatSession.Data(FAKE_UUID, pkd);

            ClientboundPlayerInfoUpdatePacket.Entry entry = new ClientboundPlayerInfoUpdatePacket.Entry(
                    FAKE_UUID, profile, true, 0, GameType.SURVIVAL,
                    Component.literal(FAKE_NAME), true, 0, session);

            ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(
                    ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, viewer);

            Field f = ClientboundPlayerInfoUpdatePacket.class.getDeclaredField("entries");
            f.setAccessible(true);
            List<ClientboundPlayerInfoUpdatePacket.Entry> list = new ArrayList<>();
            list.add(entry);
            f.set(packet, list);

            viewer.connection.send(packet);
        } catch (Throwable ignored) {
            // 静默降级：Tab 列表失败不影响 AI 本体、聊天、建造
        }
    }

    private static KeyPair cachedKey() {
        KeyPair k = CACHED_KEY;
        if (k == null) {
            try {
                KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
                gen.initialize(512);
                k = gen.generateKeyPair();
                CACHED_KEY = k;
            } catch (Throwable t) {
                k = null;
            }
        }
        return k;
    }
}
