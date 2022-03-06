/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.api.gnom.cache.sync.contract.annotation;

import fr.linkit.api.gnom.cache.sync.contract.BasicInvocationRule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Determines that the synchronised instances of the annotated class
 * are to be controlled remotely by engines that retrieves the synchronized object.
 * <p>
 * a Remote object works exactly the same as a synchronized object,
 * excepted that any method invocation on an object which is not the original object will throw an exception.
 * <p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface FullRemote {

    /**
     * The default behavior applied on all methods
     * */
    BasicInvocationRule value() default BasicInvocationRule.ONLY_OWNER;

}