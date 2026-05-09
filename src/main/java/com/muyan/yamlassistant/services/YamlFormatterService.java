package com.muyan.yamlassistant.services;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Node;

import java.io.StringReader;
import java.io.StringWriter;

/**
 * YAML 格式化服务 — 美化或压缩 YAML 文本。
 *
 * 核心逻辑:
 * 1. 使用 SnakeYAML 解析 YAML 文本为 Java 对象
 * 2. 配置 DumperOptions（缩进、行宽、风格等）
 * 3. 使用配置后的 Yaml 实例重新输出为字符串
 */
public class YamlFormatterService {

    /**
     * 美化 YAML（块式风格，标准缩进）
     *
     * @param yamlText 原始 YAML 文本
     * @return 格式化后的 YAML 文本
     */
    public String beautify(String yamlText) {
        return format(yamlText, 2, 120, DumperOptions.FlowStyle.BLOCK);
    }

    /**
     * 压缩 YAML（流式风格，单行紧凑）
     *
     * @param yamlText 原始 YAML 文本
     * @return 压缩后的 YAML 文本
     */
    public String minify(String yamlText) {
        return format(yamlText, 0, Integer.MAX_VALUE, DumperOptions.FlowStyle.FLOW);
    }

    /**
     * 自定义格式化 YAML
     *
     * @param yamlText  原始 YAML 文本
     * @param indent    缩进空格数
     * @param lineWidth 每行最大宽度
     * @param flowStyle 输出风格（BLOCK 块式 / FLOW 流式）
     * @return 格式化后的 YAML 文本
     */
    public String format(String yamlText, int indent, int lineWidth, DumperOptions.FlowStyle flowStyle) {
        // Parse the YAML AST instead of plain Java objects so comments survive formatting.
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setProcessComments(true);
        Yaml parser = new Yaml(loaderOptions);
        Node node = parser.compose(new StringReader(yamlText));

        if (node == null) {
            return yamlText;
        }

        // 2. 配置输出选项
        DumperOptions options = new DumperOptions();
        options.setIndent(Math.max(indent, 1));
        if (flowStyle == DumperOptions.FlowStyle.BLOCK && indent >= 2) {
            options.setIndicatorIndent(indent);
            options.setIndentWithIndicator(true);
        }
        options.setWidth(lineWidth);
        options.setDefaultFlowStyle(flowStyle);
        options.setPrettyFlow(true);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        options.setProcessComments(true);

        // 3. 重新输出
        Yaml dumper = new Yaml(options);
        StringWriter writer = new StringWriter();
        dumper.serialize(node, writer);
        return writer.toString();
    }
}
