/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.persistence.obj

import fr.linkit.api.gnom.cache.sync.{ConnectedObject, OriginReferencedConnectedObjectReference}
import fr.linkit.api.gnom.persistence.PersistenceBundle
import fr.linkit.api.gnom.persistence.context.ContextualObjectReference
import fr.linkit.api.gnom.referencing.linker.InitialisableNetworkObjectLinker
import fr.linkit.api.gnom.referencing.{DynamicNetworkObject, NetworkObject, NetworkObjectReference, StaticNetworkObject}
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.cache.sync.ChippedObjectAdapter
import fr.linkit.engine.gnom.referencing.ContextObject
import fr.linkit.engine.gnom.referencing.presence.ExternalNetworkObjectPresence

import java.util.concurrent.locks.ReentrantReadWriteLock

class ObjectSelector(bundle: PersistenceBundle) {

    private val gnol       = bundle.network.gnol
    private val rnol       = gnol.defaultNOL
    private val col        = bundle.config.contextualObjectLinker
    private val boundId    = bundle.boundId
    private val packetPath = bundle.packetPath

    def findObjectReference(obj: AnyRef): Option[NetworkObjectReference] = {
        if (!obj.isInstanceOf[NetworkObject[_]]) {
            return findNonNetworkObjectReference(obj)
        }
        val found = obj match {
            case sync: ConnectedObject[_]                          =>
                findConnectedObjectReference(sync)
            case obj: DynamicNetworkObject[NetworkObjectReference] =>
                findDynamicNetworkObjectReference(obj)
            case obj: StaticNetworkObject[NetworkObjectReference]  =>
                Some(obj.reference)
            case obj: NetworkObject[NetworkObjectReference]        =>
                findNetworkObjectReference(obj)
        }
        found
    }

    private def findConnectedObjectReference(obj: ConnectedObject[_]): Option[NetworkObjectReference] = {
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
                        AppLoggers.Persistence.trace(s"Presence at '$reference' automatically presumed to be present on engine $boundId as the object will be send to the engine")

                    case _ =>
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
        }).orElse(ChippedObjectAdapter.findAdapter(obj).flatMap(findConnectedObjectReference))
    }

    private def findDynamicNetworkObjectReference(obj: DynamicNetworkObject[NetworkObjectReference]): Option[NetworkObjectReference] = {
        val reference = obj.reference
        val presence  = obj.presence
        if (presence.isPresentOn(boundId))
            Some(reference)
        else {
            if (!gnol.touchesAnyLinker(reference))
                rnol.save(obj)
            val opt = findNonNetworkObjectReference(obj) //send a reference to the object we want to synchronize
            if (opt.isEmpty) {
                presence match {
                    case presence: ExternalNetworkObjectPresence[_] =>
                        //As no reference has been found, the object will be send to the target, so we manually
                        // set it as present for the bound identifier (the target identifier)
                        // in order to avoid sending the same object several times when the same
                        // object is present in several successive packets
                        presence.setToPresent(boundId)
                        AppLoggers.Persistence.trace(s"Presence at '$reference' automatically presumed to be present on engine $boundId as the object will be send to the engine")

                    case _ =>
                }
            }
            opt
        }
    }

    private def findNetworkObjectReference(obj: NetworkObject[NetworkObjectReference]): Option[NetworkObjectReference] = {
        val reference = obj.reference
        if (gnol.getPresence(reference).isPresentOn(boundId))
            Some(reference)
        else {
            if (!gnol.touchesAnyLinker(reference))
                rnol.save(obj)
            findNonNetworkObjectReference(obj) //send a reference of the object we want to synchronize
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
        val lock = NetworkObjectReferencesLocks.getLock(reference)
        lock.lock()
        try {
            gnol.findRootLinker(reference) match {
                case Some(linker: InitialisableNetworkObjectLinker[NetworkObjectReference]) =>
                    AppLoggers.Persistence.trace(s"Initializing network object $reference")
                    linker.initializeObject(obj)
                case _                                                                      =>
                    if (!gnol.touchesAnyLinker(reference)) {
                        AppLoggers.Persistence.trace(s"No linker found for object $reference. The object is stored in a remaining object linker.")
                        rnol.save(obj)
                        return
                    }
                    AppLoggers.Persistence.trace(s"No handling method found for reference $reference.")
            }
        } finally {
            lock.unlock()
        }
    }

}
