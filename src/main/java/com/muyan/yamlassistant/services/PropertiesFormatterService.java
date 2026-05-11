package com.muyan.yamlassistant.services;

public class PropertiesFormatterService {

    private final PropertiesValidatorService validatorService = new PropertiesValidatorService();

    public String beautify(String text) {
        String validation = validatorService.validateLenient(text);
        if (validation != null) {
            throw new IllegalArgumentException(validation);
        }

        String normalizedText = normalizeLineSeparators(text);
        if (normalizedText.isEmpty()) {
            return normalizedText;
        }

        String[] lines = normalizedText.split("\n", -1);
        StringBuilder formatted = new StringBuilder(normalizedText.length());
        for (int index = 0; index < lines.length; index++) {
            if (index > 0) {
                formatted.append('\n');
            }

            if (isContinuationStart(lines, index)) {
                int end = index;
                formatted.append(lines[end]);
                while (endsWithContinuation(lines[end]) && end + 1 < lines.length) {
                    end++;
                    formatted.append('\n').append(lines[end]);
                }
                index = end;
                continue;
            }

            formatted.append(formatSingleLine(lines[index]));
        }
        return formatted.toString();
    }

    private String formatSingleLine(String line) {
        int contentStart = firstNonWhitespace(line);
        if (contentStart < 0) {
            return "";
        }

        char first = line.charAt(contentStart);
        if (first == '#' || first == '!') {
            return line;
        }

        int keyEnd = line.length();
        boolean escaped = false;
        for (int index = contentStart; index < line.length(); index++) {
            char current = line.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '=' || current == ':') {
                keyEnd = index;
                int valueStart = skipValuePrefix(line, index + 1);
                return trimUnescapedTrailingWhitespace(line.substring(contentStart, keyEnd)) + "=" + line.substring(valueStart);
            }
            if (Character.isWhitespace(current)) {
                keyEnd = index;
                int valueStart = skipValuePrefix(line, index);
                return line.substring(contentStart, keyEnd) + "=" + line.substring(valueStart);
            }
        }

        return trimUnescapedTrailingWhitespace(line.substring(contentStart)) + "=";
    }

    private int skipValuePrefix(String line, int index) {
        int cursor = index;
        while (cursor < line.length() && Character.isWhitespace(line.charAt(cursor))) {
            cursor++;
        }
        if (cursor < line.length() && (line.charAt(cursor) == '=' || line.charAt(cursor) == ':')) {
            cursor++;
        }
        while (cursor < line.length() && Character.isWhitespace(line.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }

    private boolean isContinuationStart(String[] lines, int index) {
        return index < lines.length && endsWithContinuation(lines[index]);
    }

    private boolean endsWithContinuation(String line) {
        int slashCount = 0;
        for (int index = line.length() - 1; index >= 0 && line.charAt(index) == '\\'; index--) {
            slashCount++;
        }
        return slashCount % 2 == 1;
    }

    private int firstNonWhitespace(String line) {
        for (int index = 0; index < line.length(); index++) {
            if (!Character.isWhitespace(line.charAt(index))) {
                return index;
            }
        }
        return -1;
    }

    private String trimUnescapedTrailingWhitespace(String text) {
        int end = text.length();
        while (end > 0 && Character.isWhitespace(text.charAt(end - 1))) {
            int slashCount = 0;
            for (int index = end - 2; index >= 0 && text.charAt(index) == '\\'; index--) {
                slashCount++;
            }
            if (slashCount % 2 == 1) {
                break;
            }
            end--;
        }
        return text.substring(0, end);
    }

    private String normalizeLineSeparators(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }
}
