package com.github.easylog.annotation;

import java.lang.annotation.*;

/**
 * @author Gaosl
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface EasyLogs {
    EasyLog[] value();
}
