package com.gai.mod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * /gai 命令树：start / stop / status / hero / spawn / ask / config / shader / structure / help。
 */
public final class GaiCommands {

    private GaiCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("gai")
                .requires(src -> src.permissions().hasPermission(
                        new net.minecraft.server.permissions.Permission.HasCommandLevel(
                                net.minecraft.server.permissions.PermissionLevel.GAMEMASTERS)))
                .then(Commands.literal("help").executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "§e§lG-ai v1.0.0 命令帮助§r\n" +
                            "§a/gai start§r 启动连接服务（外部软件/手机端可连接下发命令）\n" +
                            "§a/gai stop§r 停止连接服务\n" +
                            "§a/gai status§r 查看连接服务与 AI 配置状态\n" +
                            "§a/gai spawn§r 召唤 AI 本体（HIM 分身）\n" +
                            "§a/gai hero§r 苍天会宣言：苍天有眼！我乃苍天会\n" +
                            "§a/gai ask <任务>§r 让 AI 执行任务（自动生成并执行命令）\n" +
                            "§a/gai config url|key|model|port <值>§r 配置自定义 AI\n" +
                            "§a/gai shader import <zip路径>§r 导入光影（复制到 shaderpacks）\n" +
                            "§a/gai structure import <nbt路径>§r 导入结构（/structure load 加载）\n" +
                            "§a/gai update§r 检查 G-ai 是否有新版本（GitHub Release）\n" +
                            "§7聊天框 @AI 开头（或带“苍天”）可与 AI 聊天§r"),
                            false);
                    return 1;
                }))
                .then(Commands.literal("start").executes(ctx -> {
                    try {
                        GaiActions.startWs();
                    } catch (Exception e) {
                        ctx.getSource().sendFailure(Component.literal("启动失败: " + e.getMessage()));
                        return 0;
                    }
                    return 1;
                }))
                .then(Commands.literal("stop").executes(ctx -> {
                    GaiActions.stopWs();
                    return 1;
                }))
                .then(Commands.literal("status").executes(ctx -> {
                    String aiOk = GaiConfig.key().isEmpty() ? "§c未配置 key" : "§a已配置";
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "§e[G-ai 状态]§r\n" +
                            "连接服务: " + (GaiActions.wsRunning() ? "§a运行中" : "§7未启动") + "§r（端口 " + GaiConfig.port() + "）\n" +
                            "AI 地址: §f" + GaiConfig.url() + "§r\n" +
                            "AI 模型: §f" + GaiConfig.model() + "§r\n" +
                            "AI Key: " + aiOk + "§r"), false);
                    return 1;
                }))
                .then(Commands.literal("spawn").executes(ctx -> {
                    GaiActions.spawnAvatar();
                    return 1;
                }))
                .then(Commands.literal("hero").executes(ctx -> {
                    GaiActions.hero();
                    return 1;
                }))
                .then(Commands.literal("ask")
                        .then(Commands.argument("task", StringArgumentType.greedyString()).executes(ctx -> {
                            GaiActions.askAndExecute(ctx.getSource(), StringArgumentType.getString(ctx, "task"));
                            return 1;
                        })))
                .then(Commands.literal("config")
                        .then(Commands.literal("url")
                                .then(Commands.argument("value", StringArgumentType.greedyString()).executes(ctx -> {
                                    String v = StringArgumentType.getString(ctx, "value");
                                    GaiConfig.set("url", v);
                                    ctx.getSource().sendSuccess(() -> Component.literal("§a已设置 AI 地址: " + v), true);
                                    return 1;
                                })))
                        .then(Commands.literal("key")
                                .then(Commands.argument("value", StringArgumentType.greedyString()).executes(ctx -> {
                                    String v = StringArgumentType.getString(ctx, "value");
                                    GaiConfig.set("key", v);
                                    ctx.getSource().sendSuccess(() -> Component.literal("§a已设置 AI Key"), true);
                                    return 1;
                                })))
                        .then(Commands.literal("model")
                                .then(Commands.argument("value", StringArgumentType.greedyString()).executes(ctx -> {
                                    String v = StringArgumentType.getString(ctx, "value");
                                    GaiConfig.set("model", v);
                                    ctx.getSource().sendSuccess(() -> Component.literal("§a已设置 AI 模型: " + v), true);
                                    return 1;
                                })))
                        .then(Commands.literal("port")
                                .then(Commands.argument("value", StringArgumentType.greedyString()).executes(ctx -> {
                                    String v = StringArgumentType.getString(ctx, "value");
                                    GaiConfig.set("port", v);
                                    ctx.getSource().sendSuccess(() -> Component.literal("§a已设置端口: " + v + "（重启连接服务生效）"), true);
                                    return 1;
                                }))))
                .then(Commands.literal("shader")
                        .then(Commands.literal("import")
                                .then(Commands.argument("path", StringArgumentType.greedyString()).executes(ctx -> {
                                    GaiActions.shaderImport(ctx.getSource(), StringArgumentType.getString(ctx, "path"));
                                    return 1;
                                }))))
                .then(Commands.literal("structure")
                        .then(Commands.literal("import")
                                .then(Commands.argument("path", StringArgumentType.greedyString()).executes(ctx -> {
                                    GaiActions.structureImport(ctx.getSource(), StringArgumentType.getString(ctx, "path"));
                                    return 1;
                                }))))
                .then(Commands.literal("update").executes(ctx -> {
                    GaiActions.checkUpdate(ctx.getSource());
                    return 1;
                })));
    }
}
