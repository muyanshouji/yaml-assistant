package com.muyan.yamlassistant.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.muyan.yamlassistant.services.YamlConverterService;
import org.jetbrains.annotations.NotNull;

/**
 * YAML ↔ JSON 转换 Action — 自动检测当前内容格式并转换。
 *
 * 逻辑:
 * 1. 获取编辑器文本
 * 2. 检测是 JSON 还是 YAML
 * 3. JSON → YAML 或 YAML → JSON
 * 4. 替换编辑器内容
 */
public class ConvertYamlJsonAction extends AnAction {

    private final YamlConverterService converterService = new YamlConverterService();

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (project == null || editor == null) return;

        Document document = editor.getDocument();
        String originalText = document.getText();

        try {
            String converted;
            if (converterService.isJson(originalText)) {
                // JSON → YAML
                converted = converterService.jsonToYaml(originalText);
            } else {
                // YAML → JSON
                converted = converterService.yamlToJson(originalText);
            }

            WriteCommandAction.runWriteCommandAction(project, () ->
                    document.setText(converted)
            );
        } catch (Exception ex) {
            // TODO: 显示错误通知
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        e.getPresentation().setEnabled(editor != null);
    }
}
