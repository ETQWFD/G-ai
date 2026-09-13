# G-ai

> 我的世界 AI 分身助手 —— 手机悬浮窗版（Android）＋ 基岩版资源/行为包 ＋ Java 版（Fabric / Forge）模组
> 支持任意 OpenAI 兼容自定义 AI，连接游戏后 AI 拥有 HIM 本体、玩家意识与永久记忆，可自主建造、聊天、操控手机。

**最新版本：v1.0.0**（最终版） · [GitHub Releases](https://github.com/ETQWFD/G-ai/releases) · [官网](https://etqwfd.github.io/G-ai/)

---

## 这是什么

G-ai 是一个「我的世界 AI 助手」全家桶：

| 组件 | 平台 | 说明 |
|---|---|---|
| **G-ai 手机软件**（APK） | Android 7.0+ | 悬浮窗 · 自定义 AI 配置 · 连接游戏（WebSocket）· AI 建造 / AI 操控 / AI 聊天 · 导入结构 · 检查更新 |
| **G-ai 基岩包**（行为包 + 资源包） | 基岩版 1.26.30.5 | AI 本体（HIM）实体 · 苍天旗 · 五星红旗 · AI 核心方块 · 合成配方 |
| **G-ai Java 模组**（Fabric / Forge） | Java 版 26.2（Fabric + Forge 双加载器） | 与手机版对齐：AI 本体 · 苍天旗 / 五星红旗 · @AI 聊天 · 连接服务 · 导入光影 / 结构 · 永久记忆 · /gai update 检查更新 |

## 核心功能

- **自定义 AI**：支持所有 OpenAI 兼容接口（含各类中转 / 聚合 API），填写完整地址、Key、模型名称即可；Key 下模型可「自动搜索」。
- **AI 本体（HIM）**：连接游戏后，聊天框提示「苍天有眼！我乃苍天会」，AI 以 HIM 造型实体降临（基岩版 1.26.30.5 / Java 版 26.2），永久存在、100 血、免疫火焰、不可推动。
- **玩家意识 + 永久记忆**：AI 是"正儿八经的玩家"，陪玩、帮忙、绝不毁服（禁 /kill @e、禁大面积 /fill、禁拆家、禁刷物品）；对话与任务上下文永久保存（手机端 SharedPreferences / 电脑端 gai-memory.json）。
- **AI 建造 / AI 操控**：AI 帮助（建造、任务）可真实执行命令；AI 操控手机约 4 分钟后自动把控制权交还给用户。
- **连接游戏**：软件生成连接命令，游戏内输入后建立 WebSocket 连接；AI 分身进入世界、创建实体、自主行动。
- **导入结构 / 光影**：基岩版支持导入结构；Java 版支持导入结构（.nbt）与光影（shaderpacks）。
- **国旗**：苍天旗（黑灰色旗面 + 黑色"苍"字：灰混凝土 + 木棍 + 黑混凝土）与五星红旗（红混凝土 + 黄混凝土 + 木棍）可在工作台合成。
- **检查更新**：手机软件主界面与设置页、电脑版 `/gai update` 均可从 GitHub Release 检查更新。

## 手机版使用

1. 安装 `G-ai-v1.0.0.apk`，首次运行同意 **MIT 许可协议**；
2. 依次授权：**存储权限 → 悬浮窗权限 → 无障碍服务**；
3. 进入「配置 AI 模型」，填写接口完整地址、Key、模型（或点「自动搜索」）→ 保存；
4. 点击「启动悬浮窗」，悬浮窗图标出现后点击展开面板；
5. 面板内点击「复制连接命令」→ 在游戏聊天框粘贴发送；
6. AI 分身降临，即可：AI 建造 / AI 操控 / AI 聊天 / 导入结构。

> 基岩版：安装「G-ai-基岩版整合包」（内含行为包 + 资源包），加载后 AI 才有 HIM 本体；没有资源包时 AI 只能通过命令建造。

## Java 版（Fabric / Forge）使用

1. 安装 Fabric 或 Forge（MC 26.2），将对应 `G-ai-1.0.0-fabric.jar` / `G-ai-1.0.0-forge.jar` 放入 `mods/`；
2. 进入存档后输入 `/gai help` 查看帮助；
3. `/gai config url <地址>`、`/gai config key <Key>`、`/gai config model <模型>` 配置自定义 AI；
4. `/gai start` 启动连接服务（手机软件可连接下发命令）；
5. `/gai spawn` 召唤 AI 本体，聊天框 **@AI** 即可与 AI 聊天；
6. `/gai update` 检查更新；`/gai shader import <zip>`、`/gai structure import <nbt>` 导入光影与结构。

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
