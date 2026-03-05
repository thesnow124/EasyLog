package com.github.easylog.diff.render;

/**
 * Text rendering options for diff output.
 */
public class DiffTextRenderOptions {

    private boolean numbered = true;
    private String lineSeparator = "\n";
    private String lineTemplate = "${index}. ${field}: ${old} -> ${new}";
    private String emptyPlaceholder = "";

    public static DiffTextRenderOptions defaults() {
        return new DiffTextRenderOptions();
    }

    public boolean isNumbered() {
        return numbered;
    }

    public DiffTextRenderOptions setNumbered(boolean numbered) {
        this.numbered = numbered;
        return this;
    }

    public String getLineSeparator() {
        return lineSeparator;
    }

    public DiffTextRenderOptions setLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
        return this;
    }

    public String getLineTemplate() {
        return lineTemplate;
    }

    public DiffTextRenderOptions setLineTemplate(String lineTemplate) {
        this.lineTemplate = lineTemplate;
        return this;
    }

    public String getEmptyPlaceholder() {
        return emptyPlaceholder;
    }

    public DiffTextRenderOptions setEmptyPlaceholder(String emptyPlaceholder) {
        this.emptyPlaceholder = emptyPlaceholder;
        return this;
    }
}
