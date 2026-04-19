# YAML Assistant 开发指南

## 面向 Java 后端开发者的 IDEA 插件开发入门

如果你是 Java 后端开发者，以下是你需要理解的关键差异：

### 与后端开发的区别

| 后端开发 | IDEA 插件开发 |
|---------|-------------|
| Spring Boot 框架 | IntelliJ Platform SDK |
| REST Controller | Action（菜单/快捷键入口） |
| Service Bean | ApplicationService / ProjectService |
| HTML/Vue 页面 | Swing UI 组件 |
| application.yml 配置 | plugin.xml 配置 |
| Spring IoC 注入 | ServiceManager / @Service 注解 |
| Tomcat 运行 | runIde 沙箱运行 |
| jar/war 部署 | buildPlugin → .zip 发布 |

### plugin.xml 是核心

类似 Spring Boot 的 `application.yml`，`plugin.xml` 是插件的核心配置文件：

```xml
<!-- 声明一个 Action（相当于 Controller 接口） -->
<action id="MyAction" class="com.example.MyAction" text="My Action"/>

<!-- 声明一个 Tool Window（相当于一个页面） -->
<toolWindow id="MyTool" factoryClass="com.example.MyToolFactory"/>

<!-- 声明一个 Service（相当于 Spring Bean） -->
<applicationService serviceImplementation="com.example.MyService"/>
```

### 线程模型

IDEA 有严格的线程模型：
- **EDT（Event Dispatch Thread）**: UI 操作必须在此线程
- **Read Action**: 读取 PSI/文档需要在 Read Action 中
- **Write Action**: 修改文档/PSI 需要在 Write Action 中（WriteCommandAction）
- **Background Thread**: 耗时操作放在后台线程（Task.Backgroundable）

### 调试技巧

1. **runIde** 会启动一个全新的 IDEA 实例，你的插件自动加载在里面
2. 可以在代码中打断点，用 Debug 模式运行 runIde
3. 沙箱 IDEA 的配置独立于你的开发 IDEA
4. 修改代码后需要重启 runIde（热重载支持有限）

---

## 推荐学习路径

1. 阅读 [IntelliJ Platform SDK 官方文档](https://plugins.jetbrains.com/docs/intellij/welcome.html)
2. 参考 [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
3. 查看本项目代码，从 `ViewYamlTreeAction.java` 开始阅读
4. 运行 `./gradlew runIde` 体验效果
5. 逐步修改和扩展功能
