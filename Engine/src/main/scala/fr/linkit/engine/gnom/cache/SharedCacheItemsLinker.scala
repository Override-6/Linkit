/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.gnom.cache

import fr.linkit.api.gnom.cache.traffic.content.SharedCacheItemLocation
import fr.linkit.api.gnom.packet.PacketCoordinates
import fr.linkit.api.gnom.reference.NetworkObjectsLinker
import fr.linkit.api.gnom.reference.presence.ObjectNetworkPresence

class SharedCacheItemsLinker[R <: AnyRef] extends NetworkObjectsLinker[SharedCacheItemLocation, R] {

    override def isObjectReferencedOnCurrent(ref: R): Boolean = ???

    override def isObjectReferencedOnEngine(engineID: String, ref: R): Boolean = ???

    override def findObjectPresence(ref: R): Option[ObjectNetworkPresence] = ???

    override def findObjectLocation(coordsOrigin: PacketCoordinates, ref: R): Option[SharedCacheItemLocation] = ???

    override def findObjectLocation(ref: R): Option[SharedCacheItemLocation] = ???

    override def findObject(coordsOrigin: PacketCoordinates, location: SharedCacheItemLocation): Option[R] = ???
}
