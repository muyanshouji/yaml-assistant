package com.muyan.yamlassistant.workspace;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.muyan.yamlassistant.services.YamlParserService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@State(
        name = "YamlWorkspaceState",
        storages = @Storage(StoragePathMacros.WORKSPACE_FILE)
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
    public void loadState(@Nullable State state) {
        this.state = normalizeState(state);
        ensureAtLeastOneView();
    }

    public List<YamlViewState> getViews() {
        ensureAtLeastOneView();
        return Collections.unmodifiableList(state.views);
    }

    public YamlViewState createView(String content) {
        String name = state.views.isEmpty() ? "View" : "View " + nextViewIndex();
        YamlViewState view = new YamlViewState(
                UUID.randomUUID().toString(),
                name,
                content
        );
        state.views.add(view);
        state.nextViewIndex = Math.max(state.nextViewIndex, nextViewIndex());
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

    @Nullable
    public YamlViewState getView(String id) {
        for (YamlViewState view : state.views) {
            if (view.getId().equals(id)) {
                return view;
            }
        }
        return null;
    }

    @Nullable
    public String validateCompareSelection(String leftViewId, String rightViewId, YamlParserService parserService) {
        if (leftViewId == null || rightViewId == null) {
            return "Selected view no longer exists.";
        }

        if (leftViewId.equals(rightViewId)) {
            return "Please choose two different views.";
        }

        YamlViewState leftView = getView(leftViewId);
        YamlViewState rightView = getView(rightViewId);
        if (leftView == null || rightView == null) {
            return "Selected view no longer exists.";
        }

        String leftValidation = parserService.validate(leftView.getContent());
        if (leftValidation != null) {
            return "Left view is invalid: " + leftValidation;
        }

        String rightValidation = parserService.validate(rightView.getContent());
        if (rightValidation != null) {
            return "Right view is invalid: " + rightValidation;
        }

        return null;
    }

    private State normalizeState(@Nullable State loadedState) {
        State normalizedState = new State();
        if (loadedState == null || loadedState.views == null) {
            return normalizedState;
        }

        int maxViewIndex = 0;
        for (YamlViewState view : loadedState.views) {
            if (view == null) {
                continue;
            }

            String id = view.getId();
            if (id == null || id.isEmpty()) {
                id = UUID.randomUUID().toString();
            }

            String content = view.getContent();
            if (content == null) {
                content = "";
            }

            String name = view.getName();
            if (name == null || name.isEmpty()) {
                name = normalizedState.views.isEmpty() ? "View" : "View " + (maxViewIndex + 1);
            }

            normalizedState.views.add(new YamlViewState(id, name, content));
            maxViewIndex = Math.max(maxViewIndex, parseViewIndex(name));
        }

        migrateLegacyDefaultViewNames(normalizedState.views);
        maxViewIndex = 0;
        for (YamlViewState view : normalizedState.views) {
            maxViewIndex = Math.max(maxViewIndex, parseViewIndex(view.getName()));
        }

        normalizedState.nextViewIndex = Math.max(loadedState.nextViewIndex, maxViewIndex + 1);
        return normalizedState;
    }

    private int parseViewIndex(String name) {
        if ("View".equals(name)) {
            return 0;
        }
        if (name != null && name.startsWith("View ")) {
            try {
                return Integer.parseInt(name.substring("View ".length()));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private int nextViewIndex() {
        int maxViewIndex = 0;
        for (YamlViewState view : state.views) {
            maxViewIndex = Math.max(maxViewIndex, parseViewIndex(view.getName()));
        }
        return maxViewIndex + 1;
    }

    private void migrateLegacyDefaultViewNames(List<YamlViewState> views) {
        if (views.isEmpty()) {
            return;
        }

        int firstIndex = parseViewIndex(views.get(0).getName());
        if (firstIndex <= 0) {
            return;
        }

        for (int index = 0; index < views.size(); index++) {
            int expectedIndex = firstIndex + index;
            if (!("View " + expectedIndex).equals(views.get(index).getName())) {
                return;
            }
        }

        views.get(0).setName("View");
        for (int index = 1; index < views.size(); index++) {
            views.get(index).setName("View " + index);
        }
    }
}
