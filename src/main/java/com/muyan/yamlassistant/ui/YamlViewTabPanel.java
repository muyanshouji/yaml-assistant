package com.muyan.yamlassistant.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.json.JsonFileType;
import com.intellij.lang.properties.PropertiesFileType;
import com.intellij.ui.EditorTextField;
import com.muyan.yamlassistant.services.JsonValidatorService;
import com.muyan.yamlassistant.services.PropertiesValidatorService;
import com.muyan.yamlassistant.services.YamlParserService;
import com.muyan.yamlassistant.workspace.YamlViewState;
import com.muyan.yamlassistant.workspace.WorkspaceContentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

import static com.intellij.util.ui.JBUI.Borders;

public class YamlViewTabPanel {

    private static final Color READY_COLOR = new Color(0x80889A);
    private static final Color SUCCESS_COLOR = new Color(0x7BC275);
    private static final Color ERROR_COLOR = new Color(0xE06C75);

    private final JPanel mainPanel;
    private final JPanel statusPanel;
    private final JPanel statusIndicator;
    private final JLabel statusLabel;
    private final Project project;
    private final Document document;
    private final JComponent editorComponent;
    private final EditorTextField editorField;
    private final JTextArea fallbackTextArea;
    private final YamlViewState viewState;
    private final YamlParserService parserService;
    private final PropertiesValidatorService propertiesValidatorService;
    private final JsonValidatorService jsonValidatorService;
    private final boolean showLineNumbers;
    private RangeHighlighter errorHighlighter;
    private final Consumer<String> onContentChanged;

    public YamlViewTabPanel(YamlViewState viewState,
                            Project project,
                            YamlParserService parserService,
                            Consumer<String> onContentChanged) {
        this(viewState, project, parserService, true, onContentChanged);
    }

    public YamlViewTabPanel(YamlViewState viewState,
                            Project project,
                            YamlParserService parserService,
                            boolean showLineNumbers,
                            Consumer<String> onContentChanged) {
        this.viewState = viewState;
        this.project = project;
        this.parserService = parserService;
        this.propertiesValidatorService = new PropertiesValidatorService();
        this.jsonValidatorService = new JsonValidatorService();
        this.showLineNumbers = showLineNumbers;
        this.onContentChanged = onContentChanged;

        mainPanel = new JPanel(new BorderLayout());

        String initialContent = viewState.getContent() != null ? viewState.getContent() : "";
        FileType fileType = getFileType();
        if (ApplicationManager.getApplication() != null) {
            document = EditorFactory.getInstance().createDocument(initialContent);
            editorField = new EditorTextField(document, project, fileType, false, false);
            configureEditorField(editorField);
            editorComponent = editorField;
            fallbackTextArea = null;
        } else {
            document = null;
            editorField = null;
            JTextArea textArea = new JTextArea(initialContent);
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, textArea.getFont().getSize()));
            textArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent e) {
                    syncContent();
                }

                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent e) {
                    syncContent();
                }

                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent e) {
                    syncContent();
                }
            });
            fallbackTextArea = textArea;
            editorComponent = new JScrollPane(textArea);
        }

        statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0x3A3D52)),
                Borders.empty(6, 10)
        ));
        statusPanel.setPreferredSize(new Dimension(0, 36));
        statusIndicator = new JPanel();
        statusIndicator.setPreferredSize(new Dimension(10, 10));
        statusIndicator.setMinimumSize(new Dimension(10, 10));
        statusIndicator.setMaximumSize(new Dimension(10, 10));
        statusIndicator.setOpaque(true);

        statusLabel = new JLabel();
        statusLabel.setBorder(Borders.empty(4, 8));
        applyTheme();

        statusPanel.add(statusIndicator);
        statusPanel.add(statusLabel);

        mainPanel.add(editorComponent, BorderLayout.CENTER);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        if (document != null) {
            document.addDocumentListener(new DocumentListener() {
                @Override
                public void documentChanged(@NotNull DocumentEvent event) {
                    syncContent();
                }
            });
        }

        refreshStatus();
    }

    private void configureEditorField(EditorTextField editorField) {
        editorField.setOneLineMode(false);
        editorField.setBorder(Borders.empty());
        editorField.addSettingsProvider(editor -> {
            editor.setHorizontalScrollbarVisible(false);
            editor.setVerticalScrollbarVisible(true);
            editor.setOneLineMode(false);

            // Keep the workspace editor visually closer to a normal IDEA file editor.
            editor.getSettings().setLineNumbersShown(showLineNumbers);
            editor.getSettings().setLineMarkerAreaShown(showLineNumbers);
            editor.getSettings().setFoldingOutlineShown(true);
            editor.getSettings().setIndentGuidesShown(true);
            editor.getSettings().setCaretRowShown(true);
            editor.getSettings().setAdditionalColumnsCount(2);
            editor.getSettings().setAdditionalLinesCount(1);
        });
    }

    private void applyTheme() {
        Color editorBackground = getEditorBackground();
        Color panelBackground = getPanelBackground();
        Color labelForeground = getLabelForeground();
        Color separatorColor = getSeparatorColor();

        mainPanel.setBackground(editorBackground);
        editorComponent.setBackground(editorBackground);
        if (editorField != null) {
            editorField.setBackground(editorBackground);
        }
        if (fallbackTextArea != null) {
            fallbackTextArea.setBackground(editorBackground);
            fallbackTextArea.setForeground(labelForeground);
            fallbackTextArea.setCaretColor(labelForeground);
            fallbackTextArea.setSelectionColor(UIManager.getColor("TextArea.selectionBackground"));
            fallbackTextArea.setSelectedTextColor(UIManager.getColor("TextArea.selectionForeground"));
            fallbackTextArea.setBorder(Borders.empty(8));
        }

        statusPanel.setOpaque(true);
        statusPanel.setBackground(panelBackground);
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, separatorColor),
                Borders.empty(6, 10)
        ));
        statusLabel.setOpaque(false);
        statusLabel.setForeground(labelForeground);
    }

    private Color getEditorBackground() {
        if (ApplicationManager.getApplication() != null) {
            Color background = EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground();
            if (background != null) {
                return background;
            }
        }

        Color background = UIManager.getColor("EditorPane.background");
        if (background != null) {
            return background;
        }

        background = UIManager.getColor("TextArea.background");
        return background != null ? background : Color.WHITE;
    }

    private Color getPanelBackground() {
        Color background = UIManager.getColor("Panel.background");
        return background != null ? background : getEditorBackground();
    }

    private Color getLabelForeground() {
        Color foreground = UIManager.getColor("Label.foreground");
        return foreground != null ? foreground : Color.BLACK;
    }

    private Color getSeparatorColor() {
        Color separator = UIManager.getColor("Component.borderColor");
        if (separator != null) {
            return separator;
        }

        separator = UIManager.getColor("Separator.foreground");
        return separator != null ? separator : new Color(0x3A3D52);
    }

    private void syncContent() {
        String content = getContent();
        viewState.setContent(content);
        onContentChanged.accept(content);
        refreshStatus();
    }

    private void refreshStatus() {
        String content = getContent();
        if (content.trim().isEmpty()) {
            statusLabel.setText("Ready");
            statusIndicator.setBackground(READY_COLOR);
            clearErrorHighlight();
            return;
        }

        String validation = validateContent(content);
        if (validation == null) {
            statusLabel.setText("Parsed successfully");
            statusIndicator.setBackground(SUCCESS_COLOR);
            clearErrorHighlight();
        } else {
            statusLabel.setText(getValidationErrorLabel());
            statusIndicator.setBackground(ERROR_COLOR);
            updateErrorHighlight(validation);
        }
    }

    private String validateContent(String content) {
        if (viewState.getContentType() == WorkspaceContentType.PROPERTIES) {
            return propertiesValidatorService.validate(content);
        }
        if (viewState.getContentType() == WorkspaceContentType.JSON) {
            return jsonValidatorService.validate(content);
        }
        return parserService.validate(content);
    }

    private String getValidationErrorLabel() {
        if (viewState.getContentType() == WorkspaceContentType.PROPERTIES) {
            return "Properties error";
        }
        if (viewState.getContentType() == WorkspaceContentType.JSON) {
            return "JSON error";
        }
        return "YAML error";
    }

    private FileType getFileType() {
        if (viewState.getContentType() == WorkspaceContentType.PROPERTIES) {
            return PropertiesFileType.INSTANCE;
        }
        if (viewState.getContentType() == WorkspaceContentType.JSON) {
            return JsonFileType.INSTANCE;
        }
        return YAMLFileType.YML;
    }

    private void updateErrorHighlight(String validation) {
        if (editorField == null) {
            return;
        }

        EditorEx editor = editorField.getEditor(true);
        if (editor == null) {
            return;
        }

        clearErrorHighlight();

        int offset = findErrorOffset(validation, document.getText());
        int endOffset = Math.min(document.getTextLength(), Math.max(offset + 1, findLineEndOffset(offset, document.getText())));
        errorHighlighter = editor.getMarkupModel().addRangeHighlighter(
                offset,
                endOffset,
                HighlighterLayer.ERROR,
                new TextAttributes(null, null, ERROR_COLOR, EffectType.WAVE_UNDERSCORE, Font.PLAIN),
                HighlighterTargetArea.EXACT_RANGE
        );
        errorHighlighter.setErrorStripeMarkColor(ERROR_COLOR);
        errorHighlighter.setErrorStripeTooltip(validation);
    }

    private void clearErrorHighlight() {
        if (errorHighlighter == null || editorField == null) {
            errorHighlighter = null;
            return;
        }

        EditorEx editor = editorField.getEditor(false);
        if (editor != null) {
            editor.getMarkupModel().removeHighlighter(errorHighlighter);
        }
        errorHighlighter = null;
    }

    private int findErrorOffset(String validation, String content) {
        int line = extractMarker(validation, "line ");
        int column = extractMarker(validation, "column ");
        if (line <= 0 || column <= 0) {
            return 0;
        }

        int currentLine = 1;
        int currentColumn = 1;
        for (int index = 0; index < content.length(); index++) {
            if (currentLine == line && currentColumn == column) {
                return index;
            }

            char current = content.charAt(index);
            if (current == '\n') {
                currentLine++;
                currentColumn = 1;
            } else {
                currentColumn++;
            }
        }

        return Math.max(0, content.length() - 1);
    }

    private int extractMarker(String validation, String marker) {
        int markerIndex = validation.indexOf(marker);
        if (markerIndex < 0) {
            return -1;
        }

        int start = markerIndex + marker.length();
        int end = start;
        while (end < validation.length() && Character.isDigit(validation.charAt(end))) {
            end++;
        }
        if (start == end) {
            return -1;
        }

        try {
            return Integer.parseInt(validation.substring(start, end));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private int findLineEndOffset(int startOffset, String content) {
        int index = startOffset;
        while (index < content.length() && content.charAt(index) != '\n') {
            index++;
        }
        return index;
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    void setContent(String content) {
        if (document != null) {
            WriteCommandAction.runWriteCommandAction(null, () -> document.setText(content));
        } else if (fallbackTextArea != null) {
            fallbackTextArea.setText(content);
        }
    }

    JComponent getEditorComponent() {
        return editorComponent;
    }

    JLabel getStatusLabel() {
        return statusLabel;
    }

    JPanel getStatusIndicator() {
        return statusIndicator;
    }

    JPanel getStatusPanel() {
        return statusPanel;
    }

    boolean isLineNumbersShown() {
        if (editorField == null) {
            return showLineNumbers;
        }

        EditorEx editor = editorField.getEditor(false);
        if (editor == null) {
            return showLineNumbers;
        }
        return editor.getSettings().isLineNumbersShown();
    }

    private String getContent() {
        if (document != null) {
            return document.getText();
        }
        return fallbackTextArea != null ? fallbackTextArea.getText() : "";
    }
}
