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

package fr.linkit.engine.gnom.cache

import fr.linkit.api.gnom.cache.traffic.content.SharedCacheItemReference
import fr.linkit.api.gnom.packet.PacketCoordinates
import fr.linkit.api.gnom.reference.NetworkObjectLinker
import fr.linkit.api.gnom.reference.presence.ObjectNetworkPresence

class SharedCacheItemsLinker[R <: AnyRef] extends NetworkObjectLinker[SharedCacheItemReference, R] {

    override def isObjectReferencedOnCurrent(ref: R): Boolean = ???

    override def isObjectReferencedOnEngine(engineID: String, ref: R): Boolean = ???

    override def findObjectPresence(ref: R): Option[ObjectNetworkPresence] = ???

    override def findObjectLocation(coordsOrigin: PacketCoordinates, ref: R): Option[SharedCacheItemReference] = ???

    override def findObjectLocation(ref: R): Option[SharedCacheItemReference] = ???

    override def findObject(coordsOrigin: PacketCoordinates, location: SharedCacheItemReference): Option[R] = ???
}
