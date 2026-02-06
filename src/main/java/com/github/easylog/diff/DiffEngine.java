package com.github.easylog.diff;

/**
 * Diff引擎
 */
public interface DiffEngine {

    /**
     * 对比旧对象与新对象，返回差异结构。
     */
    DiffDTO diff(Object oldObject, Object newObject);
}
