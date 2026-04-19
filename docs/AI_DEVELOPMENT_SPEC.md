# YAML Assistant - AI 可执行开发规范

> 本文档面向 AI 编程助手（如 GitHub Copilot、Cursor、ChatGPT 等），
> 提供结构化的、可直接执行的开发任务描述。

---

## 项目元信息

```yaml
project:
  name: yaml-assistant
  type: IntelliJ IDEA Plugin
  language: Java 17
  build_tool: Gradle (Kotlin DSL)
  yaml_parser: SnakeYAML 2.2
  json_parser: Gson 2.10.1
  idea_version: "2023.3+"
  package: com.muyan.yamlassistant
```

---

## 模块清单与状态

```yaml
modules:
  - name: model
    path: src/main/java/com/muyan/yamlassistant/model/
    status: DONE
    files:
      - YamlNode.java        # YAML 节点模型（key, value, type, children, path）
      - YamlDocument.java    # YAML 文档模型（支持多文档）

  - name: services
    path: src/main/java/com/muyan/yamlassistant/services/
    status: DONE
    files:
      - YamlParserService.java     # YAML 文本 → YamlNode 树
      - YamlFormatterService.java  # YAML 美化/压缩
      - YamlConverterService.java  # YAML ↔ JSON 转换

  - name: diff
    path: src/main/java/com/muyan/yamlassistant/diff/
    status: DONE
    files:
      - YamlDiffService.java   # 递归比对两个 YAML
      - YamlDiffResult.java    # 比对结果模型

  - name: actions
    path: src/main/java/com/muyan/yamlassistant/actions/
    status: DONE
    files:
      - ViewYamlTreeAction.java      # 打开树形视图
      - FormatYamlAction.java        # 格式化当前文件
      - CompareYamlAction.java       # 比对两个文件
      - ConvertYamlJsonAction.java   # YAML↔JSON 转换

  - name: ui
    path: src/main/java/com/muyan/yamlassistant/ui/
    status: DONE
    files:
      - YamlToolWindowFactory.java  # Tool Window 工厂
      - YamlTreePanel.java          # 树形展示面板
      - YamlDiffPanel.java          # 差异展示面板

  - name: editor
    path: src/main/java/com/muyan/yamlassistant/editor/
    status: DONE
    files:
      - YamlAnnotator.java  # 语法错误标注（ExternalAnnotator）

  - name: settings
    path: src/main/java/com/muyan/yamlassistant/settings/
    status: DONE
    files:
      - YamlAssistantSettings.java       # 持久化设置状态
      - YamlAssistantConfigurable.java   # 设置界面

  - name: util
    path: src/main/java/com/muyan/yamlassistant/util/
    status: DONE
    files:
      - YamlPathUtil.java  # 路径解析和查找工具
```

---

## 待开发任务队列

以下是 AI 助手可以直接执行的开发任务，按优先级排列：

### P0 - 必须完成（MVP）

```yaml
tasks:
  - id: T001
    title: 添加单元测试
    description: |
      为 YamlParserService、YamlFormatterService、YamlConverterService、
      YamlDiffService 添加 JUnit 单元测试。
      测试文件放在 src/test/java/com/muyan/yamlassistant/ 对应目录下。
    acceptance_criteria:
      - 每个 Service 至少 5 个测试用例
      - 覆盖正常场景和异常场景（空输入、非法 YAML、嵌套结构）
      - 所有测试通过

  - id: T002
    title: 错误通知机制
    description: |
      在 Action 中的 catch 块添加 IDEA 通知。
      使用 com.intellij.notification.NotificationGroupManager 显示错误。
      替换所有 TODO 注释处的错误处理。
    implementation_hint: |
      NotificationGroupManager.getInstance()
        .getNotificationGroup("YAML Assistant")
        .createNotification(message, NotificationType.ERROR)
        .notify(project);
      同时需要在 plugin.xml 中注册 notificationGroup。

  - id: T003
    title: YAML 路径右键复制
    description: |
      在 YamlTreePanel 的 JTree 上添加右键菜单，
      支持复制选中节点的完整 YAML 路径到剪贴板。
    implementation_hint: |
      tree.addMouseListener → 右键 → JPopupMenu → "Copy Path"
      使用 java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
```

### P1 - 重要增强

```yaml
tasks:
  - id: T004
    title: 搜索过滤功能
    description: |
      在 YamlTreePanel 顶部添加搜索框，
      支持按 key 或 value 过滤树形节点，
      匹配的节点高亮显示。
    ui_spec: |
      [搜索框 JTextField] [🔍 按钮]
      ─────────────────────────
      [过滤后的 JTree]

  - id: T005
    title: 多文档 YAML 支持优化
    description: |
      当 YAML 包含多个 --- 分隔的文档时，
      在 YamlTreePanel 中用 Tab 或 ComboBox 切换不同文档。

  - id: T006
    title: IDEA 主题适配
    description: |
      使用 IDEA 的 JBColor 替代硬编码颜色，
      确保 YamlDiffPanel 在亮色和暗色主题下都正常显示。
    implementation_hint: |
      import com.intellij.ui.JBColor;
      new JBColor(lightColor, darkColor);
```

### P2 - 锦上添花

```yaml
tasks:
  - id: T007
    title: YAML Schema 校验
    description: |
      支持用户指定 JSON Schema 文件，
      对 YAML 内容进行 Schema 校验。
    tech_hint: |
      可用库: everit-org/json-schema 或 networknt/json-schema-validator

  - id: T008
    title: YAML 注释保留
    description: |
      格式化时保留原始注释（SnakeYAML 默认会丢失注释）。
      需要自定义 SnakeYAML 的 Representer 或使用 snakeyaml-engine。

  - id: T009
    title: 拖拽比对
    description: |
      支持从 Project 面板拖拽两个 YAML 文件到插件面板进行比对。
```

---

## 关键 API 参考

### IDEA Platform SDK 常用 API

```java
// 获取当前项目
Project project = e.getProject();

// 获取当前编辑器
Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();

// 获取编辑器文本
String text = editor.getDocument().getText();

// 写入编辑器（必须在 WriteCommandAction 中）
WriteCommandAction.runWriteCommandAction(project, () -> {
    document.setText(newText);
});

// 显示 Diff
DiffManager.getInstance().showDiff(project, diffRequest);

// 文件选择器
VirtualFile file = FileChooser.chooseFile(descriptor, project, null);

// 显示通知
Notifications.Bus.notify(new Notification(groupId, title, content, type));

// 复制到剪贴板
CopyPasteManager.getInstance().setContents(new StringSelection(text));
```

### SnakeYAML 常用 API

```java
// 解析 YAML
Yaml yaml = new Yaml();
Object data = yaml.load(yamlText);           // 单文档
Iterable<Object> docs = yaml.loadAll(yamlText); // 多文档

// 输出 YAML
DumperOptions options = new DumperOptions();
options.setIndent(2);
options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
Yaml dumper = new Yaml(options);
String output = dumper.dump(data);
```

---

## 构建和测试命令

```bash
# 编译
./gradlew build

# 运行插件（沙箱 IDEA）
./gradlew runIde

# 运行测试
./gradlew test

# 构建安装包
./gradlew buildPlugin

# 发布到 JetBrains Marketplace
./gradlew publishPlugin
```

---

## 代码规范

```yaml
conventions:
  - 包名: com.muyan.yamlassistant.*
  - 类命名: PascalCase，后缀表明类型（*Service, *Action, *Panel, *Factory）
  - 方法命名: camelCase
  - 注释: 每个类必须有 Javadoc 说明类的职责和核心逻辑
  - 编码: UTF-8
  - 缩进: 4 spaces
  - 行宽: 120 字符
  - 异常处理: catch 后必须有用户可见的错误提示（通知或日志）
  - IDEA API: 写操作必须包在 WriteCommandAction 中
  - UI 更新: 必须在 EDT（Event Dispatch Thread）中执行
```
