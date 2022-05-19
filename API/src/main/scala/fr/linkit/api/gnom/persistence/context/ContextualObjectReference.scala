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

import fr.linkit.api.gnom.persistence.obj.TrafficObjectReference
import fr.linkit.api.gnom.reference.NetworkObjectReference

import java.util

class ContextualObjectReference(trafficPath: Array[Int], val objectID: Int) extends TrafficObjectReference(trafficPath) {

    override def asSuper: Option[NetworkObjectReference] = Some(new TrafficObjectReference(trafficPath))

    override def toString: String = super.toString + s"/~$objectID"

    override def hashCode(): Int = util.Arrays.deepHashCode(Array(trafficPath, objectID))

    override def equals(obj: Any): Boolean = obj match {
        case ref: ContextualObjectReference => ref.objectID == objectID && (ref.trafficPath sameElements trafficPath)
        case _ => false
    }
}