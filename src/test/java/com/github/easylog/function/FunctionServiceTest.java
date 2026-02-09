package com.github.easylog.function;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FunctionServiceTest {

    @Test
    void parseFunctionFactory_registers_valid_functions_only() {
        IParseFunction valid = new IParseFunction() {
            @Override
            public boolean executeBefore() {
                return true;
            }

            @Override
            public String functionName() {
                return "trim";
            }

            @Override
            public Object apply(Object... value) {
                Object first = (value == null || value.length == 0) ? null : value[0];
                String content = first == null ? null : String.valueOf(first);
                if (content == null) {
                    return null;
                }
                return content.trim();
            }
        };
        IParseFunction blankName = new IParseFunction() {
            @Override
            public String functionName() {
                return " ";
            }

            @Override
            public Object apply(Object... value) {
                return value;
            }
        };

        ParseFunctionFactory factory = new ParseFunctionFactory(Arrays.asList(valid, blankName));
        assertTrue(factory.isBeforeFunction("trim"));
        assertEquals(valid, factory.getFunction("trim"));
        assertNull(factory.getFunction(" "));
    }

    @Test
    void defaultFunctionService_returns_input_when_function_missing() {
        ParseFunctionFactory factory = new ParseFunctionFactory(Collections.<IParseFunction>emptyList());
        IFunctionService functionService = new DefaultFunctionServiceImpl(factory);

        assertNull(functionService.apply("upper", "alice"));
        assertFalse(functionService.beforeFunction("upper"));
    }
}
