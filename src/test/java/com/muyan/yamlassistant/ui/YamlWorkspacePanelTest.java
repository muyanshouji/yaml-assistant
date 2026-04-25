package com.muyan.yamlassistant.ui;

import com.muyan.yamlassistant.diff.YamlDiffService;
import com.muyan.yamlassistant.services.YamlFormatterService;
import com.muyan.yamlassistant.services.YamlParserService;
import com.muyan.yamlassistant.workspace.YamlViewState;
import com.muyan.yamlassistant.workspace.YamlWorkspaceStateService;
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
        assertEquals("View 1", panel.getTabbedPane().getTitleAt(0));
        assertEquals("View 2", panel.getTabbedPane().getTitleAt(1));

        runOnEdt(panel::createNewView);

        assertEquals(3, stateService.getViews().size());
        assertEquals(3, panel.getTabbedPane().getTabCount());
        assertEquals("View 3", panel.getTabbedPane().getTitleAt(2));
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
        assertEquals("View 1 vs View 2", diffTitles.get(0));
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
        assertEquals("View 1", panel.getTabbedPane().getTitleAt(0));
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
        assertEquals("View 1", panel.getTabbedPane().getTitleAt(0));
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

        assertEquals(3, panel.getActionButtonsPanel().getComponentCount());
        assertNotNull(panel.getAddViewButton());
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

        assertTrue(tabComponent instanceof JToggleButton);
        assertEquals("View 1", ((JToggleButton) tabComponent).getText());
    }

    @Test
    public void testWorkspacePanelBuildsTwoRowHeader() throws Exception {
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
        JPanel workspaceHeader = panel.getWorkspaceHeaderPanel();
        JPanel tabsRow = panel.getTabsRowPanel();
        JScrollPane tabsScrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, panel.getViewTabsPanel());

        assertSame(topBar, workspaceHeader.getParent());
        assertSame(topBar, tabsRow.getParent());
        assertSame(workspaceHeader, panel.getWorkspaceTitleLabel().getParent());
        assertSame(workspaceHeader, panel.getActionButtonsPanel().getParent());
        assertNotNull(tabsScrollPane);
        assertSame(tabsRow, tabsScrollPane.getParent());
        assertSame(tabsRow, panel.getAddViewButton().getParent());
        assertEquals("Workspace", panel.getWorkspaceTitleLabel().getText());
        assertEquals(3, panel.getActionButtonsPanel().getComponentCount());
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
    public void testWorkspacePanelReservesBottomGutterForTabScrollbar() throws Exception {
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

        assertTrue(panel.getViewTabsScrollPane().getPreferredSize().height > panel.getAddViewButton().getPreferredSize().height);
        assertTrue(insets.bottom > 0);
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

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
