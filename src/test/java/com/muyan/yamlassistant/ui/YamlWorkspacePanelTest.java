package com.muyan.yamlassistant.ui;

import com.muyan.yamlassistant.diff.YamlDiffService;
import com.muyan.yamlassistant.services.YamlFormatterService;
import com.muyan.yamlassistant.services.YamlParserService;
import com.muyan.yamlassistant.workspace.YamlViewState;
import com.muyan.yamlassistant.workspace.YamlWorkspaceStateService;
import com.muyan.yamlassistant.workspace.WorkspaceContentType;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class YamlWorkspacePanelTest {

    private final YamlParserService parserService = new YamlParserService();
    private final YamlDiffService diffService = new YamlDiffService();
    private final YamlFormatterService formatterService = new YamlFormatterService();

    @Test
    public void testViewTabPanelShowsReadyForEmptyContent() throws Exception {
        YamlViewState view = new YamlViewState("view-1", "View 1", "");
        List<String> updates = new ArrayList<>();

        YamlViewTabPanel panel = runOnEdt(() -> new YamlViewTabPanel(view, null, parserService, updates::add));

        assertEquals("Ready", panel.getStatusLabel().getText());
        assertEquals(new Color(0x80889A), panel.getStatusIndicator().getBackground());
        assertNotNull(panel.getEditorComponent());
    }

    @Test
    public void testViewTabPanelUpdatesContentAndValidationStatus() throws Exception {
        YamlViewState view = new YamlViewState("view-1", "View 1", "name: test");
        List<String> updates = new ArrayList<>();

        YamlViewTabPanel panel = runOnEdt(() -> new YamlViewTabPanel(view, null, parserService, updates::add));
        runOnEdt(() -> panel.setContent("invalid: [unclosed"));

        assertEquals("invalid: [unclosed", view.getContent());
        assertEquals("invalid: [unclosed", updates.get(updates.size() - 1));
        assertEquals("YAML error", panel.getStatusLabel().getText());
        assertEquals(new Color(0xE06C75), panel.getStatusIndicator().getBackground());

        runOnEdt(() -> panel.setContent("name: fixed"));

        assertEquals("name: fixed", view.getContent());
        assertEquals("Parsed successfully", panel.getStatusLabel().getText());
        assertEquals(new Color(0x7BC275), panel.getStatusIndicator().getBackground());
    }

    @Test
    public void testViewTabPanelSupportsPropertiesValidation() throws Exception {
        YamlViewState view = new YamlViewState("view-1", "View", "server.port=8080", WorkspaceContentType.PROPERTIES);
        List<String> updates = new ArrayList<>();

        YamlViewTabPanel panel = runOnEdt(() -> new YamlViewTabPanel(view, null, parserService, updates::add));
        runOnEdt(() -> panel.setContent("server.port=8080\ninvalid\\u00ZZ"));

        assertEquals("Properties error", panel.getStatusLabel().getText());
        assertEquals(new Color(0xE06C75), panel.getStatusIndicator().getBackground());
    }

    @Test
    public void testViewTabPanelRejectsYamlLikeContentInPropertiesMode() throws Exception {
        YamlViewState view = new YamlViewState("view-1", "View", "server.port=8080", WorkspaceContentType.PROPERTIES);
        List<String> updates = new ArrayList<>();

        YamlViewTabPanel panel = runOnEdt(() -> new YamlViewTabPanel(view, null, parserService, updates::add));
        runOnEdt(() -> panel.setContent("spring:\n  application:\n    name: shop\n"));

        assertEquals("Properties error", panel.getStatusLabel().getText());
        assertEquals(new Color(0xE06C75), panel.getStatusIndicator().getBackground());
    }

    @Test
    public void testViewTabPanelRejectsPropertiesWithoutEqualsSeparator() throws Exception {
        YamlViewState view = new YamlViewState("view-1", "View", "server.port=8080", WorkspaceContentType.PROPERTIES);
        List<String> updates = new ArrayList<>();

        YamlViewTabPanel panel = runOnEdt(() -> new YamlViewTabPanel(view, null, parserService, updates::add));
        runOnEdt(() -> panel.setContent("spring.datasource.password"));

        assertEquals("Properties error", panel.getStatusLabel().getText());
        assertEquals(new Color(0xE06C75), panel.getStatusIndicator().getBackground());
    }

    @Test
    public void testViewTabPanelAcceptsMavenStyleYamlPlaceholders() throws Exception {
        YamlViewState view = new YamlViewState("view-1", "View", "spring:\n  profiles:\n    active: @profileActive@\n");
        List<String> updates = new ArrayList<>();

        YamlViewTabPanel panel = runOnEdt(() -> new YamlViewTabPanel(view, null, parserService, updates::add));

        assertEquals("Parsed successfully", panel.getStatusLabel().getText());
        assertEquals(new Color(0x7BC275), panel.getStatusIndicator().getBackground());
    }

    @Test
    public void testViewTabPanelCanHideLineNumbers() throws Exception {
        YamlViewState view = new YamlViewState("view-1", "View", "name: test");
        List<String> updates = new ArrayList<>();

        YamlViewTabPanel panel = runOnEdt(() -> new YamlViewTabPanel(view, null, parserService, false, updates::add));

        assertFalse(panel.isLineNumbersShown());
    }

    @Test
    public void testViewTabPanelSupportsJsonValidation() throws Exception {
        YamlViewState view = new YamlViewState("view-1", "View", "{\"name\":\"shop\"}", WorkspaceContentType.JSON);
        List<String> updates = new ArrayList<>();

        YamlViewTabPanel panel = runOnEdt(() -> new YamlViewTabPanel(view, null, parserService, updates::add));
        runOnEdt(() -> panel.setContent("{\"name\": }"));

        assertEquals("JSON error", panel.getStatusLabel().getText());
        assertEquals(new Color(0xE06C75), panel.getStatusIndicator().getBackground());
    }

    @Test
    public void testViewTabPanelAcceptsValidJson() throws Exception {
        YamlViewState view = new YamlViewState("view-1", "View", "{\"name\":\"shop\"}", WorkspaceContentType.JSON);
        List<String> updates = new ArrayList<>();

        YamlViewTabPanel panel = runOnEdt(() -> new YamlViewTabPanel(view, null, parserService, updates::add));

        assertEquals("Parsed successfully", panel.getStatusLabel().getText());
        assertEquals(new Color(0x7BC275), panel.getStatusIndicator().getBackground());
    }

    @Test
    public void testWorkspacePanelLoadsPersistedViewsAndCreatesNewView() throws Exception {
        YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
        stateService.createView("alpha: 1");
        stateService.createView("beta: 2");

        YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
                stateService,
                parserService,
                formatterService,
                (views, parent) -> null,
                message -> {
                },
                (title, leftName, rightName, leftText, rightText) -> {
                },
                () -> {
                }
        ));

        assertEquals(2, panel.getTabbedPane().getTabCount());
        assertEquals("View", panel.getTabbedPane().getTitleAt(0));
        assertEquals("View 1", panel.getTabbedPane().getTitleAt(1));

        runOnEdt(panel::createNewView);

        assertEquals(3, stateService.getViews().size());
        assertEquals(3, panel.getTabbedPane().getTabCount());
        assertEquals("View 2", panel.getTabbedPane().getTitleAt(2));
        assertEquals(2, panel.getTabbedPane().getSelectedIndex());
    }

    @Test
    public void testWorkspacePanelExposesSingleTopBarNewViewAction() throws Exception {
        YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
        stateService.ensureAtLeastOneView();

        YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
                stateService,
                parserService,
                formatterService,
                (views, parent) -> null,
                message -> {
                },
                (title, leftName, rightName, leftText, rightText) -> {
                },
                () -> {
                }
        ));

        assertNotNull(panel.getAddViewButton());
        assertEquals("New View", panel.getAddViewButton().getToolTipText());
        assertSame(panel.getViewTabsPanel(), panel.getAddViewButton().getParent());
    }

    @Test
    public void testWorkspacePanelCompareUsesNativeDiffInsteadOfCreatingTab() throws Exception {
        YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
        YamlViewState left = stateService.createView("name: before");
        YamlViewState right = stateService.createView("name: after");
        List<String> diffTitles = new ArrayList<>();

        YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
                stateService,
                parserService,
                formatterService,
                (views, parent) -> new YamlWorkspacePanel.CompareSelection(left.getId(), right.getId()),
                message -> {
                },
                (title, leftName, rightName, leftText, rightText) -> diffTitles.add(title),
                () -> {
                }
        ));

        runOnEdt(panel::compareViews);

        assertEquals(2, panel.getTabbedPane().getTabCount());
        assertEquals(1, diffTitles.size());
        assertEquals("View vs View 1", diffTitles.get(0));
    }

    @Test
    public void testCompareDoesNotMutateWorkspaceTabs() throws Exception {
        YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
        YamlViewState left = stateService.createView("name: before");
        YamlViewState right = stateService.createView("name: after");

        YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
                stateService,
                parserService,
                formatterService,
                (views, parent) -> new YamlWorkspacePanel.CompareSelection(left.getId(), right.getId()),
                message -> {
                },
                (title, leftName, rightName, leftText, rightText) -> {
                },
                () -> {
                }
        ));

        int beforeTabs = panel.getTabbedPane().getTabCount();
        runOnEdt(panel::compareViews);

        assertEquals(beforeTabs, panel.getTabbedPane().getTabCount());
    }

    @Test
    public void testWorkspacePanelShowsErrorForInvalidCompareSelection() throws Exception {
        YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
        YamlViewState left = stateService.createView("invalid: [unclosed");
        YamlViewState right = stateService.createView("name: after");
        List<String> errors = new ArrayList<>();

        YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
                stateService,
                parserService,
                formatterService,
                (views, parent) -> new YamlWorkspacePanel.CompareSelection(left.getId(), right.getId()),
                errors::add,
                (title, leftName, rightName, leftText, rightText) -> {
                },
                () -> {
                }
        ));

        runOnEdt(panel::compareViews);

        assertEquals(2, panel.getTabbedPane().getTabCount());
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).startsWith("Left view is invalid:"));
    }

    @Test
    public void testWorkspacePanelFormatsCurrentViewContent() throws Exception {
        YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
        YamlViewState view = stateService.createView("name: test\nitems: [1,2]");

        YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
                stateService,
                parserService,
                formatterService,
                (views, parent) -> null,
                message -> {
                },
                (title, leftName, rightName, leftText, rightText) -> {
                },
                () -> {
                }
        ));

        runOnEdt(panel::formatCurrentView);

        assertTrue(stateService.getView(view.getId()).getContent().contains("name: test"));
        assertTrue(stateService.getView(view.getId()).getContent().contains("items:"));
    }

    @Test
    public void testWorkspacePanelFormatsCurrentPropertiesViewContent() throws Exception {
        YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
        stateService.setSelectedContentType(WorkspaceContentType.PROPERTIES);
        YamlViewState view = stateService.getViews(WorkspaceContentType.PROPERTIES).get(0);
        stateService.updateViewContent(view.getId(), " server.port : 8080\nspring.application.name    shop\n");

        YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
                stateService,
                parserService,
                formatterService,
                (views, parent) -> null,
                message -> {
                },
                (title, leftName, rightName, leftText, rightText) -> {
                },
                () -> {
                }
        ));

        runOnEdt(panel::formatCurrentView);

        assertEquals("server.port=8080\nspring.application.name=shop\n", stateService.getView(view.getId()).getContent());
    }

    @Test
    public void testWorkspacePanelFormatsCurrentJsonViewContent() throws Exception {
        YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
        stateService.setSelectedContentType(WorkspaceContentType.JSON);
        YamlViewState view = stateService.getViews(WorkspaceContentType.JSON).get(0);
        stateService.updateViewContent(view.getId(), "{\"name\":\"shop\",\"port\":8080}");

        YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
                stateService,
                parserService,
                formatterService,
                (views, parent) -> null,
                message -> {
                },
                (title, leftName, rightName, leftText, rightText) -> {
                },
                () -> {
                }
        ));

        runOnEdt(panel::formatCurrentView);

        String content = stateService.getView(view.getId()).getContent();
        assertTrue(content.contains("\"name\": \"shop\""));
        assertTrue(content.contains("\n  \"port\": 8080\n"));
    }

    @Test
    public void testWorkspacePanelBuildsJsonTypeTab() throws Exception {
        YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
        stateService.ensureAtLeastOneView();

        YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
                stateService,
                parserService,
                formatterService,
                (views, parent) -> null,
                message -> {
                },
                (title, leftName, rightName, leftText, rightText) -> {
                },
                () -> {
                }
        ));

        assertNotNull(findDescendant(panel.getTypeTabsPanel(), JLabel.class, "JSON"));
    }

    @Test
    public void testWorkspacePanelDeletingLastViewDoesNothing() throws Exception {
        YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
        stateService.ensureAtLeastOneView();

        YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
                stateService,
                parserService,
                formatterService,
                (views, parent) -> null,
                message -> {
                },
                (title, leftName, rightName, leftText, rightText) -> {
                },
                () -> {
                }
        ));

        runOnEdt(panel::deleteCurrentView);

        assertEquals(1, stateService.getViews().size());
        assertEquals(1, panel.getTabbedPane().getTabCount());
        assertEquals("View", panel.getTabbedPane().getTitleAt(0));
    }

    @Test
    public void testWorkspacePanelDeleteRemovesCurrentViewWhenMoreThanOneExists() throws Exception {
        YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
        stateService.createView("alpha: 1");
        stateService.createView("beta: 2");

        YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
                stateService,
                parserService,
                formatterService,
                (views, parent) -> null,
                message -> {
                },
                (title, leftName, rightName, leftText, rightText) -> {
                },
                () -> {
                }
        ));

        runOnEdt(() -> panel.getTabbedPane().setSelectedIndex(1));
        runOnEdt(panel::deleteCurrentView);

        assertEquals(1, stateService.getViews().size());
        assertEquals(1, panel.getTabbedPane().getTabCount());
        assertEquals("View", panel.getTabbedPane().getTitleAt(0));
    }

    @Test
    public void testWorkspacePanelDoesNotDeleteFirstFixedViewWhenMultipleTabsExist() throws Exception {
        YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
        stateService.createView("alpha: 1");
        stateService.createView("beta: 2");

        YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
                stateService,
                parserService,
                formatterService,
                (views, parent) -> null,
                message -> {
                },
                (title, leftName, rightName, leftText, rightText) -> {
                },
                () -> {
                }
        ));

        runOnEdt(() -> panel.getTabbedPane().setSelectedIndex(0));
        runOnEdt(panel::deleteCurrentView);

        assertEquals(2, stateService.getViews().size());
        assertEquals("View", panel.getTabbedPane().getTitleAt(0));
        assertEquals("View 1", panel.getTabbedPane().getTitleAt(1));
    }

    @Test
    public void testWorkspacePanelBuildsNativeToolWindowActions() throws Exception {
        YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
        stateService.ensureAtLeastOneView();

        YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
                stateService,
                parserService,
                formatterService,
                (views, parent) -> null,
                message -> {
                },
                (title, leftName, rightName, leftText, rightText) -> {
                },
                () -> {
                }
        ));

        assertNotNull(panel.getAddViewButton());
        assertEquals(3, panel.getToolWindowTitleActions().length);
    }

    @Test
    public void testWorkspacePanelUsesVisibleViewTabLabel() throws Exception {
        YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
        stateService.ensureAtLeastOneView();

        YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
                stateService,
                parserService,
                formatterService,
                (views, parent) -> null,
                message -> {
                },
                (title, leftName, rightName, leftText, rightText) -> {
                },
                () -> {
                }
        ));

        Component tabComponent = panel.getViewTabsPanel().getComponent(0);

        assertTrue(tabComponent instanceof JPanel);

        JLabel titleLabel = (JLabel) findDescendant(tabComponent, JLabel.class, "View");
        assertNotNull(titleLabel);
    }

    @Test
    public void testWorkspacePanelBuildsSingleRowTabHeader() throws Exception {
        YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
        stateService.ensureAtLeastOneView();

        YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
                stateService,
                parserService,
                formatterService,
                (views, parent) -> null,
                message -> {
                },
                (title, leftName, rightName, leftText, rightText) -> {
                },
                () -> {
                }
        ));

        JPanel topBar = panel.getTopBarPanel();
        JPanel tabsRow = panel.getTabsRowPanel();
        JScrollPane tabsScrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, panel.getViewTabsPanel());

        assertSame(topBar, tabsRow.getParent());
        assertNotNull(tabsScrollPane);
        assertSame(tabsRow, tabsScrollPane.getParent());
        assertSame(panel.getViewTabsPanel(), panel.getAddViewButton().getParent());
        assertSame(topBar, panel.getTypeTabsPanel().getParent());
        assertTrue(panel.getTypeTabsPanel().getComponentCount() >= 2);
    }

    @Test
    public void testWorkspacePanelUsesScrollableTabLayout() throws Exception {
        YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
        stateService.ensureAtLeastOneView();

        YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
                stateService,
                parserService,
                formatterService,
                (views, parent) -> null,
                message -> {
                },
                (title, leftName, rightName, leftText, rightText) -> {
                },
                () -> {
                }
        ));

        assertEquals(JTabbedPane.SCROLL_TAB_LAYOUT, panel.getTabbedPane().getTabLayoutPolicy());
    }

    @Test
    public void testWorkspacePanelKeepsTabStripFlushWithEditor() throws Exception {
        YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
        stateService.ensureAtLeastOneView();

        YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
                stateService,
                parserService,
                formatterService,
                (views, parent) -> null,
                message -> {
                },
                (title, leftName, rightName, leftText, rightText) -> {
                },
                () -> {
                }
        ));

        Insets insets = panel.getViewTabsPanel().getBorder().getBorderInsets(panel.getViewTabsPanel());

        assertTrue(panel.getViewTabsScrollPane().getPreferredSize().height >= panel.getAddViewButton().getPreferredSize().height);
        assertEquals(0, insets.bottom);
    }

    @Test
    public void testWorkspacePanelShowsCloseAffordanceOnClosableTabs() throws Exception {
        YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
        stateService.createView("alpha: 1");
        stateService.createView("beta: 2");

        YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
                stateService,
                parserService,
                formatterService,
                (views, parent) -> null,
                message -> {
                },
                (title, leftName, rightName, leftText, rightText) -> {
                },
                () -> {
                }
        ));

        Component firstTab = panel.getViewTabsPanel().getComponent(0);
        Component secondTab = panel.getViewTabsPanel().getComponent(1);

        assertNull(findDescendant(firstTab, JButton.class, "x"));
        assertNotNull(findDescendant(secondTab, JButton.class, "x"));
    }

    @Test
    public void testWorkspacePanelDoesNotShowCloseAffordanceOnLastRemainingTab() throws Exception {
        YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
        stateService.ensureAtLeastOneView();

        YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
                stateService,
                parserService,
                formatterService,
                (views, parent) -> null,
                message -> {
                },
                (title, leftName, rightName, leftText, rightText) -> {
                },
                () -> {
                }
        ));

        Component onlyTab = panel.getViewTabsPanel().getComponent(0);

        assertNull(findDescendant(onlyTab, JButton.class, "x"));
    }

    @Test
    public void testWorkspacePanelPlacesAddButtonAtEndOfTabStrip() throws Exception {
        YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
        stateService.createView("alpha: 1");
        stateService.createView("beta: 2");

        YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
                stateService,
                parserService,
                formatterService,
                (views, parent) -> null,
                message -> {
                },
                (title, leftName, rightName, leftText, rightText) -> {
                },
                () -> {
                }
        ));

        Component lastComponent = panel.getViewTabsPanel().getComponent(panel.getViewTabsPanel().getComponentCount() - 1);

        assertSame(panel.getAddViewButton(), lastComponent);
        assertEquals(panel.getAddViewButton().getPreferredSize().height, ((JComponent) panel.getViewTabsPanel().getComponent(0)).getPreferredSize().height);
    }

    @Test
    public void testWorkspacePanelCreateViewReusesDeletedTabNumber() throws Exception {
        YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
        stateService.createView("alpha: 1");
        stateService.createView("beta: 2");
        stateService.createView("gamma: 3");

        YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
                stateService,
                parserService,
                formatterService,
                (views, parent) -> null,
                message -> {
                },
                (title, leftName, rightName, leftText, rightText) -> {
                },
                () -> {
                }
        ));

        runOnEdt(() -> panel.getTabbedPane().setSelectedIndex(2));
        runOnEdt(panel::deleteCurrentView);
        runOnEdt(panel::createNewView);

        assertEquals("View 2", panel.getTabbedPane().getTitleAt(panel.getTabbedPane().getSelectedIndex()));
    }

    @Test
    public void testWorkspacePanelUsesFixedTabWidth() throws Exception {
        YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
        stateService.createView("alpha: 1");
        stateService.createView("beta: 2");

        YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
                stateService,
                parserService,
                formatterService,
                (views, parent) -> null,
                message -> {
                },
                (title, leftName, rightName, leftText, rightText) -> {
                },
                () -> {
                }
        ));

        Dimension first = ((JComponent) panel.getViewTabsPanel().getComponent(0)).getPreferredSize();
        Dimension second = ((JComponent) panel.getViewTabsPanel().getComponent(1)).getPreferredSize();

        assertEquals(first.width, second.width);
    }

    @Test
    public void testWorkspacePanelBuildsFixedStatusBar() throws Exception {
        YamlViewState view = new YamlViewState("view-1", "View 1", "");
        YamlViewTabPanel panel = runOnEdt(() -> new YamlViewTabPanel(view, null, parserService, content -> {
        }));

        assertTrue(panel.getStatusPanel().getPreferredSize().height >= 28);
    }

    private static void runOnEdt(ThrowingRunnable runnable) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static <T> T runOnEdt(ThrowingSupplier<T> supplier) throws Exception {
        final Object[] result = new Object[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                result[0] = supplier.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return (T) result[0];
    }

    private static <T extends Component> T findDescendant(Component root, Class<T> type, String text) {
        if (type.isInstance(root)) {
            T candidate = type.cast(root);
            if (candidate instanceof AbstractButton button) {
                if (text.equals(button.getText())) {
                    return candidate;
                }
            } else if (candidate instanceof JLabel label) {
                if (text.equals(label.getText())) {
                    return candidate;
                }
            } else {
                return candidate;
            }
        }

        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T match = findDescendant(child, type, text);
                if (match != null) {
                    return match;
                }
            }
        }

        return null;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
