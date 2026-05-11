package com.muyan.yamlassistant.workspace;

public class YamlViewState {
    private String id;
    private String name;
    private String content;
    private WorkspaceContentType contentType;

    public YamlViewState() {
    }

    public YamlViewState(String id, String name, String content) {
        this(id, name, content, WorkspaceContentType.YAML);
    }

    public YamlViewState(String id, String name, String content, WorkspaceContentType contentType) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.contentType = contentType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public WorkspaceContentType getContentType() {
        return contentType != null ? contentType : WorkspaceContentType.YAML;
    }

    public void setContentType(WorkspaceContentType contentType) {
        this.contentType = contentType != null ? contentType : WorkspaceContentType.YAML;
    }

    @Override
    public String toString() {
        return name;
    }
}
