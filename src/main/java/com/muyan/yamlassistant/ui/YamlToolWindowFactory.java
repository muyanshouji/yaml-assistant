package com.muyan.yamlassistant.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Config Assistant Tool Window 工厂 — 创建侧边栏面板。
 *
 * 在 plugin.xml 中注册:
 * <toolWindow id="Config Assistant" factoryClass="...YamlToolWindowFactory"/>
 *
 * IDEA 启动后，在侧边栏显示 "Config Assistant" 入口，
 * 用户点击后调用 createToolWindowContent() 初始化面板。
 */
public class YamlToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        YamlWorkspacePanel workspacePanel = new YamlWorkspacePanel(project, toolWindow::hide);

        if (toolWindow instanceof ToolWindowEx toolWindowEx) {
            toolWindowEx.setTitleActions(java.util.Collections.emptyList());
            toolWindowEx.setTabActions();
        }

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(workspacePanel.getMainPanel(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
