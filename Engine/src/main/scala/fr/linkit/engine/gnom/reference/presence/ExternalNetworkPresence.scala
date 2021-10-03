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

import fr.linkit.api.gnom.reference.NetworkObjectReference
import fr.linkit.api.gnom.reference.presence.ObjectPresenceType._
import fr.linkit.api.gnom.reference.presence.{ObjectNetworkPresence, ObjectPresenceType}
import fr.linkit.engine.gnom.reference.AbstractNetworkObjectLinker

import scala.collection.mutable

class ExternalNetworkPresence[L <: NetworkObjectReference](handler: AbstractNetworkObjectLinker[_, L, _ <: L], val location: L)
        extends ObjectNetworkPresence {

    private val presences = mutable.HashMap.empty[String, ObjectPresenceType]

    handler.bindListener(location, this)

    override def getPresenceFor(engineId: String): ObjectPresenceType = {
        presences.getOrElseUpdate(engineId, {
            if (handler.askIfPresent(engineId, location)) PRESENT
            else NOT_PRESENT
        })
    }

    def onObjectSet(engineId: String): Unit = presences(engineId) = PRESENT

    def onObjectRemoved(engineId: String): Unit = presences(engineId) = NOT_PRESENT
}
