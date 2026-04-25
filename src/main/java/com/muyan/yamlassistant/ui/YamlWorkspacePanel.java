package com.muyan.yamlassistant.ui;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.DumbAwareAction;
import com.muyan.yamlassistant.services.YamlFormatterService;
import com.muyan.yamlassistant.services.YamlParserService;
import com.muyan.yamlassistant.workspace.YamlViewState;
import com.muyan.yamlassistant.workspace.YamlWorkspaceStateService;
import com.intellij.ui.components.JBTabbedPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class YamlWorkspacePanel {

    private static final Color SHELL_BACKGROUND = new Color(0x35384A);
    private static final Color VIEW_BAR_BACKGROUND = new Color(0x323649);
    private static final Color VIEW_TAB_BACKGROUND = new Color(0x3A3F55);
    private static final Color VIEW_TAB_BACKGROUND_SELECTED = new Color(0x404762);
    private static final Color TEXT_PRIMARY = new Color(0xD6D9E3);
    private static final Color TEXT_SECONDARY = new Color(0xAEB6C9);
    private static final Color TAB_SELECTION = new Color(0x4FA3FF);
    private static final Color VIEW_TAB_SEPARATOR = new Color(0x4B5067);
    private static final Color ACTION_BUTTON_BACKGROUND = new Color(0x373C52);
    private static final Color ACTION_BUTTON_BACKGROUND_HOVER = new Color(0x434965);
    private static final int VIEW_TAB_HEIGHT = 24;
    private static final int VIEW_TAB_MIN_WIDTH = 60;
    private static final int VIEW_TAB_SCROLL_GUTTER = 3;
    private static final int VIEW_TAB_SCROLLBAR_HEIGHT = 8;
    private static final float VIEW_TAB_FONT_SIZE = 11f;

    private final JPanel mainPanel;
    private final JPanel topBarPanel;
    private final JPanel workspaceHeaderPanel;
    private final JPanel tabsRowPanel;
    private final JLabel workspaceTitleLabel;
    private final JPanel viewTabsPanel;
    private final JScrollPane viewTabsScrollPane;
    private final JPanel actionButtonsPanel;
    private final JButton addViewButton;
    private final JTabbedPane tabbedPane;
    private final YamlWorkspaceStateService stateService;
    private final YamlParserService parserService;
    private final YamlFormatterService formatterService;
    private final Project project;
    private final CompareDialogHandler compareDialogHandler;
    private final ErrorNotifier errorNotifier;
    private final NativeDiffLauncher nativeDiffLauncher;
    private final WorkspaceToolbarAction newViewAction;
    private final List<WorkspaceToolbarAction> workspaceActions;
    private final Map<Component, TabMetadata> tabMetadata = new IdentityHashMap<>();

    public YamlWorkspacePanel(Project project, Runnable hideWorkspaceHandler) {
        mainPanel = new JPanel(new BorderLayout());
        topBarPanel = new JPanel();
        workspaceHeaderPanel = new JPanel();
        tabsRowPanel = new JPanel();
        workspaceTitleLabel = new JLabel("Workspace");
        viewTabsPanel = new JPanel();
        viewTabsScrollPane = new JScrollPane(
                viewTabsPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        actionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        tabbedPane = new JBTabbedPane();
        this.project = project;
        this.stateService = YamlWorkspaceStateService.getInstance(project);
        this.parserService = new YamlParserService();
        this.formatterService = new YamlFormatterService();
        this.compareDialogHandler = (views, parent) -> showCompareDialog(parent, views);
        this.errorNotifier = message -> JOptionPane.showMessageDialog(
                mainPanel,
                message,
                "Compare Views",
                JOptionPane.ERROR_MESSAGE
        );
        this.nativeDiffLauncher = (title, leftName, rightName, leftText, rightText) -> {
            DiffContentFactory factory = DiffContentFactory.getInstance();
            SimpleDiffRequest request = new SimpleDiffRequest(
                    title,
                    factory.create(project, leftText, YAMLFileType.YML),
                    factory.create(project, rightText, YAMLFileType.YML),
                    leftName,
                    rightName
            );
            DiffManager.getInstance().showDiff(project, request);
        };
        this.newViewAction = new WorkspaceToolbarAction("New View", AllIcons.General.Add, this::createNewView);
        this.workspaceActions = createWorkspaceActions();
        addViewButton = createAddViewButton(newViewAction);
        initializeUi();
    }

    YamlWorkspacePanel(YamlWorkspaceStateService stateService,
                       YamlParserService parserService,
                       YamlFormatterService formatterService,
                       CompareDialogHandler compareDialogHandler,
                       ErrorNotifier errorNotifier,
                       NativeDiffLauncher nativeDiffLauncher,
                       Runnable hideWorkspaceHandler) {
        this.stateService = stateService;
        this.parserService = parserService;
        this.formatterService = formatterService;
        this.compareDialogHandler = compareDialogHandler;
        this.errorNotifier = errorNotifier;
        this.nativeDiffLauncher = nativeDiffLauncher;
        this.project = null;
        this.newViewAction = new WorkspaceToolbarAction("New View", AllIcons.General.Add, this::createNewView);
        this.workspaceActions = createWorkspaceActions();

        mainPanel = new JPanel(new BorderLayout());
        topBarPanel = new JPanel();
        workspaceHeaderPanel = new JPanel();
        tabsRowPanel = new JPanel();
        workspaceTitleLabel = new JLabel("Workspace");
        viewTabsPanel = new JPanel();
        viewTabsScrollPane = new JScrollPane(
                viewTabsPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        actionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        tabbedPane = new JBTabbedPane();
        addViewButton = createAddViewButton(newViewAction);
        initializeUi();
    }

    private void initializeUi() {
        applyWorkspaceTheme();
        buildTopBar();

        mainPanel.add(topBarPanel, BorderLayout.NORTH);
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        tabbedPane.addChangeListener(event -> updateViewTabSelection());

        rebuildViewTabs(null);
    }

    private void applyWorkspaceTheme() {
        int tabsScrollHeight = VIEW_TAB_HEIGHT
                + VIEW_TAB_SCROLL_GUTTER
                + VIEW_TAB_SCROLLBAR_HEIGHT;

        mainPanel.setBackground(SHELL_BACKGROUND);
        topBarPanel.setBackground(VIEW_BAR_BACKGROUND);
        topBarPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, VIEW_TAB_SEPARATOR),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        workspaceHeaderPanel.setBackground(VIEW_BAR_BACKGROUND);
        tabsRowPanel.setBackground(VIEW_BAR_BACKGROUND);
        workspaceTitleLabel.setForeground(TEXT_PRIMARY);
        workspaceTitleLabel.setFont(workspaceTitleLabel.getFont().deriveFont(Font.BOLD, 12f));
        viewTabsPanel.setBackground(VIEW_BAR_BACKGROUND);
        viewTabsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, VIEW_TAB_SCROLL_GUTTER, 0));
        viewTabsPanel.setLayout(new BoxLayout(viewTabsPanel, BoxLayout.X_AXIS));
        viewTabsScrollPane.setBackground(VIEW_BAR_BACKGROUND);
        viewTabsScrollPane.setBorder(BorderFactory.createEmptyBorder());
        viewTabsScrollPane.setOpaque(false);
        viewTabsScrollPane.getViewport().setOpaque(false);
        viewTabsScrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, VIEW_TAB_SCROLLBAR_HEIGHT));
        viewTabsScrollPane.getHorizontalScrollBar().setBorder(BorderFactory.createEmptyBorder());
        viewTabsScrollPane.setPreferredSize(new Dimension(240, tabsScrollHeight));
        viewTabsScrollPane.setMinimumSize(new Dimension(120, tabsScrollHeight));
        viewTabsScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, tabsScrollHeight));
        actionButtonsPanel.setBackground(VIEW_BAR_BACKGROUND);
        tabbedPane.setBackground(SHELL_BACKGROUND);
        tabbedPane.setForeground(TEXT_PRIMARY);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder());
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setUI(new HiddenTabsUI());
    }

    private void buildTopBar() {
        topBarPanel.setLayout(new BoxLayout(topBarPanel, BoxLayout.Y_AXIS));
        workspaceHeaderPanel.setLayout(new BoxLayout(workspaceHeaderPanel, BoxLayout.X_AXIS));
        tabsRowPanel.setLayout(new BoxLayout(tabsRowPanel, BoxLayout.X_AXIS));

        workspaceHeaderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabsRowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        workspaceTitleLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        workspaceHeaderPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        tabsRowPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        viewTabsScrollPane.setAlignmentY(Component.CENTER_ALIGNMENT);
        actionButtonsPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        addViewButton.setAlignmentY(Component.CENTER_ALIGNMENT);

        populateActionButtons();

        workspaceHeaderPanel.add(workspaceTitleLabel);
        workspaceHeaderPanel.add(Box.createHorizontalStrut(10));
        workspaceHeaderPanel.add(actionButtonsPanel);
        workspaceHeaderPanel.add(Box.createHorizontalGlue());

        tabsRowPanel.add(viewTabsScrollPane);
        tabsRowPanel.add(Box.createHorizontalStrut(6));
        tabsRowPanel.add(addViewButton);
        tabsRowPanel.add(Box.createHorizontalGlue());

        topBarPanel.add(workspaceHeaderPanel);
        topBarPanel.add(Box.createVerticalStrut(4));
        topBarPanel.add(tabsRowPanel);
    }

    private void populateActionButtons() {
        actionButtonsPanel.removeAll();
        for (WorkspaceToolbarAction action : workspaceActions) {
            actionButtonsPanel.add(createActionButton(action));
        }
    }

    private JButton createActionButton(WorkspaceToolbarAction action) {
        JButton button = new JButton(action.getTemplatePresentation().getIcon());
        button.setToolTipText(action.getTemplatePresentation().getText());
        button.setFocusable(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setBackground(ACTION_BUTTON_BACKGROUND);
        button.setPreferredSize(new Dimension(26, 26));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(VIEW_TAB_SEPARATOR),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));
        button.addActionListener(event -> action.run());
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                button.setBackground(ACTION_BUTTON_BACKGROUND_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                button.setBackground(ACTION_BUTTON_BACKGROUND);
            }
        });
        return button;
    }

    private JButton createAddViewButton(WorkspaceToolbarAction action) {
        JButton button = createActionButton(action);
        button.setPreferredSize(new Dimension(VIEW_TAB_HEIGHT, VIEW_TAB_HEIGHT));
        return button;
    }

    private List<WorkspaceToolbarAction> createWorkspaceActions() {
        List<WorkspaceToolbarAction> actions = new ArrayList<>();
        actions.add(new WorkspaceToolbarAction("Compare", AllIcons.Actions.Diff, this::compareViews));
        actions.add(new WorkspaceToolbarAction("Format", AllIcons.Actions.ReformatCode, this::formatCurrentView));
        actions.add(new WorkspaceToolbarAction("Delete Current View", AllIcons.General.Remove, this::deleteCurrentView));
        return Collections.unmodifiableList(actions);
    }

    private static CompareSelection showCompareDialog(Component parent, List<YamlViewState> views) {
        Window owner = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
        CompareViewsDialog dialog = new CompareViewsDialog(owner, views);
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) {
            return null;
        }
        return new CompareSelection(dialog.getSelectedLeftViewId(), dialog.getSelectedRightViewId());
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    void createNewView() {
        stateService.createView("");
        rebuildViewTabs(stateService.getViews().size() - 1);
    }

    void compareViews() {
        List<YamlViewState> views = stateService.getViews();
        CompareSelection selection = compareDialogHandler.show(new ArrayList<>(views), mainPanel);
        if (selection == null) {
            return;
        }

        String validation = stateService.validateCompareSelection(
                selection.getLeftViewId(),
                selection.getRightViewId(),
                parserService
        );
        if (validation != null) {
            errorNotifier.show(validation);
            return;
        }

        YamlViewState leftView = stateService.getView(selection.getLeftViewId());
        YamlViewState rightView = stateService.getView(selection.getRightViewId());
        if (leftView == null || rightView == null) {
            errorNotifier.show("Selected view no longer exists.");
            return;
        }

        nativeDiffLauncher.show(
                leftView.getName() + " vs " + rightView.getName(),
                leftView.getName(),
                rightView.getName(),
                leftView.getContent(),
                rightView.getContent()
        );
    }

    void formatCurrentView() {
        YamlViewState currentView = getCurrentView();
        if (currentView == null) {
            return;
        }

        try {
            String formatted = formatterService.beautify(currentView.getContent());
            currentView.setContent(formatted);
            stateService.updateViewContent(currentView.getId(), formatted);
            rebuildViewTabs(tabbedPane.getSelectedIndex());
        } catch (Exception ex) {
            errorNotifier.show("Format failed: " + ex.getMessage());
        }
    }

    void deleteCurrentView() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex < 0) {
            return;
        }

        Component component = tabbedPane.getComponentAt(selectedIndex);
        TabMetadata metadata = tabMetadata.get(component);
        if (metadata == null) {
            return;
        }

        if (stateService.getViews().size() <= 1) {
            return;
        }

        stateService.deleteView(metadata.getViewId());
        rebuildViewTabs(tabbedPane.getTabCount() > 1 ? Math.max(0, selectedIndex - 1) : null);
    }

    JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    private void rebuildViewTabs(Integer preferredSelection) {
        tabbedPane.removeAll();
        viewTabsPanel.removeAll();
        tabMetadata.clear();

        int viewIndex = 0;
        for (YamlViewState view : stateService.getViews()) {
            String title = getDisplayName(view, viewIndex + 1);
            addViewTab(view, title);
            addViewButton(title, viewIndex);
            viewIndex++;
        }

        if (preferredSelection != null && preferredSelection < tabbedPane.getTabCount()) {
            tabbedPane.setSelectedIndex(preferredSelection);
        } else if (tabbedPane.getTabCount() > 0) {
            tabbedPane.setSelectedIndex(0);
        }

        updateViewTabSelection();
        viewTabsPanel.revalidate();
        viewTabsPanel.repaint();
    }

    private void addViewTab(YamlViewState view, String title) {
        YamlViewTabPanel viewTabPanel = new YamlViewTabPanel(
                view,
                project,
                parserService,
                content -> stateService.updateViewContent(view.getId(), content)
        );
        JPanel component = viewTabPanel.getMainPanel();
        tabbedPane.addTab(title, component);
        tabbedPane.setToolTipTextAt(tabbedPane.getTabCount() - 1, title);
        tabMetadata.put(component, TabMetadata.forView(view.getId()));
    }

    private String getDisplayName(YamlViewState view, int displayIndex) {
        String name = view.getName();
        if (name == null || name.isBlank()) {
            name = "View " + displayIndex;
            view.setName(name);
        }
        return name;
    }

    private void addViewButton(String title, int tabIndex) {
        JToggleButton button = new JToggleButton(title);
        button.setFocusable(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBackground(VIEW_TAB_BACKGROUND);
        button.setForeground(TEXT_SECONDARY);
        button.setFont(button.getFont().deriveFont(Font.PLAIN, VIEW_TAB_FONT_SIZE));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setPreferredSize(new Dimension(Math.max(VIEW_TAB_MIN_WIDTH, title.length() * 7 + 18), VIEW_TAB_HEIGHT));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 0, SHELL_BACKGROUND),
                        BorderFactory.createMatteBorder(0, 0, 0, 1, VIEW_TAB_SEPARATOR)
                ),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        button.addActionListener(event -> tabbedPane.setSelectedIndex(tabIndex));
        viewTabsPanel.add(button);
    }

    JPanel getTopBarPanel() {
        return topBarPanel;
    }

    JPanel getWorkspaceHeaderPanel() {
        return workspaceHeaderPanel;
    }

    JPanel getTabsRowPanel() {
        return tabsRowPanel;
    }

    JLabel getWorkspaceTitleLabel() {
        return workspaceTitleLabel;
    }

    JPanel getViewTabsPanel() {
        return viewTabsPanel;
    }

    JPanel getActionButtonsPanel() {
        return actionButtonsPanel;
    }

    JButton getAddViewButton() {
        return addViewButton;
    }

    JScrollPane getViewTabsScrollPane() {
        return viewTabsScrollPane;
    }

    private void updateViewTabSelection() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        Component[] components = viewTabsPanel.getComponents();
        for (int index = 0; index < components.length; index++) {
            if (!(components[index] instanceof JToggleButton button)) {
                continue;
            }

            boolean selected = index == selectedIndex;
            button.setSelected(selected);
            button.setBackground(selected ? VIEW_TAB_BACKGROUND_SELECTED : VIEW_TAB_BACKGROUND);
            button.setForeground(selected ? TEXT_PRIMARY : TEXT_SECONDARY);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 2, 0, selected ? TAB_SELECTION : SHELL_BACKGROUND),
                            BorderFactory.createMatteBorder(0, 0, 0, 1, VIEW_TAB_SEPARATOR)
                    ),
                    BorderFactory.createEmptyBorder(2, 8, 2, 8)
            ));
        }
    }

    private static final class HiddenTabsUI extends BasicTabbedPaneUI {
        @Override
        protected int calculateTabAreaHeight(int tabPlacement, int horizRunCount, int maxTabHeight) {
            return 0;
        }

        @Override
        protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
        }
    }

    private YamlViewState getCurrentView() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex < 0) {
            return null;
        }
        Component component = tabbedPane.getComponentAt(selectedIndex);
        TabMetadata metadata = tabMetadata.get(component);
        if (metadata == null) {
            return null;
        }
        return stateService.getView(metadata.getViewId());
    }

    static class CompareSelection {
        private final String leftViewId;
        private final String rightViewId;

        CompareSelection(String leftViewId, String rightViewId) {
            this.leftViewId = leftViewId;
            this.rightViewId = rightViewId;
        }

        String getLeftViewId() {
            return leftViewId;
        }

        String getRightViewId() {
            return rightViewId;
        }
    }

    @FunctionalInterface
    interface CompareDialogHandler {
        CompareSelection show(List<YamlViewState> views, Component parent);
    }

    @FunctionalInterface
    interface ErrorNotifier {
        void show(String message);
    }

    @FunctionalInterface
    interface NativeDiffLauncher {
        void show(String title, String leftName, String rightName, String leftText, String rightText);
    }

    private static final class WorkspaceToolbarAction extends DumbAwareAction {
        private final Runnable handler;

        private WorkspaceToolbarAction(String text, Icon icon, Runnable handler) {
            super(text, text, icon);
            this.handler = handler;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            handler.run();
        }

        private void run() {
            handler.run();
        }
    }

    private static class TabMetadata {
        private final String viewId;

        private TabMetadata(String viewId) {
            this.viewId = viewId;
        }

        static TabMetadata forView(String viewId) {
            return new TabMetadata(viewId);
        }

        String getViewId() {
            return viewId;
        }
    }
}
