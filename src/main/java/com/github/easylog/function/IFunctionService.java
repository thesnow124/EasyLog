package com.github.easylog.function;

/**
 * @author Gaosl
 */
public interface IFunctionService {

    /**
     * 执行函数
     *
     * @param functionName 函数名
     * @param value        参数
     * @return 执行结果
     */
    String apply(String functionName, Object value);

    /**
     * 是否在拦截的方法执行前执行
     *
     * @param functionName 函数名
     * @return
     */
    boolean executeBefore(String functionName);

    /**
     * 是否在拦截的方法执行时环绕执行
     *
     * @param functionName 函数名
     * @return
     */
     boolean executeAround(String functionName);

}
