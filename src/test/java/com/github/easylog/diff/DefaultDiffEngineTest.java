package com.github.easylog.diff;

import com.github.easylog.annotation.EasyLogDiffField;
import com.github.easylog.annotation.EasyLogDiffObject;
import com.github.easylog.configuration.EasyLogProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultDiffEngineTest {

    @EasyLogDiffObject(alias = "user")
    static class User {
        @EasyLogDiffField(alias = "年龄")
        private final Integer age;
        @EasyLogDiffField(alias = "姓名")
        private final String name;
        @EasyLogDiffField(alias = "忽略", ignored = true)
        private final String ignored;

        User(Integer age, String name, String ignored) {
            this.age = age;
            this.name = name;
            this.ignored = ignored;
        }
    }

    @EasyLogDiffObject(alias = "oldUser")
    static class OldUser {
        @EasyLogDiffField(alias = "旧名")
        private final String name;

        OldUser(String name) {
            this.name = name;
        }
    }

    @EasyLogDiffObject(alias = "newUser")
    static class NewUser {
        @EasyLogDiffField(alias = "新名")
        private final String name;

        NewUser(String name) {
            this.name = name;
        }
    }

    @Test
    void diffs_same_class_fields_with_alias() {
        DefaultDiffEngine engine = new DefaultDiffEngine(new EasyLogProperties());
        User oldUser = new User(1, "alice", "x");
        User newUser = new User(2, "alice", "y");

        DiffDTO diff = engine.diff(oldUser, newUser);

        assertNotNull(diff);
        List<DiffFieldDTO> fields = diff.getDiffFieldDTOList();
        assertEquals(1, fields.size());
        DiffFieldDTO field = fields.get(0);
        assertEquals("age", field.getFieldName());
        assertEquals("年龄", field.getOldFieldAlias());
        assertEquals("年龄", field.getNewFieldAlias());
        assertEquals(1, field.getOldValue());
        assertEquals(2, field.getNewValue());
    }

    @Test
    void preserves_old_new_alias_for_cross_class_diff() {
        DefaultDiffEngine engine = new DefaultDiffEngine(new EasyLogProperties());
        OldUser oldUser = new OldUser("leo");
        NewUser newUser = new NewUser("max");

        DiffDTO diff = engine.diff(oldUser, newUser);

        assertNotNull(diff);
        List<DiffFieldDTO> fields = diff.getDiffFieldDTOList();
        assertEquals(1, fields.size());
        DiffFieldDTO field = fields.get(0);
        assertEquals("name", field.getFieldName());
        assertEquals("旧名", field.getOldFieldAlias());
        assertEquals("新名", field.getNewFieldAlias());
    }

    @Test
    void ignores_null_values_when_configured() {
        EasyLogProperties properties = new EasyLogProperties();
        properties.setDiffIgnoreOldObjectNullValue(true);
        properties.setDiffIgnoreNewObjectNullValue(true);
        DefaultDiffEngine engine = new DefaultDiffEngine(properties);

        User oldUser = new User(null, "alice", "x");
        User newUser = new User(1, null, "y");

        DiffDTO diff = engine.diff(oldUser, newUser);
        assertNotNull(diff);
        assertTrue(diff.getDiffFieldDTOList().isEmpty());
    }
}
