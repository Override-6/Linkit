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

package fr.linkit.api.gnom.reference.linker

import fr.linkit.api.gnom.reference.presence.NetworkPresenceHandler
import fr.linkit.api.gnom.reference.{NetworkObject, NetworkObjectReference}

/**
 * Depicts any class that is responsible for object linkage between all engines for the [[fr.linkit.api.gnom.network.Network]]. <br>
 * This trait is a pillar of the [[https://drive.google.com/file/d/1nbuscC1mLd_I047VNW8X0u1uEzGGsiVZ/view?usp=sharing GNOM]] System.
 *
 * Network Objects Linkers guarantees that the presence of an object's location is synchronized between each Engines.<br>
 * @tparam R the location kind of this NetworkObjectLinker
 * */
trait NetworkObjectLinker[R <: NetworkObjectReference] extends NetworkPresenceHandler[R] {

    def isAssignable(reference: NetworkObjectReference): Boolean

    def findObject(reference: R): Option[NetworkObject[_ <: R]]

}
