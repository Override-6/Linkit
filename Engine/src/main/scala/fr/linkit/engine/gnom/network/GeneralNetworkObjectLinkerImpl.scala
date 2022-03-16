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
import fr.linkit.api.gnom.reference.linker.{GeneralNetworkObjectLinker, InitialisableNetworkObjectLinker, NetworkObjectLinker, RemainingNetworkObjectsLinker}
import fr.linkit.api.gnom.reference.presence.NetworkObjectPresence
import fr.linkit.api.gnom.reference.traffic.{LinkerRequestBundle, ObjectManagementChannel, TrafficInterestedNPH}
import fr.linkit.engine.gnom.network.GeneralNetworkObjectLinkerImpl.ReferenceAttributeKey
import fr.linkit.engine.gnom.packet.UnexpectedPacketException
import fr.linkit.engine.gnom.packet.traffic.channel.request.DefaultRequestBundle
import fr.linkit.engine.gnom.reference.NOLUtils.throwUnknownRef

import scala.collection.mutable.ListBuffer

class GeneralNetworkObjectLinkerImpl(omc: ObjectManagementChannel,
                                     network: Network,
                                     override val cacheNOL: InitialisableNetworkObjectLinker[SharedCacheManagerReference] with TrafficInterestedNPH,
                                     override val trafficNOL: NetworkObjectLinker[TrafficReference] with TrafficInterestedNPH,
                                     override val remainingNOL: Option[RemainingNetworkObjectsLinker with TrafficInterestedNPH])
    extends GeneralNetworkObjectLinker {

    private val connection   = network.connection
    private val application  = connection.getApp
    private val otherLinkers = ListBuffer.empty[NetworkObjectLinker[NetworkObjectReference]]

    startObjectManagement()

    override def addRootLinker(linker: NetworkObjectLinker[NetworkObjectReference]): Unit = {
        otherLinkers += linker
    }


    /**
     *
     * @param reference the reference to test
     * @return true if a [[NetworkObjectLinker]] (excepted remainingNOL) is able to handle the given reference.
     */
    override def touchesAnyLinker(reference: NetworkObjectReference): Boolean = {
        reference.isInstanceOf[SharedCacheManagerReference] ||
            reference.isInstanceOf[TrafficReference] ||
            reference.isInstanceOf[SystemObjectReference] ||
            otherLinkers.exists(_.isAssignable(reference))
    }

    override def isPresentOnEngine(engineID: String, reference: NetworkObjectReference): Boolean = reference match {
        case ref: SharedCacheManagerReference => cacheNOL.isPresentOnEngine(engineID, ref)
        case ref: TrafficReference            => trafficNOL.isPresentOnEngine(engineID, ref)
        case _: SystemObjectReference         => true //System references, are guaranteed to be present on every remote engines
        case _                                =>
            otherLinkers.find(_.isAssignable(reference)) match {
                case Some(linker) => linker.isPresentOnEngine(engineID, reference)
                case None         => remainingNOL.exists(_.isPresentOnEngine(engineID, reference))
            }
    }

    override def findPresence(reference: NetworkObjectReference): Option[NetworkObjectPresence] = reference match {
        case ref: SharedCacheManagerReference => cacheNOL.findPresence(ref)
        case ref: TrafficReference            => trafficNOL.findPresence(ref)
        case _: SystemObjectReference         =>
            /* As other NetworkReferences are guaranteed
            * to be present on every engines,
            * The result of their presence on any engine will be always present if they are legit objects.
            */
            Some(SystemNetworkObjectPresence)
        case _                                =>
            otherLinkers.find(_.isAssignable(reference)) match {
                case Some(linker) => linker.findPresence(reference)
                case None         => remainingNOL.fold(throwUnknownRef(reference))(_.findPresence(reference))
            }
    }

    override def findObject(reference: NetworkObjectReference): Option[NetworkObject[_ <: NetworkObjectReference]] = reference match {
        case ref: SharedCacheManagerReference => cacheNOL.findObject(ref)
        case ref: TrafficReference            => trafficNOL.findObject(ref)
        case _: NetworkConnectionReference    => Some(connection)
        case NetworkReference                 => Some(network)
        case _: ApplicationReference          => Some(application)
        case _                                =>
            otherLinkers.find(_.isAssignable(reference)) match {
                case Some(linker) => linker.findObject(reference)
                case None         => remainingNOL.fold(throwUnknownRef(reference))(_.findObject(reference))
            }
    }

    private def handleRequest(request: RequestPacketBundle): Unit = {
        val reference    = request.attributes.getAttribute[NetworkObjectReference](ReferenceAttributeKey).getOrElse {
            throw UnexpectedPacketException(s"Could not find attribute '$ReferenceAttributeKey' in object management request bundle $request")
        }
        val linkerBundle = new DefaultRequestBundle(omc, request.packet, request.coords, request.responseSubmitter) with LinkerRequestBundle {
            override val linkerReference: NetworkObjectReference = reference
        }
        reference match {
            case _: SharedCacheManagerReference => cacheNOL.injectRequest(linkerBundle)
            case _: TrafficReference            => trafficNOL.injectRequest(linkerBundle)
            case _: SystemObjectReference       => throw UnexpectedPacketException("Could not handle Object Manager Request for System objects")
            case _                              =>
                otherLinkers.find(_.isAssignable(reference)) match {
                    case Some(linker: TrafficInterestedNPH) => linker.findPresence(reference)
                    case None                               => remainingNOL.fold()(_.injectRequest(linkerBundle))
                }
        }
    }

    private def startObjectManagement(): Unit = {
        omc.addRequestListener(handleRequest)
    }

    override def isAssignable(reference: NetworkObjectReference): Boolean = true //all kind of references are accepted at the root of the network object management
}

object GeneralNetworkObjectLinkerImpl {

    final val ReferenceAttributeKey: String = "ref"
}
