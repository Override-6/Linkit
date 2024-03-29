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

package fr.linkit.engine.gnom.cache.sync.invokation

import fr.linkit.api.gnom.cache.sync.ChippedObject
import fr.linkit.api.gnom.cache.sync.invocation.MethodInvocation
import fr.linkit.api.gnom.cache.sync.invocation.local.LocalMethodInvocation
import fr.linkit.api.gnom.cache.sync.env.{ChippedObjectCompanion, ObjectConnector, SyncObjectCompanion}

abstract class AbstractMethodInvocation[R](override val methodID : Int,
                                           override val obj      : ChippedObject[_],
                                           override val connector: ObjectConnector) extends MethodInvocation[R] {

    def this(local: LocalMethodInvocation[R]) = {
        this(local.methodID, local.obj, local.connector)
    }

}
