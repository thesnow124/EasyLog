package com.github.easylog.function;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EasyLogParserTest {

    static class Demo {
        public void demo(String name) {}
    }

    static class BeforeFunction implements ParseFunction {
        @Override
        public String functionName() {
            return "before";
        }

        @Override
        public String apply(String value) {
            return "before-" + value;
        }

        @Override
        public boolean executeBefore() {
            return true;
        }
    }

    static class AfterFunction implements ParseFunction {
        @Override
        public String functionName() {
            return "after";
        }

        @Override
        public String apply(String value) {
            return "after-" + value;
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

    @Test
    void shouldParseBeforeAndAfterFunctions() throws Exception {
        ParseFunctionFactory factory = new ParseFunctionFactory(
                List.of(new BeforeFunction(), new AfterFunction(), new UpperFunction()));
        EasyLogParser parser = new EasyLogParser(factory);
        Method method = Demo.class.getMethod("demo", String.class);
        Object[] args = new Object[]{"alice"};
        List<String> templates = List.of("{before{#p0}}", "{after{#p0}}", "{upper{#p0}}", "#p0");

        Map<String, String> beforeCache = parser.processBeforeExec(templates, method, args, Demo.class);
        assertEquals("before-alice", beforeCache.get("{before{#p0}}"));
        assertTrue(beforeCache.size() == 1, "only before-exec function should be cached");

        Map<String, String> rendered = parser.processAfterExec(templates, beforeCache, method, args, Demo.class, null, null);
        assertEquals("before-alice", rendered.get("{before{#p0}}"));
        assertEquals("after-alice", rendered.get("{after{#p0}}"));
        assertEquals("ALICE", rendered.get("{upper{#p0}}"));
        assertEquals("alice", rendered.get("#p0"));
    }
}
