package com.muyan.yamlassistant.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * YAML ↔ JSON 转换服务。
 *
 * 核心逻辑:
 * - YAML → JSON: SnakeYAML 解析为 Java 对象 → Gson 序列化为 JSON
 * - JSON → YAML: Gson 解析 JSON 为 Java 对象 → SnakeYAML 输出为 YAML
 */
public class YamlConverterService {

    private final Yaml yaml;
    private final Gson gson;

    public YamlConverterService() {
        this.yaml = new Yaml();
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL)
                .create();
    }

    /**
     * YAML 转 JSON
     *
     * @param yamlText YAML 文本
     * @return JSON 字符串
     */
    public String yamlToJson(String yamlText) {
        Object data = yaml.load(yamlText);
        return gson.toJson(data);
    }

    /**
     * JSON 转 YAML
     *
     * @param jsonText JSON 文本
     * @return YAML 字符串
     */
    @SuppressWarnings("unchecked")
    public String jsonToYaml(String jsonText) {
        Object data = normalizeNumbers(gson.fromJson(jsonText, Object.class));

        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);

        Yaml dumper = new Yaml(options);
        return dumper.dump(data);
    }

    private Object normalizeNumbers(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                normalized.put(entry.getKey(), normalizeNumbers(entry.getValue()));
            }
            return normalized;
        }

        if (value instanceof List<?> list) {
            List<Object> normalized = new ArrayList<>(list.size());
            for (Object item : list) {
                normalized.add(normalizeNumbers(item));
            }
            return normalized;
        }

        if (value instanceof BigDecimal decimal && decimal.scale() <= 0) {
            return decimal.toBigIntegerExact();
        }

        return value;
    }

    /**
     * 检测文本是否为 JSON 格式
     */
    public boolean isJson(String text) {
        if (text == null) return false;
        String trimmed = text.trim();
        if ((!trimmed.startsWith("{") || !trimmed.endsWith("}"))
                && (!trimmed.startsWith("[") || !trimmed.endsWith("]"))) {
            return false;
        }
        try {
            gson.fromJson(trimmed, Object.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
