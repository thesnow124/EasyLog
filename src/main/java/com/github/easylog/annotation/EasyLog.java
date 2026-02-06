package com.github.easylog.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
    String success() default "";

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
     * Diff变量或表达式（支持固定 key 或 SpEL 表达式）
     */
    String diffKey() default "";

    /**
     * 额外扩展信息（不参与字段差异对比）
     */
    String extra() default "";

    /**
     * 记录条件 默认 true
     */
    String condition() default "";


}
