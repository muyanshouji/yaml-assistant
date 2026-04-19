package com.muyan.yamlassistant.settings;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * 插件设置界面 — 在 Settings → Tools → YAML Assistant 中显示。
 *
 * 实现 Configurable 接口:
 * - createComponent(): 创建设置面板 UI
 * - isModified(): 判断用户是否修改了设置
 * - apply(): 保存设置
 * - reset(): 重置为已保存的值
 */
public class YamlAssistantConfigurable implements Configurable {

    private JSpinner indentSpinner;
    private JSpinner lineWidthSpinner;
    private JCheckBox autoRefreshCheckBox;
    private JCheckBox showValueTypeCheckBox;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "YAML Assistant";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        YamlAssistantSettings.State state = YamlAssistantSettings.getInstance().getState();

        // 缩进大小
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Indent size:"), gbc);
        gbc.gridx = 1;
        indentSpinner = new JSpinner(new SpinnerNumberModel(
                state != null ? state.indentSize : 2, 1, 8, 1));
        panel.add(indentSpinner, gbc);

        // 行宽
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Line width:"), gbc);
        gbc.gridx = 1;
        lineWidthSpinner = new JSpinner(new SpinnerNumberModel(
                state != null ? state.lineWidth : 120, 40, 500, 10));
        panel.add(lineWidthSpinner, gbc);

        // 自动刷新
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        autoRefreshCheckBox = new JCheckBox("Auto refresh tree view",
                state != null && state.autoRefreshTree);
        panel.add(autoRefreshCheckBox, gbc);

        // 显示值类型
        gbc.gridy = 3;
        showValueTypeCheckBox = new JCheckBox("Show value types in tree",
                state != null && state.showValueType);
        panel.add(showValueTypeCheckBox, gbc);

        // 填充剩余空间
        gbc.gridy = 4; gbc.weighty = 1.0;
        panel.add(new JPanel(), gbc);

        return panel;
    }

    @Override
    public boolean isModified() {
        YamlAssistantSettings.State state = YamlAssistantSettings.getInstance().getState();
        if (state == null) return false;

        return (int) indentSpinner.getValue() != state.indentSize
                || (int) lineWidthSpinner.getValue() != state.lineWidth
                || autoRefreshCheckBox.isSelected() != state.autoRefreshTree
                || showValueTypeCheckBox.isSelected() != state.showValueType;
    }

    @Override
    public void apply() {
        YamlAssistantSettings.State state = YamlAssistantSettings.getInstance().getState();
        if (state == null) return;

        state.indentSize = (int) indentSpinner.getValue();
        state.lineWidth = (int) lineWidthSpinner.getValue();
        state.autoRefreshTree = autoRefreshCheckBox.isSelected();
        state.showValueType = showValueTypeCheckBox.isSelected();
    }

    @Override
    public void reset() {
        YamlAssistantSettings.State state = YamlAssistantSettings.getInstance().getState();
        if (state == null) return;

        indentSpinner.setValue(state.indentSize);
        lineWidthSpinner.setValue(state.lineWidth);
        autoRefreshCheckBox.setSelected(state.autoRefreshTree);
        showValueTypeCheckBox.setSelected(state.showValueType);
    }
}
