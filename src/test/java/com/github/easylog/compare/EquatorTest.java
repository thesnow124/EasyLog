package com.github.easylog.compare;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Sample oldObj = new Sample("2024-01-02T03:04:05", "1.2300");
        Sample newObj = new Sample("2024-01-02T03:04:06", "2.5000");
        List<FieldInfo> diff = Equator.getDiffField(oldObj, newObj);
        assertFalse(diff.isEmpty());
        assertTrue(diff.stream().anyMatch(f -> "t".equals(f.getFieldName())
                && "2024-01-02T03:04:05".equals(f.getOldFieldVal())
                && "2024-01-02T03:04:06".equals(f.getNewFieldVal())));
        assertTrue(diff.stream().anyMatch(f -> "n".equals(f.getFieldName())
                && "1.2300".equals(f.getOldFieldVal())
                && "2.5000".equals(f.getNewFieldVal())));
    }

    @Test
    void capturesListAndMapChanges() {
        SampleComplex oldObj = new SampleComplex(Arrays.asList(1, 2), new HashMap<String, String>() {{
            put("a", "1");
        }});
        SampleComplex newObj = new SampleComplex(Arrays.asList(2, 3), new HashMap<String, String>() {{
            put("a", "1");
            put("b", "2");
        }});
        List<FieldInfo> diff = Equator.getDiffField(oldObj, newObj);
        assertFalse(diff.isEmpty());
        assertTrue(diff.stream().anyMatch(f -> "list".equals(f.getFieldName())));
        assertTrue(diff.stream().anyMatch(f -> "map".equals(f.getFieldName())));
    }

    @Test
    void comparesMapObjectsDirectly() {
        Map<String, Object> oldMap = new HashMap<>();
        oldMap.put("a", 1);
        Map<String, Object> newMap = new HashMap<>();
        newMap.put("a", 2);
        List<FieldInfo> diff = Equator.getDiffField(oldMap, newMap);
        assertFalse(diff.isEmpty());
        assertEquals(1, diff.size());
        assertEquals("a", diff.get(0).getFieldName());
        assertEquals("1", diff.get(0).getOldFieldVal());
        assertEquals("2", diff.get(0).getNewFieldVal());
    }

    static class Sample {
        private final String t;
        private final String n;

        Sample(String t, String n) {
            this.t = t;
            this.n = n;
        }
    }

    static class SampleComplex {
        private final List<Integer> list;
        private final Map<String, String> map;

        SampleComplex(List<Integer> list, Map<String, String> map) {
            this.list = list;
            this.map = map;
        }
    }
}
