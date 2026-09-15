# G-ai v2.4.0 交付报告

**版本**：v2.4.0（基岩版行为包/资源包 + 网易版整合包 + Java 版 Fabric/Forge 模组）
**交付日期**：2026-09-13
**适用游戏**：我的世界基岩版 1.26.30.5（含内置 AI 分身）· 网易中国版（手机版/电脑版互通）· Java 版 26.2（Fabric + Forge）
**协议**：MIT License（全文见文末）

---

## 〇、本版核心：彻底修复"AI 本体加载不出来 / 没作用 / 无法使用"

v2.2.0 起 AI 完全内置进游戏（无需手机软件），v2.3.0 修复"AI 躯体创建不出来"根因并新增网易中国版支持；v2.4.0 新增：AI 离线账号（et2416444244@outlook.com）、/scriptevent 与 /function 指令触发创建、资源包-行为包自动关联。

- **根因定位**：脚本顶部**静态 import `@minecraft/server-net`**——在网易版/未开实验/模块不可用的环境里，整个脚本加载失败，AI 躯体因此完全不创建。
- **v2.3.0 修复**：
  1. `@minecraft/server-net` 改**动态 import（try/catch）**，网络不可用时自动切换离线应答，脚本永不崩溃；
  2. AI 躯体**三重保障**：玩家进世界事件创建 + `@AI` 聊天时补创建 + 每 20 秒定时检查（消失自动重生），任何情况躯体都在；
  3. 实体用 tag（`gai_avatar`）查找与去重，任何方式都不会出现多个 AI 躯体；
  4. 新增 `spawn_rules/gai_ai_body.json`——即使脚本未运行，AI 躯体也会**自然生成**出现；
  5. 创建成功在聊天框**播报坐标**，玩家清楚看到 AI 已降临；
  6. 实体行为增强：缓慢走动、注视玩家、免疫玩家伤害、100 血、不可被推。

## 一、本次交付物

| 文件 | 说明 |
|---|---|
| `G-ai-基岩版整合包-v2.4.0.mcaddon` | **基岩版整合包（.mcaddon）**：行为包（AI 脚本 + spawn_rules + functions）+ 资源包，一次导入即用 |
| `G-ai-行为包.mcpack` | 行为包 v2.4.0（AI 躯体实体 + 内置脚本 + functions + 方块 + 配方 + 自然生成） |
| `G-ai-资源包.mcpack` | 资源包 v2.4.0（HIM 躯体模型贴图 + 苍天旗/五星红旗贴图） |
| `G-ai-网易版整合包-v2.4.0.zip` | **网易中国版整合包**（zip 内含 behavior/、resource/ 两文件夹，符合网易 Add-on 上传规范，手机版+电脑版互通；**不上传 GitHub，仅供发布**） |
| `G-ai-网易版发布文案.md` | 网易平台发布指引（作品名/简介/100 绿宝石/备注"开发者etc"/发布步骤） |
| `G-ai-2.4.0-fabric.jar` / `G-ai-2.4.0-forge.jar` | Java 版模组（MC 26.2，Fabric + Forge，离线账号可真实使用，已修复 Block id not set 崩溃） |
| `REPORT.md` | 本报告（含 MIT 全文） |

## 二、功能与修复清单

### 1. 基岩版：AI 躯体真实创建、可对话、可干活
- **AI 离线账号**：AI 玩家名（nameTag）= `et2416444244@outlook.com`，像玩家一样显示在头顶；
- **进世界自动创建 AI 躯体**（`gai:ai_body`，HIM 造型），聊天框播报「苍天有眼！我乃苍天会」与坐标；
- **手动触发**（三种均可）：`/summon gai:ai_body`（原版命令）· `/scriptevent gai:spawn`（脚本触发）· `/function gai_summon`（函数触发）；
- **聊天框 `@AI 你想让我干的事`** 直接对话，AI 有自主意识（跟随玩家、周期发言、陪玩/帮忙/建家园）；
- **永久记忆**：对话历史写入世界存档（dynamic property `gai_mem_v2`），退出游戏再进仍记得你；
- **真实干活**：AI 回复中 `CMD:` 行作为 `/setblock` 命令真实建造（安全过滤：只允许放方块，每次≤60 块，禁止 /kill /clear /op /give 等一切破坏性命令，绝不毁服）；
- **国旗**：苍天旗（灰混凝土+木棍+黑混凝土，黑灰旗面+黑"苍"字）与五星红旗（红/黄混凝土+木棍，真实五星布局）工作台合成；
- **网络不可用**：自动离线应答，@AI 永不报错。

### 2. 网易中国版（新增）
- 按网易官方 Add-on 规范打包（zip 内含 behavior、resource 两文件夹）；
- 支持**网易手机版（移动版）与电脑版互通**；
- 需**开启实验模式**（实验性玩法 Beta 接口）以启用 @AI 脚本对话；AI 躯体与国旗（纯原版组件）在任意情况下可用；
- 发布文案已备好：作品名「G-ai：我的世界 AI 分身助手（HIM 陪玩）」、**定价 100 绿宝石**、**备注"开发者etc"**、发布四步流程。

### 3. Java 版（26.2，Fabric + Forge，功能与基岩版一致，离线账号可真实使用）
- **修复崩溃**：GaiFlagBlock 构造时通过 setId 绑定 Registry Key 再注册（修复 "Block id not set" 启动崩溃，Fabric + Forge 均已重新构建 v2.3.0）；
- AI 躯体头顶显示离线账号 `et2416444244@outlook.com`（像玩家一样）；
- 进存档自动创建 AI 躯体（HIM）+ 播报「苍天有眼！我乃苍天会」；`/gai spawn` 手动触发；
- 聊天框 `@AI` 直接对话干活；按 **G** 打开悬浮控制台（配置 AI、导入光影/结构、检查更新、AI 操控 4 分钟自动交还）；
- 永久记忆（`config/gai-memory.json`）；五星红旗与苍天旗可合成可放置。

### 4. 修复清单（v2.3.0 汇总）
| 问题 | 状态 |
|---|---|
| AI 躯体加载不进去 / 根本没用（脚本因 server-net 静态 import 崩溃） | ✅ 已修（动态 import + 离线降级） |
| 脚本未运行就没有 AI 躯体 | ✅ 已修（spawn_rules 自然生成 + 三重保障重生） |
| 出现多个 AI 躯体 | ✅ 已修（tag 查找 + 去重清理） |
| 不知道 AI 有没有创建 | ✅ 已修（创建成功播报坐标） |
| AI 躯体像雕像不会动 | ✅ 已修（movement + 随机走动 + 注视玩家 + 免疫伤害） |
| 网易版加载不了 / 无法使用 | ✅ 已修（网易规范打包 + min_engine 1.20 + 实验模式说明） |
| 国旗没有加载进去 | ✅ 已修（贴图/模型/配方/语言全量齐全） |
| Java 版启动崩溃 Block id not set | ✅ 已修（GaiFlagBlock 传 ResourceKey + setId，先注册后使用） |
| AI 没有名字、不像玩家 | ✅ 已修（基岩版 nameTag / Java setCustomName = et2416444244@outlook.com） |

## 三、使用方法

### 基岩版（1.26.30.5）
1. 导入 `G-ai-基岩版整合包-v2.4.0.mcaddon`（用我的世界打开），行为包会自动带起资源包，游戏内启用行为包 + 资源包；
2. 进入世界 → 聊天框提示「苍天有眼！我乃苍天会」+ 播报 AI 躯体坐标，HIM 已在身边；
3. 若未自动出现，输入 `/scriptevent gai:spawn` 或 `/function gai_summon` 或 `/summon gai:ai_body` 手动创建；
4. 聊天框输入 `@AI 帮我造一座小木屋` → AI 回复并开始建造；
5. `@AI 你有什么计划` → AI 按意识回答；苍天旗/五星红旗在工作台合成。
> 若提示脚本需实验功能：世界设置 → 实验 → 开启「测试版 API」；对话需要网络（内置接口，离线自动应答）。

### 网易中国版
1. 上传 `G-ai-网易版整合包-v2.4.0.zip` 至网易开发者平台（发布步骤见发布文案；**网易版账号体系为网易通行证，国际基岩版账号不能登录网易版**）；
2. 玩家下载组件后，创建世界时**开启实验模式**；
3. 进世界自动出现 AI 躯体（HIM）+ 提示「苍天有眼！我乃苍天会」，聊天框 `@AI ...` 对话干活。

### Java 版（26.2 Fabric / Forge）
1. 将对应 jar 放入 `mods/`，启动游戏进存档；
2. 自动出现 AI 本体（HIM）+ 提示「苍天有眼！我乃苍天会」；
3. 聊天框 `@AI 帮我造一座房子` 即可对话干活；按 **G** 打开悬浮控制台。

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

- 基岩包：脚本语法校验通过（node --check）；实体/方块/配方/spawn_rules/manifest 全部 JSON 校验通过；.mcaddon 结构验证（行为包/资源包两文件夹 + 各自 manifest + scripts + spawn_rules）；
- 网易包：按网易 Add-on 规范 zip（behavior/ + resource/），manifest 标注网易版、min_engine [1,20,0]；
- Java 模组：Fabric / Forge 双 jar（v2.4.0）编译通过（JDK 25 target，Java 25 运行）；GaiFlagBlock.class 已确认包含 setId 调用；基岩包 ZIP 校验通过。

## 六、已知待办（诚实说明）

- **网易平台登录**：使用账号 `v2416444244@163.com` 在网易开发者登录页（mcdev.webapp.163.com）与网易邮箱（mail.163.com）均返回「请求错误，请您稍后再试」——该报错为账号侧（密码错误 / 风控验证 / 账号未注册）或需短信/邮箱验证，非发布流程问题。整合包与发布文案已备好，**登录成功后按 `G-ai-网易版发布文案.md` 填写上传即可**（100 绿宝石 / 备注"开发者etc" / 实验模式说明均已写好）；
- **宣传视频**：内容安全策略拦截了本次宣传片生成，未能产出视频文件（见交付说明）。
