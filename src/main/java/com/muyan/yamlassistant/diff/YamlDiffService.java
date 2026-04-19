package com.muyan.yamlassistant.diff;

import java.util.*;

/**
 * YAML Diff 比对服务 — 递归比较两个 YAML 结构的差异。
 *
 * 核心算法:
 * 1. 将两个 YAML 文本解析为 Map 结构
 * 2. 递归遍历两个 Map 的所有 Key
 * 3. 对每个 Key 判断: ADDED / REMOVED / MODIFIED / UNCHANGED
 * 4. 将差异收集到 YamlDiffResult
 */
public class YamlDiffService {

    private final org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();

    /**
     * 比对两个 YAML 文本
     *
     * @param yamlLeft  左侧 YAML 文本
     * @param yamlRight 右侧 YAML 文本
     * @return 差异结果
     */
    @SuppressWarnings("unchecked")
    public YamlDiffResult compare(String yamlLeft, String yamlRight) {
        YamlDiffResult result = new YamlDiffResult();

        try {
            Object left = yaml.load(yamlLeft);
            Object right = yaml.load(yamlRight);

            if (left instanceof Map && right instanceof Map) {
                compareMap((Map<String, Object>) left, (Map<String, Object>) right, "", result);
            } else {
                // 非 Map 结构，直接比较
                if (!Objects.equals(left, right)) {
                    result.addDiff("", YamlDiffResult.DiffType.MODIFIED,
                            String.valueOf(left), String.valueOf(right));
                }
            }
        } catch (Exception e) {
            result.setError("Diff error: " + e.getMessage());
        }

        return result;
    }

    /**
     * 递归比较两个 Map
     */
    @SuppressWarnings("unchecked")
    private void compareMap(Map<String, Object> left, Map<String, Object> right,
                            String parentPath, YamlDiffResult result) {

        // 收集所有 Key
        Set<String> allKeys = new LinkedHashSet<>();
        allKeys.addAll(left.keySet());
        allKeys.addAll(right.keySet());

        for (String key : allKeys) {
            String currentPath = parentPath.isEmpty() ? key : parentPath + "." + key;
            boolean inLeft = left.containsKey(key);
            boolean inRight = right.containsKey(key);

            if (inLeft && !inRight) {
                // 左边有，右边没有 → REMOVED
                result.addDiff(currentPath, YamlDiffResult.DiffType.REMOVED,
                        String.valueOf(left.get(key)), null);
            } else if (!inLeft && inRight) {
                // 左边没有，右边有 → ADDED
                result.addDiff(currentPath, YamlDiffResult.DiffType.ADDED,
                        null, String.valueOf(right.get(key)));
            } else {
                // 两边都有 → 递归比较
                Object leftVal = left.get(key);
                Object rightVal = right.get(key);

                if (leftVal instanceof Map && rightVal instanceof Map) {
                    compareMap((Map<String, Object>) leftVal,
                            (Map<String, Object>) rightVal, currentPath, result);
                } else if (leftVal instanceof List && rightVal instanceof List) {
                    compareList((List<Object>) leftVal,
                            (List<Object>) rightVal, currentPath, result);
                } else if (!Objects.equals(leftVal, rightVal)) {
                    result.addDiff(currentPath, YamlDiffResult.DiffType.MODIFIED,
                            String.valueOf(leftVal), String.valueOf(rightVal));
                }
                // 相同则跳过（UNCHANGED 不记录，减少噪音）
            }
        }
    }

    /**
     * 比较两个 List
     */
    @SuppressWarnings("unchecked")
    private void compareList(List<Object> left, List<Object> right,
                             String parentPath, YamlDiffResult result) {
        int maxLen = Math.max(left.size(), right.size());

        for (int i = 0; i < maxLen; i++) {
            String currentPath = parentPath + "[" + i + "]";

            if (i >= left.size()) {
                result.addDiff(currentPath, YamlDiffResult.DiffType.ADDED,
                        null, String.valueOf(right.get(i)));
            } else if (i >= right.size()) {
                result.addDiff(currentPath, YamlDiffResult.DiffType.REMOVED,
                        String.valueOf(left.get(i)), null);
            } else {
                Object leftVal = left.get(i);
                Object rightVal = right.get(i);

                if (leftVal instanceof Map && rightVal instanceof Map) {
                    compareMap((Map<String, Object>) leftVal,
                            (Map<String, Object>) rightVal, currentPath, result);
                } else if (leftVal instanceof List && rightVal instanceof List) {
                    compareList((List<Object>) leftVal,
                            (List<Object>) rightVal, currentPath, result);
                } else if (!Objects.equals(leftVal, rightVal)) {
                    result.addDiff(currentPath, YamlDiffResult.DiffType.MODIFIED,
                            String.valueOf(leftVal), String.valueOf(rightVal));
                }
            }
        }
    }
}
