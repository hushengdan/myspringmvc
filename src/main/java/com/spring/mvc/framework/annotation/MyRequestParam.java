package com.spring.mvc.framework.annotation;

import java.lang.annotation.*;

/**
 * Created by daniel on 2018/5/4.
 */
@Target(ElementType.PARAMETER)//加到参数上
@Retention(RetentionPolicy.RUNTIME)//运行时生效
@Documented
public @interface MyRequestParam {
    String value() default "";
    boolean required() default true;
}
