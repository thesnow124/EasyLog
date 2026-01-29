package com.github.easylog.compare;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquatorTest {

    @Test
    void returnsEmptyWhenBothNull() {
        List<FieldInfo> diff = Equator.getDiffField(null, null);
        assertTrue(diff.isEmpty());
    }

    @Test
    void nonJsonFallsBackToSingleField() {
        List<FieldInfo> diff = Equator.getDiffField("plain", "next");
        assertEquals(1, diff.size());
        assertEquals("plain", diff.get(0).getOldFieldVal());
        assertEquals("next", diff.get(0).getNewFieldVal());
    }

    @Test
    void keepsRawDateAndNumberValues() {
        String oldJson = "{\"t\":\"2024-01-02T03:04:05\",\"n\":\"1.2300\"}";
        String newJson = "{\"t\":\"2024-01-02T03:04:06\",\"n\":\"2.5000\"}";
        List<FieldInfo> diff = Equator.getDiffField(oldJson, newJson);
        assertFalse(diff.isEmpty());
        assertTrue(diff.stream().anyMatch(f -> "2024-01-02T03:04:05".equals(f.getOldFieldVal())));
        assertTrue(diff.stream().anyMatch(f -> "2024-01-02T03:04:06".equals(f.getNewFieldVal())));
        assertTrue(diff.stream().anyMatch(f -> "1.2300".equals(f.getOldFieldVal())));
        assertTrue(diff.stream().anyMatch(f -> "2.5000".equals(f.getNewFieldVal())));
    }

    @Test
    void capturesListAndMapChanges() {
        String oldJson = "{\"list\":[1,2],\"map\":{\"a\":\"1\"}}";
        String newJson = "{\"list\":[2,3],\"map\":{\"a\":\"1\",\"b\":\"2\"}}";
        List<FieldInfo> diff = Equator.getDiffField(oldJson, newJson);
        assertFalse(diff.isEmpty());
        assertTrue(diff.stream().anyMatch(f -> f.getFieldName() != null && f.getFieldName().contains("list")));
        assertTrue(diff.stream().anyMatch(f -> f.getFieldName() != null && f.getFieldName().contains("map")));
    }
}
