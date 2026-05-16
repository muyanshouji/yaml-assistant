package com.muyan.yamlassistant.workspace;

public enum WorkspaceContentType {
    YAML("YAML"),
    PROPERTIES("Properties"),
    JSON("JSON");

    private final String displayName;

    WorkspaceContentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
