# 向下移植指南（Meteor + Soar 合并版）

> 目标：把这套 Material You 界面移植到 Meteor 支持的其它 MC 版本 / 加载器时，
> **只改极少数文件**，而不是翻遍全工程。

---

## 一、为什么这个架构好移植

实测统计（`com.soarclient` 下 142 个文件）：

| 类别 | 文件数 | 移植时是否需要动 |
|------|--------|------------------|
| 纯逻辑（颜色算法、动画、布局、Material3） | **110** | **完全不用动** |
| 直接碰 MC / GL API | 32 | 需要动 |
| 其中：自制 shader 子系统（已停用） | 5 | 不用动 |
| **实际需要适配的** | **约 27** | 见下 |

结论：**约 78% 的代码是版本无关的**。Material 3 配色、Skia 绘制封装、动画曲线、
页面/组件逻辑全都不依赖具体 MC 版本。

---

## 二、移植时改哪里

### 1. 首选：`com/soarclient/compat/McCompat.java`（唯一必改文件）

所有易变的窗口 / 尺寸 / 鼠标 API 都收敛到这里：

| 方法 | 作用 | 各版本差异点 |
|------|------|--------------|
| `framebufferScale()` | 帧缓冲像素 → GUI 缩放坐标 | 恒定 framebufferWidth/scaledWidth，一般不随版本变 |
| `framebufferWidth/Height()` | Skia 渲染面尺寸 | 1.20.x 起叫法一致 |
| `scaledWidth/Height()` | 布局空间尺寸 | 一致 |
| `mouseX()/mouseY()` | 鼠标的缩放坐标 | **最容易出问题**：`Mouse.getX()` 各版本返回的坐标系可能不同 |
| `windowReady()` | 空窗口保护 | — |

**移植步骤**：把本文件里用到的 MC 方法名按目标版本调整即可。
若目标版本 `Mouse` 已经返回缩放坐标，直接把 `mouseX()` 改成 `mc().mouse.getX()`。

### 2. 其次：`mixin/widget/` 下的两个 mixin

| 文件 | 目标 | 各版本差异 |
|------|------|------------|
| `PressableWidgetMixin` | 原版按钮 → 毛玻璃 | `renderWidget` 签名在 1.20.x 与 1.21.x 一致；若目标版本用 `renderButton`（1.19 及更早）需改注入点 |
| `ScreenGlassMixin` | 原版界面背景 → 毛玻璃 | `renderBackground(DrawContext,int,int,float)` 在 1.20.x 起才有；更早版本背景是 `renderBackground(MatrixStack)` |

> 这两个 mixin 都带 `GlassRenderer.enabled()` 开关，出问题可在设置里一键关掉，
> 不会导致游戏无法启动。

### 3. 极少改：`SkiaContext`

`SkiaContext.draw()` 里有一批 `RenderSystem.*` / `GL11.*` / `GL33.*` 状态复位调用，
用于把 MC 的渲染状态还原干净。**1.20 / 1.21 / 1.21.x 上这些调用基本一致**。
若移植到 1.21.5+（渲染管线重构）或 1.20 以下，这里是重点。

---

## 三、已经做过的兼容性设计

1. **坐标体系统一**：绘制、布局、鼠标三者统一在「GUI 缩放坐标」空间，
   换算比例只有一个来源 `McCompat.framebufferScale()`。
   早期版本混用三套坐标（帧缓冲 / 窗口 / 缩放），是按钮点不动、缩放窗口错位的根因。

2. **不再自建 GL 后处理**：原本 Soar 自带一套 Kawase 模糊（裸 GL 调用），
   会绕过 `RenderSystem` 弄脏状态，导致 MC 后续绘制的文字错位。
   现已全部停用，改用 **MC 原生模糊**
   （`Screen.renderBackground` → `applyBlur` → `GameRenderer.renderBlur`），
   该管线跨版本稳定且自带状态保存/恢复。

3. **UI 与数据解耦**：界面直接读 Meteor 的 `Module` / `Setting`，
   自身不持有模块模型。Meteor 换了版本，界面层不用跟着改。

4. **内置 Baritone 的版本约束**：`baritone-1.21-SNAPSHOT.jar` 的
   `fabric.mod.json` 写死 `"minecraft": "1.21"`，在 1.21.1 上会被 Fabric
   **静默跳过**（表现为 `Path Manager: none`）。
   解决办法：`libs/baritone-1.21-patched.jar`（约束放宽为 `>=1.21 <1.22`）
   放进 `local-maven/`，用坐标依赖引入。
   **移植到别的 MC 版本时，这个 jar 必须换成对应版本的 Baritone 并同样放宽约束。**

---

## 四、移植到新版本的清单

```
[ ] 1. 复制整个工程目录（各版本工程互相独立，不共享 sourceSet）
[ ] 2. gradle.properties 改 minecraft_version / yarn_mappings / loader_version / fapi_version
[ ] 3. 依赖版本对齐：sodium / lithium / iris（不存在的版本号会直接构建失败）
[ ] 4. 换对应版本的 Baritone，放宽 minecraft 约束，放进 local-maven/
[ ] 5. 改 McCompat.java 里不再匹配的 MC 方法
[ ] 6. 按需调整 mixin/widget/ 的注入点
[ ] 7. 编译 → 真机启动 → 看日志确认：
        - "Initializing Meteor + Soar 客户端" 出现
        - "Path Manager: Baritone"（不是 none）
        - 无 ERROR
[ ] 8. 进游戏验证：Right Shift 开界面、点开关、缩放窗口、切浅色主题
```

---

## 五、已知版本差异坑

| 版本 | 坑 |
|------|-----|
| 1.20.5 / 1.21.2 | **没有官方 Forge**，只有 NeoForge + Fabric |
| 1.21.5+ | 渲染管线重构（`RenderLayer` / `GpuDevice`），`SkiaContext` 与 mixin 都要重写 |
| 1.20.x | `Screen.renderBackground` 签名与 1.21 不同；`DrawContext` 已有但背景方法不同 |
| ≤1.19 | 没有 `DrawContext`，整体 UI 层需要重写（不建议走这条线） |
| 所有版本 | Baritone 的 `minecraft` 版本约束都要放宽，否则被静默跳过 |
