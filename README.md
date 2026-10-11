# 星陨 Starfall

> Minecraft 实用客户端 —— 基于 **Meteor Client** 的功能与 **Material You** 界面，已完整汉化。

**作者：** cavanluo666
**仓库地址：** https://github.com/cavanluo666/Starfall
**GitHub：** https://github.com/cavanluo666
**Bilibili：** https://space.bilibili.com/3493095616284680
**个人网站：** https://246644.xyz

**许可证：** GNU General Public License v3.0

---

## 特性

| 模块 | 说明 |
|------|------|
| **Meteor 功能全量** | 188+ 模块：战斗、移动、渲染、世界、玩家、杂项 |
| **Material You 界面** | Skia GPU 渲染、动态取色、深浅主题、圆角动画 |
| **完整汉化** | 2749 条词条，模块名 / 描述 / 设置项全中文 |
| **内置 Baritone** | 寻路与自动挖掘，开箱即用 |
| **内置 CozyUI+** | 原版界面与高清中文字体美化 |
| **内置矿石透视** | Xray Ultimate 材质包，一键开关 |
| **自研扩展** | 24 个模块 + 6 个 HUD 元素 |

## 自研扩展功能

全部为**自行实现**（未复制第三方代码），保持纯 GPLv3：

- **战斗**：命中标记（含音效）、主手辅助、鞘翅辅助、摔落伤害预测、攻击距离显示
- **HUD**：按键显示、服务器信息（延迟/TPS/时钟）、物品使用状态、目标信息、灵动岛、方块数量统计
- **视觉**：低血量警告、动态模糊、攻击特效、伤害飘字、无潜行动画、方块描边、火球落点预测、1.7 动画、附魔光效控制
- **工具**：输入法修复、自动 GG、服务器自动登录、自动截图、自动存入箱子、胜利音效、平滑快捷栏、鼠标逻辑优化、计分板美化、物品栏选择器

## 使用方法

1. 安装 **Fabric Loader >= 0.15.11**（Minecraft **1.21.1**）
2. 将本模组与 **Fabric API** 放入 `.minecraft/mods/`
3. 启动游戏

| 操作 | 说明 |
|------|------|
| **Right Shift** | 打开 / 关闭主界面 |
| 左侧导航栏 | 首页 / 模块 / 设置 / 关于 |
| 分类标签 | 全部 / 战斗 / 玩家 / 移动 / 渲染 / 世界 / 杂项 |
| 点击模块卡片 | 切换开关；点上半部分进入设置 |
| 悬停模块 | 显示功能说明 |
| 导航栏铅笔图标 | 打开 HUD 编辑器 |

## 构建

需要 **JDK 21**：

```bash
./gradlew build -I init-mirrors.gradle -x javadoc -x javadocJar
# 产物: build/libs/starfall-*.jar
```

## 许可证

本项目以 **GNU General Public License v3.0** 发布，详见 [LICENSE](LICENSE)。

你可以自由使用、修改、分发本项目（包括商用），但**衍生作品必须同样以 GPLv3 开源**。

## 使用的开源项目

| 项目 | 许可证 | 用途 |
|------|--------|------|
| [Meteor Client](https://github.com/MeteorDevelopment/meteor-client) | GPL-3.0 | 客户端核心功能与框架 |
| [SoarClient](https://github.com/SoarClient/SoarClient) | MIT | Material You 界面与 Skia 渲染层 |
| [Baritone](https://github.com/cabaletta/baritone) | LGPL-3.0 | 寻路与自动挖掘 |
| [Skija](https://github.com/HumbleUI/Skija) | Apache-2.0 | 界面 GPU 渲染 |
| [Fabric](https://fabricmc.net/) | Apache-2.0 | 模组加载与 API |
| [Orbit](https://github.com/MeteorDevelopment/orbit) | MIT | 事件总线 |
| [Starscript](https://github.com/MeteorDevelopment/starscript) | MIT | HUD 表达式 |
| [Caffeine](https://github.com/ben-manes/caffeine) | Apache-2.0 | 缓存 |
| [fastutil](https://github.com/vigna/fastutil) | Apache-2.0 | 高性能集合 |
| [JOML](https://github.com/JOML-CI/JOML) | MIT | 数学库 |
| [Commons IO](https://commons.apache.org/proper/commons-io/) | Apache-2.0 | IO 工具 |
| [JNA](https://github.com/java-native-access/jna) | Apache-2.0 / LGPL-2.1+ | 输入法修复 |
| [LWJGL NFD](https://github.com/LWJGL/lwjgl3) | BSD-3-Clause | 原生文件选择框 |
| [CozyUI+](https://github.com/Fogg05/CozyUI) | 见包内 LICENSE | 原版界面与字体材质 |
| [Xray Ultimate](https://www.curseforge.com/minecraft/texture-packs/xray-ultimate-1-11-compatible) | 见包内 LICENSE | 矿石透视材质 |

完整许可证声明见 [NOTICE.md](NOTICE.md)。

## 免责声明

本项目仅供学习与研究使用。使用者需自行遵守所在服务器的规则，因使用本客户端产生的任何后果由使用者自行承担。
