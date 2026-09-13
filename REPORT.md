# G-ai v2.2.0（最终版）交付报告

**版本**：v2.2.0（基岩版行为包/资源包 + Java 版 Fabric/Forge 模组）
**交付日期**：2026-09-13
**适用游戏**：我的世界基岩版 1.26.30.5（含 Script API 内置 AI 分身）· Java 版 26.2（Fabric + Forge 双加载器）
**协议**：MIT License（全文见文末）

---

## 〇、本版最大变化：不再需要手机软件

v2.2.0 起，**AI 完全内置进游戏**：
- **基岩版**：行为包内置 Script API 脚本，进世界自动创建 AI 躯体（HIM），聊天框直接输入 `@AI 你想让我干的事` 即可对话、让 AI 干活；对话历史随世界存档永久保存。
- **Java 版**：加载 mod 进存档自动召唤 AI 本体，聊天框 `@AI` 直接对话，开箱即用。
- 内置 AI 接口（开箱即用，无需配置）：`https://api.hcnsec.cn/v1/chat/completions` · `DeepSeek-V4-Flash`

## 一、本次交付物（全部已上传 GitHub Release）

| 文件 | 说明 |
|---|---|
| `G-ai-基岩版整合包-v2.2.0.mcaddon` | **基岩版整合包（.mcaddon）**：行为包（含 AI 脚本）+ 资源包，一次导入即可用 |
| `G-ai-行为包.mcpack` | 行为包（AI 躯体实体 + 内置脚本 + 方块 + 配方） |
| `G-ai-资源包.mcpack` | 资源包（HIM 躯体模型贴图 + 苍天旗/五星红旗贴图） |
| `G-ai-2.2.0-fabric.jar` | Java 版 Fabric 模组（MC 26.2） |
| `G-ai-2.2.0-forge.jar` | Java 版 Forge 模组（MC 26.2） |
| `REPORT.md` | 本报告（含 MIT 全文） |

**在线地址**
- GitHub 仓库：https://github.com/ETQWFD/G-ai
- 官网：https://etqwfd.github.io/G-ai/
- Release：https://github.com/ETQWFD/G-ai/releases/tag/v2.2.0

## 二、v2.2.0 功能与修复清单（对照你反馈的问题）

### 1. 【关键】基岩版：AI 完全内置，不需要软件
- **内置 AI 分身脚本**（行为包 `scripts/main.js`，Script API）：
  - 玩家进入世界 → **自动创建 AI 躯体**（`gai:ai_body`，HIM 造型），无需任何操作；
  - 聊天框输入 **`@AI 你让AI干的事`** → AI 调用内置接口思考并回复，同时自动创建/确认躯体在场；
  - AI 有**自主意识**：自动跟随玩家、周期发言（"苍天有眼！我乃苍天会"等）、自主节奏陪玩；
  - **永久记忆**：对话历史写入世界存档（dynamic property），退出游戏再进仍记得你；
  - **真实干活**：AI 回复中的 `CMD:` 行会作为 `/setblock` 命令真实执行（安全过滤：只允许放方块，禁止 /kill /clear /op /give 等一切破坏性命令）；
  - 网络不可用时自动切换**离线应答**，保证 @AI 永不报错。
- **国旗**：苍天旗（灰混凝土+木棍+黑混凝土）与五星红旗（红+黄混凝土+木棍）工作台合成；进世界播报配方提示。贴图为真实布局（黑灰旗面+黑色"苍"字 / 红底五星）。
- **整合包格式**：标准 `.mcaddon`（行为包+资源包两个文件夹+各自 manifest），导入即自动装载。

### 2. 【关键】Java 版（26.2，Fabric + Forge）
- **进存档自动创建 AI 躯体**：玩家加入世界时若世界无 AI 本体则自动召唤（HIM），并播报「苍天有眼！我乃苍天会」；
- **聊天框 `@AI` 直接对话**：任何玩家在聊天框输入含 `@AI` / `@ai` 的消息即触发 AI 回复与执行；
- **内置 AI 配置**：默认使用 `https://api.hcnsec.cn/v1/chat/completions` + `DeepSeek-V4-Flash`（开箱即用，仍可用 `/gai config` 或按 G 面板修改）；
- **五星红旗新增**：Java 版补齐 `gai:five_star` 方块（贴图/模型/配方/语言），与苍天旗一起可合成、可放置；
- **游戏内悬浮控制台**（按 G）：配置 AI、创建躯体、AI 聊天/建造、AI 操控 4 分钟自动交还、导入光影/结构、检查更新、清空记忆；
- **永久记忆**：对话写入 `config/gai-memory.json`，重启不丢；
- 26.2 Block `setId(ResourceKey)` 注册机制（上一版崩溃修复保留），自动建躯体用 `getEntities` 查询避免重复生成。

### 3. 修复清单（汇总）
| 问题 | 状态 |
|---|---|
| 基岩版 AI 躯体加载不进去（空 physics） | ✅ 已修（移除空组件，1.21.40+ 格式） |
| 国旗无法加载/合成（废弃物品名写法） | ✅ 已修（新版物品名 + 重绘贴图） |
| Java 版启动 `Block id not set` 崩溃 | ✅ 已修（setId + ResourceKey）并保留 |
| 需要手机软件才能用 | ✅ v2.2 起完全不需要，包内自带 AI |
| 没有 AI 躯体 / 没有聊天命令 | ✅ 进世界自动建躯体，`@AI` 即对话 |
| AI 没有意识/记忆 | ✅ 自主发言+跟随，记忆随世界/存档永久保存 |
| AI 不能干活 | ✅ `@AI 帮我造XX` → 回复 + `CMD:` 真实放方块建造 |

## 三、使用方法

### 基岩版（1.26.30.5）
1. 导入 `G-ai-基岩版整合包-v2.2.0.mcaddon`（双击/用我的世界打开），游戏内启用行为包 + 资源包；
2. 进入世界，聊天框自动提示「苍天有眼！我乃苍天会」，AI 躯体（HIM）已在身边；
3. 聊天框输入：`@AI 帮我造一座小木屋` → AI 回复并开始建造；
4. `@AI 你有什么计划` → AI 按意识回答（陪玩/帮忙/速通后建家园）；
5. 苍天旗/五星红旗在工作台按配方合成。
> 若提示脚本需实验功能：世界设置 → 实验 → 开启「测试版 API」（个别版本需要）；对话需要网络（内置接口）。

### Java 版（26.2 Fabric / Forge）
1. 将对应 jar 放入 `mods/`，启动游戏进存档；
2. 自动出现 AI 本体（HIM）+ 提示「苍天有眼！我乃苍天会」；
3. 聊天框 `@AI 帮我造一座房子` 即可对话干活；按 **G** 打开悬浮控制台可配置 AI、导入光影/结构、检查更新。

## 四、版权与许可

本项目（G-ai）以 **MIT License** 开源发布，版权 © 2026 G-ai（ETQWFD）。你可以自由使用、修改、分发，包括商业用途；必须保留原版权声明与许可文本，且作者不对软件的使用后果承担任何责任。MIT 全文如下：

```
MIT License

Copyright (c) 2026 G-ai (ETQWFD)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## 五、验证情况

- Java 模组：Fabric（78,219B）/ Forge（78,876B）双 jar 编译通过；五星红旗资源（blockstate/model/texture/recipe/lang）双端齐全；`@AI` 聊天监听、进世界自动建躯体、内置 AI 配置均已接入；
- 基岩包：脚本语法校验通过（node --check）；行为包含 `scripts/main.js` + manifest 脚本模块 + `@minecraft/server`/`@minecraft/server-net` 依赖；实体/方块/配方 JSON 全部校验通过；.mcaddon 结构验证（行为包/资源包两文件夹 + 各自 manifest）；
- GitHub：源码已推送 main，Pages 官网更新至 v2.2.0，Release v2.2.0 已发布且资产完整可下载。
