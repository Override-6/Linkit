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
import fr.linkit.api.gnom.cache.sync.{OriginReferencedConnectedObjectReference, SynchronizedObject}
import fr.linkit.api.gnom.persistence.PersistenceBundle
import fr.linkit.api.gnom.persistence.context.ContextualObjectReference
import fr.linkit.api.gnom.reference.{DynamicNetworkObject, NetworkObject, NetworkObjectReference, StaticNetworkObject, SystemNetworkObjectPresence}
import fr.linkit.engine.gnom.reference.ContextObject
import fr.linkit.engine.gnom.reference.presence.{ExternalNetworkObjectPresence, InternalNetworkObjectPresence}

class ObjectSelector(bundle: PersistenceBundle) {

    private val gnol       = bundle.network.gnol
    private val rnol       = gnol.remainingNOL
    private val col        = bundle.config.contextualObjectLinker
    private val boundId    = bundle.boundId
    private val packetPath = bundle.packetPath
    private val cnol       = gnol.cacheNOL

    def findObjectReference(obj: AnyRef): Option[NetworkObjectReference] = {
        if (!obj.isInstanceOf[NetworkObject[_]]) {
            return findNonNetworkObjectReference(obj)
        }
        val found = obj match {
            case sync: SynchronizedObject[_]                       =>
                findSyncObjectReference(sync)
            case obj: DynamicNetworkObject[NetworkObjectReference] =>
                findDynamicNetworkObjectReference(obj)
            case obj: StaticNetworkObject[NetworkObjectReference]  =>
                Some(obj.reference)
            case obj: NetworkObject[NetworkObjectReference]        =>
                findNetworkObjectReference(obj)
        }
        found
    }

    private def findSyncObjectReference(obj: SynchronizedObject[_]): Option[NetworkObjectReference] = {
        val reference = obj.reference
        val presence  = obj.presence
        if (presence.isPresentOn(boundId))
            Some(reference)
        else {
            //send a reference to the object we want to synchronize
            val opt = findNonNetworkObjectReference(obj).map(OriginReferencedConnectedObjectReference(reference, _))
            if (opt.isEmpty) {
                presence match {
                    case presence: ExternalNetworkObjectPresence[_] =>
                        //As no reference has been found, the object will be send to the target, so we manually
                        // set it as present for the bound identifier (the target identifier)
                        // in order to avoid sending the same object several times when the same
                        // object is present in several successive packets
                        presence.setToPresent(boundId)
                    case _                                          =>
                }
            }
            opt
        }
    }

    private def findNonNetworkObjectReference(obj: AnyRef): Option[NetworkObjectReference] = {
        (col.findReferenceID(obj) match {
            case None     => None
            case Some(id) =>
                val ref = new ContextualObjectReference(packetPath, id)
                if (col.isPresentOnEngine(boundId, ref)) Some(ref)
                else None
        }) match {
            case None =>
            case some => some
        }
    }

    private def findDynamicNetworkObjectReference(obj: DynamicNetworkObject[NetworkObjectReference]): Option[NetworkObjectReference] = {
        val reference = obj.reference
        val presence  = obj.presence
        if (presence.isPresentOn(boundId))
            Some(reference)
        else {
            if (rnol.isDefined && !gnol.touchesAnyLinker(reference))
                rnol.get.save(obj)
            val opt = findNonNetworkObjectReference(obj) //send a reference to the object we want to synchronize
            if (opt.isEmpty) {
                presence match {
                    case presence: ExternalNetworkObjectPresence[_] =>
                        //As no reference has been found, the object will be send to the target, so we manually
                        // set it as present for the bound identifier (the target identifier)
                        // in order to avoid sending the same object several times when the same
                        // object is present in several successive packets
                        presence.setToPresent(boundId)
                    case _                                          =>
                }
            }
            opt
        }
    }

    private def findNetworkObjectReference(obj: NetworkObject[NetworkObjectReference]): Option[NetworkObjectReference] = {
        val reference = obj.reference
        if (gnol.findPresence(reference).exists(_.isPresentOn(boundId)))
            Some(reference)
        else {
            if (rnol.isDefined && !gnol.touchesAnyLinker(reference))
                rnol.get.save(obj)
            findNonNetworkObjectReference(obj) //send a reference to the object we want to synchronize
        }
    }

    def findObject(reference: NetworkObjectReference): Option[AnyRef] = {
        gnol.findObject(reference) match {
            case Some(value: ContextObject) =>
                Some(value.obj)
            case o                          => o
        }
    }

    def handleObject(obj: NetworkObject[NetworkObjectReference]): Unit = {
        val reference = obj.reference
        if (reference.isInstanceOf[SharedCacheManagerReference])
            cnol.initializeObject(obj.asInstanceOf[NetworkObject[SharedCacheManagerReference]])
        else if (rnol.isDefined && !gnol.touchesAnyLinker(reference))
            rnol.get.save(obj)
    }

}
