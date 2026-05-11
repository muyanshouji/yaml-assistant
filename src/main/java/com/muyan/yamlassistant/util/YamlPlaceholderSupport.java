package com.muyan.yamlassistant.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YamlPlaceholderSupport {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("@[A-Za-z0-9._-]+@");

    public SanitizedYaml sanitize(String yamlText) {
        if (yamlText == null || yamlText.indexOf('@') < 0) {
            return new SanitizedYaml(yamlText, Collections.emptyMap());
        }

        LinkedHashMap<String, String> replacements = new LinkedHashMap<>();
        StringBuilder sanitized = new StringBuilder(yamlText.length());
        int nextTokenIndex = 0;
        int start = 0;
        while (start < yamlText.length()) {
            int lineEnd = yamlText.indexOf('\n', start);
            boolean hasNewline = lineEnd >= 0;
            if (!hasNewline) {
                lineEnd = yamlText.length();
            }

            String newline = hasNewline ? "\n" : "";
            String line = yamlText.substring(start, lineEnd);
            if (line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
                newline = hasNewline ? "\r\n" : "\r";
            }

            LineSanitization lineSanitization = sanitizeLine(line, replacements, nextTokenIndex);
            sanitized.append(lineSanitization.getLine()).append(newline);
            nextTokenIndex = lineSanitization.getNextTokenIndex();
            start = lineEnd + 1;
        }

        if (replacements.isEmpty()) {
            return new SanitizedYaml(yamlText, Collections.emptyMap());
        }
        return new SanitizedYaml(sanitized.toString(), replacements);
    }

    public String restoreText(String text, SanitizedYaml sanitizedYaml) {
        if (text == null || sanitizedYaml == null || sanitizedYaml.getReplacements().isEmpty()) {
            return text;
        }

        String restored = text;
        for (Map.Entry<String, String> entry : sanitizedYaml.getReplacements().entrySet()) {
            restored = restored.replace(entry.getKey(), entry.getValue());
        }
        return restored;
    }

    public Object restoreObject(Object value, SanitizedYaml sanitizedYaml) {
        if (value == null || sanitizedYaml == null || sanitizedYaml.getReplacements().isEmpty()) {
            return value;
        }

        if (value instanceof String text) {
            return restoreText(text, sanitizedYaml);
        }

        if (value instanceof List<?> list) {
            List<Object> restored = new ArrayList<>(list.size());
            for (Object item : list) {
                restored.add(restoreObject(item, sanitizedYaml));
            }
            return restored;
        }

        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> restored = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object restoredKey = entry.getKey() instanceof String text
                        ? restoreText(text, sanitizedYaml)
                        : entry.getKey();
                restored.put(restoredKey, restoreObject(entry.getValue(), sanitizedYaml));
            }
            return restored;
        }

        return value;
    }

    private LineSanitization sanitizeLine(String line,
                                          LinkedHashMap<String, String> replacements,
                                          int nextTokenIndex) {
        int scalarStart = findScalarStart(line);
        if (scalarStart < 0) {
            return new LineSanitization(line, nextTokenIndex);
        }

        char scalarStartChar = line.charAt(scalarStart);
        if (scalarStartChar == '\'' || scalarStartChar == '"') {
            return new LineSanitization(line, nextTokenIndex);
        }

        int commentStart = findCommentStart(line, scalarStart);
        String scalarSegment = line.substring(scalarStart, commentStart);
        if (scalarSegment.indexOf('@') < 0) {
            return new LineSanitization(line, nextTokenIndex);
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(scalarSegment);
        StringBuilder replacedSegment = new StringBuilder(scalarSegment.length());
        int cursor = 0;
        boolean changed = false;
        while (matcher.find()) {
            changed = true;
            replacedSegment.append(scalarSegment, cursor, matcher.start());
            String placeholder = matcher.group();
            String token = createToken(placeholder.length(), nextTokenIndex++);
            replacements.put(token, placeholder);
            replacedSegment.append(token);
            cursor = matcher.end();
        }

        if (!changed) {
            return new LineSanitization(line, nextTokenIndex);
        }

        replacedSegment.append(scalarSegment.substring(cursor));
        return new LineSanitization(
                line.substring(0, scalarStart) + replacedSegment + line.substring(commentStart),
                nextTokenIndex
        );
    }

    private int findScalarStart(String line) {
        int contentStart = firstNonWhitespace(line, 0);
        if (contentStart < 0) {
            return -1;
        }

        char first = line.charAt(contentStart);
        if (first == '#') {
            return -1;
        }

        if (first == '-' && (contentStart + 1 >= line.length() || Character.isWhitespace(line.charAt(contentStart + 1)))) {
            return firstNonWhitespace(line, contentStart + 1);
        }

        int mappingValueStart = findMappingValueStart(line, contentStart);
        if (mappingValueStart >= 0) {
            return mappingValueStart;
        }

        return contentStart;
    }

    private int findMappingValueStart(String line, int contentStart) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int index = contentStart; index < line.length(); index++) {
            char current = line.charAt(index);
            if (inSingleQuote) {
                if (current == '\'') {
                    inSingleQuote = false;
                }
                continue;
            }

            if (inDoubleQuote) {
                if (current == '"' && !isEscaped(line, index)) {
                    inDoubleQuote = false;
                }
                continue;
            }

            if (current == '\'') {
                inSingleQuote = true;
                continue;
            }

            if (current == '"') {
                inDoubleQuote = true;
                continue;
            }

            if (current == '#' && (index == contentStart || Character.isWhitespace(line.charAt(index - 1)))) {
                break;
            }

            if (current == ':' && (index + 1 >= line.length() || Character.isWhitespace(line.charAt(index + 1)))) {
                return firstNonWhitespace(line, index + 1);
            }
        }

        return -1;
    }

    private int findCommentStart(String line, int start) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int index = start; index < line.length(); index++) {
            char current = line.charAt(index);
            if (inSingleQuote) {
                if (current == '\'') {
                    inSingleQuote = false;
                }
                continue;
            }

            if (inDoubleQuote) {
                if (current == '"' && !isEscaped(line, index)) {
                    inDoubleQuote = false;
                }
                continue;
            }

            if (current == '\'') {
                inSingleQuote = true;
                continue;
            }

            if (current == '"') {
                inDoubleQuote = true;
                continue;
            }

            if (current == '#' && (index == start || Character.isWhitespace(line.charAt(index - 1)))) {
                return index;
            }
        }

        return line.length();
    }

    private int firstNonWhitespace(String line, int start) {
        for (int index = start; index < line.length(); index++) {
            if (!Character.isWhitespace(line.charAt(index))) {
                return index;
            }
        }
        return -1;
    }

    private boolean isEscaped(String text, int index) {
        int slashCount = 0;
        for (int cursor = index - 1; cursor >= 0 && text.charAt(cursor) == '\\'; cursor--) {
            slashCount++;
        }
        return slashCount % 2 == 1;
    }

    private String createToken(int length, int index) {
        String value = Integer.toString(index, 36);
        StringBuilder token = new StringBuilder(length);
        token.append('y');
        int maxIndexChars = Math.max(1, length - 2);
        if (value.length() > maxIndexChars) {
            value = value.substring(value.length() - maxIndexChars);
        }
        token.append(value);
        while (token.length() < length) {
            token.append('x');
        }
        return token.toString();
    }

    public static final class SanitizedYaml {
        private final String text;
        private final Map<String, String> replacements;

        public SanitizedYaml(String text, Map<String, String> replacements) {
            this.text = text;
            this.replacements = replacements;
        }

        public String getText() {
            return text;
        }

        public Map<String, String> getReplacements() {
            return replacements;
        }
    }

    private static final class LineSanitization {
        private final String line;
        private final int nextTokenIndex;

        private LineSanitization(String line, int nextTokenIndex) {
            this.line = line;
            this.nextTokenIndex = nextTokenIndex;
        }

        public String getLine() {
            return line;
        }

        public int getNextTokenIndex() {
            return nextTokenIndex;
        }
    }
}
