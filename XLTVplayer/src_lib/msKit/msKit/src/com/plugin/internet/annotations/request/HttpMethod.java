package com.plugin.internet.annotations.request;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * HTTP请求的方法
 * 目前支持GET和POST
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface HttpMethod {
    String value() default "";
}
