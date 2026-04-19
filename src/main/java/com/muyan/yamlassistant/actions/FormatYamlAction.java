package com.muyan.yamlassistant.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.muyan.yamlassistant.services.YamlFormatterService;
import org.jetbrains.annotations.NotNull;

/**
 * 格式化 YAML Action — 美化当前编辑器中的 YAML 内容。
 *
 * 触发方式:
 * 1. 菜单: Tools → YAML Assistant → Format YAML
 * 2. 编辑器右键 → YAML Assistant → Format YAML
 *
 * 逻辑:
 * 1. 获取当前编辑器文本
 * 2. 调用 YamlFormatterService.beautify() 格式化
 * 3. 通过 WriteCommandAction 替换编辑器内容（支持撤销）
 */
public class FormatYamlAction extends AnAction {

    private final YamlFormatterService formatterService = new YamlFormatterService();

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (project == null || editor == null) return;

        Document document = editor.getDocument();
        String originalText = document.getText();

        try {
            String formatted = formatterService.beautify(originalText);

            // 使用 WriteCommandAction 确保操作可撤销
            WriteCommandAction.runWriteCommandAction(project, () ->
                    document.setText(formatted)
            );
        } catch (Exception ex) {
            // TODO: 显示错误通知
            // NotificationUtil.showError(project, "Format failed: " + ex.getMessage());
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        e.getPresentation().setEnabled(editor != null);
    }
}
