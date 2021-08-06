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

package fr.linkit.api.connection.cache.obj.invokation

import fr.linkit.api.connection.cache.obj.PuppetWrapper
import fr.linkit.api.connection.cache.obj.behavior.MethodBehavior

trait WrapperMethodInvocation[R] {

    val wrapper: PuppetWrapper[_]

    val methodID: Int

    val methodBehavior: MethodBehavior

    val callerIdentifier: String

    val currentIdentifier: String

    def callSuper(): R

    val methodArguments: Array[Any]

    val debug: Boolean = true

}
