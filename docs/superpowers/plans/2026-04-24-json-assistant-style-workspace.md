# YAML Assistant JSON-Assistant-Style Workspace Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the YAML Assistant tool window into a JSON Assistant-style view workspace with a left action rail, native IntelliJ diff flow, workspace formatting, and a fixed status bar.

**Architecture:** Keep the existing persisted `View` model and editor-backed `YamlViewTabPanel`, but replace the current tool window shell and compare result handling. The workspace becomes a custom layout with a left icon rail, a styled top tab/workspace header, a single editor surface, and a fixed bottom status bar, while diff results move to IntelliJ's native diff manager.

**Tech Stack:** Java 17, Swing, IntelliJ Platform SDK, IntelliJ native diff APIs, existing `YamlWorkspaceStateService`, existing `YamlFormatterService`, JUnit 4

---

## File Structure

### Modified files

- `src/main/java/com/muyan/yamlassistant/ui/YamlWorkspacePanel.java`
  - Replace generic toolbar layout with JSON Assistant-style workspace shell.
- `src/main/java/com/muyan/yamlassistant/ui/YamlViewTabPanel.java`
  - Refine editor surface integration and fixed status bar presentation.
- `src/main/java/com/muyan/yamlassistant/ui/CompareViewsDialog.java`
  - Keep compare selection but allow minor workspace-style polish.
- `src/main/java/com/muyan/yamlassistant/actions/CompareYamlAction.java`
  - Likely no behavior change, but verify it still fits the redesigned workspace entry point.
- `src/test/java/com/muyan/yamlassistant/ui/YamlWorkspacePanelTest.java`
  - Update view-workspace tests for the new compare and format behavior.

### Existing files reused

- `src/main/java/com/muyan/yamlassistant/workspace/YamlWorkspaceStateService.java`
- `src/main/java/com/muyan/yamlassistant/workspace/YamlViewState.java`
- `src/main/java/com/muyan/yamlassistant/services/YamlParserService.java`
- `src/main/java/com/muyan/yamlassistant/services/YamlFormatterService.java`

### Code to remove or simplify

- In-tool diff tab creation path inside `YamlWorkspacePanel`
- Any diff-tab metadata handling that no longer has a purpose after native diff handoff

## Task 1: Replace in-tool diff tabs with native IntelliJ diff

**Files:**
- Modify: `src/main/java/com/muyan/yamlassistant/ui/YamlWorkspacePanel.java`
- Modify: `src/test/java/com/muyan/yamlassistant/ui/YamlWorkspacePanelTest.java`

- [ ] **Step 1: Write the failing compare behavior test**

Add this test to `src/test/java/com/muyan/yamlassistant/ui/YamlWorkspacePanelTest.java`:

```java
@Test
public void testWorkspacePanelCompareUsesNativeDiffInsteadOfCreatingTab() throws Exception {
    YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
    YamlViewState left = stateService.createView("name: before");
    YamlViewState right = stateService.createView("name: after");
    List<String> diffTitles = new ArrayList<>();

    YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
            stateService,
            parserService,
            diffService,
            (views, parent) -> new YamlWorkspacePanel.CompareSelection(left.getId(), right.getId()),
            message -> {
            },
            (title, leftText, rightText) -> diffTitles.add(title)
    ));

    runOnEdt(panel::compareViews);

    assertEquals(0, panel.getTabbedPane().indexOfTab("Diff 1") + 1);
    assertEquals(1, diffTitles.size());
    assertEquals("View 1 vs View 2", diffTitles.get(0));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.muyan.yamlassistant.ui.YamlWorkspacePanelTest`
Expected: FAIL because `YamlWorkspacePanel` still creates an in-tool diff tab and has no injectable native diff launcher.

- [ ] **Step 3: Implement native diff handoff**

Update `src/main/java/com/muyan/yamlassistant/ui/YamlWorkspacePanel.java`:

```java
import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.requests.SimpleDiffRequest;
```

Add a new injected collaborator and constructor parameter:

```java
    private final NativeDiffLauncher nativeDiffLauncher;
```

In the project constructor:

```java
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
```

In the test constructor:

```java
    YamlWorkspacePanel(...,
                       ErrorNotifier errorNotifier,
                       NativeDiffLauncher nativeDiffLauncher) {
        ...
        this.nativeDiffLauncher = nativeDiffLauncher;
        initializeUi();
    }
```

Replace the current compare result handling:

```java
        nativeDiffLauncher.show(
                leftView.getName() + " vs " + rightView.getName(),
                leftView.getName(),
                rightView.getName(),
                leftView.getContent(),
                rightView.getContent()
        );
```

Remove the old in-tool diff-tab creation block and delete any now-unused diff-tab metadata logic.

Add the interface near the bottom of the file:

```java
    @FunctionalInterface
    interface NativeDiffLauncher {
        void show(String title, String leftName, String rightName, String leftText, String rightText);
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.muyan.yamlassistant.ui.YamlWorkspacePanelTest`
Expected: PASS with compare no longer creating a workspace diff tab.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/muyan/yamlassistant/ui/YamlWorkspacePanel.java src/test/java/com/muyan/yamlassistant/ui/YamlWorkspacePanelTest.java
git commit -m "feat: use native IntelliJ diff for workspace compare"
```

## Task 2: Add workspace-level format action for the active view

**Files:**
- Modify: `src/main/java/com/muyan/yamlassistant/ui/YamlWorkspacePanel.java`
- Modify: `src/test/java/com/muyan/yamlassistant/ui/YamlWorkspacePanelTest.java`

- [ ] **Step 1: Write the failing format behavior test**

Add this test to `src/test/java/com/muyan/yamlassistant/ui/YamlWorkspacePanelTest.java`:

```java
@Test
public void testWorkspacePanelFormatsCurrentViewContent() throws Exception {
    YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
    YamlViewState view = stateService.createView("name:test");

    YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
            stateService,
            parserService,
            diffService,
            (views, parent) -> null,
            message -> {
            },
            (title, leftName, rightName, leftText, rightText) -> {
            }
    ));

    runOnEdt(panel::formatCurrentView);

    assertTrue(stateService.getView(view.getId()).getContent().contains("name: test"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.muyan.yamlassistant.ui.YamlWorkspacePanelTest`
Expected: FAIL because `formatCurrentView()` does not exist.

- [ ] **Step 3: Implement minimal format-current-view behavior**

Update `src/main/java/com/muyan/yamlassistant/ui/YamlWorkspacePanel.java`:

```java
import com.muyan.yamlassistant.services.YamlFormatterService;
```

Add field:

```java
    private final YamlFormatterService formatterService;
```

Initialize it in constructors.

Add helper methods:

```java
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

    private YamlViewState getCurrentView() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex < 0) {
            return null;
        }
        Component component = tabbedPane.getComponentAt(selectedIndex);
        TabMetadata metadata = tabMetadata.get(component);
        if (metadata == null || metadata.isDiffTab()) {
            return null;
        }
        return stateService.getView(metadata.getViewId());
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.muyan.yamlassistant.ui.YamlWorkspacePanelTest`
Expected: PASS with current-view format behavior covered.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/muyan/yamlassistant/ui/YamlWorkspacePanel.java src/test/java/com/muyan/yamlassistant/ui/YamlWorkspacePanelTest.java
git commit -m "feat: add workspace format action for YAML views"
```

## Task 3: Refactor workspace shell to JSON Assistant-style layout

**Files:**
- Modify: `src/main/java/com/muyan/yamlassistant/ui/YamlWorkspacePanel.java`
- Modify: `src/main/java/com/muyan/yamlassistant/ui/YamlViewTabPanel.java`
- Test: `src/test/java/com/muyan/yamlassistant/ui/YamlWorkspacePanelTest.java`

- [ ] **Step 1: Write the failing layout tests**

Add these tests to `src/test/java/com/muyan/yamlassistant/ui/YamlWorkspacePanelTest.java`:

```java
@Test
public void testWorkspacePanelBuildsLeftActionRail() throws Exception {
    YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
    stateService.ensureAtLeastOneView();

    YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
            stateService,
            parserService,
            diffService,
            (views, parent) -> null,
            message -> {
            },
            (title, leftName, rightName, leftText, rightText) -> {
            }
    ));

    assertNotNull(panel.getActionRail());
    assertEquals(4, panel.getActionRail().getComponentCount());
}

@Test
public void testWorkspacePanelBuildsFixedStatusBar() throws Exception {
    YamlViewState view = new YamlViewState("view-1", "View 1", "");
    YamlViewTabPanel panel = runOnEdt(() -> new YamlViewTabPanel(view, parserService, content -> { }));

    assertTrue(panel.getStatusPanel().getPreferredSize().height >= 28);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.muyan.yamlassistant.ui.YamlWorkspacePanelTest`
Expected: FAIL because the current panel has no action rail getter and no fixed-height status bar.

- [ ] **Step 3: Implement the workspace shell redesign**

Update `src/main/java/com/muyan/yamlassistant/ui/YamlWorkspacePanel.java` to:

- Replace the top `FlowLayout` toolbar with a left vertical rail panel.
- Create exactly four rail buttons: `New View`, `Compare`, `Format`, `Delete Current View`.
- Keep tabs and `+` affordance in a custom top bar row.
- Add a small right-side action cluster with `Settings` and `Hide` buttons.
- Remove remaining diff-tab creation and diff-tab toolbar handling.

Add test-visible getters:

```java
    JPanel getActionRail() {
        return actionRail;
    }
```

Update `src/main/java/com/muyan/yamlassistant/ui/YamlViewTabPanel.java`:

- Give the status panel fixed vertical padding and a visible top border.
- Ensure the status panel sits outside the editor surface and remains visible.
- Keep the gray/green/red indicator behavior.

Expose for tests:

```java
    JPanel getStatusPanel() {
        return statusPanel;
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests com.muyan.yamlassistant.ui.YamlWorkspacePanelTest`
Expected: PASS with left action rail and fixed status bar checks green.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/muyan/yamlassistant/ui/YamlWorkspacePanel.java src/main/java/com/muyan/yamlassistant/ui/YamlViewTabPanel.java src/test/java/com/muyan/yamlassistant/ui/YamlWorkspacePanelTest.java
git commit -m "feat: redesign YAML workspace shell"
```

## Task 4: Polish compare dialog and workspace controls

**Files:**
- Modify: `src/main/java/com/muyan/yamlassistant/ui/CompareViewsDialog.java`
- Modify: `src/main/java/com/muyan/yamlassistant/ui/YamlWorkspacePanel.java`
- Test: manual verification in sandbox IDE

- [ ] **Step 1: Write the failing manual expectation**

Run: `./gradlew runIde`
Expected before implementation: compare dialog and workspace chrome still look like plain Swing utility UI rather than a plugin-workspace flow.

- [ ] **Step 2: Add minimal visual polish for compare and top controls**

Update `src/main/java/com/muyan/yamlassistant/ui/CompareViewsDialog.java` to:

- Keep the same explicit left/right selection behavior.
- Improve spacing and dark-theme-friendly defaults if needed.
- Preserve current functionality exactly.

Update `src/main/java/com/muyan/yamlassistant/ui/YamlWorkspacePanel.java` to:

- Wire the right-side control buttons.
- Make the `Hide` button collapse the tool window.
- Keep `Settings` as a stub only if it maps to opening plugin settings; otherwise omit it.

- [ ] **Step 3: Run sandbox IDE for visual verification**

Run: `./gradlew runIde`
Expected: workspace looks closer to the reference style, compare still opens explicit selector dialog, and diff opens natively.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/muyan/yamlassistant/ui/CompareViewsDialog.java src/main/java/com/muyan/yamlassistant/ui/YamlWorkspacePanel.java
git commit -m "refactor: polish YAML workspace controls"
```

## Task 5: Final verification and regression sweep

**Files:**
- Modify: `src/test/java/com/muyan/yamlassistant/ui/YamlWorkspacePanelTest.java`
- Test: full build and sandbox verification

- [ ] **Step 1: Add one regression test proving compare does not mutate tabs**

Add this test to `src/test/java/com/muyan/yamlassistant/ui/YamlWorkspacePanelTest.java`:

```java
@Test
public void testCompareDoesNotMutateWorkspaceTabs() throws Exception {
    YamlWorkspaceStateService stateService = new YamlWorkspaceStateService();
    YamlViewState left = stateService.createView("name: before");
    YamlViewState right = stateService.createView("name: after");

    YamlWorkspacePanel panel = runOnEdt(() -> new YamlWorkspacePanel(
            stateService,
            parserService,
            diffService,
            (views, parent) -> new YamlWorkspacePanel.CompareSelection(left.getId(), right.getId()),
            message -> {
            },
            (title, leftName, rightName, leftText, rightText) -> {
            }
    ));

    int beforeTabs = panel.getTabbedPane().getTabCount();
    runOnEdt(panel::compareViews);

    assertEquals(beforeTabs, panel.getTabbedPane().getTabCount());
}
```

- [ ] **Step 2: Run focused tests**

Run: `./gradlew test --tests com.muyan.yamlassistant.ui.YamlWorkspacePanelTest --tests com.muyan.yamlassistant.YamlAssistantTest --tests com.muyan.yamlassistant.actions.CompareYamlActionTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run full build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run final manual sandbox checklist**

Run: `./gradlew runIde`

Verify manually in the sandbox:

- The left action rail contains exactly `New View`, `Compare`, `Format`, and `Delete Current View`.
- The top workspace looks closer to the JSON Assistant reference than the previous generic toolbar layout.
- The bottom status bar is not visually clipped.
- Compare opens IntelliJ native diff instead of creating a workspace diff tab.
- Format rewrites the current view in place.
- `+` creates a new view.
- The last remaining view cannot be closed.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/muyan/yamlassistant/ui/YamlWorkspacePanelTest.java
git commit -m "test: cover redesigned YAML workspace behavior"
```

## Spec Coverage Check

- Left vertical action rail: covered by Task 3.
- Right action cluster: covered by Task 3 and Task 4.
- Native IntelliJ diff instead of in-tool diff tabs: covered by Task 1 and Task 5.
- Workspace formatting: covered by Task 2.
- Fixed status bar and dark workspace polish: covered by Task 3.
- Explicit compare dialog selection: preserved and checked in Task 4.
- View-only workspace persistence: preserved throughout Tasks 1-5.

No uncovered spec requirements remain.

## Placeholder Scan

- No `TBD`, `TODO`, or empty implementation notes appear in task steps.
- Each step has exact files and verification commands.
- All new behaviors are tied to concrete code changes or manual verification steps.

## Type Consistency Check

- `YamlWorkspacePanel` remains the shell and owns compare/format actions.
- `YamlViewTabPanel` remains the editor/status view component.
- `YamlWorkspaceStateService` remains the source of truth for persisted views.
- Native compare handoff consistently replaces workspace diff tabs across the plan.
