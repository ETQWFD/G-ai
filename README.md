# G-ai

> 我的世界 AI 分身助手 —— 手机悬浮窗版（Android）＋ 基岩版资源/行为包 ＋ Java 版（Fabric / Forge）模组
> 支持任意 OpenAI 兼容自定义 AI，连接游戏后 AI 拥有 HIM 本体、玩家意识与永久记忆，可自主建造、聊天、操控；电脑版带游戏内悬浮控制台。

**最新版本：v1.1.0**（修复 Java 版 26.2 启动崩溃 + 基岩包全新 .mcaddon 格式） · [GitHub Releases](https://github.com/ETQWFD/G-ai/releases) · [官网](https://etqwfd.github.io/G-ai/)

---

## 这是什么

G-ai 是一个「我的世界 AI 助手」全家桶：

| 组件 | 平台 | 说明 |
|---|---|---|
| **G-ai 手机软件**（APK） | Android 7.0+ | 悬浮窗 · 自定义 AI 配置 · 连接游戏（WebSocket）· AI 建造 / AI 操控 / AI 聊天 · 导入结构 · 检查更新 |
| **G-ai 基岩包**（行为包 + 资源包，.mcaddon） | 基岩版 1.26.30.5 | AI 本体（HIM）实体 · 苍天旗 · 五星红旗 · AI 核心方块 · 合成配方 |
| **G-ai Java 模组**（Fabric / Forge） | Java 版 26.2 | 游戏内悬浮控制台（按 G 键）· AI 本体 · 苍天旗 / 五星红旗 · @AI 聊天 · 连接服务 · 导入光影 / 结构 · 永久记忆 · /gai update 检查更新 |

## 核心功能

- **自定义 AI**：支持所有 OpenAI 兼容接口（含各类中转 / 聚合 API），填写完整地址、Key、模型名称即可；Key 下模型可「自动搜索」。
- **电脑版游戏内悬浮控制台**：进入存档后按 **G** 键打开半透明悬浮窗（不暂停游戏、可拖动标题栏、右上角有悬浮球提示），窗口内完成：自定义 AI 配置、启动/停止连接服务、创建 AI 躯体、AI 聊天、AI 帮助/建造、AI 操控 4 分钟、导入光影、导入结构、检查更新、清空记忆。
- **AI 本体（HIM）**：连接游戏后，聊天框提示「苍天有眼！我乃苍天会」，AI 以 HIM 造型实体降临（基岩版 1.26.30.5 / Java 版 26.2），永久存在、100 血、免疫火焰、不可推动。
- **玩家意识 + 永久记忆**：AI 是"正儿八经的玩家"，陪玩、帮忙、绝不毁服（禁 /kill @e、禁大面积 /fill、禁拆家、禁刷物品）；对话与任务上下文永久保存（手机端 SharedPreferences / 电脑端 config/gai-memory.json）。
- **AI 建造 / AI 操控**：AI 帮助（建造、任务）可真实执行命令；AI 操控约 4 分钟后自动把控制权交还给用户。
- **连接游戏**：软件生成连接命令，游戏内输入后建立 WebSocket 连接；AI 分身进入世界、创建实体、自主行动。
- **导入结构 / 光影**：基岩版支持导入结构；Java 版支持导入结构（.nbt）与光影（shaderpacks）。
- **国旗**：苍天旗（黑灰色旗面 + 黑色"苍"字：灰混凝土 + 木棍 + 黑混凝土）与五星红旗（红混凝土 + 黄混凝土 + 木棍）可在工作台合成。
- **检查更新**：手机软件主界面与设置页、电脑版 `/gai update` 与面板按钮均可从 GitHub Release 检查更新。

## 手机版使用

1. 安装 `G-ai-v1.1.0.apk`，首次运行同意 **MIT 许可协议**；
2. 依次授权：**存储权限 → 悬浮窗权限 → 无障碍服务**；
3. 进入「配置 AI 模型」，填写接口完整地址、Key、模型（或点「自动搜索」）→ 保存；
4. 点击「启动悬浮窗」，悬浮窗图标出现后点击展开面板；
5. 面板内点击「复制连接命令」→ 在游戏聊天框粘贴发送；
6. AI 分身降临，即可：AI 建造 / AI 操控 / AI 聊天 / 导入结构。

> 基岩版：安装「G-ai-基岩版整合包-v1.1.0.mcaddon」（行为包 + 资源包一次导入），加载后 AI 才有 HIM 本体；没有资源包时 AI 只能通过命令建造。

## Java 版（Fabric / Forge）使用

1. 安装 Fabric 或 Forge（MC 26.2），将对应 `G-ai-1.1.0-fabric.jar` / `G-ai-1.1.0-forge.jar` 放入 `mods/`；
2. 进入存档后按 **G** 键打开游戏内悬浮控制台（或 `/gai help` 查看全部命令）；
3. 面板内配置 url/key/model 并保存（等价 `/gai config url <地址>` 等）；
4. `启动连接` 启动连接服务（手机软件可连接下发命令），`创建AI躯体` 召唤 AI 本体；
5. 聊天框 **@AI** 或面板「发送聊天」即可与 AI 对话；「AI 操控4分钟」让 AI 接管巡视，到时自动交还；
6. `/gai update` 或面板「检查更新」获取最新版；面板可导入光影（.zip）与结构（.nbt）。

## 目录结构

```
mobile/        手机版 Android 工程源码（G-ai，包名 com.gai.app）
java-mod/      Java 版模组源码（common 公共 + fabric + forge）
packs/         基岩版行为包（behavior）与资源包（resource）源码
index.html     官网主页（GitHub Pages）
LICENSE        MIT License
```

## 构建

- 手机版：`cd mobile && bash build.sh`（需 JDK 17 + Android SDK build-tools 34）
- Java 版：`cd java-mod/fabric` 或 `cd java-mod/forge` 后 `gradle build`

## 许可

[MIT License](LICENSE) © 2026 G-ai
