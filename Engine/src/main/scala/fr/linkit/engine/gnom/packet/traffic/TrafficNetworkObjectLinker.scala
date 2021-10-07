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
import fr.linkit.api.gnom.persistence.obj.TrafficNetworkPresenceReference
import fr.linkit.api.gnom.reference.traffic.{LinkerRequestBundle, ObjectManagementChannel}
import fr.linkit.api.gnom.reference.{NetworkObject, NetworkObjectLinker}
import fr.linkit.engine.gnom.reference.AbstractNetworkPresenceHandler

class TrafficNetworkObjectLinker(omc: ObjectManagementChannel, traffic: AbstractPacketTraffic) extends
        AbstractNetworkPresenceHandler[TrafficNetworkPresenceReference](omc) with NetworkObjectLinker[TrafficNetworkPresenceReference] {

    override def findObject(reference: TrafficNetworkPresenceReference): Option[NetworkObject[_ <: TrafficNetworkPresenceReference]] = {
        reference match {
            case reference: ContextualObjectReference   =>
                traffic.getPersistenceConfig(reference.trafficPath)
                        .contextualObjectLinker
                        .findObject(reference)
            case _ if reference.trafficPath.length == 0 =>
                Some(traffic)
            case _                                      =>
                traffic.findPresence(reference)
        }
    }

    override def injectRequest(bundle: LinkerRequestBundle): Unit = {
        val reference = bundle.linkerReference
        reference match {
            case reference: ContextualObjectReference =>
                traffic.getPersistenceConfig(reference.trafficPath)
                        .contextualObjectLinker
                        .injectRequest(bundle)
            case _ =>
                handleBundle(bundle)
        }
    }
}
