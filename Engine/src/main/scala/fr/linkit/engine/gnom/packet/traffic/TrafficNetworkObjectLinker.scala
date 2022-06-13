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

package fr.linkit.engine.gnom.packet.traffic

import fr.linkit.api.gnom.persistence.context.ContextualObjectReference
import fr.linkit.api.gnom.persistence.obj.{TrafficObjectReference, TrafficReference}
import fr.linkit.api.gnom.reference.traffic.{LinkerRequestBundle, ObjectManagementChannel}
import fr.linkit.api.gnom.reference.{NetworkObject, NetworkObjectReference}
import fr.linkit.api.gnom.reference.linker.NetworkObjectLinker
import fr.linkit.engine.gnom.reference.AbstractNetworkPresenceHandler

class TrafficNetworkObjectLinker(omc: ObjectManagementChannel, traffic: AbstractPacketTraffic) extends
        AbstractNetworkPresenceHandler[TrafficReference](null, omc) with NetworkObjectLinker[TrafficReference] {

    override def findObject(reference: TrafficReference): Option[NetworkObject[_ <: TrafficReference]] = {
        reference match {
            case reference: ContextualObjectReference                                   =>
                traffic.getPersistenceConfig(reference.trafficPath)
                        .contextualObjectLinker
                        .findObject(reference)
            case reference: TrafficObjectReference if reference.trafficPath.length == 0 =>
                Some(traffic.getObjectManagementChannel)
            case ref if ref == TrafficReference                                         =>
                Some(traffic)
            case reference: TrafficObjectReference                                      =>
                traffic.findTrafficObject(reference)
        }
    }
    
    
    override def injectRequest(bundle: LinkerRequestBundle): Unit = {
        val reference = bundle.linkerReference
        reference match {
            case reference: ContextualObjectReference =>
                traffic.getPersistenceConfig(reference.trafficPath)
                        .contextualObjectLinker
                        .injectRequest(bundle)
            case _                                    =>
                super.injectRequest(bundle)
        }
    }

    override def registerReference(ref: TrafficReference): Unit = super.registerReference(ref)

    override def unregisterReference(ref: TrafficReference): Unit = super.unregisterReference(ref)

    override def isAssignable(reference: NetworkObjectReference): Boolean = reference.isInstanceOf[TrafficReference]
}
