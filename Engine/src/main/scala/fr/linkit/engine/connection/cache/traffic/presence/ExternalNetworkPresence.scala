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

package fr.linkit.engine.connection.cache.traffic.presence

import fr.linkit.api.connection.cache.traffic.content.ObjectPresenceType._
import fr.linkit.api.connection.cache.traffic.content.{ObjectNetworkPresence, ObjectPresenceType}

import scala.collection.mutable

class ExternalNetworkPresence[L](handler: AbstractNetworkPresenceHandler[L], val location: L) extends ObjectNetworkPresence {

    private val presences = mutable.HashMap.empty[String, ObjectPresenceType]

    handler.bindListener(location, this)

    override def getPresenceFor(engineId: String): ObjectPresenceType = {
        presences.getOrElseUpdate(engineId, {
            if (handler.isPresentOnEngine(engineId, location)) PRESENT
            else NOT_PRESENT
        })
    }

    def onObjectSet(engineId: String): Unit = presences(engineId) = PRESENT

    def onObjectRemoved(engineId: String): Unit = presences(engineId) = NOT_PRESENT

}
