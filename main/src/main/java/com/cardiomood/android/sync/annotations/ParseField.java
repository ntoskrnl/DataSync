package com.cardiomood.android.sync.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by antondanhsin on 08/10/14.
 */

@Target(FIELD)
@Retention(RUNTIME)
public @interface ParseField {

    String name() default "";

}
