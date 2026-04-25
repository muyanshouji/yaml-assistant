package com.muyan.yamlassistant.actions;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.muyan.yamlassistant.services.YamlFormatterService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

import com.intellij.psi.PsiFile;

/**
 * 格式化 YAML Action — 美化当前编辑器中的 YAML 内容。
 *
 * 触发方式:
 * 1. 菜单: Tools → Config Assistant → Format YAML
 * 2. 编辑器右键 → Config Assistant → Format YAML
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
        if (!isYamlContext(e)) {
            showError(editor, "Config Assistant only works with YAML files.");
            return;
        }

        Document document = editor.getDocument();
        String originalText = document.getText();

        try {
            String formatted = formatterService.beautify(originalText);

            // 使用 WriteCommandAction 确保操作可撤销
            WriteCommandAction.runWriteCommandAction(project, () ->
                    document.setText(formatted)
            );
        } catch (Exception ex) {
            showError(editor, "Format failed: " + safeMessage(ex));
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(isYamlContext(e));
    }

    private static boolean isYamlContext(AnActionEvent e) {
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (psiFile != null) {
            return psiFile.getFileType() == YAMLFileType.YML;
        }

        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        return virtualFile != null && virtualFile.getFileType() == YAMLFileType.YML;
    }

    private static void showError(Editor editor, String message) {
        HintManager.getInstance().showErrorHint(editor, message);
    }

    private static String safeMessage(Exception ex) {
        return ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
    }
}
