package com.muyan.yamlassistant.services;

import com.muyan.yamlassistant.model.YamlDocument;
import com.muyan.yamlassistant.model.YamlNode;
import com.muyan.yamlassistant.model.YamlNode.NodeType;
import com.muyan.yamlassistant.util.YamlPlaceholderSupport;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

/**
 * YAML 解析服务 — 将 YAML 文本解析为 YamlNode 树形结构。
 *
 * 核心流程:
 * 1. 使用 SnakeYAML 将 YAML 字符串解析为 Java 对象（Map/List/String）
 * 2. 递归遍历 Java 对象，构建 YamlNode 树
 * 3. 计算每个节点的完整路径（如 spring.datasource.url）
 */
public class YamlParserService {

    private final Yaml yaml;
    private final YamlPlaceholderSupport placeholderSupport;

    public YamlParserService() {
        this.yaml = new Yaml();
        this.placeholderSupport = new YamlPlaceholderSupport();
    }

    /**
     * 解析 YAML 文本为文档模型
     *
     * @param yamlText YAML 文本内容
     * @return YamlDocument 解析结果
     */
    public YamlDocument parse(String yamlText) {
        YamlDocument document = new YamlDocument();
        document.setRawText(yamlText);

        if (yamlText == null || yamlText.trim().isEmpty()) {
            document.setValid(false);
            document.setErrorMessage("YAML content is empty");
            return document;
        }

        try {
            YamlPlaceholderSupport.SanitizedYaml sanitizedYaml = placeholderSupport.sanitize(yamlText);
            // SnakeYAML loadAll 支持多文档（--- 分隔）
            Iterable<Object> documents = yaml.loadAll(sanitizedYaml.getText());
            int docIndex = 0;

            for (Object doc : documents) {
                YamlNode root = new YamlNode("Document " + docIndex, null, NodeType.MAPPING);
                root.setPath("");

                Object restoredDoc = placeholderSupport.restoreObject(doc, sanitizedYaml);
                if (restoredDoc != null) {
                    buildTree(root, restoredDoc, "");
                }

                document.addRoot(root);
                docIndex++;
            }

            document.setValid(true);
        } catch (Exception e) {
            document.setValid(false);
            document.setErrorMessage("YAML parse error: " + restoreErrorMessage(yamlText, e));
        }

        return document;
    }

    /**
     * 递归构建 YamlNode 树
     *
     * @param parent    父节点
     * @param data      SnakeYAML 解析出的 Java 对象
     * @param parentPath 父节点路径
     */
    @SuppressWarnings("unchecked")
    private void buildTree(YamlNode parent, Object data, String parentPath) {
        if (data instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) data;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                String currentPath = parentPath.isEmpty() ? key : parentPath + "." + key;

                if (value instanceof Map) {
                    // 映射节点
                    YamlNode child = new YamlNode(key, null, NodeType.MAPPING);
                    child.setPath(currentPath);
                    buildTree(child, value, currentPath);
                    parent.addChild(child);
                } else if (value instanceof List) {
                    // 序列节点
                    YamlNode child = new YamlNode(key, null, NodeType.SEQUENCE);
                    child.setPath(currentPath);
                    buildTree(child, value, currentPath);
                    parent.addChild(child);
                } else {
                    // 标量节点（叶子）
                    YamlNode child = new YamlNode(key, value, NodeType.SCALAR);
                    child.setPath(currentPath);
                    parent.addChild(child);
                }
            }
        } else if (data instanceof List) {
            List<Object> list = (List<Object>) data;
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                String indexKey = "[" + i + "]";
                String currentPath = parentPath + indexKey;

                if (item instanceof Map || item instanceof List) {
                    NodeType type = item instanceof Map ? NodeType.MAPPING : NodeType.SEQUENCE;
                    YamlNode child = new YamlNode(indexKey, null, type);
                    child.setPath(currentPath);
                    buildTree(child, item, currentPath);
                    parent.addChild(child);
                } else {
                    YamlNode child = new YamlNode(indexKey, item, NodeType.SCALAR);
                    child.setPath(currentPath);
                    parent.addChild(child);
                }
            }
        }
    }

    /**
     * 检查 YAML 文本是否合法
     *
     * @param yamlText YAML 文本
     * @return null 表示合法，否则返回错误信息
     */
    public String validate(String yamlText) {
        try {
            YamlPlaceholderSupport.SanitizedYaml sanitizedYaml = placeholderSupport.sanitize(yamlText);
            yaml.load(sanitizedYaml.getText());
            return null;
        } catch (Exception e) {
            return restoreErrorMessage(yamlText, e);
        }
    }

    private String restoreErrorMessage(String yamlText, Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return e.getClass().getSimpleName();
        }
        return placeholderSupport.restoreText(message, placeholderSupport.sanitize(yamlText));
    }
}
