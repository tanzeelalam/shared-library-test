package com.mcafee.orbit

import java.lang.annotation.*

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Deprecated {
    public String[] value() default [];
}
