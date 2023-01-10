/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.api.gnom.persistence.context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 *      Classes that implements this interface will receive a special handling from the persistence system.
 * </p>
 * <p>
 *     a Deconstructible object must have one <strong>and only one</strong> constructor
 *     annotated with the @{@link Persist} annotation.
 * </p>
 * <p>
 *     the {@link #deconstruct()} method will be called during serialization and it's result will be passed to the
 *     constructor marked with @{@link Persist} once the object will get deserialized. This way, each element of
 *     the object array returned by the {@link #deconstruct()} method is a parameter of the annotated constructor.
 * </p>
 * */
public interface Deconstructible {

    /**
     * <p>
     * Used to spot the constructor that'll be used by the deserialization system.
     * Place this on your constructor to mark the persistence system that the annotated constructor
     * must be used instead.
     * </p>
     * <p>
     * <strong>NOTE: </strong>A class containing an annotated constructor must also implement the {@link Deconstructible} trait.
     * </p>
     */
    @Target(ElementType.CONSTRUCTOR)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Persist {

    }

    /**
     * Turn this object to an array of object that must match the parameters of the @{@link Persist} constructor.
     * */
    Object[] deconstruct();

}