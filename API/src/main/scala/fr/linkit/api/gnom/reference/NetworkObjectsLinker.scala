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

package fr.linkit.api.gnom.reference

import fr.linkit.api.gnom.packet.PacketCoordinates
import fr.linkit.api.gnom.reference.presence.ObjectNetworkPresence

trait NetworkObjectsLinker[L <: NetworkReferenceLocation, R <: AnyRef] {

    def isObjectReferencedOnCurrent(ref: R): Boolean

    def isObjectReferencedOnEngine(engineID: String, ref: R): Boolean

    def findObjectPresence(ref: R): Option[ObjectNetworkPresence]

    def findObjectLocation(coordsOrigin: PacketCoordinates, ref: R): Option[L]

    def findObjectLocation(ref: R): Option[L]

    def findObject(coordsOrigin: PacketCoordinates, location: L): Option[R]

}
