/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.gnom.cache.sync.invokation

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.behavior.member.method.MethodBehavior
import fr.linkit.api.gnom.cache.sync.invokation.MethodInvocation
import fr.linkit.api.gnom.cache.sync.invokation.local.LocalMethodInvocation
import fr.linkit.engine.gnom.cache.sync.behavior.member.SyncMethodBehavior

abstract class AbstractMethodInvocation[R](override val methodBehavior: MethodBehavior,
                                           override val synchronizedObject: SynchronizedObject[_]) extends MethodInvocation[R] {

    def this(local: LocalMethodInvocation[R]) = {
        this(local.methodBehavior, local.synchronizedObject)
    }

    override val methodID: Int = methodBehavior.desc.methodId

}
