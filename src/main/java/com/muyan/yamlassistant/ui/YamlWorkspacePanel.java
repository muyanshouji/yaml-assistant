package com.muyan.yamlassistant.ui;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
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
import javax.swing.border.Border;
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

    private static final int VIEW_TAB_HEIGHT = 24;
    private static final int VIEW_TAB_WIDTH = 88;
    private static final int VIEW_TAB_SCROLL_GUTTER = 0;
    private static final int VIEW_TAB_SCROLLBAR_HEIGHT = 0;
    private static final float VIEW_TAB_FONT_SIZE = 12f;
    private static final int CLOSE_BUTTON_SIZE = 14;
    private static final int ADD_BUTTON_SIZE = 24;
    private static final int ADD_ICON_SIZE = 10;

    private final JPanel mainPanel;
    private final JPanel topBarPanel;
    private final JPanel tabsRowPanel;
    private final JPanel viewTabsPanel;
    private final JScrollPane viewTabsScrollPane;
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
        tabsRowPanel = new JPanel();
        viewTabsPanel = new JPanel();
        viewTabsScrollPane = new JScrollPane(
                viewTabsPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
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
        tabsRowPanel = new JPanel();
        viewTabsPanel = new JPanel();
        viewTabsScrollPane = new JScrollPane(
                viewTabsPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
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
        Color shellBackground = getShellBackground();
        Color viewBarBackground = getViewBarBackground();

        mainPanel.setBackground(shellBackground);
        topBarPanel.setBackground(viewBarBackground);
        topBarPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        tabsRowPanel.setBackground(viewBarBackground);
        viewTabsPanel.setBackground(viewBarBackground);
        viewTabsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, VIEW_TAB_SCROLL_GUTTER, 0));
        viewTabsPanel.setLayout(new BoxLayout(viewTabsPanel, BoxLayout.X_AXIS));
        viewTabsScrollPane.setBackground(viewBarBackground);
        viewTabsScrollPane.setBorder(BorderFactory.createEmptyBorder());
        viewTabsScrollPane.setOpaque(false);
        viewTabsScrollPane.getViewport().setOpaque(false);
        viewTabsScrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, VIEW_TAB_SCROLLBAR_HEIGHT));
        viewTabsScrollPane.getHorizontalScrollBar().setBorder(BorderFactory.createEmptyBorder());
        viewTabsScrollPane.setPreferredSize(new Dimension(240, tabsScrollHeight));
        viewTabsScrollPane.setMinimumSize(new Dimension(120, tabsScrollHeight));
        viewTabsScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, tabsScrollHeight));
        tabbedPane.setBackground(shellBackground);
        tabbedPane.setForeground(getPrimaryTextColor());
        tabbedPane.setBorder(BorderFactory.createEmptyBorder());
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setUI(new HiddenTabsUI());
    }

    private void buildTopBar() {
        topBarPanel.setLayout(new BorderLayout());
        tabsRowPanel.setLayout(new BoxLayout(tabsRowPanel, BoxLayout.X_AXIS));

        tabsRowPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        viewTabsScrollPane.setAlignmentY(Component.CENTER_ALIGNMENT);
        addViewButton.setAlignmentY(Component.CENTER_ALIGNMENT);

        tabsRowPanel.add(viewTabsScrollPane);
        topBarPanel.add(tabsRowPanel, BorderLayout.CENTER);
    }

    private JButton createAddViewButton(WorkspaceToolbarAction action) {
        JButton button = new AddViewButton();
        button.setToolTipText(action.getTemplatePresentation().getText());
        button.setFocusable(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setBackground(getViewTabBackground());
        button.setForeground(getPrimaryTextColor());
        button.setPreferredSize(new Dimension(ADD_BUTTON_SIZE, VIEW_TAB_HEIGHT));
        button.setMinimumSize(new Dimension(ADD_BUTTON_SIZE, VIEW_TAB_HEIGHT));
        button.setMaximumSize(new Dimension(ADD_BUTTON_SIZE, VIEW_TAB_HEIGHT));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 0, getViewTabBackground()),
                        BorderFactory.createMatteBorder(0, 0, 0, 1, getViewTabSeparatorColor())
                ),
                BorderFactory.createEmptyBorder(0, 0, 2, 0)
        ));
        button.addActionListener(event -> action.run());
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                button.setBackground(getActionButtonHoverBackground());
            }

            @Override
            public void mouseExited(MouseEvent event) {
                button.setBackground(getViewTabBackground());
            }
        });
        return button;
    }

    private List<WorkspaceToolbarAction> createWorkspaceActions() {
        List<WorkspaceToolbarAction> actions = new ArrayList<>();
        actions.add(new WorkspaceToolbarAction("Compare", AllIcons.Actions.Diff, this::compareViews));
        actions.add(new WorkspaceToolbarAction("Format", AllIcons.Actions.ReformatCode, this::formatCurrentView));
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

        deleteView(metadata.getViewId(), selectedIndex);
    }

    private void deleteView(String viewId, int tabIndex) {
        if (stateService.getViews().size() <= 1) {
            return;
        }

        if (tabIndex == 0) {
            return;
        }

        int selectedIndex = tabbedPane.getSelectedIndex();
        stateService.deleteView(viewId);

        int preferredSelection = selectedIndex;
        if (tabIndex < selectedIndex) {
            preferredSelection = selectedIndex - 1;
        } else if (tabIndex == selectedIndex) {
            preferredSelection = Math.max(0, selectedIndex - 1);
        }

        rebuildViewTabs(preferredSelection);
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
            addViewChip(view, title, viewIndex);
            viewIndex++;
        }
        viewTabsPanel.add(addViewButton);

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

    private void addViewChip(YamlViewState view, String title, int tabIndex) {
        boolean closable = tabIndex > 0 && stateService.getViews().size() > 1;
        viewTabsPanel.add(new ViewTabChip(view.getId(), title, tabIndex, closable));
    }

    JPanel getTopBarPanel() {
        return topBarPanel;
    }

    JPanel getTabsRowPanel() {
        return tabsRowPanel;
    }

    JPanel getViewTabsPanel() {
        return viewTabsPanel;
    }

    JButton getAddViewButton() {
        return addViewButton;
    }

    JScrollPane getViewTabsScrollPane() {
        return viewTabsScrollPane;
    }

    AnAction[] getToolWindowTitleActions() {
        return workspaceActions.toArray(AnAction[]::new);
    }

    private void updateViewTabSelection() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        Component[] components = viewTabsPanel.getComponents();
        int tabIndex = 0;
        for (Component component : components) {
            if (!(component instanceof ViewTabChip chip)) {
                continue;
            }

            chip.applySelection(tabIndex == selectedIndex);
            tabIndex++;
        }

        addViewButton.setBackground(getViewTabBackground());
    }

    private Color getShellBackground() {
        Color background = UIManager.getColor("EditorPane.background");
        if (background != null) {
            return background;
        }

        background = UIManager.getColor("Panel.background");
        return background != null ? background : new Color(0x35384A);
    }

    private Color getViewBarBackground() {
        Color background = UIManager.getColor("ToolWindow.Header.background");
        if (background != null) {
            return background;
        }

        return getShellBackground();
    }

    private Color getViewTabBackground() {
        return getViewBarBackground();
    }

    private Color getSelectedViewTabBackground() {
        return mix(getViewBarBackground(), getShellBackground(), 0.82f);
    }

    private Color getPrimaryTextColor() {
        Color color = UIManager.getColor("Label.foreground");
        return color != null ? color : new Color(0xD6D9E3);
    }

    private Color getSecondaryTextColor() {
        return withAlpha(getPrimaryTextColor(), 0.72f);
    }

    private Color getSelectionColor() {
        Color color = UIManager.getColor("TabbedPane.underlineColor");
        if (color != null) {
            return color;
        }

        color = UIManager.getColor("Focus.color");
        return color != null ? color : new Color(0x4FA3FF);
    }

    private Color getViewTabSeparatorColor() {
        Color color = UIManager.getColor("Component.borderColor");
        if (color != null) {
            return withAlpha(color, 0.32f);
        }

        color = UIManager.getColor("Separator.foreground");
        if (color != null) {
            return withAlpha(color, 0.32f);
        }

        return new Color(0x4B5067, true);
    }

    private Color getActionButtonHoverBackground() {
        return mix(getViewTabBackground(), getShellBackground(), 0.25f);
    }

    private Color getAddButtonForeground() {
        return withAlpha(getPrimaryTextColor(), 0.88f);
    }

    private Color getCloseButtonForeground() {
        return getSecondaryTextColor();
    }

    private Color getCloseButtonHoverForeground() {
        return getPrimaryTextColor();
    }

    private Color getCloseButtonHoverBackground() {
        return withAlpha(getPrimaryTextColor(), 0.14f);
    }

    private static Color mix(Color first, Color second, float secondWeight) {
        float clampedWeight = Math.max(0f, Math.min(1f, secondWeight));
        float firstWeight = 1f - clampedWeight;
        return new Color(
                Math.round(first.getRed() * firstWeight + second.getRed() * clampedWeight),
                Math.round(first.getGreen() * firstWeight + second.getGreen() * clampedWeight),
                Math.round(first.getBlue() * firstWeight + second.getBlue() * clampedWeight),
                Math.round(first.getAlpha() * firstWeight + second.getAlpha() * clampedWeight)
        );
    }

    private static Color withAlpha(Color color, float alphaFactor) {
        return new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                Math.round(Math.max(0f, Math.min(1f, alphaFactor)) * 255)
        );
    }

    private final class ViewTabChip extends JPanel {
        private final JLabel titleLabel;
        private final JButton closeButton;

        private ViewTabChip(String viewId, String title, int tabIndex, boolean closable) {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setOpaque(true);
            setFocusable(false);
            setBackground(getViewTabBackground());
            setBorder(createTabBorder(false));
            setPreferredSize(new Dimension(VIEW_TAB_WIDTH, VIEW_TAB_HEIGHT));
            setMinimumSize(new Dimension(VIEW_TAB_WIDTH, VIEW_TAB_HEIGHT));
            setMaximumSize(new Dimension(VIEW_TAB_WIDTH, VIEW_TAB_HEIGHT));

            titleLabel = new JLabel(title);
            titleLabel.setForeground(getSecondaryTextColor());
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.PLAIN, VIEW_TAB_FONT_SIZE));
            titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            titleLabel.setAlignmentY(Component.CENTER_ALIGNMENT);

            add(Box.createHorizontalStrut(6));
            add(titleLabel);
            add(Box.createHorizontalGlue());

            if (closable) {
                add(Box.createHorizontalStrut(4));
                closeButton = new CloseTabButton(viewId, tabIndex);
                add(closeButton);
            } else {
                closeButton = null;
            }

            add(Box.createHorizontalStrut(6));

            MouseAdapter selectListener = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    tabbedPane.setSelectedIndex(tabIndex);
                }
            };
            addMouseListener(selectListener);
            titleLabel.addMouseListener(selectListener);
        }

        private void applySelection(boolean selected) {
            setBackground(selected ? getSelectedViewTabBackground() : getViewTabBackground());
            setBorder(createTabBorder(selected));
            titleLabel.setForeground(selected ? getPrimaryTextColor() : getSecondaryTextColor());
        }

        private Border createTabBorder(boolean selected) {
            return BorderFactory.createCompoundBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 2, 0, selected ? getSelectionColor() : getViewTabBackground()),
                            BorderFactory.createMatteBorder(0, 0, 0, 1, getViewTabSeparatorColor())
                    ),
                    BorderFactory.createEmptyBorder(2, 0, 2, 0)
            );
        }
    }

    private final class CloseTabButton extends JButton {
        private boolean hovered;

        private CloseTabButton(String viewId, int tabIndex) {
            super("x");
            setToolTipText("Close View");
            setFocusable(false);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setBorder(BorderFactory.createEmptyBorder());
            setMargin(new Insets(0, 0, 0, 0));
            setForeground(getCloseButtonForeground());
            setFont(getFont().deriveFont(Font.BOLD, 10f));
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
            setPreferredSize(new Dimension(CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE));
            setMinimumSize(new Dimension(CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE));
            setMaximumSize(new Dimension(CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE));
            setAlignmentY(Component.CENTER_ALIGNMENT);
            addActionListener(event -> deleteView(viewId, tabIndex));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    hovered = true;
                    setForeground(getCloseButtonHoverForeground());
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hovered = false;
                    setForeground(getCloseButtonForeground());
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (hovered) {
                g2.setColor(getCloseButtonHoverBackground());
                g2.fillOval(0, 0, getWidth(), getHeight());
            }
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    private final class AddViewButton extends JButton {
        private boolean hovered;

        private AddViewButton() {
            setText(null);
            setMargin(new Insets(0, 0, 0, 0));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    hovered = true;
                    setBackground(getActionButtonHoverBackground());
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hovered = false;
                    setBackground(getViewTabBackground());
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());

            int strokeWidth = 2;
            int iconHalf = ADD_ICON_SIZE / 2;
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            g2.setColor(getAddButtonForeground());
            g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(centerX - iconHalf, centerY, centerX + iconHalf, centerY);
            g2.drawLine(centerX, centerY - iconHalf, centerX, centerY + iconHalf);

            if (hovered) {
                g2.setColor(withAlpha(getPrimaryTextColor(), 0.05f));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.dispose();
        }
    }

    private static final class HiddenTabsUI extends BasicTabbedPaneUI {
        @Override
        protected int calculateTabAreaHeight(int tabPlacement, int horizRunCount, int maxTabHeight) {
            return 0;
        }

        @Override
        protected Insets getContentBorderInsets(int tabPlacement) {
            return new Insets(0, 0, 0, 0);
        }

        @Override
        protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
        }

        @Override
        protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
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
