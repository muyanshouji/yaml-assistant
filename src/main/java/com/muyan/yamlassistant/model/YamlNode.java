package com.muyan.yamlassistant.model;

import java.util.ArrayList;
import java.util.List;

/**
 * YAML 节点模型 — 将 YAML 结构表示为树形节点。
 *
 * 每个节点包含:
 * - key: 键名（根节点为 "root"）
 * - value: 值（叶子节点有值，分支节点为 null）
 * - nodeType: 节点类型（MAPPING / SEQUENCE / SCALAR）
 * - children: 子节点列表
 * - path: 完整路径（如 "spring.datasource.url"）
 */
public class YamlNode {

    /**
     * YAML 节点类型
     */
    public enum NodeType {
        /** 映射节点（Map/Object），包含键值对子节点 */
        MAPPING,
        /** 序列节点（List/Array），包含有序子节点 */
        SEQUENCE,
        /** 标量节点（叶子），包含具体值 */
        SCALAR
    }

    private String key;
    private Object value;
    private NodeType nodeType;
    private List<YamlNode> children;
    private String path;

    public YamlNode() {
        this.children = new ArrayList<>();
    }

    public YamlNode(String key, Object value, NodeType nodeType) {
        this.key = key;
        this.value = value;
        this.nodeType = nodeType;
        this.children = new ArrayList<>();
    }

    /**
     * 添加子节点
     */
    public void addChild(YamlNode child) {
        this.children.add(child);
    }

    /**
     * 是否为叶子节点
     */
    public boolean isLeaf() {
        return nodeType == NodeType.SCALAR;
    }

    /**
     * 获取节点的展示文本（用于 JTree 渲染）
     */
    public String getDisplayText() {
        if (isLeaf() && value != null) {
            return key + ": " + value;
        }
        return key;
    }

    // ========== Getters & Setters ==========

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public List<YamlNode> getChildren() {
        return children;
    }

    public void setChildren(List<YamlNode> children) {
        this.children = children;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return getDisplayText();
    }
}
