[English](./README.md) | [简体中文](./README.zh-CN.md)

# Config Assistant

[![JetBrains Plugin Version](https://img.shields.io/jetbrains/plugin/v/31658?label=Marketplace)](https://plugins.jetbrains.com/plugin/31658-config-assistant)
[![JetBrains Plugin Downloads](https://img.shields.io/jetbrains/plugin/d/31658)](https://plugins.jetbrains.com/plugin/31658-config-assistant)
[![GitHub Release](https://img.shields.io/github/v/release/muyanshouji/config-assistant)](https://github.com/muyanshouji/config-assistant/releases)
[![Build](https://github.com/muyanshouji/config-assistant/actions/workflows/build.yml/badge.svg)](https://github.com/muyanshouji/config-assistant/actions/workflows/build.yml)
![IntelliJ IDEA 2023.3+](https://img.shields.io/badge/IntelliJ%20IDEA-2023.3%2B-000000?logo=intellijidea)

Inspect, validate, format, and compare YAML or `.properties` configs faster inside IntelliJ IDEA.

**Quick links:** [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/31658-config-assistant) · [GitHub Releases](https://github.com/muyanshouji/config-assistant/releases) · [Latest Release](https://github.com/muyanshouji/config-assistant/releases/latest) · [Report an Issue](https://github.com/muyanshouji/config-assistant/issues)

Config Assistant gives you a lightweight config workspace inside IntelliJ IDEA for temporary YAML and `.properties` inspection and comparison. It is especially useful when you are checking differences across environments, reviewing service migration configs, or debugging deployment issues without creating throwaway local files in your project.

## Why Config Assistant

When you are comparing configs from different environments, the usual workflow is noisy:

- create a temporary local file in the project
- paste one config into one file and another config into a second file
- open a diff manually
- clean up the temporary files afterward

Config Assistant removes that friction.

You can open a dedicated tool window, create saved views for different configs, compare them with IntelliJ's native diff, and keep everything persisted at the project level.

Typical use cases:

- compare test and production YAML during incident investigation
- review configuration differences during service migration
- keep before and after snapshots while adjusting config
- format pasted YAML quickly before sharing or reviewing it
- keep copied configs in temporary project-level views instead of creating throwaway files

## Key Features

- Multi-view YAML and `.properties` workspace inside IntelliJ IDEA
- Project-level persisted views that survive IDE restart
- Native IntelliJ diff between saved views of the same type
- In-place YAML formatting with comment preservation
- Inline YAML and `.properties` validation feedback while editing
- No need to create temporary config files in your project

## Workflow

1. Open `Config Assistant` from the tool window.
2. Choose `YAML` or `Properties`, then create one or more `View` tabs.
3. Paste config content from different environments or systems.
4. Use `Format` to normalize YAML or `.properties` content when needed.
5. Use `Compare` to open IntelliJ's native diff for two views of the same type.
6. Keep the views for later review without cluttering the repo.

## Feature Notes

- The first tab is fixed as `View` and cannot be deleted.
- Additional tabs use incremental names like `View 1`, `View 2`, and so on.
- YAML and `Properties` keep independent view lists.
- Compare opens IntelliJ's native diff viewer instead of an in-tool diff tab.
- Format supports both YAML and `.properties` views.
- Views are stored at the project level.
- The workspace is designed for temporary config analysis, not committed config authoring.

## Installation

### JetBrains Marketplace

[Install `Config Assistant` from JetBrains Marketplace](https://plugins.jetbrains.com/plugin/31658-config-assistant).

New Marketplace uploads may take a short review cycle before the latest version appears publicly.

### GitHub Release ZIP

[Download the latest plugin ZIP from GitHub Releases](https://github.com/muyanshouji/config-assistant/releases/latest).

### Local ZIP

1. Build or download the plugin ZIP.
2. Open `Settings` -> `Plugins` in IntelliJ IDEA.
3. Choose `Install Plugin from Disk...`
4. Select the ZIP from `build/distributions/`.

## Compatibility

- IntelliJ IDEA `2023.3+`
- `sinceBuild = 233`

## Development

```bash
./gradlew runIde
./gradlew buildPlugin
```

Plugin ZIP output:

```bash
build/distributions/config-assistant-*.zip
```

## Support the Project

If Config Assistant helps you compare configs faster during migration, deployment, or troubleshooting, you can support its continued development with a small donation.

Chinese donation options are available in the [Chinese README](./README.zh-CN.md).

## License

MIT License
