package com.github.easylog.diff;

import com.github.easylog.diff.render.DefaultDiffRenderer;
import com.github.easylog.diff.render.DiffRenderResult;
import com.github.easylog.diff.render.DiffTextRenderOptions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultDiffRendererTest {

    @Test
    void render_text_multiline_numbered() {
        DiffFieldDTO field1 = new DiffFieldDTO();
        field1.setOldFieldAlias("Name");
        field1.setOldValue("old");
        field1.setNewValue("new");

        DiffFieldDTO field2 = new DiffFieldDTO();
        field2.setFieldName("qty");
        field2.setOldValue(1);
        field2.setNewValue(2);

        DiffDTO diffDTO = new DiffDTO();
        diffDTO.setDiffFieldDTOList(Arrays.asList(field1, field2));

        DiffRenderResult result = new DefaultDiffRenderer().render(diffDTO, DiffTextRenderOptions.defaults(), null);

        assertTrue(result.getText().contains("1. Name: old -> new"));
        assertTrue(result.getText().contains("2. qty: 1 -> 2"));
        assertTrue(result.getText().contains("\n"));
    }

    @Test
    void render_field_alias_fallback_order() {
        DiffFieldDTO field1 = new DiffFieldDTO();
        field1.setOldFieldAlias("oldAlias");
        field1.setNewFieldAlias("newAlias");
        field1.setFieldName("field");
        field1.setOldValue("a");
        field1.setNewValue("b");

        DiffFieldDTO field2 = new DiffFieldDTO();
        field2.setOldFieldAlias("");
        field2.setNewFieldAlias("newAliasOnly");
        field2.setFieldName("field2");
        field2.setOldValue("x");
        field2.setNewValue("y");

        DiffFieldDTO field3 = new DiffFieldDTO();
        field3.setFieldName("field3");
        field3.setOldValue("m");
        field3.setNewValue("n");

        DiffDTO diffDTO = new DiffDTO();
        diffDTO.setDiffFieldDTOList(Arrays.asList(field1, field2, field3));

        DiffRenderResult result = new DefaultDiffRenderer().render(diffDTO, DiffTextRenderOptions.defaults(), null);

        assertTrue(result.getText().contains("oldAlias: a -> b"));
        assertTrue(result.getText().contains("newAliasOnly: x -> y"));
        assertTrue(result.getText().contains("field3: m -> n"));
    }

    @Test
    void render_empty_diff_returns_empty() {
        DiffDTO empty = new DiffDTO();
        empty.setDiffFieldDTOList(Collections.emptyList());

        DiffRenderResult withNull = new DefaultDiffRenderer().render(null, DiffTextRenderOptions.defaults(), null);
        DiffRenderResult withEmpty = new DefaultDiffRenderer().render(empty, DiffTextRenderOptions.defaults(), null);

        assertEquals("", withNull.getText());
        assertEquals("", withNull.getHtml());
        assertEquals("", withEmpty.getText());
        assertEquals("", withEmpty.getHtml());
    }

    @Test
    void render_html_escapes_untrusted_values() {
        DiffFieldDTO field = new DiffFieldDTO();
        field.setOldFieldAlias("<b>field</b>");
        field.setOldValue("<script>alert(1)</script>");
        field.setNewValue("x&y");

        DiffDTO diffDTO = new DiffDTO();
        diffDTO.setDiffFieldDTOList(Collections.singletonList(field));

        DiffRenderResult result = new DefaultDiffRenderer().render(diffDTO, DiffTextRenderOptions.defaults(), null);

        assertTrue(result.getHtml().contains("&lt;b&gt;field&lt;/b&gt;"));
        assertTrue(result.getHtml().contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertTrue(result.getHtml().contains("x&amp;y"));
        assertFalse(result.getHtml().contains("<script>alert(1)</script>"));
    }
}
