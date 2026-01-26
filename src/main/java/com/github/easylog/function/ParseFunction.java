package com.github.easylog.function;

/**
 * 自定义函数 DSL：{funcName{SpEL}}。
 * <p>
 * 实现类作为 Spring Bean 注入后即可在模板中通过函数名调用。
 * 可用于：
 * <ul>
 *     <li>将 ID 翻译为展示名/标签。</li>
 *     <li>在业务执行前获取“旧值”。</li>
 *     <li>对 SpEL 解析结果做格式化。</li>
 * </ul>
 */
public interface ParseFunction {

    /**
     * 函数在模板中的调用名称。
     */
    String functionName();

    /**
     * 执行函数逻辑，入参为 SpEL 求值后的字符串结果。
     */
    String apply(String value);

    /**
     * 是否在业务方法执行前计算，典型用于“查询旧值”场景。
     */
    default boolean executeBefore() {
        return false;
    }
}
