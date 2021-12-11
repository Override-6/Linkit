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
import fr.linkit.api.gnom.reference.{DynamicNetworkObject, NetworkObject, NetworkObjectReference, StaticNetworkObject, SystemNetworkObjectPresence}
import fr.linkit.engine.gnom.reference.ContextObject
import fr.linkit.engine.gnom.reference.presence.{ExternalNetworkObjectPresence, InternalNetworkObjectPresence}

class ObjectSelector(bundle: PersistenceBundle) {

    private val gnol       = bundle.gnol
    private val rnol       = gnol.remainingNOL
    private val col        = bundle.config.contextualObjectLinker
    private val boundId    = bundle.boundId
    private val packetPath = bundle.packetPath
    private val cnol       = gnol.cacheNOL

    def findObjectReference(obj: AnyRef): Option[NetworkObjectReference] = {
        if (!obj.isInstanceOf[NetworkObject[_]]) {
            return findNonNetworkObjectReference(obj)
        }
        obj match {
            case obj: DynamicNetworkObject[NetworkObjectReference] =>
                findDynamicNetworkObjectReference(obj)
            case obj: StaticNetworkObject[NetworkObjectReference]  =>
                Some(obj.reference)
            case obj: NetworkObject[NetworkObjectReference]        =>
                findNetworkObjectReference(obj)
        }
    }

    def informObjectSent(no: NetworkObject[_]): Unit = {
        no match {
            case obj: DynamicNetworkObject[NetworkObjectReference] =>
                informDynamicNetworkObjectSent(obj)
            case _: StaticNetworkObject[NetworkObjectReference]    =>
            //Static objects are supposed to be statically presents on engines.
            //Thus Static Netowrk Objects are already presents on all engines.
            case obj: NetworkObject[NetworkObjectReference] =>
                informNetworkObjectSent(obj)
        }
    }

    private def informDynamicNetworkObjectSent(obj: DynamicNetworkObject[NetworkObjectReference]): Unit = {
        obj.presence match {
            case presence: ExternalNetworkObjectPresence[_] => presence.onObjectSet(boundId)
            case _                                          =>
        }
    }

    private def informNetworkObjectSent(obj: NetworkObject[NetworkObjectReference]): Unit = {
        gnol.findPresence(obj.reference) match {
            case Some(presence) => presence match {
                case presence: ExternalNetworkObjectPresence[_] => presence.onObjectSet(boundId)
                case _                                          =>
            }
            case _              =>
        }
    }

    private def findNonNetworkObjectReference(obj: AnyRef): Option[NetworkObjectReference] = {
        col.findReferenceID(obj) match {
            case None     => None
            case Some(id) =>
                val ref = new ContextualObjectReference(packetPath, id)
                if (col.isPresentOnEngine(boundId, ref)) Some(ref)
                else None
        }
    }

    private def findDynamicNetworkObjectReference(obj: DynamicNetworkObject[NetworkObjectReference]): Option[NetworkObjectReference] = {
        val reference = obj.reference
        if (obj.presence.isPresentOn(boundId))
            Some(reference)
        else {
            if (rnol.isDefined && !gnol.touchesAnyLinker(reference))
                rnol.get.save(obj)
            None
        }
    }

    private def findNetworkObjectReference(obj: NetworkObject[NetworkObjectReference]): Option[NetworkObjectReference] = {
        val reference = obj.reference
        if (gnol.findPresence(reference).exists(_.isPresentOn(boundId)))
            Some(reference)
        else {
            if (rnol.isDefined && !gnol.touchesAnyLinker(reference))
                rnol.get.save(obj)
            None
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
