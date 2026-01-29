package com.github.easylog.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EasyLogContextTest {

    @AfterEach
    void tearDown() {
        EasyLogContext.clearAll();
    }

    @Test
    void putGetRequiresPush() {
        EasyLogContext.put("k1", "v1");
        assertNull(EasyLogContext.get("k1"));
        assertTrue(EasyLogContext.getVariables().isEmpty());
    }

    @Test
    void nestedScopesStayIsolated() {
        EasyLogContext.push();
        EasyLogContext.put("k1", "v1");
        EasyLogContext.push();
        EasyLogContext.put("k1", "v2");
        assertEquals("v2", EasyLogContext.get("k1"));

        EasyLogContext.pop();
        assertEquals("v1", EasyLogContext.get("k1"));

        EasyLogContext.pop();
        EasyLogContext.clearIfEmpty();
        assertTrue(EasyLogContext.getVariables().isEmpty());
    }

    @Test
    void popOnEmptyReturnsNull() {
        assertNull(EasyLogContext.pop());
    }
}
