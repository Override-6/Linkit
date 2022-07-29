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

package fr.linkit.api.gnom.referencing.linker

import fr.linkit.api.gnom.referencing.presence.NetworkPresenceHandler
import fr.linkit.api.gnom.referencing.{NetworkObject, NetworkObjectReference}

/**
 * Depicts any class that is responsible for registering network objects on an engine. <br>
 * This trait is a pillar of the <a href="https://override-6.github.io/Linkit/docs/GNOM">GNOM</a> System.
 *
 * Network Objects Linkers guarantees that the presence state of an object's reference is synchronized between current and remote engines.<br>
 *
 * @tparam R the reference level this NetworkObjectLinker handles
 * */
trait NetworkObjectLinker[R <: NetworkObjectReference] extends NetworkPresenceHandler[R] {

    def findObject(reference: R): Option[NetworkObject[_ <: R]]

}
