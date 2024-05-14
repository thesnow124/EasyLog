package com.github.easylog.function;

/**
 * @author Gaosl
 */
public interface ICustomFunction {

    /**
     * 目标方法执行前 执行自定义函数
     *
     * @return 是否执行前的函数
     */
    default boolean executeBefore(){return  false;};
    /**
     * 目标方法执行时，环绕执行自定义函数
     *
     * @return 是否执行前的函数
     */
    default boolean executeAround(){return  false;};

    /**
     * 自定义函数名
     *
     * @return 自定义函数名
     */
    String functionName();

    /**
     * 自定义函数
     *
     * @param param 参数
     * @return 执行结果
     */
    String apply(Object param);
}
