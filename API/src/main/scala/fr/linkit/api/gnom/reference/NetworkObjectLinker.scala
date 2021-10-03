/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.api.gnom.reference

import fr.linkit.api.gnom.packet.PacketCoordinates
import fr.linkit.api.gnom.reference.presence.ObjectNetworkPresence

/**
 * Depicts any class that is responsible for object linkage between all engines for the [[fr.linkit.api.gnom.network.Network]]. <br>
 * This trait is a pillar of the [[https://drive.google.com/file/d/1nbuscC1mLd_I047VNW8X0u1uEzGGsiVZ/view?usp=sharing GNOM]] System.
 *
 * Network Objects Linkers guarantees that the presence of an object's location is synchronized between each Engines.<br>
 * @tparam R the location kind of this NetworkObjectLinker
 * */
trait NetworkObjectLinker[R <: NetworkObjectReference] {

    /**
     * checks if the current ref object is referenced (bound to a [[NetworkObjectReference]]) in this Linker.
     * @param ref the object to test
     * @return true if there is a [[NetworkObjectReference]] bound with this reference contained into this Linker
     */
    def isObjectReferencedOnCurrent(ref: NetworkObject[R]): Boolean

    /**
     * Checks if the current ref object is referenced (bound to a [[NetworkObjectReference]])
     * */
    def isObjectReferencedOnEngine(engineID: String, ref: NetworkObject[R]): Boolean

    def findObjectPresence(ref: NetworkObject[R]): Option[ObjectNetworkPresence]

    def findObject(coordsOrigin: PacketCoordinates, location: R): Option[NetworkObject[_ <: R]]

}