package com.github.easylog.function;

import com.github.easylog.annotation.EasyLogDiffField;
import com.github.easylog.annotation.EasyLogDiffObject;
import com.github.easylog.configuration.EasyLogProperties;
import com.github.easylog.diff.DefaultDiffEngine;
import com.github.easylog.diff.DiffDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EasyLogParserTest {

    static class EchoBean {
        public String echo(String value) {
            return "bean:" + value;
        }
    }

    static class Target {
        String work(String name, String code) {
            return name + ":" + code;
        }
    }

    static class ObjectTarget {
        void work(Object payload) {
        }
    }

    @EasyLogDiffObject
    static class DiffUser {
        @EasyLogDiffField(alias = "age")
        private final int age;

        DiffUser(int age) {
            this.age = age;
        }
    }

    static class DiffTarget {
        void work(DiffUser oldObj, DiffUser newObj) {
        }
    }

    @Test
    void renders_spel_blocks_and_plain_expression() throws Exception {
        EasyLogParser parser = new EasyLogParser(new DefaultDiffEngine(new EasyLogProperties()));

        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("echoBean", new EchoBean());
        parser.setBeanFactory(beanFactory);

        Method method = Target.class.getDeclaredMethod("work", String.class, String.class);
        Object[] args = {"alice", "bob"};
        List<String> templates = Arrays.asList(
                "v1={{#name}}",
                "{{#name}}-{{#code}}",
                "#name",
                "{{@echoBean.echo(#name)}}"
        );

        Map<String, Object> afterMap = parser.processAfterExec(
                templates,
                java.util.Collections.<String, Object>emptyMap(),
                method,
                args,
                Target.class,
                null,
                "result");
        assertEquals("v1=alice", afterMap.get("v1={{#name}}"));
        assertEquals("alice-bob", afterMap.get("{{#name}}-{{#code}}"));
        assertEquals("alice", afterMap.get("#name"));
        assertEquals("bean:alice", afterMap.get("{{@echoBean.echo(#name)}}"));
    }

    @Test
    void returnsObjectWhenTemplateIsSingleSpelBlock() throws Exception {
        EasyLogParser parser = new EasyLogParser(new DefaultDiffEngine(new EasyLogProperties()));

        Method method = ObjectTarget.class.getDeclaredMethod("work", Object.class);
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("a", 1);
        Map<String, Object> map = parser.processAfterExec(
                Arrays.asList("{{#p0}}"), java.util.Collections.<String, Object>emptyMap(),
                method, new Object[]{payload}, ObjectTarget.class, null, null);
        Object resolved = map.get("{{#p0}}");
        assertEquals(payload, resolved);
    }

    @Test
    void resolves_diff_function_in_spel_block() throws Exception {
        EasyLogParser parser = new EasyLogParser(new DefaultDiffEngine(new EasyLogProperties()));

        Method method = DiffTarget.class.getDeclaredMethod("work", DiffUser.class, DiffUser.class);
        DiffUser oldObj = new DiffUser(1);
        DiffUser newObj = new DiffUser(2);
        Map<String, Object> map = parser.processAfterExec(
                Arrays.asList("{{DIFF(#p0,#p1)}}"), java.util.Collections.<String, Object>emptyMap(),
                method, new Object[]{oldObj, newObj}, DiffTarget.class, null, null);
        Object resolved = map.get("{{DIFF(#p0,#p1)}}");
        assertNotNull(resolved);
        assertTrue(resolved instanceof DiffDTO);
    }
}
