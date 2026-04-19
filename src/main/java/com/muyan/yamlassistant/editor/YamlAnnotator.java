package com.muyan.yamlassistant.editor;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.muyan.yamlassistant.services.YamlParserService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * YAML 语法错误标注器 — 在编辑器中高亮显示 YAML 语法错误。
 *
 * 继承 ExternalAnnotator 而非 Annotator，因为 YAML 解析可能较慢，
 * ExternalAnnotator 在后台线程执行，不会阻塞 UI。
 *
 * 流程:
 * 1. collectInformation() — 收集 PsiFile 信息
 * 2. doAnnotate() — 后台线程执行 YAML 语法校验
 * 3. apply() — 将错误标注应用到编辑器
 */
public class YamlAnnotator extends ExternalAnnotator<String, String> {

    private final YamlParserService parserService = new YamlParserService();

    @Nullable
    @Override
    public String collectInformation(@NotNull PsiFile file) {
        // 只处理 YAML 文件
        String fileName = file.getName();
        if (!fileName.endsWith(".yaml") && !fileName.endsWith(".yml")) {
            return null;
        }
        return file.getText();
    }

    @Nullable
    @Override
    public String doAnnotate(String yamlText) {
        // 后台线程执行校验
        return parserService.validate(yamlText);
    }

    @Override
    public void apply(@NotNull PsiFile file, String errorMessage, @NotNull AnnotationHolder holder) {
        if (errorMessage != null) {
            Document document = file.getViewProvider().getDocument();
            if (document != null) {
                // 在文件开头标注错误（SnakeYAML 的错误信息中通常包含行号）
                holder.newAnnotation(HighlightSeverity.ERROR, "YAML Syntax Error: " + errorMessage)
                        .range(new TextRange(0, Math.min(document.getTextLength(), 1)))
                        .create();
            }
        }
    }
}
