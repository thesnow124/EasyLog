package com.github.easylog.context;

/**
 * Phase helper for templates. Using @easyLogPhase.after(...) in a SpEL block
 * will defer evaluation to the after-phase (post method execution) because
 * the parser skips pre-evaluation for such expressions.
 * @author gaoshuanglong
 */
public class EasyLogPhase {
    public <T> T after(T value) {
        return value;
    }
}

