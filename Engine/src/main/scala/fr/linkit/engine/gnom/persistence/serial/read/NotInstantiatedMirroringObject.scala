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

import fr.linkit.api.gnom.cache.NoSuchCacheException
import fr.linkit.api.gnom.cache.sync.ConnectedObjectReference
import fr.linkit.api.gnom.cache.sync.contract.description.SyncClassDef
import fr.linkit.api.gnom.persistence.obj.{ProfilePoolObject, SyncPoolObject}
import fr.linkit.engine.gnom.cache.sync.InternalConnectedObjectCache
import fr.linkit.engine.gnom.persistence.ProtocolConstants.Object
import fr.linkit.engine.gnom.persistence.UnexpectedObjectException
import fr.linkit.engine.gnom.persistence.obj.ObjectSelector

class MirroringObject(override val referenceIdx: Int,
                      override val stubClassDef: SyncClassDef,
                      selector                 : ObjectSelector,
                      pool                     : DeserializerObjectPool) extends SyncPoolObject {

    override lazy val reference: ConnectedObjectReference = pool
            .getChunkFromFlag[ProfilePoolObject[AnyRef]](Object)
            .get(referenceIdx)
            .value match {
        case l: ConnectedObjectReference => l
        case o                           =>
            throw new UnexpectedObjectException(s"Received reference '$o' to point at a mirrored connected object but it is not a connected object reference")
    }

    override val identity: Int = referenceIdx

    override lazy val value: AnyRef = {
        val ref      = reference
        val cacheRef = ref.asSuper.get
        lazy val errPrefix = s"Could not deserialize mirroring object '$ref':"
        selector.findObject(cacheRef).getOrElse {
            throw new NoSuchCacheException(s"$errPrefix cache '$cacheRef' not found.")
        } match {
            case cache: InternalConnectedObjectCache[_] =>
                val connector = cache.registry.connector
                val connected = connector.makeMirroredObject(stubClassDef, ref.owner, ref.identifier).obj
                connected
        }
    }
}