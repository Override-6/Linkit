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

package fr.linkit.api.connection.cache.obj.invokation.remote

import fr.linkit.api.connection.cache.obj.SynchronizedObject
import fr.linkit.api.connection.cache.obj.behavior.member.MethodBehavior

/**
 * The invocation information for a synchronized object's method.
 * @tparam R the return type of the method invoked
 * */
trait SynchronizedMethodInvocation[R] {

    /**
     * The synchronized object on which the method is called.
     * */
    val synchronizedObject: SynchronizedObject[_]

    /**
     * The method's identifier.
     * */
    val methodID: Int

    /**
     * The method's behavior
     */
    val methodBehavior: MethodBehavior

    //TODO doc
    val callerIdentifier: String

    /**
     * The identifier of the current engine.
     */
    val currentIdentifier: String

    /**
     * Calls the local method of the object.
     * @return the method's call result.
     * */
    def callSuper(): R

    /**
     * The final argument array for the method invocation that will be used in [[callSuper()]] and for the RMI
     * */
    val methodArguments: Array[Any]

    val debug: Boolean = true

}
