package com.muyan.yamlassistant.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * YAML Assistant Tool Window 工厂 — 创建侧边栏面板。
 *
 * 在 plugin.xml 中注册:
 * <toolWindow id="YAML Assistant" factoryClass="...YamlToolWindowFactory"/>
 *
 * IDEA 启动后，在侧边栏显示 "YAML Assistant" 入口，
 * 用户点击后调用 createToolWindowContent() 初始化面板。
 */
public class YamlToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 创建 YAML 树形展示面板
        YamlTreePanel treePanel = new YamlTreePanel(project);

        // 将面板添加到 Tool Window
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(treePanel.getMainPanel(), "YAML Tree", false);
        toolWindow.getContentManager().addContent(content);
    }
}
