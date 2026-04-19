package com.muyan.yamlassistant.diff;

import java.util.ArrayList;
import java.util.List;

/**
 * YAML 差异比对结果模型。
 *
 * 包含差异项列表，每项记录:
 * - path: YAML 键路径
 * - diffType: 差异类型（ADDED/REMOVED/MODIFIED）
 * - leftValue: 左侧值
 * - rightValue: 右侧值
 */
public class YamlDiffResult {

    /**
     * 差异类型
     */
    public enum DiffType {
        /** 新增: 右侧有，左侧无 */
        ADDED,
        /** 删除: 左侧有，右侧无 */
        REMOVED,
        /** 修改: 两侧值不同 */
        MODIFIED,
        /** 未变: 两侧值相同 */
        UNCHANGED
    }

    /**
     * 单个差异项
     */
    public static class DiffEntry {
        private final String path;
        private final DiffType diffType;
        private final String leftValue;
        private final String rightValue;

        public DiffEntry(String path, DiffType diffType, String leftValue, String rightValue) {
            this.path = path;
            this.diffType = diffType;
            this.leftValue = leftValue;
            this.rightValue = rightValue;
        }

        public String getPath() { return path; }
        public DiffType getDiffType() { return diffType; }
        public String getLeftValue() { return leftValue; }
        public String getRightValue() { return rightValue; }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s → %s", diffType, path, leftValue, rightValue);
        }
    }

    private final List<DiffEntry> diffs;
    private String error;

    public YamlDiffResult() {
        this.diffs = new ArrayList<>();
    }

    public void addDiff(String path, DiffType diffType, String leftValue, String rightValue) {
        diffs.add(new DiffEntry(path, diffType, leftValue, rightValue));
    }

    public boolean hasDifferences() {
        return !diffs.isEmpty();
    }

    public int getDiffCount() {
        return diffs.size();
    }

    public List<DiffEntry> getDiffs() {
        return diffs;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    /**
     * 生成可读的差异摘要
     */
    public String getSummary() {
        if (error != null) {
            return "Error: " + error;
        }
        if (!hasDifferences()) {
            return "No differences found.";
        }

        long added = diffs.stream().filter(d -> d.getDiffType() == DiffType.ADDED).count();
        long removed = diffs.stream().filter(d -> d.getDiffType() == DiffType.REMOVED).count();
        long modified = diffs.stream().filter(d -> d.getDiffType() == DiffType.MODIFIED).count();

        return String.format("Differences: %d added, %d removed, %d modified",
                added, removed, modified);
    }
}
