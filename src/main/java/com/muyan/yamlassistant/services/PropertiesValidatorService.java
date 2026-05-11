package com.muyan.yamlassistant.services;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PropertiesValidatorService {

    public String validate(String text) {
        String validation = validateLenient(text);
        if (validation != null) {
            return validation;
        }

        return detectStrictKeyValueIssues(text);
    }

    public String validateLenient(String text) {
        String normalizedText = text != null ? text : "";
        try {
            Properties properties = new Properties();
            properties.load(new StringReader(normalizedText));
        } catch (IOException | IllegalArgumentException ex) {
            return ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        }

        return detectYamlLikeContent(normalizedText);
    }

    private String detectStrictKeyValueIssues(String text) {
        String normalized = text != null ? text.replace("\r\n", "\n").replace('\r', '\n') : "";
        List<LogicalLine> logicalLines = collectLogicalLines(normalized.split("\n", -1));
        for (LogicalLine line : logicalLines) {
            String trimmed = stripLeading(line.content);
            if (trimmed.isEmpty() || isComment(trimmed)) {
                continue;
            }

            int contentStart = indentation(line.content);
            int separatorIndex = findUnescapedEquals(line.content, contentStart);
            if (separatorIndex < 0) {
                return "Expected key=value syntax at line " + line.lineNumber + ", column " + (contentStart + 1) + ".";
            }

            String key = line.content.substring(contentStart, separatorIndex).trim();
            if (key.isEmpty()) {
                return "Missing key before '=' at line " + line.lineNumber + ", column " + (separatorIndex + 1) + ".";
            }
        }

        return null;
    }

    private String detectYamlLikeContent(String text) {
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        List<LogicalLine> logicalLines = collectLogicalLines(normalized.split("\n", -1));
        for (int index = 0; index < logicalLines.size(); index++) {
            LogicalLine line = logicalLines.get(index);
            String trimmed = stripLeading(line.content);
            if (trimmed.isEmpty() || isComment(trimmed)) {
                continue;
            }

            if ("---".equals(trimmed) || "...".equals(trimmed)) {
                return buildYamlLikeError("document marker", line);
            }

            if (trimmed.startsWith("- ") || "-".equals(trimmed)) {
                return buildYamlLikeError("sequence item", line);
            }

            if (isYamlMappingHeader(trimmed) && nextMeaningfulLineIsIndented(logicalLines, index, line.indent)) {
                return buildYamlLikeError("mapping", line);
            }
        }

        return null;
    }

    private List<LogicalLine> collectLogicalLines(String[] lines) {
        List<LogicalLine> logicalLines = new ArrayList<>();
        boolean continuation = false;
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            if (!continuation) {
                logicalLines.add(new LogicalLine(index + 1, indentation(line), line));
            }
            continuation = endsWithContinuation(line);
        }
        return logicalLines;
    }

    private boolean nextMeaningfulLineIsIndented(List<LogicalLine> logicalLines, int currentIndex, int currentIndent) {
        for (int index = currentIndex + 1; index < logicalLines.size(); index++) {
            LogicalLine next = logicalLines.get(index);
            String trimmed = stripLeading(next.content);
            if (trimmed.isEmpty() || isComment(trimmed)) {
                continue;
            }
            return next.indent > currentIndent;
        }
        return false;
    }

    private boolean isYamlMappingHeader(String trimmedLine) {
        int commentIndex = findInlineCommentStart(trimmedLine);
        String content = commentIndex >= 0 ? trimmedLine.substring(0, commentIndex) : trimmedLine;
        int colonIndex = content.indexOf(':');
        if (colonIndex < 0) {
            return false;
        }
        for (int index = colonIndex + 1; index < content.length(); index++) {
            if (!Character.isWhitespace(content.charAt(index))) {
                return false;
            }
        }
        return colonIndex > 0;
    }

    private int findInlineCommentStart(String text) {
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == '#' && (index == 0 || Character.isWhitespace(text.charAt(index - 1)))) {
                return index;
            }
        }
        return -1;
    }

    private boolean endsWithContinuation(String line) {
        int slashCount = 0;
        for (int index = line.length() - 1; index >= 0 && line.charAt(index) == '\\'; index--) {
            slashCount++;
        }
        return slashCount % 2 == 1;
    }

    private int findUnescapedEquals(String line, int start) {
        boolean escaped = false;
        for (int index = start; index < line.length(); index++) {
            char current = line.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '=') {
                return index;
            }
        }
        return -1;
    }

    private int indentation(String line) {
        int indent = 0;
        while (indent < line.length() && Character.isWhitespace(line.charAt(indent))) {
            indent++;
        }
        return indent;
    }

    private String stripLeading(String text) {
        int start = indentation(text);
        return text.substring(start);
    }

    private boolean isComment(String trimmedLine) {
        return trimmedLine.startsWith("#") || trimmedLine.startsWith("!");
    }

    private String buildYamlLikeError(String kind, LogicalLine line) {
        return "Likely YAML " + kind + " syntax at line " + line.lineNumber + ", column " + (line.indent + 1)
                + ". Nested YAML-style content is not valid in .properties files.";
    }

    private static final class LogicalLine {
        private final int lineNumber;
        private final int indent;
        private final String content;

        private LogicalLine(int lineNumber, int indent, String content) {
            this.lineNumber = lineNumber;
            this.indent = indent;
            this.content = content;
        }
    }
}
