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

package fr.linkit.engine.gnom.cache.sync.behavior.v2.builder

import scala.collection.mutable.ListBuffer

class AbstractBehaviorBuilder[C <: AnyRef] {
    protected var context: C = _
    private val methodCalls  = ListBuffer.empty[() => Unit]

    protected def callOnceContextSet(action: => Unit): Unit = {
        if (context != null)
            action
        else
            methodCalls += (() => action)
    }

    def setContext(context: C): Unit = {
        this.context = context
        methodCalls.foreach(_.apply())
    }

}
