package com.github.easylog.function.impl;


import com.github.easylog.function.CustomFunctionFactory;
import com.github.easylog.function.ICustomFunction;
import com.github.easylog.function.IFunctionService;


/**
 * @author Gaosl
 */
public class DefaultFunctionServiceImpl implements IFunctionService {

    private final CustomFunctionFactory customFunctionFactory;

    public DefaultFunctionServiceImpl(CustomFunctionFactory customFunctionFactory) {
        this.customFunctionFactory = customFunctionFactory;
    }

    @Override
    public String apply(String functionName, Object value) {
        ICustomFunction function = customFunctionFactory.getFunction(functionName);
        if (function == null) {
            return value.toString();
        }
        return function.apply(value);
    }

    @Override
    public boolean executeBefore(String functionName) {
        ICustomFunction function = customFunctionFactory.getFunction(functionName);
        return function != null && function.executeBefore();
    }

    @Override
    public boolean executeAround(String functionName) {
        ICustomFunction function = customFunctionFactory.getFunction(functionName);
        return function != null && function.executeAround();
    }
}
