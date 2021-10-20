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

package fr.linkit.api.gnom.persistence.obj

import fr.linkit.api.gnom.persistence.context.ContextualObjectReference

import java.util

class TrafficPresenceReference(val trafficPath: Array[Int]) extends TrafficReference {

    override def toString: String = {
        s"@traffic/${trafficPath.mkString("/")}"
    }

    override def hashCode(): Int = util.Arrays.deepHashCode(Array(trafficPath))

    override def equals(obj: Any): Boolean = obj match {
        case ref: TrafficPresenceReference => ref.trafficPath sameElements trafficPath
        case _                             => false
    }

    def /(id: Int): TrafficPresenceReference = new TrafficPresenceReference(trafficPath :+ id)

    def /(path: Array[Int]): TrafficPresenceReference = new TrafficPresenceReference(trafficPath ++ path)

    def /~(id: Int): TrafficPresenceReference = new ContextualObjectReference(trafficPath, id)
}