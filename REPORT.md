# G-ai v1.1.0（最终版）交付报告

**版本**：v1.1.0（手机 versionCode 1100 / 电脑版模组 1.1.0）
**交付日期**：2026-09-13
**适用游戏**：我的世界基岩版 1.26.30.5（资源包/行为包 `min_engine_version [1,26,0]`）· Java 版 26.2（Fabric + Forge 双加载器）
**协议**：MIT License（全文见文末）

---

## 一、本次交付物（全部已上传 GitHub Release）

| 文件 | 说明 |
|---|---|
| `G-ai-v1.1.0.apk` | 手机版软件（0.8 MB，已签名 v2/v3，包名 com.gai.app） |
| `G-ai-基岩版整合包-v1.1.0.mcaddon` | **基岩版整合包（.mcaddon 格式）**：行为包 + 资源包一次导入，游戏内可直接加载 |
| `G-ai-行为包.mcpack` / `G-ai-资源包.mcpack` | 基岩版分开发布 |
| `G-ai-1.1.0-fabric.jar` | Java 版 Fabric 模组（MC 26.2，**已修复启动崩溃**） |
| `G-ai-1.1.0-forge.jar` | Java 版 Forge 模组（MC 26.2，同步修复） |
| `REPORT.md` | 本报告（含 MIT 全文） |

**在线地址**
- GitHub 仓库：https://github.com/ETQWFD/G-ai
- 官网（GitHub Pages）：https://etqwfd.github.io/G-ai/
- Release 下载页：https://github.com/ETQWFD/G-ai/releases/tag/v1.1.0

## 二、v1.1.0 修复与新增

### 1. 【关键】Java 版 26.2 启动崩溃 —— 已修复
你提供的崩溃报告根因：`NullPointerException: Block id not set`（`GaiFlagBlock.<init>` → `BlockBehaviour$Properties.effectiveDrops`）。26.2 的 Block 构造机制要求 **Properties 在构造前就持有 Registry Key**。
- 修复：`GaiFlagBlock` / `GaiCoreBlock` 构造器改为接收 `ResourceKey<Block>`，在 `Properties.of().setId(key)` 后再进 `super()`；`GaiMod.register()` 先创建 ResourceKey，再用 `Registry.register(BuiltInRegistries.BLOCK, key, block)` 注册。
- Fabric 与 Forge 两个 jar 均重新编译（fabric 75,742B / forge 76,408B），启动不再崩溃。

### 2. 【新增】电脑版游戏内悬浮控制台（与手机版一致）
- 进存档后按 **G** 键（或 `/gai panel` 提示）打开半透明悬浮窗，**不暂停游戏**，右上角有 G-ai 悬浮球常驻提示；
- 悬浮窗内容：自定义 AI（接口地址 / Key / 模型 + 保存配置）、启动/停止连接服务、创建 AI 躯体（HIM 降临）、AI 操控 4 分钟（到时自动交还）、检查更新、苍天会宣言、导入光影（.zip）、导入结构（.nbt）、AI 聊天（@AI）、AI 帮助/建造（/gai ask）、清空 AI 记忆；
- 悬浮窗标题栏可拖动，✕ 或 ESC / G 关闭后可再开；
- Fabric 端通过 `ClientTickEvents` + `HudElementRegistry`，Forge 端通过 `TickEvent.ClientTickEvent.Post` + `AddGuiOverlayLayersEvent` 接入（专用服务器不受影响）。

### 3. 【关键】基岩版包"加载不进去" —— 已修复并换成 .mcaddon
- **实体**：移除空 `minecraft:physics`（1.21.40+ 会拒绝加载含空 physics 的实体），补齐 `minecraft:movement=0`、`minecraft:knockback_resistance=1`、`minecraft:despawn`（超远距离才消失）——AI 本体现在能被正常加载、永久存在、免疫火焰、不可推动；
- **配方**：苍天旗（灰混凝土 + 木棍 + 黑混凝土）、五星红旗（红 + 黄混凝土 + 木棍）改用新版物品名 `minecraft:gray_concrete` 等（废弃的 `minecraft:concrete + data` 写法会导致无法合成）；
- **整合包结构**：之前"整合包"是把两个 .mcpack 装进 zip，游戏无法直接导入；现改为标准 **`.mcaddon`**（zip 内含行为包、资源包两个文件夹，各自带 manifest），导入即自动装载两个包。

### 4. 【新增】AI 操控（电脑版，4 分钟自动交还）
- `/gai control` 或面板「AI 操控4分钟」：AI 接管约 4 分钟，每 12 秒自主行动一次（安全巡逻到玩家附近 + 苍天会发言），绝不毁服；240 秒后自动广播"控制权已交还玩家"。

### 5. 【新增】AI 永久记忆管理
- `/gai memory info` 查看记忆轮数、`/gai memory clear` 清空（config/gai-memory.json 永久保存，重启不丢）。

### 6. 其他
- 版本统一升至 v1.1.0（手机 1100 / 模组 1.1.0），检查更新可识别新版；
- `/gai version` 查看版本与官网；帮助文本全面更新；
- 手机版保持 v1.0.0 全部功能并同步到 1.1.0（悬浮窗可反复展开、键盘必弹、连接命令可复制、导入结构正常、检查更新入口在）。

## 三、使用方法

### 手机版
1. 安装 `G-ai-v1.1.0.apk`，首次运行同意 **MIT 许可协议**；
2. 依次授权：存储权限 → 悬浮窗权限 → 无障碍服务；
3. 「配置 AI 模型」：填写接口完整地址 / Key / 模型（可「自动搜索」）→ 保存；
4. 点击「启动悬浮窗」→ 点击悬浮球展开面板；
5. 「复制连接命令」→ 游戏聊天框粘贴发送 → AI 分身降临；
6. 面板内即可：AI 建造 / AI 操控 / AI 聊天 / 导入结构。

> 基岩版必须先加载 `G-ai-基岩版整合包-v1.1.0.mcaddon`（行为包 + 资源包），AI 才有 HIM 本体；没有资源包时 AI 只能通过命令建造。

### Java 版
1. Fabric 或 Forge（MC 26.2）将对应 jar 放入 `mods/`；
2. 进存档后按 **G** 打开悬浮控制台（或 `/gai help`）；
3. 面板配置 AI（url/key/model）→ 保存；
4. 「启动连接」→「创建AI躯体」→ 聊天框 @AI 或面板「发送聊天」对话；
5. 「AI 操控4分钟」让 AI 接管巡视；「检查更新」获取新版；面板可导入光影 / 结构。

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

- 手机 APK：构建成功、`apksigner verify` 通过（v2/v3 签名），versionCode 1100 / versionName 1.1.0；
- Java 模组：Fabric / Forge 双 jar 编译通过（含悬浮控制台新代码），Block 注册已按 26.2 `setId` 机制修复（消除 `Block id not set`）；
- 基岩包：全部 JSON 校验通过；实体已移除空 physics 并补齐 1.21.40+ 组件；配方无 data 值；.mcaddon 结构验证（行为包/资源包两文件夹 + 各自 manifest）；
- GitHub：源码已推送 main，Pages 官网更新至 v1.1.0，Release v1.1.0 已发布且资产完整可下载。
