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
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void renders_spel_blocks_plain_expression_and_custom_function() throws Exception {
        IParseFunction upper = new IParseFunction() {
            @Override
            public String functionName() {
                return "UPPER";
            }

            @Override
            public Object apply(Object... values) {
                if (values == null || values.length == 0 || values[0] == null) {
                    return null;
                }
                return String.valueOf(values[0]).toUpperCase();
            }
        };
        EasyLogParser parser = new EasyLogParser(functionService(upper));

        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("echoBean", new EchoBean());
        parser.setBeanFactory(beanFactory);

        Method method = Target.class.getDeclaredMethod("work", String.class, String.class);
        Object[] args = {"alice", "bob"};
        List<String> templates = Arrays.asList(
                "v1={{#name}}",
                "{{#name}}-{{#code}}",
                "#name",
                "{{@echoBean.echo(#name)}}",
                "{{UPPER(#name)}}"
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
        assertEquals("ALICE", afterMap.get("{{UPPER(#name)}}"));
    }

    @Test
    void returns_object_when_template_is_single_spel_block() throws Exception {
        EasyLogParser parser = new EasyLogParser();

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
        IParseFunction diffFunction = new DefaultDiffEngine(new EasyLogProperties());
        EasyLogParser parser = new EasyLogParser(functionService(diffFunction));

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

    @Test
    void unknown_function_returns_null_in_single_spel_block() throws Exception {
        EasyLogParser parser = new EasyLogParser(functionService());
        Method method = Target.class.getDeclaredMethod("work", String.class, String.class);

        Map<String, Object> map = parser.processAfterExec(
                Arrays.asList("{{NOT_REGISTERED(#name)}}"),
                java.util.Collections.<String, Object>emptyMap(),
                method, new Object[]{"alice", "bob"}, Target.class, null, null);

        assertTrue(map.containsKey("{{NOT_REGISTERED(#name)}}"));
        assertNull(map.get("{{NOT_REGISTERED(#name)}}"));
    }

    private IFunctionService functionService(IParseFunction... functions) {
        return new DefaultFunctionServiceImpl(new ParseFunctionFactory(Arrays.asList(functions)));
    }
}
