package com.github.easylog.diff.render;

import com.github.easylog.diff.DiffDTO;

/**
 * Diff renderer SPI.
 */
public interface DiffRenderer {

    /**
     * Render diff in both text and html.
     */
    DiffRenderResult render(DiffDTO diffDTO,
                            DiffTextRenderOptions textOptions,
                            DiffHtmlRenderOptions htmlOptions);
}
