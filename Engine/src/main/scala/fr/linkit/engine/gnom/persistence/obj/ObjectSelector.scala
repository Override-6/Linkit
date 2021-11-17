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
    private val rnol        = gnol.remainingNOL
    private val col         = bundle.config.contextualObjectLinker
    private val coords      = bundle.coordinates
    private val channelPath = coords.path
    private val cnol        = gnol.cacheNOL

    def findObjectReference(obj: AnyRef): Option[NetworkObjectReference] = {
        if (!obj.isInstanceOf[NetworkObject[_]]) {
            return col
                    .findPersistableReference(obj, coords)
                    .map(new ContextualObjectReference(channelPath, _))
        }
        obj match {
            case obj: DynamicNetworkObject[NetworkObjectReference] =>
                val reference = obj.reference
                if (obj.presence.isPresentOn(coords))
                    Some(reference)
                else {
                    if (rnol.isDefined && !gnol.touchesAnyLinker(reference))
                        rnol.get.save(obj)
                    None
                }
            case obj: StaticNetworkObject[NetworkObjectReference]  =>
                Some(obj.reference)
            case obj: NetworkObject[NetworkObjectReference]        =>
                val reference = obj.reference
                if (gnol.findPresence(reference).exists(_.isPresentOn(coords)))
                    Some(reference)
                else {
                    if (rnol.isDefined && !gnol.touchesAnyLinker(reference))
                        rnol.get.save(obj)
                    None
                }
        }
    }

    def findObject(reference: NetworkObjectReference): Option[AnyRef] = {
        gnol.findObject(reference) match {
            case Some(value: ContextObject) => Some(value.obj)
            case o                          => o
        }
    }

    def handleObject(obj: NetworkObject[NetworkObjectReference]): Unit = {
        val reference = obj.reference
        if (reference.isInstanceOf[SharedCacheManagerReference])
            cnol.initializeObject(obj.asInstanceOf[NetworkObject[SharedCacheManagerReference]])
        if (rnol.isDefined && !gnol.touchesAnyLinker(reference))
            rnol.get.save(obj)
    }

}
