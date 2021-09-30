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

package fr.linkit.engine.gnom.reference

import fr.linkit.api.gnom.packet.PacketCoordinates
import fr.linkit.api.gnom.reference.presence.ObjectNetworkPresence
import fr.linkit.api.gnom.reference.{NetworkObjectsLinker, NetworkReferenceLocation}

import scala.reflect.ClassTag

class GeneralNetworkObjectsLinker extends NetworkObjectsLinker {

    override def isObjectReferencedOnCurrent(ref: AnyRef): Boolean = ???

    override def isObjectReferencedOnEngine(engineID: String, ref: AnyRef): Boolean = ???

    override def findObjectPresence(ref: AnyRef): Option[ObjectNetworkPresence] = ???

    override def findObjectLocation[R <: NetworkReferenceLocation : ClassTag](coordsOrigin: PacketCoordinates, ref: AnyRef): Option[R] = ???

    override def findObjectLocation[R <: NetworkReferenceLocation : ClassTag](ref: AnyRef): Option[R] = ???

    override def findObject(coordsOrigin: PacketCoordinates, location: NetworkReferenceLocation): Option[AnyRef] = ???
}
