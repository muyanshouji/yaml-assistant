package com.muyan.yamlassistant.actions;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 比对 YAML Action — 选择两个 YAML 文件进行结构化差异对比。
 *
 * 逻辑:
 * 1. 弹出文件选择器，让用户选择两个 YAML 文件
 * 2. 读取两个文件的内容
 * 3. 使用 IDEA 内置 DiffManager 展示差异（文本级别）
 *
 * 注: 结构化差异（YamlDiffService）可用于后续自定义展示面板
 */
public class CompareYamlAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        // 选择第一个文件
        VirtualFile file1 = FileChooser.chooseFile(
                FileChooserDescriptorFactory.createSingleFileDescriptor("yaml"),
                project, null);
        if (file1 == null) return;

        // 选择第二个文件
        VirtualFile file2 = FileChooser.chooseFile(
                FileChooserDescriptorFactory.createSingleFileDescriptor("yaml"),
                project, null);
        if (file2 == null) return;

        try {
            // 使用 IDEA 内置 Diff 视图
            DiffContent content1 = DiffContentFactory.getInstance().create(
                    project, new String(file1.contentsToByteArray(), StandardCharsets.UTF_8));
            DiffContent content2 = DiffContentFactory.getInstance().create(
                    project, new String(file2.contentsToByteArray(), StandardCharsets.UTF_8));

            SimpleDiffRequest diffRequest = new SimpleDiffRequest(
                    "YAML Comparison",
                    content1, content2,
                    file1.getName(), file2.getName());

            DiffManager.getInstance().showDiff(project, diffRequest);
        } catch (IOException ex) {
            // TODO: 显示错误通知
        }
    }
}
