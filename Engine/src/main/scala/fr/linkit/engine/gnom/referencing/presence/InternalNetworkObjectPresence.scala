/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.referencing.presence

import fr.linkit.api.gnom.referencing.NetworkObjectReference
import fr.linkit.api.gnom.referencing.presence.ObjectPresenceState._
import fr.linkit.api.gnom.referencing.presence.{NetworkObjectPresence, ObjectPresenceState}
import fr.linkit.api.internal.system.log.AppLoggers

import scala.collection.mutable

class InternalNetworkObjectPresence[R <: NetworkObjectReference](handler: AbstractNetworkPresenceHandler[R], val location: R) extends NetworkObjectPresence {

    private val presences        = mutable.HashMap.empty[String, ObjectPresenceState]
    private var present: Boolean = false

    def isPresentOnCurrent: Boolean = present
    
    override def isPresenceKnownFor(engineId: String): Boolean = presences.synchronized {
        presences.contains(engineId)
    }
    
    def setPresenceFor(engineId: String, kind: ObjectPresenceState): Unit = presences.synchronized {
        presences.put(engineId, kind)
    }

    override def getPresenceFor(engineId: String): ObjectPresenceState = presences.synchronized {
        presences.getOrElse(engineId, NEVER_ASKED)
    }

    def setPresent(): Unit = presences.synchronized {
        //set to all engines who thinks that the reference is not present on this engine
        //that it's no has been referenced
        //on this current engine.
        present = true
        val enginesID = presences
            .filter(_._2 eq NOT_PRESENT)
            .keys
            .toArray
        AppLoggers.GNOM.debug(s"presence modified for $location to present on this engine. Affected engines : ${enginesID.mkString(", ")}")
        if (enginesID.isEmpty)
            return
        handler.informPresence(enginesID, location, PRESENT)
        presences.clear()
        enginesID.foreach(presences.put(_, PRESENT))
    }

    def setNotPresent(): Unit = presences.synchronized {
        //set to all engines who thinks that the reference is present on this engine
        //that it's no longer referenced
        //on this current engine.
        present = false
        val enginesID = presences
            .filter(_._2 eq PRESENT)
            .keys
            .toArray
        if (enginesID.isEmpty)
            return
        handler.informPresence(enginesID, location, NOT_PRESENT)
        enginesID.foreach(presences.put(_, NOT_PRESENT))
    }

}
