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
import fr.linkit.api.gnom.reference.presence.NetworkObjectPresence
import fr.linkit.api.gnom.reference.traffic.{LinkerRequestBundle, ObjectManagementChannel}
import fr.linkit.api.gnom.reference.{NetworkObject, NetworkObjectLinker}
import fr.linkit.engine.gnom.reference.AbstractNetworkPresenceHandler
import fr.linkit.engine.gnom.reference.NOLUtils._

class SharedCacheManagerLinker(network: Network, omc: ObjectManagementChannel)
        extends AbstractNetworkPresenceHandler[SharedCacheManager, SharedCacheManagerReference](omc)
                with NetworkObjectLinker[SharedCacheManagerReference] {

    override def isPresentOnEngine(engineId: String, ref: SharedCacheManagerReference): Boolean = {
        val manager = network.findCacheManager(ref.family)
        ref match {
            case ref: SharedCacheReference        => manager.exists(_.getCachesLinker.isPresentOnEngine(engineId, ref))
            case _: SharedCacheManagerReference => super.isPresentOnEngine(engineId, ref)
            case _                              => throwUnknownRef(ref)
        }
    }

    override def getPresence(ref: SharedCacheManagerReference): Option[NetworkObjectPresence] = {
        network.findCacheManager(ref.family).fold[Option[NetworkObjectPresence]](None) { manager =>
            ref match {
                case ref: SharedCacheReference          => manager.getCachesLinker.getPresence(ref)
                case ref: SharedCacheManagerReference => super.getPresence(ref)
            }
        }
    }

    override def findObject(reference: SharedCacheManagerReference): Option[NetworkObject[_ <: SharedCacheManagerReference]] = {
        val manager = network.findCacheManager(reference.family)
        reference match {
            case _: SharedCacheReference        => manager.flatMap(_.getCachesLinker.findObject(reference.asInstanceOf[SharedCacheReference]))
            case _: SharedCacheManagerReference => manager
            case _                              => throwUnknownRef(reference)
        }
    }

    override def injectRequest(bundle: LinkerRequestBundle): Unit = {
        bundle.linkerReference match {
            case ref: SharedCacheReference      =>
                network.findCacheManager(ref.family).fold() { manager =>
                    manager.getCachesLinker.injectRequest(bundle)
                }
            case _: SharedCacheManagerReference => handleBundle(bundle)
            case ref                            => throwUnknownRef(ref)
        }
    }

}
