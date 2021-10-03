/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.gnom.network

import fr.linkit.api.gnom.cache.{SharedCacheManager, SharedCacheManagerReference}
import fr.linkit.api.gnom.network.{Engine, EngineReference, Network, NetworkReference}
import fr.linkit.api.gnom.packet.PacketCoordinates
import fr.linkit.api.gnom.packet.channel.request.RequestPacketBundle
import fr.linkit.api.gnom.persistence.obj.TrafficNetworkObjectReference
import fr.linkit.api.gnom.reference.presence.ObjectNetworkPresence
import fr.linkit.api.gnom.reference.traffic.{LinkerRequestBundle, ObjectManagementChannel, TrafficInterestedNOL}
import fr.linkit.api.gnom.reference.{NetworkObject, NetworkObjectLinker, NetworkObjectReference, SystemObjectNetworkPresence}
import fr.linkit.engine.gnom.network.GeneralNetworkObjectLinker.ReferenceAttributeKey
import fr.linkit.engine.gnom.packet.UnexpectedPacketException
import fr.linkit.engine.gnom.packet.traffic.channel.request.DefaultRequestBundle

class GeneralNetworkObjectLinker(omc: ObjectManagementChannel,
                                 network: Network,
                                 cacheNOL: NetworkObjectLinker[SharedCacheManagerReference] with TrafficInterestedNOL,
                                 trafficNOL: NetworkObjectLinker[TrafficNetworkObjectReference] with TrafficInterestedNOL) extends NetworkObjectLinker[NetworkObjectReference] {

    startObjectManagement()

    override def isObjectReferencedOnCurrent(obj: NetworkObject[NetworkObjectReference]): Boolean = {
        obj.reference match {
            case _: NetworkReference              =>
                isSystemObject(obj)
            case _: SharedCacheManagerReference   => cacheNOL.isObjectReferencedOnCurrent(trustedCast(obj))
            case _: TrafficNetworkObjectReference => trafficNOL.isObjectReferencedOnCurrent(trustedCast(obj))
            case _                                => throwUnknownObject(obj)
        }

    }

    override def isObjectReferencedOnEngine(engineID: String, obj: NetworkObject[NetworkObjectReference]): Boolean = {
        obj.reference match {
            case _: NetworkReference              =>
                /* As Network, Engine and SCM are guaranteed
                 * to be present on every engines as long as they come from the framework's system,
                 * The result of their presence on any engine can be determined by their registration status onto this engine.
                 */
                isSystemObject(obj)
            case _: SharedCacheManagerReference   => cacheNOL.isObjectReferencedOnEngine(engineID, trustedCast(obj))
            case _: TrafficNetworkObjectReference => trafficNOL.isObjectReferencedOnEngine(engineID, trustedCast(obj))
            case _                                => throwUnknownObject(obj)
        }
    }

    override def findObjectPresence(obj: NetworkObject[NetworkObjectReference]): Option[ObjectNetworkPresence] = {
        obj.reference match {
            case _: NetworkReference if isSystemObject(obj) =>
                /* As Network, Engine and SCM are guaranteed
                * to be present on every engines as long as they come from the framework's system,
                * The result of their presence on any engine will be always present if they are legit objects.
                */
                Some(new SystemObjectNetworkPresence())
            case _: SharedCacheManagerReference             => cacheNOL.findObjectPresence(trustedCast(obj))
            case _: TrafficNetworkObjectReference           => trafficNOL.findObjectPresence(trustedCast(obj))
            case _                                          => throwUnknownObject(obj)
        }
    }

    override def findObject(coordsOrigin: PacketCoordinates, location: NetworkObjectReference): Option[NetworkObject[_ <: NetworkObjectReference]] = {
        location match {
            case _: NetworkReference                => Some(network)
            case ref: EngineReference               => network.findEngine(ref.engineID)
            case ref: SharedCacheManagerReference   => cacheNOL.findObject(coordsOrigin, ref)
            case ref: TrafficNetworkObjectReference => trafficNOL.findObject(coordsOrigin, ref)
            case _                                  =>
                throwUnknownRef(location)
        }
    }

    private def isSystemObject(obj: NetworkObject[_]): Boolean = obj match {
        case network: Network            => network eq this.network
        case engine: Engine              => network.findEngine(engine.identifier).exists(_ eq engine)
        case manager: SharedCacheManager => network.findCacheManager(manager.family).exists(_ eq manager)
        case _                           => throwUnknownObject(obj)
    }

    @inline
    private def trustedCast[T <: NetworkObjectReference](obj: NetworkObject[_]): NetworkObject[T] = {
        obj.asInstanceOf[NetworkObject[T]]
    }

    private def startObjectManagement(): Unit = {
        omc.addRequestListener(handleRequest)
    }

    private def handleRequest(request: RequestPacketBundle): Unit = {
        val ref       = request.attributes.getAttribute[NetworkObjectReference](ReferenceAttributeKey).getOrElse {
            throw UnexpectedPacketException(s"Could not find attribute '$ReferenceAttributeKey' in object management request bundle $request")
        }
        val omcBundle = new DefaultRequestBundle(omc, request.packet, request.coords, request.responseSubmitter) with LinkerRequestBundle {
            override val linkerReference: NetworkObjectReference = ref
        }
        ref match {
            case _: SharedCacheManagerReference   => cacheNOL.injectRequest(omcBundle)
            case _: TrafficNetworkObjectReference => trafficNOL.injectRequest(omcBundle)
            case _: NetworkReference              => throw UnexpectedPacketException("Could not handle Object Manager Request for System objects")
            case _                                => throwUnknownRef(ref)
        }
    }

    @inline
    private def throwUnknownObject(ref: AnyRef): Nothing = {
        throw new IllegalArgumentException(s"Unknown object '$ref' for the General Network Object Linker")
    }

    @inline
    private def throwUnknownRef(ref: NetworkObjectReference): Nothing = {
        throw new IllegalArgumentException(s"Unknown object reference '$ref' for the General Network Object Linker")
    }

}

object GeneralNetworkObjectLinker {

    private final val ReferenceAttributeKey: String = "ref"
}
