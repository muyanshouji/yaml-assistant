package com.muyan.yamlassistant.model;

import java.util.ArrayList;
import java.util.List;

/**
 * YAML 文档模型 — 支持一个 YAML 文件包含多个文档（用 --- 分隔）。
 */
public class YamlDocument {

    /** 文档名称/来源文件路径 */
    private String sourcePath;

    /** 原始 YAML 文本 */
    private String rawText;

    /** 解析后的根节点列表（多文档场景下有多个根节点） */
    private List<YamlNode> roots;

    /** 是否解析成功 */
    private boolean valid;

    /** 解析错误信息 */
    private String errorMessage;

    public YamlDocument() {
        this.roots = new ArrayList<>();
        this.valid = true;
    }

    public YamlDocument(String sourcePath, String rawText) {
        this();
        this.sourcePath = sourcePath;
        this.rawText = rawText;
    }

    public void addRoot(YamlNode root) {
        this.roots.add(root);
    }

    // ========== Getters & Setters ==========

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public List<YamlNode> getRoots() {
        return roots;
    }

    public void setRoots(List<YamlNode> roots) {
        this.roots = roots;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
