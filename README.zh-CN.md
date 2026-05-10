[English](./README.md) | [简体中文](./README.zh-CN.md)

# Config Assistant

一个直接工作在 IntelliJ IDEA 里的 YAML 配置工作区，用来更快地查看、整理、对比和转换配置内容。

Config Assistant 的核心目标很简单：当你在排查服务迁移问题、核对不同环境配置、或者检查线上问题时，不需要再在本地项目里临时新建 YAML 文件、粘贴内容、对比完再删除。

它提供了一个独立的 config workspace，让你在 IDEA 里直接保存多个临时视图，随时格式化、对比，并在项目级别持久化这些内容。

## 为什么做这个插件

很多配置排查场景都很重复：

- 需要对比测试环境和生产环境的 YAML
- 需要比对服务迁移前后的配置差异
- 需要先把别人发来的 YAML 粘进本地临时文件里再看
- 对比完成后还要把这些临时文件删掉

Config Assistant 就是为了解决这类低效操作。

你可以直接在工具窗口里创建多个 `View`，把不同来源的 YAML 粘进去，用原生 IntelliJ Diff 查看差异，不污染项目目录，也不需要维护临时文件。

典型使用场景：

- 排查测试环境和生产环境配置差异
- 服务迁移时对比新旧配置
- 调整配置时保留 before / after 快照
- 快速整理粘贴进来的 YAML
- 直接保存临时配置视图，不需要在项目里新建一次性文件

## 核心能力

- IntelliJ IDEA 内置的多视图 YAML 工作区
- 项目级持久化视图，重启 IDE 后仍可恢复
- 使用 IntelliJ 原生 Diff 对比已保存视图
- 原地格式化 YAML，并尽量保留注释
- 编辑时提供基础校验反馈
- 不需要在项目里新建临时 YAML 文件

## 使用流程

1. 打开 `Config Assistant` 工具窗口。
2. 创建一个或多个 `View`。
3. 粘贴不同环境、不同服务或不同版本的 YAML。
4. 用 `Format` 统一整理格式。
5. 用 `Compare` 打开 IntelliJ 原生 Diff。
6. 保留这些视图，后续继续排查，不污染仓库。

## 功能说明

- 第一个 tab 固定为 `View`，不可删除。
- 后续 tab 按 `View 1`、`View 2` 递增命名。
- 对比使用 IntelliJ 原生 Diff，不在工具窗口里额外维护 Diff tab。
- 所有视图都按项目维度保存。
- 这个 workspace 更适合临时配置分析和比对，不是替代正式配置文件管理。

## 安装方式

### JetBrains Marketplace

在 JetBrains Marketplace 搜索并安装 `Config Assistant`。

### 本地 ZIP 安装

1. 构建或下载插件 ZIP。
2. 打开 IntelliJ IDEA 的 `Settings` -> `Plugins`。
3. 选择 `Install Plugin from Disk...`
4. 选择 `build/distributions/` 里的 ZIP 文件。

## 兼容性

- IntelliJ IDEA `2023.3+`
- `sinceBuild = 233`

## 本地开发

```bash
./gradlew runIde
./gradlew buildPlugin
```

构建产物：

```bash
build/distributions/config-assistant-*.zip
```

## 支持项目

如果 Config Assistant 在服务迁移、环境配置对比或问题排查中帮你节省了时间，欢迎自愿赞赏支持这个项目继续迭代。

| 微信赞赏 | 支付宝 |
| --- | --- |
| <img src="./docs/images/weixin-zanshang.png" alt="微信赞赏码" width="260" /> | <img src="./docs/images/zhifubao.png" alt="支付宝收款码" width="260" /> |

## License

MIT License
