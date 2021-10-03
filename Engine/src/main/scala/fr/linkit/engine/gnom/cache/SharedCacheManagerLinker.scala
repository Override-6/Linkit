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

package fr.linkit.engine.gnom.cache

import fr.linkit.api.gnom.cache.{SharedCacheManager, SharedCacheManagerReference, SharedCacheReference}
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.packet.PacketCoordinates
import fr.linkit.api.gnom.reference.presence.ObjectNetworkPresence
import fr.linkit.api.gnom.reference.traffic.{ObjectManagementChannel, TrafficInterestedNOL}
import fr.linkit.api.gnom.reference.{NetworkObject, NetworkObjectLinker, NetworkObjectReference}
import fr.linkit.engine.gnom.reference.AbstractNetworkPresenceHandler
import fr.linkit.engine.gnom.reference.NOLUtils._

class SharedCacheManagerLinker(network: Network, omc: ObjectManagementChannel) extends AbstractNetworkPresenceHandler[SharedCacheManager, SharedCacheManagerReference](omc) with NetworkObjectLinker[SharedCacheManagerReference] with TrafficInterestedNOL {

    override def isObjectReferencedOnCurrent(obj: NetworkObject[SharedCacheManagerReference]): Boolean = {
        val manager = network.findCacheManager(obj.reference.family)
        obj.reference match {
            case _: SharedCacheReference        => manager.exists(_.getCachesLinker.isObjectReferencedOnCurrent(trustedCast(obj)))
            case _: SharedCacheManagerReference => manager.isDefined
            case _                              => throwUnknownObject(obj)
        }
    }

    override def isObjectReferencedOnEngine(engineID: String, obj: NetworkObject[SharedCacheManagerReference]): Boolean = {
        val manager = network.findCacheManager(obj.reference.family)
        obj.reference match {
            case _: SharedCacheReference        => manager.exists(_.getCachesLinker.isObjectReferencedOnEngine(engineID, trustedCast(obj)))
            case _: SharedCacheManagerReference => manager.isDefined
            case _                              => throwUnknownObject(obj)
        }
    }

    override def findObjectPresence(obj: NetworkObject[SharedCacheManagerReference]): Option[ObjectNetworkPresence] = {
        val manager = network.findCacheManager(obj.reference.family)
        obj.reference match {
            case _: SharedCacheReference                => manager.flatMap(_.getCachesLinker.findObjectPresence(trustedCast(obj)))
            case reference: SharedCacheManagerReference => Some(getPresence(reference))
            case _                                      => throwUnknownObject(obj)
        }
    }

    override def findObject(coordsOrigin: PacketCoordinates, reference: SharedCacheManagerReference): Option[NetworkObject[_ <: SharedCacheManagerReference]] = {
        val manager = network.findCacheManager(reference.family)
        reference match {
            case _: SharedCacheReference        => manager.flatMap(_.getCachesLinker.findObject(coordsOrigin, reference.asInstanceOf[SharedCacheReference]))
            case _: SharedCacheManagerReference => manager
            case _                              => throwUnknownRef(reference)
        }
    }

    @inline
    private def trustedCast[T <: NetworkObjectReference](obj: NetworkObject[_]): NetworkObject[T] = {
        obj.asInstanceOf[NetworkObject[T]]
    }
}
