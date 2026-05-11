package com.muyan.yamlassistant.workspace;

public enum WorkspaceContentType {
    YAML("YAML"),
    PROPERTIES("Properties");

    private final String displayName;

    WorkspaceContentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
