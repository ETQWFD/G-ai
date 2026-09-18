# G-ai 完整命令手册 v1.6.1

> 适用：基岩版（国际版 1.26.30.5 / 网易中国版）、Java 版（Fabric 26.2 / Forge 26.1.2 / Forge 26.2）
> 仓库：https://github.com/ETQWFD/G-ai · 官网：https://etqwfd.github.io/G-ai/

---

## 一、基岩版（行为包 + 资源包）

### 0. 安装与启用
1. 导入 `G-ai-基岩版整合包-v1.6.1.mcaddon`（会自动装入行为包 + 资源包两个）
2. 创建/进入世界时，在 **实验性内容** 里打开 **「测试版 API / Beta APIs」**（必须）
3. 进入世界后聊天框提示：`苍天有眼！我乃苍天会`，AI 会自动进场并创建 HIM 躯体

### 1. 召唤 / 创建 AI 躯体（任意一种）

| 方式 | 命令 | 说明 |
|---|---|---|
| 自动 | 进世界自动 | 进世界 1.5 秒后自动创建，无需手动 |
| 脚本事件 | `/scriptevent gai:spawn` | 立即在玩家身边创建/复用 AI 躯体 |
| 函数 | `/function gai_summon` | 同上，函数触发 |
| 原版命令 | `/summon gai:ai_body` | 直接召唤一个 AI 躯体实体 |
| 刷怪蛋 | 手持「G-ai AI 分身刷怪蛋」对地面使用 | 物品栏「物品」分类（和床图标同组） |

### 2. 和 AI 聊天 / 下达任务（任意一种）

| 方式 | 示例 | 说明 |
|---|---|---|
| 聊天前缀 | `@AI 帮我建一座小木屋` | 大小写不敏感，`@ai` 也行 |
| 聊天前缀 | `ai: 帮我砍树` | 冒号形式 |
| 聊天前缀 | `AI：陪我玩` | 中文全角冒号也行 |
| 斜杠直发 | `/ai 给我想个建筑方案` | 斜杠命令形式 |
| 脚本事件 | `/scriptevent gai:chat 帮我建一座小木屋` | 不经过聊天前缀解析 |

AI 会真实调用 DeepSeek-V4-Flash 接口思考，有自主意识、情绪、永久记忆（每次退出重进还记得你）。
AI 会自己干活：`CMD: /setblock ...` 放方块建造（每次最多 60 块）、`CMD: /tp`、`CMD: /say` 等非破坏性命令。
**AI 铁律：绝不毁服**——禁止 /kill /clear /op /ban /give /gamemode /difficulty /weather /time 等破坏性命令。

### 3. 获取旗帜 / 方块 / 刷怪蛋

| 命令 | 说明 |
|---|---|
| `/give @s gai:flag 1` | 苍天旗（灰旗面 + 黑色「苍」字） |
| `/give @s gai:five_star 1` | 五星红旗 |
| `/give @s gai:ai_core 1` | AI 核心方块 |
| `/give @s gai:ai_body_spawn_egg 1` | AI 分身刷怪蛋 |

物品全部在创造模式「**物品**」分类（和床图标同一组），无需命令也能直接翻到。

### 4. 合成（工作台）

| 成品 | 配方 |
|---|---|
| 苍天旗 gai:flag | 灰色混凝土 ×2 + 木棍 ×2 + 黑色混凝土 ×1 |
| 五星红旗 gai:five_star | 红色混凝土 ×2 + 木棍 ×2 + 黄色混凝土 ×1 |

### 5. 其他常用

| 命令 | 说明 |
|---|---|
| `/tp @s @e[type=gai:ai_body]` | 传送到 AI 身边 |
| `/tp @e[type=gai:ai_body] @s` | 把 AI 传送到你身边 |
| `/kill @e[type=gai:ai_body]` | 清除 AI 躯体（AI 记忆不丢，重进世界自动复活） |

> 提示：AI 是本世界的真实玩家分身（离线账号 et2416444244@outlook.com），头顶有名字，可对话、可帮忙、可陪伴。
> 基岩版玩家列表只显示真实登录玩家，AI 不会出现在玩家列表里——这是引擎限制，靠头顶名字 + 聊天互动呈现。

---

## 二、Java 版（Fabric / Forge 模组）

### 0. 安装
- Fabric：MC 26.2 + Fabric Loader 0.19.5 + Fabric API 0.160.0 → `G-ai-1.6.0-fabric.jar`
- Forge：MC 26.1.2（Forge 64.1.3）→ `G-ai-1.6.0-forge-26.1.2.jar`；MC 26.2（Forge 65.1.3）→ `G-ai-1.6.0-forge-26.2.jar`
- 放入 mods 文件夹，单人/局域网/服务器（安装同款模组）均可

### 1. 游戏内命令（全部 `/gai` 开头）

| 命令 | 说明 |
|---|---|
| `/gai spawn` | 在玩家身边创建 AI 躯体（HIM 造型，头顶显示名字） |
| `/gai chat <内容>` | 和 AI 对话 / 下达任务 |
| `/gai reset` | 清空 AI 记忆并重新初始化 |
| `/gai status` | 查看 AI 当前状态 |
| `/gai flag` | 快捷获取苍天旗 |
| `/gai star` | 快捷获取五星红旗 |

### 2. 聊天框直聊
- 输入 `@AI <内容>`（或 `@ai`、`ai: `、`AI：`、`/ai <内容>`）即可对话
- AI 会显示在 **Tab 玩家列表**（假玩家，名字 = AI 账号），聊天时有自己的发言

### 3. 原版命令

| 命令 | 说明 |
|---|---|
| `/summon gai:ai_body` | 直接召唤 AI 躯体 |
| `/give @s gai:flag 1` | 苍天旗 |
| `/give @s gai:five_star 1` | 五星红旗 |
| `/give @s gai:ai_core 1` | AI 核心方块 |
| `/kill @e[type=gai:ai_body]` | 清除 AI（记忆保留） |

### 4. 合成
与基岩版相同：苍天旗 = 灰混凝土 + 木棍 + 黑混凝土；五星红旗 = 红/黄混凝土 + 木棍。

---

## 三、网易中国版（手机/电脑互通服）

- 使用 `G-ai-网易版整合包-v1.6.1.zip`（内含行为包 + 资源包，min_engine [1,20,0]）
- 发布/加载时必须注明：**需开启实验模式**
- 命令与基岩版国际版完全一致（见第一部分）

---

## 四、内置 AI 接口（默认配置）

- API：`https://api.hcnsec.cn/v1/chat/completions`
- 模型：`DeepSeek-V4-Flash`
- 记忆：永久保存（世界存档内）
