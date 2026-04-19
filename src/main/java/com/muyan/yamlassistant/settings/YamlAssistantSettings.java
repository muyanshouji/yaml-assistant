package com.muyan.yamlassistant.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 插件设置状态 — 持久化插件的用户配置。
 *
 * 使用 IDEA 的 PersistentStateComponent 机制:
 * - @State: 指定存储名称和位置
 * - @Storage: 指定存储文件名
 * - 配置会自动序列化为 XML 存储在用户的 IDEA 配置目录中
 */
@State(
        name = "YamlAssistantSettings",
        storages = @Storage("YamlAssistantSettings.xml")
)
public class YamlAssistantSettings implements PersistentStateComponent<YamlAssistantSettings.State> {

    /**
     * 设置状态（会被序列化存储）
     */
    public static class State {
        /** YAML 缩进空格数 */
        public int indentSize = 2;

        /** 格式化时的行宽限制 */
        public int lineWidth = 120;

        /** 是否自动刷新树形视图 */
        public boolean autoRefreshTree = true;

        /** 是否在树形视图中显示值类型 */
        public boolean showValueType = false;
    }

    private State state = new State();

    public static YamlAssistantSettings getInstance() {
        return ApplicationManager.getApplication().getService(YamlAssistantSettings.class);
    }

    @Nullable
    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }
}
