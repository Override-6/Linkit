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

package fr.linkit.engine.gnom.reference.presence

import fr.linkit.api.gnom.reference.{NetworkObject, NetworkObjectReference}
import fr.linkit.api.gnom.reference.presence.ObjectPresenceType._
import fr.linkit.api.gnom.reference.presence.{ObjectNetworkPresence, ObjectPresenceType}
import fr.linkit.engine.gnom.reference.AbstractNetworkObjectLinker

import scala.collection.mutable

class InternalNetworkPresence[R <: NetworkObjectReference](handler: AbstractNetworkObjectLinker[_, R, _ <: R], val location: R) extends ObjectNetworkPresence {

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
