package com.muyan.yamlassistant.ui;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.muyan.yamlassistant.model.YamlDocument;
import com.muyan.yamlassistant.model.YamlNode;
import com.muyan.yamlassistant.services.YamlParserService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

/**
 * YAML 树形展示面板 — 在 Tool Window 中以 JTree 展示 YAML 结构。
 *
 * 核心逻辑:
 * 1. 监听编辑器文件切换事件（FileEditorManagerListener）
 * 2. 监听文档内容变化事件（DocumentListener）
 * 3. 当 YAML 文件内容变化时，重新解析并刷新 JTree
 */
public class YamlTreePanel {

    private final JPanel mainPanel;
    private final Tree tree;
    private final DefaultTreeModel treeModel;
    private final YamlParserService parserService;
    private final JLabel statusLabel;
    private DocumentListener currentDocListener;
    private com.intellij.openapi.editor.Document currentDocument;

    public YamlTreePanel(Project project) {
        this.parserService = new YamlParserService();

        // 初始化 UI 组件
        mainPanel = new JPanel(new BorderLayout());

        // 树形组件
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("No YAML file open");
        treeModel = new DefaultTreeModel(rootNode);
        tree = new Tree(treeModel);
        tree.setRootVisible(true);

        // 状态栏
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        // 布局
        mainPanel.add(new JBScrollPane(tree), BorderLayout.CENTER);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        // 监听编辑器切换
        project.getMessageBus().connect().subscribe(
                FileEditorManagerListener.FILE_EDITOR_MANAGER,
                new FileEditorManagerListener() {
                    @Override
                    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                        VirtualFile file = event.getNewFile();
                        if (file != null && isYamlFile(file)) {
                            refreshFromEditor(project);
                        }
                    }
                }
        );

        // 初始加载当前编辑器内容
        refreshFromEditor(project);
    }

    /**
     * 从当前编辑器获取内容并刷新树形结构
     */
    private void refreshFromEditor(Project project) {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            updateStatus("No editor open");
            return;
        }

        String text = editor.getDocument().getText();
        refreshTree(text);

        // 移除之前的监听器，防止累积
        if (currentDocListener != null && currentDocument != null) {
            currentDocument.removeDocumentListener(currentDocListener);
        }

        // 添加文档变化监听（实时刷新）
        currentDocument = editor.getDocument();
        currentDocListener = new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                refreshTree(event.getDocument().getText());
            }
        };
        currentDocument.addDocumentListener(currentDocListener);
    }

    /**
     * 解析 YAML 并刷新 JTree
     */
    private void refreshTree(String yamlText) {
        if (yamlText == null || yamlText.trim().isEmpty()) {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Empty");
            treeModel.setRoot(root);
            updateStatus("Empty content");
            return;
        }

        YamlDocument document = parserService.parse(yamlText);

        if (!document.isValid()) {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Parse Error");
            root.add(new DefaultMutableTreeNode(document.getErrorMessage()));
            treeModel.setRoot(root);
            updateStatus("Parse error");
            return;
        }

        // 构建 JTree
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("YAML");
        for (YamlNode yamlRoot : document.getRoots()) {
            DefaultMutableTreeNode treeNode = buildTreeNode(yamlRoot);
            root.add(treeNode);
        }

        treeModel.setRoot(root);
        expandAll();
        updateStatus("Parsed successfully");
    }

    /**
     * 递归构建 JTree 节点
     */
    private DefaultMutableTreeNode buildTreeNode(YamlNode yamlNode) {
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(yamlNode.getDisplayText());

        for (YamlNode child : yamlNode.getChildren()) {
            treeNode.add(buildTreeNode(child));
        }

        return treeNode;
    }

    /**
     * 展开所有树节点
     */
    private void expandAll() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    private void updateStatus(String text) {
        statusLabel.setText(text);
    }

    private boolean isYamlFile(VirtualFile file) {
        String ext = file.getExtension();
        return "yaml".equalsIgnoreCase(ext) || "yml".equalsIgnoreCase(ext);
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}
