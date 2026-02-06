package com.github.easylog.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明该类参与Diff并配置别名/字段开关。
 * @author gaoshuanglong
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EasyLogDiffObject {

    /**
     * 类别名
     */
    String alias() default "";

    /**
     * 是否启用所有字段Diff，默认启用
     */
    boolean enableAllFields() default true;
}
