package com.github.easylog.function;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 默认函数服务实现。
 */
public class DefaultFunctionServiceImpl implements IFunctionService {

    private static final Logger LOG = Logger.getLogger(DefaultFunctionServiceImpl.class.getName());

    private final ParseFunctionFactory parseFunctionFactory;

    public DefaultFunctionServiceImpl(ParseFunctionFactory parseFunctionFactory) {
        this.parseFunctionFactory = parseFunctionFactory;
    }

    @Override
    public Object apply(String functionName, Object... values) {
        if (parseFunctionFactory == null) {
            return null;
        }
        IParseFunction function = parseFunctionFactory.getFunction(functionName);
        if (function == null) {
            LOG.log(Level.WARNING, "未注册函数: {0}", functionName);
            return null;
        }
        try {
            return function.apply(values);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "函数执行失败: " + functionName, e);
            return null;
        }
    }

    @Override
    public boolean beforeFunction(String functionName) {
        return parseFunctionFactory != null && parseFunctionFactory.isBeforeFunction(functionName);
    }
}
