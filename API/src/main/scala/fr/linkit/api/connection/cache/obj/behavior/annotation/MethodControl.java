/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.api.connection.cache.obj.behavior.annotation;

import fr.linkit.api.connection.cache.obj.behavior.member.MethodBehavior;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Contains basic information for a {@link MethodBehavior}
 * @see fr.linkit.api.connection.cache.obj.behavior.SynchronizedObjectBehavior
 * @see MethodBehavior
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MethodControl {

    /**
     * @return The basic rules for the annotated method invocation
     * @see BasicInvocationRule
     */
    BasicInvocationRule value() default BasicInvocationRule.ONLY_CURRENT;

    /**
     * @return true if the returned value of the method must be synchronized
     * (Only useful if the value is an object. Nothing would be sync if the value is a primitive.)
     */
    boolean synchronizeReturnValue() default false;

    /**
     * Used to hide this method from remote invocations.
     * If a RMI request tries to call this method while this method is set as hidden,
     * an exception will occur. //TODO Throw the exception on the RMI request sender
     * */
    boolean hide() default false;

    /**
     *
     * @return true if the Remote Method Invocation should not wait for any resulting value or exception.
     */
    boolean invokeOnly() default false;
}
