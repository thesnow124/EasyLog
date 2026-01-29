package com.github.easylog.function;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParseFunctionFactoryTest {

    static class NamedFunction implements ParseFunction {
        private final String name;

        NamedFunction(String name) {
            this.name = name;
        }

        @Override
        public String functionName() {
            return name;
        }

        @Override
        public String apply(String value) {
            return value;
        }
    }

    static class BeforeFunction extends NamedFunction {
        BeforeFunction(String name) {
            super(name);
        }

        @Override
        public boolean executeBefore() {
            return true;
        }
    }

    @Test
    void ignoresBlankNamesAndNullFunctions() {
        ParseFunction blank = new NamedFunction(" ");
        ParseFunction valid = new NamedFunction("valid");
        ParseFunctionFactory factory = new ParseFunctionFactory(Arrays.asList(blank, null, valid));

        assertNotNull(factory.getFunction("valid"));
        assertNull(factory.getFunction(" "));
    }

    @Test
    void reportsBeforeFunctions() {
        ParseFunctionFactory factory = new ParseFunctionFactory(Collections.singletonList(new BeforeFunction("before")));
        assertTrue(factory.isBeforeFunction("before"));
        assertFalse(factory.isBeforeFunction("missing"));
    }
}
