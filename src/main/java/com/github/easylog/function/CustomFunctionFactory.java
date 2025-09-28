package com.github.easylog.function;

import com.github.easylog.context.ApplicationContextHolder;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义函数工厂
 */
public class CustomFunctionFactory {

    private final Map<String, ICustomFunction> customFunctionMap = new HashMap<>();

    public CustomFunctionFactory(List<ICustomFunction> customFunctions) {
        if (!CollectionUtils.isEmpty(customFunctions)) {
            for (ICustomFunction customFunction : customFunctions) {
                customFunctionMap.put(customFunction.functionName(), customFunction);
            }
        }
    }

    /**
     * 通过函数名获取对应自定义函数
     *
     * @param functionName 函数名
     * @return 自定义函数
     */
    public ICustomFunction getFunction(String functionName) {
        return customFunctionMap.get(functionName);
    }

}
