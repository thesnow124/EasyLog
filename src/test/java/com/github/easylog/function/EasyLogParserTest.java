package com.github.easylog.function;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EasyLogParserTest {

    static class BeforeFunction implements ParseFunction {
        private final AtomicInteger count;

        BeforeFunction(AtomicInteger count) {
            this.count = count;
        }

        @Override
        public String functionName() {
            return "before";
        }

        @Override
        public String apply(String value) {
            count.incrementAndGet();
            return "before:" + value;
        }

        @Override
        public boolean executeBefore() {
            return true;
        }
    }

    static class UpperFunction implements ParseFunction {
        @Override
        public String functionName() {
            return "upper";
        }

        @Override
        public String apply(String value) {
            return value == null ? "" : value.toUpperCase();
        }
    }

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

    @Test
    void processesBeforeAndAfterTemplates() throws Exception {
        AtomicInteger beforeCount = new AtomicInteger();
        ParseFunctionFactory factory = new ParseFunctionFactory(Arrays.asList(
                new BeforeFunction(beforeCount),
                new UpperFunction()
        ));
        EasyLogParser parser = new EasyLogParser(factory);

        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("echoBean", new EchoBean());
        parser.setBeanFactory(beanFactory);

        Method method = Target.class.getDeclaredMethod("work", String.class, String.class);
        Object[] args = {"alice", "bob"};
        List<String> templates = Arrays.asList(
                "v1={before{#name}}",
                "v2={upper{#code}}",
                "{{#name}}-{{#code}}",
                "#name",
                "{{@echoBean.echo(#name)}}",
                "{missing{#name}}"
        );

        Map<String, String> beforeCache = parser.processBeforeExec(templates, method, args, Target.class);
        assertEquals("before:alice", beforeCache.get("{before{#name}}"));
        assertFalse(beforeCache.containsKey("{upper{#code}}"));
        assertEquals(1, beforeCount.get());

        Map<String, String> afterMap = parser.processAfterExec(templates, beforeCache, method, args, Target.class, null, "result");
        assertEquals("v1=before:alice", afterMap.get("v1={before{#name}}"));
        assertEquals("v2=BOB", afterMap.get("v2={upper{#code}}"));
        assertEquals("alice-bob", afterMap.get("{{#name}}-{{#code}}"));
        assertEquals("alice", afterMap.get("#name"));
        assertEquals("bean:alice", afterMap.get("{{@echoBean.echo(#name)}}"));
        assertEquals("alice", afterMap.get("{missing{#name}}"));
        assertEquals(1, beforeCount.get());
    }

    @Test
    void treatsPlainSpelAsExpression() throws Exception {
        ParseFunctionFactory factory = new ParseFunctionFactory(Arrays.asList());
        EasyLogParser parser = new EasyLogParser(factory);

        Method method = Target.class.getDeclaredMethod("work", String.class, String.class);
        Map<String, String> map = parser.processAfterExec(
                Arrays.asList("#code"), java.util.Collections.<String, String>emptyMap(), method, new Object[]{"x", "y"}, Target.class, null, null);
        assertEquals("y", map.get("#code"));
    }
}
