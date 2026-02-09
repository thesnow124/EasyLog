package com.github.easylog.function;

/**
 * 自定义模板函数扩展点。
 *
 * <p>模板语法：{@code {{functionName(arg1,arg2)}}}</p>
 */
public interface IParseFunction {

    /**
     * 是否在业务方法执行前执行。
     */
    default boolean executeBefore() {
        return false;
    }

    /**
     * 函数名称。
     */
    String functionName();

    /**
     * 处理函数参数并返回渲染值。
     */
    Object apply(Object... values);
}
