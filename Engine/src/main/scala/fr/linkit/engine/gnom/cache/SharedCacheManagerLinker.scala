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

import fr.linkit.api.gnom.cache.{NoSuchCacheManagerException, SharedCacheManagerReference, SharedCacheReference}
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.reference.presence.NetworkObjectPresence
import fr.linkit.api.gnom.reference.traffic.{LinkerRequestBundle, ObjectManagementChannel}
import fr.linkit.api.gnom.reference.{NetworkObject, NetworkObjectReference}
import fr.linkit.api.gnom.reference.linker.InitialisableNetworkObjectLinker
import fr.linkit.engine.gnom.reference.AbstractNetworkPresenceHandler
import fr.linkit.engine.gnom.reference.NOLUtils._

class SharedCacheManagerLinker(network: Network, omc: ObjectManagementChannel)
        extends AbstractNetworkPresenceHandler[SharedCacheManagerReference](null, omc)
                with InitialisableNetworkObjectLinker[SharedCacheManagerReference] {

    override def isAssignable(reference: NetworkObjectReference): Boolean = reference.isInstanceOf[SharedCacheManagerReference]

    override def isPresentOnEngine(engineId: String, ref: SharedCacheManagerReference): Boolean = {
        val manager = network.findCacheManager(ref.family)
        ref match {
            case ref: SharedCacheReference      => manager.exists(_.getCachesLinker.isPresentOnEngine(engineId, ref))
            case _: SharedCacheManagerReference => super.isPresentOnEngine(engineId, ref)
            case _                              => throwUnknownRef(ref)
        }
    }

    override def findPresence(ref: SharedCacheManagerReference): Option[NetworkObjectPresence] = {
        network.findCacheManager(ref.family).fold[Option[NetworkObjectPresence]](None) { manager =>
            ref match {
                case ref: SharedCacheReference        => manager.getCachesLinker.findPresence(ref)
                case ref: SharedCacheManagerReference => super.findPresence(ref)
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
                network.findCacheManager(ref.family)
                        .fold(throw new NoSuchElementException(s"Could not find cache manager '${ref.family}'."))
                { manager =>
                    manager.getCachesLinker.injectRequest(bundle)
                }
            case _: SharedCacheManagerReference => super.injectRequest(bundle)
            case ref                            => throwUnknownRef(ref)
        }
    }

    override def initializeObject(obj: NetworkObject[_ <: SharedCacheManagerReference]): Unit = {
        obj.reference match {
            case ref: SharedCacheReference      =>
                network.findCacheManager(ref.family).fold(
                    throw new NoSuchCacheManagerException(s"Could not initialize object located at ${ref}: no such cache manager '${ref.family}")
                ) { manager =>
                    manager.getCachesLinker.initializeObject(obj.asInstanceOf[NetworkObject[_ <: SharedCacheReference]])
                }
            case _: SharedCacheManagerReference =>
                throw new UnsupportedOperationException(s"Can not initialize Shared Cache Manager. Shared Cache Manager can only be opened using Network#declareNewCacheManager.")
            case ref                            => throwUnknownRef(ref)
        }
    }

    override def registerReference(ref: SharedCacheManagerReference): Unit = super.registerReference(ref)

    override def unregisterReference(ref: SharedCacheManagerReference): Unit = super.unregisterReference(ref)
}
