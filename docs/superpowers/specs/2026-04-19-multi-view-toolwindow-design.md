# YAML Assistant Multi-View ToolWindow Design

## Goal

Turn the current single-purpose YAML Assistant tool window into a workspace that can manage multiple user-created YAML views and create in-tool diff tabs from any two saved views.

## Current State

The plugin currently creates one fixed tool window tab named `YAML Tree` via `YamlToolWindowFactory`. That tab is backed by `YamlTreePanel`, which listens to the currently selected editor and renders a tree view of the open YAML file. YAML comparison is implemented separately in `CompareYamlAction`, which asks the user to pick two files and then opens IntelliJ's built-in diff window. There is no concept of user-managed view tabs, no tool-window-local diff results, and no project-scoped persistence for YAML content.

## Product Direction

The new tool window should behave like a small YAML workspace inside IntelliJ:

- Users manually paste or type YAML into multiple `View` tabs.
- Users can create, delete, and switch between view tabs inside one tool window.
- Users can choose any two existing view tabs and generate a new `Diff` tab inside the same tool window.
- View tabs must persist per project and be restored after reopening the project or restarting IntelliJ.
- Diff tabs are session-only derived views and do not need persistence in the first version.

## Scope

Included in this design:

- Replace the single fixed tree tab with a tabbed workspace UI.
- Add editable YAML view tabs.
- Add project-level persistence for view tabs.
- Add a compare flow that selects two existing views.
- Render structural diff results in a new diff tab within the tool window.
- Keep basic YAML parse validation for views before diffing.

Explicitly not included in this design:

- Per-view tree rendering inside each tab.
- Persisting diff tabs or diff history.
- Synchronization with currently open editor files.
- Drag-and-drop tab reordering beyond what the default tab UI already offers.
- Rich rename, duplicate, import-from-file, or export features.

## Recommended Approach

Use a single tool window containing one workspace panel with an internal tabbed UI. Represent each user-authored YAML draft as a `View` tab and each comparison result as a `Diff` tab. Persist only the authored views in a new project-level `PersistentStateComponent` service.

This approach matches the requested Json Assistant-style workflow while minimizing disruption to the rest of the plugin. It reuses the existing diff rendering code and keeps persistence simple by storing only primary user data rather than derived diff output.

## Alternatives Considered

### Alternative A: Single workspace with internal view and diff tabs (recommended)

Users work entirely inside the tool window. The tab strip contains both editable views and generated diff results.

Pros:

- Closest to the requested UX.
- Clear user mental model.
- Reuses one tool window instead of opening external windows.
- Easy to extend later with rename or duplicate actions.

Cons:

- Requires refactoring the current tool window from a single panel to a container.

### Alternative B: Separate fixed `Views` tab and fixed `Diff` tab

Users manage drafts in a list and compare them in one shared diff page.

Pros:

- Simpler internal state.
- Less tab churn.

Cons:

- Does not match the desired reference UX.
- Less direct when switching among multiple drafts.

### Alternative C: Keep file-based compare and add editable view tabs only

Users author YAML in tabs but diff still opens in IntelliJ's built-in diff window.

Pros:

- Lower implementation cost.

Cons:

- Fails the requested in-tool diff requirement.
- Produces a split interaction model.

## Architecture

### UI Container

Add a new main UI component, tentatively `YamlWorkspacePanel`, and make `YamlToolWindowFactory` create this panel instead of directly creating `YamlTreePanel`.

`YamlWorkspacePanel` is responsible for:

- Owning the tabbed pane.
- Rendering view and diff tabs.
- Hosting a small toolbar for workspace actions.
- Coordinating persistence and compare actions.

This panel becomes the single root of the tool window UI.

### View Tab

Each `View` tab represents one persisted YAML draft.

Each tab contains:

- An editable text area or IntelliJ editor-backed document component.
- A small status label at the bottom.

Status values should include at least:

- `Ready`
- `Parsed successfully`
- `Parse error: <message>`

The view tab validates its current text using the existing `YamlParserService`. Validation is used for user feedback and as a gate before diff creation.

### Diff Tab

Each `Diff` tab represents one comparison result created from two selected view tabs.

Each diff tab contains:

- A summary header such as `Comparing View 1 vs View 2`.
- The existing `YamlDiffPanel` result table.

Diff data is generated using the existing diff service path rather than IntelliJ's built-in text diff popup. Diff tabs are ephemeral and not restored after restart.

### Persistence Layer

Add a new project-level persistent service, tentatively `YamlWorkspaceStateService`, using `PersistentStateComponent`.

Its stored state should contain:

- Ordered list of views.
- A monotonically increasing counter for generating default names.

Suggested state model:

```java
public static class State {
    public List<YamlViewState> views = new ArrayList<>();
    public int nextViewIndex = 1;
}

public static class YamlViewState {
    public String id;
    public String name;
    public String content;
}
```

The service should be project-scoped rather than application-scoped so that each project has its own independent workspace.

### Compare Flow

The workspace toolbar exposes a `Compare` action.

When triggered:

1. Collect all current persisted view tabs.
2. Open a lightweight dialog with two selectors: `Left view` and `Right view`.
3. Reject selecting the same view on both sides.
4. Validate both YAML texts.
5. If valid, generate the diff result and open a new diff tab.
6. If invalid, show a clear error message and do not create a diff tab.

## Data Flow

### Opening the tool window

1. `YamlToolWindowFactory` creates `YamlWorkspacePanel`.
2. `YamlWorkspacePanel` loads persisted project state from `YamlWorkspaceStateService`.
3. If no views exist, it creates a default empty `View 1` entry and saves it.
4. The panel renders one tab per persisted view.

### Editing a view

1. User edits YAML in a view tab.
2. The tab updates its in-memory content.
3. The panel or service updates the persisted project state.
4. The tab validates content and updates status text.

### Creating a diff

1. User clicks `Compare`.
2. User selects two views in the compare dialog.
3. The workspace loads the selected view contents.
4. YAML is parsed and compared.
5. A new `Diff` tab is appended to the tab strip.

### Restarting IntelliJ or reopening the project

1. Project-level state reloads saved views.
2. Workspace recreates all saved `View` tabs.
3. No `Diff` tabs are restored.

## Edge Cases and Rules

- On first open, create exactly one empty view.
- The last remaining view cannot leave the workspace empty; deleting it should immediately recreate one empty view.
- Comparing the same view against itself is invalid.
- Invalid YAML in either selected view blocks diff creation.
- Empty YAML content is allowed as view content but should behave consistently during validation and diffing.
- Deleting a diff tab should only remove the tab, not affect persisted state.
- Deleting a view tab must remove it from persisted project state.

## Interaction Details

### Toolbar Actions for v1

Include exactly these toolbar actions in the first version:

- `New View`
- `Compare`
- `Delete Current Tab`

These three actions are enough to support the requested workflow without expanding scope into rename or duplicate behavior.

### Tab Naming

Default view names should be generated as:

- `View 1`
- `View 2`
- `View 3`

Default diff names should be generated as:

- `Diff 1`
- `Diff 2`

The diff tab content itself should still display which two views were compared.

## Impact on Existing Code

### Files likely to change

- `src/main/java/com/muyan/yamlassistant/ui/YamlToolWindowFactory.java`
  - Replace direct `YamlTreePanel` creation with workspace panel creation.

- `src/main/java/com/muyan/yamlassistant/actions/CompareYamlAction.java`
  - Redirect behavior away from file chooser flow toward the workspace compare flow, or retire this action in favor of tool-window-local compare handling.

- `src/main/resources/META-INF/plugin.xml`
  - Register any new project service and update actions if the compare entry point changes.

### Files likely to be added

- `src/main/java/com/muyan/yamlassistant/ui/YamlWorkspacePanel.java`
- `src/main/java/com/muyan/yamlassistant/ui/YamlViewTabPanel.java`
- `src/main/java/com/muyan/yamlassistant/ui/CompareViewsDialog.java`
- `src/main/java/com/muyan/yamlassistant/workspace/YamlWorkspaceStateService.java`
- `src/main/java/com/muyan/yamlassistant/workspace/YamlViewState.java`

### Existing code to reuse

- `YamlParserService` for validation.
- `YamlDiffPanel` for diff result rendering.
- Existing diff domain classes and services under `diff/`.

## Testing Strategy

### Manual scenarios

- Open tool window in a fresh project and verify one empty view appears.
- Create multiple views and verify each gets a new default name.
- Paste valid YAML into two views and create a diff tab.
- Paste invalid YAML into one view and verify compare is blocked with a clear error.
- Delete a view and verify state is updated.
- Delete the last view and verify a new empty view is created automatically.
- Restart IntelliJ and verify all view tabs reappear with saved content.
- Verify diff tabs do not reappear after restart.

### Automated coverage target

Add unit coverage for:

- Project state serialization and restoration.
- View creation and deletion rules.
- Compare input validation rules.
- Diff result creation from two stored views.

UI-heavy behavior can remain primarily manual in the first implementation, but persistence and compare rules should be covered by tests where feasible.

## Risks

### Risk: Saving on every keystroke is too noisy

Mitigation:

- Start with direct state updates because scope is small.
- If needed later, debounce writes inside the workspace panel.

### Risk: Tool window state becomes hard to manage

Mitigation:

- Keep persistence limited to authored view tabs only.
- Treat diff tabs as transient and reconstructable.

### Risk: Reusing current compare action causes duplicated UX

Mitigation:

- Decide during implementation whether the menu action should open the workspace compare dialog instead of file chooser compare.
- Avoid maintaining both interaction models unless there is a clear product reason.

## Open Implementation Choices

These do not change product behavior and can be finalized during implementation:

- Whether each view tab uses a plain Swing text component first or an IntelliJ editor-backed component.
- Whether compare is triggered only from the toolbar or also through the existing `Tools` action entry.
- Whether diff tab names are generic (`Diff 1`) or descriptive (`View 1 vs View 2`).

The recommended implementation should prefer the smallest correct option in each case.
