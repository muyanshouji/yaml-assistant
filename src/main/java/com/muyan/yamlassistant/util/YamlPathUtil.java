package com.muyan.yamlassistant.util;

import com.muyan.yamlassistant.model.YamlNode;

import java.util.ArrayList;
import java.util.List;

/**
 * YAML 路径工具类 — 提供路径相关的工具方法。
 *
 * 用于:
 * - 从 YamlNode 获取完整路径（如 spring.datasource.url）
 * - 路径解析和查找
 */
public class YamlPathUtil {

    /**
     * 获取节点的完整路径
     *
     * @param node YAML 节点
     * @return 点分隔的路径字符串
     */
    public static String getFullPath(YamlNode node) {
        return node.getPath();
    }

    /**
     * 将路径字符串解析为路径段列表
     * 例: "spring.datasource.url" → ["spring", "datasource", "url"]
     *
     * @param path 点分隔路径
     * @return 路径段列表
     */
    public static List<String> parsePath(String path) {
        List<String> segments = new ArrayList<>();
        if (path == null || path.isEmpty()) {
            return segments;
        }

        StringBuilder current = new StringBuilder();
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '.' && current.length() > 0) {
                segments.add(current.toString());
                current = new StringBuilder();
            } else if (c == '[') {
                if (current.length() > 0) {
                    segments.add(current.toString());
                    current = new StringBuilder();
                }
                // 读取数组索引
                int end = path.indexOf(']', i);
                if (end > i) {
                    segments.add(path.substring(i, end + 1));
                    i = end;
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            segments.add(current.toString());
        }

        return segments;
    }

    /**
     * 在 YamlNode 树中根据路径查找节点
     *
     * @param root 根节点
     * @param path 目标路径
     * @return 找到的节点，未找到返回 null
     */
    public static YamlNode findByPath(YamlNode root, String path) {
        if (root == null || path == null) return null;

        if (path.equals(root.getPath())) {
            return root;
        }

        for (YamlNode child : root.getChildren()) {
            YamlNode found = findByPath(child, path);
            if (found != null) {
                return found;
            }
        }

        return null;
    }
}
