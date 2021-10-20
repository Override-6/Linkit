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

import fr.linkit.api.application.ApplicationReference
import fr.linkit.api.application.connection.NetworkConnectionReference
import fr.linkit.api.gnom.cache.SharedCacheManagerReference
import fr.linkit.api.gnom.network.{Network, NetworkReference}
import fr.linkit.api.gnom.packet.channel.request.RequestPacketBundle
import fr.linkit.api.gnom.persistence.obj.TrafficReference
import fr.linkit.api.gnom.reference._
import fr.linkit.api.gnom.reference.presence.NetworkObjectPresence
import fr.linkit.api.gnom.reference.traffic.{LinkerRequestBundle, ObjectManagementChannel, TrafficInterestedNPH}
import fr.linkit.engine.gnom.network.AbstractGeneralNetworkObjectLinker.ReferenceAttributeKey
import fr.linkit.engine.gnom.packet.UnexpectedPacketException
import fr.linkit.engine.gnom.packet.traffic.channel.request.DefaultRequestBundle
import fr.linkit.engine.gnom.reference.NOLUtils.throwUnknownRef

class GeneralNetworkObjectLinkerImpl(omc: ObjectManagementChannel,
                                     network: Network,
                                     override val cacheNOL: InitialisableNetworkObjectLinker[SharedCacheManagerReference]
                                             with TrafficInterestedNPH,
                                     override val trafficNOL: NetworkObjectLinker[TrafficReference]
                                             with TrafficInterestedNPH)
        extends GeneralNetworkObjectLinker {

    private val connection  = network.connection
    private val application = connection.getApp

    startObjectManagement()

    override def isPresentOnEngine(engineID: String, reference: NetworkObjectReference): Boolean = reference match {
        case ref: SharedCacheManagerReference => cacheNOL.isPresentOnEngine(engineID, ref)
        case ref: TrafficReference            => trafficNOL.isPresentOnEngine(engineID, ref)
        case _: SystemObjectReference         => true //System references, are guaranteed to be present on every remote engines
        case _                                => throwUnknownRef(reference)
    }

    override def findPresence(reference: NetworkObjectReference): Option[NetworkObjectPresence] = reference match {
        case ref: SharedCacheManagerReference => cacheNOL.findPresence(ref)
        case ref: TrafficReference            => trafficNOL.findPresence(ref)
        case _: SystemObjectReference         =>
            /* As other NetworkReferences are guaranteed to be
            * to be present on every engines as long as they come from the framework's system,
            * The result of their presence on any engine will be always present if they are legit objects.
            */
            Some(SystemNetworkObjectPresence)
        case _                                => throwUnknownRef(reference)
    }
    /*
    Use once more system object are added.
    private def findSystemObject(reference: NetworkReference): Option[NetworkObject[_ <: NetworkReference]] = reference match {
        case _: NetworkConnectionReference => Some(network.connection)
        case _                             => Some(network)
    }*/

    override def findObject(reference: NetworkObjectReference): Option[NetworkObject[_ <: NetworkObjectReference]] = reference match {
        case ref: SharedCacheManagerReference                 => cacheNOL.findObject(ref)
        case ref: TrafficReference                            => trafficNOL.findObject(ref)
        case _: NetworkConnectionReference                    => Some(connection)
        case ref: NetworkReference if ref == NetworkReference => Some(network)
        case _: ApplicationReference                          => Some(application)
        case _                                                => throwUnknownRef(reference)
    }

    private def handleRequest(request: RequestPacketBundle): Unit = {
        val ref       = request.attributes.getAttribute[NetworkObjectReference](ReferenceAttributeKey).getOrElse {
            throw UnexpectedPacketException(s"Could not find attribute '$ReferenceAttributeKey' in object management request bundle $request")
        }
        val omcBundle = new DefaultRequestBundle(omc, request.packet, request.coords, request.responseSubmitter) with LinkerRequestBundle {
            override val linkerReference: NetworkObjectReference = ref
        }
        ref match {
            case _: SharedCacheManagerReference => cacheNOL.injectRequest(omcBundle)
            case _: TrafficReference            => trafficNOL.injectRequest(omcBundle)
            case _: SystemObjectReference       => throw UnexpectedPacketException("Could not handle Object Manager Request for System objects")
            case _                              => throwUnknownRef(ref)
        }
    }

    private def startObjectManagement(): Unit = {
        omc.addRequestListener(handleRequest)
    }

}

object AbstractGeneralNetworkObjectLinker {

    final val ReferenceAttributeKey: String = "ref"
}
