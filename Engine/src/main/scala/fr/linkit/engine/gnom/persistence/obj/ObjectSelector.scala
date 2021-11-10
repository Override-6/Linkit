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

package fr.linkit.engine.gnom.persistence.obj

import fr.linkit.api.gnom.cache.SharedCacheManagerReference
import fr.linkit.api.gnom.persistence.PersistenceBundle
import fr.linkit.api.gnom.persistence.context.ContextualObjectReference
import fr.linkit.api.gnom.reference.{DynamicNetworkObject, NetworkObject, NetworkObjectReference, StaticNetworkObject}
import fr.linkit.engine.gnom.reference.ContextObject

class ObjectSelector(bundle: PersistenceBundle) {

    private val gnol        = bundle.gnol
    private val col         = bundle.config.contextualObjectLinker
    private val coords      = bundle.coordinates
    private val channelPath = coords.path
    private val cnol        = gnol.cacheNOL

    def findObjectReference(obj: AnyRef): Option[NetworkObjectReference] = {
        obj match {
            case obj: DynamicNetworkObject[NetworkObjectReference] =>
                if (obj.presence.isPresentOn(coords))
                    Some(obj.reference)
                else
                    None
            case obj: StaticNetworkObject[NetworkObjectReference]  =>
                Some(obj.reference)
            case obj: NetworkObject[NetworkObjectReference]        =>
                val reference = obj.reference
                if (gnol.findPresence(reference).exists(_.isPresentOn(coords)))
                    Some(reference)
                else None
            case _                                                 =>
                col
                        .findPersistableReference(obj, coords)
                        .map(new ContextualObjectReference(channelPath, _))
        }
    }

    def findObject(reference: NetworkObjectReference): Option[AnyRef] = {
        gnol.findObject(reference) match {
            case Some(value: ContextObject) => Some(value.obj)
            case o                          => o
        }
    }

    def initObject(obj: NetworkObject[SharedCacheManagerReference]): Unit = {
        cnol.initializeObject(obj)
    }

}
