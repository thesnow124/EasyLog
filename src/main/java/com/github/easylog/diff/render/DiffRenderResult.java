package com.github.easylog.diff.render;

/**
 * Rendered diff output in different formats.
 */
public class DiffRenderResult {

    private String text;
    private String html;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }
}
