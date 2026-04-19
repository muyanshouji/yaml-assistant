package com.muyan.yamlassistant.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

/**
 * 查看 YAML 树 Action — 在侧边栏 Tool Window 中以树形结构展示当前 YAML 文件。
 *
 * 触发方式:
 * 1. 菜单: Tools → YAML Assistant → View YAML Tree
 * 2. 编辑器右键 → YAML Assistant → View YAML Tree
 *
 * 逻辑:
 * 1. 获取当前编辑器的文本内容
 * 2. 打开 YAML Assistant Tool Window
 * 3. Tool Window 内部会解析 YAML 并展示树形结构
 */
public class ViewYamlTreeAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        // 打开 YAML Assistant Tool Window
        ToolWindow toolWindow = ToolWindowManager.getInstance(project)
                .getToolWindow("YAML Assistant");
        if (toolWindow != null) {
            toolWindow.show();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // 只在有编辑器打开时启用此 Action
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        e.getPresentation().setEnabled(editor != null);
    }
}
