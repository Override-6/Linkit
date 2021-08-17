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

package fr.linkit.engine.connection.cache.obj.invokation

import fr.linkit.api.connection.cache.obj.SynchronizedObject
import fr.linkit.api.connection.cache.obj.behavior.member.MethodBehavior
import fr.linkit.api.connection.cache.obj.invokation.MethodInvocation
import fr.linkit.api.connection.cache.obj.invokation.local.LocalMethodInvocation

abstract class AbstractMethodInvocation[R](override val methodBehavior: MethodBehavior,
                                           override val synchronizedObject: SynchronizedObject[_]) extends MethodInvocation[R] {

    def this(local: LocalMethodInvocation[R]) = {
        this(local.methodBehavior, local.synchronizedObject)
    }

    override val methodID: Int = methodBehavior.desc.methodId

}
