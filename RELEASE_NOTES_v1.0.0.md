# 星陨 Starfall 1.0.0

Minecraft 实用客户端 —— 基于 **Meteor Client** 的功能与 **Material You** 界面，已完整汉化。

> 本发行版为 **Fabric / Minecraft 1.21.1** 版本。
> 其它 MC 版本与加载器（Forge / NeoForge / 更高 MC 版本）的兼容性**尚未实现**，后续版本处理。

---

## 运行环境（重要）

| 项目 | 要求 |
|------|------|
| **加载器** | **Fabric Loader >= 0.15.11** |
| **Minecraft** | **1.21.1**（fabric.mod.json 声明 ["1.21","1.21.1"]，仅在 1.21.1 上验证） |
| **Java** | **21 或更高**（声明 >=21，低于 21 会直接拒绝加载） |
| **Fabric API** | **不需要**。所需的 fabric-resource-loader-v0 已作为模块内置 |

### 安装

1. 安装 **Fabric Loader 0.15.11+**，选择 **Minecraft 1.21.1**
2. 把本 jar 放进 .minecraft/mods/
3. 启动游戏（Baritone 与界面渲染所需的原生库都已内置，无需另装）

### 互斥 / 不兼容

以下模组与本客户端冲突，**不要同时安装**（已在 breaks 中声明）：

optifabric · feather · origins · wurst · morechathistory · sodium < 0.6.0-beta.2

---

## 内置内容

| 组件 | 说明 |
|------|------|
| **Baritone** | 寻路与自动挖掘，已内置（约束已放宽，1.21.1 上可正常加载） |
| **CozyUI+ v1.11** | 原版界面与高清中文字体材质 |
| **Xray Ultimate v5.4.3** | 矿石透视材质，由「透视」模块控制开关 |
| **Skija / Skia** | Material You 界面的 GPU 渲染层，含 Windows / Linux / macOS 原生库 |

---

## 功能

- **Meteor 全量功能**：188+ 模块，覆盖战斗、玩家、移动、渲染、世界、杂项
- **Material You 界面**：Skia GPU 渲染、动态取色、深浅主题、圆角动画
- **完整汉化**：2749 条词条，模块名 / 描述 / 设置项全中文
- **自研扩展 24 个模块 + 6 个 HUD 元素**（自行实现，未复制第三方代码）：
  - 战斗：命中标记（含音效）、主手辅助、鞘翅辅助、摔落伤害预测、攻击距离显示
  - HUD：按键显示、服务器信息（延迟/TPS/时钟）、物品使用状态、目标信息、灵动岛、方块数量统计
  - 视觉：低血量警告、动态模糊、攻击特效、伤害飘字、无潜行动画、方块描边、火球落点预测、1.7 动画、附魔光效控制
  - 工具：输入法修复、自动 GG、服务器自动登录、自动截图、自动存入箱子、胜利音效、平滑快捷栏、鼠标逻辑优化、计分板美化、物品栏选择器

### 操作

| 按键 | 说明 |
|------|------|
| **右 Shift** | 打开 / 关闭主界面 |
| 左侧导航栏 | 首页 / 模块 / 设置 / 关于 |
| 点击模块卡片 | 切换开关；点上半部分进入设置 |
| 导航栏铅笔图标 | 打开 HUD 编辑器 |

---

## 本版修复

- **内置材质包不生效**：资源包的 profile 名被写成 meteor-client/xray（斜杠），而 Fabric 注册的真实名字是 meteor-client:xray（冒号，来自 Identifier.toString()）。后果是 hasProfile() 恒为 false，options.txt 里那条记录每次启动都被 "Removed resource pack ... because it doesnt seem to exist anymore" 清掉，两个内置包从未参与加载 —— 表现为「界面显示已启用、材质没生效」。现已修正，并在启动日志打印两个包的真实加载状态以便自查。
- **滚动条被分类栏遮挡**：顶栏几何有三套互相矛盾的硬编码（滚动条轨道 y+92、contentTop() 写死 88、顶部底卡写死 56f+分类栏高度+14f）。分类标签换行成两行后，轨道与分类栏第二行完全重叠。现已把顶栏几何收敛为单一来源，搜索栏与分类栏上移，滚动条落在分类栏下方，内容可视区也一并变大。

---

## 许可证

本项目以 **GNU General Public License v3.0** 发布，详见仓库根目录 LICENSE。

可自由使用、修改、分发（含商用），但**衍生作品必须同样以 GPLv3 开源**。

### 第三方组件

| 组件 | 许可证 |
|------|--------|
| Meteor Client | GPL-3.0 |
| SoarClient | MIT |
| Baritone | LGPL-3.0 |
| Skija | Apache-2.0 |
| Fabric Loader / API | Apache-2.0 |
| Orbit / Starscript | MIT |
| Caffeine / fastutil / Commons IO | Apache-2.0 |
| JOML | MIT |
| JNA | Apache-2.0 / LGPL-2.1+ |
| LWJGL NFD | BSD-3-Clause |
| CozyUI+ | 见包内 LICENSE |
| Xray Ultimate | 见包内 LICENSE |

---

## 免责声明

本项目仅供学习与研究使用。使用者需自行遵守所在服务器的规则，因使用本客户端产生的任何后果由使用者自行承担。