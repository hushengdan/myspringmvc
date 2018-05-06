package com.spring.mvc.framework.annotation;

import java.lang.annotation.*;

/**
 * Created by daniel on 2018/5/4.
 */
@Target({ElementType.METHOD,ElementType.TYPE})//加到方法，class上
@Retention(RetentionPolicy.RUNTIME)//运行时生效
@Documented
public @interface MyRequestMapping {
    String value() default "";
}
