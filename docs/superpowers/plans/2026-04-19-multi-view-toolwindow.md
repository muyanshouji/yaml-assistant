# YAML Assistant Multi-View ToolWindow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the single-purpose YAML tool window with a project-persisted multi-view workspace that can create in-tool diff tabs from any two saved YAML views.

**Architecture:** Add a new `YamlWorkspacePanel` as the tool window root, keep user-authored YAML drafts in a project-scoped persistent service, and generate transient `Diff` tabs from selected saved views. Reuse the existing YAML parser and diff service so the new feature is mostly a UI and state-management refactor rather than a new data engine.

**Tech Stack:** Java 17, Swing, IntelliJ Platform SDK, `PersistentStateComponent`, JUnit 4, existing `YamlParserService`, existing `YamlDiffService`

---

## File Structure

### New files

- `src/main/java/com/muyan/yamlassistant/workspace/YamlWorkspaceStateService.java`
  - Project-level persistent service for saved view tabs.
- `src/main/java/com/muyan/yamlassistant/workspace/YamlViewState.java`
  - Serializable per-view state model.
- `src/main/java/com/muyan/yamlassistant/ui/YamlWorkspacePanel.java`
  - New tool window root panel with tabbed UI and toolbar.
- `src/main/java/com/muyan/yamlassistant/ui/YamlViewTabPanel.java`
  - One editable YAML view tab with validation status.
- `src/main/java/com/muyan/yamlassistant/ui/CompareViewsDialog.java`
  - Small compare dialog for selecting left and right views.

### Modified files

- `src/main/java/com/muyan/yamlassistant/ui/YamlToolWindowFactory.java`
  - Create the workspace panel instead of the old tree panel.
- `src/main/java/com/muyan/yamlassistant/actions/CompareYamlAction.java`
  - Retarget compare behavior to the workspace compare flow.
- `src/main/resources/META-INF/plugin.xml`
  - Register project-level persistent service and keep action wiring consistent.
- `src/test/java/com/muyan/yamlassistant/YamlAssistantTest.java`
  - Add tests for new workspace state and compare validation behavior.

### Existing files reused without structural change

- `src/main/java/com/muyan/yamlassistant/diff/YamlDiffService.java`
- `src/main/java/com/muyan/yamlassistant/diff/YamlDiffResult.java`
- `src/main/java/com/muyan/yamlassistant/services/YamlParserService.java`
- `src/main/java/com/muyan/yamlassistant/ui/YamlDiffPanel.java`

### Deferred files

- `src/main/java/com/muyan/yamlassistant/ui/YamlTreePanel.java`
  - Leave untouched in the first implementation unless cleanup is required later.

## Task 1: Add project-persisted view state

**Files:**
- Create: `src/main/java/com/muyan/yamlassistant/workspace/YamlViewState.java`
- Create: `src/main/java/com/muyan/yamlassistant/workspace/YamlWorkspaceStateService.java`
- Modify: `src/main/resources/META-INF/plugin.xml`
- Test: `src/test/java/com/muyan/yamlassistant/YamlAssistantTest.java`

- [ ] **Step 1: Write the failing state-model tests**

Add these tests to `src/test/java/com/muyan/yamlassistant/YamlAssistantTest.java`:

```java
import com.muyan.yamlassistant.workspace.YamlViewState;
import com.muyan.yamlassistant.workspace.YamlWorkspaceStateService;

@Test
public void testWorkspaceStateCreatesDefaultViewName() {
    YamlWorkspaceStateService service = new YamlWorkspaceStateService();

    YamlViewState first = service.createView("alpha: 1");
    YamlViewState second = service.createView("beta: 2");

    assertEquals("View 1", first.getName());
    assertEquals("View 2", second.getName());
    assertEquals(2, service.getViews().size());
}

@Test
public void testWorkspaceStateRestoresEmptyWorkspaceWithDefaultView() {
    YamlWorkspaceStateService service = new YamlWorkspaceStateService();

    service.ensureAtLeastOneView();

    assertEquals(1, service.getViews().size());
    assertEquals("View 1", service.getViews().get(0).getName());
    assertEquals("", service.getViews().get(0).getContent());
}

@Test
public void testWorkspaceStateDeletesLastViewByReplacingIt() {
    YamlWorkspaceStateService service = new YamlWorkspaceStateService();
    service.ensureAtLeastOneView();

    String id = service.getViews().get(0).getId();
    service.deleteView(id);

    assertEquals(1, service.getViews().size());
    assertNotEquals(id, service.getViews().get(0).getId());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.muyan.yamlassistant.YamlAssistantTest`
Expected: FAIL with missing `YamlWorkspaceStateService` and `YamlViewState` types.

- [ ] **Step 3: Write the minimal persisted state model**

Create `src/main/java/com/muyan/yamlassistant/workspace/YamlViewState.java`:

```java
package com.muyan.yamlassistant.workspace;

public class YamlViewState {
    private String id;
    private String name;
    private String content;

    public YamlViewState() {
    }

    public YamlViewState(String id, String name, String content) {
        this.id = id;
        this.name = name;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
```

Create `src/main/java/com/muyan/yamlassistant/workspace/YamlWorkspaceStateService.java`:

```java
package com.muyan.yamlassistant.workspace;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@State(
        name = "YamlWorkspaceState",
        storages = @Storage("yaml-assistant-workspace.xml")
)
public class YamlWorkspaceStateService implements PersistentStateComponent<YamlWorkspaceStateService.State> {

    public static class State {
        public List<YamlViewState> views = new ArrayList<>();
        public int nextViewIndex = 1;
    }

    private State state = new State();

    public static YamlWorkspaceStateService getInstance(Project project) {
        return project.getService(YamlWorkspaceStateService.class);
    }

    @Nullable
    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
        ensureAtLeastOneView();
    }

    public List<YamlViewState> getViews() {
        ensureAtLeastOneView();
        return Collections.unmodifiableList(state.views);
    }

    public YamlViewState createView(String content) {
        YamlViewState view = new YamlViewState(
                UUID.randomUUID().toString(),
                "View " + state.nextViewIndex++,
                content
        );
        state.views.add(view);
        return view;
    }

    public void ensureAtLeastOneView() {
        if (state.views.isEmpty()) {
            createView("");
        }
    }

    public void updateViewContent(String id, String content) {
        for (YamlViewState view : state.views) {
            if (view.getId().equals(id)) {
                view.setContent(content);
                return;
            }
        }
    }

    public void deleteView(String id) {
        state.views.removeIf(view -> view.getId().equals(id));
        ensureAtLeastOneView();
    }
}
```

Update `src/main/resources/META-INF/plugin.xml` by adding this extension inside `<extensions defaultExtensionNs="com.intellij">`:

```xml
<projectService serviceImplementation="com.muyan.yamlassistant.workspace.YamlWorkspaceStateService"/>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.muyan.yamlassistant.YamlAssistantTest`
Expected: PASS for the new state tests.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/muyan/yamlassistant/workspace/YamlViewState.java src/main/java/com/muyan/yamlassistant/workspace/YamlWorkspaceStateService.java src/main/resources/META-INF/plugin.xml src/test/java/com/muyan/yamlassistant/YamlAssistantTest.java
git commit -m "feat: add project-persisted YAML workspace state"
```

## Task 2: Add compare validation helpers and diff-tab input rules

**Files:**
- Modify: `src/main/java/com/muyan/yamlassistant/workspace/YamlWorkspaceStateService.java`
- Create: `src/main/java/com/muyan/yamlassistant/ui/CompareViewsDialog.java`
- Test: `src/test/java/com/muyan/yamlassistant/YamlAssistantTest.java`

- [ ] **Step 1: Write the failing compare validation tests**

Add these tests to `src/test/java/com/muyan/yamlassistant/YamlAssistantTest.java`:

```java
@Test
public void testCompareRequiresDifferentViews() {
    YamlWorkspaceStateService service = new YamlWorkspaceStateService();
    YamlViewState view = service.createView("name: test");

    String error = service.validateCompareSelection(view.getId(), view.getId(), parserService);

    assertEquals("Please choose two different views.", error);
}

@Test
public void testCompareRejectsInvalidLeftYaml() {
    YamlWorkspaceStateService service = new YamlWorkspaceStateService();
    YamlViewState left = service.createView("invalid: [broken");
    YamlViewState right = service.createView("name: test");

    String error = service.validateCompareSelection(left.getId(), right.getId(), parserService);

    assertTrue(error.startsWith("Left view is invalid:"));
}

@Test
public void testCompareAcceptsTwoValidViews() {
    YamlWorkspaceStateService service = new YamlWorkspaceStateService();
    YamlViewState left = service.createView("name: test");
    YamlViewState right = service.createView("name: changed");

    String error = service.validateCompareSelection(left.getId(), right.getId(), parserService);

    assertNull(error);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.muyan.yamlassistant.YamlAssistantTest`
Expected: FAIL because `validateCompareSelection` does not exist.

- [ ] **Step 3: Write the minimal compare validation implementation**

Update `src/main/java/com/muyan/yamlassistant/workspace/YamlWorkspaceStateService.java` by adding these methods:

```java
import com.muyan.yamlassistant.services.YamlParserService;

public YamlViewState getView(String id) {
    for (YamlViewState view : state.views) {
        if (view.getId().equals(id)) {
            return view;
        }
    }
    return null;
}

public String validateCompareSelection(String leftId, String rightId, YamlParserService parserService) {
    if (leftId == null || rightId == null || leftId.equals(rightId)) {
        return "Please choose two different views.";
    }

    YamlViewState left = getView(leftId);
    YamlViewState right = getView(rightId);
    if (left == null || right == null) {
        return "Selected view no longer exists.";
    }

    String leftError = parserService.validate(left.getContent());
    if (leftError != null) {
        return "Left view is invalid: " + leftError;
    }

    String rightError = parserService.validate(right.getContent());
    if (rightError != null) {
        return "Right view is invalid: " + rightError;
    }

    return null;
}
```

Create `src/main/java/com/muyan/yamlassistant/ui/CompareViewsDialog.java`:

```java
package com.muyan.yamlassistant.ui;

import com.muyan.yamlassistant.workspace.YamlViewState;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class CompareViewsDialog extends JDialog {

    private final JComboBox<YamlViewState> leftCombo;
    private final JComboBox<YamlViewState> rightCombo;
    private boolean confirmed;

    public CompareViewsDialog(Window owner, List<YamlViewState> views) {
        super(owner, "Compare YAML Views", ModalityType.APPLICATION_MODAL);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        leftCombo = new JComboBox<>(views.toArray(new YamlViewState[0]));
        rightCombo = new JComboBox<>(views.toArray(new YamlViewState[0]));
        if (views.size() > 1) {
            rightCombo.setSelectedIndex(1);
        }

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("Left view:"), gbc);
        gbc.gridx = 1;
        add(leftCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("Right view:"), gbc);
        gbc.gridx = 1;
        add(rightCombo, gbc);

        JButton compareButton = new JButton("Compare");
        compareButton.addActionListener(event -> {
            confirmed = true;
            setVisible(false);
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(event -> setVisible(false));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(compareButton);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        add(buttons, gbc);

        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getLeftViewId() {
        YamlViewState selected = (YamlViewState) leftCombo.getSelectedItem();
        return selected != null ? selected.getId() : null;
    }

    public String getRightViewId() {
        YamlViewState selected = (YamlViewState) rightCombo.getSelectedItem();
        return selected != null ? selected.getId() : null;
    }
}
```

Also add this `toString()` override to `YamlViewState` so combo boxes show the tab name:

```java
@Override
public String toString() {
    return name;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.muyan.yamlassistant.YamlAssistantTest`
Expected: PASS for compare validation tests.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/muyan/yamlassistant/workspace/YamlWorkspaceStateService.java src/main/java/com/muyan/yamlassistant/workspace/YamlViewState.java src/main/java/com/muyan/yamlassistant/ui/CompareViewsDialog.java src/test/java/com/muyan/yamlassistant/YamlAssistantTest.java
git commit -m "feat: add compare selection validation for YAML views"
```

## Task 3: Build editable YAML view tabs and workspace container

**Files:**
- Create: `src/main/java/com/muyan/yamlassistant/ui/YamlViewTabPanel.java`
- Create: `src/main/java/com/muyan/yamlassistant/ui/YamlWorkspacePanel.java`
- Modify: `src/main/java/com/muyan/yamlassistant/ui/YamlToolWindowFactory.java`
- Test: manual verification in sandbox IDE

- [ ] **Step 1: Write the failing integration expectation**

Document the expected behavior in the plan run itself before coding:

Run: `./gradlew runIde`
Expected before implementation: the tool window still shows the old fixed `YAML Tree` tab instead of editable view tabs.

- [ ] **Step 2: Create the editable view tab panel**

Create `src/main/java/com/muyan/yamlassistant/ui/YamlViewTabPanel.java`:

```java
package com.muyan.yamlassistant.ui;

import com.muyan.yamlassistant.services.YamlParserService;
import com.muyan.yamlassistant.workspace.YamlViewState;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.function.Consumer;

public class YamlViewTabPanel {

    private final JPanel mainPanel;
    private final JTextArea textArea;
    private final JLabel statusLabel;
    private final YamlViewState viewState;
    private final YamlParserService parserService;

    public YamlViewTabPanel(YamlViewState viewState,
                            YamlParserService parserService,
                            Consumer<String> onContentChanged) {
        this.viewState = viewState;
        this.parserService = parserService;

        mainPanel = new JPanel(new BorderLayout());
        textArea = new JTextArea(viewState.getContent());
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                handleChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                handleChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                handleChange();
            }

            private void handleChange() {
                String content = textArea.getText();
                viewState.setContent(content);
                onContentChanged.accept(content);
                refreshStatus();
            }
        });

        mainPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        refreshStatus();
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public String getContent() {
        return textArea.getText();
    }

    private void refreshStatus() {
        if (textArea.getText().trim().isEmpty()) {
            statusLabel.setText("Ready");
            return;
        }

        String error = parserService.validate(textArea.getText());
        statusLabel.setText(error == null ? "Parsed successfully" : "Parse error: " + error);
    }
}
```

- [ ] **Step 3: Create the workspace panel and wire tab actions**

Create `src/main/java/com/muyan/yamlassistant/ui/YamlWorkspacePanel.java`:

```java
package com.muyan.yamlassistant.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.muyan.yamlassistant.diff.YamlDiffResult;
import com.muyan.yamlassistant.diff.YamlDiffService;
import com.muyan.yamlassistant.services.YamlParserService;
import com.muyan.yamlassistant.workspace.YamlViewState;
import com.muyan.yamlassistant.workspace.YamlWorkspaceStateService;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class YamlWorkspacePanel {

    private final JPanel mainPanel;
    private final JTabbedPane tabbedPane;
    private final Project project;
    private final YamlWorkspaceStateService stateService;
    private final YamlParserService parserService;
    private final YamlDiffService diffService;
    private final Map<Component, String> viewIdsByComponent;
    private int diffCounter = 1;

    public YamlWorkspacePanel(Project project) {
        this.project = project;
        this.stateService = YamlWorkspaceStateService.getInstance(project);
        this.parserService = new YamlParserService();
        this.diffService = new YamlDiffService();
        this.viewIdsByComponent = new HashMap<>();

        mainPanel = new JPanel(new BorderLayout());
        tabbedPane = new JTabbedPane();

        mainPanel.add(createToolbar(), BorderLayout.NORTH);
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        rebuildViewTabs();
    }

    private JComponent createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));

        JButton newViewButton = new JButton("New View");
        newViewButton.addActionListener(event -> {
            YamlViewState view = stateService.createView("");
            addViewTab(view);
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        });

        JButton compareButton = new JButton("Compare");
        compareButton.addActionListener(event -> openCompareDialog());

        JButton deleteButton = new JButton("Delete Current Tab");
        deleteButton.addActionListener(event -> deleteCurrentTab());

        toolbar.add(newViewButton);
        toolbar.add(compareButton);
        toolbar.add(deleteButton);
        return toolbar;
    }

    private void rebuildViewTabs() {
        tabbedPane.removeAll();
        viewIdsByComponent.clear();
        for (YamlViewState view : stateService.getViews()) {
            addViewTab(view);
        }
    }

    private void addViewTab(YamlViewState view) {
        YamlViewTabPanel panel = new YamlViewTabPanel(
                view,
                parserService,
                content -> stateService.updateViewContent(view.getId(), content)
        );
        tabbedPane.addTab(view.getName(), panel.getMainPanel());
        viewIdsByComponent.put(panel.getMainPanel(), view.getId());
    }

    private void openCompareDialog() {
        CompareViewsDialog dialog = new CompareViewsDialog(
                SwingUtilities.getWindowAncestor(mainPanel),
                stateService.getViews()
        );
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) {
            return;
        }

        String error = stateService.validateCompareSelection(
                dialog.getLeftViewId(),
                dialog.getRightViewId(),
                parserService
        );
        if (error != null) {
            Messages.showErrorDialog(project, error, "Compare YAML Views");
            return;
        }

        YamlViewState left = stateService.getView(dialog.getLeftViewId());
        YamlViewState right = stateService.getView(dialog.getRightViewId());
        YamlDiffResult result = diffService.compare(left.getContent(), right.getContent());

        YamlDiffPanel diffPanel = new YamlDiffPanel();
        diffPanel.updateDiff(result);
        tabbedPane.addTab("Diff " + diffCounter++, diffPanel.getMainPanel());
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
    }

    private void deleteCurrentTab() {
        int index = tabbedPane.getSelectedIndex();
        if (index < 0) {
            return;
        }

        Component component = tabbedPane.getComponentAt(index);
        String viewId = viewIdsByComponent.remove(component);
        tabbedPane.removeTabAt(index);

        if (viewId != null) {
            stateService.deleteView(viewId);
            rebuildViewTabs();
        }
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}
```

Update `src/main/java/com/muyan/yamlassistant/ui/YamlToolWindowFactory.java`:

```java
@Override
public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    YamlWorkspacePanel workspacePanel = new YamlWorkspacePanel(project);

    ContentFactory contentFactory = ContentFactory.getInstance();
    Content content = contentFactory.createContent(workspacePanel.getMainPanel(), "Workspace", false);
    toolWindow.getContentManager().addContent(content);
}
```

- [ ] **Step 4: Run sandbox IDE to verify the new workspace appears**

Run: `./gradlew runIde`
Expected: the tool window opens with `Workspace` content, shows at least one editable `View 1` tab, and the toolbar contains `New View`, `Compare`, and `Delete Current Tab`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/muyan/yamlassistant/ui/YamlViewTabPanel.java src/main/java/com/muyan/yamlassistant/ui/YamlWorkspacePanel.java src/main/java/com/muyan/yamlassistant/ui/YamlToolWindowFactory.java
git commit -m "feat: add multi-view YAML workspace tool window"
```

## Task 4: Retarget compare action to the tool window workflow

**Files:**
- Modify: `src/main/java/com/muyan/yamlassistant/actions/CompareYamlAction.java`
- Modify: `src/main/resources/META-INF/plugin.xml`
- Test: manual verification in sandbox IDE

- [ ] **Step 1: Write the failing interaction expectation**

Run: `./gradlew runIde`
Expected before implementation: `Tools -> YAML Assistant -> Compare YAML Files` still opens file choosers instead of the workspace compare flow.

- [ ] **Step 2: Replace file chooser compare with tool window activation**

Update `src/main/java/com/muyan/yamlassistant/actions/CompareYamlAction.java`:

```java
package com.muyan.yamlassistant.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

public class CompareYamlAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("YAML Assistant");
        if (toolWindow != null) {
            toolWindow.show();
        }
    }
}
```

Update the action text in `src/main/resources/META-INF/plugin.xml`:

```xml
<action id="YamlAssistant.Compare"
        class="com.muyan.yamlassistant.actions.CompareYamlAction"
        text="Open YAML Compare Workspace"
        description="Open the YAML Assistant workspace and compare saved YAML views"
        icon="AllIcons.Actions.Diff"/>
```

- [ ] **Step 3: Run sandbox IDE to verify the menu action now opens the workspace**

Run: `./gradlew runIde`
Expected: invoking the compare action no longer opens file choosers and instead focuses the YAML Assistant tool window.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/muyan/yamlassistant/actions/CompareYamlAction.java src/main/resources/META-INF/plugin.xml
git commit -m "feat: route compare action to YAML workspace"
```

## Task 5: Verify end-to-end behavior and stabilize

**Files:**
- Modify: `src/test/java/com/muyan/yamlassistant/YamlAssistantTest.java`
- Test: full build and sandbox manual checks

- [ ] **Step 1: Add one regression test for diffing stored YAML views**

Add this test to `src/test/java/com/muyan/yamlassistant/YamlAssistantTest.java`:

```java
@Test
public void testDiffStoredViewsProducesModifiedEntry() {
    YamlWorkspaceStateService service = new YamlWorkspaceStateService();
    YamlViewState left = service.createView("name: test\nversion: 1");
    YamlViewState right = service.createView("name: test\nversion: 2");

    assertNull(service.validateCompareSelection(left.getId(), right.getId(), parserService));

    YamlDiffResult result = diffService.compare(left.getContent(), right.getContent());

    assertTrue(result.hasDifferences());
    assertEquals(1, result.getDiffCount());
    assertEquals("version", result.getDiffs().get(0).getPath());
}
```

- [ ] **Step 2: Run focused tests to verify the regression test passes**

Run: `./gradlew test --tests com.muyan.yamlassistant.YamlAssistantTest`
Expected: PASS with all workspace and diff tests green.

- [ ] **Step 3: Run the full build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run final manual sandbox checklist**

Run: `./gradlew runIde`

In the sandbox IDE verify:

- The tool window shows one default `View 1` tab on first open.
- Clicking `New View` creates `View 2`.
- Pasting valid YAML into two views and clicking `Compare` creates a `Diff` tab.
- Selecting the same view twice shows an error.
- Pasting invalid YAML into one view blocks diff creation.
- Deleting a diff tab only removes that tab.
- Deleting the last remaining view recreates one empty view.
- Closing and reopening the project restores only `View` tabs and their content.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/muyan/yamlassistant/YamlAssistantTest.java
git commit -m "test: cover YAML workspace diff flow"
```

## Spec Coverage Check

- Multi-tab tool window workspace: covered by Task 3.
- Editable manual-paste views: covered by Task 3.
- Project-level persistence: covered by Task 1.
- Compare two saved views via dialog: covered by Task 2 and Task 3.
- Diff tab inside tool window: covered by Task 3.
- Retargeted compare action: covered by Task 4.
- Edge rules for last view, invalid YAML, same-view compare: covered by Task 1, Task 2, and Task 5.

No uncovered spec requirements remain.

## Placeholder Scan

- No `TBD`, `TODO`, or deferred implementation instructions appear inside task steps.
- Each code-writing step includes concrete file paths and code blocks.
- Each verification step includes a specific command and expected outcome.

## Type Consistency Check

- Persisted state uses `YamlViewState` consistently in both service and UI.
- Workspace container consistently uses `YamlWorkspaceStateService`.
- Compare flow consistently validates through `validateCompareSelection` and diff generation through `YamlDiffService.compare`.
- `View` tabs are persistent and `Diff` tabs are transient throughout the plan.
