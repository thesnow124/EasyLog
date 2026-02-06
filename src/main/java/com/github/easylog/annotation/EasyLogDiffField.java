package com.github.easylog.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明字段参与Diff并配置别名/忽略。
 * @author gaoshuanglong
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EasyLogDiffField {

    /**
     * 字段别名
     */
    String alias() default "";

    /**
     * 是否忽略该字段
     */
    boolean ignored() default false;
}
