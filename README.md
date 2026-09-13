# G-ai

> 我的世界 AI 分身助手 —— **基岩版（资源包+行为包，AI 完全内置）** ＋ Java 版（Fabric / Forge）模组
> 内置 AI 接口（DeepSeek-V4-Flash），**不需要手机软件**：进游戏自动创建 AI 躯体（HIM），聊天框 `@AI 你想让我干的事` 即可对话干活，AI 有自主意识与永久记忆。

**最新版本：v2.2.0** · [GitHub Releases](https://github.com/ETQWFD/G-ai/releases) · [官网](https://etqwfd.github.io/G-ai/)

---

## 这是什么

G-ai 是一个「我的世界 AI 助手」全家桶，**AI 完全内置进游戏，不需要额外软件**：

| 组件 | 平台 | 说明 |
|---|---|---|
| **G-ai 基岩包**（.mcaddon） | 基岩版 1.26.30.5 | 行为包内置 Script API 脚本：进世界自动创建 AI 躯体（HIM）、聊天框 @AI 对话、自主意识、永久记忆、真实建造；资源包含 HIM 模型与苍天旗/五星红旗 |
| **G-ai Java 模组**（Fabric / Forge） | Java 版 26.2 | 进存档自动召唤 AI 本体、聊天框 @AI 对话、按 G 打开游戏内悬浮控制台、导入光影/结构、永久记忆 |

## 核心功能

- **不需要手机软件**：AI 就在游戏包里，加载即用。
- **自动创建 AI 躯体（HIM）**：基岩版进世界 / Java 版进存档，AI 自动以 HIM 造型降临，聊天框提示「苍天有眼！我乃苍天会」。
- **聊天框 @AI 直接对话干活**：输入 `@AI 帮我造一座小木屋`，AI 思考回复并把 `CMD:` 行作为 `/setblock` 命令**真实执行**（安全过滤，绝不毁服）。
- **自主意识 + 永久记忆**：AI 是"正儿八经的玩家"，自动跟随、周期发言、陪玩帮忙、速通后建家园；基岩版记忆随世界存档、Java 版存 `config/gai-memory.json`，退出重进仍记得你。
- **内置 AI 接口**：`https://api.hcnsec.cn/v1/chat/completions` + `DeepSeek-V4-Flash`，开箱即用（Java 版可用 `/gai config` 或 G 键面板更换）。
- **国旗**：苍天旗（灰混凝土+木棍+黑混凝土）与五星红旗（红+黄混凝土+木棍）工作台合成。
- **电脑版悬浮控制台**（按 G）：配置 AI、创建躯体、AI 操控 4 分钟自动交还、导入光影/结构、检查更新、清空记忆。

## 基岩版使用（1.26.30.5）

1. 下载 `G-ai-基岩版整合包-v2.2.0.mcaddon`（行为包+资源包一次导入），在游戏设置中启用；
2. 进入世界 → 自动提示「苍天有眼！我乃苍天会」，AI 躯体（HIM）在身边；
3. 聊天框输入：`@AI 帮我造一座小木屋` → AI 回复并真实建造；
4. 苍天旗 / 五星红旗在工作台按配方合成。
> 若提示脚本需实验功能：世界设置 → 实验 → 开启「测试版 API」；对话需要网络（内置接口）。

## Java 版使用（26.2 Fabric / Forge）

1. 将 `G-ai-2.2.0-fabric.jar` / `G-ai-2.2.0-forge.jar` 放入 `mods/`；
2. 进存档自动出现 AI 本体（HIM）+ 「苍天有眼！我乃苍天会」；
3. 聊天框 `@AI ...` 对话干活；按 **G** 打开悬浮控制台（配置 AI / 创建躯体 / 操控 4 分钟 / 导入光影结构 / 检查更新 / 清空记忆）。

## 目录结构

```
mobile/        手机版 Android 工程源码（可选旧版工具，v2.2 起不再需要）
java-mod/      Java 版模组源码（common 公共 + fabric + forge）
packs/         基岩版行为包（behavior，含 scripts/）与资源包（resource）源码
index.html     官网主页（GitHub Pages）
LICENSE        MIT License
```

## 构建

- Java 版：`cd java-mod/fabric` 或 `cd java-mod/forge` 后 `gradle build`
- 基岩包：`packs/behavior` 与 `packs/resource` 分别打包为 .mcpack，再合成 .mcaddon

## 许可

[MIT License](LICENSE) © 2026 G-ai
