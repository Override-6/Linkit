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
import fr.linkit.api.gnom.reference.presence.{NetworkObjectPresence, ObjectPresenceType}
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.reference.AbstractNetworkPresenceHandler

import scala.collection.mutable

class ExternalNetworkObjectPresence[L <: NetworkObjectReference](handler: AbstractNetworkPresenceHandler[L], val location: L)
        extends NetworkObjectPresence {

    private val presences = mutable.HashMap.empty[String, ObjectPresenceType]

    handler.bindListener(location, this)

    override def getPresenceFor(engineId: String): ObjectPresenceType = {
        if (engineId == null)
            throw new NullPointerException("engineId is null.")
        presences.getOrElseUpdate(engineId, {
            val present = handler.askIfPresent(engineId, location)
            AppLogger.warn(s"is present : $present")
            if (present) PRESENT
            else NOT_PRESENT
        })
    }

    def onObjectSet(engineId: String): Unit = {
        presences(engineId) = PRESENT
    }

    def onObjectRemoved(engineId: String): Unit = {
        presences(engineId) = NOT_PRESENT
    }
}
