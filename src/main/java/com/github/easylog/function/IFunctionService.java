package com.github.easylog.function;

/**
 * 函数调度服务。
 */
public interface IFunctionService {

    /**
     * 执行指定函数。
     *
     * @param functionName 函数名
     * @param values 函数入参
     * @return 函数返回值
     */
    Object apply(String functionName, Object... values);

    /**
     * 判断函数是否为前置执行函数。
     */
    boolean beforeFunction(String functionName);
}
