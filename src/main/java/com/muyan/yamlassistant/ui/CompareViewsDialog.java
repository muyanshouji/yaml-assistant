package com.muyan.yamlassistant.ui;

import com.muyan.yamlassistant.workspace.YamlViewState;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class CompareViewsDialog extends JDialog {

    private final JComboBox<YamlViewState> leftViewComboBox;
    private final JComboBox<YamlViewState> rightViewComboBox;
    private boolean confirmed;

    public CompareViewsDialog(Window owner, List<YamlViewState> views) {
        super(owner, "Compare Views", ModalityType.APPLICATION_MODAL);

        leftViewComboBox = new JComboBox<>(views.toArray(new YamlViewState[0]));
        rightViewComboBox = new JComboBox<>(views.toArray(new YamlViewState[0]));
        confirmed = false;

        if (views.size() > 1) {
            rightViewComboBox.setSelectedIndex(1);
        }

        Dimension comboSize = new Dimension(220, leftViewComboBox.getPreferredSize().height);
        leftViewComboBox.setPreferredSize(comboSize);
        rightViewComboBox.setPreferredSize(comboSize);

        JPanel selectionPanel = new JPanel(new GridBagLayout());
        selectionPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 8, 10);
        selectionPanel.add(new JLabel("Left View"), gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(0, 0, 8, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        selectionPanel.add(leftViewComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 10);
        selectionPanel.add(new JLabel("Right View"), gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        selectionPanel.add(rightViewComboBox, gbc);

        JButton compareButton = new JButton("Compare");
        compareButton.addActionListener(event -> {
            confirmed = true;
            dispose();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(event -> {
            confirmed = false;
            dispose();
        });

        JLabel descriptionLabel = new JLabel("Choose two saved views to open in IntelliJ Diff.");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(compareButton);
        buttonPanel.add(cancelButton);

        JPanel contentPanel = new JPanel(new BorderLayout(0, 12));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        applyTheme(contentPanel, descriptionLabel);
        contentPanel.add(descriptionLabel, BorderLayout.NORTH);
        contentPanel.add(selectionPanel, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(contentPanel);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(compareButton);
        pack();
        setLocationRelativeTo(owner);
    }

    private void applyTheme(JPanel contentPanel, JLabel descriptionLabel) {
        Color background = UIManager.getColor("Panel.background");
        contentPanel.setBackground(background);

        if (background != null && background.getRed() < 128) {
            Color fieldBackground = new Color(0x2F3141);
            Color foreground = new Color(0xE6E8EF);
            descriptionLabel.setForeground(new Color(0xAEB6C9));
            leftViewComboBox.setBackground(fieldBackground);
            leftViewComboBox.setForeground(foreground);
            rightViewComboBox.setBackground(fieldBackground);
            rightViewComboBox.setForeground(foreground);
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getSelectedLeftViewId() {
        YamlViewState view = (YamlViewState) leftViewComboBox.getSelectedItem();
        return view != null ? view.getId() : null;
    }

    public String getSelectedRightViewId() {
        YamlViewState view = (YamlViewState) rightViewComboBox.getSelectedItem();
        return view != null ? view.getId() : null;
    }
}
