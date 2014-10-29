/**
 * Copyright 2011-2012 Renren Inc. All rights reserved.
 * － Powered by Team Pegasus. －
 */

package com.plugin.internet.annotations.request;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  Rest API的URL，是一个以HTTP开始的全路径
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface RestMethodUrl {
	String value() default "";
}
