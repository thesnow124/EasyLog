package com.github.easylog.diff.render;

/**
 * Html rendering options for diff output.
 */
public class DiffHtmlRenderOptions {

    private boolean numbered = true;
    private String containerTag = "div";
    private String itemTag = "div";
    private String lineTemplate = "<span class=\"idx\">${index}.</span> <span class=\"field\">${field}</span>: <span class=\"old\">${old}</span> -> <span class=\"new\">${new}</span>";
    private String emptyPlaceholder = "";

    public static DiffHtmlRenderOptions defaults() {
        return new DiffHtmlRenderOptions();
    }

    public boolean isNumbered() {
        return numbered;
    }

    public DiffHtmlRenderOptions setNumbered(boolean numbered) {
        this.numbered = numbered;
        return this;
    }

    public String getContainerTag() {
        return containerTag;
    }

    public DiffHtmlRenderOptions setContainerTag(String containerTag) {
        this.containerTag = containerTag;
        return this;
    }

    public String getItemTag() {
        return itemTag;
    }

    public DiffHtmlRenderOptions setItemTag(String itemTag) {
        this.itemTag = itemTag;
        return this;
    }

    public String getLineTemplate() {
        return lineTemplate;
    }

    public DiffHtmlRenderOptions setLineTemplate(String lineTemplate) {
        this.lineTemplate = lineTemplate;
        return this;
    }

    public String getEmptyPlaceholder() {
        return emptyPlaceholder;
    }

    public DiffHtmlRenderOptions setEmptyPlaceholder(String emptyPlaceholder) {
        this.emptyPlaceholder = emptyPlaceholder;
        return this;
    }
}
