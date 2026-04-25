package com.muyan.yamlassistant.actions;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

/**
 * 查看 YAML 树 Action — 在侧边栏 Tool Window 中以树形结构展示当前 YAML 文件。
 *
 * 触发方式:
 * 1. 菜单: Tools → Config Assistant → View YAML Tree
 * 2. 编辑器右键 → Config Assistant → View YAML Tree
 *
 * 逻辑:
 * 1. 获取当前编辑器的文本内容
 * 2. 打开 Config Assistant Tool Window
 * 3. Tool Window 内部会解析 YAML 并展示树形结构
 */
public class ViewYamlTreeAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (!isYamlContext(e)) {
            if (editor != null) {
                HintManager.getInstance().showErrorHint(editor, "Config Assistant only works with YAML files.");
            }
            return;
        }

        // 打开 Config Assistant Tool Window
        ToolWindow toolWindow = ToolWindowManager.getInstance(project)
                .getToolWindow("Config Assistant");
        if (toolWindow != null) {
            toolWindow.show();
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
}
