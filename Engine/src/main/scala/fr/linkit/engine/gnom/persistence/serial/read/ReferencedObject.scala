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

package fr.linkit.engine.gnom.persistence.serial.read

import fr.linkit.api.gnom.cache.SharedCacheReference
import fr.linkit.api.gnom.cache.sync.{ConnectedObject, OriginReferencedConnectedObjectReference}
import fr.linkit.api.gnom.persistence.obj.{ProfilePoolObject, ReferencedPoolObject}
import fr.linkit.api.gnom.referencing.NetworkObjectReference
import fr.linkit.engine.gnom.cache.sync.DefaultConnectedObjectCache
import fr.linkit.engine.gnom.persistence.UnexpectedObjectException
import fr.linkit.engine.gnom.persistence.obj.{NetworkObjectReferencesLocks, ObjectSelector}
import fr.linkit.engine.gnom.persistence.ProtocolConstants.Object

class ReferencedObject(override val referenceIdx: Int,
                       selector                 : ObjectSelector,
                       pool                     : DeserializerObjectPool) extends ReferencedPoolObject {

    override lazy val reference: NetworkObjectReference = pool
            .getChunkFromFlag[ProfilePoolObject[AnyRef]](Object)
            .get(referenceIdx)
            .value match {
        case l: NetworkObjectReference => l
        case o                         =>
            throw new UnexpectedObjectException(s"Received object '$o' which seems to be used as a network reference location, but does not extends NetworkReferenceLocation.")
    }

    override val identity: Int = referenceIdx

    override lazy val value: AnyRef = {
        val lock = NetworkObjectReferencesLocks.getLock(reference)
        lock.lock()
        try {
            retrieveObject()
        } finally lock.unlock()
    }

    private def retrieveObject(): AnyRef = (reference match {
        case OriginReferencedConnectedObjectReference(syncRef, originRef) =>
            val cacheRef = new SharedCacheReference(syncRef.family, syncRef.cacheID)
            selector.findObject(cacheRef).map {
                case cache: DefaultConnectedObjectCache[AnyRef] =>
                    val origin = selector.findObject(originRef).getOrElse {
                        throw new NoSuchElementException(s"Could not find network object referenced at $reference.")
                    }
                    cache.forest.linkWithReference(origin, syncRef)
                    origin
                case cache                                      =>
                    throw new UnsupportedOperationException(s"Could not deserialize referenced sync object: $cacheRef referer to a shared cache of type '${cache.getClass.getName}', expected: ${classOf[DefaultConnectedObjectCache[_]].getName}. ")
            }.getOrElse(throw new NoSuchElementException(s"Could not find any shared cache at $cacheRef"))

        case loc =>
            selector.findObject(loc) match {
                case Some(value) => value
                case None        =>
                    selector.findObject(loc) //for debugger purposes
                    throw new NoSuchElementException(s"Could not find network object referenced at $loc.")
            }

    }) match {
        case c: ConnectedObject[AnyRef] =>
            c.connected
        case o                          => o
    }
}