package com.spring.mvc.framework.annotation;

import java.lang.annotation.*;

/**
 * Created by daniel on 2018/5/4.
 */
@Target(ElementType.FIELD)//加到字段上
@Retention(RetentionPolicy.RUNTIME)//运行时生效
@Documented//说明使用
public @interface MyAutowired {
    String value() default "";
}
