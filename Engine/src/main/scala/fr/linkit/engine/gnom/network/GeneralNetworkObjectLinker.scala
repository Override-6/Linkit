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

package fr.linkit.engine.gnom.network

import fr.linkit.api.gnom.cache.SharedCacheManagerReference
import fr.linkit.api.gnom.network.{EngineReference, Network, NetworkReference}
import fr.linkit.api.gnom.packet.channel.request.RequestPacketBundle
import fr.linkit.api.gnom.persistence.obj.TrafficNetworkPresenceReference
import fr.linkit.api.gnom.reference.presence.NetworkObjectPresence
import fr.linkit.api.gnom.reference.traffic.{LinkerRequestBundle, ObjectManagementChannel, TrafficInterestedNPH}
import fr.linkit.api.gnom.reference.{NetworkObject, NetworkObjectLinker, NetworkObjectReference, SystemNetworkObjectPresence}
import fr.linkit.engine.gnom.network.GeneralNetworkObjectLinker.ReferenceAttributeKey
import fr.linkit.engine.gnom.packet.UnexpectedPacketException
import fr.linkit.engine.gnom.packet.traffic.channel.request.DefaultRequestBundle
import fr.linkit.engine.gnom.reference.NOLUtils.throwUnknownRef

class GeneralNetworkObjectLinker(omc: ObjectManagementChannel,
                                 network: Network,
                                 cacheNOL: NetworkObjectLinker[SharedCacheManagerReference] with TrafficInterestedNPH,
                                 trafficNOL: NetworkObjectLinker[TrafficNetworkPresenceReference] with TrafficInterestedNPH) extends NetworkObjectLinker[NetworkObjectReference] {

    startObjectManagement()

    override def isPresentOnEngine(engineID: String, reference: NetworkObjectReference): Boolean = reference match {
        case _: NetworkReference                  => true //Network references are guaranteed to be present on every remote engines
        case ref: SharedCacheManagerReference     => cacheNOL.isPresentOnEngine(engineID, ref)
        case ref: TrafficNetworkPresenceReference => trafficNOL.isPresentOnEngine(engineID, ref)
        case _                                    => throwUnknownRef(reference)
    }

    override def getPresence(reference: NetworkObjectReference): Option[NetworkObjectPresence] = reference match {
        case _: NetworkReference                  =>
            /* As Network, Engine and SCM are guaranteed
            * to be present on every engines as long as they come from the framework's system,
            * The result of their presence on any engine will be always present if they are legit objects.
            */
            Some(new SystemNetworkObjectPresence())
        case ref: SharedCacheManagerReference     => cacheNOL.getPresence(ref)
        case ref: TrafficNetworkPresenceReference => trafficNOL.getPresence(ref)
        case _                                    => throwUnknownRef(reference)
    }

    override def findObject(reference: NetworkObjectReference): Option[NetworkObject[_ <: NetworkObjectReference]] = reference match {
        case _: NetworkReference                  => Some(network)
        //case ref: EngineReference                 => network.findEngine(ref.engineID)
        case ref: SharedCacheManagerReference     => cacheNOL.findObject(ref)
        case ref: TrafficNetworkPresenceReference => trafficNOL.findObject(ref)
        case _                                    => throwUnknownRef(reference)
    }

    private def handleRequest(request: RequestPacketBundle): Unit = {
        val ref       = request.attributes.getAttribute[NetworkObjectReference](ReferenceAttributeKey).getOrElse {
            throw UnexpectedPacketException(s"Could not find attribute '$ReferenceAttributeKey' in object management request bundle $request")
        }
        val omcBundle = new DefaultRequestBundle(omc, request.packet, request.coords, request.responseSubmitter) with LinkerRequestBundle {
            override val linkerReference: NetworkObjectReference = ref
        }
        ref match {
            case _: SharedCacheManagerReference     => cacheNOL.injectRequest(omcBundle)
            case _: TrafficNetworkPresenceReference => trafficNOL.injectRequest(omcBundle)
            case _: NetworkReference                => throw UnexpectedPacketException("Could not handle Object Manager Request for System objects")
            case _                                  => throwUnknownRef(ref)
        }
    }

    private def startObjectManagement(): Unit = {
        omc.addRequestListener(handleRequest)
    }

}

object GeneralNetworkObjectLinker {

    final val ReferenceAttributeKey: String = "ref"
}
