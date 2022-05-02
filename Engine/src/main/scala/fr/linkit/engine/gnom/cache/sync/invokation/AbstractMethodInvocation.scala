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

package fr.linkit.engine.gnom.cache.sync.invokation

import fr.linkit.api.gnom.cache.sync.invocation.MethodInvocation
import fr.linkit.api.gnom.cache.sync.invocation.local.LocalMethodInvocation
import fr.linkit.api.gnom.cache.sync.tree.{ChippedObjectNode, ObjectConnector, ObjectSyncNode}

abstract class AbstractMethodInvocation[R](override val methodID: Int,
                                           override val objectNode: ChippedObjectNode[_],
                                           override val connector: ObjectConnector) extends MethodInvocation[R] {

    def this(local: LocalMethodInvocation[R]) = {
        this(local.methodID, local.objectNode, local.connector)
    }

}
