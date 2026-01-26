package com.github.easylog.function;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一管理自定义函数 Bean。
 * <p>
 * 用户侧只需声明实现 {@link ParseFunction} 的 Spring Bean，工厂会在自动装配阶段收集并按名称暴露，
 * 供 {@link EasyLogParser} 通过函数名调用。
 */
public class ParseFunctionFactory {

    private final Map<String, ParseFunction> functionMap = new HashMap<>();

    public ParseFunctionFactory(List<ParseFunction> parseFunctions) {
        if (parseFunctions == null) {
            return;
        }
        for (ParseFunction parseFunction : parseFunctions) {
            if (parseFunction == null || StringUtils.isBlank(parseFunction.functionName())) {
                continue;
            }
            functionMap.put(parseFunction.functionName(), parseFunction);
        }
    }

    public ParseFunction getFunction(String functionName) {
        return functionMap.get(functionName);
    }

    public boolean isBeforeFunction(String functionName) {
        ParseFunction function = functionMap.get(functionName);
        return function != null && function.executeBefore();
    }
}
