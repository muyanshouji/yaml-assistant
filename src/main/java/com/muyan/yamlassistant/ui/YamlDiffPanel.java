package com.muyan.yamlassistant.ui;

import com.muyan.yamlassistant.diff.YamlDiffResult;
import com.muyan.yamlassistant.diff.YamlDiffResult.DiffEntry;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * YAML 差异展示面板 — 以表格形式展示两个 YAML 的结构化差异。
 *
 * 用于自定义 Diff 视图（与 IDEA 内置 DiffManager 互补）。
 * 展示内容: 路径 | 差异类型 | 左侧值 | 右侧值
 */
public class YamlDiffPanel {

    private final JPanel mainPanel;
    private final JTable diffTable;
    private final DefaultTableModel tableModel;
    private final JLabel summaryLabel;

    public YamlDiffPanel() {
        mainPanel = new JPanel(new BorderLayout());

        // 摘要标签
        summaryLabel = new JLabel("No comparison performed");
        summaryLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        mainPanel.add(summaryLabel, BorderLayout.NORTH);

        // 差异表格
        String[] columns = {"Path", "Type", "Left Value", "Right Value"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 只读
            }
        };

        diffTable = new JTable(tableModel);
        diffTable.setDefaultRenderer(Object.class, new DiffCellRenderer());
        diffTable.setRowHeight(25);

        mainPanel.add(new JScrollPane(diffTable), BorderLayout.CENTER);
    }

    /**
     * 更新差异展示
     */
    public void updateDiff(YamlDiffResult result) {
        tableModel.setRowCount(0);
        summaryLabel.setText(result.getSummary());

        List<DiffEntry> diffs = result.getDiffs();
        for (DiffEntry entry : diffs) {
            tableModel.addRow(new Object[]{
                    entry.getPath(),
                    entry.getDiffType().name(),
                    entry.getLeftValue() != null ? entry.getLeftValue() : "",
                    entry.getRightValue() != null ? entry.getRightValue() : ""
            });
        }
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * 自定义单元格渲染器 — 不同差异类型用不同颜色
     */
    private static class DiffCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                String type = (String) table.getValueAt(row, 1);
                switch (type) {
                    case "ADDED":
                        c.setBackground(new Color(220, 255, 220)); // 浅绿
                        break;
                    case "REMOVED":
                        c.setBackground(new Color(255, 220, 220)); // 浅红
                        break;
                    case "MODIFIED":
                        c.setBackground(new Color(255, 255, 200)); // 浅黄
                        break;
                    default:
                        c.setBackground(Color.WHITE);
                }
            }

            return c;
        }
    }
}
