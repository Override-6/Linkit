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

package fr.linkit.engine.connection.reference.presence

import fr.linkit.api.connection.reference.NetworkReferenceLocation
import fr.linkit.api.connection.reference.presence.ObjectPresenceType._
import fr.linkit.api.connection.reference.presence.{ObjectNetworkPresence, ObjectPresenceType}

import scala.collection.mutable

class InternalNetworkPresence[R <: AnyRef, L <: NetworkReferenceLocation[R]](handler: AbstractNetworkPresenceHandler[R, L], val location: L) extends ObjectNetworkPresence {

    private val presences        = mutable.HashMap.empty[String, ObjectPresenceType]
    private var present: Boolean = false

    def isPresent: Boolean = present

    override def getPresenceFor(engineId: String): ObjectPresenceType = {
        presences.getOrElse(engineId, NEVER_ASKED)
    }

    def setPresent(): Unit = {
        presences.foreachEntry((engineId, presence) => presence match {
            case NOT_PRESENT =>
                handler.informPresence(engineId, location, PRESENT)
            case PRESENT     => //Already thinks that an object is present on this location
        })
        present = true
    }

    def setNotPresent(): Unit = {
        presences.foreachEntry((engineId, presence) => presence match {
            case PRESENT     =>
                handler.informPresence(engineId, location, NOT_PRESENT)
            case NOT_PRESENT => //Already thinks that an object is not present on this location
        })
        present = false
    }

}
