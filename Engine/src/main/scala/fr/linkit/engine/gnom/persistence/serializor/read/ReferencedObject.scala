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

package fr.linkit.engine.gnom.persistence.serializor.read

import fr.linkit.api.gnom.cache.SharedCacheReference
import fr.linkit.api.gnom.cache.sync.OriginReferencedSyncObjectReference
import fr.linkit.api.gnom.persistence.obj.{ProfilePoolObject, ReferencedPoolObject}
import fr.linkit.api.gnom.reference.NetworkObjectReference
import fr.linkit.engine.gnom.cache.sync.DefaultSynchronizedObjectCache
import fr.linkit.engine.gnom.persistence.UnexpectedObjectException
import fr.linkit.engine.gnom.persistence.obj.ObjectSelector
import fr.linkit.engine.gnom.persistence.serializor.ConstantProtocol.Object

class ReferencedObject(override val referenceIdx: Int,
                       selector: ObjectSelector,
                       pool: DeserializerObjectPool) extends ReferencedPoolObject {

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
        val loc = reference
        loc.getClass.synchronized {
            loc match {
                case OriginReferencedSyncObjectReference(syncRef, originRef) =>
                    val cacheRef = new SharedCacheReference(syncRef.family, syncRef.cacheID)
                    selector.findObject(cacheRef).map {
                        case cache: DefaultSynchronizedObjectCache[AnyRef] =>
                            val origin   = selector.findObject(originRef).getOrElse {
                                throw new NoSuchElementException(s"Could not find network object referenced at $loc.")
                            }
                            cache.forest.linkWithReference(origin, syncRef)
                            origin
                        case cache =>
                            throw new UnsupportedOperationException(s"Could not deserialize referenced sync object: $cacheRef referer to a shared cache of type '${cache.getClass.getName}', expected: ${classOf[DefaultSynchronizedObjectCache[_]].getName}. ")
                    }.getOrElse(throw new NoSuchElementException(s"could not find any shared cache at $cacheRef"))

                case loc =>
                    selector.findObject(loc).getOrElse {
                        throw new NoSuchElementException(s"Could not find network object referenced at $loc.")
                    }
            }
        }
    }

}