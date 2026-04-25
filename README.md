# Config Assistant - IntelliJ IDEA 插件

> 一个用于 **查看、编辑、格式化、比对配置文件 YAML** 的 IntelliJ IDEA 插件，灵感来自 JSON Assistant。

---

## 📖 目录

- [项目简介](#项目简介)
- [功能规划](#功能规划)
- [技术选型](#技术选型)
- [从0到1开发流程](#从0到1开发流程)
- [架构设计](#架构设计)
- [核心逻辑说明](#核心逻辑说明)
- [开发环境搭建](#开发环境搭建)
- [快速开始](#快速开始)
- [发布流程](#发布流程)

---

## 项目简介

Config Assistant 是一个 IntelliJ IDEA 插件，提供以下能力：
- YAML 文件的 **结构化树形查看**
- YAML 文件的 **格式化与美化**
- 两个 YAML 文件的 **差异比对（Diff）**
- YAML ↔ JSON 的 **双向转换**
- YAML **语法校验** 与错误提示
- YAML **路径复制**（如 `spring.datasource.url`）

---

## 功能规划

### Phase 1 - MVP（最小可用版本）
| 功能 | 说明 |
|------|------|
| YAML 树形查看 | 以 Tree 结构展示 YAML 内容 |
| YAML 格式化 | 美化/压缩 YAML |
| 语法校验 | 实时检测语法错误 |

### Phase 2 - 核心功能
| 功能 | 说明 |
|------|------|
| YAML Diff 比对 | 两个 YAML 文件的结构化差异对比 |
| YAML ↔ JSON 转换 | 双向格式转换 |
| 路径复制 | 右键复制 YAML Key 路径 |

### Phase 3 - 增强体验
| 功能 | 说明 |
|------|------|
| YAML 搜索/过滤 | 按 key 或 value 搜索 |
| 多文档支持 | 支持 `---` 分隔的多文档 YAML |
| 主题适配 | 适配 IDEA 的亮色/暗色主题 |
| YAML Schema 校验 | 基于 JSON Schema 的校验 |

---

## 技术选型

### 核心技术栈

| 技术 | 用途 | 选型原因 |
|------|------|---------|
| **Java 17** | 主开发语言 | 你熟悉 Java 后端，学习成本最低 |
| **Gradle (Kotlin DSL)** | 构建工具 | IntelliJ 插件开发的官方推荐构建工具 |
| **IntelliJ Platform SDK** | 插件框架 | IDEA 插件必须基于此 SDK 开发 |
| **gradle-intellij-plugin** | Gradle 插件 | JetBrains 官方提供，简化插件构建/运行/发布 |
| **SnakeYAML** | YAML 解析 | Java 生态最成熟的 YAML 解析库 |
| **Swing** | UI 组件 | IDEA 插件的 UI 基于 Swing（不是 JavaFX） |

### 为什么不选其他方案？

| 方案 | 不选原因 |
|------|---------|
| Kotlin | 虽然 JetBrains 推荐，但你是 Java 背景，MVP 阶段用 Java 更快 |
| Maven | IntelliJ 插件生态对 Gradle 支持更好，官方模板都是 Gradle |
| Jackson YAML | 功能也可以，但 SnakeYAML 更轻量，社区示例更多 |
| JavaFX | IDEA 插件 UI 层基于 Swing，不支持 JavaFX |

---

## 从0到1开发流程

### 整体流程链路图

```
环境准备 → 项目脚手架 → 理解插件机制 → 开发核心功能 → 调试运行 → 打包发布
   ↓           ↓             ↓              ↓            ↓          ↓
安装IDEA   Gradle项目     plugin.xml      Action/UI    runIde    publishPlugin
JDK17     build.gradle   注册扩展点      Service层     沙箱IDEA   JetBrains
```

### 详细步骤

#### Step 1: 环境准备
```
1. 安装 IntelliJ IDEA Ultimate 或 Community（推荐 Ultimate）
2. 安装 JDK 17（IDEA 2023+ 需要 JDK 17）
3. 安装 Plugin DevKit 插件（IDEA 内置，确认已启用）
4. 安装 Gradle（或使用 Gradle Wrapper）
```

#### Step 2: 创建项目脚手架
```
方式1（推荐）: 使用 IntelliJ Platform Plugin Template
  → GitHub: https://github.com/JetBrains/intellij-platform-plugin-template
  → 点击 "Use this template" 创建你的仓库
  → Clone 到本地，用 IDEA 打开

方式2: 手动创建 Gradle 项目
  → 本仓库已提供完整脚手架，可直接使用
```

#### Step 3: 理解 IDEA 插件核心机制
```
IDEA 插件的核心概念：

1. plugin.xml — 插件的"清单文件"，声明插件 ID、名称、依赖、扩展点
2. Action — 用户触发的动作（菜单项、快捷键、工具栏按钮）
3. Tool Window — 侧边栏面板（类似 Project 面板、Terminal 面板）
4. Service — 单例服务，管理状态和业务逻辑
5. Editor — 编辑器相关扩展（高亮、补全、检查）
6. PSI (Program Structure Interface) — IDEA 的代码模型抽象
```

#### Step 4: 开发核心功能（按功能模块）
```
模块1: YAML 解析服务
  → 使用 SnakeYAML 解析 YAML 字符串为 Java 对象
  → 将解析结果转为树形数据模型

模块2: 树形查看 UI
  → 创建 Tool Window（侧边栏面板）
  → 使用 JTree 组件展示 YAML 结构
  → 监听编辑器内容变化，实时刷新

模块3: YAML Diff 比对
  → 获取两个 YAML 文件内容
  → 解析为标准化 Map 结构
  → 递归比较，生成差异报告
  → 使用 IDEA 内置 DiffManager 展示差异

模块4: 格式化
  → 使用 SnakeYAML 的 DumperOptions 控制输出格式
  → 注册为 Editor Action
```

#### Step 5: 调试运行
```
1. 执行 Gradle Task: runIde
   → 会启动一个沙箱 IDEA 实例，自动加载你的插件
2. 在沙箱 IDEA 中测试功能
3. 修改代码后重新 runIde 即可热加载
```

#### Step 6: 打包发布
```
1. 执行 Gradle Task: buildPlugin → 生成 .zip 安装包
2. 本地安装测试: Settings → Plugins → Install from Disk
3. 发布到 JetBrains Marketplace:
   → 注册 JetBrains 开发者账号
   → 执行 publishPlugin Task 或手动上传
```

---

## 架构设计

### 目录结构

```
config-assistant/
├── build.gradle.kts              # Gradle 构建配置
├── settings.gradle.kts           # Gradle 设置
├── gradle.properties             # 版本配置
├── src/
│   ├── main/
│   │   ├── java/com/muyan/yamlassistant/
│   │   │   ├── actions/          # Action 层 — 用户操作入口
│   │   │   │   ├── ViewYamlTreeAction.java      # 查看 YAML 树
│   │   │   │   ├── FormatYamlAction.java        # 格式化 YAML
│   │   │   │   ├── CompareYamlAction.java       # 比对 YAML
│   │   │   │   └── ConvertYamlJsonAction.java   # YAML↔JSON 转换
│   │   │   │
│   │   │   ├── editor/           # 编辑器扩展
│   │   │   │   └── YamlAnnotator.java           # 语法错误高亮
│   │   │   │
│   │   │   ├── diff/             # Diff 比对模块
│   │   │   │   ├── YamlDiffService.java         # 比对逻辑
│   │   │   │   └── YamlDiffResult.java          # 比对结果模型
│   │   │   │
│   │   │   ├── model/            # 数据模型
│   │   │   │   ├── YamlNode.java                # YAML 节点模型
│   │   │   │   └── YamlDocument.java            # YAML 文档模型
│   │   │   │
│   │   │   ├── services/         # 服务层 — 核心业务逻辑
│   │   │   │   ├── YamlParserService.java       # YAML 解析服务
│   │   │   │   ├── YamlFormatterService.java    # YAML 格式化服务
│   │   │   │   └── YamlConverterService.java    # 格式转换服务
│   │   │   │
│   │   │   ├── ui/               # UI 组件
│   │   │   │   ├── YamlToolWindowFactory.java   # Tool Window 工厂
│   │   │   │   ├── YamlTreePanel.java           # 树形展示面板
│   │   │   │   └── YamlDiffPanel.java           # 差异展示面板
│   │   │   │
│   │   │   ├── settings/         # 插件设置
│   │   │   │   ├── YamlAssistantSettings.java   # 设置状态
│   │   │   │   └── YamlAssistantConfigurable.java # 设置界面
│   │   │   │
│   │   │   └── util/             # 工具类
│   │   │       └── YamlPathUtil.java            # YAML 路径工具
│   │   │
│   │   └── resources/
│   │       └── META-INF/
│   │           └── plugin.xml     # 插件描述文件（最重要）
│   │
│   └── test/java/com/muyan/yamlassistant/
│       └── ...                    # 单元测试
│
├── docs/
│   ├── DEVELOPMENT_GUIDE.md       # 开发指南
│   └── AI_DEVELOPMENT_SPEC.md     # AI 可读的开发规范
│
└── .github/
    └── workflows/
        └── build.yml              # CI/CD
```

### 分层架构

```
┌─────────────────────────────────────────────┐
│              Action 层（用户入口）              │
│  ViewYamlTreeAction / FormatYamlAction / ... │
├─────────────────────────────────────────────┤
│              UI 层（展示）                     │
│  YamlToolWindow / YamlTreePanel / DiffPanel  │
├─────────────────────────────────────────────┤
│            Service 层（业务逻辑）              │
│  YamlParserService / FormatterService / ...  │
├─────────────────────────────────────────────┤
│            Model 层（数据模型）                │
│      YamlNode / YamlDocument / DiffResult    │
├─────────────────────────────────────────────┤
│           Util 层（工具/底层）                 │
│       SnakeYAML / IDEA Platform SDK          │
└─────────────────────────────────────────────┘
```

---

## 核心逻辑说明

### 1. YAML 解析流程

```
用户打开 YAML 文件
       ↓
编辑器内容变化监听 (DocumentListener)
       ↓
获取当前编辑器文本内容
       ↓
调用 YamlParserService.parse(yamlText)
       ↓
SnakeYAML 解析为 Map/List 结构
       ↓
转换为 YamlNode 树形模型
       ↓
通知 UI 刷新 (YamlTreePanel)
       ↓
JTree 展示树形结构
```

### 2. YAML Diff 比对流程

```
用户选择两个 YAML 文件 → Action 触发
       ↓
分别解析两个文件为 Map 结构
       ↓
YamlDiffService.compare(map1, map2)
       ↓
递归比较 Key-Value:
  - Key 只在左边 → REMOVED
  - Key 只在右边 → ADDED
  - Key 两边都有但值不同 → MODIFIED
  - Key 两边都有且值相同 → UNCHANGED
       ↓
生成 YamlDiffResult（差异列表）
       ↓
方式1: 使用 IDEA DiffManager 展示（推荐）
方式2: 自定义 YamlDiffPanel 展示
```

### 3. YAML 格式化流程

```
用户触发格式化 Action（菜单/快捷键）
       ↓
获取当前编辑器的 YAML 文本
       ↓
YamlFormatterService.format(yamlText, options)
       ↓
SnakeYAML 解析 → 设置 DumperOptions → 重新输出
  - 缩进: 2 spaces
  - 行宽: 120
  - 流式/块式风格选择
       ↓
通过 WriteCommandAction 替换编辑器内容
```

### 4. Tool Window 注册流程

```
plugin.xml 中声明:
  <toolWindow id="Config Assistant" ...
      factoryClass="com.muyan.yamlassistant.ui.YamlToolWindowFactory"/>
       ↓
IDEA 启动时读取 plugin.xml
       ↓
在侧边栏创建 Tool Window 入口
       ↓
用户点击时，调用 Factory.createToolWindowContent()
       ↓
初始化 YamlTreePanel，监听编辑器事件
```

---

## 开发环境搭建

### 前置条件
- IntelliJ IDEA 2023.3+（Community 或 Ultimate）
- JDK 17+
- Git

### 本地运行
```bash
# 1. 克隆项目
git clone https://github.com/muyanshouji/config-assistant.git
cd config-assistant

# 2. 用 IDEA 打开项目（File → Open → 选择项目根目录）

# 3. 等待 Gradle 同步完成

# 4. 运行插件（沙箱模式）
./gradlew runIde

# 5. 构建插件安装包
./gradlew buildPlugin
# 产物在 build/distributions/config-assistant-*.zip
```

---

## 快速开始

如果你是第一次开发 IDEA 插件，建议按以下顺序学习：

1. **先跑通 Hello World** — 创建一个简单 Action，弹出通知
2. **理解 plugin.xml** — 所有扩展点都在这里声明
3. **开发树形查看** — 学习 Tool Window + JTree
4. **添加格式化** — 学习 Editor Action + WriteCommandAction
5. **实现 Diff** — 学习 DiffManager API
6. **发布插件** — 学习 buildPlugin + publishPlugin

---

## 发布流程

```
1. 更新版本号 (gradle.properties)
2. 执行 ./gradlew buildPlugin
3. 本地测试安装包
4. 注册 JetBrains Hub 账号 (https://hub.jetbrains.com)
5. 获取 Marketplace Token
6. 配置 token 到环境变量或 gradle.properties
7. 执行 ./gradlew publishPlugin
8. 等待 JetBrains 审核（通常 1-3 个工作日）
```

---

## 参考资料

- [IntelliJ Platform Plugin SDK 官方文档](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- [SnakeYAML 文档](https://bitbucket.org/snakeyaml/snakeyaml/wiki/Documentation)
- [JSON Assistant 插件](https://plugins.jetbrains.com/plugin/21691-json-assistant) — 参考对象

---

## License

MIT License
