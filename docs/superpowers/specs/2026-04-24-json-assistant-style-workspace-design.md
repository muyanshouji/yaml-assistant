# YAML Assistant JSON-Assistant-Style Workspace Design

## Goal

Redesign the YAML Assistant tool window so it behaves like a JSON Assistant-style workspace: view-only working tabs, a left vertical action rail, a small right-side action cluster, a full dark workspace presentation, and native IntelliJ diff popups instead of in-tool diff tabs.

## Current State

The current implementation already has these pieces:

- Persisted project-scoped YAML views.
- `View` tabs with `+` and close affordances.
- A dark editor area.
- Compare selection dialog.
- Existing YAML formatter service.

The current workspace is still structurally closer to a basic `JTabbedPane` utility panel than the requested reference product. The main gaps are:

- No dedicated left-side action rail.
- No right-side top action cluster.
- Bottom status bar is visually weak and can feel crowded.
- Compare still creates an in-tool `Diff` tab instead of opening IntelliJ's native diff UI.
- The workspace still exposes generic utility-panel layout rather than a focused editor-workspace layout.

## Product Direction

The tool window should become a single-purpose YAML draft workspace:

- Only `View` tabs remain in the tool window.
- The left side contains icon-only workspace actions.
- The top tab strip remains the primary navigation area.
- The editor surface fills most of the panel and keeps a dark workspace look.
- The bottom status area becomes a stable fixed-height bar with clear state indication.
- Compare opens IntelliJ's native two-way diff window, similar to Git diff UX.

## Scope

Included in this redesign:

- Replace the current top toolbar with a left-side vertical action rail.
- Add a small right-side action cluster for workspace-level controls.
- Keep project-level `View` persistence.
- Keep the existing compare dialog for choosing left and right views.
- Replace in-tool diff tabs with IntelliJ native diff popup/window.
- Add current-view formatting from the workspace.
- Improve the bottom status bar layout and styling.
- Keep the current `+` and close-tab behavior for views.

Explicitly not included:

- Resizable split panes or embedded tree navigation in the workspace.
- In-tool diff tabs.
- Rich action customization, pinning, drag reordering, or import/export.
- Full JSON Assistant feature parity.

## Recommended Approach

Keep the existing view persistence and tab-management model, but replace the panel shell around it.

The new shell should use a custom workspace layout composed of:

- Left vertical action rail.
- Top tab strip.
- Editor workspace center.
- Bottom fixed status bar.
- Right-side small action cluster.

Compare should stop generating `Diff` tabs entirely and instead build two temporary diff contents from the selected views and hand them to IntelliJ's diff manager.

This approach preserves the working core that already exists while shifting the user experience to the requested plugin-style workspace.

## Alternatives Considered

### Alternative A: Full workspace shell redesign on top of the current view model (recommended)

Reuse the persisted views, compare dialog, formatter service, and editor panel logic, but replace the workspace shell and compare result handling.

Pros:

- Smallest correct change toward the desired UX.
- Keeps working persistence logic.
- Reuses native IntelliJ diff UI rather than rebuilding it.

Cons:

- Requires meaningful UI refactor.

### Alternative B: Visual-only skin over the current panel

Only restyle the current toolbar and tabs while keeping compare tabs internally.

Pros:

- Lower implementation effort.

Cons:

- Would still behave differently from the requested workflow.
- Would leave duplicated compare mental models in place.

### Alternative C: Fully custom workspace with bespoke tab management and bespoke diff preview

Pros:

- Maximum visual control.

Cons:

- Too much complexity for the current stage.
- Rebuilds mature IntelliJ behaviors for no product gain.

## Architecture

### Workspace Layout

`YamlWorkspacePanel` becomes the composition root for a more opinionated layout:

- `BorderLayout.WEST`: left action rail.
- `BorderLayout.NORTH`: top bar containing workspace title, tabs, and right-side actions.
- `BorderLayout.CENTER`: current editor view.
- `BorderLayout.SOUTH`: fixed status bar.

The `JTabbedPane` can still be used for view selection if styled and wrapped appropriately, but the surrounding layout should no longer look like a plain utility panel.

### Left Action Rail

The action rail contains exactly these buttons in the first version:

- `New View`
- `Compare`
- `Format`
- `Delete Current View`

Behavior:

- Icon-only buttons.
- Hover emphasis only; no complex persistent selected states.
- `Delete Current View` should no-op when only one view remains.
- `Compare` always opens the compare dialog.
- `Format` applies to the currently selected view only.

### Top Bar

The top bar contains:

- Workspace title area: `YAML Assistant: View`
- Current view tabs
- `+` tab affordance on the right side of the tab strip
- A right action cluster

### Right Action Cluster

First version buttons:

- Settings / configuration entry
- Collapse / hide tool window

Optional refresh-style button can be included only if it directly maps to a clear workspace behavior. If there is no concrete behavior, omit it.

### Editor Surface

The center area continues using the existing editor-backed `YamlViewTabPanel` approach.

Requirements:

- Dark, uninterrupted workspace background in dark themes.
- YAML syntax highlighting when running inside IntelliJ platform context.
- Fallback plain text editor only for non-platform tests.
- No in-tool diff panels in the center area anymore.

### Status Bar

The bottom status area becomes a fixed-height bar and must not be visually crushed by the editor component.

Requirements:

- Clear top border or separator.
- Stable height.
- Left-aligned state indicator dot and message.
- No overlay effect from the editor area.

State mapping:

- Gray dot: `Ready`
- Green dot: `Parsed successfully`
- Red dot: `Parse error: ...`

## Compare Flow

### Selection

The existing compare dialog pattern remains, with explicit left and right selection every time.

### Validation

The existing validation rules remain:

- Left and right views must differ.
- Both views must exist.
- Both YAML contents must parse successfully.

### Result Presentation

Instead of creating a workspace `Diff` tab, compare should:

1. Build two temporary diff contents from the selected view texts.
2. Open IntelliJ's native diff UI via the diff manager.
3. Title the diff request using the selected view names.

The tool window should keep focus on view management; diff is delegated to IntelliJ.

## Format Flow

`Format` should act on the currently selected view.

Behavior:

- Read current view text.
- Run `YamlFormatterService.beautify(...)`.
- Replace the current view content.
- Persist the updated content.
- Let normal validation refresh the bottom status bar.

If formatting fails:

- Keep content unchanged.
- Surface an error message in the workspace status or dialog.

## Data Model

No new persistence model is required.

Keep the existing project-scoped persisted `YamlViewState` collection. Continue restoring views on project reopen. Diff results remain non-persistent because compare no longer creates workspace tabs.

## Interaction Rules

- The tool window always contains at least one `View` tab.
- The final remaining `View` tab has no close affordance.
- `Delete Current View` should not remove the last remaining view.
- `+` always creates a new empty view and selects it.
- Compare never mutates workspace tabs.
- Format never creates new tabs.

## Impact on Existing Code

### Files likely to change

- `src/main/java/com/muyan/yamlassistant/ui/YamlWorkspacePanel.java`
  - Major layout and compare-flow changes.

- `src/main/java/com/muyan/yamlassistant/ui/YamlViewTabPanel.java`
  - Status bar layout refinement and editor presentation polish.

- `src/main/java/com/muyan/yamlassistant/ui/CompareViewsDialog.java`
  - Minor visual polish only if needed.

- `src/main/java/com/muyan/yamlassistant/actions/CompareYamlAction.java`
  - Likely unchanged, because its current role is already to activate the workspace.

### Existing code to reuse

- `YamlWorkspaceStateService`
- `YamlFormatterService`
- `YamlParserService`
- IntelliJ native diff APIs

### Existing code to remove or reduce

- In-tool diff tab creation path in `YamlWorkspacePanel`
- Any `Diff` tab metadata or tab restoration logic tied specifically to compare results

## Testing Strategy

### Automated coverage

Add or update tests for:

- `Compare` no longer creates a workspace diff tab.
- `Compare` invokes the compare flow and exits without mutating view tabs.
- `Format` rewrites the active view content.
- Status bar still reflects ready/success/error states.
- Last remaining view cannot be removed via toolbar delete.

### Manual checks

- Left action rail looks coherent in dark theme.
- Top tab strip visually matches the intended plugin-workspace style better than the current basic tabs.
- Bottom status bar is fully visible and not visually compressed.
- Compare opens IntelliJ native diff with the selected view labels.
- Format updates the selected view content in place.

## Risks

### Risk: Native diff handoff loses context

Mitigation:

- Use clear request titles like `View 1 vs View 2`.
- Keep compare dialog explicit.

### Risk: UI refactor destabilizes existing tab behavior

Mitigation:

- Preserve current view-state service and tab creation rules.
- Re-test `+`, close, and last-view protection.

### Risk: Status bar still fights editor layout

Mitigation:

- Keep status bar outside the editor component.
- Give it fixed padding and visible separation.

## Open Implementation Choices

- Whether the top tab strip remains a styled `JTabbedPane` or becomes a custom tab header row.
- Which icon set to use for the left rail and right cluster.
- Whether format errors should surface only in the status bar or also in a dialog.

These can be finalized during implementation as long as the behavior described above stays intact.
