package com.spring.mvc.framework.annotation;

import java.lang.annotation.*;

/**
 * Created by daniel on 2018/5/4.
 */
@Target(ElementType.TYPE)//加到class上
@Retention(RetentionPolicy.RUNTIME)//运行时生效
@Documented
public @interface MyService {
    String value() default "";
}
