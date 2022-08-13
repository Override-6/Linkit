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

package fr.linkit.engine.gnom.referencing.linker

import fr.linkit.api.gnom.referencing.linker.NetworkObjectLinker
import fr.linkit.api.gnom.referencing.traffic.ObjectManagementChannel
import fr.linkit.api.gnom.referencing.{NetworkObject, NetworkObjectReference}
import fr.linkit.engine.gnom.referencing.presence.AbstractNetworkPresenceHandler

import scala.collection.mutable

private[gnom] class MapNetworkObjectLinker(omc: ObjectManagementChannel) extends AbstractNetworkPresenceHandler[NetworkObjectReference](null, omc) with NetworkObjectLinker[NetworkObjectReference] {

    private val map = mutable.HashMap.empty[NetworkObjectReference, NetworkObject[NetworkObjectReference]]

    override def findObject(reference: NetworkObjectReference): Option[NetworkObject[_ <: NetworkObjectReference]] = {
        map.get(reference)
    }

    def save(no: NetworkObject[NetworkObjectReference]): Unit = put(no)

    def unsave(ref: NetworkObjectReference): Unit = remove(ref)

    def put(no: NetworkObject[NetworkObjectReference]): Unit = {
        put(no.reference, no)
    }

    def put[R <: NetworkObjectReference](reference: R, no: NetworkObject[R]): Unit = {
        if (reference == null)
            throw new NullPointerException("Network Object's reference is null.")
        map.put(reference, no)
        registerReference(reference)
    }

    def remove(ref: NetworkObjectReference): Unit = {
        map.remove(ref)
    }


}
