package com.github.easylog.context;

import com.github.easylog.diff.DiffDTO;
import com.github.easylog.diff.DiffEngine;

/**
 * SpEL Root object: expose built-in functions for templates.
 */
public class EasyLogSpelRoot {

    private final DiffEngine diffEngine;

    public EasyLogSpelRoot(DiffEngine diffEngine) {
        this.diffEngine = diffEngine;
    }

    /**
     * Built-in DIFF function: DIFF(oldObj, newObj)
     */
    public DiffDTO DIFF(Object oldObj, Object newObj) {
        if (diffEngine == null) {
            return null;
        }
        try {
            return diffEngine.diff(oldObj, newObj);
        } catch (Exception ignore) {
            return null;
        }
    }
}
