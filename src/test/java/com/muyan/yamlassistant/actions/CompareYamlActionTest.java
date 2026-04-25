package com.muyan.yamlassistant.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowBalloonShowOptions;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.intellij.testFramework.ServiceContainerUtil.registerOrReplaceServiceInstance;

public class CompareYamlActionTest extends BasePlatformTestCase {

    public void testActionActivatesConfigAssistantToolWindow() {
        AtomicBoolean activated = new AtomicBoolean(false);
        ToolWindow toolWindow = createRecordingToolWindow(activated);
        registerOrReplaceServiceInstance(
                getProject(),
                ToolWindowManager.class,
                new TestToolWindowManager(getProject(), toolWindow),
                getTestRootDisposable()
        );

        DataContext context = SimpleDataContext.getSimpleContext(CommonDataKeys.PROJECT, getProject());
        AnActionEvent event = TestActionEvent.createTestEvent(new CompareYamlAction(), context);

        new CompareYamlAction().actionPerformed(event);

        assertTrue(activated.get());
    }

    private ToolWindow createRecordingToolWindow(AtomicBoolean activated) {
        return (ToolWindow) Proxy.newProxyInstance(
                ToolWindow.class.getClassLoader(),
                new Class[]{ToolWindow.class},
                (proxy, method, args) -> {
                    if ("activate".equals(method.getName())) {
                        activated.set(true);
                        Runnable runnable = args != null && args.length > 0 ? (Runnable) args[0] : null;
                        if (runnable != null) {
                            runnable.run();
                        }
                        return null;
                    }
                    if ("getProject".equals(method.getName())) {
                        return getProject();
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType.equals(boolean.class)) {
                        return false;
                    }
                    if (returnType.equals(int.class)) {
                        return 0;
                    }
                    return null;
                }
        );
    }

    private static final class TestToolWindowManager extends ToolWindowManager {
        private final Project project;
        private final ToolWindow toolWindow;

        private TestToolWindowManager(Project project, ToolWindow toolWindow) {
            this.project = project;
            this.toolWindow = toolWindow;
        }

        @Override
        public @NotNull IdeFocusManager getFocusManager() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean canShowNotification(@NotNull String toolWindowId) {
            return false;
        }

        @Override
        public @NotNull ToolWindow registerToolWindow(@NotNull com.intellij.openapi.wm.RegisterToolWindowTask task) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void unregisterToolWindow(@NotNull String id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void activateEditorComponent() {
        }

        @Override
        public boolean isEditorComponentActive() {
            return false;
        }

        @Override
        public String @NotNull [] getToolWindowIds() {
            return new String[]{"Config Assistant"};
        }

        @Override
        public @NotNull Set<String> getToolWindowIdSet() {
            return Set.of("Config Assistant");
        }

        @Override
        public @Nullable String getActiveToolWindowId() {
            return null;
        }

        @Override
        public @Nullable String getLastActiveToolWindowId() {
            return null;
        }

        @Override
        public ToolWindow getToolWindow(@Nullable String id) {
            return "Config Assistant".equals(id) ? toolWindow : null;
        }

        @Override
        public void invokeLater(@NotNull Runnable runnable) {
            runnable.run();
        }

        @Override
        public void notifyByBalloon(@NotNull ToolWindowBalloonShowOptions options) {
        }

        @Override
        public Balloon getToolWindowBalloon(@NotNull String id) {
            return null;
        }

        @Override
        public boolean isMaximized(@NotNull ToolWindow window) {
            return false;
        }

        @Override
        public void setMaximized(@NotNull ToolWindow window, boolean maximized) {
        }
    }
}
