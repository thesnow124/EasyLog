package com.github.easylog.annotation;

import java.lang.annotation.*;

/**
 * 日志记录注解
 *
 * @author Gaosl
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Repeatable(EasyLogs.class)
public @interface EasyLog {

    /**
     * 平台
     */
    String platform() default "";

    /**
     * 操作者
     */
    String operator() default "";

    /**
     * 模块
     */
    String module() default "";

    /**
     * 操作类型：比如增删改查
     */
    String type() default "";

    /**
     * 关联的业务id
     */
    String bizNo() default "";

    /**
     * 成功模板
     */
    String success();

    /**
     * 成功参数
     */
    String[] successParamList() default {};

    /**
     * 失败模板
     */
    String fail() default "";

    /**
     * 失败参数
     */
    String[] failParamList() default {};


    /**
     * 记录更详细的
     */
    String detail() default "";

    /**
     * 记录条件 默认 true
     */
    String condition() default "";

}
