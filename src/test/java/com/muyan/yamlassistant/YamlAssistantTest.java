package com.muyan.yamlassistant;

import com.muyan.yamlassistant.diff.YamlDiffResult;
import com.muyan.yamlassistant.diff.YamlDiffService;
import com.muyan.yamlassistant.model.YamlDocument;
import com.muyan.yamlassistant.model.YamlNode;
import com.muyan.yamlassistant.services.YamlConverterService;
import com.muyan.yamlassistant.services.YamlFormatterService;
import com.muyan.yamlassistant.services.YamlParserService;
import com.muyan.yamlassistant.workspace.YamlViewState;
import com.muyan.yamlassistant.workspace.YamlWorkspaceStateService;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Config Assistant 核心服务单元测试
 */
public class YamlAssistantTest {

    private final YamlParserService parserService = new YamlParserService();
    private final YamlFormatterService formatterService = new YamlFormatterService();
    private final YamlConverterService converterService = new YamlConverterService();
    private final YamlDiffService diffService = new YamlDiffService();

    // ==================== Parser Tests ====================

    @Test
    public void testParseSimpleYaml() {
        String yaml = "name: test\nversion: 1.0";
        YamlDocument doc = parserService.parse(yaml);
        assertTrue(doc.isValid());
        assertFalse(doc.getRoots().isEmpty());
    }

    @Test
    public void testParseNestedYaml() {
        String yaml = "server:\n  port: 8080\n  host: localhost";
        YamlDocument doc = parserService.parse(yaml);
        assertTrue(doc.isValid());
        YamlNode root = doc.getRoots().get(0);
        assertFalse(root.getChildren().isEmpty());
    }

    @Test
    public void testParseEmptyYaml() {
        YamlDocument doc = parserService.parse("");
        assertFalse(doc.isValid());
    }

    @Test
    public void testParseInvalidYaml() {
        String yaml = "invalid: [unclosed";
        YamlDocument doc = parserService.parse(yaml);
        assertFalse(doc.isValid());
    }

    @Test
    public void testValidateCorrectYaml() {
        String yaml = "key: value";
        assertNull(parserService.validate(yaml));
    }

    // ==================== Formatter Tests ====================

    @Test
    public void testBeautifyYaml() {
        String yaml = "{name: test, version: '1.0'}";
        String result = formatterService.beautify(yaml);
        assertTrue(result.contains("name:"));
        assertTrue(result.contains("version:"));
    }

    @Test
    public void testBeautifyYamlPreservesComments() {
        String yaml = "# top comment\nserver: # inline comment\n  port: 8080\n";

        String result = formatterService.beautify(yaml);

        assertTrue(result.contains("# top comment"));
        assertTrue(result.contains("# inline comment"));
        assertTrue(result.contains("port: 8080"));
    }

    @Test
    public void testBeautifyYamlKeepsSequenceItemsIndented() {
        String yaml = "list:\n- alpha\n- beta\n- gamma\n";

        String result = formatterService.beautify(yaml);

        assertTrue(result.contains("list:\n  - alpha\n  - beta\n  - gamma\n"));
    }

    @Test
    public void testMinifyYaml() {
        String yaml = "name: test\nversion: 1.0\n";
        String result = formatterService.minify(yaml);
        assertNotNull(result);
    }

    // ==================== Converter Tests ====================

    @Test
    public void testYamlToJson() {
        String yaml = "name: test\nage: 25";
        String json = converterService.yamlToJson(yaml);
        assertTrue(json.contains("\"name\""));
        assertTrue(json.contains("\"test\""));
    }

    @Test
    public void testJsonToYaml() {
        String json = "{\"name\":\"test\",\"age\":25}";
        String yaml = converterService.jsonToYaml(json);
        assertTrue(yaml.contains("name:"));
    }

    @Test
    public void testJsonToYamlPreservesIntegerNumbers() {
        String json = "{\"age\":25,\"ratio\":2.5}";

        String yaml = converterService.jsonToYaml(json);

        assertTrue(yaml.contains("age: 25"));
        assertFalse(yaml.contains("age: 25.0"));
        assertTrue(yaml.contains("ratio: 2.5"));
    }

    @Test
    public void testIsJson() {
        assertTrue(converterService.isJson("{\"key\":\"value\"}"));
        assertTrue(converterService.isJson("[1,2,3]"));
        assertFalse(converterService.isJson("key: value"));
        assertFalse(converterService.isJson(null));
    }

    // ==================== Workspace State Tests ====================

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

    @Test
    public void testWorkspaceStateLoadStateNormalizesNullState() {
        YamlWorkspaceStateService service = new YamlWorkspaceStateService();

        service.loadState(null);

        assertEquals(1, service.getViews().size());
        assertEquals("View 1", service.getViews().get(0).getName());
        assertEquals("", service.getViews().get(0).getContent());
        assertNotNull(service.getViews().get(0).getId());
    }

    @Test
    public void testWorkspaceStateLoadStateNormalizesRestoredViews() {
        YamlWorkspaceStateService service = new YamlWorkspaceStateService();
        YamlWorkspaceStateService.State restored = new YamlWorkspaceStateService.State();
        restored.views = null;
        restored.nextViewIndex = 0;

        service.loadState(restored);

        assertEquals(1, service.getViews().size());
        assertEquals("View 1", service.getViews().get(0).getName());
        assertEquals("", service.getViews().get(0).getContent());
        assertNotNull(service.getViews().get(0).getId());
    }

    @Test
    public void testWorkspaceStateLoadStatePreservesValidViewsAndRestoresNumbering() {
        YamlWorkspaceStateService service = new YamlWorkspaceStateService();
        YamlWorkspaceStateService.State restored = new YamlWorkspaceStateService.State();
        restored.nextViewIndex = 1;
        restored.views.add(new YamlViewState("kept-id", "View 4", "alpha: 1"));
        restored.views.add(new YamlViewState(null, null, null));

        service.loadState(restored);

        assertEquals(2, service.getViews().size());
        assertEquals("kept-id", service.getViews().get(0).getId());
        assertEquals("View 4", service.getViews().get(0).getName());
        assertEquals("alpha: 1", service.getViews().get(0).getContent());
        assertNotNull(service.getViews().get(1).getId());
        assertEquals("View 5", service.getViews().get(1).getName());
        assertEquals("", service.getViews().get(1).getContent());

        YamlViewState created = service.createView("beta: 2");
        assertEquals("View 6", created.getName());
    }

    @Test
    public void testValidateCompareSelectionRequiresDifferentViews() {
        YamlWorkspaceStateService service = new YamlWorkspaceStateService();
        YamlViewState view = service.createView("alpha: 1");

        assertEquals(
                "Please choose two different views.",
                service.validateCompareSelection(view.getId(), view.getId(), parserService)
        );
    }

    @Test
    public void testValidateCompareSelectionRejectsInvalidLeftYaml() {
        YamlWorkspaceStateService service = new YamlWorkspaceStateService();
        YamlViewState left = service.createView("invalid: [unclosed");
        YamlViewState right = service.createView("beta: 2");

        String validation = service.validateCompareSelection(left.getId(), right.getId(), parserService);

        assertNotNull(validation);
        assertTrue(validation.startsWith("Left view is invalid:"));
    }

    @Test
    public void testValidateCompareSelectionRejectsDeletedView() {
        YamlWorkspaceStateService service = new YamlWorkspaceStateService();
        YamlViewState left = service.createView("alpha: 1");
        YamlViewState right = service.createView("beta: 2");

        service.deleteView(right.getId());

        assertEquals(
                "Selected view no longer exists.",
                service.validateCompareSelection(left.getId(), right.getId(), parserService)
        );
    }

    @Test
    public void testValidateCompareSelectionRejectsInvalidRightYaml() {
        YamlWorkspaceStateService service = new YamlWorkspaceStateService();
        YamlViewState left = service.createView("alpha: 1");
        YamlViewState right = service.createView("invalid: [unclosed");

        String validation = service.validateCompareSelection(left.getId(), right.getId(), parserService);

        assertNotNull(validation);
        assertTrue(validation.startsWith("Right view is invalid:"));
    }

    @Test
    public void testValidateCompareSelectionAcceptsTwoValidViews() {
        YamlWorkspaceStateService service = new YamlWorkspaceStateService();
        YamlViewState left = service.createView("alpha: 1");
        YamlViewState right = service.createView("beta: 2");

        assertNull(service.validateCompareSelection(left.getId(), right.getId(), parserService));
    }

    @Test
    public void testStoredViewsCanBeValidatedAndDiffedByVersion() {
        YamlWorkspaceStateService service = new YamlWorkspaceStateService();
        YamlViewState versionOne = service.createView("name: test\nversion: 1");
        YamlViewState versionTwo = service.createView("name: test\nversion: 2");

        assertNull(service.validateCompareSelection(versionOne.getId(), versionTwo.getId(), parserService));

        YamlDiffResult result = diffService.compare(versionOne.getContent(), versionTwo.getContent());

        assertTrue(result.hasDifferences());
        assertEquals(1, result.getDiffCount());
        assertEquals("version", result.getDiffs().get(0).getPath());
    }

    // ==================== Diff Tests ====================

    @Test
    public void testDiffIdenticalYaml() {
        String yaml = "name: test\nversion: 1.0";
        YamlDiffResult result = diffService.compare(yaml, yaml);
        assertFalse(result.hasDifferences());
    }

    @Test
    public void testDiffModifiedValue() {
        String left = "name: test\nversion: 1.0";
        String right = "name: test\nversion: 2.0";
        YamlDiffResult result = diffService.compare(left, right);
        assertTrue(result.hasDifferences());
        assertEquals(1, result.getDiffCount());
    }

    @Test
    public void testDiffAddedKey() {
        String left = "name: test";
        String right = "name: test\nversion: 1.0";
        YamlDiffResult result = diffService.compare(left, right);
        assertTrue(result.hasDifferences());
    }

    @Test
    public void testDiffRemovedKey() {
        String left = "name: test\nversion: 1.0";
        String right = "name: test";
        YamlDiffResult result = diffService.compare(left, right);
        assertTrue(result.hasDifferences());
    }

    @Test
    public void testDiffSummary() {
        String left = "name: test";
        String right = "name: changed\nnew_key: value";
        YamlDiffResult result = diffService.compare(left, right);
        String summary = result.getSummary();
        assertNotNull(summary);
        assertTrue(summary.contains("added") || summary.contains("modified"));
    }
}
