package com.muyan.yamlassistant.workspace;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.muyan.yamlassistant.services.PropertiesValidatorService;
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
        public WorkspaceContentType selectedContentType = WorkspaceContentType.YAML;
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
        return getViews(getSelectedContentType());
    }

    public List<YamlViewState> getViews(WorkspaceContentType contentType) {
        List<YamlViewState> filteredViews = new ArrayList<>();
        for (YamlViewState view : state.views) {
            if (view.getContentType() == contentType) {
                filteredViews.add(view);
            }
        }
        return Collections.unmodifiableList(filteredViews);
    }

    public YamlViewState createView(String content) {
        return createView(content, getSelectedContentType());
    }

    public YamlViewState createView(String content, WorkspaceContentType contentType) {
        List<YamlViewState> typedViews = getViews(contentType);
        String name = typedViews.isEmpty() ? "View" : "View " + nextViewIndex(contentType);
        YamlViewState view = new YamlViewState(
                UUID.randomUUID().toString(),
                name,
                content,
                contentType
        );
        state.views.add(view);
        state.nextViewIndex = Math.max(state.nextViewIndex, nextViewIndex(contentType));
        return view;
    }

    public void ensureAtLeastOneView() {
        ensureAtLeastOneView(getSelectedContentType());
    }

    public void ensureAtLeastOneView(WorkspaceContentType contentType) {
        if (getViews(contentType).isEmpty()) {
            createView("", contentType);
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
        WorkspaceContentType contentType = null;
        for (YamlViewState view : state.views) {
            if (view.getId().equals(id)) {
                contentType = view.getContentType();
                break;
            }
        }
        state.views.removeIf(view -> view.getId().equals(id));
        if (contentType != null) {
            ensureAtLeastOneView(contentType);
        } else {
            ensureAtLeastOneView();
        }
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
        return validateCompareSelection(leftViewId, rightViewId, parserService, new PropertiesValidatorService());
    }

    @Nullable
    public String validateCompareSelection(String leftViewId,
                                           String rightViewId,
                                           YamlParserService parserService,
                                           PropertiesValidatorService propertiesValidatorService) {
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

        if (leftView.getContentType() != rightView.getContentType()) {
            return "Please choose two views of the same content type.";
        }

        String leftValidation = validateView(leftView, parserService, propertiesValidatorService);
        if (leftValidation != null) {
            return "Left view is invalid: " + leftValidation;
        }

        String rightValidation = validateView(rightView, parserService, propertiesValidatorService);
        if (rightValidation != null) {
            return "Right view is invalid: " + rightValidation;
        }

        return null;
    }

    public WorkspaceContentType getSelectedContentType() {
        return state.selectedContentType != null ? state.selectedContentType : WorkspaceContentType.YAML;
    }

    public void setSelectedContentType(WorkspaceContentType contentType) {
        WorkspaceContentType previousContentType = getSelectedContentType();
        WorkspaceContentType nextContentType = contentType != null ? contentType : WorkspaceContentType.YAML;
        ensureAtLeastOneView(previousContentType);
        state.selectedContentType = nextContentType;
        ensureAtLeastOneView(nextContentType);
    }

    private String validateView(YamlViewState view,
                                YamlParserService parserService,
                                PropertiesValidatorService propertiesValidatorService) {
        if (view.getContentType() == WorkspaceContentType.PROPERTIES) {
            return propertiesValidatorService.validate(view.getContent());
        }

        return parserService.validate(view.getContent());
    }

    private State normalizeState(@Nullable State loadedState) {
        State normalizedState = new State();
        if (loadedState == null || loadedState.views == null) {
            return normalizedState;
        }

        if (loadedState.selectedContentType != null) {
            normalizedState.selectedContentType = loadedState.selectedContentType;
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

            WorkspaceContentType contentType = view.getContentType();
            normalizedState.views.add(new YamlViewState(id, name, content, contentType));
            if (contentType == WorkspaceContentType.YAML) {
                maxViewIndex = Math.max(maxViewIndex, parseViewIndex(name));
            }
        }

        migrateLegacyDefaultViewNames(normalizedState.views, WorkspaceContentType.YAML);
        maxViewIndex = 0;
        for (YamlViewState view : normalizedState.views) {
            if (view.getContentType() == WorkspaceContentType.YAML) {
                maxViewIndex = Math.max(maxViewIndex, parseViewIndex(view.getName()));
            }
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

    private int nextViewIndex(WorkspaceContentType contentType) {
        int maxViewIndex = 0;
        for (YamlViewState view : state.views) {
            if (view.getContentType() == contentType) {
                maxViewIndex = Math.max(maxViewIndex, parseViewIndex(view.getName()));
            }
        }
        return maxViewIndex + 1;
    }

    private void migrateLegacyDefaultViewNames(List<YamlViewState> views, WorkspaceContentType contentType) {
        List<YamlViewState> filteredViews = new ArrayList<>();
        for (YamlViewState view : views) {
            if (view.getContentType() == contentType) {
                filteredViews.add(view);
            }
        }

        if (filteredViews.isEmpty()) {
            return;
        }

        int firstIndex = parseViewIndex(filteredViews.get(0).getName());
        if (firstIndex <= 0) {
            return;
        }

        for (int index = 0; index < filteredViews.size(); index++) {
            int expectedIndex = firstIndex + index;
            if (!("View " + expectedIndex).equals(filteredViews.get(index).getName())) {
                return;
            }
        }

        filteredViews.get(0).setName("View");
        for (int index = 1; index < filteredViews.size(); index++) {
            filteredViews.get(index).setName("View " + index);
        }
    }
}
