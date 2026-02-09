package com.github.easylog.function;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 函数工厂：收集并管理所有 {@link IParseFunction}。
 */
public class ParseFunctionFactory {

    private final Map<String, IParseFunction> allFunctionMap;

    public ParseFunctionFactory(List<IParseFunction> parseFunctions) {
        if (CollectionUtils.isEmpty(parseFunctions)) {
            this.allFunctionMap = Collections.emptyMap();
            return;
        }
        Map<String, IParseFunction> map = new HashMap<>();
        for (IParseFunction parseFunction : parseFunctions) {
            if (parseFunction == null || !StringUtils.hasText(parseFunction.functionName())) {
                continue;
            }
            map.put(parseFunction.functionName().trim(), parseFunction);
        }
        this.allFunctionMap = map;
    }

    public IParseFunction getFunction(String functionName) {
        if (!StringUtils.hasText(functionName)) {
            return null;
        }
        return allFunctionMap.get(functionName.trim());
    }

    public boolean isBeforeFunction(String functionName) {
        IParseFunction function = allFunctionMap.get(functionName);
        return function != null && function.executeBefore();
    }
}
