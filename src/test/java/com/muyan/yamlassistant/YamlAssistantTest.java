package com.muyan.yamlassistant;

import com.muyan.yamlassistant.diff.YamlDiffResult;
import com.muyan.yamlassistant.diff.YamlDiffService;
import com.muyan.yamlassistant.model.YamlDocument;
import com.muyan.yamlassistant.model.YamlNode;
import com.muyan.yamlassistant.services.YamlConverterService;
import com.muyan.yamlassistant.services.YamlFormatterService;
import com.muyan.yamlassistant.services.YamlParserService;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * YAML Assistant 核心服务单元测试
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
    public void testIsJson() {
        assertTrue(converterService.isJson("{\"key\":\"value\"}"));
        assertTrue(converterService.isJson("[1,2,3]"));
        assertFalse(converterService.isJson("key: value"));
        assertFalse(converterService.isJson(null));
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
