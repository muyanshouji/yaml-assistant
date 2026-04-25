package com.muyan.yamlassistant.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class YamlEditorActionsTest extends BasePlatformTestCase {

    public void testFormatActionOnlyEnabledForYamlFiles() {
        assertTrue(isActionEnabled(new FormatYamlAction(), "sample.yaml", "name: test"));
        assertFalse(isActionEnabled(new FormatYamlAction(), "sample.txt", "name: test"));
    }

    public void testConvertActionOnlyEnabledForYamlFiles() {
        assertTrue(isActionEnabled(new ConvertYamlJsonAction(), "sample.yml", "name: test"));
        assertFalse(isActionEnabled(new ConvertYamlJsonAction(), "sample.json", "{\"name\":\"test\"}"));
    }

    public void testViewTreeActionOnlyEnabledForYamlFiles() {
        assertTrue(isActionEnabled(new ViewYamlTreeAction(), "sample.yaml", "name: test"));
        assertFalse(isActionEnabled(new ViewYamlTreeAction(), "sample.java", "class Test {}"));
    }

    private boolean isActionEnabled(AnAction action, String fileName, String content) {
        myFixture.configureByText(fileName, content);

        DataContext context = SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, getProject())
                .add(CommonDataKeys.EDITOR, myFixture.getEditor())
                .add(CommonDataKeys.PSI_FILE, myFixture.getFile())
                .add(CommonDataKeys.VIRTUAL_FILE, myFixture.getFile().getVirtualFile())
                .build();
        AnActionEvent event = TestActionEvent.createTestEvent(action, context);

        action.update(event);
        return event.getPresentation().isEnabled();
    }
}
