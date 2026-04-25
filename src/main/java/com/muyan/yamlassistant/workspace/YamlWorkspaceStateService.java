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
                name = "View " + (maxViewIndex + 1);
            }

            normalizedState.views.add(new YamlViewState(id, name, content));
            maxViewIndex = Math.max(maxViewIndex, parseViewIndex(name));
        }

        normalizedState.nextViewIndex = Math.max(loadedState.nextViewIndex, maxViewIndex + 1);
        return normalizedState;
    }

    private int parseViewIndex(String name) {
        if (name != null && name.startsWith("View ")) {
            try {
                return Integer.parseInt(name.substring("View ".length()));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }
}
