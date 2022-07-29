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

package fr.linkit.engine.test.gnom

import fr.linkit.api.gnom.referencing.linker.NetworkObjectLinker
import fr.linkit.api.gnom.referencing.traffic.ObjectManagementChannel
import fr.linkit.api.gnom.referencing.{NetworkObject, NetworkObjectReference}
import fr.linkit.engine.gnom.referencing.AbstractNetworkPresenceHandler

class LinkageTests {

    class MyLinker(omc: ObjectManagementChannel) extends AbstractNetworkPresenceHandler[NetworkObjectReference](null, omc) with NetworkObjectLinker[NetworkObjectReference] {
        override def isAssignable(reference: NetworkObjectReference): Boolean = ???

        override def findObject(reference: NetworkObjectReference): Option[NetworkObject[_ <: NetworkObjectReference]] = ???
    }

}
