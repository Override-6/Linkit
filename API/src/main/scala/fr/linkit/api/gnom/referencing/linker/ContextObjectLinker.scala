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

package fr.linkit.api.gnom.referencing.linker

import fr.linkit.api.gnom.packet.PacketCoordinates
import fr.linkit.api.gnom.persistence.context.ContextualObjectReference
import fr.linkit.api.gnom.referencing.traffic.TrafficInterestedNPH

trait ContextObjectLinker extends NetworkObjectLinker[ContextualObjectReference] with TrafficInterestedNPH {

    def findReferenceID(obj: AnyRef): Option[Int]

    def findPersistableReference(obj: AnyRef, coords: PacketCoordinates): Option[Int]

    def ++=(objs: AnyRef*): this.type

    def ++=(objs: Map[Int, AnyRef]): this.type

    def putAllNotContained(objs: Map[Int, AnyRef]): this.type

    def +=(anyRef: AnyRef): this.type

    def +=(code: Int, anyRef: AnyRef): this.type

    def -=(obj: AnyRef): this.type

    def transferTo(linker: ContextObjectLinker): this.type
}
