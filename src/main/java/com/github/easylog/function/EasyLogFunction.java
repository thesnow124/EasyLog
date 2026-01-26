package com.github.easylog.function;

/**
 * 自定义函数的执行时机控制，参考美团日志组件的 {@code IParseFunction} 设计。
 * <p>
 * 业务侧可让函数所在的 Bean 实现该接口，指定哪些函数需要在业务方法执行前求值，
 * 以便获取“修改前”的数据；未覆盖的方法默认为业务方法执行后再求值。
 */
public interface EasyLogFunction {

    /**
     * 是否在业务方法执行前计算指定函数。
     *
     * @param functionName SpEL 中调用的目标方法名
     * @return true：前置执行；false：后置执行
     */
    default boolean executeBefore(String functionName) {
        return false;
    }
}
