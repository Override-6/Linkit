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

package fr.linkit.api.gnom.persistence.obj

import fr.linkit.api.gnom.referencing.{NetworkObjectReference, SystemObjectReference}

class TrafficReference extends NetworkObjectReference {

    override def asSuper: Option[NetworkObjectReference] = None

    override def toString: String = s"@traffic"

    override def hashCode(): Int = 31

    override def equals(obj: Any): Boolean = obj.isInstanceOf[TrafficReference]

}

object TrafficReference extends TrafficReference with SystemObjectReference {

    def /(id: Int): TrafficObjectReference = new TrafficObjectReference(Array(id))

    def /(path: Array[Int]): TrafficObjectReference = new TrafficObjectReference(path)

}
