package com.github.easylog.diff.render;

import com.alibaba.fastjson2.JSON;
import com.github.easylog.diff.DiffDTO;
import com.github.easylog.diff.DiffFieldDTO;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Default implementation for rendering diff text and html.
 */
public class DefaultDiffRenderer implements DiffRenderer {

    private static final String SAFE_TAG_PATTERN = "^[A-Za-z][A-Za-z0-9-]*$";

    @Override
    public DiffRenderResult render(DiffDTO diffDTO,
                                   DiffTextRenderOptions textOptions,
                                   DiffHtmlRenderOptions htmlOptions) {
        DiffTextRenderOptions resolvedTextOptions = textOptions == null ? DiffTextRenderOptions.defaults() : textOptions;
        DiffHtmlRenderOptions resolvedHtmlOptions = htmlOptions == null ? DiffHtmlRenderOptions.defaults() : htmlOptions;
        List<DiffFieldDTO> fields = diffDTO == null || diffDTO.getDiffFieldDTOList() == null
                ? Collections.emptyList()
                : diffDTO.getDiffFieldDTOList();

        DiffRenderResult result = new DiffRenderResult();
        if (fields.isEmpty()) {
            result.setText("");
            result.setHtml("");
            return result;
        }
        result.setText(renderText(fields, resolvedTextOptions));
        result.setHtml(renderHtml(fields, resolvedHtmlOptions));
        return result;
    }

    private String renderText(List<DiffFieldDTO> fields, DiffTextRenderOptions options) {
        String separator = options.getLineSeparator() == null ? "\n" : options.getLineSeparator();
        String lineTemplate = options.getLineTemplate() == null
                ? "${index}. ${field}: ${old} -> ${new}"
                : options.getLineTemplate();
        String emptyPlaceholder = options.getEmptyPlaceholder() == null ? "" : options.getEmptyPlaceholder();
        StringBuilder sb = new StringBuilder();
        int displayIndex = 1;
        for (DiffFieldDTO field : fields) {
            if (field == null) {
                continue;
            }
            String fieldName = resolveFieldName(field);
            String oldVal = stringify(field.getOldValue(), emptyPlaceholder);
            String newVal = stringify(field.getNewValue(), emptyPlaceholder);
            if (isBlank(fieldName) && isBlank(oldVal) && isBlank(newVal)) {
                continue;
            }
            String indexValue = options.isNumbered() ? String.valueOf(displayIndex) : "";
            String line = lineTemplate
                    .replace("${index}", indexValue)
                    .replace("${field}", nvl(fieldName, emptyPlaceholder))
                    .replace("${old}", nvl(oldVal, emptyPlaceholder))
                    .replace("${new}", nvl(newVal, emptyPlaceholder));
            if (!options.isNumbered()) {
                line = line.replaceFirst("^\\s*\\.?\\s*", "");
            }
            if (sb.length() > 0) {
                sb.append(separator);
            }
            sb.append(line);
            displayIndex++;
        }
        return sb.toString();
    }

    private String renderHtml(List<DiffFieldDTO> fields, DiffHtmlRenderOptions options) {
        String lineTemplate = options.getLineTemplate() == null
                ? "<span class=\"idx\">${index}.</span> <span class=\"field\">${field}</span>: <span class=\"old\">${old}</span> -> <span class=\"new\">${new}</span>"
                : options.getLineTemplate();
        String emptyPlaceholder = options.getEmptyPlaceholder() == null ? "" : options.getEmptyPlaceholder();
        String safeContainerTag = sanitizeTag(options.getContainerTag(), "div");
        String safeItemTag = sanitizeTag(options.getItemTag(), "div");

        StringBuilder items = new StringBuilder();
        int displayIndex = 1;
        for (DiffFieldDTO field : fields) {
            if (field == null) {
                continue;
            }
            String fieldName = resolveFieldName(field);
            String oldVal = stringify(field.getOldValue(), emptyPlaceholder);
            String newVal = stringify(field.getNewValue(), emptyPlaceholder);
            if (isBlank(fieldName) && isBlank(oldVal) && isBlank(newVal)) {
                continue;
            }
            String indexValue = options.isNumbered() ? String.valueOf(displayIndex) : "";
            String line = lineTemplate
                    .replace("${index}", HtmlEscaper.escape(indexValue))
                    .replace("${field}", HtmlEscaper.escape(nvl(fieldName, emptyPlaceholder)))
                    .replace("${old}", HtmlEscaper.escape(nvl(oldVal, emptyPlaceholder)))
                    .replace("${new}", HtmlEscaper.escape(nvl(newVal, emptyPlaceholder)));
            items.append(wrapTag(safeItemTag, line));
            displayIndex++;
        }
        return wrapTag(safeContainerTag, items.toString());
    }

    private String resolveFieldName(DiffFieldDTO field) {
        return firstNonBlank(field.getOldFieldAlias(), field.getNewFieldAlias(), field.getFieldName());
    }

    private String sanitizeTag(String tagName, String fallback) {
        if (tagName == null) {
            return fallback;
        }
        String trimmed = tagName.trim();
        if (trimmed.matches(SAFE_TAG_PATTERN)) {
            return trimmed;
        }
        return fallback;
    }

    private String wrapTag(String tagName, String content) {
        if (isBlank(tagName)) {
            return nvl(content, "");
        }
        return "<" + tagName + ">" + nvl(content, "") + "</" + tagName + ">";
    }

    private String stringify(Object value, String emptyPlaceholder) {
        if (value == null) {
            return emptyPlaceholder;
        }
        if (value instanceof CharSequence) {
            return value.toString();
        }
        try {
            return JSON.toJSONString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        if (value == null) {
            return true;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private String nvl(String value, String defaultValue) {
        return Objects.isNull(value) ? defaultValue : value;
    }
}
