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

package fr.linkit.engine.gnom.reference.linker

import fr.linkit.api.gnom.reference.linker.NetworkObjectLinker
import fr.linkit.api.gnom.reference.traffic.{LinkerRequestBundle, ObjectManagementChannel}
import fr.linkit.api.gnom.reference.{NetworkObject, NetworkObjectReference}
import fr.linkit.engine.gnom.reference.AbstractNetworkPresenceHandler

import scala.collection.mutable

private[gnom] class RemainingNetworkObjectsLinker(omc: ObjectManagementChannel) extends AbstractNetworkPresenceHandler[NetworkObjectReference](omc) with NetworkObjectLinker[NetworkObjectReference] {

    private val map = mutable.HashMap.empty[NetworkObjectReference, NetworkObject[NetworkObjectReference]]

    override def isAssignable(reference: NetworkObjectReference): Boolean = true //all remaining network objects are allowed

    override def findObject(reference: NetworkObjectReference): Option[NetworkObject[_ <: NetworkObjectReference]] = {
        map.get(reference)
    }

    override def injectRequest(bundle: LinkerRequestBundle): Unit = super.handleBundle(bundle)

    def addObject(no: NetworkObject[NetworkObjectReference]): Unit = {
        val reference = no.reference
        map.put(reference, no)
        registerReference(reference)
    }

}
