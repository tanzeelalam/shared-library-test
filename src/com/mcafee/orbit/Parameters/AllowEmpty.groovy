package com.mcafee.orbit.Parameters

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Annotation to mark a field inside an implementation of the
 * {@link Parameters} class as optional without a default value
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface AllowEmpty {}