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

package fr.linkit.api.gnom.persistence.context

import fr.linkit.api.gnom.persistence.obj.TrafficNetworkPresenceReference

import java.util

class ContextualObjectReference(private val channelPath: Array[Int], val objectID: Int) extends TrafficNetworkPresenceReference(channelPath) {
    override def toString: String = {
        s"@traffic/${channelPath.mkString("/")}:$objectID"
    }

    override def hashCode(): Int = util.Arrays.deepHashCode(Array(channelPath, objectID))

    override def equals(obj: Any): Boolean = obj match {
        case ref: ContextualObjectReference => ref.objectID == objectID && (ref.channelPath sameElements channelPath)
        case _ => false
    }
}