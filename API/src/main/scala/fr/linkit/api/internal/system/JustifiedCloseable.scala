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

package fr.linkit.api.internal.system

import fr.linkit.api.internal.system.Reason.NOT_SPECIFIED

trait JustifiedCloseable extends AutoCloseable {

    def close(reason: Reason): Unit

    def isClosed: Boolean

    @inline def isOpen: Boolean = !isClosed

    override def close(): Unit = close(NOT_SPECIFIED)

}
